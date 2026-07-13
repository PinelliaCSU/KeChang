<template>
  <div class="learn-page">
    <div class="back-row">
      <el-button link @click="router.back()">
        <span class="back-arrow">←</span> 返回
      </el-button>
    </div>

    <div class="course-header">
      <div class="course-info">
        <h2 class="course-title">{{ course.courseName || '课程学习' }}</h2>
        <p class="course-meta">{{ course.teacherName || '授课教师' }} · {{ course.description || '暂无课程简介' }}</p>
      </div>
      <el-button type="primary" @click="router.push('/student/assignments')">查看我的作业</el-button>
    </div>

    <div class="learn-tabs">
      <span :class="{ active: activeTab === 'chapter' }" @click="activeTab = 'chapter'">章节</span>
      <span :class="{ active: activeTab === 'announcement' }" @click="activeTab = 'announcement'">公告</span>
      <span :class="{ active: activeTab === 'resource' }" @click="activeTab = 'resource'">课件</span>
      <span :class="{ active: activeTab === 'assignment' }" @click="activeTab = 'assignment'">作业</span>
      <span :class="{ active: activeTab === 'note' }" @click="activeTab = 'note'">笔记</span>
    </div>

    <!-- 章节 -->
    <div class="chapter-panel" v-if="activeTab === 'chapter'">
      <el-collapse v-model="openChapterNames">
        <el-collapse-item v-for="chapter in chapters" :key="chapter.id" :name="String(chapter.id)">
          <template #title>
            <div class="chapter-title">
              <span>{{ chapter.sortOrder }} {{ chapter.chapterName }}</span>
              <span class="chapter-stat">{{ resourcesByChapter(chapter.id).length }} 个资源 · {{
                assignmentsByChapter(chapter.id).length }} 个作业</span>
            </div>
          </template>

          <div class="chapter-body">
            <div v-for="resource in resourcesByChapter(chapter.id)" :key="`r-${resource.id}`" class="learn-item">
              <span class="item-icon">📎</span>
              <span class="item-name">{{ resource.resourceName }}</span>
              <el-button size="small" type="primary" link @click="previewFile(resource)">预览</el-button>
            </div>

            <div v-for="assignment in assignmentsByChapter(chapter.id)" :key="`a-${assignment.id}`" class="learn-item">
              <span class="item-icon assignment">作</span>
              <span class="item-name">{{ assignment.title }}</span>
              <span class="item-meta">截止 {{ formatDate(assignment.deadline) }}</span>
              <el-button size="small" @click="router.push('/student/assignments')">
                {{ assignment.submitted ? '查看提交' : '去完成' }}
              </el-button>
            </div>

            <el-empty
              v-if="resourcesByChapter(chapter.id).length === 0 && assignmentsByChapter(chapter.id).length === 0"
              description="本章节暂无内容" :image-size="70" />
          </div>
        </el-collapse-item>
      </el-collapse>
      <el-empty v-if="chapters.length === 0" description="暂无章节内容" :image-size="110" />
    </div>

    <!-- 公告 -->
    <div class="announcement-panel" v-if="activeTab === 'announcement'">
      <div v-if="announcements.length > 0" class="announcement-list">
        <div class="announcement-card" v-for="a in announcements" :key="a.id">
          <div class="announcement-card-header">
            <h3>{{ a.title }}</h3>
            <el-tag :type="a.confirmed ? 'success' : 'warning'" size="small">
              {{ a.confirmed ? '已确认' : '待确认' }}
            </el-tag>
          </div>
          <p class="announcement-desc">{{ truncateContent(a.content) }}</p>
          <div class="announcement-meta">
            <span>{{ formatTime(a.publishTime) }}</span>
            <span>{{ a.publisherName || '' }}</span>
            <span>{{ a.confirmCount || 0 }} 人已确认</span>
          </div>
          <div class="announcement-actions">
            <el-button v-if="!a.confirmed" type="primary" size="small" @click="handleConfirm(a)">
              确认公告
            </el-button>
            <el-button v-else type="success" size="small" disabled>已确认</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无课程公告" :image-size="100" />
      <PaginationWrapper v-bind="annPaginationProps" @update:page="handleAnnPageChange"
        @update:size="handleAnnSizeChange" @change="fetchAnnouncements" />
    </div>

    <!-- 课件 -->
    <div class="resource-panel" v-if="activeTab === 'resource'">
      <div v-if="resources.length > 0">
        <div v-for="resource in resources" :key="`res-${resource.id}`" class="learn-item">
          <span class="item-icon">📎</span>
          <span class="item-name">{{ resource.resourceName }}</span>
          <span class="item-meta">{{ resource.resourceType }}</span>
          <el-button size="small" type="primary" link @click="previewFile(resource)">预览</el-button>
        </div>
      </div>
      <el-empty v-else description="暂无课件资源" :image-size="100" />
    </div>

    <!-- 作业 -->
    <div class="assignment-panel" v-if="activeTab === 'assignment'">
      <div v-if="assignments.length > 0">
        <div v-for="assignment in assignments" :key="`as-${assignment.id}`" class="learn-item">
          <span class="item-icon assignment">作</span>
          <span class="item-name">{{ assignment.title }}</span>
          <span class="item-meta">截止 {{ formatDate(assignment.deadline) }}</span>
          <el-button size="small" @click="router.push('/student/assignments')">
            {{ assignment.submitted ? '查看提交' : '去完成' }}
          </el-button>
        </div>
      </div>
      <el-empty v-else description="暂无作业" :image-size="100" />
      <PaginationWrapper v-bind="assignPaginationProps" @update:page="handleAssignPageChange"
        @update:size="handleAssignSizeChange" @change="fetchAssignments" />
    </div>

    <!-- 笔记 -->
    <div class="note-panel" v-if="activeTab === 'note'">
      <h4>学习笔记</h4>
      <RichTextEditor ref="noteEditorRef" :modelValue="noteContent" placeholder="在此输入本课程学习笔记..." />
      <div class="note-actions">
        <el-button type="primary" @click="saveNote">保存笔记</el-button>
      </div>
    </div>

    <el-dialog v-model="previewVisible" title="资源预览" width="80%" top="5vh" destroy-on-close>
      <FilePreview v-if="previewResource" :url="previewResource.fileUrl" :name="previewResource.resourceName"
        :type="previewResource.resourceType" :resource-id="previewResource.id" :course-id="courseId()" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { assignmentApi, courseApi, knowledgeApi, announcementApi } from '@/api'
import { ElMessage } from 'element-plus'
import FilePreview from '@/components/FilePreview.vue'
import { usePagination } from '@/composables/usePagination'
import PaginationWrapper from '@/components/PaginationWrapper.vue'

const route = useRoute()
const router = useRouter()
const course = ref({})
const chapters = ref([])
const resources = ref([])
const assignments = ref([])
const announcements = ref([])
const openChapterNames = ref([])
const noteContent = ref('')
const noteEditorRef = ref(null)
const previewVisible = ref(false)
const previewResource = ref(null)
const activeTab = ref('chapter')

const courseId = () => route.params.id

// 分页状态
const { page: assignPage, size: assignSize, total: assignTotal, resetPage: resetAssignPage, paginationProps: assignPaginationProps, handleSizeChange: handleAssignSizeChange, handleCurrentChange: handleAssignPageChange } = usePagination({ defaultSize: 10 })
const { page: annPage, size: annSize, total: annTotal, resetPage: resetAnnPage, paginationProps: annPaginationProps, handleSizeChange: handleAnnSizeChange, handleCurrentChange: handleAnnPageChange } = usePagination({ defaultSize: 10 })

const fetchCourseData = async () => {
  try {
    const [courseRes, resourceRes] = await Promise.all([
      courseApi.detail(courseId()),
      courseApi.getResources(courseId())
    ])
    if (courseRes.data.code === 200) {
      course.value = courseRes.data.data || {}
      chapters.value = course.value.chapters || []
      openChapterNames.value = chapters.value.slice(0, 1).map(chapter => String(chapter.id))
    }
    if (resourceRes.data.code === 200) resources.value = resourceRes.data.data || []
  } catch (e) {
    ElMessage.error('课程内容加载失败')
  }
}

const fetchAssignments = async () => {
  try {
    const res = await assignmentApi.list({ page: assignPage.value, size: assignSize.value, courseId: courseId() })
    if (res.data.code === 200) {
      assignments.value = res.data.data?.records || []
      assignTotal.value = res.data.data?.total || 0
    }
  } catch (e) { /* ignore */ }
}

const fetchAnnouncements = async () => {
  try {
    const res = await announcementApi.list({ page: annPage.value, size: annSize.value, courseId: courseId() })
    if (res.data.code === 200) {
      announcements.value = res.data.data?.records || []
      annTotal.value = res.data.data?.total || 0
    }
  } catch (e) { /* ignore */ }
}

// 使用 computed 按章节分组为 Map，O(1) 查找替代每次渲染时 O(n) filter
const resourcesByChapterMap = computed(() => {
  const map = new Map()
  resources.value.forEach(r => {
    const arr = map.get(r.chapterId) || []
    arr.push(r)
    map.set(r.chapterId, arr)
  })
  return map
})
const assignmentsByChapterMap = computed(() => {
  const map = new Map()
  assignments.value.forEach(a => {
    const arr = map.get(a.chapterId) || []
    arr.push(a)
    map.set(a.chapterId, arr)
  })
  return map
})
const resourcesByChapter = (chapterId) => resourcesByChapterMap.value.get(chapterId) || []
const assignmentsByChapter = (chapterId) => assignmentsByChapterMap.value.get(chapterId) || []
const formatDate = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : '--'

const formatTime = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const truncateContent = (content) => {
  if (!content) return '暂无内容'
  const text = content.replace(/<[^>]*>/g, '')
  return text.length > 200 ? text.substring(0, 200) + '...' : text
}

const previewFile = async (resource) => {
  previewResource.value = resource
  previewVisible.value = true
  try {
    await courseApi.markResourceViewed(courseId(), resource.id)
  } catch { /* ignore */ }
}

const saveNote = async () => {
  const content = noteEditorRef.value?.getHTML() || ''
  if (!content.trim() && !content.includes('<img')) {
    ElMessage.warning('请输入笔记内容')
    return
  }
  try {
    await knowledgeApi.createNote({
      title: `${course.value.courseName || '课程'}学习笔记`,
      content,
      courseId: courseId()
    })
    ElMessage.success('笔记已保存')
    noteContent.value = ''
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const handleConfirm = async (announcement) => {
  try {
    await announcementApi.confirm(announcement.id)
    ElMessage.success('公告确认成功')
    announcement.confirmed = true
    announcement.confirmCount = (announcement.confirmCount || 0) + 1
  } catch (e) { /* ignore */ }
}

onMounted(async () => {
  await fetchCourseData()
  await Promise.all([fetchAssignments(), fetchAnnouncements()])
})
</script>

<style scoped>
.learn-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.back-row {
  margin-bottom: -14px;
}

.back-arrow {
  font-size: 16px;
  margin-right: 2px;
}

.course-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.course-title {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
}

.course-meta {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.learn-tabs {
  display: flex;
  gap: 44px;
  border-bottom: 1px solid #e4e7ed;
  color: #606266;
  font-size: 18px;
}

.learn-tabs span {
  padding: 0 0 12px;
  cursor: pointer;
  transition: color 0.2s;
}

.learn-tabs span:hover {
  color: var(--bio-primary);
}

.learn-tabs .active {
  color: var(--bio-primary);
  border-bottom: 3px solid var(--bio-primary);
}

.chapter-panel,
.note-panel,
.announcement-panel,
.resource-panel,
.assignment-panel {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 24px;
}

.chapter-title {
  width: 100%;
  display: flex;
  justify-content: space-between;
  padding-right: 20px;
  font-size: 18px;
}

.chapter-stat {
  color: #909399;
  font-size: 13px;
  font-weight: 400;
}

.chapter-body {
  padding: 12px 24px 20px;
}

.learn-item {
  display: grid;
  grid-template-columns: 36px minmax(180px, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  color: #606266;
}

.item-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #ffe1a3;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-icon.assignment {
  background: var(--bio-primary-soft);
  color: var(--bio-primary);
  font-size: 13px;
  font-weight: 700;
}

.item-name {
  color: #303133;
  font-size: 15px;
}

.item-meta {
  color: #909399;
  font-size: 13px;
}

.note-panel h4 {
  margin: 0 0 12px;
  color: #303133;
}

.note-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* 公告样式 */
.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.announcement-card {
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  transition: box-shadow 0.2s;
}

.announcement-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.announcement-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.announcement-card-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.announcement-desc {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
  line-height: 1.6;
}

.announcement-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
}

.announcement-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
</style>
