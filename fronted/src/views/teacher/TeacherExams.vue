<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">⏱ 考试管理</h2>
      <el-button type="primary" size="default">+ 创建考试</el-button>
    </div>

    <div class="panel">
      <el-table :data="[]" style="width:100%">
        <el-table-column prop="examName" label="考试名称" min-width="200" />
        <el-table-column prop="courseName" label="所属课程" min-width="140" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="duration" label="时长" width="100" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="180" />
      </el-table>
      <el-empty description="考试管理功能开发中，敬请期待" :image-size="120" />
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
