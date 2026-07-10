<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">问卷管理</h2>
      <el-button type="primary" @click="openCreateDialog">+ 创建问卷</el-button>
    </div>

    <div class="panel">
      <div class="search-bar">
        <el-select v-model="courseFilter" placeholder="按课程筛选" clearable style="width:200px" @change="fetchSurveys">
          <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
      </div>

      <el-table v-if="surveys.length > 0 || loading" :data="surveys" v-loading="loading" stripe class="kechang-table"
        style="width:100%">
        <el-table-column prop="title" label="问卷标题" min-width="180" />
        <el-table-column prop="courseName" label="关联课程" min-width="120" />
        <el-table-column label="题目数" width="80" align="center">
          <template #default="{ row }">{{ row.questionCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="回收数" width="80" align="center">
          <template #default="{ row }">{{ row.responseCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="110" />
        <el-table-column prop="endDate" label="结束日期" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(computeStatus(row.startDate, row.endDate))" size="small">{{
              computeStatus(row.startDate, row.endDate) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="success" @click="viewStats(row)">统计</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && surveys.length === 0" description="暂无问卷" :image-size="100" />
      <el-pagination v-if="total > pageSize" :current-page="page" :page-size="pageSize" :total="total"
        layout="prev, pager, next" @current-change="p => { page = p; fetchSurveys() }"
        style="margin-top:16px;justify-content:center" />
    </div>

    <!-- 创建/编辑问卷弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑问卷' : '创建问卷'" width="750px" destroy-on-close
      @closed="resetForm">
      <el-form :model="form" label-width="90px">
        <el-form-item label="问卷标题" required><el-input v-model="form.title" placeholder="请输入问卷标题" /></el-form-item>
        <el-form-item label="关联课程">
          <el-select v-model="form.courseId" style="width:100%" placeholder="选择关联课程">
            <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="问卷说明"><el-input v-model="form.description" type="textarea" :rows="2"
            placeholder="选填" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width:100%">
                <el-option label="未开始" value="未开始" />
                <el-option label="进行中" value="进行中" />
                <el-option label="已结束" value="已结束" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date"
                style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束日期"><el-date-picker v-model="form.endDate" type="date"
                style="width:100%" /></el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">问卷题目</el-divider>
        <div v-for="(q, idx) in questionList" :key="idx" class="question-edit-item">
          <div class="q-header">
            <span class="q-num">题目 {{ idx + 1 }}</span>
            <el-button size="small" type="danger" circle @click="removeQuestion(idx)">✕</el-button>
          </div>
          <el-input v-model="q.questionContent" placeholder="请输入题目内容" style="margin-bottom:8px" />
          <el-row :gutter="12">
            <el-col :span="8">
              <el-select v-model="q.questionType" style="width:100%" @change="onTypeChange(q)">
                <el-option label="单选题" value="SINGLE" />
                <el-option label="多选题" value="MULTI" />
                <el-option label="问答题" value="TEXT" />
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-select v-model="q.isRequired" style="width:100%">
                <el-option label="必填" :value="1" />
                <el-option label="非必填" :value="0" />
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-input-number v-model="q.sortOrder" :min="1" placeholder="排序" style="width:100%" />
            </el-col>
          </el-row>
          <div v-if="q.questionType === 'SINGLE' || q.questionType === 'MULTI'" style="margin-top:8px">
            <div v-for="(opt, oi) in q.optionList" :key="oi"
              style="display:flex;align-items:center;gap:6px;margin-bottom:4px">
              <span style="font-weight:600;min-width:20px">{{ String.fromCharCode(65 + oi) }}.</span>
              <el-input v-model="q.optionList[oi]" placeholder="选项内容" size="small" style="flex:1" />
              <el-button size="small" type="danger" circle @click="q.optionList.splice(oi, 1)"
                :disabled="q.optionList.length <= 1">✕</el-button>
            </div>
            <el-button size="small" type="primary" plain @click="q.optionList.push('')">+ 添加选项</el-button>
          </div>
        </div>
        <el-button type="primary" plain style="width:100%;margin-top:8px" @click="addQuestion">+ 添加题目</el-button>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存修改' : '创建问卷' }}</el-button>
      </template>
    </el-dialog>

    <!-- 统计弹窗 -->
    <el-dialog v-model="statsDialogVisible" :title="statsTitle" width="800px" destroy-on-close>
      <div v-if="statsData" class="stats-content">
        <div class="stats-summary">
          <el-statistic title="参与人数" :value="statsData.totalRespondents || 0" />
          <el-statistic title="题目数" :value="statsData.questionCount || 0" />
        </div>
        <el-divider />
        <div v-for="(qs, qi) in (statsData.questionStats || [])" :key="qi" class="stats-question-item">
          <h4>{{ qi + 1 }}. [{{ typeLabel(qs.questionType) }}] {{ qs.questionContent }}</h4>
          <p class="stats-meta">共 {{ qs.totalAnswers }} 人回答</p>
          <div v-if="qs.optionCounts && Object.keys(qs.optionCounts).length > 0" class="option-chart">
            <div v-for="(count, opt) in qs.optionCounts" :key="opt" class="option-bar-row">
              <span class="option-label">{{ opt }}</span>
              <div class="option-bar-track">
                <div class="option-bar-fill" :style="{ width: barWidth(count, qs.totalAnswers) }"></div>
              </div>
              <span class="option-count">{{ count }}票 ({{ percentStr(count, qs.totalAnswers) }})</span>
            </div>
          </div>
          <div v-if="qs.textAnswers && qs.textAnswers.length > 0" class="text-answers">
            <el-tag size="small" v-for="(ta, tai) in qs.textAnswers" :key="tai" style="margin:4px">
              {{ ta || '(空)' }}
            </el-tag>
          </div>
          <div v-if="qs.textAnswers && qs.textAnswers.length === 0 && qs.questionType === 'TEXT'"
            style="color:#909399;font-size:13px">暂无文本回答</div>
        </div>
      </div>
      <el-empty v-else description="暂无统计数据" />
      <template #footer><el-button @click="statsDialogVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { surveyApi, courseApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computeStatus, statusTagType } from '@/utils/status'

const surveys = ref([]); const loading = ref(false)
const page = ref(1); const pageSize = ref(10); const total = ref(0)
const courseFilter = ref(null)
const courseList = ref([])

// 创建/编辑
const dialogVisible = ref(false); const isEdit = ref(false); const editId = ref(null)
const submitting = ref(false)
const form = reactive({ title: '', courseId: null, description: '', startDate: '', endDate: '', status: '未开始' })
const questionList = ref([])

// 统计
const statsDialogVisible = ref(false); const statsTitle = ref(''); const statsData = ref(null)

const statusTag = (s) => statusTagType(s)

const typeLabel = (t) => ({ SINGLE: '单选', MULTI: '多选', TEXT: '问答' }[t] || t)

const barWidth = (count, total) => total > 0 ? (count / total * 100).toFixed(1) + '%' : '0%'
const percentStr = (count, total) => total > 0 ? (count / total * 100).toFixed(1) + '%' : '0%'

const fetchSurveys = async () => {
  loading.value = true
  try {
    const res = await surveyApi.list({ page: page.value, size: pageSize.value, courseId: courseFilter.value || undefined })
    if (res.data.code === 200) { surveys.value = res.data.data.records; total.value = res.data.data.total }
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

const fetchCourses = async () => {
  try {
    const res = await courseApi.list({ page: 1, size: 100 })
    if (res.data.code === 200) courseList.value = res.data.data.records
  } catch (e) { /* ignore */ }
}

const resetForm = () => {
  form.title = ''; form.courseId = null; form.description = ''; form.startDate = ''; form.endDate = ''; form.status = '未开始'
  questionList.value = [{ questionContent: '', questionType: 'SINGLE', isRequired: 1, sortOrder: 1, optionList: ['', ''] }]
  isEdit.value = false; editId.value = null
}

const openCreateDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = async (row) => {
  try {
    const res = await surveyApi.detail(row.id)
    if (res.data.code !== 200) { ElMessage.error('获取问卷详情失败'); return }
    const d = res.data.data
    form.title = d.title || ''
    form.courseId = d.courseId
    form.description = d.description || ''
    form.startDate = d.startDate || ''
    form.endDate = d.endDate || ''
    form.status = d.status || '未开始'
    isEdit.value = true; editId.value = d.id

    if (d.questions && d.questions.length > 0) {
      questionList.value = d.questions.map(q => {
        let optionList = ['', '']
        try { const opts = JSON.parse(q.options || '[]'); if (opts.length > 0) optionList = opts } catch (e) { /* */ }
        return {
          id: q.id,
          questionContent: q.questionContent || '',
          questionType: q.questionType || 'SINGLE',
          isRequired: q.isRequired != null ? q.isRequired : 1,
          sortOrder: q.sortOrder || 1,
          optionList
        }
      })
    } else {
      questionList.value = [{ questionContent: '', questionType: 'SINGLE', isRequired: 1, sortOrder: 1, optionList: ['', ''] }]
    }
    dialogVisible.value = true
  } catch (e) { ElMessage.error('获取问卷详情失败') }
}

const addQuestion = () => {
  questionList.value.push({
    questionContent: '', questionType: 'SINGLE', isRequired: 1,
    sortOrder: questionList.value.length + 1, optionList: ['', '']
  })
}

const removeQuestion = (idx) => { if (questionList.value.length > 1) questionList.value.splice(idx, 1) }

const onTypeChange = (q) => {
  if (q.questionType === 'TEXT') q.optionList = []
  else if (!q.optionList || q.optionList.length === 0) q.optionList = ['', '']
}

const buildQuestionsPayload = () => {
  return questionList.value.map(q => ({
    questionContent: q.questionContent,
    questionType: q.questionType,
    options: (q.questionType === 'SINGLE' || q.questionType === 'MULTI')
      ? JSON.stringify(q.optionList.filter(o => o.trim() !== ''))
      : null,
    sortOrder: q.sortOrder,
    isRequired: q.isRequired
  }))
}

const handleSubmit = async () => {
  if (!form.title) { ElMessage.warning('请输入问卷标题'); return }
  if (questionList.value.some(q => !q.questionContent)) { ElMessage.warning('请完善所有题目内容'); return }
  submitting.value = true
  try {
    const payload = {
      title: form.title,
      courseId: form.courseId,
      description: form.description,
      startDate: form.startDate || null,
      endDate: form.endDate || null,
      status: form.status,
      questions: buildQuestionsPayload()
    }
    if (isEdit.value) {
      await surveyApi.update(editId.value, payload)
      ElMessage.success('问卷更新成功')
    } else {
      await surveyApi.create(payload)
      ElMessage.success('问卷创建成功')
    }
    dialogVisible.value = false
    fetchSurveys()
  } catch (e) { /* ignore */ }
  finally { submitting.value = false }
}

const viewStats = async (row) => {
  statsTitle.value = '问卷统计 - ' + row.title
  statsData.value = null
  try {
    const res = await surveyApi.getStats(row.id)
    if (res.data.code === 200) statsData.value = res.data.data
    statsDialogVisible.value = true
  } catch (e) { /* ignore */ }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该问卷？删除后所有题目和回收数据将被清除。', '确认删除', { type: 'warning' })
    await surveyApi.delete(id)
    ElMessage.success('问卷已删除')
    fetchSurveys()
  } catch (e) { if (e !== 'cancel') { /* ignore */ } }
}

onMounted(() => { fetchCourses(); fetchSurveys() })
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

.search-bar {
  margin-bottom: 16px;
}

.question-edit-item {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 12px;
}

.q-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-weight: 600;
}

.q-num {
  font-weight: 600;
  color: #409eff;
}

.stats-content {
  max-height: 60vh;
  overflow-y: auto;
}

.stats-summary {
  display: flex;
  gap: 40px;
  padding: 8px 0;
}

.stats-question-item {
  margin-bottom: 20px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.stats-question-item h4 {
  margin: 0 0 6px 0;
  font-size: 15px;
  color: #303133;
}

.stats-meta {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.option-chart {
  padding-left: 12px;
}

.option-bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.option-label {
  font-size: 13px;
  min-width: 80px;
  color: #606266;
}

.option-bar-track {
  flex: 1;
  height: 22px;
  background: #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.option-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #67c23a);
  border-radius: 4px;
  transition: width 0.4s ease;
  min-width: 2px;
}

.option-count {
  font-size: 12px;
  color: #909399;
  min-width: 100px;
  text-align: right;
}

.text-answers {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding-left: 12px;
}

:deep(.kechang-table) {
  width: 100%;
  border: none;
}

:deep(.kechang-table .el-table__header-wrapper th.el-table__cell) {
  background-color: #F5F7FA !important;
  color: #606266;
  font-weight: 600;
  font-size: 13px;
}

:deep(.kechang-table .el-table__body-wrapper td.el-table__cell) {
  border-bottom: none;
}

:deep(.kechang-table .el-table__cell) {
  border-right: none;
}

:deep(.kechang-table::before),
:deep(.kechang-table::after),
:deep(.kechang-table .el-table__inner-wrapper::before),
:deep(.kechang-table .el-table__border-left-patch),
:deep(.kechang-table .el-table__border-right-patch) {
  display: none;
}

:deep(.kechang-table.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background-color: rgba(163, 177, 138, 0.06);
}

:deep(.kechang-table .el-table__body tr.el-table__row:hover td) {
  background-color: rgba(163, 177, 138, 0.08) !important;
}
</style>
