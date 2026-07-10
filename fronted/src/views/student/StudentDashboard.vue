<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">我的仪表盘</h2>
    </div>

    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-icon" style="background:#e8eddf">
          <el-icon :size="28" color="#7f8c52">
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
          <el-icon :size="28" color="#6b8e4e">
            <TrendCharts />
          </el-icon>
        </div>
        <div class="stat-num">{{ pointsOverview.courseCount || 0 }}</div>
        <div class="stat-label">课堂积分</div>
      </div>
    </div>

    <div class="content-row">
      <div class="panel chart-panel">
        <h4 class="section-title">学习日历 · 本周学习: 12.5h</h4>
        <div ref="chartRef" style="height:280px"></div>
      </div>
      <div class="panel deadline-panel">
        <h4 class="section-title">即将截止</h4>
        <el-timeline>
          <el-timeline-item timestamp="剩余1天" color="#f5222d">《Java高级》第3次作业</el-timeline-item>
          <el-timeline-item timestamp="剩余3天" color="#faad14">《软件测试》期中考试</el-timeline-item>
          <el-timeline-item timestamp="剩余2天" color="#1890ff">《数据库》实训报告</el-timeline-item>
        </el-timeline>
      </div>
    </div>

    <div class="panel">
      <h4 class="section-title">我的课程进度</h4>
      <div v-for="c in courses" :key="c.name" class="progress-item">
        <span class="progress-label">{{ c.name }}</span>
        <el-progress :percentage="c.progress" :color="c.color" :stroke-width="14" />
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
                <el-tag :type="row.points >= 0 ? 'success' : 'danger'">{{ row.points > 0 ? '+' : '' }}{{ row.points || 0 }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, nextTick } from 'vue'
import { dashboardApi, courseApi, participationApi } from '@/api'
import * as echarts from 'echarts'

const stats = ref({})
const courses = ref([])
const chartRef = ref(null)
const pointsOverview = ref({ courseCount: 0, courses: [], records: [] })
const pointsVisible = ref(false)
const pointCourses = computed(() => Array.isArray(pointsOverview.value.courses) ? pointsOverview.value.courses : [])

const fetchData = async () => {
  try {
    const res = await dashboardApi.studentStats()
    if (res.data.code === 200) stats.value = res.data.data
  } catch (e) { /* ignore */ }

  try {
    const pointRes = await participationApi.my()
    if (pointRes.data.code === 200) {
      pointsOverview.value = pointRes.data.data || { courseCount: 0, courses: [], records: [] }
    }
  } catch (e) { /* ignore */ }

  try {
    const cRes = await courseApi.list({ page: 1, size: 20, onlyEnrolled: true })
    if (cRes.data.code === 200) {
      const d = cRes.data.data
      const list = Array.isArray(d?.records) ? d.records : Array.isArray(d) ? d : []
      courses.value = list.map((c, i) => ({
        id: c.id,
        name: c.courseName,
        progress: Math.floor(Math.random() * 60 + 30),
        color: ['#7f8c52', '#6b8e4e', '#b08968', '#5c7a8a', '#9c6644'][i % 5]
      }))
    }
  } catch (e) { /* ignore */ }
}

onMounted(async () => {
  await fetchData()
  await nextTick()
  if (chartRef.value) {
    const chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
      yAxis: { type: 'value', name: '小时' },
      series: [{
        data: [2.5, 3.0, 1.5, 2.0, 2.5, 1.0, 0.5],
        type: 'bar', barWidth: '50%',
        itemStyle: { color: '#7f8c52', borderRadius: [4, 4, 0, 0] }
      }]
    })
  }
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

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: var(--bio-paper);
  border-radius: var(--bio-radius-md);
  box-shadow: var(--bio-shadow-sm);
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid rgba(163, 177, 138, 0.08);
  transition: box-shadow var(--bio-dur-slow) var(--bio-ease-out);
}

.stat-card:hover {
  box-shadow: var(--bio-shadow-md);
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
  display: flex;
  gap: 20px;
}

.chart-panel {
  flex: 1;
}

.deadline-panel {
  width: 360px;
  flex-shrink: 0;
}

.panel {
  background: var(--bio-paper);
  border-radius: var(--bio-radius-md);
  box-shadow: var(--bio-shadow-sm);
  padding: 24px;
  border: 1px solid rgba(163, 177, 138, 0.08);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--bio-ink);
  margin: 0 0 16px 0;
}

.progress-item {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
}

.progress-label {
  width: 140px;
  font-size: 13px;
  color: var(--bio-ink-soft);
  flex-shrink: 0;
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
    flex-direction: column;
  }

  .deadline-panel {
    width: 100%;
  }
}
</style>
