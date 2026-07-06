<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">📝 试题管理</h2>
      <div class="page-actions">
        <el-button type="primary" size="default">+ 新建试题</el-button>
        <el-input v-model="keyword" placeholder="搜索试题..." size="default" style="width:220px" clearable />
      </div>
    </div>

    <div class="panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="全部试题" name="all" />
        <el-tab-pane label="单选题" name="single" />
        <el-tab-pane label="多选题" name="multi" />
        <el-tab-pane label="判断题" name="judge" />
        <el-tab-pane label="填空题" name="fill" />
      </el-tabs>
      <el-empty description="试题管理功能开发中，敬请期待" :image-size="120" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const activeTab = ref('all')
const keyword = ref('')
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

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 24px;
}
</style>
