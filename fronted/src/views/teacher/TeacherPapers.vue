<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">试卷管理</h2>
      <div class="page-actions">
        <el-button type="success" @click="openAiDialog">AI 智能组卷</el-button>
        <el-button type="primary" @click="openCreateDialog">+ 手动组卷</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="search-bar">
        <el-select v-model="courseFilter" placeholder="按课程筛选" clearable style="width:200px" @change="fetchPapers">
          <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
      </div>

      <el-table v-if="papers.length > 0 || loading" :data="papers" v-loading="loading" stripe class="kechang-table"
        style="width:100%">
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

      <PaginationWrapper v-model:page="page" v-model:size="size" :total="total" @change="fetchPapers" />
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
                style="width:100%" @change="passScoreManual = true" /></el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">选择试题</el-divider>

        <div class="question-select-area">
          <div class="q-filter">
            <el-select v-model="qTypeFilter" placeholder="题型" clearable style="width:120px" @change="loadQuestions">
              <el-option label="单选" value="SINGLE" /><el-option label="多选" value="MULTI" />
              <el-option label="判断" value="JUDGE" /><el-option label="填空" value="FILL" /><el-option label="简答"
                value="ESSAY" /><el-option label="编程" value="CODE" />
            </el-select>
            <el-input v-model="qKeyword" placeholder="搜索题目" style="width:200px" clearable @change="loadQuestions" />
            <span class="selected-count">已选: {{ selectedQuestions.length }} 题 / 合计 {{ selectedTotalScore }} 分</span>
          </div>
          <el-table :data="bankQuestions" v-loading="qLoading" stripe class="kechang-table" max-height="300"
            style="width:100%" @selection-change="onSelectionChange" ref="qTableRef">
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
    <el-dialog v-model="aiDialogVisible" title="AI 智能组卷" width="550px" destroy-on-close>
      <el-form :model="aiForm" label-width="100px">
        <el-form-item label="所属课程" required>
          <el-select v-model="aiForm.courseId" placeholder="请选择课程" style="width:100%">
            <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="试卷名称">
          <el-input v-model="aiForm.paperName" placeholder="留空由 AI 自动生成" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="题目数量">
              <el-input-number v-model="aiForm.questionCount" :min="3" :max="50" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标总分">
              <el-input-number v-model="aiForm.totalScore" :min="10" :max="500" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="考试时长">
              <el-input-number v-model="aiForm.duration" :min="10" :max="300" style="width:100%" />
              <template #extra><span style="font-size:11px;color:#909399">分钟</span></template>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="及格分">
              <el-input-number v-model="aiForm.passScore" :min="1" :max="500" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="知识点范围">
          <el-input v-model="aiForm.knowledgePoints" placeholder="如：Java基础,面向对象" />
        </el-form-item>
        <el-form-item label="难度等级">
          <el-select v-model="aiForm.difficulty" style="width:100%">
            <el-option label="综合（各难度搭配）" :value="null" />
            <el-option label="容易" :value="1" />
            <el-option label="中等" :value="3" />
            <el-option label="困难" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型范围">
          <el-select v-model="aiForm.types" multiple placeholder="不选则包含全部题型" style="width:100%">
            <el-option label="单选题" value="SINGLE" />
            <el-option label="多选题" value="MULTI" />
            <el-option label="判断题" value="JUDGE" />
            <el-option label="填空题" value="FILL" />
            <el-option label="简答题" value="ESSAY" />
            <el-option label="编程题" value="CODE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="aiGenerating" @click="handleAiGenerate">
          {{ aiGenerating ? 'AI 正在选题中...' : '生成预览' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- AI 组卷预览弹窗 -->
    <el-dialog v-model="aiPreviewVisible" title="AI 组卷预览" width="850px" destroy-on-close>
      <div v-if="aiPreviewData">
        <el-descriptions :column="2" border style="margin-bottom:16px">
          <el-descriptions-item label="试卷名称">{{ aiPreviewData.paperName }}</el-descriptions-item>
          <el-descriptions-item label="选题总分">
            <el-tag type="warning">{{ aiPreviewData.totalScore }} 分</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="候选题目数">{{ aiPreviewData.candidateCount }} 题</el-descriptions-item>
          <el-descriptions-item label="选中题目数">{{ aiPreviewData.suggestedQuestions?.length || 0 }}
            题</el-descriptions-item>
        </el-descriptions>
        <el-alert v-if="aiPreviewData.summary" :title="aiPreviewData.summary" type="info" :closable="false"
          style="margin-bottom:12px" />
        <div style="margin-bottom:8px;display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:600">选题明细（可调整分值和移除）</span>
          <span style="color:#909399;font-size:13px">合计：{{ aiAdjustedTotalScore }} 分</span>
        </div>
        <el-table :data="aiPreviewData.suggestedQuestions || []" max-height="400" stripe class="kechang-table"
          style="width:100%">
          <el-table-column label="序号" width="50">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="题型" width="65">
            <template #default="{ row }"><el-tag size="small">{{ typeLabel(row.questionType) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="content" label="题目内容" min-width="200" show-overflow-tooltip />
          <el-table-column prop="knowledgePoint" label="知识点" width="100" />
          <el-table-column label="难度" width="65">
            <template #default="{ row }">
              <el-tag :type="row.difficulty <= 2 ? 'success' : row.difficulty <= 3 ? 'warning' : 'danger'" size="small">
                {{ row.difficulty }}星
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="分值" width="90">
            <template #default="{ row }">
              <el-input-number v-model="aiScoreAdjust[row.questionId]" :min="1" :max="100" size="small"
                style="width:80px" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="65">
            <template #default="{ row }">
              <el-button size="small" type="danger" text @click="removeAiQuestion(row.questionId)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="aiPreviewVisible = false">返回调整</el-button>
        <el-button type="primary" :loading="aiCreating" @click="handleConfirmAiPaper">确认创建试卷</el-button>
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
          <p style="color:var(--bio-primary);font-size:13px">答案: {{ q.answer }}</p>
        </div>
      </div>
      <template #footer><el-button @click="detailDialogVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { examApi, courseApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePagination } from '@/composables/usePagination'
import PaginationWrapper from '@/components/PaginationWrapper.vue'

const papers = ref([]); const loading = ref(false)
const { page, size, total, resetPage } = usePagination({ defaultSize: 10 })
const courseFilter = ref(null)
const courseList = ref([])

// 创建
const createDialogVisible = ref(false)
const submitting = ref(false)
const paperForm = reactive({ paperName: '', courseId: null, totalScore: 100, duration: 90, passScore: 60 })
const passScoreManual = ref(false)
// 总分变化时自动计算及格分（手动改过后不再联动）
watch(() => paperForm.totalScore, (newVal) => {
  if (!passScoreManual.value) {
    paperForm.passScore = Math.round(newVal * 0.6)
  }
})
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
const aiForm = reactive({
  courseId: null, paperName: '', knowledgePoints: '',
  difficulty: null, types: [], questionCount: 10,
  totalScore: 100, duration: 90, passScore: 60
})

// AI 预览
const aiPreviewVisible = ref(false)
const aiPreviewData = ref(null)
const aiCreating = ref(false)
const aiScoreAdjust = reactive({})
const aiAdjustedTotalScore = computed(() => {
  if (!aiPreviewData.value?.suggestedQuestions) return 0
  return aiPreviewData.value.suggestedQuestions.reduce((s, q) => s + (aiScoreAdjust[q.questionId] || q.questionScore || 0), 0)
})

// 详情
const detailDialogVisible = ref(false)
const paperDetail = ref(null)

const typeLabel = (t) => ({ SINGLE: '单选', MULTI: '多选', JUDGE: '判断', FILL: '填空', ESSAY: '简答', CODE: '编程' }[t] || t)

const fetchPapers = async () => {
  loading.value = true
  try {
    const res = await examApi.listPapers({ page: page.value, size: size.value, courseId: courseFilter.value || undefined })
    if (res.data.code === 200) {
      // 将 courseId 映射为 courseName，以便表格显示"所属课程"
      const courseMap = Object.fromEntries(courseList.value.map(c => [c.id, c.courseName]))
      papers.value = (res.data.data.records || []).map(p => ({
        ...p,
        courseName: courseMap[p.courseId] || '未知课程'
      }))
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
  paperForm.paperName = ''; paperForm.courseId = null; paperForm.totalScore = 100; paperForm.duration = 90
  passScoreManual.value = false; paperForm.passScore = Math.round(paperForm.totalScore * 0.6)
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

const openAiDialog = () => {
  aiForm.courseId = null; aiForm.paperName = ''; aiForm.knowledgePoints = ''
  aiForm.difficulty = null; aiForm.types = []; aiForm.questionCount = 10
  aiForm.totalScore = 100; aiForm.duration = 90; aiForm.passScore = 60
  aiPreviewData.value = null
  Object.keys(aiScoreAdjust).forEach(k => delete aiScoreAdjust[k])
  aiDialogVisible.value = true
}

const handleAiGenerate = async () => {
  if (!aiForm.courseId) { ElMessage.warning('请选择所属课程'); return }
  if (aiForm.questionCount < 3) { ElMessage.warning('题目数量至少为 3'); return }
  aiGenerating.value = true
  try {
    const data = {
      courseId: aiForm.courseId,
      paperName: aiForm.paperName || undefined,
      knowledgePoints: aiForm.knowledgePoints || undefined,
      difficulty: aiForm.difficulty,
      types: aiForm.types.length > 0 ? aiForm.types.join(',') : undefined,
      questionCount: aiForm.questionCount,
      totalScore: aiForm.totalScore,
      duration: aiForm.duration,
      passScore: aiForm.passScore
    }
    const res = await examApi.aiGenerate(data)
    if (res.data.code === 200) {
      aiPreviewData.value = res.data.data
      // 初始化分值调整映射
      Object.keys(aiScoreAdjust).forEach(k => delete aiScoreAdjust[k])
        ; (res.data.data.suggestedQuestions || []).forEach(q => {
          aiScoreAdjust[q.questionId] = q.questionScore
        })
      aiDialogVisible.value = false
      aiPreviewVisible.value = true
      ElMessage.success('AI 选题完成，请预览确认')
    } else {
      ElMessage.error(res.data.message || 'AI 组卷失败')
    }
  } catch (e) {
    ElMessage.error('AI 组卷请求失败，请检查网络或 API 配置')
  }
  finally { aiGenerating.value = false }
}

const removeAiQuestion = (questionId) => {
  if (!aiPreviewData.value?.suggestedQuestions) return
  aiPreviewData.value.suggestedQuestions = aiPreviewData.value.suggestedQuestions.filter(q => q.questionId !== questionId)
  delete aiScoreAdjust[questionId]
}

const handleConfirmAiPaper = async () => {
  const questions = aiPreviewData.value?.suggestedQuestions || []
  if (questions.length === 0) { ElMessage.warning('请至少保留一道题目'); return }

  // 使用 AI 建议的试卷参数（用户在对话框设置的参数）
  const paperName = aiPreviewData.value.paperName || 'AI 组卷'
  const totalScore = aiAdjustedTotalScore.value
  const duration = aiForm.duration || 90
  const passScore = aiForm.passScore || 60

  if (passScore > totalScore) {
    ElMessage.warning(`及格分(${passScore})不能大于总分(${totalScore})`)
    return
  }

  aiCreating.value = true
  try {
    const data = {
      paperName,
      courseId: aiForm.courseId,
      totalScore,
      duration,
      passScore,
      questions: questions.map((q, i) => ({
        questionId: q.questionId,
        questionScore: aiScoreAdjust[q.questionId] || q.questionScore || 5,
        sortOrder: i + 1
      }))
    }
    const res = await examApi.createPaper(data)
    if (res.data.code === 200) {
      ElMessage.success('AI 组卷创建成功')
      aiPreviewVisible.value = false
      fetchPapers()
    } else {
      ElMessage.error(res.data.message || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建试卷失败')
  }
  finally { aiCreating.value = false }
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

onMounted(async () => { await fetchCourses(); fetchPapers() })
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
  background-color: rgba(32, 196, 204, 0.06);
}

:deep(.kechang-table .el-table__body tr.el-table__row:hover td) {
  background-color: rgba(32, 196, 204, 0.08) !important;
}
</style>
