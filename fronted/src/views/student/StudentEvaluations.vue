<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">教学评价</h2>
    </div>

    <div class="panel" v-if="evaluations.length > 0">
      <div class="eval-list">
        <div class="eval-item" v-for="e in evaluations" :key="e.id">
          <div class="eval-info">
            <h4>{{ e.courseName || '未知课程' }}</h4>
            <p>{{ e.teacherName || '未知教师' }}</p>
            <div class="eval-meta">
              <span>{{ e.startDate || '-' }} ~ {{ e.endDate || '-' }}</span>
              <span>共 {{ e.items?.length || 0 }} 项指标</span>
            </div>
          </div>
          <div class="eval-actions">
            <el-tag v-if="e.submitted" type="success" size="small">已评价</el-tag>
            <el-button v-else size="small" type="primary" @click="openEvaluation(e)">去评价</el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="panel empty-panel" v-else-if="!loading">
      <EmptyIllustration variant="star" text="暂无待评价内容" />
    </div>

    <!-- 评价填写弹窗 -->
    <el-dialog v-model="evalDialogVisible" :title="'教学评价: ' + currentEval.evaluationName" width="650px" destroy-on-close
      :close-on-click-modal="false">
      <div v-if="currentEval.items && currentEval.items.length > 0" class="eval-form">
        <div class="eval-course-info">
          <span>课程：{{ currentEval.courseName }}</span>
          <span>教师：{{ currentEval.teacherName }}</span>
        </div>
        <el-divider />
        <div v-for="(item, idx) in currentEval.items" :key="item.id" class="eval-item-row">
          <div class="eval-item-header">
            <span class="eval-item-num">{{ idx + 1 }}.</span>
            <span class="eval-item-content">{{ item.itemContent }}</span>
            <el-tag v-if="item.itemType === 'TEXT'" size="small" type="info">文本评价</el-tag>
            <el-tag v-else size="small" type="primary">评分</el-tag>
          </div>
          <div class="eval-item-body">
            <!-- RADIO 评分（1-5星） -->
            <template v-if="item.itemType === 'RADIO'">
              <el-rate v-model="scores[item.id]" :max="5" show-score score-template="{value} 分"
                :colors="['#99a9bf', '#f7ba2a', '#ff9900']" />
            </template>
            <!-- TEXT 文本评价 -->
            <el-input v-if="item.itemType === 'TEXT'" v-model="texts[item.id]" type="textarea" :rows="3"
              placeholder="请输入您的评价意见" maxlength="500" show-word-limit />
          </div>
        </div>
      </div>
      <el-empty v-else description="该评价暂无指标项" />
      <template #footer>
        <el-button @click="evalDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingEval" @click="handleSubmitEvaluation">
          提交评价
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { evaluationApi } from '@/api'
import { ElMessage } from 'element-plus'
import EmptyIllustration from '@/components/EmptyIllustration.vue'

const evaluations = ref([])
const loading = ref(false)

// 评价填写
const evalDialogVisible = ref(false)
const currentEval = ref({})
const scores = ref({})
const texts = ref({})
const submittingEval = ref(false)

const fetchEvaluations = async () => {
  loading.value = true
  try {
    const res = await evaluationApi.list({ page: 1, size: 50 })
    if (res.data.code === 200) {
      const d = res.data.data
      evaluations.value = Array.isArray(d?.records) ? d.records : Array.isArray(d) ? d : []
    }
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

const openEvaluation = async (evalItem) => {
  currentEval.value = evalItem
  scores.value = {}
  texts.value = {}

  // 如果已有指标项数据，直接打开
  if (evalItem.items && evalItem.items.length > 0) {
    evalDialogVisible.value = true
    return
  }

  // 否则从接口获取
  try {
    const res = await evaluationApi.detail(evalItem.id)
    if (res.data.code === 200) {
      currentEval.value = res.data.data
    }
    evalDialogVisible.value = true
  } catch (e) {
    ElMessage.error('获取评价详情失败')
  }
}

const handleSubmitEvaluation = async () => {
  const items = currentEval.value.items || []
  if (items.length === 0) {
    ElMessage.warning('暂无评价指标项')
    return
  }

  // 检查所有 RADIO 项是否都已评分
  for (const item of items) {
    if (item.itemType === 'RADIO') {
      const score = scores.value[item.id]
      if (score === undefined || score === null || score === 0) {
        ElMessage.warning(`请为"${item.itemContent}"评分`)
        return
      }
    }
  }

  submittingEval.value = true
  try {
    const results = items.map(item => ({
      evaluationId: currentEval.value.id,
      itemId: item.id,
      score: item.itemType === 'RADIO' ? (scores.value[item.id] || 0) : null,
      content: item.itemType === 'TEXT' ? (texts.value[item.id] || '') : null
    }))

    await evaluationApi.submit(currentEval.value.id, results)
    ElMessage.success('评价提交成功！')
    evalDialogVisible.value = false
    fetchEvaluations()
  } catch (e) {
    if (e?.response?.data?.message) {
      ElMessage.error(e.response.data.message)
    }
  } finally {
    submittingEval.value = false
  }
}

onMounted(() => { fetchEvaluations() })
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

.eval-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.eval-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.eval-item:last-child {
  border-bottom: none;
}

.eval-info h4 {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.eval-info p {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.eval-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #c0c4cc;
}

.eval-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.empty-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
}

/* 评价填写弹窗样式 */
.eval-form {
  max-height: 60vh;
  overflow-y: auto;
}

.eval-course-info {
  display: flex;
  gap: 24px;
  font-size: 14px;
  color: #606266;
}

.eval-course-info span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.eval-item-row {
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.eval-item-row:last-child {
  border-bottom: none;
}

.eval-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.eval-item-num {
  font-weight: 600;
  color: #409eff;
  min-width: 20px;
}

.eval-item-content {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.eval-item-body {
  padding-left: 28px;
}
</style>
