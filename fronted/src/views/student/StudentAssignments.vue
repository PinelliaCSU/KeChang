<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">我的作业</h2>
      <div class="page-actions">
        <el-select v-model="statusFilter" placeholder="状态筛选" size="default" style="width:140px" clearable>
          <el-option label="全部" value="" />
          <el-option label="待提交" value="pending" />
          <el-option label="已提交" value="submitted" />
          <el-option label="已批改" value="graded" />
        </el-select>
      </div>
    </div>

    <div class="panel" v-if="assignments.length > 0">
      <el-table :data="assignments" style="width:100%" row-class-name="table-row">
        <el-table-column prop="title" label="作业标题" min-width="200">
          <template #default="{ row }">
            <span class="link-text" @click="handleClick(row)">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="所属课程" min-width="140" />
        <el-table-column prop="deadline" label="截止时间" width="170">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isOverdue(row.deadline) }">{{ row.deadline || '未设定' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleClick(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="panel empty-panel" v-else>
      <div class="empty-state">
        <svg class="empty-illustration" width="120" height="90" viewBox="0 0 120 90" fill="none">
          <rect x="20" y="10" width="80" height="70" rx="4" fill="#e8ecf1" />
          <rect x="28" y="20" width="28" height="4" rx="2" fill="#d0d5dd" />
          <rect x="28" y="30" width="50" height="3" rx="1.5" fill="#dce1e8" />
        </svg>
        <p class="empty-text">暂无作业任务</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { assignmentApi } from '@/api'

const assignments = ref([])
const statusFilter = ref('')

const isOverdue = (d) => d && new Date(d) < new Date()
const statusLabel = (s) => ({ 'pending': '待提交', 'submitted': '已提交', 'graded': '已批改' }[s] || s || '--')
const statusTagType = (s) => ({ 'pending': 'warning', 'submitted': '', 'graded': 'success' }[s] || 'info')

const handleClick = (row) => { /* 跳转到作业详情 */ }

onMounted(async () => {
  try {
    const res = await assignmentApi.list({ page: 1, size: 50 })
    if (res.data.code === 200) {
      const d = res.data.data
      assignments.value = Array.isArray(d?.records) ? d.records : Array.isArray(d?.list) ? d.list : Array.isArray(d) ? d : []
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

.link-text {
  color: #409eff;
  cursor: pointer;
}

.link-text:hover {
  color: #337ecc;
}

.text-danger {
  color: #f56c6c;
  font-weight: 500;
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
