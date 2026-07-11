<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">作业管理</h2>
      <el-button type="primary" @click="openCreate">布置作业</el-button>
    </div>

    <div class="panel">
      <div class="search-bar">
        <el-select v-model="courseFilter" placeholder="按课程筛选" clearable style="width:220px"
          @change="onFilterCourseChange">
          <el-option v-for="course in courseList" :key="course.id" :label="course.courseName" :value="course.id" />
        </el-select>
        <el-select v-model="chapterFilter" placeholder="按章节筛选" clearable style="width:220px" @change="fetchAssignments">
          <el-option v-for="chapter in filterChapters" :key="chapter.id" :label="chapter.chapterName"
            :value="chapter.id" />
        </el-select>
      </div>

      <el-table v-if="assignments.length > 0 || loading" :data="assignments" v-loading="loading" stripe
        class="kechang-table" style="width:100%">
        <el-table-column label="作业标题" min-width="220">
          <template #default="{ row }">
            <div class="assign-name-cell">
              <div class="assign-title">{{ row.title }}</div>
              <div class="assign-status">{{ computeDeadlineStatus(row.deadline) }}</div>
              <div class="assign-chapter">章节/单元：{{ chapterName(row.chapterId) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="课程" min-width="140">
          <template #default="{ row }">
            <div class="course-icon-cell">
              <el-icon class="course-icon">
                <School />
              </el-icon>
              <span class="course-text">{{ row.courseName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="资源" width="90" align="center">
          <template #default="{ row }">
            <el-button v-if="assignmentFiles(row).length" size="small" link type="primary"
              @click="previewFiles(assignmentFiles(row), row.title)">预览</el-button>
            <span v-else class="muted">无</span>
          </template>
        </el-table-column>
        <el-table-column label="截止时间" width="170">
          <template #default="{ row }">
            <span :class="{ 'deadline-overdue': isOverdue(row.deadline) }">{{ formatDate(row.deadline) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交" width="110" align="center">
          <template #default="{ row }">
            <span class="submit-count">{{ row.submitCount || 0 }}</span>
            <span class="submit-sep">/</span>
            <span class="submit-total">{{ row.totalStudents || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="总分" width="80" align="center">
          <template #default="{ row }">
            <span class="score-value">{{ row.totalScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" class="act-btn" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="primary" class="act-btn" @click="viewSubmits(row)">批阅</el-button>
            <el-button size="small" type="danger" class="act-btn" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && assignments.length === 0" description="暂无作业" :image-size="100" />
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑作业' : '布置作业'" width="620px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="作业标题" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="所属课程" required>
          <el-select v-model="form.courseId" placeholder="选择课程" style="width:100%" @change="loadFormChapters">
            <el-option v-for="course in courseList" :key="course.id" :label="course.courseName" :value="course.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属章节">
          <el-select v-model="form.chapterId" placeholder="选择章节" clearable style="width:100%">
            <el-option v-for="chapter in formChapters" :key="chapter.id" :label="chapter.chapterName"
              :value="chapter.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作业说明"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="截止时间" required>
              <el-date-picker v-model="form.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总分"><el-input-number v-model="form.totalScore" :min="1" :max="100"
                style="width:100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="作业资源">
          <div class="file-stack">
            <div v-for="(f, i) in form.files" :key="i" class="file-item">
              <span class="file-name">{{ f.name }}</span>
              <span class="file-size">{{ formatFileSize(f.size) }}</span>
              <el-button size="small" link type="primary" @click="previewFiles([f], f.name)">预览</el-button>
              <el-button size="small" link type="danger" @click="form.files.splice(i, 1)">移除</el-button>
            </div>
            <el-upload :http-request="uploadAttachment" :show-file-list="false">
              <el-button :loading="uploadLoading">上传文件</el-button>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存修改' : '布置作业' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="submitDialogVisible" title="作业提交列表" width="960px">
      <div class="submit-tools">
        <el-button type="primary" plain :disabled="!submits.length" @click="downloadSubmitArchive">下载全部提交附件</el-button>
      </div>
      <el-table v-if="submits.length > 0 || subLoading" :data="submits" v-loading="subLoading" stripe
        class="kechang-table" style="width:100%">
        <el-table-column prop="studentName" label="学生" width="120" />
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="content" label="提交内容" min-width="190" show-overflow-tooltip />
        <el-table-column label="附件" width="100">
          <template #default="{ row }">
            <el-button v-if="submitFiles(row).length" size="small" link type="primary"
              @click="previewFiles(submitFiles(row), `${row.studentName || '学生'}提交`)">预览</el-button>
            <span v-else class="muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="170" />
        <el-table-column label="评分" width="80">
          <template #default="{ row }">{{ row.score != null ? row.score : '未评' }}</template>
        </el-table-column>
        <el-table-column prop="comment" label="评语" min-width="140" show-overflow-tooltip />
        <el-table-column label="优秀" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.isExcellent" type="success" size="small">优秀</el-tag>
            <span v-else class="muted">--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openGradeDialog(row)">在线批阅</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!subLoading && submits.length === 0" description="暂无提交" :image-size="80" />
    </el-dialog>

    <el-dialog v-model="gradeDialogVisible" title="在线批阅作业" width="92%" top="4vh" destroy-on-close>
      <div class="review-layout" v-if="currentSubmit">
        <div class="review-left">
          <div class="review-header">
            <div>
              <h3>{{ currentSubmit.studentName || '学生' }}的提交</h3>
              <p>{{ currentSubmit.studentNo || currentSubmit.studentId }} · {{ currentSubmit.submitTime || '--' }}</p>
            </div>
            <el-tag :type="currentSubmit.status === '已批改' ? 'success' : 'warning'">{{ currentSubmit.status || '待批改'
            }}</el-tag>
          </div>
          <div class="submit-content">{{ currentSubmit.content || '无文字内容' }}</div>
          <div class="file-tabs" v-if="submitFiles(currentSubmit).length">
            <el-tabs v-model="activeReviewFile">
              <el-tab-pane v-for="(file, index) in submitFiles(currentSubmit)" :key="file.url"
                :label="file.name || `文件${index + 1}`" :name="String(index)">
                <DocumentAnnotator v-model="feedbackMarks" :url="file.url" :name="file.name" :file-key="file.url" />
              </el-tab-pane>
            </el-tabs>
          </div>
          <el-empty v-else description="该学生未提交附件" :image-size="80" />
        </div>
        <div class="review-right">
          <el-form label-width="70px">
            <el-form-item label="分数"><el-input-number v-model="gradeForm.score" :min="0" :max="100"
                style="width:160px" /></el-form-item>
            <el-form-item label="评语"><el-input v-model="gradeForm.comment" type="textarea" :rows="4"
                placeholder="给学生的整体评语" /></el-form-item>
            <el-form-item label="优秀作业">
              <el-checkbox v-model="gradeForm.isExcellent">设为优秀作业，供选课学生参考</el-checkbox>
            </el-form-item>
          </el-form>
          <div class="review-mark-summary">
            <strong>文件贴纸批注</strong>
            <span>已添加 {{ feedbackMarks.length }} 条。请在左侧文件画面上点击，贴上对钩、错号或文字说明。</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="gradeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="grading" @click="doGrade">保存反馈并评分</el-button>
        <el-button type="success" plain :disabled="!hasNextSubmit" @click="openNextSubmit">批改下一个</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" :title="previewTitle" width="82%" top="5vh" destroy-on-close>
      <el-tabs v-if="previewFilesList.length > 1" v-model="activePreviewFile">
        <el-tab-pane v-for="(file, index) in previewFilesList" :key="file.url" :label="file.name || `文件${index + 1}`"
          :name="String(index)">
          <FilePreview :url="file.url" :name="file.name" />
        </el-tab-pane>
      </el-tabs>
      <FilePreview v-else-if="previewFilesList.length" :url="previewFilesList[0].url"
        :name="previewFilesList[0].name" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { School } from '@element-plus/icons-vue'
import { assignmentApi, courseApi, uploadApi } from '@/api'
import FilePreview from '@/components/FilePreview.vue'
import DocumentAnnotator from '@/components/DocumentAnnotator.vue'

const assignments = ref([])
const loading = ref(false)
const courseFilter = ref(null)
const chapterFilter = ref(null)
const courseList = ref([])
const filterChapters = ref([])
const formChapters = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)
const uploadLoading = ref(false)
const submitDialogVisible = ref(false)
const submits = ref([])
const subLoading = ref(false)
const gradeDialogVisible = ref(false)
const grading = ref(false)
const currentSubmit = ref(null)
const currentAssignmentId = ref(null)
const currentAssignmentTitle = ref('')
const feedbackMarks = ref([])
const activeReviewFile = ref('0')
const previewVisible = ref(false)
const previewTitle = ref('文件预览')
const previewFilesList = ref([])
const activePreviewFile = ref('0')

const form = reactive({ title: '', courseId: null, chapterId: null, content: '', deadline: '', totalScore: 100, attachmentUrl: '', files: [] })
const gradeForm = reactive({ score: 0, comment: '', isExcellent: false })
const currentSubmitIndex = computed(() => submits.value.findIndex(item => item.id === currentSubmit.value?.id))
const hasNextSubmit = computed(() => currentSubmitIndex.value >= 0 && currentSubmitIndex.value < submits.value.length - 1)

const normalizeFiles = (row, urlKey = 'attachmentUrl') => {
  if (row?.attachments?.length) {
    return row.attachments.map(att => ({ url: att.fileUrl, name: att.fileName, size: att.fileSize }))
  }
  return row?.[urlKey] ? [{ url: row[urlKey], name: row[urlKey].split('/').pop(), size: 0 }] : []
}
const assignmentFiles = (row) => normalizeFiles(row)
const submitFiles = (row) => normalizeFiles(row)
const parseMarks = (value) => {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    return []
  }
}

const fetchAssignments = async () => {
  loading.value = true
  try {
    const res = await assignmentApi.list({ page: 1, size: 100, courseId: courseFilter.value || undefined, chapterId: chapterFilter.value || undefined })
    if (res.data.code === 200) assignments.value = res.data.data?.records || []
  } finally {
    loading.value = false
  }
}

const fetchCourses = async () => {
  const res = await courseApi.list({ page: 1, size: 100 })
  if (res.data.code === 200) courseList.value = res.data.data?.records || []
}

const loadChaptersByCourse = async (courseId) => {
  if (!courseId) return []
  const res = await courseApi.detail(courseId)
  return res.data.code === 200 ? (res.data.data?.chapters || []) : []
}
const onFilterCourseChange = async () => {
  chapterFilter.value = null
  filterChapters.value = await loadChaptersByCourse(courseFilter.value)
  fetchAssignments()
}
const loadFormChapters = async () => {
  form.chapterId = null
  formChapters.value = await loadChaptersByCourse(form.courseId)
}
const chapterName = (chapterId) => [...filterChapters.value, ...formChapters.value].find(ch => ch.id === chapterId)?.chapterName || (chapterId ? '未知章节' : '未分配')

const uploadAttachment = async ({ file }) => {
  const raw = file instanceof File ? file : (file?.raw || file?.file || file)
  const formData = new FormData()
  formData.append('file', raw)
  uploadLoading.value = true
  try {
    const res = await uploadApi.uploadFile(formData)
    if (res.data.code === 200) {
      form.files.push({ url: res.data.data.url, name: res.data.data.name || raw.name, size: res.data.data.size || raw.size })
      ElMessage.success('文件上传成功')
    }
  } finally {
    uploadLoading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, { title: '', courseId: null, chapterId: null, content: '', deadline: '', totalScore: 100, attachmentUrl: '', files: [] })
  formChapters.value = []
}
const openCreate = () => {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}
const openEdit = async (row) => {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    title: row.title,
    courseId: row.courseId,
    chapterId: row.chapterId || null,
    content: row.content || '',
    deadline: row.deadline,
    totalScore: row.totalScore || 100,
    attachmentUrl: row.attachmentUrl || '',
    files: assignmentFiles(row)
  })
  formChapters.value = await loadChaptersByCourse(row.courseId)
  dialogVisible.value = true
}
const handleSubmit = async () => {
  if (!form.title || !form.courseId || !form.deadline) {
    ElMessage.warning('请填写标题、课程和截止时间')
    return
  }
  submitting.value = true
  try {
    const payload = { ...form, attachmentUrl: form.files[0]?.url || '', attachmentUrls: form.files.map(f => f.url), attachmentNames: form.files.map(f => f.name), attachmentSizes: form.files.map(f => f.size) }
    if (isEdit.value) await assignmentApi.update(editId.value, payload)
    else await assignmentApi.create(payload)
    ElMessage.success(isEdit.value ? '作业已更新' : '作业已布置')
    dialogVisible.value = false
    fetchAssignments()
  } finally {
    submitting.value = false
  }
}
const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除该作业？', '确认删除', { type: 'warning' })
  await assignmentApi.delete(id)
  ElMessage.success('删除成功')
  fetchAssignments()
}
const viewSubmits = async (row) => {
  currentAssignmentId.value = row.id
  currentAssignmentTitle.value = row.title || '作业附件'
  submitDialogVisible.value = true
  subLoading.value = true
  try {
    const res = await assignmentApi.getSubmitList(row.id, { page: 1, size: 100 })
    if (res.data.code === 200) submits.value = res.data.data?.records || []
  } finally {
    subLoading.value = false
  }
}
const downloadSubmitArchive = async () => {
  if (!currentAssignmentId.value) return
  const res = await assignmentApi.downloadArchive(currentAssignmentId.value)
  const blob = new Blob([res.data], { type: 'application/zip' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${currentAssignmentTitle.value || '作业附件'}-提交附件.zip`
  link.click()
  URL.revokeObjectURL(url)
}
const openGradeDialog = (row) => {
  currentSubmit.value = row
  gradeForm.score = row.score ?? 0
  gradeForm.comment = row.comment || ''
  gradeForm.isExcellent = Boolean(row.isExcellent)
  feedbackMarks.value = parseMarks(row.feedbackMarks)
  activeReviewFile.value = '0'
  gradeDialogVisible.value = true
}
const doGrade = async () => {
  if (!currentSubmit.value) return
  const submitId = currentSubmit.value.id
  grading.value = true
  try {
    await assignmentApi.grade(submitId, {
      score: gradeForm.score,
      comment: gradeForm.comment,
      feedbackMarks: JSON.stringify(feedbackMarks.value),
      isExcellent: gradeForm.isExcellent
    })
    ElMessage.success('提交成功')
    await refreshSubmits()
    const latest = submits.value.find(item => item.id === submitId)
    if (latest) openGradeDialog(latest)
    fetchAssignments()
  } finally {
    grading.value = false
  }
}
const refreshSubmits = async () => {
  if (!currentAssignmentId.value) return
  subLoading.value = true
  try {
    const res = await assignmentApi.getSubmitList(currentAssignmentId.value, { page: 1, size: 100 })
    if (res.data.code === 200) submits.value = res.data.data?.records || []
  } finally {
    subLoading.value = false
  }
}
const openNextSubmit = () => {
  if (!hasNextSubmit.value) return
  openGradeDialog(submits.value[currentSubmitIndex.value + 1])
}
const previewFiles = (files, title = '文件预览') => {
  previewFilesList.value = files
  previewTitle.value = title
  activePreviewFile.value = '0'
  previewVisible.value = true
}
const formatFileSize = (size) => {
  const n = Number(size || 0)
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(1)} MB`
}

const computeDeadlineStatus = (deadline) => {
  if (!deadline) return '未设定'
  return new Date(deadline) < new Date() ? '已结束' : '进行中'
}
const isOverdue = (deadline) => deadline && new Date(deadline) < new Date()
const formatDate = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : '未设定'

onMounted(async () => {
  await fetchCourses()
  if (courseList.value.length > 0) {
    courseFilter.value = courseList.value[0].id
    await onFilterCourseChange()
  } else {
    fetchAssignments()
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
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.panel {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, .05);
  padding: 24px;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.submit-tools {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.muted {
  color: #c0c4cc;
}

.file-stack {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) auto auto auto;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: #f5f7fa;
  border-radius: 8px;
}

.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #303133;
}

.file-size {
  color: #909399;
  font-size: 12px;
}

.review-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
}

.review-left,
.review-right {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.review-header h3 {
  margin: 0;
  color: #303133;
  font-size: 18px;
}

.review-header p {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

.submit-content {
  min-height: 96px;
  padding: 14px;
  border-radius: 8px;
  background: #f5f7fa;
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
}

.file-tabs :deep(.el-tabs__content) {
  border: 1px solid #e4e7ed;
  border-top: 0;
  border-radius: 0 0 8px 8px;
  overflow: hidden;
}

.review-mark-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #f8fbff;
  color: #606266;
  line-height: 1.6;
}

.review-mark-summary strong {
  color: #303133;
}

@media (max-width: 1100px) {
  .review-layout {
    grid-template-columns: 1fr;
  }
}

/* ===== 新表格样式 ===== */
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

.assign-name-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.assign-title {
  font-weight: 700;
  color: #303133;
  font-size: 14px;
}

.assign-status {
  font-weight: 700;
  color: #f56c6c;
  font-size: 12px;
}

.assign-chapter {
  color: #909399;
  font-size: 12px;
  margin-top: 6px;
}

.course-icon-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.course-icon {
  color: #fff;
  background: var(--bio-primary);
  border-radius: 50%;
  padding: 4px;
  color: white;
  font-size: 14px;
}

.course-text {
  color: var(--bio-primary);
  font-size: 13px;
}

.deadline-overdue {
  color: #f56c6c;
}

.submit-count {
  color: var(--bio-primary);
  font-weight: 600;
}

.submit-sep {
  color: #c0c4cc;
  margin: 0 2px;
}

.submit-total {
  color: #909399;
}

.score-value {
  color: #13C2C2;
  font-weight: 700;
  font-size: 15px;
}

.act-btn {
  margin: 0 2px;
}
</style>
