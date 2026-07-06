<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">🔬 实训管理</h2>
      <el-button type="primary" size="default">+ 创建实训项目</el-button>
    </div>

    <div class="panel">
      <div class="training-grid">
        <div class="training-card" v-for="i in 0" :key="i"></div>
      </div>
      <el-empty description="实训管理功能开发中，敬请期待" :image-size="120" />
    </div>
  </div>
</template>

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
</style>
