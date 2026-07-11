<template>
  <div class="page-shell" v-if="exam">
    <div class="exam-top-bar">
      <div class="exam-info">
        <h2 class="exam-title">{{ exam.examName }}</h2>
        <span class="exam-course">{{ exam.courseName || '' }}</span>
      </div>
      <div class="exam-actions">
        <el-tag type="danger" size="large">剩余 {{ formatTime(remaining) }}</el-tag>
      </div>
    </div>

    <div class="exam-body">
      <div class="exam-sidebar">
        <div class="panel index-panel">
          <h4 class="section-title">题目索引</h4>
          <div class="index-grid">
            <span v-for="(q, i) in questions" :key="q.id" :class="['index-item',
              currentIndex === i ? 'current' : '',
              isQuestionAnswered(q) ? 'answered' : '',
              markedQuestions.has(q.id) ? 'marked' : '']" @click="currentIndex = i">
              {{ i + 1 }}
            </span>
          </div>
          <div class="index-legend">
            <span><span class="dot answered-dot"></span> 已答</span>
            <span><span class="dot current-dot"></span> 当前</span>
            <span><span class="dot marked-dot"></span> 标记</span>
          </div>
          <el-button type="danger" style="width:100%;margin-top:16px" :loading="submitting"
            @click="confirmSubmit">交卷</el-button>
        </div>
      </div>

      <div class="exam-main">
        <div class="panel" v-if="currentQuestion">
          <div class="question-header">
            <span class="q-number">第 {{ currentIndex + 1 }} 题（{{ typeLabel(currentQuestion.questionType) }}）</span>
            <el-button size="small" :type="markedQuestions.has(currentQuestion.id) ? 'warning' : 'default'"
              @click="toggleMark(currentQuestion.id)">
              {{ markedQuestions.has(currentQuestion.id) ? '已标记' : '标记本题' }}
            </el-button>
          </div>

          <div class="question-content">
            <p class="q-text">{{ currentQuestion.content }}</p>
            <div class="options">
              <template v-if="['SINGLE', 'JUDGE'].includes(currentQuestion.questionType)">
                <el-radio-group v-model="answers[currentQuestion.id]" class="option-group">
                  <el-radio v-for="(opt, oi) in parseOptions(currentQuestion)" :key="oi" :value="opt.label"
                    class="option-item">{{ opt.label }}. {{ opt.content }}</el-radio>
                </el-radio-group>
              </template>
              <template v-else-if="currentQuestion.questionType === 'MULTI'">
                <el-checkbox-group v-model="multiAnswers[currentQuestion.id]" class="option-group">
                  <el-checkbox v-for="(opt, oi) in parseOptions(currentQuestion)" :key="oi" :value="opt.label"
                    class="option-item">{{ opt.label }}. {{ opt.content }}</el-checkbox>
                </el-checkbox-group>
              </template>
              <template v-else-if="currentQuestion.questionType === 'FILL'">
                <el-input v-model="answers[currentQuestion.id]" placeholder="请输入答案" size="large" />
              </template>
              <template v-else-if="currentQuestion.questionType === 'CODE'">
                <div class="code-area">
                  <CodeEditor v-model="answers[currentQuestion.id]" :language="getCodeLanguage(currentQuestion)"
                    :template-code="getCodeTemplate(currentQuestion)" :show-run-button="true" :show-results="true"
                    :run-results="runResults" :running="running" @run="handleCodeRun" />
                  <div v-if="getCodeTestCases(currentQuestion).length" class="sample-cases">
                    <h4 class="sample-cases-title">样例测试用例</h4>
                    <div v-for="(tc, idx) in getCodeTestCases(currentQuestion)" :key="idx" class="sample-case">
                      <div><b>输入：</b><code>{{ tc.input }}</code></div>
                      <div><b>预期输出：</b><code>{{ tc.expectedOutput }}</code></div>
                    </div>
                  </div>
                </div>
              </template>
              <template v-else>
                <el-input v-model="answers[currentQuestion.id]" type="textarea" :rows="5" placeholder="请输入作答内容" />
              </template>
            </div>
          </div>

          <div class="q-actions">
            <el-button :disabled="currentIndex === 0" @click="currentIndex--">上一题</el-button>
            <el-button :disabled="currentIndex >= questions.length - 1" @click="currentIndex++">下一题</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { examApi, codeApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import CodeEditor from '@/components/CodeEditor.vue'

const route = useRoute(); const router = useRouter()
const exam = ref(null)
const questions = ref([])
const answers = reactive({})
const multiAnswers = reactive({})
const markedQuestions = ref(new Set())
const currentIndex = ref(0)
const remaining = ref(0)
const cheatCount = ref(0)
const submitting = ref(false)
const running = ref(false)
const runResults = ref([])
let timer = null

const currentQuestion = computed(() => questions.value[currentIndex.value] || null)

onMounted(async () => {
  // 考试须知
  await ElMessageBox.confirm('考试须知：请独立完成考试，系统将记录切屏次数。请在规定时间内作答。', '考试须知', {
    confirmButtonText: '开始考试', cancelButtonText: '稍后再考', type: 'warning'
  })
  try {
    const res = await examApi.startExam(route.params.id)
    if (res.data.code === 200) {
      const data = res.data.data
      exam.value = data
      questions.value = data.questions || []
      initializeAnswerState()
      remaining.value = (data.duration || 120) * 60
      startTimer()
    }
  } catch (e) { router.back() }
  // 监听切屏
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onBeforeUnmount(() => {
  clearInterval(timer)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})

// 切换题目时清空上一次的运行结果
watch(currentIndex, () => {
  runResults.value = []
})

function handleVisibilityChange() {
  if (document.hidden) cheatCount.value++
}

function startTimer() {
  timer = setInterval(() => {
    remaining.value--
    if (remaining.value <= 0) { submitExam() }
  }, 1000)
}

function initializeAnswerState() {
  questions.value.forEach(q => {
    if (q.questionType === 'MULTI' && !Array.isArray(multiAnswers[q.id])) {
      multiAnswers[q.id] = []
    } else if (q.questionType === 'CODE') {
      // 编程题预填模板代码
      if (!answers[q.id]) {
        answers[q.id] = getCodeTemplate(q)
      }
    } else if (!answers[q.id]) {
      answers[q.id] = ''
    }
  })
}

function parseOptions(question) {
  if (question?.questionType === 'JUDGE') {
    return [{ label: '1', content: '正确' }, { label: '0', content: '错误' }]
  }
  const options = question?.options
  if (!options) return []
  try {
    const obj = typeof options === 'string' ? JSON.parse(options) : options
    if (Array.isArray(obj)) {
      return obj.map((item, index) => {
        if (typeof item === 'string') {
          const label = String.fromCharCode(65 + index)
          return { label, content: item }
        }
        return {
          label: item.label || item.key || String.fromCharCode(65 + index),
          content: item.content || item.value || ''
        }
      })
    }
    return Object.entries(obj).map(([label, content]) => ({ label, content: String(content || '') }))
  } catch (e) { return [] }
}

function isQuestionAnswered(question) {
  if (!question) return false
  if (question.questionType === 'MULTI') {
    return Array.isArray(multiAnswers[question.id]) && multiAnswers[question.id].length > 0
  }
  const value = answers[question.id]
  return value !== undefined && value !== null && String(value).trim() !== ''
}

function typeLabel(type) {
  const map = { SINGLE: '单选题', MULTI: '多选题', JUDGE: '判断题', FILL: '填空题', ESSAY: '简答题', CODE: '编程题' }
  return map[type] || type
}

function getCodeLanguage(question) {
  try { const cfg = JSON.parse(question.programmingConfig || '{}'); return cfg.language || 'cpp' } catch { return 'cpp' }
}

function getCodeTemplate(question) {
  try { const cfg = JSON.parse(question.programmingConfig || '{}'); return cfg.templateCode || '' } catch { return '' }
}

function getCodeTestCases(question) {
  try { const cfg = JSON.parse(question.programmingConfig || '{}'); return cfg.testCases || [] } catch { return [] }
}

async function handleCodeRun({ code, language }) {
  if (running.value) return
  running.value = true
  runResults.value = []
  try {
    const testCases = getCodeTestCases(currentQuestion.value).map(tc => ({
      input: tc.input || '', expectedOutput: tc.expectedOutput || ''
    }))
    const res = await codeApi.run({ language, sourceCode: code, testCases })
    if (res.data.code === 200) {
      runResults.value = (res.data.data.caseResults || []).map(r => ({ ...r, passed: r.passed }))
    }
  } catch (e) {
    ElMessage.error('代码运行失败')
  } finally { running.value = false }
}

function toggleMark(qid) {
  if (markedQuestions.value.has(qid)) markedQuestions.value.delete(qid)
  else markedQuestions.value.add(qid)
  markedQuestions.value = new Set(markedQuestions.value)
}

function formatTime(sec) {
  const h = Math.floor(sec / 3600); const m = Math.floor((sec % 3600) / 60); const s = sec % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

async function confirmSubmit() {
  try {
    await ElMessageBox.confirm('确定要交卷吗？交卷后不可修改。', '确认交卷', { type: 'warning' })
    await submitExam()
  } catch (e) { /* cancelled */ }
}

async function submitExam() {
  if (submitting.value) return
  submitting.value = true
  clearInterval(timer)
  const answerList = questions.value.map(q => ({
    questionId: Number(q.id),
    answer: q.questionType === 'MULTI'
      ? (multiAnswers[q.id] || []).slice().sort().join(',')
      : String(answers[q.id] ?? '')
  }))
  try {
    await examApi.submitExam(route.params.id, { answers: answerList, cheatCount: cheatCount.value })
    ElMessage.success('交卷成功')
    router.push('/student/exams')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '交卷失败，请稍后重试')
    if (remaining.value > 0) startTimer()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.exam-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 20px 24px;
}

.exam-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.exam-course {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
  display: block;
}

.exam-body {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.exam-sidebar {
  width: 200px;
  flex-shrink: 0;
}

.exam-main {
  flex: 1;
  min-width: 0;
}

.panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 14px 0;
}

.index-panel {
  position: sticky;
  top: 84px;
}

.index-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.index-item {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  background: #f5f5f5;
  color: #909399;
  transition: all 0.2s;
  font-weight: 500;
}

.index-item.answered {
  background: #f6ffed;
  color: #52c41a;
  border: 2px solid #52c41a;
}

.index-item.current {
  background: #e6f7ff;
  color: #1890ff;
  border: 2px solid #1890ff;
}

.index-item.marked {
  background: #fff7e6;
  color: #faad14;
  border: 2px solid #faad14;
}

.index-legend {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
  flex-wrap: wrap;
}

.dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 3px;
}

.answered-dot {
  background: #52c41a;
}

.current-dot {
  background: #1890ff;
}

.marked-dot {
  background: #faad14;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.q-number {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.q-text {
  font-size: 16px;
  line-height: 1.8;
  margin-bottom: 24px;
  color: #303133;
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.option-item {
  font-size: 15px;
}

.q-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
  justify-content: center;
}

.sample-cases {
  margin-top: 16px;
  padding: 12px 16px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.sample-cases-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 10px 0;
}

.sample-case {
  padding: 8px 0;
  border-bottom: 1px dashed #e5e7eb;
  font-size: 13px;
  line-height: 1.8;
}

.sample-case:last-child {
  border-bottom: none;
}

.sample-case code {
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Cascadia Code', 'Consolas', monospace;
  font-size: 12px;
}

.code-area {
  max-height: calc(100vh - 340px);
  overflow-y: auto;
  padding-right: 4px;
}

.code-area::-webkit-scrollbar {
  width: 6px;
}

.code-area::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}
</style>
