<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div>
        <h2 class="page-title">{{ course.courseName || '课程详情' }}</h2>
        <p class="page-subtitle">{{ course.teacherName || '当前教师' }} · {{ course.studentCount || 0 }} 人选课</p>
      </div>
      <div class="title-actions">
        <el-button type="primary" @click="openCourseEditDialog">编辑课程</el-button>
        <el-button @click="router.back()">返回</el-button>
      </div>
    </div>

    <div class="panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="课程信息" name="info">
          <div v-if="course.coverImg" class="cover-display">
            <img :src="resolveFileUrl(course.coverImg)" :alt="course.courseName + '封面'" />
          </div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="课程名称">{{ course.courseName }}</el-descriptions-item>
            <el-descriptions-item label="授课教师">{{ course.teacherName }}</el-descriptions-item>
            <el-descriptions-item label="开始日期">{{ course.startDate || '--' }}</el-descriptions-item>
            <el-descriptions-item label="结束日期">{{ course.endDate || '--' }}</el-descriptions-item>
            <el-descriptions-item label="选课人数">{{ course.studentCount || 0 }} 人</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTagType(computeStatus(course.startDate, course.endDate))">{{
                computeStatus(course.startDate, course.endDate) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="课程简介" :span="2">{{ course.description || '暂无课程简介' }}</el-descriptions-item>
          </el-descriptions>
          <div class="info-actions">
            <el-button type="primary" @click="openCourseEditDialog">编辑课程信息</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="课程统计" name="stats">
          <div v-loading="statsLoading" class="stats-container">
            <template v-if="courseStats">
              <div class="stats-grid">
                <div class="stat-card">
                  <div class="stat-value">{{ courseStats.avg ?? '-' }}</div>
                  <div class="stat-label">平均分</div>
                </div>
                <div class="stat-card highlight">
                  <div class="stat-value">{{ courseStats.max ?? '-' }}</div>
                  <div class="stat-label">最高分</div>
                </div>
                <div class="stat-card warn">
                  <div class="stat-value">{{ courseStats.min ?? '-' }}</div>
                  <div class="stat-label">最低分</div>
                </div>
                <div class="stat-card success">
                  <div class="stat-value">{{ courseStats.passRate ?? 0 }}%</div>
                  <div class="stat-label">及格率</div>
                </div>
              </div>

              <el-divider />

              <h4 class="section-title">成绩分布</h4>
              <div class="dist-chart" v-if="courseStats.distribution">
                <div v-for="(count, label) in courseStats.distribution" :key="label" class="dist-bar-row">
                  <span class="dist-label">{{ label }}</span>
                  <div class="dist-track">
                    <div class="dist-fill" :style="{ width: distPercent(count) }" :class="distBarClass(label)"></div>
                  </div>
                  <span class="dist-count">{{ count }}人</span>
                </div>
              </div>
              <el-empty v-else description="暂无成绩数据" :image-size="80" />

              <el-divider />

              <div class="stats-footer">
                <span>课程总人数：{{ courseStats.total || 0 }} 人</span>
                <span>已出成绩：{{ courseStats.scoredTotal || 0 }} 人</span>
                <span>及格人数：{{ courseStats.passed || 0 }} 人</span>
              </div>
            </template>
            <el-empty v-else-if="!statsLoading" description="暂无统计数据" :image-size="100" />
          </div>
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
                  <span class="chapter-stat">{{ resourcesByChapter(chapter.id).length }} 个资源 · {{
                    assignmentsByChapter(chapter.id).length }} 个作业</span>
                </div>
              </template>

              <div class="chapter-actions">
                <el-button size="small" @click="openChapterDialog(chapter)">编辑章节</el-button>
                <el-button size="small" type="danger" @click="deleteChapter(chapter)">删除章节</el-button>
                <el-upload action="#" :http-request="payload => uploadResource(payload, chapter.id)"
                  :show-file-list="false" :disabled="uploading">
                  <el-button size="small" type="primary" :loading="uploading" :disabled="uploading">上传资源</el-button>
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
                      <el-button v-if="resource.fileUrl" size="small" type="primary" link
                        @click="previewFile(resource)">预览</el-button>
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
                    <span class="item-meta">截止 {{ formatDate(assignment.deadline) }} · {{ assignment.submitCount || 0
                    }}/{{
                        assignment.totalStudents || 0 }}</span>
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
          <div class="tab-toolbar">
            <span style="color:#909399;font-size:14px;">共 {{ students.length }} 名学生</span>
          </div>
          <el-table v-if="students.length > 0" :data="students" border stripe>
            <el-table-column prop="studentNo" label="学号" width="130" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="major" label="专业" min-width="160" />
            <el-table-column prop="className" label="班级" width="140" />
            <el-table-column prop="grade" label="年级" width="100" />
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button size="small" type="danger" @click="kickStudent(row)">踢出</el-button>
              </template>
            </el-table-column>
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

    <el-dialog v-model="previewVisible" title="资源预览" width="80%" top="5vh" destroy-on-close>
      <FilePreview v-if="previewResource" :url="previewResource.fileUrl" :name="previewResource.resourceName"
        :type="previewResource.resourceType" />
    </el-dialog>

    <CourseEditDialog v-model="courseDialogVisible" :course="course" @saved="fetchCourse" />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { assignmentApi, courseApi, gradeApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import FilePreview from '@/components/FilePreview.vue'
import CourseEditDialog from '@/components/CourseEditDialog.vue'
import { computeStatus, statusTagType } from '@/utils/status'
import { resolveFileUrl } from '@/utils/fileUrl'

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
const courseDialogVisible = ref(false)
const savingChapter = ref(false)
const uploading = ref(false)
const editingChapterId = ref(null)
const previewVisible = ref(false)
const previewResource = ref(null)

// 统计相关
const statsLoading = ref(false)
const courseStats = ref(null)

const chapterForm = reactive({ chapterName: '', sortOrder: 1, description: '' })
const courseId = () => route.params.id

const openCourseEditDialog = () => {
  courseDialogVisible.value = true
}

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

const kickStudent = async (student) => {
  try {
    await ElMessageBox.confirm(
      `确定将学生「${student.realName || student.studentNo}」踢出课程吗？`,
      '踢出学生',
      { type: 'warning', confirmButtonText: '确定踢出', cancelButtonText: '取消' }
    )
    await courseApi.removeStudent(courseId(), student.userId)
    ElMessage.success('已将该学生踢出课程')
    students.value = students.value.filter(s => s.userId !== student.userId)
    course.value.studentCount = Math.max(0, Number(course.value.studentCount || 0) - 1)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const fetchStats = async () => {
  statsLoading.value = true
  try {
    const res = await gradeApi.courseStats(courseId())
    if (res.data.code === 200) {
      courseStats.value = res.data.data
    }
  } catch (e) { /* ignore */ }
  finally { statsLoading.value = false }
}

const distPercent = (count) => {
  if (!courseStats.value?.scoredTotal) return '0%'
  return ((count / courseStats.value.scoredTotal) * 100).toFixed(0) + '%'
}

const distBarClass = (label) => {
  if (label.includes('优秀')) return 'bar-excellent'
  if (label.includes('良好')) return 'bar-good'
  if (label.includes('中等')) return 'bar-medium'
  if (label.includes('及格')) return 'bar-pass'
  return 'bar-fail'
}

const loadAll = async () => {
  try {
    await Promise.all([fetchCourse(), fetchStudents(), fetchResources(), fetchAssignments(), fetchStats()])
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

const previewFile = (resource) => {
  previewResource.value = resource
  previewVisible.value = true
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
.page-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--bio-ink);
  margin: 0;
}

.page-subtitle {
  color: var(--bio-ink-muted);
  font-size: 15px;
  margin: 6px 0 0;
}

.panel {
  background: #fff;
  border: 1px solid var(--bio-border);
  border-radius: 16px;
  box-shadow: none;
  padding: 24px;
}

.panel :deep(.el-tabs__active-bar) {
  background: var(--bio-primary);
}

.panel :deep(.el-tabs__item.is-active),
.panel :deep(.el-tabs__item:hover) {
  color: var(--bio-primary);
}

.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.chapter-collapse :deep(.el-collapse-item__header) {
  height: 58px;
  font-size: 16px;
}

.chapter-title {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-right: 20px;
}

.chapter-stat {
  color: var(--bio-ink-muted);
  font-size: 14px;
  font-weight: 400;
}

.chapter-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}

.content-group {
  border-top: 1px solid var(--bio-border);
  padding-top: 14px;
  margin-top: 14px;
}

.group-title {
  margin: 0 0 10px;
  color: var(--bio-ink);
  font-size: 16px;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.content-item {
  display: grid;
  grid-template-columns: 56px minmax(160px, 1fr) minmax(180px, auto) auto;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  background: #fff;
  border: 1px solid var(--bio-border);
  border-radius: 6px;
}

.item-kind {
  color: var(--bio-primary);
  background: var(--bio-primary-soft);
  border-radius: 4px;
  padding: 3px 8px;
  text-align: center;
  font-size: 13px;
}

.item-kind.assignment {
  color: var(--bio-primary);
  background: var(--bio-primary-soft);
}

.item-title {
  color: var(--bio-ink);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  color: var(--bio-ink-muted);
  font-size: 14px;
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ---------- 标题操作按钮 ---------- */
.title-actions {
  display: flex;
  gap: 8px;
}

.info-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.cover-display {
  margin-bottom: 20px;
  border-radius: 10px;
  overflow: hidden;
  max-height: 260px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.cover-display img {
  width: 100%;
  height: auto;
  max-height: 260px;
  object-fit: cover;
  display: block;
}

/* ---------- 课程统计 ---------- */
.stats-container {
  min-height: 200px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: #fff;
  border: 1px solid var(--bio-border);
  border-radius: 6px;
  padding: 20px;
  text-align: center;
  transition: all 0.25s;
}

.stat-card:hover {
  transform: none;
  box-shadow: none;
  border-color: var(--bio-primary);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--bio-ink);
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: var(--bio-ink-muted);
}

.stat-card.highlight .stat-value {
  color: var(--bio-primary);
}

.stat-card.warn .stat-value {
  color: #e6a23c;
}

.stat-card.success .stat-value {
  color: var(--bio-primary);
}

.section-title {
  margin: 0 0 16px;
  font-size: 15px;
  color: var(--bio-ink);
}

.dist-chart {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dist-bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dist-label {
  width: 100px;
  font-size: 13px;
  color: var(--bio-ink-soft);
  text-align: right;
  flex-shrink: 0;
}

.dist-track {
  flex: 1;
  height: 22px;
  background: #e9eef3;
  border-radius: 11px;
  overflow: hidden;
}

.dist-fill {
  height: 100%;
  border-radius: 11px;
  transition: width 0.6s ease;
  min-width: 4px;
}

.dist-fill.bar-excellent {
  background: var(--bio-primary);
}

.dist-fill.bar-good {
  background: var(--bio-primary);
}

.dist-fill.bar-medium {
  background: linear-gradient(90deg, #e6a23c, #f4d19b);
}

.dist-fill.bar-pass {
  background: linear-gradient(90deg, #909399, #b0b3b8);
}

.dist-fill.bar-fail {
  background: linear-gradient(90deg, #f56c6c, #f89898);
}

.dist-count {
  width: 44px;
  font-size: 13px;
  color: var(--bio-ink-soft);
  text-align: left;
  flex-shrink: 0;
}

.stats-footer {
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: var(--bio-ink-muted);
}
</style>
