<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">我的作业</h2>
      <el-select v-model="statusFilter" placeholder="状态筛选" style="width:140px" clearable>
        <el-option label="全部" value="" />
        <el-option label="未提交" value="pending" />
        <el-option label="已提交" value="submitted" />
        <el-option label="已批改" value="graded" />
      </el-select>
    </div>

    <div class="panel" v-if="visibleAssignments.length > 0">
      <el-table :data="visibleAssignments" stripe class="kechang-table" style="width:100%">
        <el-table-column label="作业标题" min-width="220">
          <template #default="{ row }">
            <div class="assign-name-cell">
              <div class="assign-title" @click="openDetail(row)">{{ row.title }}</div>
              <el-tag :type="submitTagType(row)" size="small" class="assign-tag">{{ submitLabel(row) }}</el-tag>
              <div class="assign-chapter">章节/单元：{{ chapterName(row) }}</div>
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
        <el-table-column label="截止时间" width="170">
          <template #default="{ row }">
            <span :class="{ 'deadline-overdue': isOverdue(row.deadline) && !row.submitted }">{{ formatDate(row.deadline)
            }}</span>
          </template>
        </el-table-column>
        <el-table-column label="总分" width="80" align="center">
          <template #default="{ row }">
            <span class="score-value">{{ row.totalScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="成绩" width="80" align="center">
          <template #default="{ row }">
            <span :class="row.myScore != null ? 'my-score' : 'muted'">{{ row.myScore != null ? row.myScore : '--'
            }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="panel empty-panel" v-else>
      <EmptyIllustration text="暂无作业任务" />
    </div>

    <el-dialog v-model="detailVisible" title="作业详情" width="88%" top="4vh" destroy-on-close>
      <div v-if="currentAssignment" class="detail-layout">
        <div class="detail-main">
          <div class="detail-head">
            <h3>{{ currentAssignment.title }}</h3>
            <el-tag :type="submitTagType(currentAssignment)">{{ submitLabel(currentAssignment) }}</el-tag>
          </div>
          <div class="detail-meta">
            <span>课程：{{ currentAssignment.courseName || '--' }}</span>
            <span>截止：{{ formatDate(currentAssignment.deadline) }}</span>
            <span>总分：{{ currentAssignment.totalScore || 100 }}</span>
            <span v-if="currentAssignment.myScore != null">得分：{{ currentAssignment.myScore }}</span>
          </div>

          <section class="section">
            <h4>作业说明</h4>
            <div class="text-box">{{ currentAssignment.content || '教师暂未填写作业说明' }}</div>
          </section>

          <section class="section" v-if="assignmentFiles(currentAssignment).length">
            <h4>教师资源</h4>
            <el-tabs v-model="activeAssignmentFile">
              <el-tab-pane v-for="(file, index) in assignmentFiles(currentAssignment)" :key="file.url"
                :label="file.name || `文件${index + 1}`" :name="String(index)">
                <FilePreview :url="file.url" :name="file.name" />
              </el-tab-pane>
            </el-tabs>
          </section>

          <section class="section" v-if="currentAssignment.submitted">
            <h4>我的提交</h4>
            <div class="text-box">{{ currentAssignment.submitContent || '无文字内容' }}</div>
            <div class="detail-meta">
              <span>提交时间：{{ formatDate(currentAssignment.submitTime) }}</span>
            </div>
            <el-tabs v-if="submitFiles(currentAssignment).length" v-model="activeSubmitFile">
              <el-tab-pane v-for="(file, index) in submitFiles(currentAssignment)" :key="file.url"
                :label="file.name || `文件${index + 1}`" :name="String(index)">
                <FilePreview :url="file.url" :name="file.name" />
              </el-tab-pane>
            </el-tabs>
          </section>

          <section class="section" v-if="feedbackFiles(currentAssignment).length">
            <h4>教师批注版文件</h4>
            <el-tabs v-model="activeFeedbackFile">
              <el-tab-pane v-for="(file, index) in feedbackFiles(currentAssignment)" :key="file.fileUrl || file.url"
                :label="file.fileName || file.name || `批注版${index + 1}`" :name="String(index)">
                <FilePreview :url="file.fileUrl || file.url" :name="file.fileName || file.name" />
              </el-tab-pane>
            </el-tabs>
          </section>

          <section class="section" v-if="currentAssignment.excellentSubmits?.length">
            <div class="section-title-row">
              <h4>优秀作业参考</h4>
              <el-tag type="success" size="small">{{ currentAssignment.excellentSubmits.length }} 份</el-tag>
            </div>
            <el-collapse class="excellent-list">
              <el-collapse-item v-for="submit in currentAssignment.excellentSubmits" :key="submit.id"
                :title="excellentTitle(submit)">
                <div class="excellent-card">
                  <div class="detail-meta">
                    <span>提交时间：{{ formatDate(submit.submitTime) }}</span>
                    <span v-if="submit.score != null">得分：{{ submit.score }}</span>
                  </div>
                  <div class="text-box">{{ submit.content || '无文字内容' }}</div>
                  <el-tabs v-if="excellentFiles(submit).length">
                    <el-tab-pane v-for="(file, index) in excellentFiles(submit)" :key="file.url"
                      :label="file.name || `文件${index + 1}`">
                      <FilePreview :url="file.url" :name="file.name" />
                    </el-tab-pane>
                  </el-tabs>
                </div>
              </el-collapse-item>
            </el-collapse>
          </section>

          <template v-if="!currentAssignment.submitted">
            <section class="section">
              <h4>提交内容</h4>
              <el-input v-model="submitForm.content" type="textarea" :rows="6" placeholder="请输入作业答案、说明或提交内容"
                :disabled="submitting || isOverdue(currentAssignment.deadline)" />
            </section>
            <section class="section">
              <h4>上传文件</h4>
              <div class="file-list" v-if="submitForm.files.length">
                <div v-for="(file, index) in submitForm.files" :key="file.url" class="file-item">
                  <span class="file-name">{{ file.name }}</span>
                  <span class="file-size">{{ formatFileSize(file.size) }}</span>
                  <el-button size="small" link type="primary" @click="previewLocalFile(file)">预览</el-button>
                  <el-button size="small" link type="danger" @click="submitForm.files.splice(index, 1)">移除</el-button>
                </div>
              </div>
              <el-upload :http-request="uploadAttachment" :show-file-list="false"
                :disabled="submitting || isOverdue(currentAssignment.deadline)">
                <el-button :loading="uploadLoading"
                  :disabled="submitting || isOverdue(currentAssignment.deadline)">上传文件</el-button>
              </el-upload>
              <p v-if="isOverdue(currentAssignment.deadline)" class="submit-tip">该作业已过截止时间，不能提交。</p>
            </section>
          </template>
        </div>

        <aside class="feedback-side">
          <h4>教师反馈</h4>
          <div v-if="currentAssignment.myScore != null" class="score-card">
            <span>得分</span>
            <strong>{{ currentAssignment.myScore }}</strong>
          </div>
          <div v-if="currentAssignment.gradeComment" class="text-box">{{ currentAssignment.gradeComment }}</div>
          <div v-if="feedbackMarks.length" class="feedback-mark-tip">
            老师已在你提交的文件上添加 {{ feedbackMarks.length }} 条贴纸批注，请在“教师批注版文件”中查看生成后的 PDF。
          </div>
          <el-empty v-if="currentAssignment.myScore == null && !currentAssignment.gradeComment && !feedbackMarks.length"
            description="暂未批改" :image-size="70" />
        </aside>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="currentAssignment && !currentAssignment.submitted" type="primary" :loading="submitting"
          :disabled="isOverdue(currentAssignment.deadline)" @click="submitAssignment">提交作业</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="文件预览" width="80%" top="5vh" destroy-on-close>
      <FilePreview v-if="previewFile" :url="previewFile.url" :name="previewFile.name" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { School } from '@element-plus/icons-vue'
import { assignmentApi, courseApi, uploadApi } from '@/api'
import EmptyIllustration from '@/components/EmptyIllustration.vue'
import FilePreview from '@/components/FilePreview.vue'

const assignments = ref([])
const statusFilter = ref('')
const detailVisible = ref(false)
const currentAssignment = ref(null)
const submitting = ref(false)
const uploadLoading = ref(false)
const submitForm = reactive({ content: '', files: [] })
const activeAssignmentFile = ref('0')
const activeSubmitFile = ref('0')
const activeFeedbackFile = ref('0')
const feedbackMarks = ref([])
const previewVisible = ref(false)
const previewFile = ref(null)

const isOverdue = (deadline) => deadline && new Date(deadline) < new Date()
const formatDate = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : '未设定'
const normalizeFiles = (row, urlKey = 'attachmentUrl') => {
  if (row?.attachments?.length) return row.attachments.map(att => ({ url: att.fileUrl, name: att.fileName, size: att.fileSize }))
  return row?.[urlKey] ? [{ url: row[urlKey], name: row[urlKey].split('/').pop(), size: 0 }] : []
}
const assignmentFiles = (row) => normalizeFiles(row)
const submitFiles = (row) => {
  if (row?.submitAttachments?.length) return row.submitAttachments.map(att => ({ url: att.fileUrl, name: att.fileName, size: att.fileSize }))
  return row?.submitAttachmentUrl ? [{ url: row.submitAttachmentUrl, name: row.submitAttachmentUrl.split('/').pop(), size: 0 }] : []
}
const feedbackFiles = (row) => row?.feedbackAttachments || []
const excellentFiles = (row) => {
  if (row?.attachments?.length) return row.attachments.map(att => ({ url: att.fileUrl, name: att.fileName, size: att.fileSize }))
  return row?.attachmentUrl ? [{ url: row.attachmentUrl, name: row.attachmentUrl.split('/').pop(), size: 0 }] : []
}
const excellentTitle = (row) => {
  const owner = [row.studentNo, row.studentName || '同学'].filter(Boolean).join(' - ')
  const score = row.score != null ? ` · ${row.score}分` : ''
  return `${owner || '优秀作业'}${score}`
}
const parseMarks = (value) => {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    return []
  }
}

const submitLabel = (row) => {
  if (row.myScore != null || row.mySubmitStatus === '已批改') return '已批改'
  if (row.submitted) return '已提交'
  if (isOverdue(row.deadline)) return '已截止'
  return '未提交'
}
const submitTagType = (row) => {
  if (row.myScore != null || row.mySubmitStatus === '已批改') return 'success'
  if (row.submitted) return 'warning'
  if (isOverdue(row.deadline)) return 'info'
  return 'danger'
}
const visibleAssignments = computed(() => {
  if (!statusFilter.value) return assignments.value
  if (statusFilter.value === 'pending') return assignments.value.filter(a => !a.submitted && !isOverdue(a.deadline))
  if (statusFilter.value === 'submitted') return assignments.value.filter(a => a.submitted && a.myScore == null)
  return assignments.value.filter(a => a.myScore != null || a.mySubmitStatus === '已批改')
})

const openDetail = (row) => {
  currentAssignment.value = row
  submitForm.content = ''
  submitForm.files = []
  activeAssignmentFile.value = '0'
  activeSubmitFile.value = '0'
  activeFeedbackFile.value = '0'
  feedbackMarks.value = parseMarks(row.feedbackMarks)
  detailVisible.value = true
}
const previewLocalFile = (file) => {
  previewFile.value = file
  previewVisible.value = true
}
const uploadAttachment = async ({ file }) => {
  const raw = file instanceof File ? file : (file?.raw || file?.file || file)
  const formData = new FormData()
  formData.append('file', raw)
  uploadLoading.value = true
  try {
    const res = await uploadApi.uploadFile(formData)
    if (res.data.code === 200) {
      submitForm.files.push({ url: res.data.data.url, name: res.data.data.name || raw.name, size: res.data.data.size || raw.size })
      ElMessage.success('文件上传成功')
    }
  } finally {
    uploadLoading.value = false
  }
}
const chapterCache = reactive({})
const loadChaptersForCourse = async (courseId) => {
  if (!courseId || chapterCache[courseId]) return
  try {
    const res = await courseApi.detail(courseId)
    if (res.data.code === 200) {
      chapterCache[courseId] = res.data.data?.chapters || []
    }
  } catch { /* ignore */ }
}
const chapterName = (row) => {
  if (row.chapterName) return row.chapterName
  if (!row.chapterId) return '未分配'
  const chapters = chapterCache[row.courseId] || []
  const found = chapters.find(ch => ch.id === row.chapterId)
  return found?.chapterName || (row.chapterId ? '未知章节' : '未分配')
}

const fetchAssignments = async () => {
  const res = await assignmentApi.list({ page: 1, size: 100 })
  if (res.data.code === 200) {
    assignments.value = res.data.data?.records || []
    // 异步加载各课程的章节信息，用于解析章节名称
    const courseIds = [...new Set(assignments.value.map(a => a.courseId).filter(Boolean))]
    await Promise.all(courseIds.map(id => loadChaptersForCourse(id)))
  }
}
const submitAssignment = async () => {
  if (!currentAssignment.value) return
  if (!submitForm.content.trim() && submitForm.files.length === 0) {
    ElMessage.warning('请填写作业内容或上传文件')
    return
  }
  submitting.value = true
  try {
    await assignmentApi.submit(currentAssignment.value.id, {
      content: submitForm.content,
      attachmentUrl: submitForm.files[0]?.url || '',
      attachmentUrls: submitForm.files.map(f => f.url),
      attachmentNames: submitForm.files.map(f => f.name),
      attachmentSizes: submitForm.files.map(f => f.size)
    })
    ElMessage.success('作业提交成功')
    detailVisible.value = false
    await fetchAssignments()
  } finally {
    submitting.value = false
  }
}
const formatFileSize = (size) => {
  const n = Number(size || 0)
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(1)} MB`
}

onMounted(fetchAssignments)
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

.link-text {
  color: var(--bio-primary);
  cursor: pointer;
  font-weight: 500;
}

.text-danger {
  color: #f56c6c;
  font-weight: 500;
}

.empty-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
}

.detail-main,
.feedback-side {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feedback-side {
  border-left: 1px solid #ebeef5;
  padding-left: 18px;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.detail-head h3,
.section h4,
.feedback-side h4 {
  margin: 0;
  color: #303133;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: #606266;
  font-size: 13px;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.text-box {
  padding: 12px;
  border-radius: 8px;
  background: #f5f7fa;
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
}

.score-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px;
  border-radius: 8px;
  background: var(--bio-primary-soft);
  color: var(--bio-primary);
}

.score-card strong {
  font-size: 26px;
}

.feedback-mark-tip {
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #f8fbff;
  color: #606266;
  line-height: 1.6;
}

.excellent-list {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.excellent-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px 0 10px;
}

.submit-tip {
  margin: 8px 0 0;
  color: #f56c6c;
  font-size: 13px;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) auto auto auto;
  gap: 10px;
  align-items: center;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f5f7fa;
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

@media (max-width: 1080px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .feedback-side {
    border-left: 0;
    padding-left: 0;
  }
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

.assign-name-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.assign-title {
  font-weight: 700;
  color: #303133;
  font-size: 14px;
  cursor: pointer;
}

.assign-title:hover {
  color: var(--bio-primary);
}

.assign-tag {
  margin-top: 4px;
  align-self: flex-start;
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
  font-size: 14px;
}

.course-text {
  color: var(--bio-primary);
  font-size: 13px;
}

.deadline-overdue {
  color: #f56c6c;
  font-weight: 500;
}

.score-value {
  color: #13C2C2;
  font-weight: 700;
  font-size: 15px;
}

.my-score {
  color: var(--bio-primary);
  font-weight: 700;
  font-size: 15px;
}
</style>
