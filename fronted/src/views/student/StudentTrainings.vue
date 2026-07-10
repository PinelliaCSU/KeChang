<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div>
        <h2 class="page-title">实训中心</h2>
        <p class="page-subtitle">查看实训任务、资料与提交进度。</p>
      </div>
    </div>

    <div class="panel training-panel" v-if="trainings.length > 0">
      <div class="training-grid">
        <article v-for="t in trainings" :key="t.id" class="training-card" @click="viewProject(t)">
          <div class="training-cover">
            <div class="cover-mark">
              <el-icon><Tools /></el-icon>
            </div>
            <el-tag effect="plain" size="small">{{ t.status || computeStatus(t.startDate, t.endDate) }}</el-tag>
          </div>

          <div class="training-body">
            <h3 :title="t.projectName">{{ t.projectName }}</h3>
            <p>{{ t.description || '暂无项目说明' }}</p>
            <div class="meta-row">
              <span>任务 {{ t.taskCount || 0 }}</span>
              <span>报告 {{ t.reportCount || 0 }}</span>
            </div>
            <div class="deadline">截止 {{ t.endDate || '未设置' }}</div>
          </div>

          <div class="training-footer">
            <el-button type="primary" plain @click.stop="viewProject(t)">进入实训</el-button>
          </div>
        </article>
      </div>
    </div>

    <div class="panel empty-panel" v-else>
      <el-empty description="暂无实训项目" :image-size="110" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Tools } from '@element-plus/icons-vue'
import { trainingApi } from '@/api'
import { computeStatus } from '@/utils/status'

const router = useRouter()
const trainings = ref([])

const fetchProjects = async () => {
  const res = await trainingApi.listProjects({ page: 1, size: 100 })
  if (res.data.code === 200) trainings.value = res.data.data?.records || []
}

const viewProject = (project) => {
  router.push(`/student/trainings/${project.id}`)
}

onMounted(fetchProjects)
</script>

<style scoped>
.page-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-subtitle {
  margin: 8px 0 0;
  color: var(--bio-ink-muted);
  font-size: 15px;
}

.training-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
}

.training-card {
  display: flex;
  flex-direction: column;
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

.training-cover {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  height: 96px;
  padding: 16px;
  background: #f5f7fa;
  border-bottom: 1px solid var(--bio-border);
}

.cover-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: var(--bio-primary-soft);
  color: var(--bio-primary);
  font-size: 24px;
}

.training-body {
  flex: 1;
  min-width: 0;
  padding: 16px;
}

.training-body h3 {
  margin: 0;
  overflow: hidden;
  color: var(--bio-ink);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.training-body p {
  display: -webkit-box;
  min-height: 46px;
  margin: 8px 0 0;
  overflow: hidden;
  color: var(--bio-ink-muted);
  font-size: 15px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.meta-row span,
.deadline {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 4px 10px;
  border: 1px solid var(--bio-border);
  background: #f7f8fa;
  color: var(--bio-ink-soft);
  font-size: 14px;
}

.deadline {
  margin-top: 10px;
}

.training-footer {
  padding: 0 16px 16px;
}

.training-footer .el-button {
  width: 100%;
}

.empty-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 280px;
}
</style>
