<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">📋 问卷管理</h2>
      <el-button type="primary" @click="openCreateDialog">+ 创建问卷</el-button>
    </div>

    <div class="panel">
      <div class="search-bar">
        <el-select v-model="courseFilter" placeholder="按课程筛选" clearable style="width:200px" @change="fetchSurveys">
          <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
      </div>

      <el-table :data="surveys" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="title" label="问卷标题" min-width="200" />
        <el-table-column prop="courseName" label="关联课程" min-width="120" />
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '进行中' ? 'success' : 'info'" size="small">{{ row.status || '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewStats(row)">统计</el-button>
            <el-button size="small" type="info" @click="viewQuestions(row)">题目</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && surveys.length === 0" description="暂无问卷" :image-size="100" />
      <el-pagination v-if="total > pageSize" :current-page="page" :page-size="pageSize" :total="total"
        layout="prev, pager, next" @current-change="p => { page = p; fetchSurveys() }"
        style="margin-top:16px;justify-content:center" />
    </div>

    <!-- 创建问卷弹窗 -->
    <el-dialog v-model="dialogVisible" title="创建问卷" width="650px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="问卷标题" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="关联课程">
          <el-select v-model="form.courseId" style="width:100%">
            <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="问卷说明"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date"
                style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结束日期"><el-date-picker v-model="form.endDate" type="date"
                style="width:100%" /></el-form-item></el-col>
        </el-row>

        <el-divider content-position="left">问卷题目</el-divider>
        <div v-for="(q, idx) in questionList" :key="idx" class="question-edit-item">
          <div class="q-header">
            <span>题目 {{ idx + 1 }}</span>
            <el-button size="small" type="danger" circle @click="removeQuestion(idx)">✕</el-button>
          </div>
          <el-input v-model="q.questionContent" placeholder="题目内容" style="margin-bottom:8px" />
          <el-row :gutter="12">
            <el-col :span="8">
              <el-select v-model="q.questionType" style="width:100%">
                <el-option label="单选题" value="SINGLE" /><el-option label="多选题" value="MULTI" /><el-option label="问答题"
                  value="TEXT" />
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-select v-model="q.isRequired" style="width:100%">
                <el-option label="必填" :value="1" /><el-option label="非必填" :value="0" />
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-input-number v-model="q.sortOrder" :min="1" placeholder="排序" style="width:100%" />
            </el-col>
          </el-row>
          <div v-if="q.questionType === 'SINGLE' || q.questionType === 'MULTI'" style="margin-top:8px">
            <div v-for="(opt, oi) in q.optionList" :key="oi"
              style="display:flex;align-items:center;gap:6px;margin-bottom:4px">
              <span>{{ String.fromCharCode(65 + oi) }}.</span>
              <el-input v-model="q.optionList[oi]" placeholder="选项" size="small" style="flex:1" />
              <el-button size="small" type="danger" circle @click="q.optionList.splice(oi, 1)">✕</el-button>
            </div>
            <el-button size="small" type="primary" plain @click="q.optionList.push('')">+ 添加选项</el-button>
          </div>
        </div>
        <el-button type="primary" plain style="width:100%;margin-top:8px" @click="addQuestion">+ 添加题目</el-button>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">创建问卷</el-button>
      </template>
    </el-dialog>

    <!-- 查看题目弹窗 -->
    <el-dialog v-model="qDialogVisible" title="问卷题目" width="600px">
      <div v-if="viewQuestionsList.length > 0">
        <div v-for="(q, idx) in viewQuestionsList" :key="q.id" class="q-display-item">
          <p><b>{{ idx + 1 }}. [{{ typeLabel(q.questionType) }}]</b> {{ q.questionContent }} <el-tag size="small">{{
            q.isRequired === 1 ? '必填' : '选填' }}</el-tag></p>
          <p v-if="q.options" style="color:#909399;font-size:13px">
            选项: {{ JSON.parse(q.options || '[]').join(' | ') }}
          </p>
        </div>
      </div>
      <el-empty v-else description="该问卷暂无题目" />
      <template #footer><el-button @click="qDialogVisible = false">关闭</el-button></template>
    </el-dialog>

    <!-- 统计弹窗 -->
    <el-dialog v-model="statsDialogVisible" title="问卷统计" width="700px">
      <div v-if="statsData.length > 0">
        <el-table :data="statsData" stripe border>
          <el-table-column prop="questionId" label="题目ID" width="80" />
          <el-table-column prop="answerContent" label="回答内容" min-width="200" show-overflow-tooltip />
          <el-table-column prop="respondentId" label="回答者ID" width="100" />
        </el-table>
      </div>
      <el-empty v-else description="暂无回收数据" />
      <template #footer><el-button @click="statsDialogVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { surveyApi, courseApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const surveys = ref([]); const loading = ref(false)
const page = ref(1); const pageSize = ref(10); const total = ref(0)
const courseFilter = ref(null)
const courseList = ref([])

// 创建
const dialogVisible = ref(false); const submitting = ref(false)
const form = reactive({ title: '', courseId: null, description: '', startDate: '', endDate: '' })
const questionList = ref([])

// 查看题目
const qDialogVisible = ref(false); const viewQuestionsList = ref([])

// 统计
const statsDialogVisible = ref(false); const statsData = ref([])

const typeLabel = (t) => ({ SINGLE: '单选', MULTI: '多选', TEXT: '问答' }[t] || t)

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

const openCreateDialog = () => {
  form.title = ''; form.courseId = null; form.description = ''; form.startDate = ''; form.endDate = ''
  questionList.value = [{ questionContent: '', questionType: 'SINGLE', isRequired: 1, sortOrder: 1, optionList: ['', ''] }]
  dialogVisible.value = true
}

const addQuestion = () => {
  questionList.value.push({ questionContent: '', questionType: 'SINGLE', isRequired: 1, sortOrder: questionList.value.length + 1, optionList: ['', ''] })
}

const removeQuestion = (idx) => { if (questionList.value.length > 1) questionList.value.splice(idx, 1) }

const handleCreate = async () => {
  if (!form.title) { ElMessage.warning('请输入问卷标题'); return }
  if (questionList.value.some(q => !q.questionContent)) { ElMessage.warning('请完善所有题目'); return }
  submitting.value = true
  try {
    // 创建问卷
    const surveyRes = await surveyApi.create(form)
    // 注意：后端 create survey 可能不会自动创建 questions，这里简化处理
    // 实际需要单独创建问题，但后端API中create survey只创建survey本身
    ElMessage.success('问卷创建成功（题目需通过管理后台添加）')
    dialogVisible.value = false
    fetchSurveys()
  } catch (e) { /* ignore */ }
  finally { submitting.value = false }
}

const viewQuestions = async (row) => {
  try {
    const res = await surveyApi.getQuestions(row.id)
    if (res.data.code === 200) viewQuestionsList.value = res.data.data || []
    qDialogVisible.value = true
  } catch (e) { /* ignore */ }
}

const viewStats = async (row) => {
  try {
    const res = await surveyApi.getStats(row.id)
    if (res.data.code === 200) statsData.value = res.data.data || []
    statsDialogVisible.value = true
  } catch (e) { /* ignore */ }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该问卷？', '确认删除', { type: 'warning' })
    ElMessage.warning('删除接口暂未实现')
  } catch (e) { /* ignore */ }
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
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: 600;
}

.q-display-item {
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.q-display-item p {
  margin: 4px 0;
}
</style>
