<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">我的作业</h2>
      <div class="page-actions">
        <el-select v-model="statusFilter" placeholder="状态筛选" size="default" style="width:140px" clearable>
          <el-option label="全部" value="" />
          <el-option label="未提交" value="pending" />
          <el-option label="已提交" value="submitted" />
          <el-option label="已批改" value="graded" />
        </el-select>
      </div>
    </div>

    <div class="panel" v-if="visibleAssignments.length > 0">
      <el-table :data="visibleAssignments" style="width:100%" row-class-name="table-row">
        <el-table-column prop="title" label="作业标题" min-width="200">
          <template #default="{ row }">
            <span class="link-text" @click="handleClick(row)">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="所属课程" min-width="140" />
        <el-table-column prop="deadline" label="截止时间" width="180">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isOverdue(row.deadline) }">{{ formatDate(row.deadline) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column label="提交状态" width="110">
          <template #default="{ row }">
            <el-tag :type="submitTagType(row)" size="small">{{ submitLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分数" width="90">
          <template #default="{ row }">{{ row.myScore != null ? row.myScore : '--' }}</template>
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
        <p class="empty-text">暂无作业任务，请先在课程中心加入课程</p>
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="作业详情" width="680px" destroy-on-close>
      <div v-if="currentAssignment" class="assignment-detail">
        <h3 class="detail-title">{{ currentAssignment.title }}</h3>
        <div class="detail-meta">
          <span>课程：{{ currentAssignment.courseName || '未关联课程' }}</span>
          <span>截止：{{ formatDate(currentAssignment.deadline) }}</span>
          <span>总分：{{ currentAssignment.totalScore || 100 }}</span>
          <span>状态：{{ submitLabel(currentAssignment) }}</span>
        </div>

        <div class="detail-section">
          <div class="section-label">作业内容</div>
          <div class="section-content">{{ currentAssignment.content || '教师暂未填写作业说明' }}</div>
        </div>

        <div v-if="currentAssignment.attachmentUrl" class="detail-section">
          <div class="section-label">附件链接</div>
          <a :href="currentAssignment.attachmentUrl" target="_blank" rel="noreferrer">
            {{ currentAssignment.attachmentUrl }}
          </a>
        </div>

        <div v-if="currentAssignment.submitted" class="detail-section">
          <div class="section-label">已提交内容</div>
          <div class="section-content">{{ currentAssignment.submitContent || '无文字内容' }}</div>
          <a v-if="currentAssignment.submitAttachmentUrl" :href="currentAssignment.submitAttachmentUrl" target="_blank" rel="noreferrer">
            {{ currentAssignment.submitAttachmentUrl }}
          </a>
          <div class="detail-meta">
            <span>提交时间：{{ formatDate(currentAssignment.submitTime) }}</span>
            <span v-if="currentAssignment.myScore != null">得分：{{ currentAssignment.myScore }}</span>
          </div>
        </div>

        <div v-if="currentAssignment.gradeComment" class="detail-section">
          <div class="section-label">教师评语</div>
          <div class="section-content">{{ currentAssignment.gradeComment }}</div>
        </div>

        <template v-if="!currentAssignment.submitted">
          <div class="detail-section">
            <div class="section-label">我的提交</div>
            <el-input
              v-model="submitForm.content"
              type="textarea"
              :rows="6"
              placeholder="请输入作业答案、说明或提交内容"
              :disabled="submitting || isOverdue(currentAssignment.deadline)"
            />
          </div>

          <el-input
            v-model="submitForm.attachmentUrl"
            placeholder="附件链接（可选）"
            :disabled="submitting || isOverdue(currentAssignment.deadline)"
          />
        </template>

        <p v-if="isOverdue(currentAssignment.deadline) && !currentAssignment.submitted" class="submit-tip">
          该作业已过截止时间，不能提交。
        </p>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="currentAssignment && !currentAssignment.submitted"
          type="primary"
          :loading="submitting"
          :disabled="isOverdue(currentAssignment.deadline)"
          @click="submitAssignment"
        >
          提交作业
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { assignmentApi } from '@/api'
import { ElMessage } from 'element-plus'

const assignments = ref([])
const statusFilter = ref('')
const detailVisible = ref(false)
const currentAssignment = ref(null)
const submitting = ref(false)
const submitForm = reactive({ content: '', attachmentUrl: '' })

const isOverdue = (deadline) => deadline && new Date(deadline) < new Date()

const formatDate = (value) => {
  if (!value) return '未设定'
  return String(value).replace('T', ' ')
}

const submitLabel = (row) => {
  if (row.myScore != null || row.mySubmitStatus === '已批改' || row.mySubmitStatus === '宸叉壒鏀?') return '已批改'
  if (row.submitted) return '已提交'
  if (isOverdue(row.deadline)) return '已截止'
  return '未提交'
}

const submitTagType = (row) => {
  if (row.myScore != null || row.mySubmitStatus === '已批改' || row.mySubmitStatus === '宸叉壒鏀?') return 'success'
  if (row.submitted) return 'warning'
  if (isOverdue(row.deadline)) return 'info'
  return 'danger'
}

const visibleAssignments = computed(() => {
  if (!statusFilter.value) return assignments.value
  if (statusFilter.value === 'pending') {
    return assignments.value.filter(a => !a.submitted && !isOverdue(a.deadline))
  }
  if (statusFilter.value === 'submitted') {
    return assignments.value.filter(a => a.submitted && a.myScore == null)
  }
  return assignments.value.filter(a => a.myScore != null || a.mySubmitStatus === '已批改' || a.mySubmitStatus === '宸叉壒鏀?')
})

const handleClick = (row) => {
  currentAssignment.value = row
  submitForm.content = ''
  submitForm.attachmentUrl = ''
  detailVisible.value = true
}

const fetchAssignments = async () => {
  try {
    const res = await assignmentApi.list({ page: 1, size: 50 })
    if (res.data.code === 200) {
      const data = res.data.data
      assignments.value = Array.isArray(data?.records)
        ? data.records
        : Array.isArray(data?.list)
          ? data.list
          : Array.isArray(data)
            ? data
            : []
    }
  } catch (e) {
    ElMessage.error('作业列表加载失败')
  }
}

const submitAssignment = async () => {
  if (!currentAssignment.value) return
  if (!submitForm.content.trim() && !submitForm.attachmentUrl.trim()) {
    ElMessage.warning('请填写作业内容或附件链接')
    return
  }

  submitting.value = true
  try {
    await assignmentApi.submit(currentAssignment.value.id, {
      content: submitForm.content,
      attachmentUrl: submitForm.attachmentUrl
    })
    ElMessage.success('作业提交成功')
    detailVisible.value = false
    await fetchAssignments()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
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

.assignment-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-title {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  color: #606266;
  font-size: 13px;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-label {
  font-weight: 600;
  color: #303133;
}

.section-content {
  min-height: 56px;
  padding: 12px;
  border-radius: 8px;
  background: #f5f7fa;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
}

.submit-tip {
  margin: 0;
  color: #f56c6c;
  font-size: 13px;
}
</style>
