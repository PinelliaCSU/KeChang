<template>
  <div class="quiz-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">课堂互动</p>
        <h2>随堂测验</h2>
      </div>
      <el-button type="primary" @click="startQuiz" :loading="creating">发起测验</el-button>
    </div>

    <div class="layout">
      <section class="left-col">
        <div class="panel launch-panel">
          <div class="panel-title">
            <h3>选择题目</h3>
            <span>已选 {{ selectedQuestionIds.length }} 题</span>
          </div>
          <el-form label-position="top">
            <el-form-item label="课程">
              <el-select v-model="form.courseId" placeholder="选择课程" filterable style="width:100%" @change="onCourseChange">
                <el-option v-for="course in courses" :key="course.id" :label="course.courseName" :value="course.id" />
              </el-select>
            </el-form-item>
            <div class="form-row">
              <el-form-item label="题型">
                <el-select v-model="questionType" placeholder="全部" clearable style="width:100%" @change="loadQuestions">
                  <el-option label="单选" value="SINGLE" />
                  <el-option label="多选" value="MULTI" />
                  <el-option label="判断" value="JUDGE" />
                  <el-option label="填空" value="FILL" />
                  <el-option label="简答" value="ESSAY" />
                </el-select>
              </el-form-item>
              <el-form-item label="限时">
                <el-select v-model="form.durationSeconds" style="width:100%">
                  <el-option label="30 秒" :value="30" />
                  <el-option label="1 分钟" :value="60" />
                  <el-option label="2 分钟" :value="120" />
                  <el-option label="3 分钟" :value="180" />
                  <el-option label="5 分钟" :value="300" />
                </el-select>
              </el-form-item>
            </div>
            <el-form-item label="标题">
              <el-input v-model="form.title" placeholder="可不填，默认使用题干" />
            </el-form-item>
          </el-form>

          <div class="question-list" v-loading="questionLoading">
            <div
              v-for="q in questions"
              :key="q.id"
              class="question-card"
              :class="{ selected: selectedQuestionIds.includes(q.id) }"
              @click="toggleQuestion(q)"
            >
              <div class="question-meta">
                <el-tag size="small" :type="typeTag(q.questionType)">{{ typeLabel(q.questionType) }}</el-tag>
                <span>{{ q.score || 0 }} 分</span>
                <span>难度 {{ q.difficulty || '-' }}</span>
                <span v-if="isQuestionSelected(q.id)" class="selected-order">第 {{ selectedOrder(q.id) }} 题</span>
              </div>
              <p>{{ q.content }}</p>
              <div v-if="isQuestionSelected(q.id)" class="selected-mark">已选</div>
            </div>
            <el-empty v-if="!questionLoading && questions.length === 0" description="暂无题目" :image-size="80" />
          </div>
        </div>

        <div class="panel">
          <div class="panel-title compact">
            <h3>测验记录</h3>
            <el-button size="small" text type="primary" @click="loadQuizzes">刷新</el-button>
          </div>
          <div class="quiz-list">
            <div
              v-for="quiz in quizzes"
              :key="quiz.id"
              class="quiz-row"
              :class="{ active: selectedQuiz?.id === quiz.id }"
              @click="selectQuiz(quiz)"
            >
              <div>
                <strong>{{ quiz.title }}</strong>
                <span>{{ quiz.courseName || '未命名课程' }}</span>
              </div>
              <el-tag size="small" :type="quiz.status === 'ACTIVE' ? 'success' : 'info'">{{ quiz.status === 'ACTIVE' ? '进行中' : '已结束' }}</el-tag>
            </div>
            <el-empty v-if="quizzes.length === 0" description="还没有随堂测验" :image-size="80" />
          </div>
        </div>
      </section>

      <section class="panel screen-panel" v-if="selectedQuiz">
        <div class="screen-top">
          <div>
            <p class="eyebrow">{{ selectedQuiz.courseName }}</p>
            <h2>{{ selectedQuiz.title }}</h2>
          </div>
          <div class="screen-actions">
            <el-tag :type="selectedQuiz.status === 'ACTIVE' ? 'success' : 'info'" size="large">{{ selectedQuiz.status === 'ACTIVE' ? '进行中' : '已结束' }}</el-tag>
            <el-button v-if="selectedQuiz.status === 'ACTIVE'" type="danger" plain @click="closeQuiz">结束测验</el-button>
          </div>
        </div>

        <div class="question-box" v-if="selectedQuiz.questions?.length">
          <div class="question-line">
            <el-tag type="success">{{ selectedQuiz.questions.length }} 题</el-tag>
            <span>本轮随堂测验题目</span>
          </div>
          <div class="screen-question-list">
            <div v-for="(q, index) in selectedQuiz.questions" :key="q.id" class="screen-question-item">
              <strong>{{ index + 1 }}.</strong>
              <span>{{ q.content }}</span>
              <el-tag size="small" :type="typeTag(q.questionType)">{{ typeLabel(q.questionType) }}</el-tag>
            </div>
          </div>
        </div>

        <div class="metric-grid">
          <div class="metric-card">
            <span>剩余时间</span>
            <strong>{{ formatTime(stats?.remainingSeconds || 0) }}</strong>
          </div>
          <div class="metric-card">
            <span>已答 / 应答</span>
            <strong>{{ stats?.submittedCount || 0 }} / {{ stats?.totalStudents || 0 }}</strong>
          </div>
          <div class="metric-card">
            <span>参与率</span>
            <strong>{{ participation }}%</strong>
          </div>
          <div class="metric-card">
            <span>正确率</span>
            <strong>{{ stats?.accuracy ?? '--' }}{{ stats?.accuracy != null ? '%' : '' }}</strong>
          </div>
        </div>

        <div class="distribution">
          <div class="panel-title compact">
            <h3>实时答题分布</h3>
            <span>每 2 秒自动刷新</span>
          </div>
          <div v-if="questionStats.length" class="question-stats-list">
            <div v-for="(qStat, index) in questionStats" :key="qStat.questionId" class="question-stat-card">
              <div class="question-stat-title">
                <strong>{{ index + 1 }}. {{ qStat.question?.content }}</strong>
                <span>正确率 {{ qStat.accuracy ?? '--' }}{{ qStat.accuracy != null ? '%' : '' }}</span>
              </div>
              <div v-if="qStat.distribution?.length" class="bar-list">
                <div v-for="item in qStat.distribution" :key="`${qStat.questionId}-${item.answer}`" class="bar-row">
                  <div class="bar-label">{{ answerLabel(item.answer, qStat.question) }}</div>
                  <div class="bar-track">
                    <div class="bar-fill" :style="{ width: `${Math.max(item.percent || 0, item.count ? 6 : 0)}%` }"></div>
                  </div>
                  <div class="bar-value">{{ item.count }} 人 · {{ item.percent }}%</div>
                </div>
              </div>
              <el-empty v-else description="本题等待作答" :image-size="60" />
            </div>
          </div>
          <el-empty v-else description="等待学生作答" :image-size="90" />
        </div>
      </section>

      <section class="panel empty-screen" v-else>
        <h3>选择或发起一个测验</h3>
        <p>这里会显示实时答题人数、正确率和选项分布，投屏时也能直接使用。</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { classQuizApi, courseApi, examApi } from '@/api'

const courses = ref([])
const questions = ref([])
const quizzes = ref([])
const selectedQuiz = ref(null)
const stats = ref(null)
const questionLoading = ref(false)
const creating = ref(false)
const questionType = ref('')
const selectedQuestionIds = ref([])
let pollTimer = null

const form = reactive({
  courseId: null,
  title: '',
  durationSeconds: 120
})

const questionStats = computed(() => stats.value?.questionStats || [])
const participation = computed(() => {
  const total = Number(stats.value?.totalStudents || 0)
  const submitted = Number(stats.value?.submittedCount || 0)
  return total ? Math.round(submitted * 1000 / total) / 10 : 0
})

const typeLabel = (type) => ({ SINGLE: '单选', MULTI: '多选', JUDGE: '判断', FILL: '填空', ESSAY: '简答' }[type] || type)
const typeTag = (type) => ({ SINGLE: 'primary', MULTI: 'success', JUDGE: 'warning', FILL: 'info', ESSAY: 'danger' }[type] || 'info')

const loadCourses = async () => {
  const res = await courseApi.list({ page: 1, size: 100 })
  if (res.data.code === 200) {
    courses.value = res.data.data?.records || []
    if (!form.courseId && courses.value.length) {
      form.courseId = courses.value[0].id
      await loadQuestions()
    }
  }
}

const loadQuestions = async () => {
  if (!form.courseId) return
  questionLoading.value = true
  try {
    const res = await examApi.listQuestions({
      page: 1,
      size: 50,
      courseId: form.courseId,
      questionType: questionType.value || undefined
    })
    if (res.data.code === 200) questions.value = res.data.data?.records || []
  } finally {
    questionLoading.value = false
  }
}

const loadQuizzes = async () => {
  const res = await classQuizApi.teacherList({ courseId: form.courseId || undefined })
  if (res.data.code === 200) quizzes.value = res.data.data || []
}

const onCourseChange = async () => {
  selectedQuestionIds.value = []
  await loadQuestions()
  await loadQuizzes()
}

const toggleQuestion = (question) => {
  if (selectedQuestionIds.value.includes(question.id)) {
    selectedQuestionIds.value = selectedQuestionIds.value.filter(id => id !== question.id)
  } else {
    selectedQuestionIds.value = [...selectedQuestionIds.value, question.id]
  }
  if (!form.title && selectedQuestionIds.value.length === 1) form.title = question.content.slice(0, 40)
  if (selectedQuestionIds.value.length > 1 && (!form.title || form.title === question.content.slice(0, 40))) {
    form.title = `随堂测验（${selectedQuestionIds.value.length}题）`
  }
}

const isQuestionSelected = (questionId) => selectedQuestionIds.value.includes(questionId)
const selectedOrder = (questionId) => selectedQuestionIds.value.indexOf(questionId) + 1

const startQuiz = async () => {
  if (!form.courseId || selectedQuestionIds.value.length === 0) {
    ElMessage.warning('请选择课程和至少一道题目')
    return
  }
  creating.value = true
  try {
    const res = await classQuizApi.create({
      courseId: form.courseId,
      questionIds: selectedQuestionIds.value,
      title: form.title,
      durationSeconds: form.durationSeconds
    })
    if (res.data.code === 200) {
      ElMessage.success('随堂测验已发起')
      selectedQuiz.value = res.data.data
      stats.value = res.data.data
      form.title = ''
      selectedQuestionIds.value = []
      await loadQuizzes()
      startPolling()
    }
  } finally {
    creating.value = false
  }
}

const selectQuiz = async (quiz) => {
  const res = await classQuizApi.detail(quiz.id)
  if (res.data.code === 200) {
    selectedQuiz.value = res.data.data
    await refreshStats()
    startPolling()
  }
}

const refreshStats = async () => {
  if (!selectedQuiz.value?.id) return
  const res = await classQuizApi.stats(selectedQuiz.value.id)
  if (res.data.code === 200) {
    stats.value = res.data.data
    selectedQuiz.value = { ...selectedQuiz.value, ...res.data.data }
  }
}

const startPolling = () => {
  clearInterval(pollTimer)
  refreshStats()
  pollTimer = setInterval(refreshStats, 2000)
}

const closeQuiz = async () => {
  if (!selectedQuiz.value?.id) return
  await classQuizApi.close(selectedQuiz.value.id)
  ElMessage.success('测验已结束')
  await refreshStats()
  await loadQuizzes()
}

const formatTime = (seconds) => {
  const sec = Math.max(0, Number(seconds || 0))
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
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

const answerLabel = (answer, targetQuestion) => {
  const question = targetQuestion || selectedQuiz.value?.question
  const options = parseOptions(question)
  if (!answer || !options.length) return answer || '未作答'
  return String(answer).split(',').map(code => {
    const option = options.find(opt => opt.label === code)
    return option ? `${option.label}. ${option.content}` : code
  }).join(' + ')
}

onMounted(async () => {
  await loadCourses()
  await loadQuizzes()
})

onBeforeUnmount(() => clearInterval(pollTimer))
</script>

<style scoped>
.quiz-page { display: flex; flex-direction: column; gap: 18px; }
.page-head { display: flex; align-items: center; justify-content: space-between; }
.eyebrow { margin: 0 0 4px; color: var(--bio-primary); font-size: 13px; font-weight: 600; }
.page-head h2, .screen-top h2 { margin: 0; color: var(--bio-ink); font-size: 25px; }
.layout { display: grid; grid-template-columns: 420px minmax(0, 1fr); gap: 18px; align-items: start; }
.left-col { display: flex; flex-direction: column; gap: 18px; min-width: 0; }
.panel { background: #fff; border: 1px solid var(--bio-border); border-radius: 8px; box-shadow: none; padding: 18px; }
.panel-title { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.panel-title h3 { margin: 0; color: var(--bio-ink); font-size: 17px; }
.panel-title span { color: var(--bio-ink-muted); font-size: 12px; }
.panel-title.compact { align-items: center; margin-bottom: 12px; }
.form-row { display: grid; grid-template-columns: 1fr 130px; gap: 12px; }
.question-list { max-height: 520px; overflow: auto; display: flex; flex-direction: column; gap: 10px; padding-right: 4px; }
.question-card { border: 1px solid var(--bio-border); border-radius: 8px; padding: 12px; cursor: pointer; transition: .18s; background: #fff; position: relative; overflow: visible; }
.question-card:hover { border-color: var(--bio-primary); background: var(--bio-primary-soft); }
.question-card.selected { border-color: #6b8e4e; background: linear-gradient(135deg, rgba(107, 142, 78, .16), rgba(163, 177, 138, .08)); box-shadow: inset 4px 0 0 #6b8e4e, 0 8px 18px rgba(107, 142, 78, .12); }
.question-card.selected::after { content: ''; position: absolute; right: 8px; top: 8px; width: 42px; height: 42px; border-radius: 50%; background: rgba(107, 142, 78, .12); pointer-events: none; }
.question-card p { margin: 8px 0 0; color: var(--bio-ink); line-height: 1.6; white-space: normal; overflow-wrap: anywhere; word-break: break-word; padding-right: 4px; }
.question-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; padding-right: 58px; color: var(--bio-ink-muted); font-size: 12px; }
.selected-order { color: #3f6b30; font-weight: 700; background: rgba(107, 142, 78, .13); padding: 2px 7px; border-radius: 999px; }
.selected-mark { position: absolute; right: 12px; top: 12px; color: #3f6b30; font-weight: 700; font-size: 12px; background: #f1f7ea; border: 1px solid rgba(107, 142, 78, .28); border-radius: 999px; padding: 3px 9px; z-index: 1; }
.quiz-list { display: flex; flex-direction: column; gap: 8px; max-height: 260px; overflow: auto; }
.quiz-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px; border-radius: 8px; background: #f7f8fa; border: 1px solid transparent; cursor: pointer; }
.quiz-row.active { border-color: var(--bio-primary); background: var(--bio-primary-soft); }
.quiz-row strong, .quiz-row span { display: block; }
.quiz-row strong { color: var(--bio-ink); margin-bottom: 4px; }
.quiz-row span { color: var(--bio-ink-muted); font-size: 12px; }
.screen-panel { min-height: 680px; }
.screen-top { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.screen-actions { display: flex; align-items: center; gap: 10px; }
.question-box { background: var(--bio-primary-soft); border: 1px solid var(--bio-border); border-radius: 8px; padding: 18px; margin-bottom: 18px; }
.question-line { display: flex; align-items: center; gap: 10px; color: var(--bio-ink-muted); font-size: 14px; margin-bottom: 10px; }
.question-box p { margin: 0; color: var(--bio-ink); font-size: 21px; line-height: 1.7; font-weight: 600; }
.screen-question-list { display: flex; flex-direction: column; gap: 10px; }
.screen-question-item { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: 10px; color: var(--bio-ink); line-height: 1.6; }
.screen-question-item strong { color: var(--bio-primary); }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 18px; }
.metric-card { border-radius: 8px; background: #f7f8fa; border: 1px solid var(--bio-border); padding: 16px; }
.metric-card span { color: var(--bio-ink-muted); font-size: 13px; }
.metric-card strong { display: block; margin-top: 8px; color: var(--bio-primary); font-size: 28px; }
.distribution { border-top: 1px solid var(--bio-border); padding-top: 16px; }
.question-stats-list { display: flex; flex-direction: column; gap: 16px; }
.question-stat-card { border: 1px solid var(--bio-border); border-radius: 8px; padding: 14px; background: #fff; }
.question-stat-title { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 12px; }
.question-stat-title strong { color: var(--bio-ink); line-height: 1.5; }
.question-stat-title span { color: var(--bio-ink-muted); font-size: 14px; white-space: nowrap; }
.bar-list { display: flex; flex-direction: column; gap: 14px; }
.bar-row { display: grid; grid-template-columns: 180px minmax(0, 1fr) 110px; gap: 12px; align-items: center; }
.bar-label { color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bar-track { height: 18px; background: #e9eef3; border-radius: 999px; overflow: hidden; }
.bar-fill { height: 100%; background: var(--bio-primary); border-radius: inherit; transition: width .25s; }
.bar-value { color: #606266; font-size: 13px; text-align: right; }
.empty-screen { min-height: 420px; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; color: var(--bio-ink-muted); }
.empty-screen h3 { color: var(--bio-ink); margin: 0 0 8px; }
@media (max-width: 1180px) {
  .layout { grid-template-columns: 1fr; }
  .metric-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
