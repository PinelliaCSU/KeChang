<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div>
        <h2 class="page-title">管理仪表盘</h2>
        <p class="page-subtitle">平台运行概览，展示核心指标和最近动态</p>
      </div>
      <el-button @click="refresh">刷新</el-button>
    </div>

    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-num">{{ stats.totalUsers || 0 }}</div>
        <div class="stat-label">用户总数
          <span class="stat-sub">教师 {{ stats.teacherCount || 0 }} / 学生 {{ stats.studentCount || 0 }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-num success">{{ stats.courseCount || 0 }}</div>
        <div class="stat-label">课程总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num new">{{ stats.newUsersThisMonth || 0 }}</div>
        <div class="stat-label">本月新增用户</div>
      </div>
      <div class="stat-card">
        <div class="stat-num info">{{ stats.todayActiveOps || 0 }}</div>
        <div class="stat-label">今日平台活跃</div>
      </div>
    </div>

    <DataPanel title="近7日平台活跃趋势" max-width="1400px">
      <div ref="chartRef" class="chart-container"></div>
    </DataPanel>

    <DataPanel title="最近操作记录" max-width="1400px">
      <el-table :data="recentOps" stripe class="is-borderless" style="width: 100%">
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column prop="operatorName" label="操作人" width="130" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="action" label="动作" width="140">
          <template #default="{ row }">
            <el-tag size="small">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="对象" min-width="200" show-overflow-tooltip />
      </el-table>
    </DataPanel>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, reactive } from 'vue'
import { dashboardApi } from '@/api'
import DataPanel from '@/components/DataPanel.vue'
import * as echarts from 'echarts'

const stats = ref({})
const recentOps = ref([])
const chartRef = ref(null)
let chartInstance = null

const fetchStats = async () => {
  try {
    const res = await dashboardApi.adminStats()
    if (res.data.code === 200) {
      const data = res.data.data || {}
      stats.value = data
      recentOps.value = data.recentOperations || []
      if (data.activeTrend) {
        renderChart(data.activeTrend)
      }
    }
  } catch {
    // 静默失败
  }
}

const renderChart = (trendData) => {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  const dates = trendData.map(d => d.date?.slice(5) || '') // MM-DD
  const values = trendData.map(d => d.count || 0)

  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { top: 20, right: 20, bottom: 24, left: 44 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    series: [{
      type: 'bar',
      data: values,
      barWidth: 24,
      itemStyle: {
        color: '#20c4cc',
        borderRadius: [4, 4, 0, 0]
      }
    }]
  })
}

const handleResize = () => {
  if (chartInstance) chartInstance.resize()
}

const refresh = () => {
  fetchStats()
}

onMounted(() => {
  fetchStats()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.page-shell { display: flex; flex-direction: column; gap: 20px; }
.page-title-bar { display: flex; align-items: center; justify-content: space-between; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.page-subtitle { color: #909399; font-size: 13px; margin: 6px 0 0; }
.stat-row { display: flex; gap: 16px; }
.stat-card { flex: 1; background: #fff; border-radius: 8px; padding: 20px 24px; box-shadow: 0 4px 12px rgba(0,0,0,.05); border: 1px solid #ebeef5; }
.stat-num { color: #303133; font-size: 28px; font-weight: 700; }
.stat-num.success { color: #67c23a; }
.stat-num.new { color: var(--bio-primary); }
.stat-num.info { color: #409eff; }
.stat-label { color: #606266; font-size: 13px; margin-top: 6px; }
.stat-sub { color: #909399; font-size: 12px; margin-left: 4px; }
.chart-container { width: 100%; height: 260px; }
</style>
