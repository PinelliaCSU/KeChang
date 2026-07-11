<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">教学评价</h2>
      <el-button type="primary" @click="openCreateDialog">+ 创建评价</el-button>
    </div>

    <div class="panel">
      <div class="search-bar">
        <el-select v-model="courseFilter" placeholder="按课程筛选" clearable style="width:200px" @change="fetchEvaluations">
          <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
        <el-tabs v-model="typeFilter" @tab-change="fetchEvaluations" style="margin-left:16px">
          <el-tab-pane label="全部" name="" />
          <el-tab-pane label="学生评教" name="学生评教" />
          <el-tab-pane label="教师评学" name="教师评学" />
        </el-tabs>
      </div>

      <el-table v-if="evaluations.length > 0 || loading" :data="evaluations" v-loading="loading" stripe
        class="kechang-table" style="width:100%">
        <el-table-column prop="evaluationName" label="评价名称" min-width="200" />
        <el-table-column label="评价类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.evaluationType === '学生评教' ? 'primary' : 'success'" size="small">{{ row.evaluationType
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(computeStatus(row.startDate, row.endDate))" size="small">{{
              computeStatus(row.startDate, row.endDate) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewResults(row)">结果</el-button>
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && evaluations.length === 0" description="暂无教学评价" :image-size="100" />
      <el-pagination v-if="total > pageSize" :current-page="page" :page-size="pageSize" :total="total"
        layout="prev, pager, next" @current-change="p => { page = p; fetchEvaluations() }"
        style="margin-top:16px;justify-content:center" />
    </div>

    <!-- 创建/编辑评价弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEditMode ? '编辑教学评价' : '创建教学评价'" width="650px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="评价名称" required><el-input v-model="form.evaluationName" /></el-form-item>
        <el-form-item label="评价类型" required>
          <el-select v-model="form.evaluationType" style="width:100%">
            <el-option label="学生评教" value="学生评教" /><el-option label="教师评学" value="教师评学" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联课程">
          <el-select v-model="form.courseId" style="width:100%">
            <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date"
                style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结束日期"><el-date-picker v-model="form.endDate" type="date"
                style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="未开始" value="未开始" />
            <el-option label="进行中" value="进行中" />
            <el-option label="已结束" value="已结束" />
          </el-select>
        </el-form-item>

        <!-- 评价指标项管理 -->
        <el-divider>评价指标项</el-divider>
        <div v-for="(item, idx) in form.items" :key="idx" class="item-row">
          <el-row :gutter="8" style="margin-bottom:8px;align-items:center">
            <el-col :span="1">
              <span class="item-idx">{{ idx + 1 }}.</span>
            </el-col>
            <el-col :span="11">
              <el-input v-model="item.itemContent" placeholder="请输入指标内容" size="small" />
            </el-col>
            <el-col :span="5">
              <el-select v-model="item.itemType" size="small" style="width:100%">
                <el-option label="评分(RADIO)" value="RADIO" />
                <el-option label="文本(TEXT)" value="TEXT" />
              </el-select>
            </el-col>
            <el-col :span="4">
              <el-button size="small" :disabled="form.items.length <= 1" @click="form.items.splice(idx, 1)" text
                type="danger">
                删除
              </el-button>
            </el-col>
          </el-row>
        </div>
        <el-button size="small"
          @click="form.items.push({ itemContent: '', itemType: 'RADIO', sortOrder: form.items.length })">
          + 添加指标项
        </el-button>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="isEditMode ? handleUpdate() : handleCreate()">
          {{ isEditMode ? '保存修改' : '创建评价' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 评价结果弹窗 -->
    <el-dialog v-model="resultDialogVisible" title="评价结果详情" width="800px" :close-on-click-modal="false">
      <div v-loading="resultLoading">
        <template v-if="results.length > 0">
          <div v-for="(stu, si) in results" :key="stu.evaluatorId" class="student-result-card">
            <div class="student-header" @click="stu.expanded = !stu.expanded">
              <span class="student-index">{{ si + 1 }}.</span>
              <span class="student-name">{{ stu.evaluatorName }}</span>
              <el-tag size="small" type="success">{{stu.items?.filter(i => i.score != null).length || 0}} 项评分</el-tag>
              <el-icon :class="{ 'arrow-up': stu.expanded }" class="arrow-icon">
                <ArrowDown />
              </el-icon>
            </div>
            <template v-if="stu.expanded">
              <div class="student-items">
                <div v-for="(item, ii) in stu.items" :key="item.itemId" class="student-item-row">
                  <span class="item-label">{{ ii + 1 }}. {{ item.itemContent }}</span>
                  <span v-if="item.itemType === 'RADIO'" class="item-score">
                    <el-rate v-model="item.score" disabled show-score score-template="{value} 分"
                      :colors="['#99a9bf', '#f7ba2a', '#ff9900']" />
                  </span>
                  <span v-else class="item-text">
                    {{ item.content || '（未填写）' }}
                  </span>
                </div>
              </div>
            </template>
          </div>
        </template>
        <el-empty v-else-if="!resultLoading" description="暂无学生提交评价" />
      </div>
      <template #footer><el-button @click="resultDialogVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { evaluationApi, courseApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { computeStatus, statusTagType } from '@/utils/status'

const evaluations = ref([]); const loading = ref(false)
const page = ref(1); const pageSize = ref(10); const total = ref(0)
const courseFilter = ref(null); const typeFilter = ref('')
const courseList = ref([])

// 创建 / 编辑
const dialogVisible = ref(false); const submitting = ref(false)
const isEditMode = ref(false)
const editingId = ref(null)
const form = reactive({
  evaluationName: '',
  evaluationType: '学生评教',
  courseId: null,
  startDate: '',
  endDate: '',
  status: '进行中',
  items: [
    { itemContent: '教学态度认真负责', itemType: 'RADIO' },
    { itemContent: '教学内容清晰易懂', itemType: 'RADIO' },
    { itemContent: '课堂互动充分有效', itemType: 'RADIO' },
    { itemContent: '总体评价与建议', itemType: 'TEXT' }
  ]
})

// 结果
const resultDialogVisible = ref(false)
const results = ref([])        // 指标统计
const resultLoading = ref(false)

const fetchEvaluations = async () => {
  loading.value = true
  try {
    const res = await evaluationApi.list({ page: page.value, size: pageSize.value, courseId: courseFilter.value || undefined })
    if (res.data.code === 200) {
      let data = res.data.data.records
      if (typeFilter.value) data = data.filter(e => e.evaluationType === typeFilter.value)
      evaluations.value = data
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
  form.evaluationName = ''
  form.evaluationType = '学生评教'
  form.courseId = null
  form.startDate = ''
  form.endDate = ''
  form.status = '进行中'
  form.items = [
    { itemContent: '教学态度认真负责', itemType: 'RADIO' },
    { itemContent: '教学内容清晰易懂', itemType: 'RADIO' },
    { itemContent: '课堂互动充分有效', itemType: 'RADIO' },
    { itemContent: '总体评价与建议', itemType: 'TEXT' }
  ]
}

const openCreateDialog = () => {
  isEditMode.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = async (row) => {
  isEditMode.value = true
  editingId.value = row.id
  // 填充表单
  form.evaluationName = row.evaluationName || ''
  form.evaluationType = row.evaluationType || '学生评教'
  form.courseId = row.courseId || null
  form.startDate = row.startDate || ''
  form.endDate = row.endDate || ''
  form.status = row.status || '未开始'
  form.items = (row.items && row.items.length > 0)
    ? row.items.map(item => ({ itemContent: item.itemContent, itemType: item.itemType }))
    : [{ itemContent: '教学态度认真负责', itemType: 'RADIO' }]
  dialogVisible.value = true
}

const validateForm = () => {
  if (!form.evaluationName) { ElMessage.warning('请输入评价名称'); return false }
  if (!form.items || form.items.length === 0) { ElMessage.warning('请至少添加一个评价指标项'); return false }
  for (const item of form.items) {
    if (!item.itemContent.trim()) { ElMessage.warning('请填写所有指标项内容'); return false }
  }
  return true
}

const handleCreate = async () => {
  if (!validateForm()) return
  submitting.value = true
  try {
    await evaluationApi.create({
      ...form,
      items: form.items.map((item, idx) => ({ ...item, sortOrder: idx }))
    })
    ElMessage.success('评价创建成功')
    dialogVisible.value = false
    fetchEvaluations()
  } catch (e) { /* ignore */ }
  finally { submitting.value = false }
}

const handleUpdate = async () => {
  if (!validateForm()) return
  submitting.value = true
  try {
    await evaluationApi.update(editingId.value, {
      ...form,
      items: form.items.map((item, idx) => ({ ...item, sortOrder: idx }))
    })
    ElMessage.success('评价更新成功')
    dialogVisible.value = false
    fetchEvaluations()
  } catch (e) { /* ignore */ }
  finally { submitting.value = false }
}

const viewResults = async (row) => {
  resultDialogVisible.value = true
  resultLoading.value = true
  results.value = []
  try {
    const res = await evaluationApi.getResults(row.id)
    if (res.data.code === 200) {
      const data = res.data.data || []
      // 为每个学生添加 expanded 折叠控制
      results.value = data.map(s => ({ ...s, expanded: false }))
    }
  } catch (e) { /* ignore */ }
  finally { resultLoading.value = false }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('删除后评价数据将无法恢复，确定删除？', '确认删除', {
      type: 'warning',
      confirmButtonText: '确定删除',
      confirmButtonClass: 'el-button--danger'
    })
    await evaluationApi.delete(id)
    ElMessage.success('评价已删除')
    fetchEvaluations()
  } catch (e) {
    if (e !== 'cancel') { /* ignore actual errors */ }
  }
}

onMounted(() => { fetchCourses(); fetchEvaluations() })
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
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.search-bar :deep(.el-tabs__header) {
  margin-bottom: 0;
}

/* 学生评价结果卡片 */
.student-result-card {
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
  overflow: hidden;
}

.student-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}

.student-header:hover {
  background: #f0f5ff;
}

.student-index {
  font-weight: 600;
  color: var(--bio-primary);
  min-width: 20px;
}

.student-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.arrow-icon {
  font-size: 14px;
  color: #c0c4cc;
  transition: transform 0.25s;
}

.arrow-up {
  transform: rotate(180deg);
}

.student-items {
  padding: 0 16px 12px;
  border-top: 1px solid #ebeef5;
}

.student-item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.student-item-row:last-child {
  border-bottom: none;
}

.item-label {
  font-size: 13px;
  color: #606266;
  flex: 1;
  min-width: 0;
}

.item-score {
  flex-shrink: 0;
}

.item-text {
  font-size: 13px;
  color: #909399;
  max-width: 300px;
  word-break: break-all;
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
