<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">⭐ 教学评价</h2>
    </div>

    <div class="panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="学生评价" name="student" />
        <el-tab-pane label="同行评价" name="peer" />
        <el-tab-pane label="督导评价" name="supervisor" />
      </el-tabs>
      <el-empty description="教学评价功能开发中，敬请期待" :image-size="120" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const activeTab = ref('student')
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
</style>
