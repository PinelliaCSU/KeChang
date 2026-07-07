<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">📝 试题管理</h2>
      <div class="page-actions">
        <el-button type="primary" @click="openCreate">+ 新建试题</el-button>
        <el-input v-model="keyword" placeholder="搜索试题内容..." size="default" style="width:220px" clearable
          @change="fetchQuestions" />
      </div>
    </div>

    <div class="panel">
      <div class="filter-bar">
        <el-tabs v-model="typeFilter" @tab-change="fetchQuestions">
          <el-tab-pane label="全部试题" name="" />
          <el-tab-pane label="单选题" name="SINGLE" />
          <el-tab-pane label="多选题" name="MULTI" />
          <el-tab-pane label="判断题" name="JUDGE" />
          <el-tab-pane label="填空题" name="FILL" />
          <el-tab-pane label="简答题" name="ESSAY" />
        </el-tabs>
        <div class="filter-right">
          <el-select v-model="courseFilter" placeholder="所属课程" clearable style="width:180px" @change="fetchQuestions">
            <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
          <el-select v-model="diffFilter" placeholder="难度" clearable style="width:120px" @change="fetchQuestions">
            <el-option label="⭐ 容易" :value="1" />
            <el-option label="⭐⭐ 较易" :value="2" />
            <el-option label="⭐⭐⭐ 中等" :value="3" />
            <el-option label="⭐⭐⭐⭐ 较难" :value="4" />
            <el-option label="⭐⭐⭐⭐⭐ 困难" :value="5" />
          </el-select>
        </div>
      </div>

      <el-table :data="questions" v-loading="loading" stripe border style="width:100%">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="content" label="题目内容" min-width="280" show-overflow-tooltip />
        <el-table-column label="题型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.questionType)" size="small">{{ typeLabel(row.questionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度" width="80">
          <template #default="{ row }">{{ '⭐'.repeat(row.difficulty || 1) }}</template>
        </el-table-column>
        <el-table-column prop="knowledgePoint" label="知识点" width="120" show-overflow-tooltip />
        <el-table-column prop="score" label="分值" width="70" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && questions.length === 0" description="暂无试题，点击上方按钮创建" :image-size="100" />

      <el-pagination v-if="total > pageSize" :current-page="page" :page-size="pageSize" :total="total"
        layout="prev, pager, next" @current-change="p => { page = p; fetchQuestions() }"
        style="margin-top:16px;justify-content:center" />
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑试题' : '新建试题'" width="700px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="题目类型" required>
          <el-radio-group v-model="form.questionType" :disabled="isEdit">
            <el-radio value="SINGLE">单选题</el-radio>
            <el-radio value="MULTI">多选题</el-radio>
            <el-radio value="JUDGE">判断题</el-radio>
            <el-radio value="FILL">填空题</el-radio>
            <el-radio value="ESSAY">简答题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="所属课程">
          <el-select v-model="form.courseId" placeholder="选择课程" clearable style="width:100%">
            <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目内容" required>
          <el-input v-model="form.content" type="textarea" :rows="3" placeholder="请输入题目内容" />
        </el-form-item>
        <el-form-item v-if="form.questionType === 'SINGLE' || form.questionType === 'MULTI'" label="选项设置" required>
          <div class="option-list">
            <div v-for="(opt, idx) in optionList" :key="idx" class="option-row">
              <el-radio v-if="form.questionType === 'SINGLE'" v-model="correctSingle" :value="idx"
                style="margin-right:8px" />
              <el-checkbox v-else v-model="correctMulti" :value="idx" style="margin-right:8px" />
              <span class="option-label">{{ optionLetters[idx] }}.</span>
              <el-input v-model="optionList[idx]" placeholder="选项内容" style="flex:1" />
              <el-button v-if="optionList.length > 2" size="small" type="danger" circle
                @click="removeOption(idx)">✕</el-button>
            </div>
          </div>
          <el-button size="small" type="primary" plain @click="addOption" style="margin-top:8px">+ 添加选项</el-button>
        </el-form-item>
        <el-form-item v-if="form.questionType === 'JUDGE'" label="正确答案" required>
          <el-radio-group v-model="form.answer">
            <el-radio value="对">✅ 正确</el-radio>
            <el-radio value="错">❌ 错误</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.questionType === 'FILL' || form.questionType === 'ESSAY'" label="参考答案" required>
          <el-input v-model="form.answer" type="textarea" :rows="2" placeholder="请输入参考答案" />
        </el-form-item>
        <el-form-item label="答案解析">
          <el-input v-model="form.analysis" type="textarea" :rows="2" placeholder="可选，解析将显示在试卷讲评中" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="分值"><el-input-number v-model="form.score" :min="1" :max="100"
                style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度">
              <el-select v-model="form.difficulty" style="width:100%">
                <el-option label="⭐ 容易" :value="1" />
                <el-option label="⭐⭐ 较易" :value="2" />
                <el-option label="⭐⭐⭐ 中等" :value="3" />
                <el-option label="⭐⭐⭐⭐ 较难" :value="4" />
                <el-option label="⭐⭐⭐⭐⭐ 困难" :value="5" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="知识点"><el-input v-model="form.knowledgePoint" placeholder="如：Java基础" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存修改' : '创建试题' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { examApi, courseApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const questions = ref([])
const loading = ref(false)
const page = ref(1); const pageSize = ref(10); const total = ref(0)
const keyword = ref(''); const typeFilter = ref(''); const courseFilter = ref(null); const diffFilter = ref(null)
const courseList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)

const form = reactive({
  questionType: 'SINGLE', courseId: null, content: '', answer: '', analysis: '',
  score: 5, difficulty: 3, knowledgePoint: ''
})

const optionLetters = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']
const optionList = ref(['', '', '', ''])
const correctSingle = ref(0)
const correctMulti = ref([])

const typeTag = (t) => ({ SINGLE: 'primary', MULTI: 'success', JUDGE: 'warning', FILL: 'info', ESSAY: 'danger' }[t] || 'info')
const typeLabel = (t) => ({ SINGLE: '单选', MULTI: '多选', JUDGE: '判断', FILL: '填空', ESSAY: '简答' }[t] || t)

const fetchQuestions = async () => {
  loading.value = true
  try {
    const res = await examApi.listQuestions({
      page: page.value, size: pageSize.value,
      questionType: typeFilter.value || undefined,
      courseId: courseFilter.value || undefined,
      difficulty: diffFilter.value || undefined,
      knowledgePoint: keyword.value || undefined
    })
    if (res.data.code === 200) {
      questions.value = res.data.data.records
      total.value = res.data.data.total
    }
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
  form.questionType = 'SINGLE'; form.courseId = null; form.content = ''; form.answer = ''
  form.analysis = ''; form.score = 5; form.difficulty = 3; form.knowledgePoint = ''
  optionList.value = ['', '', '', '']
  correctSingle.value = 0; correctMulti.value = []
}

const openCreate = () => {
  isEdit.value = false; editId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true; editId.value = row.id
  form.questionType = row.questionType; form.courseId = row.courseId; form.content = row.content
  form.answer = row.answer; form.analysis = row.analysis || ''
  form.score = row.score; form.difficulty = row.difficulty || 3; form.knowledgePoint = row.knowledgePoint || ''
  if (row.questionType === 'SINGLE' || row.questionType === 'MULTI') {
    try { optionList.value = JSON.parse(row.options) } catch { optionList.value = ['', '', '', ''] }
    if (row.questionType === 'SINGLE') {
      correctSingle.value = optionList.value.indexOf(row.answer)
      if (correctSingle.value < 0) correctSingle.value = 0
    } else {
      try { correctMulti.value = JSON.parse(row.answer).map(a => optionList.value.indexOf(a)).filter(i => i >= 0) } catch { correctMulti.value = [] }
    }
  }
  dialogVisible.value = true
}

const addOption = () => { if (optionList.value.length < 8) optionList.value.push('') }
const removeOption = (idx) => { optionList.value.splice(idx, 1); if (correctSingle.value >= optionList.value.length) correctSingle.value = 0 }

watch(() => form.questionType, () => {
  optionList.value = ['', '', '', '']
  correctSingle.value = 0; correctMulti.value = []
  form.answer = ''
})

const handleSubmit = async () => {
  if (!form.content) { ElMessage.warning('请输入题目内容'); return }
  let answer = form.answer
  if (form.questionType === 'SINGLE') {
    const opts = optionList.value.filter(o => o.trim())
    if (opts.length < 2) { ElMessage.warning('请至少填写2个选项'); return }
    answer = opts[correctSingle.value] || opts[0]
  } else if (form.questionType === 'MULTI') {
    const opts = optionList.value.filter(o => o.trim())
    if (opts.length < 2) { ElMessage.warning('请至少填写2个选项'); return }
    if (correctMulti.value.length === 0) { ElMessage.warning('请选择至少一个正确答案'); return }
    answer = JSON.stringify(correctMulti.value.map(i => opts[i]))
  }
  if (!answer && form.questionType !== 'ESSAY') { ElMessage.warning('请设置正确答案'); return }

  submitting.value = true
  try {
    const data = {
      questionType: form.questionType, courseId: form.courseId, content: form.content,
      options: (form.questionType === 'SINGLE' || form.questionType === 'MULTI') ? JSON.stringify(optionList.value.filter(o => o.trim())) : null,
      answer: answer, analysis: form.analysis, score: form.score,
      difficulty: form.difficulty, knowledgePoint: form.knowledgePoint
    }
    if (isEdit.value) {
      await examApi.updateQuestion(editId.value, data)
      ElMessage.success('试题更新成功')
    } else {
      await examApi.createQuestion(data)
      ElMessage.success('试题创建成功')
    }
    dialogVisible.value = false
    fetchQuestions()
  } catch (e) { /* ignore */ }
  finally { submitting.value = false }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该试题？此操作不可恢复', '确认删除', { type: 'warning' })
    await examApi.deleteQuestion(id)
    ElMessage.success('删除成功')
    fetchQuestions()
  } catch (e) { /* ignore */ }
}

onMounted(() => { fetchCourses(); fetchQuestions() })
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

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 24px;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-bar :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.filter-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.option-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.option-label {
  font-weight: 600;
  width: 24px;
  text-align: center;
}
</style>
