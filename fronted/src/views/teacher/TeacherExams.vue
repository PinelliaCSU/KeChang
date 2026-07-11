<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">考试管理</h2>
      <el-button type="primary" @click="openCreateDialog">+ 创建考试</el-button>
    </div>

    <div class="panel">
      <div class="search-bar">
        <el-select v-model="courseFilter" placeholder="按课程筛选" clearable style="width:200px" @change="fetchExams">
          <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:140px" @change="fetchExams">
          <el-option label="未开始" value="未开始" /><el-option label="进行中" value="进行中" /><el-option label="已结束"
            value="已结束" />
        </el-select>
      </div>

      <el-table v-if="exams.length > 0 || loading" :data="exams" v-loading="loading" stripe class="kechang-table"
        style="width:100%">
        <el-table-column prop="examName" label="考试名称" min-width="180" />
        <el-table-column prop="courseName" label="所属课程" min-width="120" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column prop="duration" label="时长(分)" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(computeStatus(row.startTime, row.endTime))" size="small">{{
              computeStatus(row.startTime, row.endTime) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="info" @click="viewRecords(row)">记录</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && exams.length === 0" description="暂无考试安排" :image-size="100" />
      <el-pagination v-if="total > pageSize" :current-page="page" :page-size="pageSize" :total="total"
        layout="prev, pager, next" @current-change="p => { page = p; fetchExams() }"
        style="margin-top:16px;justify-content:center" />
    </div>

    <!-- 创建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑考试' : '创建考试'" width="550px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="考试名称" required><el-input v-model="form.examName" /></el-form-item>
        <el-form-item label="所属课程" required>
          <el-select v-model="form.courseId" placeholder="选择课程" style="width:100%" @change="onCourseSelect">
            <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联试卷" required>
          <el-select v-model="form.paperId" placeholder="选择试卷" style="width:100%" @change="onPaperSelect">
            <el-option v-for="p in paperList" :key="p.id" :label="p.paperName + ' (' + p.totalScore + '分)'"
              :value="p.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间" required>
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" required>
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="10">
            <el-form-item label="时长(分)"><el-input-number v-model="form.duration" :min="1" style="width:100%" />
              <span v-if="form.paperId && selectedPaper" style="font-size:12px;color:#909399">
                试卷原始时长: {{ selectedPaper.duration }}分钟
              </span>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="允许重考">
              <el-switch v-model="allowRetry" active-text="是" inactive-text="否" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存修改' : '创建考试' }}</el-button>
      </template>
    </el-dialog>

    <!-- 考试记录弹窗 -->
    <el-dialog v-model="recordDialogVisible" title="考试记录" width="900px">
      <div class="dialog-toolbar">
        <span class="toolbar-muted">共 {{ records.length }} 条记录</span>
        <el-button size="small" :disabled="records.length === 0" @click="exportExamRecords">导出考试成绩</el-button>
      </div>
      <el-table v-if="records.length > 0 || recLoading" :data="records" v-loading="recLoading" stripe class="kechang-table" style="width:100%">
        <el-table-column prop="studentId" label="学生ID" width="80" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="submitTime" label="提交时间" width="170" />
        <el-table-column prop="totalScore" label="得分" width="80">
          <template #default="{ row }">{{ row.totalScore != null ? row.totalScore : '未批' }}</template>
        </el-table-column>
        <el-table-column label="是否通过" width="90">
          <template #default="{ row }">
            <el-tag :type="row.passStatus === 1 ? 'success' : 'danger'" size="small">{{ row.passStatus === 1 ? '通过' :
              '未通过' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cheatCount" label="切屏次数" width="90">
          <template #default="{ row }">
            <span :style="{ color: row.cheatCount > 2 ? '#f56c6c' : '#909399' }">{{ row.cheatCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="viewRecordDetail(row)">批阅</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!recLoading && records.length === 0" description="暂无考试记录" :image-size="90" />
      <template #footer><el-button @click="recordDialogVisible = false">关闭</el-button></template>
    </el-dialog>

    <!-- 批阅弹窗 -->
    <el-dialog v-model="markDialogVisible" title="批阅答卷" width="800px">
      <div v-if="recordAnswers.length > 0">
        <div v-for="(ans, idx) in recordAnswers" :key="ans.id" class="answer-item">
          <div class="answer-header">
            <span><b>{{ idx + 1 }}. [{{ typeLabel(ans.questionType || '') }}]</b> {{ ans.questionContent || '' }} ({{
              ans.questionScore || 0 }}分)</span>
          </div>
          <div v-if="ans.options" style="color:#909399;font-size:13px;margin:4px 0">
            选项: {{ JSON.parse(ans.options || '[]').join(' | ') }}
          </div>
          <div class="answer-row">
            <span>学生答案：<b :style="{ color: ans.isCorrect === 1 ? 'var(--bio-primary)' : '#f56c6c' }">{{ ans.studentAnswer || '未作答'
                }}</b></span>
            <span style="margin-left:20px">参考答案：<b style="color:var(--bio-primary)">{{ ans.correctAnswer || '' }}</b></span>
          </div>
          <div class="score-row" v-if="ans.questionType === 'ESSAY' || ans.questionType === 'FILL'">
            <span>得分：</span>
            <el-input-number v-model="scoreMap[ans.questionId]" :min="0" :max="ans.questionScore" size="small"
              style="width:100px" />
            <el-button size="small" type="primary" @click="doMark(ans.questionId, ans.recordId)"
              style="margin-left:8px">确认评分</el-button>
          </div>
          <div v-else style="color:#909399;font-size:13px">
            自动判分: {{ ans.isCorrect === 1 ? '✅ 正确' : '❌ 错误' }} ({{ ans.score || 0 }}分)
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无答题记录" />
      <template #footer>
        <el-button @click="markDialogVisible = false">关闭</el-button>
        <el-button type="success" plain :disabled="!hasNextRecord" @click="openNextRecord">批阅下一个</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { examApi, courseApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computeStatus, statusTagType } from '@/utils/status'

const exams = ref([]); const loading = ref(false)
const page = ref(1); const pageSize = ref(10); const total = ref(0)
const courseFilter = ref(null); const statusFilter = ref('')
const courseList = ref([]); const paperList = ref([])

// 创建/编辑
const dialogVisible = ref(false); const isEdit = ref(false); const editId = ref(null); const submitting = ref(false)
const form = reactive({ examName: '', courseId: null, paperId: null, startTime: '', endTime: '', duration: 90 })
const allowRetry = ref(false)

// 记录
const recordDialogVisible = ref(false)
const records = ref([]); const recLoading = ref(false)
const currentExamId = ref(null)

// 批阅
const markDialogVisible = ref(false)
const recordAnswers = ref([])
const scoreMap = reactive({})
const currentRecord = ref(null)
const currentExamName = ref('')
const currentRecordIndex = computed(() => records.value.findIndex(item => item.id === currentRecord.value?.id))
const hasNextRecord = computed(() => currentRecordIndex.value >= 0 && currentRecordIndex.value < records.value.length - 1)

const statusType = (s) => ({ '未开始': 'info', '进行中': 'success', '已结束': 'warning' }[s] || 'info')
const typeLabel = (t) => ({ SINGLE: '单选', MULTI: '多选', JUDGE: '判断', FILL: '填空', ESSAY: '简答' }[t] || t)

const fetchExams = async () => {
  loading.value = true
  try {
    const res = await examApi.listExams({ page: page.value, size: pageSize.value, courseId: courseFilter.value || undefined })
    if (res.data.code === 200) { exams.value = res.data.data.records; total.value = res.data.data.total }
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

const fetchCourses = async () => {
  try {
    const res = await courseApi.list({ page: 1, size: 100 })
    if (res.data.code === 200) courseList.value = res.data.data.records
  } catch (e) { /* ignore */ }
}

const onCourseSelect = async (courseId) => {
  paperList.value = []
  if (!courseId) return
  try {
    const res = await examApi.listPapers({ page: 1, size: 100, courseId })
    if (res.data.code === 200) paperList.value = res.data.data.records
  } catch (e) { /* ignore */ }
}

const selectedPaper = computed(() => {
  return paperList.value.find(p => p.id === form.paperId) || null
})

const onPaperSelect = (paperId) => {
  if (!paperId) return
  const paper = paperList.value.find(p => p.id === paperId)
  if (paper && paper.duration) {
    form.duration = paper.duration
  }
}

const openCreateDialog = () => {
  isEdit.value = false; editId.value = null
  form.examName = ''; form.courseId = null; form.paperId = null; form.startTime = ''; form.endTime = ''
  form.duration = 90
  allowRetry.value = false; paperList.value = []
  dialogVisible.value = true
}

const openEditDialog = async (row) => {
  isEdit.value = true; editId.value = row.id
  form.examName = row.examName; form.courseId = row.courseId; form.paperId = row.paperId
  form.startTime = row.startTime; form.endTime = row.endTime; form.duration = row.duration || 90
  allowRetry.value = row.allowRetry === 1
  await onCourseSelect(row.courseId)
  onPaperSelect(row.paperId)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.examName || !form.courseId || !form.paperId || !form.startTime || !form.endTime) {
    ElMessage.warning('请填写完整信息'); return
  }
  submitting.value = true
  try {
    const data = { ...form, allowRetry: allowRetry.value ? 1 : 0 }
    let res
    if (isEdit.value) {
      res = await examApi.updateExam(editId.value, data)
    } else {
      res = await examApi.createExam(data)
    }
    if (res.data.code === 200) {
      ElMessage.success(isEdit.value ? '考试更新成功' : '考试创建成功')
      dialogVisible.value = false
      fetchExams()
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (e) { /* ignore */ }
  finally { submitting.value = false }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该考试？', '确认删除', { type: 'warning' })
    await examApi.deleteExam(id)
    ElMessage.success('删除成功')
    fetchExams()
  } catch (e) { /* ignore */ }
}

const viewRecords = async (row) => {
  currentExamId.value = row.id
  currentExamName.value = row.examName || '考试'
  recordDialogVisible.value = true
  recLoading.value = true
  try {
    const res = await examApi.getRecords(row.id)
    if (res.data.code === 200) records.value = res.data.data
  } catch (e) { /* ignore */ }
  finally { recLoading.value = false }
}

const viewRecordDetail = async (row) => {
  currentRecord.value = row
  markDialogVisible.value = true
  recLoading.value = true
  try {
    const res = await examApi.getRecordDetail(row.id)
    if (res.data.code === 200) {
      recordAnswers.value = res.data.data || []
      recordAnswers.value.forEach(a => { scoreMap[a.questionId] = a.score || 0 })
    }
  } catch (e) { /* ignore */ }
  finally { recLoading.value = false }
}

const doMark = async (questionId, recordId) => {
  try {
    await examApi.mark(recordId, { questionId, score: scoreMap[questionId] || 0 })
    ElMessage.success('评分成功')
    const current = records.value.find(item => item.id === recordId)
    if (current) current.totalScore = null
  } catch (e) { /* ignore */ }
}

const openNextRecord = () => {
  if (!hasNextRecord.value) return
  viewRecordDetail(records.value[currentRecordIndex.value + 1])
}

const csvCell = (value) => {
  const text = value == null ? '' : String(value)
  return `"${text.replace(/"/g, '""')}"`
}

const downloadCsv = (filename, rows) => {
  const content = '\ufeff' + rows.map(row => row.map(csvCell).join(',')).join('\n')
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
  URL.revokeObjectURL(link.href)
}

const exportExamRecords = () => {
  if (!records.value.length) return
  const rows = [
    ['学生ID', '开始时间', '提交时间', '得分', '是否通过', '切屏次数'],
    ...records.value.map(item => [
      item.studentId || '',
      item.startTime || '',
      item.submitTime || '',
      item.totalScore != null ? item.totalScore : '未批',
      item.passStatus === 1 ? '通过' : '未通过',
      item.cheatCount || 0
    ])
  ]
  downloadCsv(`${currentExamName.value || '考试'}-成绩记录.csv`, rows)
}

onMounted(() => { fetchCourses(); fetchExams() })
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

.panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 24px;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.dialog-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.toolbar-muted {
  color: #909399;
  font-size: 13px;
}

.answer-item {
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.answer-header {
  font-size: 14px;
  margin-bottom: 4px;
}

.answer-row {
  margin: 6px 0;
  font-size: 14px;
}

.score-row {
  margin-top: 8px;
  display: flex;
  align-items: center;
}

:deep(.kechang-table) {
  width: 100%;
  border: none;
}

:deep(.kechang-table .el-table__header-wrapper th.el-table__cell) {
  background-color: #F5F7FA !important;
  color: #606266;
  font-weight: 600;
  font-size: 13px;
}

:deep(.kechang-table .el-table__body-wrapper td.el-table__cell) {
  border-bottom: none;
}

:deep(.kechang-table .el-table__cell) {
  border-right: none;
}

:deep(.kechang-table::before),
:deep(.kechang-table::after),
:deep(.kechang-table .el-table__inner-wrapper::before),
:deep(.kechang-table .el-table__border-left-patch),
:deep(.kechang-table .el-table__border-right-patch) {
  display: none;
}

:deep(.kechang-table.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background-color: rgba(32, 196, 204, 0.06);
}

:deep(.kechang-table .el-table__body tr.el-table__row:hover td) {
  background-color: rgba(32, 196, 204, 0.08) !important;
}
</style>
