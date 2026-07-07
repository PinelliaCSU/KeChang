<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">我的作业</h2>
      <div class="page-actions">
        <el-select v-model="statusFilter" placeholder="状态筛选" size="default" style="width:140px" clearable
          @change="fetchAssignments">
          <el-option label="全部" value="" />
          <el-option label="待提交" value="pending" />
          <el-option label="已提交" value="submitted" />
          <el-option label="已批改" value="graded" />
        </el-select>
      </div>
    </div>

    <div class="panel" v-if="assignments.length > 0">
      <el-table :data="filteredAssignments" style="width:100%" row-class-name="table-row">
        <el-table-column prop="title" label="作业标题" min-width="200">
          <template #default="{ row }">
            <span class="link-text" @click="openDetail(row)">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="所属课程" min-width="140" />
        <el-table-column prop="deadline" label="截止时间" width="170">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isOverdue(row.deadline) }">{{ row.deadline || '未设定' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column label="我的状态" width="100">
          <template #default="{ row }">
            <el-tag :type="myStatusTagType(row)" size="small">{{ myStatusLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="我的得分" width="90">
          <template #default="{ row }">{{ row.myScore != null ? row.myScore : '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="!row.mySubmitStatus" size="small" type="primary" @click="openSubmit(row)">提交作业</el-button>
            <el-button v-else size="small" @click="openDetail(row)">查看详情</el-button>
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

    <!-- 作业详情弹窗 -->
    <el-dialog v-model="detailVisible" title="作业详情" width="550px">
      <el-descriptions :column="1" border v-if="currentAssignment">
        <el-descriptions-item label="作业标题">{{ currentAssignment.title }}</el-descriptions-item>
        <el-descriptions-item label="所属课程">{{ currentAssignment.courseName }}</el-descriptions-item>
        <el-descriptions-item label="作业内容">{{ currentAssignment.content || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">{{ currentAssignment.deadline || '未设定' }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ currentAssignment.totalScore }}分</el-descriptions-item>
        <el-descriptions-item label="我的状态">
          <el-tag :type="myStatusTagType(currentAssignment)" size="small">{{ myStatusLabel(currentAssignment)
            }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentAssignment.myScore != null" label="我的得分">{{ currentAssignment.myScore
          }}分</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="!currentAssignment?.mySubmitStatus" type="primary"
          @click="detailVisible = false; openSubmit(currentAssignment)">提交作业</el-button>
      </template>
    </el-dialog>

    <!-- 提交作业弹窗 -->
    <el-dialog v-model="submitVisible" title="提交作业" width="500px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="作业标题"><b>{{ submitTarget?.title }}</b></el-form-item>
        <el-form-item label="提交内容" required>
          <el-input v-model="submitContent" type="textarea" :rows="5" placeholder="请输入作业内容或答案..." />
        </el-form-item>
        <el-form-item label="附件链接">
          <el-input v-model="submitAttachment" placeholder="可选，附件URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmit">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { assignmentApi } from '@/api'
import { ElMessage } from 'element-plus'

const assignments = ref([])
const statusFilter = ref('')

const detailVisible = ref(false)
const currentAssignment = ref(null)

const submitVisible = ref(false)
const submitTarget = ref(null)
const submitContent = ref('')
const submitAttachment = ref('')
const submitting = ref(false)

const isOverdue = (d) => d && new Date(d) < new Date()

const myStatusLabel = (row) => {
  if (!row.mySubmitStatus) return '待提交'
  if (row.mySubmitStatus === '待批改') return '已提交'
  if (row.mySubmitStatus === '已批改') return '已批改'
  return row.mySubmitStatus || '--'
}
const myStatusTagType = (row) => {
  const s = myStatusLabel(row)
  if (s === '待提交') return 'warning'
  if (s === '已提交') return ''
  if (s === '已批改') return 'success'
  return 'info'
}

const filteredAssignments = computed(() => {
  if (!statusFilter.value) return assignments.value
  return assignments.value.filter(a => {
    const s = myStatusLabel(a)
    if (statusFilter.value === 'pending') return s === '待提交'
    if (statusFilter.value === 'submitted') return s === '已提交' || s === '待批改'
    if (statusFilter.value === 'graded') return s === '已批改'
    return true
  })
})

const openDetail = (row) => {
  currentAssignment.value = row
  detailVisible.value = true
}

const openSubmit = (row) => {
  submitTarget.value = row
  submitContent.value = ''
  submitAttachment.value = ''
  submitVisible.value = true
}

const doSubmit = async () => {
  if (!submitContent.value.trim()) { ElMessage.warning('请输入提交内容'); return }
  submitting.value = true
  try {
    await assignmentApi.submit(submitTarget.value.id, {
      content: submitContent.value,
      attachmentUrl: submitAttachment.value
    })
    ElMessage.success('作业提交成功')
    submitVisible.value = false
    fetchAssignments()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '提交失败')
  }
  submitting.value = false
}

const fetchAssignments = async () => {
  try {
    const res = await assignmentApi.myList({ page: 1, size: 50 })
    if (res.data.code === 200) {
      const d = res.data.data
      assignments.value = Array.isArray(d?.records) ? d.records : Array.isArray(d?.list) ? d.list : Array.isArray(d) ? d : []
    }
  } catch (e) { /* ignore */ }
}

onMounted(fetchAssignments)
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
