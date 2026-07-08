<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div>
        <h2 class="page-title">{{ course.courseName || '课程详情' }}</h2>
        <p class="page-subtitle">{{ course.teacherName || '当前教师' }} · {{ course.studentCount || 0 }} 人选课</p>
      </div>
      <el-button @click="router.back()">返回</el-button>
    </div>

    <div class="panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="课程信息" name="info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="课程名称">{{ course.courseName }}</el-descriptions-item>
            <el-descriptions-item label="授课教师">{{ course.teacherName }}</el-descriptions-item>
            <el-descriptions-item label="开始日期">{{ course.startDate || '--' }}</el-descriptions-item>
            <el-descriptions-item label="结束日期">{{ course.endDate || '--' }}</el-descriptions-item>
            <el-descriptions-item label="选课人数">{{ course.studentCount || 0 }} 人</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="course.status === '进行中' ? 'success' : 'info'">{{ course.status || '进行中' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="课程简介" :span="2">{{ course.description || '暂无课程简介' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="章节内容" name="chapters">
          <div class="tab-toolbar">
            <el-button type="primary" @click="openChapterDialog()">+ 添加章节</el-button>
            <el-button @click="router.push('/teacher/assignments')">去布置作业</el-button>
          </div>

          <el-collapse v-model="openChapterNames" class="chapter-collapse">
            <el-collapse-item v-for="chapter in chapters" :key="chapter.id" :name="String(chapter.id)">
              <template #title>
                <div class="chapter-title">
                  <span>{{ chapter.sortOrder }}. {{ chapter.chapterName }}</span>
                  <span class="chapter-stat">{{ resourcesByChapter(chapter.id).length }} 个资源 · {{ assignmentsByChapter(chapter.id).length }} 个作业</span>
                </div>
              </template>

              <div class="chapter-actions">
                <el-button size="small" @click="openChapterDialog(chapter)">编辑章节</el-button>
                <el-button size="small" type="danger" @click="deleteChapter(chapter)">删除章节</el-button>
                <el-upload action="#" :http-request="payload => uploadResource(payload, chapter.id)" :show-file-list="false">
                  <el-button size="small" type="primary" :loading="uploading">上传资源</el-button>
                </el-upload>
              </div>

              <div class="content-group">
                <h4 class="group-title">资源</h4>
                <div v-if="resourcesByChapter(chapter.id).length" class="item-list">
                  <div v-for="resource in resourcesByChapter(chapter.id)" :key="resource.id" class="content-item">
                    <span class="item-kind">{{ resource.resourceType || '课件' }}</span>
                    <span class="item-title">{{ resource.resourceName }}</span>
                    <span class="item-meta">{{ formatFileSize(resource.fileSize) }}</span>
                    <div class="item-actions">
                      <el-link v-if="resource.fileUrl" :href="resource.fileUrl" target="_blank" type="primary">查看</el-link>
                      <el-button size="small" type="danger" @click="deleteResource(resource)">删除</el-button>
                    </div>
                  </div>
                </div>
                <el-empty v-else description="本章节暂无资源" :image-size="70" />
              </div>

              <div class="content-group">
                <h4 class="group-title">作业</h4>
                <div v-if="assignmentsByChapter(chapter.id).length" class="item-list">
                  <div v-for="assignment in assignmentsByChapter(chapter.id)" :key="assignment.id" class="content-item">
                    <span class="item-kind assignment">作业</span>
                    <span class="item-title">{{ assignment.title }}</span>
                    <span class="item-meta">截止 {{ formatDate(assignment.deadline) }} · {{ assignment.submitCount || 0 }}/{{ assignment.totalStudents || 0 }}</span>
                    <el-button size="small" @click="router.push('/teacher/assignments')">管理</el-button>
                  </div>
                </div>
                <el-empty v-else description="本章节暂无作业" :image-size="70" />
              </div>
            </el-collapse-item>
          </el-collapse>

          <el-empty v-if="chapters.length === 0" description="暂无章节，请先添加章节" :image-size="100" />
        </el-tab-pane>

        <el-tab-pane label="选课学生" name="students">
          <el-table :data="students" border stripe>
            <el-table-column prop="studentNo" label="学号" width="130" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="major" label="专业" min-width="160" />
            <el-table-column prop="className" label="班级" width="140" />
            <el-table-column prop="grade" label="年级" width="100" />
          </el-table>
          <el-empty v-if="students.length === 0" description="暂无学生选课" :image-size="90" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="chapterDialogVisible" :title="editingChapterId ? '编辑章节' : '添加章节'" width="430px">
      <el-form :model="chapterForm" label-width="80px">
        <el-form-item label="章节名称" required>
          <el-input v-model="chapterForm.chapterName" placeholder="请输入章节名称" />
        </el-form-item>
        <el-form-item label="排序" required>
          <el-input-number v-model="chapterForm.sortOrder" :min="1" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="chapterForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="chapterDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingChapter" @click="saveChapter">
          {{ editingChapterId ? '保存修改' : '确定添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { assignmentApi, courseApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const activeTab = ref('chapters')
const course = ref({})
const chapters = ref([])
const students = ref([])
const resources = ref([])
const assignments = ref([])
const openChapterNames = ref([])
const chapterDialogVisible = ref(false)
const savingChapter = ref(false)
const uploading = ref(false)
const editingChapterId = ref(null)

const chapterForm = reactive({ chapterName: '', sortOrder: 1, description: '' })
const courseId = () => route.params.id

const fetchCourse = async () => {
  const res = await courseApi.detail(courseId())
  if (res.data.code === 200) {
    course.value = res.data.data || {}
    chapters.value = course.value.chapters || []
    openChapterNames.value = chapters.value.slice(0, 1).map(chapter => String(chapter.id))
  }
}

const fetchStudents = async () => {
  const res = await courseApi.getStudents(courseId(), { page: 1, size: 100 })
  if (res.data.code === 200) students.value = res.data.data?.records || []
}

const fetchResources = async () => {
  const res = await courseApi.getResources(courseId())
  if (res.data.code === 200) resources.value = res.data.data || []
}

const fetchAssignments = async () => {
  const res = await assignmentApi.list({ page: 1, size: 200, courseId: courseId() })
  if (res.data.code === 200) assignments.value = res.data.data?.records || []
}

const loadAll = async () => {
  try {
    await Promise.all([fetchCourse(), fetchStudents(), fetchResources(), fetchAssignments()])
  } catch (e) {
    ElMessage.error('课程详情加载失败')
  }
}

const resourcesByChapter = (chapterId) => resources.value.filter(resource => resource.chapterId === chapterId)
const assignmentsByChapter = (chapterId) => assignments.value.filter(assignment => assignment.chapterId === chapterId)

const openChapterDialog = (chapter) => {
  if (chapter) {
    editingChapterId.value = chapter.id
    Object.assign(chapterForm, {
      chapterName: chapter.chapterName || '',
      sortOrder: chapter.sortOrder || 1,
      description: chapter.description || ''
    })
  } else {
    editingChapterId.value = null
    Object.assign(chapterForm, {
      chapterName: '',
      sortOrder: (chapters.value.length || 0) + 1,
      description: ''
    })
  }
  chapterDialogVisible.value = true
}

const saveChapter = async () => {
  if (!chapterForm.chapterName.trim()) {
    ElMessage.warning('请输入章节名称')
    return
  }
  savingChapter.value = true
  try {
    if (editingChapterId.value) {
      await courseApi.updateChapter(courseId(), editingChapterId.value, chapterForm)
      ElMessage.success('章节已更新')
    } else {
      await courseApi.addChapter(courseId(), chapterForm)
      ElMessage.success('章节添加成功')
    }
    chapterDialogVisible.value = false
    await fetchCourse()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存章节失败')
  } finally {
    savingChapter.value = false
  }
}

const deleteChapter = async (chapter) => {
  try {
    await ElMessageBox.confirm(`确定删除章节“${chapter.chapterName}”吗？`, '删除章节', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await courseApi.deleteChapter(courseId(), chapter.id)
    ElMessage.success('章节已删除')
    await loadAll()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '删除章节失败')
  }
}

const uploadResource = async ({ file }, chapterId) => {
  const raw = file instanceof File ? file : (file?.raw || file?.file || file)
  const formData = new FormData()
  formData.append('file', raw)
  formData.append('chapterId', chapterId)
  uploading.value = true
  try {
    await courseApi.uploadResource(courseId(), formData)
    ElMessage.success('资源上传成功')
    await Promise.all([fetchResources(), fetchCourse()])
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '资源上传失败')
  } finally {
    uploading.value = false
  }
}

const deleteResource = async (resource) => {
  try {
    await ElMessageBox.confirm(`确定删除资源"${resource.resourceName}"吗？`, '删除资源', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    await courseApi.deleteResource(courseId(), resource.id)
    ElMessage.success('资源已删除')
    await Promise.all([fetchResources(), fetchCourse()])
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '删除资源失败')
  }
}

const formatDate = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : '--'
const formatFileSize = (size) => {
  const n = Number(size || 0)
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(1)} MB`
}

onMounted(loadAll)
</script>

<style scoped>
.page-shell { display: flex; flex-direction: column; gap: 20px; }
.page-title-bar { display: flex; align-items: center; justify-content: space-between; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.page-subtitle { color: #909399; font-size: 13px; margin: 6px 0 0; }
.panel { background: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,.05); padding: 24px; }
.tab-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.chapter-collapse :deep(.el-collapse-item__header) { height: 58px; font-size: 16px; }
.chapter-title { width: 100%; display: flex; justify-content: space-between; align-items: center; padding-right: 20px; }
.chapter-stat { color: #909399; font-size: 13px; font-weight: 400; }
.chapter-actions { display: flex; align-items: center; gap: 10px; margin-bottom: 18px; }
.content-group { border-top: 1px solid #ebeef5; padding-top: 14px; margin-top: 14px; }
.group-title { margin: 0 0 10px; color: #303133; font-size: 14px; }
.item-list { display: flex; flex-direction: column; gap: 10px; }
.content-item { display: grid; grid-template-columns: 56px minmax(160px, 1fr) minmax(180px, auto) auto; gap: 12px; align-items: center; padding: 12px 14px; background: #fafafa; border: 1px solid #ebeef5; border-radius: 8px; }
.item-kind { color: #409eff; background: #ecf5ff; border-radius: 4px; padding: 3px 8px; text-align: center; font-size: 12px; }
.item-kind.assignment { color: #e6a23c; background: #fdf6ec; }
.item-title { color: #303133; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-meta { color: #909399; font-size: 13px; }
.item-actions { display: flex; align-items: center; gap: 8px; }
</style>
