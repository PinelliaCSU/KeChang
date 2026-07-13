<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div>
        <h2 class="page-title">{{ project?.projectName || '实训详情' }}</h2>
        <p class="page-subtitle">
          {{ project?.startDate || '未设开始' }} ~ {{ project?.endDate || '未设结束' }} ·
          <el-tag :type="statusTagType(computeStatus(project?.startDate, project?.endDate))" size="small">
            {{ computeStatus(project?.startDate, project?.endDate) }}
          </el-tag>
        </p>
      </div>
      <div class="title-actions">
        <el-button @click="$router.push('/student/trainings')">返回列表</el-button>
      </div>
    </div>

    <div class="panel">
      <el-tabs v-model="activeTab">
        <!-- Tab 1: 实习计划 -->
        <el-tab-pane label="实习计划" name="plan">
          <div class="content-group">
            <h4 class="group-title">项目要求</h4>
            <div class="content-box" v-if="project?.description">{{ project.description }}</div>
            <el-empty v-else description="暂无计划内容" :image-size="60" />
          </div>
          <div class="content-group">
            <h4 class="group-title">计划附件</h4>
            <div v-if="planAttachments.length > 0" class="item-list">
              <div v-for="att in planAttachments" :key="att.id" class="content-item">
                <span class="item-kind">{{ getFileTypeLabel(att.fileName) }}</span>
                <span class="item-title">{{ att.fileName }}</span>
                <span class="item-meta">{{ formatFileSize(att.fileSize) }}</span>
                <div class="item-actions">
                  <el-button size="small" type="primary" link @click="previewFile(att)">预览</el-button>
                  <el-button size="small" link @click="downloadFile(att)">下载</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无附件" :image-size="60" />
          </div>
        </el-tab-pane>

        <!-- Tab 2: 资料 -->
        <el-tab-pane label="资料" name="material">
          <el-table v-if="materials.length > 0" :data="materials" stripe border v-loading="materialLoading">
            <el-table-column prop="title" label="资料标题" min-width="140" />
            <el-table-column label="文件名" min-width="160">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="previewFile(row)">{{ row.fileName || '--' }}</el-button>
              </template>
            </el-table-column>
            <el-table-column prop="fileSize" label="大小" width="100">
              <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="上传时间" width="170" />
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="previewFile(row)">预览</el-button>
                <el-button size="small" link @click="downloadFile(row)">下载</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!materialLoading && materials.length === 0" description="暂无资料" :image-size="60" />
          <PaginationWrapper v-model:page="matPage" v-model:size="matSize" :total="matTotal" @change="fetchMaterials" />
        </el-tab-pane>

        <!-- Tab 3: 任务 -->
        <el-tab-pane label="任务" name="task">
          <el-table v-if="tasks.length > 0" :data="tasks" stripe border v-loading="taskLoading">
            <el-table-column prop="sortOrder" label="序号" width="70" />
            <el-table-column prop="taskName" label="任务名称" min-width="150" />
            <el-table-column prop="description" label="任务说明" min-width="180" show-overflow-tooltip />
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
                <el-button size="small" type="primary" @click="openSubmit(row)">
                  {{ row.myReportId ? '查看/修改' : '提交' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!taskLoading && tasks.length === 0" description="暂无任务" :image-size="60" />
          <PaginationWrapper v-model:page="taskPage" v-model:size="taskSize" :total="taskTotal" @change="fetchTasks" />
        </el-tab-pane>

        <!-- Tab 4: 文档提交 -->
        <el-tab-pane label="文档提交" name="document">
          <el-table v-if="documents.length > 0" :data="documents" stripe border v-loading="docLoading">
            <el-table-column prop="sortOrder" label="序号" width="70" />
            <el-table-column prop="taskName" label="文档名称" min-width="150" />
            <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
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
                <el-button size="small" type="primary" @click="openSubmit(row)">
                  {{ row.myReportId ? '查看/修改' : '提交' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!docLoading && documents.length === 0" description="暂无文档要求" :image-size="60" />
          <PaginationWrapper v-model:page="docPage" v-model:size="docSize" :total="docTotal" @change="fetchDocuments" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 提交弹窗（任务与文档提交共用） -->
    <el-dialog v-model="submitDialogVisible" :title="currentTask ? currentTask.taskName : '提交'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="说明">
          <div class="task-desc">{{ currentTask?.description || '--' }}</div>
        </el-form-item>
        <el-form-item label="教师附件" v-if="(currentTask?.attachments && currentTask.attachments.length > 0) || currentTask?.attachmentUrl">
          <div style="display:flex;flex-direction:column;gap:4px">
            <template v-if="currentTask?.attachments && currentTask.attachments.length > 0">
              <el-link v-for="att in currentTask.attachments" :key="att.id" :href="att.fileUrl" target="_blank" type="primary">{{ att.fileName }}</el-link>
            </template>
            <el-link v-else-if="currentTask?.attachmentUrl" :href="currentTask.attachmentUrl" target="_blank" type="primary" style="word-break:break-all">{{ currentTask.attachmentUrl }}</el-link>
          </div>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="reportForm.content" type="textarea" :rows="6" :disabled="currentTask?.reportStatus === '已批改'" />
        </el-form-item>
        <el-form-item label="附件">
          <div v-if="reportForm.files.length > 0" class="file-list" style="margin-bottom:8px">
            <div v-for="(f, i) in reportForm.files" :key="i" class="file-item">
              <span class="file-name">{{ f.name }}</span>
              <span class="file-size">{{ formatFileSize(f.size) }}</span>
              <el-button size="small" type="danger" link @click="reportForm.files.splice(i, 1)" :disabled="currentTask?.reportStatus === '已批改'">移除</el-button>
            </div>
          </div>
          <div style="display:flex;gap:8px;align-items:center">
            <el-upload :http-request="uploadAttachment" :show-file-list="false" :disabled="currentTask?.reportStatus === '已批改'">
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
        <el-button v-if="currentTask?.reportStatus !== '已批改'" type="primary" :loading="submitting" @click="submitReport">
          {{ currentTask?.myReportId ? '保存修改' : '提交' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 预览弹窗 -->
    <el-dialog v-model="previewVisible" title="文件预览" width="80%" top="5vh" destroy-on-close>
      <FilePreview v-if="previewResource" :url="previewResource.fileUrl"
        :name="previewResource.fileName || previewResource.title"
        :type="previewResource.fileType" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { trainingApi, uploadApi } from '@/api'
import { computeStatus, statusTagType } from '@/utils/status'
import { usePagination } from '@/composables/usePagination'
import PaginationWrapper from '@/components/PaginationWrapper.vue'
import FilePreview from '@/components/FilePreview.vue'

const route = useRoute()
const router = useRouter()

const projectId = computed(() => Number(route.params.id))
const project = ref(null)
const activeTab = ref('plan')

// --- Plan ---
const planAttachments = ref([])

// --- Materials ---
const materials = ref([])
const materialLoading = ref(false)
const { page: matPage, size: matSize, total: matTotal } = usePagination({ defaultSize: 10 })

// --- Tasks ---
const tasks = ref([])
const taskLoading = ref(false)
const { page: taskPage, size: taskSize, total: taskTotal } = usePagination({ defaultSize: 10 })

// --- Documents ---
const documents = ref([])
const docLoading = ref(false)
const { page: docPage, size: docSize, total: docTotal } = usePagination({ defaultSize: 10 })

// --- Submit ---
const submitDialogVisible = ref(false)
const currentTask = ref(null)
const submitting = ref(false)
const reportForm = reactive({ content: '', files: [] })
const uploadLoading = ref(false)

// --- Preview ---
const previewVisible = ref(false)
const previewResource = ref(null)

const formatFileSize = (size) => {
  const n = Number(size || 0)
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(1)} MB`
}

const previewFile = (resource) => {
  previewResource.value = resource
  previewVisible.value = true
}

const downloadFile = (file) => {
  if (!file.fileUrl) return
  const link = document.createElement('a')
  link.href = file.fileUrl
  link.download = file.fileName || file.title || ''
  link.target = '_blank'
  link.rel = 'noopener noreferrer'
  link.click()
}

const getFileTypeLabel = (fileName) => {
  const ext = (fileName || '').split('.').pop()?.toLowerCase()
  const map = { pdf: 'PDF', doc: 'Word', docx: 'Word', ppt: 'PPT', pptx: 'PPT', xls: 'Excel', xlsx: 'Excel', png: '图片', jpg: '图片', jpeg: '图片', gif: '图片', mp4: '视频', mp3: '音频', zip: '压缩包', rar: '压缩包' }
  return map[ext] || '文件'
}

const statusType = (status) => ({ 已批改: 'success', 待批改: 'warning', 未提交: 'info' }[status || '未提交'] || 'info')

// --- Fetch ---
const fetchProject = async () => {
  try {
    const res = await trainingApi.getProject(projectId.value)
    if (res.data.code === 200) project.value = res.data.data
  } catch (e) {
    ElMessage.error('获取项目详情失败')
    router.push('/student/trainings')
  }
}

const fetchPlanAttachments = async () => {
  try {
    const res = await trainingApi.getPlanAttachments(projectId.value)
    if (res.data.code === 200) planAttachments.value = res.data.data || []
  } catch (e) { /* ignore */ }
}

const fetchMaterials = async () => {
  materialLoading.value = true
  try {
    const res = await trainingApi.listMaterials(projectId.value, { page: matPage.value, size: matSize.value })
    if (res.data.code === 200) {
      materials.value = res.data.data?.records || []
      matTotal.value = res.data.data?.total || 0
    }
  } finally { materialLoading.value = false }
}

const fetchTasks = async () => {
  taskLoading.value = true
  try {
    const res = await trainingApi.listTasks(projectId.value, { page: taskPage.value, size: taskSize.value, taskType: 'TASK' })
    if (res.data.code === 200) {
      tasks.value = res.data.data?.records || []
      taskTotal.value = res.data.data?.total || 0
    }
  } finally { taskLoading.value = false }
}

const fetchDocuments = async () => {
  docLoading.value = true
  try {
    const res = await trainingApi.listTasks(projectId.value, { page: docPage.value, size: docSize.value, taskType: 'DOCUMENT' })
    if (res.data.code === 200) {
      documents.value = res.data.data?.records || []
      docTotal.value = res.data.data?.total || 0
    }
  } finally { docLoading.value = false }
}

// --- Submit ---
const uploadAttachment = async ({ file }) => {
  const raw = file instanceof File ? file : (file?.raw || file?.file || file)
  const formData = new FormData()
  formData.append('file', raw)
  uploadLoading.value = true
  try {
    const res = await uploadApi.uploadFile(formData)
    if (res.data.code === 200) {
      reportForm.files.push({ url: res.data.data.url, name: res.data.data.name || raw.name, size: res.data.data.size || raw.size })
      ElMessage.success('上传成功')
    }
  } catch (e) { ElMessage.error('上传失败') }
  finally { uploadLoading.value = false }
}

const openSubmit = (task) => {
  currentTask.value = task
  reportForm.content = task.reportContent || ''
  reportForm.files = []
  submitDialogVisible.value = true
}

const submitReport = async () => {
  if (!reportForm.content.trim() && reportForm.files.length === 0) {
    ElMessage.warning('请填写内容或上传附件')
    return
  }
  submitting.value = true
  try {
    await trainingApi.submitReport(currentTask.value.id, {
      content: reportForm.content,
      attachmentUrl: reportForm.files.length > 0 ? reportForm.files[0].url : currentTask.value.reportAttachmentUrl || '',
      attachmentUrls: reportForm.files.map(f => f.url),
      attachmentNames: reportForm.files.map(f => f.name),
      attachmentSizes: reportForm.files.map(f => f.size)
    })
    ElMessage.success(currentTask.value.myReportId ? '已更新' : '提交成功')
    submitDialogVisible.value = false
    // Refresh both task lists
    fetchTasks()
    fetchDocuments()
  } finally { submitting.value = false }
}

onMounted(() => {
  fetchProject()
  fetchPlanAttachments()
  fetchMaterials()
  fetchTasks()
  fetchDocuments()
})
</script>

<style scoped>
.page-shell { display: flex; flex-direction: column; gap: 20px; }

/* ---------- 标题栏 ---------- */
.page-title-bar { display: flex; align-items: center; justify-content: space-between; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.page-subtitle { color: #909399; font-size: 13px; margin: 6px 0 0; display: flex; align-items: center; gap: 8px; }
.title-actions { display: flex; gap: 8px; }

/* ---------- 主面板 ---------- */
.panel { background: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,.05); padding: 24px; }
.panel :deep(.el-tabs__item) { font-size: 15px; font-weight: 600; }

/* ---------- 内容区块 ---------- */
.content-group { margin-top: 16px; }
.content-group:first-child { margin-top: 0; }
.group-title { margin: 0 0 10px; color: #303133; font-size: 14px; }
.content-box { background: #f5f7fa; border-radius: 8px; padding: 16px; color: #606266; line-height: 1.7; white-space: pre-wrap; }

/* ---------- 文件/资源列表 ---------- */
.item-list { display: flex; flex-direction: column; gap: 10px; }
.content-item { display: grid; grid-template-columns: 56px minmax(160px, 1fr) minmax(120px, auto) auto; gap: 12px; align-items: center; padding: 12px 14px; background: #fafafa; border: 1px solid #ebeef5; border-radius: 8px; }
.item-kind { color: var(--bio-primary); background: var(--bio-primary-soft); border-radius: 4px; padding: 3px 8px; text-align: center; font-size: 13px; }
.item-title { color: #303133; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-meta { color: #909399; font-size: 13px; }
.item-actions { display: flex; align-items: center; gap: 8px; }

/* ---------- 弹窗内文件列表 ---------- */
.file-list { display: flex; flex-direction: column; gap: 6px; }
.file-item { display: flex; align-items: center; gap: 10px; padding: 6px 10px; background: var(--bio-primary-soft); border-radius: 6px; font-size: 13px; }
.file-name { flex: 1; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-size { color: #909399; white-space: nowrap; font-size: 12px; }

/* ---------- 表单内描述 ---------- */
.task-desc, .comment-box { width: 100%; background: #f5f7fa; border-radius: 8px; padding: 12px; color: #606266; line-height: 1.6; }
</style>
