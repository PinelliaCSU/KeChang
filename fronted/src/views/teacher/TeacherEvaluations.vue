<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">⭐ 教学评价</h2>
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

      <el-table :data="evaluations" v-loading="loading" stripe border style="width:100%">
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
            <el-tag :type="row.status === '进行中' ? 'success' : 'info'" size="small">{{ row.status || '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewResults(row)">查看结果</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && evaluations.length === 0" description="暂无教学评价" :image-size="100" />
      <el-pagination v-if="total > pageSize" :current-page="page" :page-size="pageSize" :total="total"
        layout="prev, pager, next" @current-change="p => { page = p; fetchEvaluations() }"
        style="margin-top:16px;justify-content:center" />
    </div>

    <!-- 创建评价弹窗 -->
    <el-dialog v-model="dialogVisible" title="创建教学评价" width="500px" destroy-on-close>
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">创建评价</el-button>
      </template>
    </el-dialog>

    <!-- 评价结果弹窗 -->
    <el-dialog v-model="resultDialogVisible" title="评价结果" width="750px">
      <el-table :data="results" v-loading="resultLoading" stripe border>
        <el-table-column prop="evaluatorId" label="评价人ID" width="100" />
        <el-table-column prop="itemId" label="指标ID" width="80" />
        <el-table-column prop="score" label="评分" width="80">
          <template #default="{ row }">
            <el-rate v-model="row.score" disabled show-score text-color="#ff9900" />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评语" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="评价时间" width="170" />
      </el-table>
      <el-empty v-if="!resultLoading && results.length === 0" description="暂无评价结果" />
      <template #footer><el-button @click="resultDialogVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { evaluationApi, courseApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const evaluations = ref([]); const loading = ref(false)
const page = ref(1); const pageSize = ref(10); const total = ref(0)
const courseFilter = ref(null); const typeFilter = ref('')
const courseList = ref([])

// 创建
const dialogVisible = ref(false); const submitting = ref(false)
const form = reactive({ evaluationName: '', evaluationType: '学生评教', courseId: null, startDate: '', endDate: '' })

// 结果
const resultDialogVisible = ref(false); const results = ref([]); const resultLoading = ref(false)

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

const openCreateDialog = () => {
  form.evaluationName = ''; form.evaluationType = '学生评教'; form.courseId = null; form.startDate = ''; form.endDate = ''
  dialogVisible.value = true
}

const handleCreate = async () => {
  if (!form.evaluationName) { ElMessage.warning('请输入评价名称'); return }
  submitting.value = true
  try {
    await evaluationApi.create(form)
    ElMessage.success('评价创建成功')
    dialogVisible.value = false
    fetchEvaluations()
  } catch (e) { /* ignore */ }
  finally { submitting.value = false }
}

const viewResults = async (row) => {
  resultDialogVisible.value = true
  resultLoading.value = true
  try {
    const res = await evaluationApi.getResults(row.id)
    if (res.data.code === 200) results.value = res.data.data || []
  } catch (e) { /* ignore */ }
  finally { resultLoading.value = false }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该评价？', '确认删除', { type: 'warning' })
    ElMessage.warning('删除接口暂未实现')
  } catch (e) { /* ignore */ }
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
</style>
