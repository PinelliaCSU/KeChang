<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div>
        <h2 class="page-title">我的主页</h2>
        <p class="page-subtitle">查看今日待办、课程进度和课堂积分</p>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-icon" style="background:#e8eddf">
          <el-icon :size="28" color="#20c4cc">
            <Reading />
          </el-icon>
        </div>
        <div class="stat-num">{{ stats.courseCount || 0 }}</div>
        <div class="stat-label">在学课程</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#f0ebe0">
          <el-icon :size="28" color="#b08968">
            <Edit />
          </el-icon>
        </div>
        <div class="stat-num">{{ stats.pendingAssignmentCount ?? '--' }}</div>
        <div class="stat-label">待做作业</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#f4e8e0">
          <el-icon :size="28" color="#c4665a">
            <Timer />
          </el-icon>
        </div>
        <div class="stat-num">{{ stats.upcomingExamCount ?? '--' }}</div>
        <div class="stat-label">待考试</div>
      </div>
      <div class="stat-card point-card" @click="pointsVisible = true">
        <div class="stat-icon" style="background:#dde8d6">
          <el-icon :size="28" color="#20c4cc">
            <TrendCharts />
          </el-icon>
        </div>
        <div class="stat-num">{{ pointsOverview.courseCount || 0 }}</div>
        <div class="stat-label">课堂积分</div>
      </div>
    </div>

    <div class="content-row">
      <div class="main-stack">
        <div class="panel chart-panel">
          <h4 class="section-title">学习动态 · 本周完成: {{ weeklyActivityTotal }} 项</h4>
          <div ref="chartRef" style="height:280px"></div>
        </div>

        <div class="panel progress-panel">
          <div class="section-head">
            <h4 class="section-title">我的课程进度</h4>
            <span class="section-hint">{{ courses.length }} 门课程</span>
          </div>
          <div v-if="courses.length" class="course-progress-grid">
            <div v-for="c in courses" :key="c.name" class="progress-card">
              <div class="progress-card-head">
                <span class="progress-label">{{ c.name }}</span>
                <strong>{{ c.progress }}%</strong>
              </div>
              <el-progress :percentage="c.progress" :color="c.color" :stroke-width="10" :show-text="false" />
            </div>
          </div>
          <el-empty v-else description="暂无课程进度" :image-size="80" />
        </div>

      </div>

      <div class="side-stack">
        <div class="panel deadline-panel">
          <h4 class="section-title">待办事项 ({{ todoTotal }})</h4>
          <div class="todo-list-scroll" v-if="todos.length > 0">
            <div v-for="(t, idx) in todos" :key="idx" class="todo-item" @click="handleTodoClick(t)">
              <span class="todo-dot" :class="'dot-' + t.type"></span>
              <div class="todo-body">
                <span class="todo-text">{{ t.title }}</span>
                <span class="todo-meta">{{ t.courseName }} · {{ t.deadline }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无待办" :image-size="80" />
        </div>

        <ShortcutPanel />
      </div>
    </div>

    <el-dialog v-model="pointsVisible" title="课堂积分详情" width="720px">
      <el-empty v-if="!pointCourses.length" description="暂无积分记录" />
      <el-collapse v-else class="point-course-list">
        <el-collapse-item v-for="course in pointCourses" :key="course.courseId" :name="course.courseId">
          <template #title>
            <div class="course-point-title">
              <span>{{ course.courseName }}</span>
              <el-tag :type="course.totalPoints >= 0 ? 'success' : 'danger'">{{ course.totalPoints }} 分</el-tag>
            </div>
          </template>
          <el-table :data="course.records || []" class="point-table">
            <el-table-column prop="eventTime" label="时间" width="180" />
            <el-table-column label="来源" min-width="180">
              <template #default="{ row }">
                <div class="point-source">{{ row.sourceType || '课堂互动' }}</div>
                <div class="point-reason">{{ row.reason || '-' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="积分" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.points >= 0 ? 'success' : 'danger'">{{ row.points > 0 ? '+' : '' }}{{ row.points || 0
                }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { dashboardApi, courseApi, participationApi, assignmentApi, examApi, trainingApi, messageApi, evaluationApi, surveyApi, announcementApi } from '@/api'
import ShortcutPanel from '@/components/DashBoard/ShortcutPanel.vue'
import * as echarts from 'echarts'

const router = useRouter()
const stats = ref({})
const courses = ref([])
const chartRef = ref(null)
const todos = ref([])
const pointsOverview = ref({ courseCount: 0, courses: [], records: [] })
const pointsVisible = ref(false)
const pointCourses = computed(() => Array.isArray(pointsOverview.value.courses) ? pointsOverview.value.courses : [])
const todoTotal = computed(() => todos.value.length)
const activeTrend = computed(() => Array.isArray(stats.value.activeTrend) ? stats.value.activeTrend : [])
const weeklyActivityTotal = computed(() => activeTrend.value.reduce((sum, item) => sum + Number(item.count || 0), 0))

const fmt = (d) => {
  if (!d) return '未设定'
  const s = String(d)
  return s.slice(0, 16).replace('T', ' ')
}
const isOverdue = (d) => d && new Date(d) < new Date()
const extractList = (res) => {
  if (!res || res.data?.code !== 200) return []
  const d = res.data.data
  return Array.isArray(d?.records) ? d.records : Array.isArray(d?.list) ? d.list : Array.isArray(d) ? d : []
}

const handleTodoClick = (t) => {
  if (t.route) router.push(t.route)
}

const fetchTodos = async () => {
  const items = []
  const now = new Date()

  // 1. 待提交作业
  try {
    const res = await assignmentApi.list({ page: 1, size: 50 })
    const list = extractList(res)
    list.forEach(a => {
      if (!a.submitted) {
        items.push({
          type: 'assignment',
          title: `提交作业：${a.title}`,
          courseName: a.courseName || '',
          deadline: fmt(a.deadline),
          overdue: isOverdue(a.deadline),
          route: '/student/assignments'
        })
      }
    })
  } catch { /* ignore */ }

  // 2. 待参加考试
  try {
    const res = await examApi.listExams({ page: 1, size: 50 })
    const list = extractList(res)
    list.forEach(e => {
      if (e.submitted !== 1 && e.startTime && new Date(e.startTime) <= now && e.endTime && new Date(e.endTime) >= now) {
        items.push({
          type: 'exam',
          title: `参加考试：${e.examName || e.title}`,
          courseName: e.courseName || '',
          deadline: fmt(e.endTime),
          overdue: false,
          route: '/student/exams'
        })
      }
    })
  } catch { /* ignore */ }

  // 3. 待提交实训报告
  try {
    const res = await trainingApi.listProjects({ page: 1, size: 50 })
    const list = extractList(res)
    list.forEach(t => {
      if (t.taskCount > 0 && (t.reportCount == null || t.reportCount < t.taskCount)) {
        items.push({
          type: 'training',
          title: `提交实训报告：${t.projectName || t.name || t.title}`,
          courseName: t.courseName || '',
          deadline: fmt(t.endDate || t.deadline),
          overdue: isOverdue(t.endDate || t.deadline),
          route: '/student/trainings'
        })
      }
    })
  } catch { /* ignore */ }

  // 4. 未读私信
  try {
    const convRes = await messageApi.conversations({ page: 1, size: 30 })
    if (convRes.data?.code === 200) {
      const convs = convRes.data.data?.records || convRes.data.data || []
      convs.forEach(c => {
        if (c.unreadCount > 0) {
          items.push({
            type: 'message',
            title: `查看 ${c.partnerName || c.name || '联系人'} 的私信`,
            courseName: c.courseName || '',
            deadline: c.lastTime ? fmt(c.lastTime) : '今天',
            overdue: false,
            route: '/student/messages'
          })
        }
      })
    }
  } catch { /* ignore */ }

  // 5. 待填写教学评价
  try {
    const res = await evaluationApi.list({ page: 1, size: 50 })
    const list = extractList(res)
    list.forEach(e => {
      if (!e.submitted) {
        items.push({
          type: 'evaluation',
          title: `填写教学评价：${e.evaluationName || e.courseName}`,
          courseName: e.courseName || '',
          deadline: fmt(e.endDate),
          overdue: isOverdue(e.endDate),
          route: '/student/evaluations'
        })
      }
    })
  } catch { /* ignore */ }

  // 6. 待填写问卷
  try {
    const res = await surveyApi.list({ page: 1, size: 50 })
    const list = extractList(res)
    list.forEach(s => {
      if (!s.submitted) {
        items.push({
          type: 'survey',
          title: `填写问卷：${s.title}`,
          courseName: s.courseName || '',
          deadline: fmt(s.endDate),
          overdue: isOverdue(s.endDate),
          route: '/student/surveys'
        })
      }
    })
  } catch { /* ignore */ }

  // 7. 待确认公告
  try {
    const res = await announcementApi.list({ page: 1, size: 50 })
    const list = extractList(res)
    list.forEach(a => {
      if (!a.confirmed) {
        items.push({
          type: 'announcement',
          title: `确认公告：${a.title}`,
          courseName: a.courseName || '',
          deadline: fmt(a.publishTime),
          overdue: false,
          route: `/student/courses/${a.courseId}?tab=announcements`
        })
      }
    })
  } catch { /* ignore */ }

  todos.value = items
}

const fetchData = async () => {
  try {
    const res = await dashboardApi.studentStats()
    if (res.data.code === 200) stats.value = res.data.data
  } catch { /* ignore */ }

  try {
    const pointRes = await participationApi.my()
    if (pointRes.data.code === 200) {
      pointsOverview.value = pointRes.data.data || { courseCount: 0, courses: [], records: [] }
    }
  } catch { /* ignore */ }

  try {
    const cRes = await courseApi.list({ page: 1, size: 20, onlyEnrolled: true })
    if (cRes.data.code === 200) {
      const d = cRes.data.data
      const list = Array.isArray(d?.records) ? d.records : Array.isArray(d) ? d : []
      courses.value = list.map((c, i) => ({
        id: c.id,
        name: c.courseName,
        progress: Math.max(0, Math.min(100, Math.round(Number(c.progress || 0)))),
        color: ['#20c4cc', '#64748b', '#f59e0b', '#8a94a6', '#ef476f'][i % 5]
      }))
    }
  } catch { /* ignore */ }
}

const onVisibilityChange = () => {
  if (!document.hidden) fetchTodos()
}

onMounted(async () => {
  await fetchData()
  await fetchTodos()
  document.addEventListener('visibilitychange', onVisibilityChange)

  await nextTick()
  if (chartRef.value) {
    const chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: activeTrend.value.map(item => String(item.date || '').slice(5)) },
      yAxis: { type: 'value', name: '完成项' },
      series: [{
        data: activeTrend.value.map(item => item.count || 0),
        type: 'bar', barWidth: '50%',
        itemStyle: { color: '#20c4cc', borderRadius: [4, 4, 0, 0] }
      }]
    })
  }
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
.page-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--bio-ink);
  margin: 0;
}

.page-subtitle {
  margin: 6px 0 0;
  color: var(--bio-ink-muted);
  font-size: 13px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 14px;
  box-shadow: var(--bio-shadow-sm);
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid rgba(32, 196, 204, 0.08);
  transition: box-shadow var(--bio-dur-slow) var(--bio-ease-out);
}

.stat-card:hover {
  box-shadow: var(--bio-shadow-md);
  transform: translateY(-1px);
}

.point-card {
  cursor: pointer;
}

.point-card:hover {
  transform: translateY(-1px);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--bio-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--bio-primary-soft) !important;
  color: var(--bio-primary) !important;
}

.stat-icon :deep(.el-icon) {
  color: var(--bio-primary) !important;
}

.stat-num {
  font-size: 28px;
  font-weight: bold;
  color: var(--bio-ink);
}

.stat-label {
  font-size: 13px;
  color: var(--bio-ink-muted);
  margin-top: 4px;
}

.content-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
}

.main-stack,
.side-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chart-panel {
  min-height: 330px;
}

.panel {
  background: rgba(255, 255, 255, 0.94);
  border-radius: 14px;
  box-shadow: var(--bio-shadow-sm);
  padding: 24px;
  border: 1px solid rgba(32, 196, 204, 0.08);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--bio-ink);
  margin: 0 0 16px 0;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-head .section-title {
  margin: 0;
}

.section-hint {
  color: var(--bio-ink-muted);
  font-size: 13px;
}

.course-progress-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.progress-card {
  padding: 16px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--bio-border);
}

.progress-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.progress-card-head strong {
  color: var(--bio-primary);
  font-size: 18px;
}

.progress-label {
  font-size: 13px;
  color: var(--bio-ink);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.point-table {
  width: 100%;
}

.point-course-list {
  --el-collapse-header-height: 52px;
}

.course-point-title {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 16px;
  font-weight: 700;
  color: var(--bio-ink);
}

.point-source {
  font-weight: 600;
  color: var(--bio-ink);
}

.point-reason {
  font-size: 12px;
  color: var(--bio-ink-muted);
  margin-top: 2px;
}

@media (max-width: 1024px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .content-row {
    grid-template-columns: 1fr;
  }
}

.todo-list-scroll {
  max-height: 340px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.todo-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--bio-radius-sm);
  cursor: pointer;
  transition: background var(--bio-dur-fast) var(--bio-ease-out);
}

.todo-item:hover {
  background: #f7f8f2;
}

.todo-item:active {
  transform: translateY(1px);
}

.todo-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.dot-assignment {
  background: #b08968;
}

.dot-exam {
  background: #c4665a;
}

.dot-training {
  background: #9c6644;
}

.dot-message {
  background: #5c7a8a;
}

.dot-evaluation {
  background: #8e6b8e;
}

.dot-survey {
  background: #6b8e6b;
}

.dot-announcement {
  background: #8e7a6b;
}

.todo-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.todo-text {
  font-size: 13px;
  color: var(--bio-ink);
  line-height: 1.4;
}

.todo-meta {
  font-size: 11px;
  color: var(--bio-ink-faint);
}

.shortcut-panel {
  padding-bottom: 18px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.shortcut-item {
  border: none;
  border-radius: 12px;
  background: #f5f6ef;
  color: var(--bio-ink-soft);
  padding: 14px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all var(--bio-dur-fast) var(--bio-ease-out);
}

.shortcut-item:hover {
  color: var(--bio-primary);
  background: var(--bio-primary-soft);
  transform: translateY(-1px);
}

@media (max-width: 1280px) {
  .course-progress-grid {
    grid-template-columns: 1fr;
  }
}
</style>
