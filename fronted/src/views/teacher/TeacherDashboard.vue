<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">教师仪表盘</h2>
    </div>

    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-icon" style="background:#e8eddf">
          <el-icon :size="28" color="#7f8c52">
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
          <el-icon :size="28" color="#6b8e4e">
            <TrendCharts />
          </el-icon>
        </div>
        <div class="stat-num">{{ stats.avgScore || '--' }}</div>
        <div class="stat-label">平均分</div>
      </div>
    </div>

    <div class="content-row">
      <div class="panel chart-panel">
        <h4 class="section-title">近7日学生活跃度</h4>
        <div ref="chartRef" style="height:300px"></div>
      </div>
      <div class="panel todo-panel">
        <h4 class="section-title">待办事项</h4>
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
        areaStyle: { color: 'rgba(127,140,82,0.2)' },
        itemStyle: { color: '#7f8c52' }
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

.todo-panel {
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

@media (max-width: 1024px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .content-row {
    flex-direction: column;
  }

  .todo-panel {
    width: 100%;
  }
}
</style>
