<template>
  <div class="teacher-dashboard">
    <h2>🏠 教师仪表盘</h2>
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background:#e6f7ff"><el-icon :size="28" color="#1890ff"><Reading /></el-icon></div>
          <div class="stat-num">{{ stats.courseCount || 0 }}</div>
          <div class="stat-label">📚 课程数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background:#fff7e6"><el-icon :size="28" color="#faad14"><Notebook /></el-icon></div>
          <div class="stat-num">{{ stats.pendingAssignmentCount || 0 }}</div>
          <div class="stat-label">📝 待批改</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background:#f6ffed"><el-icon :size="28" color="#52c41a"><ChatDotRound /></el-icon></div>
          <div class="stat-num">{{ stats.pendingQuestionCount || 0 }}</div>
          <div class="stat-label">❓ 待回复</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background:#f0f5ff"><el-icon :size="28" color="#1890ff"><TrendCharts /></el-icon></div>
          <div class="stat-num">{{ stats.avgScore || '--' }}</div>
          <div class="stat-label">📊 平均分</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>📈 近7日学生活跃度</template>
          <div ref="chartRef" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>📋 待办事项</template>
          <el-timeline>
            <el-timeline-item v-for="t in todos" :key="t" :timestamp="t.time" placement="top">
              {{ t.content }}
            </el-timeline-item>
          </el-timeline>
          <el-empty v-if="todos.length === 0" description="暂无待办" />
        </el-card>
      </el-col>
    </el-row>
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
      xAxis: { type: 'category', data: ['7天前','6天前','5天前','4天前','3天前','前天','昨天'] },
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
.teacher-dashboard h2 { margin-bottom: 20px; color: #303133; }
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.stat-num { font-size: 28px; font-weight: bold; color: #303133; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
</style>
