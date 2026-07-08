<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">试卷管理</h2>
      <div class="page-actions">
        <el-button type="success" @click="openAiDialog">🤖 AI 智能组卷</el-button>
        <el-button type="primary" @click="openCreateDialog">+ 手动组卷</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="search-bar">
        <el-select v-model="courseFilter" placeholder="按课程筛选" clearable style="width:200px" @change="fetchPapers">
          <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
      </div>

      <el-table :data="papers" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="paperName" label="试卷名称" min-width="200" />
        <el-table-column prop="courseName" label="所属课程" min-width="140" />
        <el-table-column prop="totalScore" label="总分" width="90" />
        <el-table-column prop="duration" label="时长(分)" width="90" />
        <el-table-column prop="passScore" label="及格分" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '已发布' ? 'success' : 'info'" size="small">{{ row.status || '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewPaper(row)">查看</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && papers.length === 0" description="暂无试卷，点击上方按钮创建" :image-size="100" />

      <el-pagination v-if="total > pageSize" :current-page="page" :page-size="pageSize" :total="total"
        layout="prev, pager, next" @current-change="p => { page = p; fetchPapers() }"
        style="margin-top:16px;justify-content:center" />
    </div>

    <!-- 手动组卷弹窗 -->
    <el-dialog v-model="createDialogVisible" title="手动组卷" width="800px" destroy-on-close>
      <el-form :model="paperForm" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="试卷名称" required><el-input v-model="paperForm.paperName"
                placeholder="如：期中考试卷" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属课程" required>
              <el-select v-model="paperForm.courseId" placeholder="选择课程" style="width:100%" @change="onCourseChange">
                <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="总分"><el-input-number v-model="paperForm.totalScore" :min="1"
                style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时长(分钟)"><el-input-number v-model="paperForm.duration" :min="1"
                style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="及格分"><el-input-number v-model="paperForm.passScore" :min="1"
                style="width:100%" /></el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">选择试题</el-divider>

        <div class="question-select-area">
          <div class="q-filter">
            <el-select v-model="qTypeFilter" placeholder="题型" clearable style="width:120px" @change="loadQuestions">
              <el-option label="单选" value="SINGLE" /><el-option label="多选" value="MULTI" />
              <el-option label="判断" value="JUDGE" /><el-option label="填空" value="FILL" /><el-option label="简答"
                value="ESSAY" />
            </el-select>
            <el-input v-model="qKeyword" placeholder="搜索题目" style="width:200px" clearable @change="loadQuestions" />
            <span class="selected-count">已选: {{ selectedQuestions.length }} 题 / 合计 {{ selectedTotalScore }} 分</span>
          </div>
          <el-table :data="bankQuestions" v-loading="qLoading" stripe border max-height="300" style="width:100%"
            @selection-change="onSelectionChange" ref="qTableRef">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="content" label="题目" min-width="240" show-overflow-tooltip />
            <el-table-column label="题型" width="70">
              <template #default="{ row }"><el-tag size="small">{{ typeLabel(row.questionType) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="选中分值" width="90">
              <template #default="{ row }">
                <el-input-number v-if="selectedIds.has(row.id)" v-model="selectedScoreMap[row.id]" :min="1" :max="100"
                  size="small" style="width:80px" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreatePaper">创建试卷</el-button>
      </template>
    </el-dialog>

    <!-- AI 组卷弹窗 -->
    <el-dialog v-model="aiDialogVisible" title="AI 智能组卷" width="500px">
      <el-form label-width="100px">
        <el-form-item label="知识点范围"><el-input v-model="aiForm.knowledgePoints"
            placeholder="如：Java基础,面向对象" /></el-form-item>
        <el-form-item label="难度等级">
          <el-select v-model="aiForm.difficulty" style="width:100%">
            <el-option label="综合" value="" /><el-option label="容易" value="1" /><el-option label="中等"
              value="3" /><el-option label="困难" value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型范围"><el-input v-model="aiForm.types" placeholder="如：SINGLE,MULTI,JUDGE" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="aiGenerating" @click="handleAiGenerate">开始生成</el-button>
      </template>
    </el-dialog>

    <!-- 试卷详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="试卷详情" width="750px">
      <div v-if="paperDetail">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="试卷名称">{{ paperDetail.paperName }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ paperDetail.totalScore }}分</el-descriptions-item>
          <el-descriptions-item label="时长">{{ paperDetail.duration }}分钟</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin-top:20px">试题列表（{{ paperDetail.questions?.length || 0 }}题）</h4>
        <div v-for="(q, idx) in (paperDetail.questions || [])" :key="q.id" class="paper-question-item">
          <p><b>{{ idx + 1 }}. [{{ typeLabel(q.questionType) }}]</b> {{ q.content }} <el-tag size="small"
              type="warning">{{
                q.score }}分</el-tag></p>
          <p v-if="q.options" style="color:#909399;font-size:13px">
            选项: {{ JSON.parse(q.options || '[]').join(' | ') }}
          </p>
          <p style="color:#67c23a;font-size:13px">答案: {{ q.answer }}</p>
        </div>
      </div>
      <template #footer><el-button @click="detailDialogVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { examApi, courseApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const papers = ref([])
const loading = ref(false)
const page = ref(1); const pageSize = ref(10); const total = ref(0)
const courseFilter = ref(null)
const courseList = ref([])

// 创建
const createDialogVisible = ref(false)
const submitting = ref(false)
const paperForm = reactive({ paperName: '', courseId: null, totalScore: 100, duration: 90, passScore: 60 })
const bankQuestions = ref([])
const qLoading = ref(false)
const qTypeFilter = ref(''); const qKeyword = ref('')
const selectedQuestions = ref([])
const selectedIds = computed(() => new Set(selectedQuestions.value.map(q => q.id)))
const selectedScoreMap = reactive({})
const selectedTotalScore = computed(() => selectedQuestions.value.reduce((s, q) => s + (selectedScoreMap[q.id] || q.score || 5), 0))
const qTableRef = ref(null)

// AI
const aiDialogVisible = ref(false)
const aiGenerating = ref(false)
const aiForm = reactive({ knowledgePoints: '', difficulty: '', types: '' })

// 详情
const detailDialogVisible = ref(false)
const paperDetail = ref(null)

const typeLabel = (t) => ({ SINGLE: '单选', MULTI: '多选', JUDGE: '判断', FILL: '填空', ESSAY: '简答' }[t] || t)

const fetchPapers = async () => {
  loading.value = true
  try {
    const res = await examApi.listPapers({ page: page.value, size: pageSize.value, courseId: courseFilter.value || undefined })
    if (res.data.code === 200) {
      papers.value = res.data.data.records
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

const loadQuestions = async () => {
  qLoading.value = true
  try {
    const res = await examApi.listQuestions({
      page: 1, size: 200,
      questionType: qTypeFilter.value || undefined,
      courseId: paperForm.courseId || undefined,
      knowledgePoint: qKeyword.value || undefined
    })
    if (res.data.code === 200) bankQuestions.value = res.data.data.records
  } catch (e) { /* ignore */ }
  finally { qLoading.value = false }
}

const onCourseChange = () => { bankQuestions.value = []; selectedQuestions.value = []; loadQuestions() }

const openCreateDialog = () => {
  paperForm.paperName = ''; paperForm.courseId = null; paperForm.totalScore = 100; paperForm.duration = 90; paperForm.passScore = 60
  bankQuestions.value = []; selectedQuestions.value = []
  Object.keys(selectedScoreMap).forEach(k => delete selectedScoreMap[k])
  createDialogVisible.value = true
}

const onSelectionChange = (rows) => {
  selectedQuestions.value = rows
  rows.forEach(r => { if (!selectedScoreMap[r.id]) selectedScoreMap[r.id] = r.score || 5 })
}

const handleCreatePaper = async () => {
  if (!paperForm.paperName || !paperForm.courseId) { ElMessage.warning('请填写试卷名称和所属课程'); return }
  if (selectedQuestions.value.length === 0) { ElMessage.warning('请至少选择一道试题'); return }
  // 校验及格分不能大于总分
  if (paperForm.passScore > paperForm.totalScore) {
    ElMessage.warning(`及格分(${paperForm.passScore})不能大于总分(${paperForm.totalScore})`)
    return
  }
  // 校验总分与题目分值合计是否一致
  if (paperForm.totalScore !== selectedTotalScore.value) {
    try {
      await ElMessageBox.confirm(
        `试卷总分(${paperForm.totalScore})与题目分值合计(${selectedTotalScore.value})不一致，是否自动将总分修正为合计值？`,
        '分数不一致',
        { confirmButtonText: '自动修正', cancelButtonText: '保持不变', type: 'warning' }
      )
      paperForm.totalScore = selectedTotalScore.value
    } catch (e) {
      // 用户选择保持不变，继续提交
    }
  }
  submitting.value = true
  try {
    const data = {
      paperName: paperForm.paperName, courseId: paperForm.courseId,
      totalScore: paperForm.totalScore, duration: paperForm.duration, passScore: paperForm.passScore,
      questions: selectedQuestions.value.map((q, i) => ({
        questionId: q.id, questionScore: selectedScoreMap[q.id] || q.score || 5, sortOrder: i + 1
      }))
    }
    const res = await examApi.createPaper(data)
    if (res.data.code === 200) {
      ElMessage.success('试卷创建成功')
      createDialogVisible.value = false
      fetchPapers()
    } else {
      ElMessage.error(res.data.message || '创建失败')
    }
  } catch (e) { /* ignore */ }
  finally { submitting.value = false }
}

const openAiDialog = () => { aiForm.knowledgePoints = ''; aiForm.difficulty = ''; aiForm.types = ''; aiDialogVisible.value = true }

const handleAiGenerate = async () => {
  aiGenerating.value = true
  try {
    await examApi.aiGenerate({
      knowledgePoints: aiForm.knowledgePoints || undefined,
      difficulty: aiForm.difficulty || undefined,
      types: aiForm.types || undefined
    })
    ElMessage.success('AI 组卷成功，请刷新列表查看')
    aiDialogVisible.value = false
    fetchPapers()
  } catch (e) { /* ignore */ }
  finally { aiGenerating.value = false }
}

const viewPaper = async (row) => {
  try {
    const res = await examApi.getPaper(row.id)
    if (res.data.code === 200) {
      paperDetail.value = res.data.data
      detailDialogVisible.value = true
    }
  } catch (e) { /* ignore */ }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该试卷？', '确认删除', { type: 'warning' })
    await examApi.deletePaper(id)
    ElMessage.success('删除成功')
    fetchPapers()
  } catch (e) { /* ignore */ }
}

onMounted(() => { fetchCourses(); fetchPapers() })
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

.search-bar {
  margin-bottom: 16px;
}

.question-select-area {
  margin-top: 8px;
}

.q-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.selected-count {
  color: #1890ff;
  font-weight: 600;
  font-size: 14px;
  margin-left: auto;
}

.paper-question-item {
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.paper-question-item p {
  margin: 4px 0;
}
</style>
