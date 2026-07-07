<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">作业管理</h2>
      <el-button type="primary" @click="openCreate">+ 布置作业</el-button>
    </div>

    <div class="panel">
      <div class="search-bar">
        <el-select v-model="courseFilter" placeholder="按课程筛选" clearable style="width:200px" @change="fetchAssignments">
          <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
      </div>

      <el-table :data="assignments" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="title" label="作业标题" min-width="200" />
        <el-table-column prop="courseName" label="所属课程" min-width="120" />
        <el-table-column prop="deadline" label="截止时间" width="170" />
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column prop="submitCount" label="提交/总人数" width="110">
          <template #default="{ row }">{{ row.submitCount || 0 }}/{{ row.totalStudents || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '进行中' ? 'success' : 'info'" size="small">{{ row.status || '进行中' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="info" @click="viewSubmits(row)">批阅</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && assignments.length === 0" description="暂无作业" :image-size="100" />
      <el-pagination v-if="total > pageSize" :current-page="page" :page-size="pageSize" :total="total"
        layout="prev, pager, next" @current-change="p => { page = p; fetchAssignments() }"
        style="margin-top:16px;justify-content:center" />
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑作业' : '布置作业'" width="550px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="作业标题" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="所属课程" required>
          <el-select v-model="form.courseId" placeholder="选择课程" style="width:100%">
            <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作业内容"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="截止时间" required>
              <el-date-picker v-model="form.deadline" type="datetime" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总分"><el-input-number v-model="form.totalScore" :min="1" :max="100"
                style="width:100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="附件链接"><el-input v-model="form.attachmentUrl" placeholder="可选，附件URL" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存修改' : '布置作业' }}</el-button>
      </template>
    </el-dialog>

    <!-- 提交列表弹窗 -->
    <el-dialog v-model="submitDialogVisible" title="作业提交列表" width="850px">
      <el-table :data="submits" v-loading="subLoading" stripe border>
        <el-table-column prop="studentId" label="学生ID" width="80" />
        <el-table-column prop="studentName" label="学生姓名" width="100" />
        <el-table-column prop="content" label="提交内容" min-width="180" show-overflow-tooltip />
        <el-table-column prop="submitTime" label="提交时间" width="170" />
        <el-table-column prop="score" label="评分" width="80">
          <template #default="{ row }">{{ row.score != null ? row.score : '未评' }}</template>
        </el-table-column>
        <el-table-column prop="comment" label="评语" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '已批改' ? 'success' : 'warning'" size="small">{{ row.status || '待批改' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openGradeDialog(row)">评分</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!subLoading && submits.length === 0" description="暂无提交" :image-size="80" />
      <template #footer><el-button @click="submitDialogVisible = false">关闭</el-button></template>
    </el-dialog>

    <!-- 评分弹窗 -->
    <el-dialog v-model="gradeDialogVisible" title="作业评分" width="500px">
      <el-form label-width="90px">
        <el-form-item label="提交内容">
          <div style="background:#f5f7fa;padding:12px;border-radius:8px;max-height:200px;overflow-y:auto">{{
            currentSubmit?.content || '无内容' }}</div>
        </el-form-item>
        <el-form-item label="分数">
          <el-input-number v-model="gradeForm.score" :min="0" :max="100" style="width:150px" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="gradeForm.comment" type="textarea" :rows="3" placeholder="可选评语" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="gradeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="grading" @click="doGrade">确认评分</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { assignmentApi, courseApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const assignments = ref([]); const loading = ref(false)
const page = ref(1); const pageSize = ref(10); const total = ref(0)
const courseFilter = ref(null)
const courseList = ref([])

// 创建/编辑
const dialogVisible = ref(false); const isEdit = ref(false); const editId = ref(null); const submitting = ref(false)
const form = reactive({ title: '', courseId: null, content: '', deadline: '', totalScore: 100, attachmentUrl: '' })

// 提交列表
const submitDialogVisible = ref(false)
const submits = ref([]); const subLoading = ref(false)

// 评分
const gradeDialogVisible = ref(false); const grading = ref(false)
const currentSubmit = ref(null)
const gradeForm = reactive({ score: 0, comment: '' })

const fetchAssignments = async () => {
  loading.value = true
  try {
    const res = await assignmentApi.list({ page: page.value, size: pageSize.value, courseId: courseFilter.value || undefined })
    if (res.data.code === 200) { assignments.value = res.data.data.records; total.value = res.data.data.total }
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

const fetchCourses = async () => {
  try {
    const res = await courseApi.list({ page: 1, size: 100 })
    if (res.data.code === 200) courseList.value = res.data.data.records
  } catch (e) { /* ignore */ }
}

const openCreate = () => {
  isEdit.value = false; editId.value = null
  form.title = ''; form.courseId = null; form.content = ''; form.deadline = ''; form.totalScore = 100; form.attachmentUrl = ''
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true; editId.value = row.id
  form.title = row.title; form.courseId = row.courseId; form.content = row.content || ''
  form.deadline = row.deadline; form.totalScore = row.totalScore; form.attachmentUrl = row.attachmentUrl || ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.title || !form.courseId || !form.deadline) { ElMessage.warning('请填写必要信息'); return }
  submitting.value = true
  try {
    if (isEdit.value) {
      await assignmentApi.update(editId.value, form)
      ElMessage.success('作业更新成功')
    } else {
      await assignmentApi.create(form)
      ElMessage.success('作业布置成功')
    }
    dialogVisible.value = false
    fetchAssignments()
  } catch (e) { /* ignore */ }
  finally { submitting.value = false }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该作业？', '确认删除', { type: 'warning' })
    await assignmentApi.delete(id)
    ElMessage.success('删除成功')
    fetchAssignments()
  } catch (e) { /* ignore */ }
}

const viewSubmits = async (row) => {
  submitDialogVisible.value = true
  subLoading.value = true
  try {
    const res = await assignmentApi.getSubmitList(row.id)
    if (res.data.code === 200) submits.value = res.data.data.records || res.data.data || []
  } catch (e) { /* ignore */ }
  finally { subLoading.value = false }
}

const openGradeDialog = (row) => {
  currentSubmit.value = row
  gradeForm.score = row.score || 0
  gradeForm.comment = row.comment || ''
  gradeDialogVisible.value = true
}

const doGrade = async () => {
  if (!currentSubmit.value) return
  grading.value = true
  try {
    await assignmentApi.grade(currentSubmit.value.id, { score: gradeForm.score, comment: gradeForm.comment })
    ElMessage.success('评分成功')
    gradeDialogVisible.value = false
    // 刷新提交列表
    viewSubmits({ id: currentSubmit.value.assignmentId })
  } catch (e) { /* ignore */ }
  finally { grading.value = false }
}

onMounted(() => { fetchCourses(); fetchAssignments() })
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
</style>
