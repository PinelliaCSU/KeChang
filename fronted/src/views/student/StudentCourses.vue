<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">课程中心</h2>
      <div class="page-actions">
        <el-input v-model="keyword" placeholder="搜索课程..." size="default" style="width:240px" clearable />
      </div>
    </div>

    <div class="panel" v-if="filteredCourses.length > 0">
      <div class="course-grid">
        <div class="course-card" v-for="course in filteredCourses" :key="course.id">
          <div class="card-cover" :style="{ background: getCoverGradient(course.id) }">
            <CourseCoverPattern />
          </div>
          <div class="card-body">
            <h4 class="card-title" :title="course.courseName || course.name">{{ course.courseName || course.name }}</h4>
            <p class="card-meta">{{ course.teacherName || '未知教师' }}</p>
          </div>
          <div class="card-footer">
            <button class="btn-enter" :disabled="!course.enrolled" @click="enterLearn(course)">
              {{ course.enrolled ? '进入学习' : '先加入后学习' }}
            </button>
            <button :class="course.enrolled ? 'btn-unenroll' : 'btn-enroll'"
              @click="course.enrolled ? unenrollCourse(course) : enrollCourse(course)">
              {{ course.enrolled ? '退出课程' : '加入课程' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="panel empty-panel" v-else>
      <EmptyIllustration text="暂无课程" />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { courseApi } from '@/api'
import { ElMessage } from 'element-plus'
import CourseCoverPattern from '@/components/CourseCoverPattern.vue'
import EmptyIllustration from '@/components/EmptyIllustration.vue'

const router = useRouter()
const courses = ref([])
const keyword = ref('')

const gradientPalette = [
  'linear-gradient(135deg, #3b5998 0%, #4c6da7 50%, #5b7ec2 100%)',
  'linear-gradient(135deg, #1e4d92 0%, #2d6bc4 50%, #3b82f6 100%)',
  'linear-gradient(135deg, #0d7377 0%, #14a3a8 50%, #20c997 100%)',
  'linear-gradient(135deg, #4a1a8a 0%, #6b3fb8 50%, #8b5cf6 100%)',
  'linear-gradient(135deg, #b45309 0%, #d97706 50%, #f59e0b 100%)',
  'linear-gradient(135deg, #9b1b30 0%, #c0392b 50%, #e74c3c 100%)',
  'linear-gradient(135deg, #2c3e50 0%, #3498db 50%, #2980b9 100%)',
  'linear-gradient(135deg, #6c3483 0%, #8e44ad 50%, #a569bd 100%)'
]

const filteredCourses = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return courses.value
  return courses.value.filter(course => String(course.courseName || course.name || '').toLowerCase().includes(kw))
})

const getCoverGradient = (id) => gradientPalette[((id || 1) - 1) % gradientPalette.length]
const enterLearn = (course) => {
  if (!course.enrolled) {
    ElMessage.warning('请先加入课程')
    return
  }
  router.push(`/student/courses/${course.id}/learn`)
}

const enrollCourse = async (course) => {
  if (course.enrolled) {
    ElMessage.info('已加入该课程')
    return
  }
  try {
    await courseApi.enroll(course.id)
    course.enrolled = true
    course.studentCount = Number(course.studentCount || 0) + 1
    ElMessage.success('选课成功，现在可以在我的作业中查看该课程作业')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '选课失败')
  }
}

const unenrollCourse = async (course) => {
  try {
    await courseApi.unenroll(course.id)
    course.enrolled = false
    course.studentCount = Math.max(0, Number(course.studentCount || 0) - 1)
    ElMessage.success('已退出课程')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '退课失败')
  }
}

const fetchCourses = async () => {
  try {
    const res = await courseApi.list({ page: 1, size: 50 })
    if (res.data.code === 200) {
      const data = res.data.data
      courses.value = Array.isArray(data?.records)
        ? data.records
        : Array.isArray(data?.list)
          ? data.list
          : Array.isArray(data)
            ? data
            : []
    }
  } catch (e) {
    ElMessage.error('课程列表加载失败')
  }
}

onMounted(fetchCourses)
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

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.course-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  overflow: hidden;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
}

.course-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transform: translateY(-4px);
}

.card-cover {
  height: 100px;
  position: relative;
  overflow: hidden;
}

.card-body {
  padding: 12px 14px 8px;
  flex: 1;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-meta {
  font-size: 12px;
  color: #606266;
  margin-bottom: 2px;
}

.card-code {
  font-size: 11px;
  color: #c0c4cc;
}

.card-footer {
  padding: 8px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.btn-enter,
.btn-enroll,
.btn-unenroll {
  width: 100%;
  height: 34px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.btn-enter {
  background: #ecf5ff;
  color: #409eff;
}

.btn-enter:hover {
  background: #d9ecff;
}

.btn-enroll {
  background: #f0f9eb;
  color: #67c23a;
}

.btn-enroll:hover {
  background: #e1f3d8;
}

.btn-unenroll {
  background: #fef0f0;
  color: #f56c6c;
}

.btn-unenroll:hover {
  background: #fde2e2;
}

.btn-enter:disabled {
  background: #f4f4f5;
  color: #a8abb2;
  cursor: not-allowed;
}

.empty-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.empty-illustration {
  opacity: 0.6;
}

.empty-text {
  color: #c0c4cc;
  font-size: 14px;
}
</style>
