<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">作业管理</h2>
      <el-button type="primary" @click="openCreate">+ 布置作业</el-button>
    </div>

    <div class="panel">
      <div class="search-bar">
        <el-select v-model="courseFilter" placeholder="按课程筛选" clearable style="width:220px" @change="onFilterCourseChange">
          <el-option v-for="course in courseList" :key="course.id" :label="course.courseName" :value="course.id" />
        </el-select>
        <el-select v-model="chapterFilter" placeholder="按章节筛选" clearable style="width:220px" @change="fetchAssignments">
          <el-option v-for="chapter in filterChapters" :key="chapter.id" :label="chapter.chapterName" :value="chapter.id" />
        </el-select>
      </div>

      <el-table :data="assignments" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="title" label="作业标题" min-width="180" />
        <el-table-column prop="courseName" label="所属课程" min-width="140" />
        <el-table-column label="所属章节" min-width="140">
          <template #default="{ row }">{{ chapterName(row.chapterId) }}</template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止时间" width="170" />
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column label="提交/总人数" width="120">
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
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑作业' : '布置作业'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="作业标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="所属课程" required>
          <el-select v-model="form.courseId" placeholder="选择课程" style="width:100%" @change="loadFormChapters">
            <el-option v-for="course in courseList" :key="course.id" :label="course.courseName" :value="course.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属章节">
          <el-select v-model="form.chapterId" placeholder="选择章节" clearable style="width:100%">
            <el-option v-for="chapter in formChapters" :key="chapter.id" :label="chapter.chapterName" :value="chapter.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作业内容">
          <el-input v-model="form.content" type="textarea" :rows="4" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="截止时间" required>
              <el-date-picker v-model="form.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总分">
              <el-input-number v-model="form.totalScore" :min="1" :max="100" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="附件链接">
          <el-input v-model="form.attachmentUrl" placeholder="可选，附件 URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ isEdit ? '保存修改' : '布置作业' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="submitDialogVisible" title="作业提交列表" width="850px">
      <el-table :data="submits" v-loading="subLoading" stripe border>
        <el-table-column prop="studentName" label="学生姓名" width="120" />
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="content" label="提交内容" min-width="180" show-overflow-tooltip />
        <el-table-column prop="submitTime" label="提交时间" width="170" />
        <el-table-column label="评分" width="80">
          <template #default="{ row }">{{ row.score != null ? row.score : '未评' }}</template>
        </el-table-column>
        <el-table-column prop="comment" label="评语" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openGradeDialog(row)">评分</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!subLoading && submits.length === 0" description="暂无提交" :image-size="80" />
    </el-dialog>

    <el-dialog v-model="gradeDialogVisible" title="作业评分" width="500px">
      <el-form label-width="90px">
        <el-form-item label="提交内容">
          <div class="submit-content">{{ currentSubmit?.content || '无内容' }}</div>
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

const assignments = ref([])
const loading = ref(false)
const courseFilter = ref(null)
const chapterFilter = ref(null)
const courseList = ref([])
const filterChapters = ref([])
const formChapters = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)
const submitDialogVisible = ref(false)
const submits = ref([])
const subLoading = ref(false)
const gradeDialogVisible = ref(false)
const grading = ref(false)
const currentSubmit = ref(null)

const form = reactive({
  title: '',
  courseId: null,
  chapterId: null,
  content: '',
  deadline: '',
  totalScore: 100,
  attachmentUrl: ''
})

const gradeForm = reactive({ score: 0, comment: '' })

const fetchAssignments = async () => {
  loading.value = true
  try {
    const res = await assignmentApi.list({
      page: 1,
      size: 100,
      courseId: courseFilter.value || undefined,
      chapterId: chapterFilter.value || undefined
    })
    if (res.data.code === 200) assignments.value = res.data.data?.records || []
  } catch (e) {
    ElMessage.error('作业列表加载失败')
  } finally {
    loading.value = false
  }
}

const fetchCourses = async () => {
  const res = await courseApi.list({ page: 1, size: 100 })
  if (res.data.code === 200) courseList.value = res.data.data?.records || []
}

const loadChaptersByCourse = async (courseId) => {
  if (!courseId) return []
  const res = await courseApi.detail(courseId)
  return res.data.code === 200 ? (res.data.data?.chapters || []) : []
}

const onFilterCourseChange = async () => {
  chapterFilter.value = null
  filterChapters.value = await loadChaptersByCourse(courseFilter.value)
  fetchAssignments()
}

const loadFormChapters = async () => {
  form.chapterId = null
  formChapters.value = await loadChaptersByCourse(form.courseId)
}

const chapterName = (chapterId) => {
  const all = [...filterChapters.value, ...formChapters.value]
  return all.find(chapter => chapter.id === chapterId)?.chapterName || (chapterId ? '未知章节' : '未分配')
}

const resetForm = () => {
  Object.assign(form, { title: '', courseId: null, chapterId: null, content: '', deadline: '', totalScore: 100, attachmentUrl: '' })
  formChapters.value = []
}

const openCreate = () => {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEdit = async (row) => {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    title: row.title,
    courseId: row.courseId,
    chapterId: null,
    content: row.content || '',
    deadline: row.deadline,
    totalScore: row.totalScore || 100,
    attachmentUrl: row.attachmentUrl || ''
  })
  formChapters.value = await loadChaptersByCourse(row.courseId)
  form.chapterId = row.chapterId || null
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.title || !form.courseId || !form.deadline) {
    ElMessage.warning('请填写必要信息')
    return
  }
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
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存作业失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该作业？', '确认删除', { type: 'warning' })
    await assignmentApi.delete(id)
    ElMessage.success('删除成功')
    fetchAssignments()
  } catch (e) { /* cancel */ }
}

const viewSubmits = async (row) => {
  submitDialogVisible.value = true
  subLoading.value = true
  try {
    const res = await assignmentApi.getSubmitList(row.id)
    if (res.data.code === 200) submits.value = res.data.data?.records || []
  } catch (e) {
    ElMessage.error('提交列表加载失败')
  } finally {
    subLoading.value = false
  }
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
    viewSubmits({ id: currentSubmit.value.assignmentId })
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '评分失败')
  } finally {
    grading.value = false
  }
}

onMounted(() => { fetchCourses(); fetchAssignments() })
</script>

<style scoped>
.page-shell { display: flex; flex-direction: column; gap: 20px; }
.page-title-bar { display: flex; align-items: center; justify-content: space-between; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.panel { background: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,.05); padding: 24px; }
.search-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.submit-content { background: #f5f7fa; padding: 12px; border-radius: 8px; max-height: 200px; overflow-y: auto; }
</style>
