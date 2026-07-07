<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">知识库</h2>
      <el-input v-model="keyword" placeholder="搜索知识..." size="default" style="width:260px" clearable />
    </div>

    <div class="panel" v-if="items.length > 0">
      <div class="knowledge-grid">
        <div class="knowledge-card" v-for="item in items" :key="item.id">
          <div class="k-icon">
            <svg v-if="item.type === 'doc'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#7f8c52"
              stroke-width="1.8" stroke-linecap="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
            </svg>
            <svg v-else-if="item.type === 'video'" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#5c7a8a"
              stroke-width="1.8" stroke-linecap="round">
              <polygon points="23 7 16 12 23 17 23 7" />
              <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
            </svg>
            <svg v-else width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#9c6644" stroke-width="1.8"
              stroke-linecap="round">
              <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
            </svg>
          </div>
          <h4 class="k-title">{{ item.title }}</h4>
          <p class="k-desc">{{ item.description?.substring(0, 80) || '暂无描述' }}</p>
          <div class="k-footer">
            <span>{{ item.courseName || '' }}</span>
            <span>{{ item.createTime || '' }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="panel empty-panel" v-else>
      <div class="empty-state">
        <svg class="empty-illustration" width="120" height="90" viewBox="0 0 120 90" fill="none">
          <rect x="20" y="10" width="80" height="70" rx="4" fill="#e8ecf1" />
          <rect x="28" y="20" width="40" height="4" rx="2" fill="#d0d5dd" />
          <rect x="28" y="30" width="55" height="3" rx="1.5" fill="#dce1e8" />
        </svg>
        <p class="empty-text">知识库建设中，敬请期待</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const items = ref([])
const keyword = ref('')

onMounted(async () => {
  // TODO: 加载知识库数据
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

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.knowledge-card {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 20px 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.knowledge-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.k-icon {
  font-size: 32px;
  margin-bottom: 10px;
}

.k-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.k-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-bottom: 10px;
}

.k-footer {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #c0c4cc;
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
