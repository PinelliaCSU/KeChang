<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">📒 学习笔记</h2>
      <el-button type="primary" size="default">+ 新建笔记</el-button>
    </div>

    <div class="panel" v-if="notes.length > 0">
      <div class="note-grid">
        <div class="note-card" v-for="n in notes" :key="n.id">
          <h4 class="note-title">{{ n.title }}</h4>
          <p class="note-preview">{{ n.content?.substring(0, 120) || '暂无内容' }}</p>
          <div class="note-footer">
            <span class="note-course">{{ n.courseName || '' }}</span>
            <span class="note-time">{{ n.createTime || '' }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="panel empty-panel" v-else>
      <div class="empty-state">
        <svg class="empty-illustration" width="120" height="90" viewBox="0 0 120 90" fill="none">
          <rect x="20" y="10" width="80" height="70" rx="4" fill="#e8ecf1" />
          <rect x="28" y="20" width="20" height="4" rx="2" fill="#d0d5dd" />
          <rect x="28" y="30" width="50" height="3" rx="1.5" fill="#dce1e8" />
        </svg>
        <p class="empty-text">暂无笔记，去课程学习中记录吧</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const notes = ref([])

onMounted(async () => {
  // TODO: 加载笔记数据
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

.note-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.note-card {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.note-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.note-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.note-preview {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
  margin-bottom: 12px;
}

.note-footer {
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
