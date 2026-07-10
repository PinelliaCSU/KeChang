<template>
  <div class="quiz-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">课堂互动</p>
        <h2>随堂测验</h2>
      </div>
      <el-button plain @click="loadActiveQuizzes">刷新</el-button>
    </div>

    <div class="layout">
      <section class="panel quiz-list-panel">
        <div class="panel-title">
          <h3>正在进行</h3>
          <span>{{ quizzes.length }} 个测验</span>
        </div>
        <div class="quiz-list">
          <div
            v-for="quiz in quizzes"
            :key="quiz.id"
            class="quiz-card"
            :class="{ active: currentQuiz?.id === quiz.id, submitted: quiz.submitted }"
            @click="selectQuiz(quiz)"
          >
            <div class="quiz-card-top">
              <el-tag :type="quiz.submitted ? 'success' : 'warning'" size="small">{{ quiz.submitted ? '已提交' : '待作答' }}</el-tag>
              <span>{{ formatTime(quiz.remainingSeconds || 0) }}</span>
            </div>
            <strong>{{ quiz.title }}</strong>
            <p>{{ quiz.courseName }}</p>
          </div>
          <el-empty v-if="!loading && quizzes.length === 0" description="暂无正在进行的随堂测验" :image-size="100" />
        </div>
      </section>

      <section v-if="currentQuiz" class="panel answer-panel">
        <div class="answer-head">
          <div>
            <p class="eyebrow">{{ currentQuiz.courseName }}</p>
            <h3>{{ currentQuiz.title }}</h3>
          </div>
          <el-tag size="large" :type="currentQuiz.submitted ? 'success' : 'danger'">
            {{ currentQuiz.submitted ? '已提交' : `剩余 ${formatTime(currentQuiz.remainingSeconds || 0)}` }}
          </el-tag>
        </div>

        <div v-if="quizQuestions.length" class="question-stack">
          <div v-for="(question, index) in quizQuestions" :key="question.id" class="question-box">
            <div class="question-meta">
              <span class="question-index">第 {{ index + 1 }} 题</span>
              <el-tag :type="typeTag(question.questionType)">{{ typeLabel(question.questionType) }}</el-tag>
              <span>{{ question.knowledgePoint || '未标注知识点' }}</span>
            </div>
            <p>{{ question.content }}</p>

            <div class="answer-area">
              <template v-if="['SINGLE', 'JUDGE'].includes(question.questionType)">
                <el-radio-group v-model="answers[question.id]" class="option-group" :disabled="currentQuiz.submitted">
                  <el-radio v-for="opt in parseOptions(question)" :key="opt.label" :value="opt.label" class="option-item">
                    {{ opt.label }}. {{ opt.content }}
                  </el-radio>
                </el-radio-group>
              </template>
              <template v-else-if="question.questionType === 'MULTI'">
                <el-checkbox-group v-model="multiAnswers[question.id]" class="option-group" :disabled="currentQuiz.submitted">
                  <el-checkbox v-for="opt in parseOptions(question)" :key="opt.label" :value="opt.label" class="option-item">
                    {{ opt.label }}. {{ opt.content }}
                  </el-checkbox>
                </el-checkbox-group>
              </template>
              <template v-else>
                <el-input v-model="answers[question.id]" type="textarea" :rows="4" placeholder="请输入你的答案" :disabled="currentQuiz.submitted" />
              </template>
            </div>
          </div>
        </div>

        <div class="submit-bar">
          <div v-if="currentQuiz.submitted" class="submitted-text">
            已提交 {{ quizQuestions.length }} 道题，
            <strong>{{ correctText }}</strong>
          </div>
          <el-button v-else type="primary" size="large" :loading="submitting" @click="submitAnswer">提交答案</el-button>
        </div>

        <div v-if="stats" class="stats-box">
          <div class="panel-title compact">
            <h3>全班答题情况</h3>
            <span>已答 {{ stats.submittedCount || 0 }} / {{ stats.totalStudents || 0 }}</span>
          </div>
          <div v-if="stats.questionStats?.length" class="question-stats-list">
            <div v-for="(qStat, index) in stats.questionStats" :key="qStat.questionId" class="question-stat-card">
              <div class="question-stat-title">
                <strong>{{ index + 1 }}. {{ qStat.question?.content }}</strong>
                <span>正确率 {{ qStat.accuracy ?? '--' }}{{ qStat.accuracy != null ? '%' : '' }}</span>
              </div>
              <div v-if="qStat.distribution?.length" class="bar-list">
                <div v-for="item in qStat.distribution" :key="`${qStat.questionId}-${item.answer}`" class="bar-row">
                  <div class="bar-label">{{ answerLabel(item.answer, qStat.question) }}</div>
                  <div class="bar-track"><div class="bar-fill" :style="{ width: `${Math.max(item.percent || 0, item.count ? 6 : 0)}%` }"></div></div>
                  <div class="bar-value">{{ item.count }} 人</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-else class="panel empty-panel">
        <h3>等待教师发起测验</h3>
        <p>教师发布后，这里会自动出现可作答的随堂测验。</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { classQuizApi } from '@/api'

const quizzes = ref([])
const currentQuiz = ref(null)
const stats = ref(null)
const answers = reactive({})
const multiAnswers = reactive({})
const loading = ref(false)
const submitting = ref(false)
let refreshTimer = null
let tickTimer = null

const typeLabel = (type) => ({ SINGLE: '单选', MULTI: '多选', JUDGE: '判断', FILL: '填空', ESSAY: '简答' }[type] || type)
const typeTag = (type) => ({ SINGLE: 'primary', MULTI: 'success', JUDGE: 'warning', FILL: 'info', ESSAY: 'danger' }[type] || 'info')
const quizQuestions = computed(() => currentQuiz.value?.questions?.length ? currentQuiz.value.questions : (currentQuiz.value?.question ? [currentQuiz.value.question] : []))
const correctText = computed(() => {
  const map = currentQuiz.value?.myCorrectMap || {}
  const values = Object.values(map)
  if (!values.length) return '等待统计'
  return `答对 ${values.filter(v => Number(v) === 1).length} / ${values.length} 题`
})

const canSubmit = computed(() => {
  if (!currentQuiz.value || currentQuiz.value.submitted) return false
  return quizQuestions.value.length > 0 && quizQuestions.value.every(question => {
    if (question.questionType === 'MULTI') {
      return Array.isArray(multiAnswers[question.id]) && multiAnswers[question.id].length > 0
    }
    return String(answers[question.id] || '').trim()
  })
})

const loadActiveQuizzes = async () => {
  loading.value = true
  try {
    const res = await classQuizApi.studentActiveList()
    if (res.data.code === 200) {
      quizzes.value = res.data.data || []
      if (currentQuiz.value) {
        const latest = quizzes.value.find(item => item.id === currentQuiz.value.id)
        if (latest) currentQuiz.value = { ...currentQuiz.value, ...latest }
      } else if (quizzes.value.length) {
        selectQuiz(quizzes.value[0])
      }
    }
  } finally {
    loading.value = false
  }
}

const selectQuiz = async (quiz) => {
  const res = await classQuizApi.detail(quiz.id)
  if (res.data.code === 200) {
    currentQuiz.value = res.data.data
    hydrateAnswer()
    await refreshStats()
  }
}

const hydrateAnswer = () => {
  if (!currentQuiz.value) return
  Object.keys(answers).forEach(key => delete answers[key])
  Object.keys(multiAnswers).forEach(key => delete multiAnswers[key])
  const myAnswers = currentQuiz.value.myAnswers || {}
  quizQuestions.value.forEach(question => {
    if (question.questionType === 'MULTI') {
      multiAnswers[question.id] = myAnswers[question.id] ? String(myAnswers[question.id]).split(',').filter(Boolean) : []
    } else {
      answers[question.id] = myAnswers[question.id] || ''
    }
  })
}

const refreshStats = async () => {
  if (!currentQuiz.value?.id) return
  const res = await classQuizApi.stats(currentQuiz.value.id)
  if (res.data.code === 200) stats.value = res.data.data
}

const submitAnswer = async () => {
  if (!canSubmit.value) {
    ElMessage.warning('请先作答')
    return
  }
  submitting.value = true
  try {
    const payload = quizQuestions.value.map(question => ({
      questionId: question.id,
      answer: question.questionType === 'MULTI' ? (multiAnswers[question.id] || []) : (answers[question.id] || '')
    }))
    const res = await classQuizApi.submit(currentQuiz.value.id, { answers: payload })
    if (res.data.code === 200) {
      ElMessage.success('提交成功')
      currentQuiz.value = res.data.data
      hydrateAnswer()
      await refreshStats()
      await loadActiveQuizzes()
    }
  } finally {
    submitting.value = false
  }
}

const parseOptions = (question) => {
  if (!question) return []
  if (question.questionType === 'JUDGE') {
    return [{ label: '1', content: '正确' }, { label: '0', content: '错误' }]
  }
  try {
    const raw = typeof question.options === 'string' ? JSON.parse(question.options || '[]') : (question.options || [])
    return Array.isArray(raw)
      ? raw.map((item, index) => typeof item === 'string'
        ? { label: String.fromCharCode(65 + index), content: item }
        : { label: item.label || item.key || String.fromCharCode(65 + index), content: item.content || item.value || '' })
      : Object.entries(raw).map(([label, content]) => ({ label, content }))
  } catch (e) {
    return []
  }
}

const answerLabel = (value, targetQuestion) => {
  if (!value) return '--'
  const options = parseOptions(targetQuestion || currentQuiz.value?.question)
  return String(value).split(',').map(code => {
    const option = options.find(opt => opt.label === code)
    return option ? `${option.label}. ${option.content}` : code
  }).join(' + ')
}

const formatTime = (seconds) => {
  const sec = Math.max(0, Number(seconds || 0))
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const startTimers = () => {
  refreshTimer = setInterval(async () => {
    await loadActiveQuizzes()
    await refreshStats()
  }, 5000)
  tickTimer = setInterval(() => {
    quizzes.value = quizzes.value.map(item => ({ ...item, remainingSeconds: Math.max(0, Number(item.remainingSeconds || 0) - 1) }))
    if (currentQuiz.value) {
      currentQuiz.value = { ...currentQuiz.value, remainingSeconds: Math.max(0, Number(currentQuiz.value.remainingSeconds || 0) - 1) }
    }
  }, 1000)
}

onMounted(async () => {
  await loadActiveQuizzes()
  startTimers()
})

onBeforeUnmount(() => {
  clearInterval(refreshTimer)
  clearInterval(tickTimer)
})
</script>

<style scoped>
.quiz-page { display: flex; flex-direction: column; gap: 18px; }
.page-head { display: flex; align-items: center; justify-content: space-between; }
.eyebrow { margin: 0 0 4px; color: #7c8a63; font-size: 13px; font-weight: 600; }
.page-head h2 { margin: 0; color: var(--bio-ink); font-size: 25px; }
.layout { display: grid; grid-template-columns: 330px minmax(0, 1fr); gap: 18px; align-items: start; }
.panel { background: #fff; border: 1px solid #edf0e8; border-radius: 8px; box-shadow: 0 10px 28px rgba(42, 54, 34, .06); padding: 18px; }
.panel-title { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.panel-title h3 { margin: 0; color: #2d3327; font-size: 17px; }
.panel-title span { color: #8a927f; font-size: 12px; }
.quiz-list { display: flex; flex-direction: column; gap: 12px; }
.quiz-card { border: 1px solid #e8ecdf; border-radius: 8px; background: #fbfcf8; padding: 14px; cursor: pointer; transition: .18s; }
.quiz-card:hover, .quiz-card.active { border-color: var(--bio-primary); background: var(--bio-primary-soft); }
.quiz-card.submitted { background: #fff; }
.quiz-card-top { display: flex; align-items: center; justify-content: space-between; color: var(--bio-ink-muted); margin-bottom: 10px; }
.quiz-card strong { color: var(--bio-ink); display: block; line-height: 1.5; }
.quiz-card p { color: #8a927f; margin: 6px 0 0; font-size: 13px; }
.answer-panel { min-height: 560px; }
.answer-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 18px; }
.answer-head h3 { margin: 0; color: var(--bio-ink); font-size: 23px; }
.question-box { border: 1px solid var(--bio-border); background: var(--bio-primary-soft); border-radius: 8px; padding: 18px; }
.question-stack { display: flex; flex-direction: column; gap: 16px; }
.question-meta { display: flex; align-items: center; gap: 10px; color: var(--bio-ink-muted); font-size: 14px; margin-bottom: 12px; }
.question-index { color: var(--bio-primary); font-weight: 700; }
.question-box p { color: var(--bio-ink); font-size: 21px; line-height: 1.7; font-weight: 600; margin: 0 0 18px; }
.answer-area { background: #fff; border-radius: 8px; padding: 14px; border: 1px solid var(--bio-border); }
.option-group { display: flex; flex-direction: column; gap: 14px; align-items: flex-start; }
.option-item { font-size: 15px; white-space: normal; line-height: 1.6; }
.submit-bar { display: flex; justify-content: flex-end; align-items: center; gap: 12px; margin: 18px 0; }
.submitted-text { flex: 1; color: #606266; background: #f6faf2; border: 1px solid #e3ecd8; border-radius: 8px; padding: 12px; }
.stats-box { border-top: 1px solid #edf0e8; padding-top: 16px; }
.question-stats-list { display: flex; flex-direction: column; gap: 14px; }
.question-stat-card { border: 1px solid #edf0e8; border-radius: 8px; padding: 14px; background: #fbfcf8; }
.question-stat-title { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.question-stat-title strong { color: #2d3327; line-height: 1.5; }
.question-stat-title span { color: var(--bio-ink-muted); white-space: nowrap; font-size: 14px; }
.bar-list { display: flex; flex-direction: column; gap: 12px; }
.bar-row { display: grid; grid-template-columns: 180px minmax(0, 1fr) 70px; gap: 12px; align-items: center; }
.bar-label { color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bar-track { height: 16px; border-radius: 999px; background: #eef1e8; overflow: hidden; }
.bar-fill { height: 100%; border-radius: inherit; background: var(--bio-primary); transition: width .25s; }
.bar-value { text-align: right; color: #606266; font-size: 13px; }
.empty-panel { min-height: 360px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: var(--bio-ink-muted); text-align: center; }
.empty-panel h3 { margin: 0 0 8px; color: var(--bio-ink); }
@media (max-width: 980px) {
  .layout { grid-template-columns: 1fr; }
}
</style>
