<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">在线考试</h2>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="未开始" value="pending" />
        <el-option label="进行中" value="ongoing" />
        <el-option label="已结束" value="ended" />
        <el-option label="已交卷" value="submitted" />
      </el-select>
    </div>

    <div class="panel">
      <el-table v-if="filteredExams.length > 0 || loading" :data="filteredExams" stripe border v-loading="loading">
        <el-table-column prop="examName" label="考试名称" min-width="180" />
        <el-table-column prop="courseName" label="所属课程" min-width="140" />
        <el-table-column prop="startTime" label="开始时间" width="170">
          <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="170">
          <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="duration" label="时长" width="80">
          <template #default="{ row }">{{ row.duration || '-' }} 分钟</template>
        </el-table-column>
        <el-table-column label="状态" width="170">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:4px;white-space:nowrap">
              <el-tag :type="statusTag(row)" size="small">{{ statusLabel(row) }}</el-tag>
              <el-tag v-if="row.allowRetry === 1" type="success" size="small">可重考</el-tag>
              <el-tag v-else type="danger" size="small">不可重考</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="成绩" width="120">
          <template #default="{ row }">
            <span v-if="row.submitted === 1">{{ row.totalScore ?? 0 }} 分</span>
            <span v-else class="muted">未交卷</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canStart(row)" size="small" type="primary" @click="enterExam(row)">{{ row.submitted === 1 ?
              '重新考试' : '进入考试' }}</el-button>
            <el-button v-else-if="row.submitted === 1" size="small" @click="openResult(row)">查看结果</el-button>
            <span v-else class="muted">--</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && filteredExams.length === 0" description="暂无考试安排" :image-size="100" />
    </div>

    <el-dialog v-model="resultVisible" title="考试结果" width="760px">
      <el-descriptions v-if="currentExam" :column="3" border>
        <el-descriptions-item label="考试">{{ currentExam.examName }}</el-descriptions-item>
        <el-descriptions-item label="课程">{{ currentExam.courseName }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ currentExam.totalScore ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="结果">
          <el-tag :type="currentExam.passStatus === 1 ? 'success' : 'danger'">
            {{ currentExam.passStatus === 1 ? '通过' : '未通过' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <div class="answer-list" v-loading="resultLoading">
        <div v-for="(answer, index) in resultAnswers" :key="answer.id" class="answer-item">
          <div class="answer-title">{{ index + 1 }}. {{ answer.questionContent }}</div>
          <div class="answer-meta">
            我的答案：{{ displayAnswer(answer.studentAnswer) || '未作答' }}
            <span>参考答案：{{ displayAnswer(answer.correctAnswer) || '-' }}</span>
            <span>得分：{{ answer.score ?? 0 }} / {{ answer.questionScore ?? 0 }}</span>
          </div>
          <div v-if="answer.analysis" class="analysis">解析：{{ answer.analysis }}</div>
        </div>
        <el-empty v-if="!resultLoading && resultAnswers.length === 0" description="暂无答题明细" />
      </div>
      <template #footer>
        <el-button @click="resultVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { examApi } from '@/api'

const router = useRouter()
const exams = ref([])
const loading = ref(false)
const statusFilter = ref('')
const resultVisible = ref(false)
const resultLoading = ref(false)
const resultAnswers = ref([])
const currentExam = ref(null)

const statusLabel = (row) => {
  if (row.submitted === 1) {
    // 允许重考且仍在考试时间窗口内，视为进行中
    if (row.allowRetry === 1) {
      const now = new Date()
      if (row.startTime && new Date(row.startTime) <= now && row.endTime && new Date(row.endTime) >= now) {
        return '进行中'
      }
    }
    return '已交卷'
  }
  const now = new Date()
  if (row.startTime && new Date(row.startTime) > now) return '未开始'
  if (row.endTime && new Date(row.endTime) < now) return '已结束'
  return '进行中'
}

const statusKey = (row) => {
  const label = statusLabel(row)
  return { 未开始: 'pending', 进行中: 'ongoing', 已结束: 'ended', 已交卷: 'submitted' }[label]
}

const statusTag = (row) => {
  const key = statusKey(row)
  return { pending: 'info', ongoing: 'success', ended: 'warning', submitted: 'primary' }[key] || 'info'
}

const filteredExams = computed(() => {
  if (!statusFilter.value) return exams.value
  return exams.value.filter(exam => statusKey(exam) === statusFilter.value)
})

const canStart = (row) => statusKey(row) === 'ongoing' && (row.submitted !== 1 || row.allowRetry === 1)
const enterExam = (row) => router.push(`/student/exams/${row.id}`)
const formatTime = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : '--'
const displayAnswer = (value) => {
  if (!value) return ''
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.join('、') : String(parsed)
  } catch {
    return value
  }
}

const fetchExams = async () => {
  loading.value = true
  try {
    const res = await examApi.listExams({ page: 1, size: 100 })
    if (res.data.code === 200) {
      exams.value = res.data.data?.records || []
    }
  } finally {
    loading.value = false
  }
}

const openResult = async (row) => {
  currentExam.value = row
  resultVisible.value = true
  resultLoading.value = true
  resultAnswers.value = []
  try {
    const res = await examApi.getRecordDetail(row.recordId)
    if (res.data.code === 200) {
      resultAnswers.value = res.data.data || []
    }
  } finally {
    resultLoading.value = false
  }
}

onMounted(fetchExams)
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

.panel {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 24px;
}

.muted {
  color: #909399;
}

.answer-list {
  margin-top: 18px;
}

.answer-item {
  padding: 14px 0;
  border-bottom: 1px solid #ebeef5;
}

.answer-title {
  color: #303133;
  font-weight: 600;
  line-height: 1.6;
}

.answer-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 8px;
  color: #606266;
  font-size: 13px;
}

.analysis {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
}
</style>
