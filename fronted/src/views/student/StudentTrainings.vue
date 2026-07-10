<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">实训中心</h2>
    </div>

    <div class="panel" v-if="trainings.length > 0">
      <div class="training-grid">
        <div class="training-card" v-for="t in trainings" :key="t.id" @click="viewProject(t)">
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
            <el-button size="small" type="primary" plain>进入实训</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="panel empty-panel" v-else>
      <el-empty description="暂无实训项目" :image-size="100" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { trainingApi } from '@/api'

const router = useRouter()
const trainings = ref([])

const gradientPalette = [
  'linear-gradient(135deg, #3b5998, #4c6da7)',
  'linear-gradient(135deg, #0d7377, #20c997)',
  'linear-gradient(135deg, #6b5b95, #8b5cf6)',
  'linear-gradient(135deg, #5f7f3f, #8aac62)'
]
const getCoverGradient = (id) => gradientPalette[(id - 1) % gradientPalette.length]

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
.page-shell { display: flex; flex-direction: column; gap: 20px; }
.page-title-bar { display: flex; align-items: center; justify-content: space-between; }
.page-title { margin: 0; font-size: 20px; font-weight: 600; color: #303133; }
.panel { background: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); padding: 24px; }
.training-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.training-card { background: #fff; border: 1px solid #ebeef5; border-radius: 8px; overflow: hidden; transition: box-shadow 0.2s, transform 0.2s; cursor: pointer; }
.training-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.1); transform: translateY(-2px); }
.training-cover { height: 80px; display: flex; align-items: flex-end; padding: 10px 14px; }
.training-tag { font-size: 11px; color: #fff; background: rgba(255,255,255,0.2); padding: 2px 8px; border-radius: 4px; }
.training-body { padding: 14px; }
.training-title { margin: 0 0 6px; font-size: 15px; font-weight: 600; color: #303133; }
.training-desc { min-height: 36px; line-height: 1.5; font-size: 12px; color: #909399; }
.meta-row { display: flex; gap: 12px; font-size: 12px; color: #909399; }
.training-footer { display: flex; align-items: center; justify-content: space-between; padding: 0 14px 14px; }
.training-deadline { font-size: 12px; color: #909399; }
.empty-panel { display: flex; align-items: center; justify-content: center; padding: 60px 24px; }
</style>
