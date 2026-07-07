<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">成绩管理</h2>
      <div class="page-actions">
        <el-select v-model="selectedCourse" placeholder="选择课程" size="default" style="width:200px" clearable
          @change="onCourseChange">
          <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
        <el-button v-if="selectedCourse" type="success" @click="exportGrades">📥 导出成绩</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-row" v-if="stats">
      <div class="stat-card">
        <div class="stat-num">{{ stats.avg || '--' }}</div>
        <div class="stat-label">📊 平均分</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ stats.max || '--' }}</div>
        <div class="stat-label">🏆 最高分</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ stats.min || '--' }}</div>
        <div class="stat-label">📉 最低分</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ passRate }}%</div>
        <div class="stat-label">✅ 通过率</div>
      </div>
    </div>

    <!-- 成绩分布 -->
    <div class="panel" v-if="distData.length > 0">
      <h4 class="section-title">📈 成绩分布</h4>
      <div class="dist-bar">
        <div v-for="d in distData" :key="d.level" class="dist-item">
          <div class="dist-label">{{ d.level }}</div>
          <div class="dist-track">
            <div class="dist-fill" :style="{ width: d.percent + '%', background: d.color }"></div>
          </div>
          <div class="dist-num">{{ d.count }}人 ({{ d.percent }}%)</div>
        </div>
      </div>
    </div>

    <div class="panel">
      <el-table :data="grades" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="studentName" label="学生姓名" min-width="120" />
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="examAvgScore" label="考试均分" width="100" />
        <el-table-column prop="assignmentAvgScore" label="作业均分" width="100" />
        <el-table-column prop="totalScore" label="总评成绩" width="100">
          <template #default="{ row }">
            <b :style="{ color: row.totalScore >= 90 ? '#67c23a' : row.totalScore >= 60 ? '#409eff' : '#f56c6c' }">
              {{ row.totalScore }}
            </b>
          </template>
        </el-table-column>
        <el-table-column prop="gradeLevel" label="等级" width="90">
          <template #default="{ row }">
            <el-tag :type="levelType(row.gradeLevel)" size="small">{{ row.gradeLevel || '--' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !selectedCourse" description="请先选择课程查看成绩" :image-size="100" />
      <el-empty v-if="!loading && selectedCourse && grades.length === 0" description="该课程暂无成绩数据" :image-size="100" />
    </div>

    <!-- 学生成绩详情弹窗 -->
    <el-dialog v-model="detailVisible" title="成绩详情" width="500px">
      <el-descriptions :column="1" border v-if="currentStudent">
        <el-descriptions-item label="学生姓名">{{ currentStudent.studentName }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ currentStudent.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="考试均分">{{ currentStudent.examAvgScore }}分</el-descriptions-item>
        <el-descriptions-item label="作业均分">{{ currentStudent.assignmentAvgScore }}分</el-descriptions-item>
        <el-descriptions-item label="总评成绩">
          <b
            :style="{ color: currentStudent.totalScore >= 90 ? '#67c23a' : currentStudent.totalScore >= 60 ? '#409eff' : '#f56c6c' }">
            {{ currentStudent.totalScore }}分
          </b>
        </el-descriptions-item>
        <el-descriptions-item label="等级">
          <el-tag :type="levelType(currentStudent.gradeLevel)">{{ currentStudent.gradeLevel }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { gradeApi, courseApi } from '@/api'
import { ElMessage } from 'element-plus'

const courseList = ref([])
const selectedCourse = ref(null)
const grades = ref([])
const loading = ref(false)
const stats = ref(null)
const distData = ref([])
const detailVisible = ref(false)
const currentStudent = ref(null)

const passRate = ref(0)

const levelType = (l) => ({ '优秀': 'success', '良好': 'primary', '中等': 'warning', '及格': 'info', '不及格': 'danger' }[l] || 'info')

const fetchCourses = async () => {
  try {
    const res = await courseApi.list({ page: 1, size: 100 })
    if (res.data.code === 200) courseList.value = res.data.data.records
  } catch (e) { /* ignore */ }
}

const onCourseChange = async (courseId) => {
  if (!courseId) { grades.value = []; stats.value = null; distData.value = []; return }
  loading.value = true
  try {
    const [gradeRes, statsRes] = await Promise.all([
      gradeApi.courseGrades(courseId),
      gradeApi.courseStats(courseId)
    ])
    if (gradeRes.data.code === 200) grades.value = gradeRes.data.data || []
    if (statsRes.data.code === 200) {
      const s = statsRes.data.data
      stats.value = s
      // 构建分布数据
      const levels = [
        { level: '优秀(90-100)', min: 90, max: 100, color: '#67c23a' },
        { level: '良好(80-89)', min: 80, max: 89, color: '#409eff' },
        { level: '中等(70-79)', min: 70, max: 79, color: '#e6a23c' },
        { level: '及格(60-69)', min: 60, max: 69, color: '#909399' },
        { level: '不及格(0-59)', min: 0, max: 59, color: '#f56c6c' }
      ]
      const total = grades.value.length || 1
      const passed = grades.value.filter(g => g.totalScore >= 60).length
      passRate.value = Math.round(passed / total * 100)
      distData.value = levels.map(l => {
        const count = grades.value.filter(g => g.totalScore >= l.min && g.totalScore <= l.max).length
        return { ...l, count, percent: Math.round(count / total * 100) }
      })
    }
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

const viewDetail = (row) => { currentStudent.value = row; detailVisible.value = true }

const exportGrades = () => {
  ElMessage.info('成绩导出功能将在后续版本中实现')
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
  margin-bottom: 4px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 24px;
}

.section-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
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
  padding: 16px 20px;
  text-align: center;
}

.stat-num {
  font-size: 26px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.dist-bar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dist-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dist-label {
  width: 110px;
  font-size: 13px;
  text-align: right;
  color: #606266;
}

.dist-track {
  flex: 1;
  height: 24px;
  background: #f0f0f0;
  border-radius: 12px;
  overflow: hidden;
}

.dist-fill {
  height: 100%;
  border-radius: 12px;
  transition: width 0.5s;
  min-width: 2px;
}

.dist-num {
  width: 100px;
  font-size: 13px;
  color: #909399;
}
</style>
