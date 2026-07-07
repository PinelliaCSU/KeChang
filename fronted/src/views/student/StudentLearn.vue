<template>
  <div class="learn-page">
    <div class="course-header">
      <div>
        <el-button link @click="router.back()">返回</el-button>
        <h2 class="course-title">{{ course.courseName || '课程学习' }}</h2>
        <p class="course-meta">{{ course.teacherName || '授课教师' }} · {{ course.description || '暂无课程简介' }}</p>
      </div>
      <el-button type="primary" @click="router.push('/student/assignments')">查看我的作业</el-button>
    </div>

    <div class="learn-tabs">
      <span class="active">章节</span>
      <span>课件</span>
      <span>作业</span>
      <span>笔记</span>
    </div>

    <div class="chapter-panel">
      <el-collapse v-model="openChapterNames">
        <el-collapse-item v-for="chapter in chapters" :key="chapter.id" :name="String(chapter.id)">
          <template #title>
            <div class="chapter-title">
              <span>{{ chapter.sortOrder }} {{ chapter.chapterName }}</span>
              <span class="chapter-stat">{{ resourcesByChapter(chapter.id).length }} 个资源 · {{ assignmentsByChapter(chapter.id).length }} 个作业</span>
            </div>
          </template>

          <div class="chapter-body">
            <div v-for="resource in resourcesByChapter(chapter.id)" :key="`r-${resource.id}`" class="learn-item">
              <span class="item-icon">📎</span>
              <span class="item-name">{{ resource.resourceName }}</span>
              <el-link :href="resource.fileUrl" target="_blank" type="primary">查看文件</el-link>
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
              description="本章节暂无内容"
              :image-size="70"
            />
          </div>
        </el-collapse-item>
      </el-collapse>
      <el-empty v-if="chapters.length === 0" description="暂无章节内容" :image-size="110" />
    </div>

    <div class="note-panel">
      <h4>学习笔记</h4>
      <el-input v-model="noteContent" type="textarea" :rows="3" placeholder="在此输入本课程学习笔记..." />
      <div class="note-actions">
        <el-button type="primary" @click="saveNote">保存笔记</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { assignmentApi, courseApi, knowledgeApi } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const course = ref({})
const chapters = ref([])
const resources = ref([])
const assignments = ref([])
const openChapterNames = ref([])
const noteContent = ref('')

const courseId = () => route.params.id

const fetchCourseData = async () => {
  try {
    const [courseRes, resourceRes, assignmentRes] = await Promise.all([
      courseApi.detail(courseId()),
      courseApi.getResources(courseId()),
      assignmentApi.list({ page: 1, size: 200, courseId: courseId() })
    ])
    if (courseRes.data.code === 200) {
      course.value = courseRes.data.data || {}
      chapters.value = course.value.chapters || []
      openChapterNames.value = chapters.value.slice(0, 1).map(chapter => String(chapter.id))
    }
    if (resourceRes.data.code === 200) resources.value = resourceRes.data.data || []
    if (assignmentRes.data.code === 200) assignments.value = assignmentRes.data.data?.records || []
  } catch (e) {
    ElMessage.error('课程内容加载失败')
  }
}

const resourcesByChapter = (chapterId) => resources.value.filter(resource => resource.chapterId === chapterId)
const assignmentsByChapter = (chapterId) => assignments.value.filter(assignment => assignment.chapterId === chapterId)
const formatDate = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : '--'

const saveNote = async () => {
  if (!noteContent.value.trim()) {
    ElMessage.warning('请输入笔记内容')
    return
  }
  try {
    await knowledgeApi.createNote({
      title: `${course.value.courseName || '课程'}学习笔记`,
      content: noteContent.value,
      courseId: courseId()
    })
    ElMessage.success('笔记已保存')
    noteContent.value = ''
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

onMounted(fetchCourseData)
</script>

<style scoped>
.learn-page { display: flex; flex-direction: column; gap: 22px; }
.course-header { display: flex; align-items: flex-start; justify-content: space-between; }
.course-title { display: inline-block; margin: 0 0 8px 8px; font-size: 24px; color: #303133; }
.course-meta { margin: 0; color: #909399; font-size: 14px; }
.learn-tabs { display: flex; gap: 44px; border-bottom: 1px solid #e4e7ed; color: #606266; font-size: 18px; }
.learn-tabs span { padding: 0 0 12px; }
.learn-tabs .active { color: #20bfc8; border-bottom: 3px solid #20bfc8; }
.chapter-panel, .note-panel { background: #fff; border: 1px solid #e4e7ed; border-radius: 8px; padding: 24px; }
.chapter-title { width: 100%; display: flex; justify-content: space-between; padding-right: 20px; font-size: 18px; }
.chapter-stat { color: #909399; font-size: 13px; font-weight: 400; }
.chapter-body { padding: 12px 24px 20px; }
.learn-item { display: grid; grid-template-columns: 36px minmax(180px, 1fr) auto auto; align-items: center; gap: 12px; padding: 14px 0; color: #606266; }
.item-icon { width: 28px; height: 28px; border-radius: 50%; background: #ffe1a3; display: flex; align-items: center; justify-content: center; }
.item-icon.assignment { background: #ecf5ff; color: #409eff; font-size: 13px; font-weight: 700; }
.item-name { color: #303133; font-size: 15px; }
.item-meta { color: #909399; font-size: 13px; }
.note-panel h4 { margin: 0 0 12px; color: #303133; }
.note-actions { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
