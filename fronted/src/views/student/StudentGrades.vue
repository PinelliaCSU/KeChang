<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">📊 我的成绩</h2>
    </div>

    <div class="panel" v-if="grades.length > 0">
      <el-table :data="grades" style="width:100%" row-class-name="table-row">
        <el-table-column prop="courseName" label="课程" min-width="160" />
        <el-table-column prop="examAvgScore" label="考试均分" width="110" />
        <el-table-column prop="assignmentAvgScore" label="作业均分" width="110" />
        <el-table-column prop="totalScore" label="总评" width="100" />
        <el-table-column prop="gradeLevel" label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.gradeLevel)" size="small">{{ row.gradeLevel || '--' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="panel empty-panel" v-else>
      <div class="empty-state">
        <svg class="empty-illustration" width="120" height="90" viewBox="0 0 120 90" fill="none">
          <rect x="20" y="10" width="80" height="70" rx="4" fill="#e8ecf1" />
          <line x1="32" y1="30" x2="88" y2="30" stroke="#d0d5dd" stroke-width="2" />
          <line x1="32" y1="42" x2="75" y2="42" stroke="#dce1e8" stroke-width="1.5" />
        </svg>
        <p class="empty-text">暂无成绩数据</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { gradeApi } from '@/api'

const grades = ref([])

const levelTagType = (level) => {
  const map = { '优秀': 'success', '良好': '', '中等': 'warning', '及格': '', '不及格': 'danger' }
  return map[level] || 'info'
}

onMounted(async () => {
  try {
    const res = await gradeApi.myGrades()
    if (res.data.code === 200) grades.value = res.data.data || []
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
