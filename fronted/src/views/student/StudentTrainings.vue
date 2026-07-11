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
          <div class="card-accent"></div>
          <div class="card-main">
            <div class="card-header">
              <div class="title-wrap">
                <h3 :title="t.projectName">{{ t.projectName }}</h3>
                <p>{{ t.description || '暂无项目说明' }}</p>
              </div>
              <el-tag :type="statusTagType(computeStatus(t.startDate, t.endDate))" effect="plain">
                {{ t.status || computeStatus(t.startDate, t.endDate) }}
              </el-tag>
            </div>

            <div class="meta-row">
              <span>任务 {{ t.taskCount || 0 }}</span>
              <span>报告 {{ t.reportCount || 0 }}</span>
              <span>{{ formatRange(t.startDate, t.endDate) }}</span>
            </div>

            <div class="card-footer" @click.stop>
              <el-button size="small" type="primary" plain @click="viewProject(t)">进入实训</el-button>
            </div>
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
import { trainingApi } from '@/api'
import { computeStatus, statusTagType } from '@/utils/status'

const router = useRouter()
const trainings = ref([])

const fetchProjects = async () => {
  const res = await trainingApi.listProjects({ page: 1, size: 100 })
  if (res.data.code === 200) trainings.value = res.data.data?.records || []
}

const formatRange = (start, end) => {
  if (!start && !end) return '未设置时间'
  return `${start || '未设置开始'} 至 ${end || '未设置结束'}`
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
  overflow: hidden;
  color: var(--bio-ink);
  font-size: 17px;
  font-weight: 600;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.empty-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 280px;
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

  .card-footer .el-button {
    width: 100%;
  }
}
</style>
