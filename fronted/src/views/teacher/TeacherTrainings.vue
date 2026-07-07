<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">实训管理</h2>
      <el-button type="primary" @click="openCreateProject">+ 创建实训项目</el-button>
    </div>

    <div class="panel">
      <div class="search-bar">
        <el-select v-model="courseFilter" placeholder="按课程筛选" clearable style="width:200px" @change="fetchProjects">
          <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
      </div>

      <div class="training-grid" v-if="projects.length > 0">
        <div class="training-card" v-for="p in projects" :key="p.id" @click="viewProject(p)">
          <div class="card-header">
            <h4>{{ p.projectName }}</h4>
            <el-tag :type="p.status === '进行中' ? 'success' : 'info'" size="small">{{ p.status || '进行中' }}</el-tag>
          </div>
          <p class="card-desc">{{ p.description || '暂无描述' }}</p>
          <div class="card-footer">
            <span>{{ p.startDate }} ~ {{ p.endDate }}</span>
            <el-button size="small" type="primary">管理</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无实训项目" :image-size="100" />
    </div>

    <!-- 创建/编辑项目弹窗 -->
    <el-dialog v-model="projDialogVisible" :title="isEditProj ? '编辑项目' : '创建实训项目'" width="500px" destroy-on-close>
      <el-form :model="projForm" label-width="90px">
        <el-form-item label="项目名称" required><el-input v-model="projForm.projectName" /></el-form-item>
        <el-form-item label="所属课程" required>
          <el-select v-model="projForm.courseId" style="width:100%">
            <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目描述"><el-input v-model="projForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="开始日期"><el-date-picker v-model="projForm.startDate" type="date"
                style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结束日期"><el-date-picker v-model="projForm.endDate" type="date"
                style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="状态">
          <el-select v-model="projForm.status" style="width:100%">
            <el-option label="进行中" value="进行中" /><el-option label="已结束" value="已结束" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="projDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="projSubmitting" @click="handleProjSubmit">{{ isEditProj ? '保存' : '创建'
        }}</el-button>
      </template>
    </el-dialog>

    <!-- 项目详情弹窗（任务管理 + 报告批阅） -->
    <el-dialog v-model="taskDialogVisible" :title="'任务管理 - ' + currentProject?.projectName" width="850px">
      <div style="margin-bottom:12px">
        <el-button type="primary" size="small" @click="openCreateTask">+ 添加任务</el-button>
      </div>
      <el-table :data="tasks" v-loading="taskLoading" stripe border>
        <el-table-column prop="sortOrder" label="序号" width="60" />
        <el-table-column prop="taskName" label="任务名称" min-width="180" />
        <el-table-column prop="deadline" label="截止时间" width="170" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="viewReports(row)">查看报告</el-button>
            <el-button size="small" type="danger" @click="handleDeleteTask(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!taskLoading && tasks.length === 0" description="暂无任务" />
      <template #footer><el-button @click="taskDialogVisible = false">关闭</el-button></template>
    </el-dialog>

    <!-- 创建任务弹窗 -->
    <el-dialog v-model="taskFormVisible" title="添加任务" width="450px">
      <el-form :model="taskForm" label-width="80px">
        <el-form-item label="任务名称" required><el-input v-model="taskForm.taskName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="taskForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="截止时间"><el-date-picker v-model="taskForm.deadline" type="datetime"
            style="width:100%" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="taskForm.sortOrder" :min="1"
            style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="taskSubmitting" @click="handleCreateTask">创建任务</el-button>
      </template>
    </el-dialog>

    <!-- 报告列表弹窗 -->
    <el-dialog v-model="reportDialogVisible" title="实训报告" width="750px">
      <el-table :data="reports" v-loading="reportLoading" stripe border>
        <el-table-column prop="studentId" label="学生ID" width="80" />
        <el-table-column prop="reportContent" label="报告内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="submitTime" label="提交时间" width="170" />
        <el-table-column prop="score" label="评分" width="80">
          <template #default="{ row }">{{ row.score != null ? row.score : '未评' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openReportGrade(row)">评分</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer><el-button @click="reportDialogVisible = false">关闭</el-button></template>
    </el-dialog>

    <!-- 报告评分弹窗 -->
    <el-dialog v-model="reportGradeVisible" title="报告评分" width="450px">
      <el-form label-width="80px">
        <el-form-item label="报告内容">
          <div style="background:#f5f7fa;padding:10px;border-radius:6px;max-height:150px;overflow-y:auto">{{
            currentReport?.reportContent }}</div>
        </el-form-item>
        <el-form-item label="分数"><el-input-number v-model="reportScore" :min="0" :max="100" /></el-form-item>
        <el-form-item label="评语"><el-input v-model="reportComment" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportGradeVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportGrading" @click="doReportGrade">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { trainingApi, courseApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const courseList = ref([]); const courseFilter = ref(null)
const projects = ref([])

// 项目
const projDialogVisible = ref(false); const isEditProj = ref(false); const projSubmitting = ref(false)
const projForm = reactive({ projectName: '', courseId: null, description: '', startDate: '', endDate: '', status: '进行中' })

// 任务
const taskDialogVisible = ref(false); const currentProject = ref(null)
const tasks = ref([]); const taskLoading = ref(false)
const taskFormVisible = ref(false); const taskSubmitting = ref(false)
const taskForm = reactive({ taskName: '', description: '', deadline: '', sortOrder: 1 })

// 报告
const reportDialogVisible = ref(false); const reports = ref([]); const reportLoading = ref(false)
const currentTask = ref(null)
const reportGradeVisible = ref(false); const currentReport = ref(null)
const reportScore = ref(0); const reportComment = ref(''); const reportGrading = ref(false)

const fetchProjects = async () => {
  try {
    const res = await trainingApi.listProjects({ courseId: courseFilter.value || undefined })
    if (res.data.code === 200) projects.value = res.data.data.records || res.data.data || []
  } catch (e) { /* ignore */ }
}

const fetchCourses = async () => {
  try {
    const res = await courseApi.list({ page: 1, size: 100 })
    if (res.data.code === 200) courseList.value = res.data.data.records
  } catch (e) { /* ignore */ }
}

const openCreateProject = () => {
  isEditProj.value = false
  projForm.projectName = ''; projForm.courseId = null; projForm.description = ''; projForm.startDate = ''; projForm.endDate = ''; projForm.status = '进行中'
  projDialogVisible.value = true
}

const handleProjSubmit = async () => {
  if (!projForm.projectName || !projForm.courseId) { ElMessage.warning('请填写必要信息'); return }
  projSubmitting.value = true
  try {
    if (isEditProj.value) {
      await trainingApi.updateProject(currentProject.value?.id, projForm)
      ElMessage.success('项目更新成功')
    } else {
      await trainingApi.createProject(projForm)
      ElMessage.success('项目创建成功')
    }
    projDialogVisible.value = false
    fetchProjects()
  } catch (e) { /* ignore */ }
  finally { projSubmitting.value = false }
}

const viewProject = async (p) => {
  currentProject.value = p
  taskDialogVisible.value = true
  taskLoading.value = true
  try {
    const res = await trainingApi.listTasks(p.id)
    if (res.data.code === 200) tasks.value = res.data.data.records || res.data.data || []
  } catch (e) { /* ignore */ }
  finally { taskLoading.value = false }
}

const openCreateTask = () => {
  taskForm.taskName = ''; taskForm.description = ''; taskForm.deadline = ''; taskForm.sortOrder = tasks.value.length + 1
  taskFormVisible.value = true
}

const handleCreateTask = async () => {
  if (!taskForm.taskName) { ElMessage.warning('请输入任务名称'); return }
  taskSubmitting.value = true
  try {
    await trainingApi.createTask(currentProject.value.id, taskForm)
    ElMessage.success('任务创建成功')
    taskFormVisible.value = false
    viewProject(currentProject.value)
  } catch (e) { /* ignore */ }
  finally { taskSubmitting.value = false }
}

const handleDeleteTask = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该任务？', '确认删除', { type: 'warning' })
    ElMessage.warning('删除接口暂未实现')
  } catch (e) { /* ignore */ }
}

const viewReports = async (task) => {
  currentTask.value = task
  reportDialogVisible.value = true
  reportLoading.value = true
  try {
    const res = await trainingApi.listReports(task.id)
    if (res.data.code === 200) reports.value = res.data.data.records || res.data.data || []
  } catch (e) { /* ignore */ }
  finally { reportLoading.value = false }
}

const openReportGrade = (row) => {
  currentReport.value = row
  reportScore.value = row.score || 0
  reportComment.value = row.comment || ''
  reportGradeVisible.value = true
}

const doReportGrade = async () => {
  reportGrading.value = true
  try {
    await trainingApi.gradeReport(currentReport.value.id, { score: reportScore.value, comment: reportComment.value })
    ElMessage.success('评分成功')
    reportGradeVisible.value = false
    viewReports(currentTask.value)
  } catch (e) { /* ignore */ }
  finally { reportGrading.value = false }
}

onMounted(() => { fetchCourses(); fetchProjects() })
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
  margin-bottom: 16px;
}

.training-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.training-card {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.training-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transform: translateY(-4px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.card-header h4 {
  margin: 0;
  font-size: 15px;
  color: #303133;
}

.card-desc {
  color: #909399;
  font-size: 13px;
  margin: 8px 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
