<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">实训中心</h2>
    </div>

    <div class="panel" v-if="trainings.length > 0">
      <div class="training-grid">
        <div class="training-card" v-for="t in trainings" :key="t.id">
          <div class="training-cover" :style="{ background: getCoverGradient(t.id) }">
            <span class="training-tag">{{ t.status || '实训' }}</span>
          </div>
          <div class="training-body">
            <h4 class="training-title">{{ t.projectName }}</h4>
            <p class="training-desc">{{ t.description || '暂无描述' }}</p>
            <div class="meta-row">
              <span>任务 {{ t.taskCount || 0 }}</span>
              <span>已交报告 {{ t.reportCount || 0 }}</span>
            </div>
          </div>
          <div class="training-footer">
            <span class="training-deadline">{{ t.endDate || '未设结束' }}</span>
            <el-button size="small" type="primary" plain @click="openProject(t)">进入实训</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="panel empty-panel" v-else>
      <EmptyIllustration variant="check" text="暂无实训项目" />
    </div>

    <el-dialog v-model="taskDialogVisible" :title="currentProject ? currentProject.projectName : '实训任务'" width="850px">
      <el-table v-if="tasks.length > 0 || taskLoading" :data="tasks" v-loading="taskLoading" stripe
        class="kechang-table" style="width:100%">
        <el-table-column prop="sortOrder" label="序号" width="70" />
        <el-table-column prop="taskName" label="任务名称" min-width="150" />
        <el-table-column prop="description" label="任务说明" min-width="190" show-overflow-tooltip />
        <el-table-column prop="deadline" label="截止时间" width="170" />
        <el-table-column prop="reportStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.reportStatus)" size="small">{{ row.reportStatus || '未提交' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="评分" width="90">
          <template #default="{ row }">{{ row.score != null ? row.score : '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openSubmit(row)">{{ row.myReportId ? '查看/修改' : '提交'
            }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!taskLoading && tasks.length === 0" description="暂无实训任务" />
      <template #footer><el-button @click="taskDialogVisible = false">关闭</el-button></template>
    </el-dialog>

    <el-dialog v-model="submitDialogVisible" :title="currentTask ? currentTask.taskName : '提交报告'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="任务说明">
          <div class="task-desc">{{ currentTask?.description || '--' }}</div>
        </el-form-item>
        <el-form-item label="教师附件"
          v-if="(currentTask?.attachments && currentTask.attachments.length > 0) || currentTask?.attachmentUrl">
          <div style="display:flex;flex-direction:column;gap:4px">
            <template v-if="currentTask?.attachments && currentTask.attachments.length > 0">
              <el-link v-for="att in currentTask.attachments" :key="att.id" :href="att.fileUrl" target="_blank"
                type="primary">{{ att.fileName }}</el-link>
            </template>
            <el-link v-else-if="currentTask?.attachmentUrl" :href="currentTask.attachmentUrl" target="_blank"
              type="primary" style="word-break:break-all">{{ currentTask.attachmentUrl }}</el-link>
          </div>
        </el-form-item>
        <el-form-item label="报告内容" required>
          <el-input v-model="reportForm.content" type="textarea" :rows="6"
            :disabled="currentTask?.reportStatus === '已批改'" />
        </el-form-item>
        <el-form-item label="附件">
          <div v-if="reportForm.files.length > 0" class="file-list" style="margin-bottom:8px">
            <div v-for="(f, i) in reportForm.files" :key="i" class="file-item">
              <span class="file-name">{{ f.name }}</span>
              <span class="file-size">{{ formatFileSize(f.size) }}</span>
              <el-button size="small" type="danger" link @click="reportForm.files.splice(i, 1)"
                :disabled="currentTask?.reportStatus === '已批改'">移除</el-button>
            </div>
          </div>
          <div style="display:flex;gap:8px;align-items:center;width:100%">
            <el-upload :http-request="uploadAttachment" :show-file-list="false"
              :disabled="currentTask?.reportStatus === '已批改'">
              <el-button :loading="uploadLoading" :disabled="currentTask?.reportStatus === '已批改'">+ 上传文件</el-button>
            </el-upload>
            <span style="font-size:12px;color:#909399">可上传多个文件</span>
          </div>
        </el-form-item>
        <el-form-item label="老师评分" v-if="currentTask?.score != null">
          <strong>{{ currentTask.score }}</strong>
        </el-form-item>
        <el-form-item label="老师评语" v-if="currentTask?.comment">
          <div class="comment-box">{{ currentTask.comment }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialogVisible = false">关闭</el-button>
        <el-button v-if="currentTask?.reportStatus !== '已批改'" type="primary" :loading="submitting"
          @click="submitReport">
          {{ currentTask?.myReportId ? '保存修改' : '提交报告' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { trainingApi, uploadApi } from '@/api'
import EmptyIllustration from '@/components/EmptyIllustration.vue'

const trainings = ref([])
const currentProject = ref(null)
const currentTask = ref(null)
const tasks = ref([])
const taskDialogVisible = ref(false)
const taskLoading = ref(false)
const submitDialogVisible = ref(false)
const submitting = ref(false)
const reportForm = reactive({ content: '', attachmentUrl: '', files: [] })
const uploadLoading = ref(false)

const gradientPalette = [
  'linear-gradient(135deg, #3b5998, #4c6da7)',
  'linear-gradient(135deg, #0d7377, #20c997)',
  'linear-gradient(135deg, #6b5b95, #8b5cf6)',
  'linear-gradient(135deg, #5f7f3f, #8aac62)'
]
const getCoverGradient = (id) => gradientPalette[(id - 1) % gradientPalette.length]
const statusType = (status) => ({ 已批改: 'success', 待批改: 'warning', 未提交: 'info' }[status || '未提交'] || 'info')

const fetchProjects = async () => {
  const res = await trainingApi.listProjects({ page: 1, size: 100 })
  if (res.data.code === 200) trainings.value = res.data.data?.records || []
}

const openProject = async (project) => {
  currentProject.value = project
  taskDialogVisible.value = true
  await fetchTasks()
}

const fetchTasks = async () => {
  if (!currentProject.value) return
  taskLoading.value = true
  try {
    const res = await trainingApi.listTasks(currentProject.value.id, { page: 1, size: 100 })
    if (res.data.code === 200) tasks.value = res.data.data?.records || []
  } finally {
    taskLoading.value = false
  }
}

const formatFileSize = (size) => {
  const n = Number(size || 0)
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(1)} MB`
}

const uploadAttachment = async ({ file }) => {
  const raw = file instanceof File ? file : (file?.raw || file?.file || file)
  const formData = new FormData()
  formData.append('file', raw)
  uploadLoading.value = true
  try {
    const res = await uploadApi.uploadFile(formData)
    if (res.data.code === 200) {
      reportForm.files.push({
        url: res.data.data.url,
        name: res.data.data.name || raw.name,
        size: res.data.data.size || raw.size
      })
      ElMessage.success('文件上传成功')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '文件上传失败')
  } finally {
    uploadLoading.value = false
  }
}

const openSubmit = (task) => {
  currentTask.value = task
  reportForm.content = task.reportContent || ''
  reportForm.attachmentUrl = task.reportAttachmentUrl || ''
  reportForm.files = []
  submitDialogVisible.value = true
}

const submitReport = async () => {
  if (!reportForm.content.trim() && reportForm.files.length === 0) {
    ElMessage.warning('请填写报告内容或上传附件')
    return
  }
  submitting.value = true
  try {
    await trainingApi.submitReport(currentTask.value.id, {
      content: reportForm.content,
      attachmentUrl: reportForm.files.length > 0 ? reportForm.files[0].url : reportForm.attachmentUrl,
      attachmentUrls: reportForm.files.map(f => f.url),
      attachmentNames: reportForm.files.map(f => f.name),
      attachmentSizes: reportForm.files.map(f => f.size)
    })
    ElMessage.success(currentTask.value.myReportId ? '报告已更新' : '报告提交成功')
    submitDialogVisible.value = false
    await fetchTasks()
    fetchProjects()
  } finally {
    submitting.value = false
  }
}

onMounted(fetchProjects)
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

.training-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.training-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s, transform 0.2s;
}

.training-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.training-cover {
  height: 80px;
  display: flex;
  align-items: flex-end;
  padding: 10px 14px;
}

.training-tag {
  font-size: 11px;
  color: #fff;
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 4px;
}

.training-body {
  padding: 14px;
}

.training-title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.course-name,
.training-desc,
.training-deadline,
.meta-row {
  font-size: 12px;
  color: #909399;
}

.training-desc {
  min-height: 36px;
  line-height: 1.5;
}

.meta-row {
  display: flex;
  gap: 12px;
}

.training-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px 14px;
}

.empty-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
}

.task-desc,
.comment-box {
  width: 100%;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px;
  color: #606266;
  line-height: 1.6;
}

.link-text {
  color: #409eff;
  word-break: break-all;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  background: #f0f9eb;
  border-radius: 6px;
  font-size: 13px;
}

.file-name {
  flex: 1;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  color: #909399;
  white-space: nowrap;
  font-size: 12px;
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
  background-color: rgba(163, 177, 138, 0.06);
}

:deep(.kechang-table .el-table__body tr.el-table__row:hover td) {
  background-color: rgba(163, 177, 138, 0.08) !important;
}
</style>
