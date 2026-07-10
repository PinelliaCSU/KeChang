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
            <el-tag :type="statusTagType(computeStatus(p.startDate, p.endDate))" size="small">{{
              computeStatus(p.startDate, p.endDate) }}</el-tag>
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
      <el-empty v-else description="暂无实训项目，点击右上角创建" :image-size="100" />
    </div>

    <!-- 创建/编辑项目弹窗 -->
    <el-dialog v-model="projDialogVisible" :title="isEditProj ? '编辑项目' : '创建实训项目'" width="520px" destroy-on-close>
      <el-form :model="projForm" label-width="90px">
        <el-form-item label="项目名称" required><el-input v-model="projForm.projectName" /></el-form-item>
        <el-form-item label="项目描述"><el-input v-model="projForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始日期"><el-date-picker v-model="projForm.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期"><el-date-picker v-model="projForm.endDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-select v-model="projForm.status" style="width:100%">
            <el-option label="进行中" value="进行中" />
            <el-option label="已结束" value="已结束" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="projDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="projSubmitting" @click="handleProjSubmit">{{ isEditProj ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { trainingApi } from '@/api'
import { computeStatus, statusTagType } from '@/utils/status'

const router = useRouter()
const projects = ref([])

const projDialogVisible = ref(false)
const isEditProj = ref(false)
const projSubmitting = ref(false)
const currentProject = ref(null)
const projForm = reactive({ projectName: '', description: '', startDate: '', endDate: '', status: '进行中' })

const formatRange = (start, end) => {
  if (!start && !end) return '未设置时间'
  return `${start || '未设开始'} ~ ${end || '未设结束'}`
}

const resetProjectForm = () => {
  Object.assign(projForm, { projectName: '', description: '', startDate: '', endDate: '', status: '进行中' })
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
      ElMessage.success('项目创建成功，已自动生成默认文档提交项')
    }
    projDialogVisible.value = false
    fetchProjects()
  } finally {
    projSubmitting.value = false
  }
}

const handleDeleteProject = async (project) => {
  await ElMessageBox.confirm('删除项目会同时删除任务、报告和资料，确定继续？', '删除项目', { type: 'warning' })
  await trainingApi.deleteProject(project.id)
  ElMessage.success('项目已删除')
  fetchProjects()
}

const viewProject = (project) => {
  router.push(`/teacher/trainings/${project.id}`)
}

onMounted(() => {
  fetchProjects()
})
</script>

<style scoped>
.page-shell { display: flex; flex-direction: column; gap: 20px; }
.page-title-bar { display: flex; align-items: center; justify-content: space-between; }
.page-title { margin: 0; font-size: 20px; font-weight: 600; color: #303133; }
.panel { background: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); padding: 24px; }
.training-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.training-card { border: 1px solid #ebeef5; border-radius: 8px; padding: 16px; cursor: pointer; transition: box-shadow 0.2s, transform 0.2s; }
.training-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.1); transform: translateY(-2px); }
.card-header, .card-footer, .meta-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.card-header h4 { margin: 0; font-size: 15px; color: #303133; }
.card-desc { min-height: 40px; color: #909399; font-size: 13px; line-height: 1.5; }
.meta-row { justify-content: flex-start; color: #606266; font-size: 12px; }
.card-footer { margin-top: 12px; font-size: 12px; color: #909399; }
.card-actions { display: flex; gap: 8px; }
</style>
