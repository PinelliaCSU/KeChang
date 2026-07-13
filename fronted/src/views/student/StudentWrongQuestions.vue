<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div class="title-left">
        <el-button text @click="$router.push('/student/knowledge')">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h2 class="page-title">我的错题</h2>
      </div>
      <div class="title-actions">
        <el-input v-model="keyword" placeholder="搜索题目或知识点..." size="default" style="width:240px" clearable
          @clear="onFilterChange" />
        <el-select v-model="filterCourse" placeholder="全部课程" size="default" style="width:160px" clearable
          @change="onFilterChange">
          <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
        <el-select v-model="filterMastered" placeholder="掌握状态" size="default" style="width:140px" clearable
          @change="onFilterChange">
          <el-option label="未掌握" :value="0" />
          <el-option label="已掌握" :value="1" />
        </el-select>
      </div>
    </div>

    <div class="stats-mini" v-if="!loading">
      <span>共 <b>{{ total }}</b> 道错题</span>
      <span class="stats-sep">|</span>
      <span>未掌握 <b style="color:#ef476f">{{ unmasteredCount }}</b> 道</span>
    </div>

    <div class="panel" v-loading="loading" v-if="wrongQuestions.length > 0">
      <div class="wq-list">
        <div class="wq-card" v-for="item in wrongQuestions" :key="item.id" :class="{ mastered: item.isMastered === 1 }">
          <div class="wq-header">
            <div class="wq-tags">
              <el-tag :type="questionTypeTag(item.questionType)" size="small" effect="dark">
                {{ questionTypeLabel(item.questionType) }}
              </el-tag>
              <el-tag :type="item.sourceType === 'EXAM' ? 'warning' : 'info'" size="small" effect="plain">
                {{ item.sourceType === 'EXAM' ? '考试' : '随堂测验' }}
              </el-tag>
              <span class="wq-knowledge" v-if="item.knowledgePoint">{{ item.knowledgePoint }}</span>
            </div>
            <div class="wq-header-actions">
              <el-button size="small" :type="item.isMastered === 1 ? 'success' : 'default'" plain @click="toggleMastered(item)">
                {{ item.isMastered === 1 ? '✓ 已掌握' : '标记掌握' }}
              </el-button>
              <el-button size="small" text type="danger" @click="deleteItem(item)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>

          <div class="wq-body" @click="item._expanded = !item._expanded" style="cursor:pointer">
            <p class="wq-content">{{ item.questionContent }}</p>
            <div class="wq-options" v-if="item.options">
              <span v-for="(opt, idx) in parseOptions(item.options)" :key="idx" class="wq-opt">
                {{ ['A','B','C','D','E','F'][idx] }}. {{ opt }}
              </span>
            </div>
            <div class="wq-footer-row">
              <span class="wq-course">{{ item.courseName || '未分类' }}</span>
              <span class="wq-count">答错 {{ item.wrongCount }} 次</span>
              <span class="wq-time">{{ item.createTime || '' }}</span>
              <el-icon class="wq-arrow" :class="{ rotated: item._expanded }"><ArrowDown /></el-icon>
            </div>
          </div>

          <div class="wq-detail" v-if="item._expanded">
            <div class="detail-row wrong-row">
              <span class="detail-label">你的答案</span>
              <span class="detail-value wrong-value">{{ item.wrongAnswer || '（未作答）' }}</span>
            </div>
            <div class="detail-row correct-row">
              <span class="detail-label">正确答案</span>
              <span class="detail-value correct-value">{{ item.correctAnswer || '--' }}</span>
            </div>
            <div class="detail-row analysis-row" v-if="item.analysis">
              <span class="detail-label">解析</span>
              <span class="detail-value">{{ item.analysis }}</span>
            </div>
          </div>
        </div>
      </div>
      <PaginationWrapper v-model:page="page" v-model:size="size" :total="total" :page-sizes="[12, 24, 48]" @change="fetchWrongQuestions" />
    </div>

    <div class="panel empty-panel" v-else-if="!loading">
      <EmptyIllustration text="暂无疑难错题，继续保持！" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { knowledgeApi, courseApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import EmptyIllustration from '@/components/EmptyIllustration.vue'
import { usePagination } from '@/composables/usePagination'
import PaginationWrapper from '@/components/PaginationWrapper.vue'

const wrongQuestions = ref([])
const courses = ref([])
const loading = ref(false)
const keyword = ref('')
const filterCourse = ref(null)
const filterMastered = ref(null)
const { page, size, total, resetPage } = usePagination({ defaultSize: 12 })
const unmasteredCount = ref(0)

const typeMap = { SINGLE: '单选', MULTI: '多选', JUDGE: '判断', FILL: '填空', ESSAY: '简答' }
const tagMap = { SINGLE: '', MULTI: 'success', JUDGE: 'warning', FILL: 'info', ESSAY: 'danger' }

const questionTypeLabel = (t) => typeMap[t] || t || '未知'
const questionTypeTag = (t) => tagMap[t] || 'info'

const parseOptions = (options) => {
  if (!options) return []
  try {
    const parsed = typeof options === 'string' ? JSON.parse(options) : options
    if (Array.isArray(parsed)) return parsed
    if (typeof parsed === 'object') return Object.values(parsed)
    return []
  } catch (e) { return [] }
}

let searchTimer = null
const onFilterChange = () => { resetPage(); fetchWrongQuestions() }

watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => onFilterChange(), 300)
})

const fetchWrongQuestions = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (filterCourse.value) params.courseId = filterCourse.value
    if (filterMastered.value !== null && filterMastered.value !== '') params.isMastered = filterMastered.value
    const res = await knowledgeApi.myWrongQuestions(params)
    if (res.data.code === 200) {
      const d = res.data.data
      wrongQuestions.value = (d?.records || []).map(item => ({ ...item, _expanded: false }))
      total.value = d?.total || 0
    }
  } catch (e) { /* ignore */ }
  loading.value = false
}

const fetchStats = async () => {
  try {
    const res = await knowledgeApi.knowledgeStats()
    if (res.data.code === 200) {
      unmasteredCount.value = res.data.data?.unmasteredCount || 0
    }
  } catch (e) { /* ignore */ }
}

const fetchCourses = async () => {
  try {
    const res = await courseApi.list({ page: 1, size: 100 })
    if (res.data.code === 200) {
      courses.value = res.data.data?.records || []
    }
  } catch (e) { /* ignore */ }
}

const toggleMastered = async (item) => {
  try {
    await knowledgeApi.markMastered(item.id)
    item.isMastered = item.isMastered === 1 ? 0 : 1
    ElMessage.success(item.isMastered === 1 ? '已标记为掌握' : '已取消掌握标记')
    fetchStats()
  } catch (e) { ElMessage.error('操作失败') }
}

const deleteItem = async (item) => {
  try {
    await ElMessageBox.confirm('确定删除该错题？', '确认删除', { type: 'warning' })
    await knowledgeApi.deleteWrongQuestion(item.id)
    ElMessage.success('已删除')
    fetchWrongQuestions()
    fetchStats()
  } catch (e) { /* cancelled */ }
}

onMounted(() => { fetchCourses(); fetchWrongQuestions(); fetchStats() })
</script>

<style scoped>
.page-shell { display: flex; flex-direction: column; gap: 20px; }
.page-title-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.title-left { display: flex; align-items: center; gap: 8px; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.title-actions { display: flex; gap: 10px; }
.stats-mini { font-size: 14px; color: #606266; padding: 0 4px; }
.stats-sep { margin: 0 10px; color: #dcdfe6; }
.panel { background: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); padding: 20px 24px; }
.wq-list { display: flex; flex-direction: column; gap: 12px; }
.wq-card { border: 1px solid #ebeef5; border-radius: 10px; padding: 16px 20px; background: #fafbfc; transition: all 0.3s; }
.wq-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.wq-card.mastered { opacity: 0.65; }
.wq-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; gap: 12px; }
.wq-tags { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.wq-knowledge { font-size: 13px; color: #8a94a6; background: #f0f2f5; padding: 2px 10px; border-radius: 4px; }
.wq-header-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.wq-body { user-select: none; }
.wq-content { font-size: 15px; color: #303133; line-height: 1.7; margin: 0 0 8px; }
.wq-options { display: flex; flex-wrap: wrap; gap: 6px 16px; margin-bottom: 10px; }
.wq-opt { font-size: 13px; color: #606266; }
.wq-footer-row { display: flex; align-items: center; gap: 16px; font-size: 12px; color: #c0c4cc; }
.wq-arrow { margin-left: auto; transition: transform 0.2s; }
.wq-arrow.rotated { transform: rotate(180deg); }
.wq-detail { margin-top: 12px; padding-top: 12px; border-top: 1px dashed #ebeef5; display: flex; flex-direction: column; gap: 8px; }
.detail-row { display: flex; gap: 12px; font-size: 14px; line-height: 1.6; }
.detail-label { flex-shrink: 0; color: #909399; min-width: 70px; }
.wrong-value { color: #ef476f; font-weight: 500; }
.correct-value { color: #06d6a0; font-weight: 500; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 20px; }
.empty-panel { display: flex; align-items: center; justify-content: center; padding: 60px 24px; }
</style>
