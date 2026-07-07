<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">在线考试</h2>
      <div class="page-actions">
        <el-select v-model="statusFilter" placeholder="状态筛选" size="default" style="width:140px" clearable>
          <el-option label="全部" value="" />
          <el-option label="待参加" value="upcoming" />
          <el-option label="进行中" value="ongoing" />
          <el-option label="已结束" value="ended" />
        </el-select>
      </div>
    </div>

    <div class="panel" v-if="exams.length > 0">
      <el-table :data="exams" style="width:100%" row-class-name="table-row">
        <el-table-column prop="examName" label="考试名称" min-width="200">
          <template #default="{ row }">
            <span class="link-text" @click="enterExam(row)">{{ row.examName || row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="所属课程" min-width="140" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column prop="duration" label="时长(分钟)" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="examStatusTagType(row)" size="small">{{ examStatusLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="canEnterExam(row)" size="small" type="primary" @click="enterExam(row)">进入考试</el-button>
            <span v-else class="text-muted">--</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="panel empty-panel" v-else>
      <div class="empty-state">
        <svg class="empty-illustration" width="120" height="90" viewBox="0 0 120 90" fill="none">
          <rect x="20" y="10" width="80" height="70" rx="4" fill="#e8ecf1" />
          <rect x="28" y="20" width="28" height="4" rx="2" fill="#d0d5dd" />
        </svg>
        <p class="empty-text">暂无考试安排</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { examApi } from '@/api'

const router = useRouter()
const exams = ref([])
const statusFilter = ref('')

const examStatusLabel = (row) => {
  const now = new Date()
  if (row.startTime && new Date(row.startTime) > now) return '待参加'
  if (row.endTime && new Date(row.endTime) < now) return '已结束'
  return '进行中'
}
const examStatusTagType = (row) => {
  const s = examStatusLabel(row)
  return s === '进行中' ? 'success' : s === '待参加' ? 'warning' : 'info'
}
const canEnterExam = (row) => examStatusLabel(row) === '进行中'
const enterExam = (row) => router.push(`/student/exams/${row.id}`)

onMounted(async () => {
  try {
    const res = await examApi.listExams({ page: 1, size: 50 })
    if (res.data.code === 200) {
      const d = res.data.data
      exams.value = Array.isArray(d?.records) ? d.records : Array.isArray(d?.list) ? d.list : Array.isArray(d) ? d : []
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

.text-muted {
  color: #c0c4cc;
  font-size: 13px;
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
