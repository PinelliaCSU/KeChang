<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">📋 问卷管理</h2>
      <el-button type="primary" size="default">+ 创建问卷</el-button>
    </div>

    <div class="panel">
      <el-table :data="[]" style="width:100%">
        <el-table-column prop="title" label="问卷标题" min-width="200" />
        <el-table-column prop="courseName" label="关联课程" min-width="140" />
        <el-table-column prop="responseCount" label="回收/总数" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" />
      </el-table>
      <el-empty description="问卷管理功能开发中，敬请期待" :image-size="120" />
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
</style>
