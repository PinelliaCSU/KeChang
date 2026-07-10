<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">成绩概览</h2>
      <el-button @click="fetchOverview">刷新</el-button>
    </div>

    <div class="stat-row" v-loading="loading">
      <div class="stat-card">
        <div class="stat-num">{{ overview.courseCount ?? 0 }}</div>
        <div class="stat-label">课程总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ overview.enrollmentCount ?? 0 }}</div>
        <div class="stat-label">选课人次</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ overview.gradeCount ?? 0 }}</div>
        <div class="stat-label">有成绩记录</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ formatScore(overview.avg) }}</div>
        <div class="stat-label">平台平均分</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ formatRate(overview.passRate) }}</div>
        <div class="stat-label">平台通过率</div>
      </div>
    </div>

    <DataPanel title="平台成绩分布" max-width="1400px">
      <div class="dist-list" v-if="distribution.length">
        <div v-for="item in distribution" :key="item.label" class="dist-row">
          <span class="dist-label">{{ item.label }}</span>
          <div class="dist-track">
            <div class="dist-fill" :style="{ width: item.percent + '%', background: item.color }"></div>
          </div>
          <span class="dist-count">{{ item.count }} 人</span>
        </div>
      </div>
      <el-empty v-else description="暂无成绩统计数据" :image-size="100" />
    </DataPanel>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { gradeApi } from '@/api'
import DataPanel from '@/components/DataPanel.vue'

const overview = ref({})
const distribution = ref([])
const loading = ref(false)
const colors = ['#67c23a', '#409eff', '#e6a23c', '#909399', '#f56c6c']

const formatScore = (value) => value == null ? '--' : Number(value).toFixed(1)
const formatRate = (value) => value == null ? '--' : `${value}%`

const buildDistribution = (raw = {}) => {
  const entries = Object.entries(raw)
  const total = entries.reduce((sum, [, count]) => sum + Number(count || 0), 0)
  if (!total) {
    distribution.value = []
    return
  }
  distribution.value = entries.map(([label, count], index) => ({
    label,
    count,
    percent: Math.round(Number(count || 0) * 100 / total),
    color: colors[index % colors.length]
  }))
}

const fetchOverview = async () => {
  loading.value = true
  try {
    const res = await gradeApi.platformOverview()
    if (res.data.code === 200) {
      overview.value = res.data.data || {}
      buildDistribution(overview.value.distribution || {})
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchOverview)
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
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.stat-card {
  background: #fff;
  border: 1px solid rgba(163, 177, 138, 0.1);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 18px 22px;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
}

.dist-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dist-row {
  display: grid;
  grid-template-columns: 120px 1fr 70px;
  align-items: center;
  gap: 12px;
}

.dist-label,
.dist-count {
  color: #606266;
  font-size: 13px;
}

.dist-track {
  height: 22px;
  background: #f0f2f5;
  border-radius: 999px;
  overflow: hidden;
}

.dist-fill {
  height: 100%;
  min-width: 2px;
  border-radius: 999px;
}

@media (max-width: 1200px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
