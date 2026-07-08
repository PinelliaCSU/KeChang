<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">实训中心</h2>
    </div>

    <div class="panel" v-if="trainings.length > 0">
      <div class="training-grid">
        <div class="training-card" v-for="t in trainings" :key="t.id">
          <div class="training-cover" :style="{ background: getCoverGradient(t.id) }">
            <span class="training-tag">{{ t.type || '实训' }}</span>
          </div>
          <div class="training-body">
            <h4 class="training-title">{{ t.projectName || t.title }}</h4>
            <p class="training-desc">{{ t.description || '暂无描述' }}</p>
          </div>
          <div class="training-footer">
            <span class="training-deadline">{{ t.endDate || t.deadline || '未设截止' }}</span>
            <button class="btn-enter">进入实训</button>
          </div>
        </div>
      </div>
    </div>

    <div class="panel empty-panel" v-else>
      <EmptyIllustration variant="check" text="暂无实训项目" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { trainingApi } from '@/api'
import EmptyIllustration from '@/components/EmptyIllustration.vue'

const trainings = ref([])

const gradientPalette = [
  'linear-gradient(135deg, #3b5998, #4c6da7)', 'linear-gradient(135deg, #1e4d92, #3b82f6)',
  'linear-gradient(135deg, #0d7377, #20c997)', 'linear-gradient(135deg, #4a1a8a, #8b5cf6)',
]
const getCoverGradient = (id) => gradientPalette[(id - 1) % gradientPalette.length]

onMounted(async () => {
  try {
    const res = await trainingApi.listProjects({ page: 1, size: 50 })
    if (res.data.code === 200) {
      const d = res.data.data
      trainings.value = Array.isArray(d?.records) ? d.records : Array.isArray(d) ? d : []
    }
  } catch (e) { /* ignore */ }
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

.training-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.training-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.training-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transform: translateY(-4px);
}

.training-cover {
  height: 80px;
  position: relative;
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
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.training-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.training-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px 14px;
}

.training-deadline {
  font-size: 12px;
  color: #909399;
}

.btn-enter {
  height: 30px;
  padding: 0 14px;
  border: none;
  border-radius: 6px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-enter:hover {
  background: #d9ecff;
}

.empty-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.empty-illustration {
  opacity: 0.6;
}

.empty-text {
  color: #c0c4cc;
  font-size: 14px;
}
</style>
