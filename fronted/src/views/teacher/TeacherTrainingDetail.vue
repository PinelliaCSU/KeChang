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
        <el-button @click="$router.push('/teacher/trainings')">返回列表</el-button>
      </div>
    </div>

    <div class="panel">
      <el-tabs v-model="activeTab">
        <!-- Tab 1: 实习计划 -->
        <el-tab-pane label="实习计划" name="plan">
          <div class="tab-toolbar" v-if="isOwner">
            <el-button type="primary" size="small" @click="openPlanEdit">编辑计划</el-button>
          </div>
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
          <div class="tab-toolbar" v-if="isOwner">
            <el-button type="primary" size="small" @click="openMaterialUpload">上传资料</el-button>
          </div>
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
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="previewFile(row)">预览</el-button>
                <el-button size="small" link @click="downloadFile(row)">下载</el-button>
                <el-button v-if="isOwner" size="small" type="danger" link @click="handleDeleteMaterial(row)" style="margin-left:8px">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!materialLoading && materials.length === 0" description="暂无资料" :image-size="60" />
        </el-tab-pane>

        <!-- Tab 3: 任务 -->
        <el-tab-pane label="任务" name="task">
          <div class="tab-toolbar" v-if="isOwner">
            <el-button type="primary" size="small" @click="openCreateTask('TASK')">添加任务</el-button>
          </div>
          <el-table v-if="tasks.length > 0" :data="tasks" stripe border v-loading="taskLoading">
            <el-table-column prop="sortOrder" label="序号" width="70" />
            <el-table-column prop="taskName" label="任务名称" min-width="150" />
            <el-table-column prop="description" label="任务说明" min-width="180" show-overflow-tooltip />
            <el-table-column prop="deadline" label="截止时间" width="170" />
            <el-table-column prop="reportCount" label="提交数" width="80" />
            <el-table-column label="操作" width="260">
              <template #default="{ row }">
                <el-button size="small" @click="viewReports(row)">查看提交</el-button>
                <el-button v-if="isOwner" size="small" @click="openEditTask(row)">编辑</el-button>
                <el-button v-if="isOwner" size="small" type="danger" @click="handleDeleteTask(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!taskLoading && tasks.length === 0" description="暂无任务" :image-size="60" />
        </el-tab-pane>

        <!-- Tab 4: 文档提交 -->
        <el-tab-pane label="文档提交" name="document">
          <div class="tab-toolbar" v-if="isOwner">
            <el-button type="primary" size="small" @click="openCreateTask('DOCUMENT')">添加文档要求</el-button>
          </div>
          <el-table v-if="documents.length > 0" :data="documents" stripe border v-loading="docLoading">
            <el-table-column prop="sortOrder" label="序号" width="70" />
            <el-table-column prop="taskName" label="文档名称" min-width="150" />
            <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
            <el-table-column prop="deadline" label="截止时间" width="170" />
            <el-table-column prop="reportCount" label="提交数" width="80" />
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button size="small" @click="viewReports(row)">查看提交</el-button>
                <el-button v-if="isOwner" size="small" @click="openEditTask(row)">编辑</el-button>
                <el-button v-if="isOwner" size="small" type="danger" @click="handleDeleteTask(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!docLoading && documents.length === 0" description="暂无文档要求" :image-size="60" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 编辑计划弹窗 -->
    <el-dialog v-model="planEditVisible" title="编辑实习计划" width="560px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="项目要求">
          <el-input v-model="planForm.description" type="textarea" :rows="6" placeholder="输入实习计划要求..." />
        </el-form-item>
        <el-form-item label="附件">
          <div v-if="planForm.files.length > 0" class="file-list" style="margin-bottom:8px">
            <div v-for="(f, i) in planForm.files" :key="i" class="file-item">
              <span class="file-name">{{ f.name }}</span>
              <span class="file-size">{{ formatFileSize(f.size) }}</span>
              <el-button size="small" type="danger" link @click="planForm.files.splice(i, 1)">移除</el-button>
            </div>
          </div>
          <div style="display:flex;gap:8px;align-items:center">
            <el-upload :http-request="uploadPlanFile" :show-file-list="false" :disabled="planUploadLoading">
              <el-button :loading="planUploadLoading" :disabled="planUploadLoading">上传文件</el-button>
            </el-upload>
            <span style="font-size:12px;color:#909399">可上传多个文件</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planEditVisible = false">取消</el-button>
        <el-button type="primary" :loading="planSubmitting" :disabled="planUploadLoading" @click="handlePlanSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 上传资料弹窗 -->
    <el-dialog v-model="materialDialogVisible" title="上传资料" width="480px" destroy-on-close>
      <el-form :model="materialForm" label-width="90px">
        <el-form-item label="资料标题" required>
          <el-input v-model="materialForm.title" placeholder="如：第一章讲义" />
        </el-form-item>
        <el-form-item label="文件" required>
          <div v-if="materialForm.fileUrl" class="file-item" style="margin-bottom:8px">
            <span class="file-name">{{ materialForm.fileName }}</span>
            <el-button size="small" type="danger" link @click="clearMaterialFile">移除</el-button>
          </div>
          <el-upload v-else :http-request="uploadMaterialFile" :show-file-list="false" :disabled="materialUploadLoading">
            <el-button :loading="materialUploadLoading" :disabled="materialUploadLoading">选择文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="materialDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="materialSubmitting" :disabled="materialUploadLoading" @click="handleMaterialSubmit">上传</el-button>
      </template>
    </el-dialog>

    <!-- 创建/编辑任务弹窗 -->
    <el-dialog v-model="taskFormVisible" :title="isEditTask ? '编辑' : '发布' + (taskFormType === 'DOCUMENT' ? '文档要求' : '任务')" width="500px" destroy-on-close>
      <el-form :model="taskForm" label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="taskForm.taskName" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="taskForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="附件">
          <div v-if="taskForm.files.length > 0" class="file-list" style="margin-bottom:8px">
            <div v-for="(f, i) in taskForm.files" :key="i" class="file-item">
              <span class="file-name">{{ f.name }}</span>
              <span class="file-size">{{ formatFileSize(f.size) }}</span>
              <el-button size="small" type="danger" link @click="taskForm.files.splice(i, 1)">移除</el-button>
            </div>
          </div>
          <div style="display:flex;gap:8px;align-items:center">
            <el-upload :http-request="uploadTaskFile" :show-file-list="false" :disabled="taskUploadLoading">
              <el-button :loading="taskUploadLoading" :disabled="taskUploadLoading">上传文件</el-button>
            </el-upload>
            <span style="font-size:12px;color:#909399">可上传多个文件</span>
          </div>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="taskForm.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="taskForm.sortOrder" :min="1" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="taskSubmitting" :disabled="taskUploadLoading" @click="handleTaskSubmit">
          {{ isEditTask ? '保存' : '发布' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 报告列表弹窗 -->
    <el-dialog v-model="reportDialogVisible" :title="currentTask ? '提交列表 - ' + currentTask.taskName : '提交列表'" width="88%" class="report-list-dialog">
      <div class="dialog-toolbar">
        <span class="toolbar-muted">共 {{ reports.length }} 份提交</span>
        <el-button size="small" :disabled="reports.length === 0" @click="exportTrainingReports">导出实训评分</el-button>
      </div>
      <el-table v-if="reports.length > 0 || reportLoading" :data="reports" v-loading="reportLoading" stripe border table-layout="fixed" class="report-list-table">
        <el-table-column label="学生" min-width="145">
          <template #default="{ row }">
            <div class="student-cell">
              <strong>{{ row.studentName || '学生' }}</strong>
              <span>{{ row.studentNo || row.studentId || '--' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="reportContent" label="内容" min-width="185" show-overflow-tooltip />
        <el-table-column label="附件" width="115">
          <template #default="{ row }">
            <div v-if="(row.attachments && row.attachments.length > 0) || row.attachmentUrl" style="display:flex;flex-wrap:wrap;gap:4px">
              <template v-if="row.attachments && row.attachments.length > 0">
                <el-link v-for="(att, i) in row.attachments" :key="att.id" :href="att.fileUrl" target="_blank" type="primary" style="font-size:12px">文件{{ i + 1 }}</el-link>
              </template>
              <el-link v-else-if="row.attachmentUrl" :href="row.attachmentUrl" target="_blank" type="primary" style="font-size:12px">文件1</el-link>
            </div>
            <span v-else style="color:#c0c4cc">--</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="155" show-overflow-tooltip />
        <el-table-column prop="score" label="评分" width="70" align="center">
          <template #default="{ row }">{{ row.score != null ? row.score : '未评' }}</template>
        </el-table-column>
        <el-table-column prop="comment" label="评语" min-width="125" show-overflow-tooltip>
          <template #default="{ row }">{{ row.comment || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="76" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openReportGrade(row)">评分</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!reportLoading && reports.length === 0" description="暂无学生提交" />
      <template #footer><el-button @click="reportDialogVisible = false">关闭</el-button></template>
    </el-dialog>

    <!-- 评分弹窗 -->
    <el-dialog v-model="reportGradeVisible" title="报告评分" width="480px">
      <el-form label-width="80px">
        <el-form-item label="学生">
          <span>{{ currentReport?.studentName || '--' }}（{{ currentReport?.studentNo || currentReport?.studentId }}）</span>
        </el-form-item>
        <el-form-item label="内容">
          <div class="report-preview">{{ currentReport?.reportContent || '--' }}</div>
        </el-form-item>
        <el-form-item label="附件" v-if="(currentReport?.attachments && currentReport.attachments.length > 0) || currentReport?.attachmentUrl">
          <div style="display:flex;flex-direction:column;gap:4px">
            <template v-if="currentReport?.attachments && currentReport.attachments.length > 0">
              <el-link v-for="att in currentReport.attachments" :key="att.id" :href="att.fileUrl" target="_blank" type="primary">{{ att.fileName }}</el-link>
            </template>
            <el-link v-else-if="currentReport?.attachmentUrl" :href="currentReport.attachmentUrl" target="_blank" type="primary">查看附件</el-link>
          </div>
        </el-form-item>
        <el-form-item label="分数"><el-input-number v-model="reportScore" :min="0" :max="100" /></el-form-item>
        <el-form-item label="评语"><el-input v-model="reportComment" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportGradeVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportGrading" @click="doReportGrade">确认</el-button>
        <el-button type="success" plain :disabled="!hasNextReport" @click="openNextReport">评分下一个</el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { trainingApi, uploadApi } from '@/api'
import { computeStatus, statusTagType } from '@/utils/status'
import { useUserStore } from '@/stores/user'
import FilePreview from '@/components/FilePreview.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const projectId = computed(() => Number(route.params.id))
const project = ref(null)
const isOwner = ref(false)
const activeTab = ref('plan')

// --- Plan ---
const planAttachments = ref([])
const planEditVisible = ref(false)
const planSubmitting = ref(false)
const planUploadLoading = ref(false)
const planForm = reactive({ description: '', files: [] })

// --- Materials ---
const materials = ref([])
const materialLoading = ref(false)
const materialDialogVisible = ref(false)
const materialSubmitting = ref(false)
const materialUploadLoading = ref(false)
const materialForm = reactive({ title: '', fileUrl: '', fileName: '', fileSize: 0 })

// --- Tasks ---
const tasks = ref([])
const taskLoading = ref(false)

// --- Documents ---
const documents = ref([])
const docLoading = ref(false)

// --- Shared task form ---
const taskFormVisible = ref(false)
const taskSubmitting = ref(false)
const taskUploadLoading = ref(false)
const isEditTask = ref(false)
const editingTaskId = ref(null)
const taskFormType = ref('TASK')
const taskForm = reactive({ taskName: '', description: '', deadline: '', sortOrder: 1, files: [], taskType: 'TASK' })

// --- Reports ---
const reportDialogVisible = ref(false)
const reports = ref([])
const reportLoading = ref(false)
const currentTask = ref(null)
const reportGradeVisible = ref(false)
const currentReport = ref(null)
const reportScore = ref(0)
const reportComment = ref('')
const reportGrading = ref(false)
const currentReportIndex = computed(() => reports.value.findIndex(item => item.id === currentReport.value?.id))
const hasNextReport = computed(() => currentReportIndex.value >= 0 && currentReportIndex.value < reports.value.length - 1)

// --- Preview ---
const previewVisible = ref(false)
const previewResource = ref(null)

// --- Helpers ---
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

// --- Fetch project ---
const fetchProject = async () => {
  try {
    const res = await trainingApi.getProject(projectId.value)
    if (res.data.code === 200) {
      project.value = res.data.data
      isOwner.value = userStore.userId === project.value.createBy || userStore.isTeacher
    }
  } catch (e) {
    ElMessage.error('获取项目详情失败')
    router.push('/teacher/trainings')
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
    const res = await trainingApi.listMaterials(projectId.value, { page: 1, size: 100 })
    if (res.data.code === 200) materials.value = res.data.data?.records || []
  } finally { materialLoading.value = false }
}

const fetchTasks = async () => {
  taskLoading.value = true
  try {
    const res = await trainingApi.listTasks(projectId.value, { page: 1, size: 100, taskType: 'TASK' })
    if (res.data.code === 200) tasks.value = res.data.data?.records || []
  } finally { taskLoading.value = false }
}

const fetchDocuments = async () => {
  docLoading.value = true
  try {
    const res = await trainingApi.listTasks(projectId.value, { page: 1, size: 100, taskType: 'DOCUMENT' })
    if (res.data.code === 200) documents.value = res.data.data?.records || []
  } finally { docLoading.value = false }
}

// --- Plan ---
const openPlanEdit = () => {
  planForm.description = project.value?.description || ''
  planForm.files = []
  planEditVisible.value = true
}

const uploadPlanFile = async ({ file }) => {
  const raw = file instanceof File ? file : (file?.raw || file?.file || file)
  const formData = new FormData()
  formData.append('file', raw)
  planUploadLoading.value = true
  try {
    const res = await uploadApi.uploadFile(formData)
    if (res.data.code === 200) {
      planForm.files.push({ url: res.data.data.url, name: res.data.data.name || raw.name, size: res.data.data.size || raw.size })
      ElMessage.success('上传成功')
    }
  } catch (e) { ElMessage.error('上传失败') }
  finally { planUploadLoading.value = false }
}

const handlePlanSubmit = async () => {
  planSubmitting.value = true
  try {
    await trainingApi.updatePlan(projectId.value, {
      description: planForm.description,
      attachmentUrls: planForm.files.map(f => f.url),
      attachmentNames: planForm.files.map(f => f.name),
      attachmentSizes: planForm.files.map(f => f.size)
    })
    ElMessage.success('计划已更新')
    planEditVisible.value = false
    await Promise.all([fetchProject(), fetchPlanAttachments()])
  } finally { planSubmitting.value = false }
}

// --- Materials ---
const openMaterialUpload = () => {
  materialForm.title = ''
  materialForm.fileUrl = ''
  materialForm.fileName = ''
  materialForm.fileSize = 0
  materialDialogVisible.value = true
}

const uploadMaterialFile = async ({ file }) => {
  const raw = file instanceof File ? file : (file?.raw || file?.file || file)
  const formData = new FormData()
  formData.append('file', raw)
  materialUploadLoading.value = true
  try {
    const res = await uploadApi.uploadFile(formData)
    if (res.data.code === 200) {
      materialForm.fileUrl = res.data.data.url
      materialForm.fileName = res.data.data.name || raw.name
      materialForm.fileSize = res.data.data.size || raw.size
      ElMessage.success('文件上传成功')
    }
  } catch (e) { ElMessage.error('上传失败') }
  finally { materialUploadLoading.value = false }
}

const clearMaterialFile = () => {
  materialForm.fileUrl = ''
  materialForm.fileName = ''
  materialForm.fileSize = 0
}

const handleMaterialSubmit = async () => {
  if (!materialForm.title) { ElMessage.warning('请填写资料标题'); return }
  if (!materialForm.fileUrl) { ElMessage.warning('请上传文件'); return }
  materialSubmitting.value = true
  try {
    await trainingApi.uploadMaterial(projectId.value, { ...materialForm })
    ElMessage.success('资料上传成功')
    materialDialogVisible.value = false
    fetchMaterials()
  } finally { materialSubmitting.value = false }
}

const handleDeleteMaterial = async (row) => {
  await ElMessageBox.confirm('确定删除该资料？', '删除资料', { type: 'warning' })
  await trainingApi.deleteMaterial(row.id)
  ElMessage.success('资料已删除')
  fetchMaterials()
}

// --- Tasks & Documents ---
const openCreateTask = (taskType) => {
  isEditTask.value = false
  editingTaskId.value = null
  taskFormType.value = taskType
  taskForm.taskName = ''
  taskForm.description = ''
  taskForm.deadline = ''
  taskForm.sortOrder = taskType === 'DOCUMENT' ? documents.value.length + 1 : tasks.value.length + 1
  taskForm.files = []
  taskForm.taskType = taskType
  taskFormVisible.value = true
}

const openEditTask = (task) => {
  isEditTask.value = true
  editingTaskId.value = task.id
  taskFormType.value = task.taskType || 'DOCUMENT'
  taskForm.taskName = task.taskName
  taskForm.description = task.description || ''
  taskForm.deadline = task.deadline || ''
  taskForm.sortOrder = task.sortOrder || 1
  taskForm.files = []
  taskForm.taskType = task.taskType || 'DOCUMENT'
  taskFormVisible.value = true
}

const uploadTaskFile = async ({ file }) => {
  const raw = file instanceof File ? file : (file?.raw || file?.file || file)
  const formData = new FormData()
  formData.append('file', raw)
  taskUploadLoading.value = true
  try {
    const res = await uploadApi.uploadFile(formData)
    if (res.data.code === 200) {
      taskForm.files.push({ url: res.data.data.url, name: res.data.data.name || raw.name, size: res.data.data.size || raw.size })
      ElMessage.success('上传成功')
    }
  } catch (e) { ElMessage.error('上传失败') }
  finally { taskUploadLoading.value = false }
}

const handleTaskSubmit = async () => {
  if (!taskForm.taskName) { ElMessage.warning('请填写名称'); return }
  taskSubmitting.value = true
  try {
    const payload = {
      taskName: taskForm.taskName,
      description: taskForm.description,
      deadline: taskForm.deadline,
      sortOrder: taskForm.sortOrder,
      taskType: taskForm.taskType,
      attachmentUrl: taskForm.files.length > 0 ? taskForm.files[0].url : '',
      attachmentUrls: taskForm.files.map(f => f.url),
      attachmentNames: taskForm.files.map(f => f.name),
      attachmentSizes: taskForm.files.map(f => f.size)
    }
    if (isEditTask.value) {
      await trainingApi.updateTask(editingTaskId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await trainingApi.createTask(projectId.value, payload)
      ElMessage.success('发布成功')
    }
    taskFormVisible.value = false
    if (taskForm.taskType === 'DOCUMENT') fetchDocuments()
    else fetchTasks()
  } finally { taskSubmitting.value = false }
}

const handleDeleteTask = async (task) => {
  await ElMessageBox.confirm('删除会同时删除学生报告，确定继续？', '删除', { type: 'warning' })
  await trainingApi.deleteTask(task.id)
  ElMessage.success('已删除')
  if (task.taskType === 'DOCUMENT') fetchDocuments()
  else fetchTasks()
}

// --- Reports ---
const viewReports = async (task) => {
  currentTask.value = task
  reportDialogVisible.value = true
  reportLoading.value = true
  try {
    const res = await trainingApi.listReports(task.id, { page: 1, size: 100 })
    if (res.data.code === 200) reports.value = res.data.data?.records || []
  } finally { reportLoading.value = false }
}

const openReportGrade = (row) => {
  currentReport.value = row
  reportScore.value = row.score ?? 0
  reportComment.value = row.comment || ''
  reportGradeVisible.value = true
}

const doReportGrade = async () => {
  reportGrading.value = true
  try {
    await trainingApi.gradeReport(currentReport.value.id, { score: reportScore.value, comment: reportComment.value })
    ElMessage.success('评分成功')
    const reportId = currentReport.value.id
    await refreshReports()
    const latest = reports.value.find(item => item.id === reportId)
    if (latest) {
      currentReport.value = latest
      reportScore.value = latest.score ?? reportScore.value
      reportComment.value = latest.comment || reportComment.value
    }
    // Refresh task counts
    if (currentTask.value?.taskType === 'DOCUMENT') fetchDocuments()
    else fetchTasks()
  } finally { reportGrading.value = false }
}

const refreshReports = async () => {
  if (!currentTask.value?.id) return
  reportLoading.value = true
  try {
    const res = await trainingApi.listReports(currentTask.value.id, { page: 1, size: 100 })
    if (res.data.code === 200) reports.value = res.data.data?.records || []
  } finally { reportLoading.value = false }
}

const openNextReport = () => {
  if (!hasNextReport.value) return
  openReportGrade(reports.value[currentReportIndex.value + 1])
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

const exportTrainingReports = () => {
  if (!reports.value.length) return
  const rows = [
    ['学号', '姓名', '提交内容', '提交时间', '评分', '评语'],
    ...reports.value.map(item => [
      item.studentNo || '',
      item.studentName || '',
      item.reportContent || '',
      item.submitTime || '',
      item.score != null ? item.score : '未评',
      item.comment || ''
    ])
  ]
  downloadCsv(`${currentTask.value?.taskName || '实训任务'}-评分表.csv`, rows)
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

/* ---------- Tabs 工具栏 ---------- */
.tab-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }

.dialog-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.report-list-dialog :deep(.el-dialog__body) {
  overflow: hidden;
}

.report-list-table {
  width: 100%;
}

.report-list-table :deep(.el-table__body-wrapper) {
  overflow-x: hidden;
}

.student-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
  line-height: 1.35;
}

.student-cell strong {
  overflow: hidden;
  color: #303133;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.student-cell span {
  overflow: hidden;
  color: #909399;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toolbar-muted {
  color: #909399;
  font-size: 13px;
}

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

/* ---------- 评分 ---------- */
.report-preview { width: 100%; max-height: 160px; overflow-y: auto; background: #f5f7fa; border-radius: 8px; padding: 12px; color: #606266; line-height: 1.6; }
</style>
