<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div>
        <h2 class="page-title">教师主页</h2>
        <p class="page-subtitle">聚合作业批改、课堂互动、考试阅卷和最近通知</p>
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
        <div class="stat-label">课程数</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#f0ebe0">
          <el-icon :size="28" color="#b08968">
            <Edit />
          </el-icon>
        </div>
        <div class="stat-num">{{ stats.pendingAssignmentCount || 0 }}</div>
        <div class="stat-label">待批改</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#dce8f0">
          <el-icon :size="28" color="#5c7a8a">
            <ChatDotRound />
          </el-icon>
        </div>
        <div class="stat-num">{{ stats.pendingQuestionCount || 0 }}</div>
        <div class="stat-label">待回复</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#dde8d6">
          <el-icon :size="28" color="#20c4cc">
            <TrendCharts />
          </el-icon>
        </div>
        <div class="stat-num">{{ todos.length || 0 }}</div>
        <div class="stat-label">今日关注</div>
      </div>
    </div>

    <div class="content-row">
      <div class="panel chart-panel">
        <h4 class="section-title">近7日学生活跃度</h4>
        <div class="chart-center">
          <div ref="chartRef" class="active-chart"></div>
        </div>
      </div>
      <div class="side-stack">
        <div class="panel todo-panel">
          <h4 class="section-title">待办事项 ({{ todoTotal }})</h4>
          <div class="todo-list-scroll" v-if="todos.length > 0">
            <div v-for="(t, idx) in todos" :key="idx" class="todo-item" @click="handleTodoClick(t)">
              <span class="todo-dot" :class="'dot-' + t.type"></span>
              <div class="todo-body">
                <span class="todo-text">{{ t.content }}</span>
                <span class="todo-meta">{{ t.courseName }} · {{ t.time }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无待办" :image-size="80" />
        </div>

        <ShortcutPanel />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { dashboardApi, assignmentApi, examApi, messageApi, qaApi } from '@/api'
import ShortcutPanel from '@/components/DashBoard/ShortcutPanel.vue'
import * as echarts from 'echarts'

const router = useRouter()
const stats = ref({})
const todos = ref([])
const chartRef = ref(null)

const todoTotal = computed(() => todos.value.length)

const formatTime = (t) => {
  if (!t) return '--'
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  const days = Math.floor(diff / 86400000)
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return `${Math.floor(days / 7)}周前`
}

const handleTodoClick = (t) => {
  if (t.route) router.push(t.route)
}

const fetchTodos = async () => {
  const items = []

  // 1. 待批改作业 — 查询有提交但未评分的作业
  try {
    const assignRes = await assignmentApi.list({ page: 1, size: 100 })
    if (assignRes.data.code === 200) {
      const assigns = assignRes.data.data?.records || []
      for (const a of assigns.slice(0, 10)) {
        try {
          const subRes = await assignmentApi.getSubmitList(a.id, { page: 1, size: 100 })
          if (subRes.data.code === 200) {
            const submits = subRes.data.data?.records || []
            const pending = submits.filter(s => s.score == null && s.status !== '已批改')
            pending.forEach(s => {
              items.push({
                type: 'grade',
                content: `批改《${a.title}》— ${s.studentName || '学生'}的作业`,
                courseName: a.courseName || '',
                time: s.submitTime ? formatTime(s.submitTime) : '今天',
                route: '/teacher/assignments'
              })
            })
          }
        } catch { /* ignore */ }
      }
    }
  } catch { /* ignore */ }

  // 2. 待阅卷考试 — 查询有记录但未评分的考试
  try {
    const examRes = await examApi.listExams({ page: 1, size: 50 })
    if (examRes.data.code === 200) {
      const exams = examRes.data.data?.records || []
      for (const e of exams.slice(0, 5)) {
        try {
          const recRes = await examApi.getRecords(e.id)
          if (recRes.data.code === 200) {
            const records = recRes.data.data || []
            const pending = records.filter(r => r.totalScore == null)
            pending.forEach(r => {
              items.push({
                type: 'exam',
                content: `阅卷《${e.examName}》— ${r.studentName || '学生'}`,
                courseName: e.courseName || '',
                time: r.submitTime ? formatTime(r.submitTime) : '今天',
                route: '/teacher/exams'
              })
            })
          }
        } catch { /* ignore */ }
      }
    }
  } catch { /* ignore */ }

  // 3. 待回复私信 — 有未读消息的会话需要回复
  try {
    const convRes = await messageApi.conversations({ page: 1, size: 50 })
    if (convRes.data.code === 200) {
      const convs = convRes.data.data?.records || convRes.data.data || []
      convs.forEach(c => {
        if (c.unreadCount > 0) {
          items.push({
            type: 'message',
            content: `回复 ${c.partnerName || c.name || c.username || '学生'} 的私信`,
            courseName: c.courseName || '',
            time: c.lastTime ? formatTime(c.lastTime) : '今天',
            route: '/teacher/messages'
          })
        }
      })
    }
  } catch { /* ignore */ }

  // 4. 待回复问答 — 查找未回复的提问帖子
  try {
    const postRes = await qaApi.listPosts({ page: 1, size: 20, replyStatus: 'unanswered' })
    if (postRes.data.code === 200) {
      const posts = postRes.data.data?.records || []
      posts.forEach(p => {
        items.push({
          type: 'qa',
          content: `回答「${p.title}」的问题`,
          courseName: p.courseName || '',
          time: p.createTime ? formatTime(p.createTime) : '今天',
          route: '/teacher/posts'
        })
      })
    }
  } catch { /* ignore */ }

  // 按时间排序，最近的在前
  items.sort((a, b) => {
    const order = { '今天': 0, '昨天': 1, '2天前': 2, '3天前': 3 }
    return (order[a.time] ?? 99) - (order[b.time] ?? 99)
  })

  todos.value = items
}

const refreshTodos = () => { fetchTodos() }

onMounted(async () => {
  try {
    const res = await dashboardApi.teacherStats()
    if (res.data.code === 200) stats.value = res.data.data
  } catch (e) { /* ignore */ }

  await fetchTodos()

  // 页面重新可见时刷新待办（处理完待办返回后自动更新）
  document.addEventListener('visibilitychange', onVisibilityChange)

  await nextTick()
  if (chartRef.value) {
    const chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: (stats.value.activeTrend || []).map(item => String(item.date || '').slice(5)) },
      yAxis: { type: 'value' },
      series: [{
        data: (stats.value.activeTrend || []).map(item => item.count || 0),
        type: 'line', smooth: true,
        areaStyle: { color: 'rgba(32,196,204,0.16)' },
        itemStyle: { color: '#20c4cc' }
      }]
    })
  }
})

const onVisibilityChange = () => {
  if (!document.hidden) refreshTodos()
}

// 组件卸载时移除监听
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
  grid-template-columns: minmax(0, 1fr) 370px;
  gap: 20px;
}

.chart-panel {
  min-height: 430px;
  display: flex;
  flex-direction: column;
}

.chart-center {
  flex: 1;
  min-height: 330px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.active-chart {
  width: min(100%, 860px);
  height: 300px;
}

.side-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.dot-grade {
  background: #b08968;
}

.dot-exam {
  background: #c4665a;
}

.dot-message {
  background: #5c7a8a;
}

.dot-qa {
  background: var(--bio-primary);
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

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
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
</style>
