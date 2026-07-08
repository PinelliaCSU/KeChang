<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">成绩管理</h2>
      <el-select v-model="selectedCourse" placeholder="选择课程" clearable style="width: 240px" @change="loadCourseGrades">
        <el-option v-for="course in courseList" :key="course.id" :label="course.courseName" :value="course.id" />
      </el-select>
    </div>

    <div class="stat-row" v-if="stats">
      <div class="stat-card">
        <div class="stat-num">{{ formatStat(stats.avg) }}</div>
        <div class="stat-label">平均分</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ formatStat(stats.max) }}</div>
        <div class="stat-label">最高分</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ formatStat(stats.min) }}</div>
        <div class="stat-label">最低分</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ formatRate(stats.passRate) }}</div>
        <div class="stat-label">通过率</div>
      </div>
    </div>

    <div class="panel" v-if="distribution.length">
      <h3 class="section-title">成绩分布</h3>
      <div class="dist-list">
        <div v-for="item in distribution" :key="item.label" class="dist-row">
          <span class="dist-label">{{ item.label }}</span>
          <div class="dist-track">
            <div class="dist-fill" :style="{ width: item.percent + '%', background: item.color }"></div>
          </div>
          <span class="dist-count">{{ item.count }} 人</span>
        </div>
      </div>
    </div>

    <div class="panel">
      <el-table :data="grades" stripe border v-loading="loading">
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="studentName" label="姓名" min-width="120" />
        <el-table-column v-if="hasExamScores" prop="examAvgScore" label="考试均分" width="110">
          <template #default="{ row }">{{ formatScore(row.examAvgScore) }}</template>
        </el-table-column>
        <el-table-column v-if="hasAssignmentScores" prop="assignmentAvgScore" label="作业均分" width="110">
          <template #default="{ row }">{{ formatScore(row.assignmentAvgScore) }}</template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总评成绩" width="110">
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

      <el-empty v-if="!loading && !selectedCourse" description="请先选择课程查看成绩" :image-size="100" />
      <el-empty v-if="!loading && selectedCourse && grades.length === 0" description="该课程暂无成绩数据" :image-size="100" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { courseApi, gradeApi } from '@/api'

const courseList = ref([])
const selectedCourse = ref(null)
const grades = ref([])
const stats = ref(null)
const distribution = ref([])
const loading = ref(false)

const hasExamScores = computed(() => grades.value.some(item => item.examAvgScore != null))
const hasAssignmentScores = computed(() => grades.value.some(item => item.assignmentAvgScore != null))

const levelType = (level) => ({ 优秀: 'success', 良好: 'primary', 中等: 'warning', 及格: 'info', 不及格: 'danger' }[level] || 'info')
const formatScore = (score) => score == null ? '--' : Number(score).toFixed(1)
const formatStat = (value) => value == null ? '--' : Number(value).toFixed(1)
const formatRate = (value) => value == null ? '--' : `${value}%`
const scoreClass = (score) => {
  if (score == null) return 'score-empty'
  const value = Number(score)
  if (value >= 90) return 'score-good'
  if (value >= 60) return 'score-pass'
  return 'score-fail'
}

const distColors = ['#67c23a', '#409eff', '#e6a23c', '#909399', '#f56c6c']

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
    color: distColors[index % distColors.length]
  }))
}

const fetchCourses = async () => {
  const res = await courseApi.list({ page: 1, size: 100 })
  if (res.data.code === 200) {
    courseList.value = res.data.data?.records || []
  }
}

const loadCourseGrades = async (courseId) => {
  if (!courseId) {
    grades.value = []
    stats.value = null
    distribution.value = []
    return
  }
  loading.value = true
  try {
    const [gradeRes, statsRes] = await Promise.all([
      gradeApi.courseGrades(courseId),
      gradeApi.courseStats(courseId)
    ])
    if (gradeRes.data.code === 200) grades.value = gradeRes.data.data || []
    if (statsRes.data.code === 200) {
      stats.value = statsRes.data.data || {}
      buildDistribution(stats.value.distribution || {})
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchCourses)
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
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card,
.panel {
  background: #fff;
  border: 1px solid rgba(163, 177, 138, 0.1);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.stat-card {
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

.panel {
  padding: 24px;
}

.section-title {
  margin: 0 0 16px;
  font-size: 16px;
  color: #303133;
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
