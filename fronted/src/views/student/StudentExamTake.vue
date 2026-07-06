<template>
  <div class="student-exam-take" v-if="exam">
    <div class="exam-header">
      <div class="exam-info">
        <h2>📏 {{ exam.examName }}</h2>
      </div>
      <div class="exam-timer">
        <el-tag type="danger" size="large">⏰ 剩余 {{ formatTime(remaining) }}</el-tag>
      </div>
    </div>

    <div class="exam-body">
      <!-- 题目索引 -->
      <div class="question-index">
        <el-card shadow="hover">
          <template #header>题目索引</template>
          <div class="index-grid">
            <span v-for="(q, i) in questions" :key="q.id"
                  :class="['index-item',
                    currentIndex === i ? 'current' : '',
                    answers[q.id] ? 'answered' : '',
                    markedQuestions.has(q.id) ? 'marked' : '']"
                  @click="currentIndex = i">
              {{ i + 1 }}
            </span>
          </div>
          <div class="index-legend">
            <span><span class="dot answered-dot"></span> 已答</span>
            <span><span class="dot current-dot"></span> 当前</span>
            <span><span class="dot marked-dot"></span> 标记</span>
          </div>
        </el-card>
      </div>

      <!-- 答题区 -->
      <div class="answer-area">
        <el-card shadow="hover" v-if="currentQuestion">
          <template #header>
            <div class="question-header">
              <span>第 {{ currentIndex + 1 }} 题（{{ typeLabel(currentQuestion.questionType) }}）</span>
              <el-button size="small" :type="markedQuestions.has(currentQuestion.id) ? 'warning' : 'default'"
                         @click="toggleMark(currentQuestion.id)">
                {{ markedQuestions.has(currentQuestion.id) ? '🏷 已标记' : '💡 标记本题' }}
              </el-button>
            </div>
          </template>

          <div class="question-content">
            <p class="q-text">{{ currentQuestion.content }}</p>
            <!-- 选项 -->
            <div class="options">
              <template v-if="['SINGLE', 'JUDGE'].includes(currentQuestion.questionType)">
                <el-radio-group v-model="answers[currentQuestion.id]" class="option-group">
                  <el-radio v-for="(opt, oi) in parseOptions(currentQuestion.options)" :key="oi"
                            :value="opt.key" class="option-item">{{ opt.key }}. {{ opt.value }}</el-radio>
                </el-radio-group>
              </template>
              <template v-else-if="currentQuestion.questionType === 'MULTI'">
                <el-checkbox-group v-model="multiAnswers[currentQuestion.id]" class="option-group">
                  <el-checkbox v-for="(opt, oi) in parseOptions(currentQuestion.options)" :key="oi"
                               :value="opt.key" class="option-item">{{ opt.key }}. {{ opt.value }}</el-checkbox>
                </el-checkbox-group>
              </template>
              <template v-else-if="currentQuestion.questionType === 'FILL'">
                <el-input v-model="answers[currentQuestion.id]" placeholder="请输入答案" />
              </template>
              <template v-else>
                <el-input v-model="answers[currentQuestion.id]" type="textarea" :rows="5" placeholder="请输入作答内容" />
              </template>
            </div>
          </div>

          <div class="q-actions">
            <el-button :disabled="currentIndex === 0" @click="currentIndex--">上一题</el-button>
            <el-button :disabled="currentIndex >= questions.length - 1" @click="currentIndex++">下一题</el-button>
            <el-button type="warning" @click="toggleMark(currentQuestion.id)">标记</el-button>
            <el-button type="danger" @click="confirmSubmit">📝 交卷</el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { examApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute(); const router = useRouter()
const exam = ref(null)
const questions = ref([])
const answers = reactive({})
const multiAnswers = reactive({})
const markedQuestions = ref(new Set())
const currentIndex = ref(0)
const remaining = ref(0)
const cheatCount = ref(0)
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

function handleVisibilityChange() {
  if (document.hidden) cheatCount.value++
}

function startTimer() {
  timer = setInterval(() => {
    remaining.value--
    if (remaining.value <= 0) { submitExam() }
  }, 1000)
}

function parseOptions(options) {
  if (!options) return []
  try {
    const obj = typeof options === 'string' ? JSON.parse(options) : options
    return Object.entries(obj).map(([k, v]) => ({ key: k, value: v }))
  } catch (e) { return [] }
}

function typeLabel(type) {
  const map = { SINGLE: '单选题', MULTI: '多选题', JUDGE: '判断题', FILL: '填空题', ESSAY: '简答题' }
  return map[type] || type
}

function toggleMark(qid) {
  if (markedQuestions.value.has(qid)) markedQuestions.value.delete(qid)
  else markedQuestions.value.add(qid)
  markedQuestions.value = new Set(markedQuestions.value)
}

function formatTime(sec) {
  const h = Math.floor(sec / 3600); const m = Math.floor((sec % 3600) / 60); const s = sec % 60
  return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`
}

async function confirmSubmit() {
  try {
    await ElMessageBox.confirm('确定要交卷吗？交卷后不可修改。', '确认交卷', { type: 'warning' })
    await submitExam()
  } catch (e) { /* cancelled */ }
}

async function submitExam() {
  clearInterval(timer)
  // 处理多选题答案
  for (const qid in multiAnswers) {
    answers[qid] = multiAnswers[qid].join(',')
  }
  const answerList = Object.entries(answers).map(([qid, ans]) => ({ questionId: Number(qid), answer: ans }))
  try {
    await examApi.submitExam(route.params.id, { answers: answerList, cheatCount: cheatCount.value })
    ElMessage.success('交卷成功')
    router.push('/student/exams')
  } catch (e) { /* ignore */ }
}
</script>

<style scoped>
.exam-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; padding: 16px 24px; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.exam-body { display: flex; gap: 20px; }
.question-index { width: 180px; flex-shrink: 0; }
.index-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; }
.index-item { width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; border-radius: 4px; font-size: 13px; cursor: pointer; background: #f0f0f0; color: #909399; transition: all 0.2s; }
.index-item.answered { background: #f6ffed; color: #52c41a; border: 2px solid #52c41a; }
.index-item.current { background: #e6f7ff; color: #1890ff; border: 2px solid #1890ff; }
.index-item.marked { background: #fff7e6; color: #faad14; border: 2px solid #faad14; }
.index-legend { display: flex; gap: 12px; margin-top: 12px; font-size: 12px; }
.dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 4px; }
.answered-dot { background: #52c41a; } .current-dot { background: #1890ff; } .marked-dot { background: #faad14; }
.answer-area { flex: 1; }
.question-header { display: flex; justify-content: space-between; align-items: center; }
.q-text { font-size: 16px; line-height: 1.8; margin-bottom: 20px; color: #303133; }
.option-group { display: flex; flex-direction: column; gap: 12px; }
.option-item { margin: 0; padding: 10px 16px; background: #fafafa; border-radius: 6px; transition: all 0.2s; }
.q-actions { display: flex; gap: 12px; margin-top: 24px; justify-content: center; }
</style>
