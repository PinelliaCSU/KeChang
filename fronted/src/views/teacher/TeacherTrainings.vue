<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">实训管理</h2>
      <el-button type="primary" @click="openCreateProject">创建实训项目</el-button>
    </div>

    <div class="panel">
      <div class="training-grid" v-if="projects.length > 0">
        <div class="training-card" v-for="p in projects" :key="p.id" @click="viewProject(p)">
          <div class="card-header">
            <h4>{{ p.projectName }}</h4>
            <el-tag :type="p.status === '已结束' ? 'info' : 'success'" size="small">{{ p.status || '进行中' }}</el-tag>
          </div>
          <p class="card-desc">{{ p.description || '暂无描述' }}</p>
          <div class="meta-row">
            <span>任务 {{ p.taskCount || 0 }}</span>
            <span>报告 {{ p.reportCount || 0 }}</span>
          </div>
          <div class="card-footer">
            <span>{{ formatRange(p.startDate, p.endDate) }}</span>
            <div class="card-actions" @click.stop>
              <el-button size="small" @click="openEditProject(p)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteProject(p)">删除</el-button>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无实训项目" :image-size="100" />
    </div>

    <el-dialog v-model="projDialogVisible" :title="isEditProj ? '编辑项目' : '创建实训项目'" width="520px" destroy-on-close>
      <el-form :model="projForm" label-width="90px">
        <el-form-item label="项目名称" required><el-input v-model="projForm.projectName" /></el-form-item>
        <el-form-item label="项目描述"><el-input v-model="projForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始日期"><el-date-picker v-model="projForm.startDate" type="date"
                value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期"><el-date-picker v-model="projForm.endDate" type="date" value-format="YYYY-MM-DD"
                style="width: 100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-select v-model="projForm.status" style="width: 100%">
            <el-option label="进行中" value="进行中" />
            <el-option label="已结束" value="已结束" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="projDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="projSubmitting" @click="handleProjSubmit">{{ isEditProj ? '保存' : '创建'
          }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="taskDialogVisible" :title="currentProject ? `任务管理 - ${currentProject.projectName}` : '任务管理'"
      width="900px">
      <div class="dialog-toolbar">
        <div class="dialog-subtitle">{{ currentProject?.description || '管理实训任务和学生报告' }}</div>
        <el-button type="primary" size="small" @click="openCreateTask">添加任务</el-button>
      </div>
      <el-table v-if="tasks.length > 0 || taskLoading" :data="tasks" v-loading="taskLoading" stripe border>
        <el-table-column prop="sortOrder" label="序号" width="70" />
        <el-table-column prop="taskName" label="任务名称" min-width="150" />
        <el-table-column prop="description" label="任务说明" min-width="180" show-overflow-tooltip />
        <el-table-column prop="deadline" label="截止时间" width="170" />
        <el-table-column prop="reportCount" label="报告数" width="80" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="viewReports(row)">报告</el-button>
            <el-button size="small" type="danger" @click="handleDeleteTask(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!taskLoading && tasks.length === 0" description="暂无任务" />
      <template #footer><el-button @click="taskDialogVisible = false">关闭</el-button></template>
    </el-dialog>

    <el-dialog v-model="taskFormVisible" title="发布实训任务" width="500px">
      <el-form :model="taskForm" label-width="90px">
        <el-form-item label="任务名称" required><el-input v-model="taskForm.taskName" /></el-form-item>
        <el-form-item label="任务说明"><el-input v-model="taskForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="附件">
          <div v-if="taskForm.files.length > 0" class="file-list" style="margin-bottom:8px">
            <div v-for="(f, i) in taskForm.files" :key="i" class="file-item">
              <span class="file-name">{{ f.name }}</span>
              <span class="file-size">{{ formatFileSize(f.size) }}</span>
              <el-button size="small" type="danger" link @click="taskForm.files.splice(i, 1)">移除</el-button>
            </div>
          </div>
          <div style="display:flex;gap:8px;align-items:center;width:100%">
            <el-upload :http-request="uploadAttachment" :show-file-list="false">
              <el-button :loading="uploadLoading">上传文件</el-button>
            </el-upload>
            <span style="font-size:12px;color:#909399">可上传多个文件</span>
          </div>
        </el-form-item>
        <el-form-item label="截止时间"><el-date-picker v-model="taskForm.deadline" type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="taskForm.sortOrder" :min="1"
            style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="taskSubmitting" @click="handleCreateTask">发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportDialogVisible" :title="currentTask ? `实训报告 - ${currentTask.taskName}` : '实训报告'"
      width="920px">
      <el-table v-if="reports.length > 0 || reportLoading" :data="reports" v-loading="reportLoading" stripe border>
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="110" />
        <el-table-column prop="reportContent" label="报告内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="附件" width="150">
          <template #default="{ row }">
            <div v-if="(row.attachments && row.attachments.length > 0) || row.attachmentUrl"
              style="display:flex;flex-wrap:wrap;gap:4px">
              <template v-if="row.attachments && row.attachments.length > 0">
                <el-link v-for="(att, i) in row.attachments" :key="att.id" :href="att.fileUrl" target="_blank"
                  type="primary" style="font-size:12px">文件{{ i + 1 }}</el-link>
              </template>
              <el-link v-else-if="row.attachmentUrl" :href="row.attachmentUrl" target="_blank" type="primary"
                style="font-size:12px">文件1</el-link>
            </div>
            <span v-else style="color:#c0c4cc">--</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="170" />
        <el-table-column prop="score" label="评分" width="90">
          <template #default="{ row }">{{ row.score != null ? row.score : '未评' }}</template>
        </el-table-column>
        <el-table-column prop="comment" label="评语" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.comment || '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openReportGrade(row)">评分</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!reportLoading && reports.length === 0" description="暂无学生提交报告" />
      <template #footer><el-button @click="reportDialogVisible = false">关闭</el-button></template>
    </el-dialog>

    <el-dialog v-model="reportGradeVisible" title="报告评分" width="480px">
      <el-form label-width="80px">
        <el-form-item label="学生">
          <span>{{ currentReport?.studentName || '--' }}（{{ currentReport?.studentNo || currentReport?.studentId
            }}）</span>
        </el-form-item>
        <el-form-item label="报告内容">
          <div class="report-preview">{{ currentReport?.reportContent || '--' }}</div>
        </el-form-item>
        <el-form-item label="附件"
          v-if="(currentReport?.attachments && currentReport.attachments.length > 0) || currentReport?.attachmentUrl">
          <div style="display:flex;flex-direction:column;gap:4px">
            <template v-if="currentReport?.attachments && currentReport.attachments.length > 0">
              <el-link v-for="att in currentReport.attachments" :key="att.id" :href="att.fileUrl" target="_blank"
                type="primary">{{ att.fileName }}</el-link>
            </template>
            <el-link v-else-if="currentReport?.attachmentUrl" :href="currentReport.attachmentUrl" target="_blank"
              type="primary">查看附件</el-link>
          </div>
        </el-form-item>
        <el-form-item label="分数"><el-input-number v-model="reportScore" :min="0" :max="100" /></el-form-item>
        <el-form-item label="评语"><el-input v-model="reportComment" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportGradeVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportGrading" @click="doReportGrade">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { trainingApi, uploadApi } from '@/api'

const projects = ref([])

const projDialogVisible = ref(false)
const isEditProj = ref(false)
const projSubmitting = ref(false)
const currentProject = ref(null)
const projForm = reactive({ projectName: '', description: '', startDate: '', endDate: '', status: '进行中' })

const taskDialogVisible = ref(false)
const tasks = ref([])
const taskLoading = ref(false)
const taskFormVisible = ref(false)
const taskSubmitting = ref(false)
const currentTask = ref(null)
const taskForm = reactive({ taskName: '', description: '', attachmentUrl: '', deadline: '', sortOrder: 1, files: [] })
const uploadLoading = ref(false)

const reportDialogVisible = ref(false)
const reports = ref([])
const reportLoading = ref(false)
const reportGradeVisible = ref(false)
const currentReport = ref(null)
const reportScore = ref(0)
const reportComment = ref('')
const reportGrading = ref(false)

const formatRange = (start, end) => {
  if (!start && !end) return '未设置时间'
  return `${start || '未设开始'} ~ ${end || '未设结束'}`
}

const resetProjectForm = () => {
  Object.assign(projForm, { projectName: '', description: '', startDate: '', endDate: '', status: '进行中' })
}

const resetTaskForm = () => {
  Object.assign(taskForm, { taskName: '', description: '', attachmentUrl: '', deadline: '', sortOrder: tasks.value.length + 1, files: [] })
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
      taskForm.files.push({
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

const fetchProjects = async () => {
  const res = await trainingApi.listProjects({ page: 1, size: 100 })
  if (res.data.code === 200) projects.value = res.data.data?.records || []
}

const openCreateProject = () => {
  isEditProj.value = false
  currentProject.value = null
  resetProjectForm()
  projDialogVisible.value = true
}

const openEditProject = (project) => {
  isEditProj.value = true
  currentProject.value = project
  Object.assign(projForm, {
    projectName: project.projectName,
    description: project.description || '',
    startDate: project.startDate || '',
    endDate: project.endDate || '',
    status: project.status || '进行中'
  })
  projDialogVisible.value = true
}

const handleProjSubmit = async () => {
  if (!projForm.projectName) {
    ElMessage.warning('请填写项目名称')
    return
  }
  projSubmitting.value = true
  try {
    if (isEditProj.value) {
      await trainingApi.updateProject(currentProject.value.id, { ...projForm })
      ElMessage.success('项目更新成功')
    } else {
      await trainingApi.createProject({ ...projForm })
      ElMessage.success('项目创建成功')
    }
    projDialogVisible.value = false
    fetchProjects()
  } finally {
    projSubmitting.value = false
  }
}

const handleDeleteProject = async (project) => {
  await ElMessageBox.confirm('删除项目会同时删除任务和报告，确定继续？', '删除项目', { type: 'warning' })
  await trainingApi.deleteProject(project.id)
  ElMessage.success('项目已删除')
  fetchProjects()
}

const viewProject = async (project) => {
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

const openCreateTask = () => {
  resetTaskForm()
  taskFormVisible.value = true
}

const handleCreateTask = async () => {
  if (!taskForm.taskName) {
    ElMessage.warning('请输入任务名称')
    return
  }
  taskSubmitting.value = true
  try {
    const payload = {
      ...taskForm,
      attachmentUrl: taskForm.files.length > 0 ? taskForm.files[0].url : taskForm.attachmentUrl,
      attachmentUrls: taskForm.files.map(f => f.url),
      attachmentNames: taskForm.files.map(f => f.name),
      attachmentSizes: taskForm.files.map(f => f.size)
    }
    delete payload.files
    await trainingApi.createTask(currentProject.value.id, payload)
    ElMessage.success('任务发布成功')
    taskFormVisible.value = false
    await fetchTasks()
    fetchProjects()
  } finally {
    taskSubmitting.value = false
  }
}

const handleDeleteTask = async (task) => {
  await ElMessageBox.confirm('删除任务会同时删除学生报告，确定继续？', '删除任务', { type: 'warning' })
  await trainingApi.deleteTask(task.id)
  ElMessage.success('任务已删除')
  await fetchTasks()
  fetchProjects()
}

const viewReports = async (task) => {
  currentTask.value = task
  reportDialogVisible.value = true
  reportLoading.value = true
  try {
    const res = await trainingApi.listReports(task.id, { page: 1, size: 100 })
    if (res.data.code === 200) reports.value = res.data.data?.records || []
  } finally {
    reportLoading.value = false
  }
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
    reportGradeVisible.value = false
    await viewReports(currentTask.value)
    await fetchTasks()
    fetchProjects()
  } finally {
    reportGrading.value = false
  }
}

onMounted(() => {
  fetchProjects()
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

.search-bar {
  margin-bottom: 16px;
}

.training-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.training-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}

.training-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.card-header,
.card-footer,
.meta-row,
.dialog-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-header h4 {
  margin: 0;
  font-size: 15px;
  color: #303133;
}

.course-name {
  margin: 8px 0 0;
  color: #606266;
  font-size: 13px;
}

.card-desc {
  min-height: 40px;
  color: #909399;
  font-size: 13px;
  line-height: 1.5;
}

.meta-row {
  justify-content: flex-start;
  color: #606266;
  font-size: 12px;
}

.card-footer {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.dialog-toolbar {
  margin-bottom: 12px;
}

.dialog-subtitle {
  color: #909399;
  font-size: 13px;
}

.report-preview {
  width: 100%;
  max-height: 160px;
  overflow-y: auto;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px;
  color: #606266;
  line-height: 1.6;
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
</style>
