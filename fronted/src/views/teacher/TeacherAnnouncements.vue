<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">公告管理</h2>
      <el-button type="primary" @click="openCreateDialog">+ 发布公告</el-button>
    </div>

    <div class="panel">
      <div class="search-bar">
        <el-select v-model="courseFilter" placeholder="按课程筛选" clearable style="width:200px"
          @change="fetchAnnouncements">
          <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
      </div>

      <el-table v-if="announcements.length > 0 || loading" :data="announcements" v-loading="loading" stripe
        class="kechang-table" style="width:100%">
        <el-table-column prop="title" label="公告标题" min-width="180" />
        <el-table-column prop="courseName" label="关联课程" min-width="120" />
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column label="确认人数" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.confirmCount > 0 ? 'success' : 'info'" size="small">{{ row.confirmCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="success" @click="viewConfirms(row)">查看确认</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && announcements.length === 0" description="暂无公告" :image-size="100" />
      <el-pagination v-if="total > pageSize" :current-page="page" :page-size="pageSize" :total="total"
        layout="prev, pager, next" @current-change="p => { page = p; fetchAnnouncements() }"
        style="margin-top:16px;justify-content:center" />
    </div>

    <!-- 发布/编辑公告弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '发布公告'" width="620px" destroy-on-close
      @closed="resetForm">
      <el-form :model="form" label-width="90px">
        <el-form-item label="公告标题" required>
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="关联课程" required>
          <el-select v-model="form.courseId" style="width:100%" placeholder="选择关联课程">
            <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="公告内容" required>
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '发布公告' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 确认列表弹窗 -->
    <el-dialog v-model="confirmDialogVisible" :title="'确认列表 - ' + confirmTitle" width="650px" destroy-on-close>
      <el-table :data="confirmList" stripe class="kechang-table" style="width:100%">
        <el-table-column prop="studentName" label="学生姓名" min-width="120" />
        <el-table-column prop="studentNo" label="学号" min-width="120" />
        <el-table-column prop="username" label="用户名" min-width="100" />
        <el-table-column label="确认时间" width="170">
          <template #default="{ row }">{{ formatTime(row.confirmTime) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="confirmList.length === 0" description="暂无学生确认" :image-size="80" />
      <el-pagination v-if="confirmTotal > 10" :current-page="confirmPage" :page-size="10" :total="confirmTotal"
        layout="prev, pager, next" @current-change="p => { confirmPage = p; viewConfirms(currentRow) }"
        style="margin-top:16px;justify-content:center" />
      <template #footer>
        <el-button @click="confirmDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { announcementApi, courseApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const announcements = ref([]); const loading = ref(false)
const page = ref(1); const pageSize = ref(10); const total = ref(0)
const courseFilter = ref(null)
const courseList = ref([])

// 创建/编辑
const dialogVisible = ref(false); const isEdit = ref(false); const editId = ref(null)
const submitting = ref(false)
const form = reactive({ title: '', courseId: null, content: '' })

// 确认列表
const confirmDialogVisible = ref(false); const confirmTitle = ref('')
const confirmList = ref([]); const confirmPage = ref(1); const confirmTotal = ref(0)
const currentRow = ref(null)

const formatTime = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const fetchAnnouncements = async () => {
  loading.value = true
  try {
    const res = await announcementApi.list({
      page: page.value,
      size: pageSize.value,
      courseId: courseFilter.value || undefined
    })
    if (res.data.code === 200) {
      announcements.value = res.data.data.records
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
  form.title = ''; form.courseId = null; form.content = ''
  isEdit.value = false; editId.value = null
}

const openCreateDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = async (row) => {
  try {
    const res = await announcementApi.detail(row.id)
    if (res.data.code !== 200) { ElMessage.error('获取公告详情失败'); return }
    const d = res.data.data
    form.title = d.title || ''
    form.courseId = d.courseId
    form.content = d.content || ''
    isEdit.value = true; editId.value = d.id
    dialogVisible.value = true
  } catch (e) { ElMessage.error('获取公告详情失败') }
}

const handleSubmit = async () => {
  if (!form.title) { ElMessage.warning('请输入公告标题'); return }
  if (!form.courseId) { ElMessage.warning('请选择关联课程'); return }
  if (!form.content) { ElMessage.warning('请输入公告内容'); return }
  submitting.value = true
  try {
    if (isEdit.value) {
      await announcementApi.update(editId.value, form)
      ElMessage.success('公告已更新')
    } else {
      await announcementApi.create(form)
      ElMessage.success('公告已发布')
    }
    dialogVisible.value = false
    fetchAnnouncements()
  } catch (e) { /* ignore */ }
  finally { submitting.value = false }
}

const viewConfirms = async (row) => {
  currentRow.value = row
  confirmTitle.value = row.title
  confirmPage.value = 1
  confirmList.value = []
  try {
    const res = await announcementApi.getConfirms(row.id, { page: confirmPage.value, size: 10 })
    if (res.data.code === 200) {
      confirmList.value = res.data.data.records
      confirmTotal.value = res.data.data.total
    }
    confirmDialogVisible.value = true
  } catch (e) { /* ignore */ }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该公告？', '确认删除', { type: 'warning' })
    await announcementApi.delete(id)
    ElMessage.success('公告已删除')
    fetchAnnouncements()
  } catch (e) { if (e !== 'cancel') { /* ignore */ } }
}

onMounted(() => { fetchCourses(); fetchAnnouncements() })
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
