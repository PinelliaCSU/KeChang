<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">我的成绩</h2>
    </div>

    <div class="summary-row" v-if="scoredGrades.length">
      <div class="summary-card">
        <div class="summary-num">{{ averageScore }}</div>
        <div class="summary-label">平均总评</div>
      </div>
      <div class="summary-card">
        <div class="summary-num">{{ passedCount }}</div>
        <div class="summary-label">通过课程</div>
      </div>
      <div class="summary-card">
        <div class="summary-num">{{ scoredGrades.length }}</div>
        <div class="summary-label">有成绩课程</div>
      </div>
    </div>

    <div class="panel">
      <el-table :data="grades" stripe border v-loading="loading">
        <el-table-column prop="courseName" label="课程" min-width="180" />
        <el-table-column v-if="hasExamScores" prop="examAvgScore" label="考试均分" width="110">
          <template #default="{ row }">{{ formatScore(row.examAvgScore) }}</template>
        </el-table-column>
        <el-table-column v-if="hasAssignmentScores" prop="assignmentAvgScore" label="作业均分" width="110">
          <template #default="{ row }">{{ formatScore(row.assignmentAvgScore) }}</template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总评" width="100">
          <template #default="{ row }">
            <b :class="scoreClass(row.totalScore)">{{ formatScore(row.totalScore) }}</b>
          </template>
        </el-table-column>
        <el-table-column prop="gradeLevel" label="等级" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.gradeLevel" :type="levelType(row.gradeLevel)" size="small">{{ row.gradeLevel }}</el-tag>
            <span v-else class="muted">--</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && grades.length === 0" description="暂无成绩数据" :image-size="100" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { gradeApi } from '@/api'

const grades = ref([])
const loading = ref(false)

const scoredGrades = computed(() => grades.value.filter(item => item.totalScore != null))
const hasExamScores = computed(() => grades.value.some(item => item.examAvgScore != null))
const hasAssignmentScores = computed(() => grades.value.some(item => item.assignmentAvgScore != null))

const averageScore = computed(() => {
  if (!scoredGrades.value.length) return '--'
  const avg = scoredGrades.value.reduce((sum, item) => sum + Number(item.totalScore), 0) / scoredGrades.value.length
  return avg.toFixed(1)
})
const passedCount = computed(() => scoredGrades.value.filter(item => Number(item.totalScore) >= 60).length)

const levelType = (level) => ({ 优秀: 'success', 良好: 'primary', 中等: 'warning', 及格: 'info', 不及格: 'danger' }[level] || 'info')
const formatScore = (score) => score == null ? '--' : Number(score).toFixed(1)
const scoreClass = (score) => {
  if (score == null) return 'score-empty'
  const value = Number(score)
  if (value >= 90) return 'score-good'
  if (value >= 60) return 'score-pass'
  return 'score-fail'
}

const fetchGrades = async () => {
  loading.value = true
  try {
    const res = await gradeApi.myGrades()
    if (res.data.code === 200) grades.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

onMounted(fetchGrades)
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

.summary-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.summary-card,
.panel {
  background: #fff;
  border: 1px solid rgba(163, 177, 138, 0.1);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.summary-card {
  padding: 20px 24px;
}

.summary-num {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.summary-label {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
}

.panel {
  padding: 24px;
}

.muted,
.score-empty {
  color: #909399;
}

.score-good {
  color: #67c23a;
}

.score-pass {
  color: #409eff;
}

.score-fail {
  color: #f56c6c;
}
</style>
