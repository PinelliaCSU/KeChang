<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">🏠 教师仪表盘</h2>
    </div>

    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-icon" style="background:#e6f7ff">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#1890ff" stroke-width="2"
            stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
          </svg>
        </div>
        <div class="stat-num">{{ stats.courseCount || 0 }}</div>
        <div class="stat-label">📚 课程数</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#fff7e6">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#faad14" stroke-width="2"
            stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 20h9" />
            <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
          </svg>
        </div>
        <div class="stat-num">{{ stats.pendingAssignmentCount || 0 }}</div>
        <div class="stat-label">📝 待批改</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#f6ffed">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#52c41a" stroke-width="2"
            stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
        </div>
        <div class="stat-num">{{ stats.pendingQuestionCount || 0 }}</div>
        <div class="stat-label">❓ 待回复</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#f0f5ff">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#1890ff" stroke-width="2"
            stroke-linecap="round" stroke-linejoin="round">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
          </svg>
        </div>
        <div class="stat-num">{{ stats.avgScore || '--' }}</div>
        <div class="stat-label">📊 平均分</div>
      </div>
    </div>

    <div class="content-row">
      <div class="panel chart-panel">
        <h4 class="section-title">📈 近7日学生活跃度</h4>
        <div ref="chartRef" style="height:300px"></div>
      </div>
      <div class="panel todo-panel">
        <h4 class="section-title">📋 待办事项</h4>
        <el-timeline v-if="todos.length > 0">
          <el-timeline-item v-for="t in todos" :key="t" :timestamp="t.time" placement="top">
            {{ t.content }}
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无待办" :image-size="80" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { dashboardApi } from '@/api'
import * as echarts from 'echarts'

const stats = ref({})
const todos = ref([
  { time: '今天', content: '批改《Java高级》第3次作业' },
  { time: '昨天', content: '回复 张三 的问题' },
  { time: '2天前', content: '阅卷《软件测试》期中考试' }
])
const chartRef = ref(null)

onMounted(async () => {
  try {
    const res = await dashboardApi.teacherStats()
    if (res.data.code === 200) stats.value = res.data.data
  } catch (e) { /* ignore */ }

  await nextTick()
  if (chartRef.value) {
    const chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['7天前', '6天前', '5天前', '4天前', '3天前', '前天', '昨天'] },
      yAxis: { type: 'value' },
      series: [{
        data: [25, 32, 28, 45, 38, 50, 42],
        type: 'line', smooth: true,
        areaStyle: { color: 'rgba(24,144,255,0.2)' },
        itemStyle: { color: '#1890ff' }
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
  color: #303133;
  margin: 0;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-num {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.content-row {
  display: flex;
  gap: 20px;
}

.chart-panel {
  flex: 1;
}

.todo-panel {
  width: 360px;
  flex-shrink: 0;
}

.panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
}
</style>
