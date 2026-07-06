<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">📊 成绩管理</h2>
      <div class="page-actions">
        <el-select v-model="courseFilter" placeholder="选择课程" size="default" style="width:180px" clearable>
          <el-option label="全部课程" value="" />
        </el-select>
      </div>
    </div>

    <div class="panel">
      <el-table :data="[]" style="width:100%">
        <el-table-column prop="studentName" label="学生姓名" min-width="120" />
        <el-table-column prop="courseName" label="课程" min-width="140" />
        <el-table-column prop="examScore" label="考试成绩" width="100" />
        <el-table-column prop="assignmentScore" label="作业成绩" width="100" />
        <el-table-column prop="totalScore" label="总评" width="100" />
        <el-table-column label="操作" width="120" />
      </el-table>
      <el-empty description="成绩管理功能开发中，敬请期待" :image-size="120" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const courseFilter = ref('')
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
