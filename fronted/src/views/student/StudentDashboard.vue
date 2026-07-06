<template>
  <div class="student-dashboard">
    <h2>🏠 我的仪表盘</h2>
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background:#e6f7ff"><el-icon :size="28" color="#1890ff"><Reading /></el-icon></div>
          <div class="stat-num">{{ stats.courseCount || 0 }}</div>
          <div class="stat-label">📚 在学课程</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background:#fff7e6"><el-icon :size="28" color="#faad14"><Notebook /></el-icon></div>
          <div class="stat-num">--</div>
          <div class="stat-label">📝 待做作业</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background:#fff1f0"><el-icon :size="28" color="#f5222d"><Timer /></el-icon></div>
          <div class="stat-num">--</div>
          <div class="stat-label">📏 待考试</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background:#f6ffed"><el-icon :size="28" color="#52c41a"><TrendCharts /></el-icon></div>
          <div class="stat-num">Top 15%</div>
          <div class="stat-label">⭐ 综合排名</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>📅 学习日历 · 本周学习: 12.5h</template>
          <div ref="chartRef" style="height:280px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>⏰ 即将截止</template>
          <el-timeline>
            <el-timeline-item timestamp="剩余1天" color="#f5222d">《Java高级》第3次作业</el-timeline-item>
            <el-timeline-item timestamp="剩余3天" color="#faad14">《软件测试》期中考试</el-timeline-item>
            <el-timeline-item timestamp="剩余2天" color="#1890ff">《数据库》实训报告</el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top:20px">
      <template #header>📚 我的课程进度</template>
      <div v-for="c in courses" :key="c.name" class="progress-item">
        <span class="progress-label">{{ c.name }}</span>
        <el-progress :percentage="c.progress" :color="c.color" :stroke-width="14" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { dashboardApi } from '@/api'
import * as echarts from 'echarts'

const stats = ref({})
const courses = ref([
  { name: 'Java高级程序设计', progress: 75, color: '#1890ff' },
  { name: '软件测试', progress: 90, color: '#52c41a' },
  { name: '数据库原理', progress: 40, color: '#faad14' }
])
const chartRef = ref(null)

onMounted(async () => {
  try {
    const res = await dashboardApi.studentStats()
    if (res.data.code === 200) stats.value = res.data.data
  } catch (e) { /* ignore */ }

  await nextTick()
  if (chartRef.value) {
    const chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['周一','周二','周三','周四','周五','周六','周日'] },
      yAxis: { type: 'value', name: '小时' },
      series: [{
        data: [2.5, 3.0, 1.5, 2.0, 2.5, 1.0, 0.5],
        type: 'bar', barWidth: '50%',
        itemStyle: { color: '#1890ff', borderRadius: [4,4,0,0] }
      }]
    })
  }
})
</script>

<style scoped>
.student-dashboard h2 { margin-bottom: 20px; color: #303133; }
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.stat-num { font-size: 28px; font-weight: bold; color: #303133; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
.progress-item { margin-bottom: 16px; }
.progress-label { display: block; margin-bottom: 6px; color: #606266; font-weight: 500; }
</style>
