<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div>
        <h2 class="page-title">实训管理</h2>
        <p class="page-subtitle">统一管理实训计划、任务、资料与学生提交。</p>
      </div>
      <el-button type="primary" @click="openCreateProject">创建实训项目</el-button>
    </div>

    <div class="panel training-panel">
      <template v-if="projects.length > 0">
        <div class="training-grid">
          <article v-for="p in projects" :key="p.id" class="training-card" @click="viewProject(p)">
            <div class="card-accent"></div>
            <div class="card-main">
              <div class="card-header">
                <div class="title-wrap">
                  <h3>{{ p.projectName }}</h3>
                  <p>{{ p.description || '暂无项目说明' }}</p>
                </div>
                <el-tag :type="statusTagType(computeStatus(p.startDate, p.endDate))" effect="plain">
                  {{ computeStatus(p.startDate, p.endDate) }}
                </el-tag>
              </div>

              <div class="meta-row">
                <span>任务 {{ p.taskCount || 0 }}</span>
                <span>报告 {{ p.reportCount || 0 }}</span>
                <span>{{ formatRange(p.startDate, p.endDate) }}</span>
              </div>

              <div class="card-footer" @click.stop>
                <el-button size="small" type="primary" plain @click="viewProject(p)">查看详情</el-button>
                <el-button size="small" @click="openEditProject(p)">编辑</el-button>
                <el-button size="small" type="danger" plain @click="handleDeleteProject(p)">删除</el-button>
              </div>
            </div>
          </article>
        </div>
        <PaginationWrapper v-model:page="page" v-model:size="size" :total="total" :page-sizes="[12, 24, 48]" @change="fetchProjects" />
      </template>
      <el-empty v-else description="暂无实训项目，点击右上角创建" :image-size="110" />
    </div>

    <el-dialog v-model="projDialogVisible" :title="isEditProj ? '编辑项目' : '创建实训项目'" width="560px" destroy-on-close>
      <el-form :model="projForm" label-width="92px">
        <el-form-item label="项目名称" required>
          <el-input v-model="projForm.projectName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目说明">
          <el-input v-model="projForm.description" type="textarea" :rows="4" placeholder="请输入项目目标、安排或提交要求" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始日期">
              <el-date-picker v-model="projForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期">
              <el-date-picker v-model="projForm.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
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
        <el-button type="primary" :loading="projSubmitting" @click="handleProjSubmit">
          {{ isEditProj ? '保存' : '创建' }}
        </el-button>
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
import { usePagination } from '@/composables/usePagination'
import PaginationWrapper from '@/components/PaginationWrapper.vue'

const router = useRouter()
const projects = ref([])

const { page, size, total, resetPage } = usePagination({ defaultSize: 12 })

const projDialogVisible = ref(false)
const isEditProj = ref(false)
const projSubmitting = ref(false)
const currentProject = ref(null)
const projForm = reactive({ projectName: '', description: '', startDate: '', endDate: '', status: '进行中' })

const formatRange = (start, end) => {
  if (!start && !end) return '未设置时间'
  return `${start || '未设置开始'} 至 ${end || '未设置结束'}`
}

const resetProjectForm = () => {
  Object.assign(projForm, { projectName: '', description: '', startDate: '', endDate: '', status: '进行中' })
}

const fetchProjects = async () => {
  const res = await trainingApi.listProjects({ page: page.value, size: size.value })
  if (res.data.code === 200) {
    projects.value = res.data.data?.records || []
    total.value = res.data.data?.total || 0
  }
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
  if (!projForm.projectName.trim()) {
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

onMounted(fetchProjects)
</script>

<style scoped>
.page-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-title-bar {
  align-items: flex-end;
}

.page-subtitle {
  margin: 8px 0 0;
  color: var(--bio-ink-muted);
  font-size: 14px;
}

.training-panel {
  padding: 24px !important;
}

.training-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 14px;
}

.training-card {
  position: relative;
  display: flex;
  min-width: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--bio-border);
  border-radius: 0;
  cursor: pointer;
  transition: border-color 160ms ease, background-color 160ms ease;
}

.training-card:hover {
  border-color: var(--bio-primary);
  background: #fcffff;
}

.card-accent {
  width: 5px;
  background: var(--bio-primary);
  flex: 0 0 auto;
}

.card-main {
  flex: 1;
  min-width: 0;
  padding: 18px 18px 16px;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.title-wrap {
  min-width: 0;
}

.title-wrap h3 {
  margin: 0;
  color: var(--bio-ink);
  font-size: 17px;
  font-weight: 600;
  line-height: 1.35;
}

.title-wrap p {
  display: -webkit-box;
  min-height: 42px;
  margin: 8px 0 0;
  overflow: hidden;
  color: var(--bio-ink-muted);
  font-size: 14px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.meta-row span {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border: 1px solid var(--bio-border);
  background: #f7f8fa;
  color: var(--bio-ink-soft);
  font-size: 13px;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

@media (max-width: 760px) {
  .training-grid {
    grid-template-columns: 1fr;
  }

  .card-header,
  .card-footer {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
