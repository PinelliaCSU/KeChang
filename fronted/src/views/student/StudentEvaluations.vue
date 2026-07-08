<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">教学评价</h2>
    </div>

    <div class="panel" v-if="evaluations.length > 0">
      <div class="eval-list">
        <div class="eval-item" v-for="e in evaluations" :key="e.id">
          <div class="eval-info">
            <h4>{{ e.courseName || '未知课程' }}</h4>
            <p>{{ e.teacherName || '未知教师' }}</p>
          </div>
          <el-button size="small" type="primary">去评价</el-button>
        </div>
      </div>
    </div>

    <div class="panel empty-panel" v-else>
      <EmptyIllustration variant="star" text="暂无待评价内容" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { evaluationApi } from '@/api'
import EmptyIllustration from '@/components/EmptyIllustration.vue'

const evaluations = ref([])

onMounted(async () => {
  try {
    const res = await evaluationApi.list({ page: 1, size: 50 })
    if (res.data.code === 200) {
      const d = res.data.data
      evaluations.value = Array.isArray(d?.records) ? d.records : Array.isArray(d) ? d : []
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

.eval-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.eval-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.eval-item:last-child {
  border-bottom: none;
}

.eval-info h4 {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.eval-info p {
  font-size: 13px;
  color: #909399;
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
