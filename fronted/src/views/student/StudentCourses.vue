<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">课程中心</h2>
      <div class="page-actions">
        <el-input v-model="keyword" placeholder="搜索课程..." style="width: 260px" clearable @change="onKeywordChange" @clear="onKeywordChange" />
      </div>
    </div>

    <div class="panel" v-if="courses.length > 0">
      <div class="course-grid">
        <article v-for="course in courses" :key="course.id" class="course-card" @click="viewDetail(course)">
          <div
            class="card-cover"
            :style="{ background: course.coverImg ? `url(${resolveFileUrl(course.coverImg)}) center/cover no-repeat` : getCoverColor(course.id) }"
            :class="{ 'has-cover': course.coverImg }"
          >
            <CourseCoverPattern v-if="!course.coverImg" />
          </div>

          <div class="card-body">
            <div class="card-title-row">
              <h4 class="card-title" :title="course.courseName || course.name">{{ course.courseName || course.name }}</h4>
              <el-tag :type="statusTagType(computeStatus(course.startDate, course.endDate))" effect="plain" size="small">
                {{ computeStatus(course.startDate, course.endDate) }}
              </el-tag>
            </div>
            <p class="card-meta">{{ course.teacherName || '未知教师' }}</p>
          </div>

          <div class="card-footer">
            <button v-if="course.enrolled" class="btn-enter" @click.stop="enterLearn(course)">进入学习</button>
            <button v-else class="btn-detail" @click.stop="viewDetail(course)">查看详情</button>

            <button
              v-if="!course.enrolled"
              :class="canEnroll(course) ? 'btn-enroll' : 'btn-enroll btn-disabled'"
              :disabled="!canEnroll(course)"
              @click.stop="canEnroll(course) && enrollCourse(course)"
            >
              {{ canEnroll(course) ? '加入课程' : '暂不可选' }}
            </button>
            <button v-else class="btn-unenroll" @click.stop="unenrollCourse(course)">退出课程</button>
          </div>
        </article>
      </div>
      <PaginationWrapper v-model:page="page" v-model:size="size" :total="total" :page-sizes="[12, 24, 48]" @change="fetchCourses" />
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
import { computeStatus, statusTagType } from '@/utils/status'
import { resolveFileUrl } from '@/utils/fileUrl'
import { usePagination } from '@/composables/usePagination'
import PaginationWrapper from '@/components/PaginationWrapper.vue'

const router = useRouter()
const courses = ref([])
const keyword = ref('')
const { page, size, total, resetPage } = usePagination({ defaultSize: 12, pageSizes: [12, 24, 48] })

const coverPalette = ['#f5f7fa', '#f2f6f8', '#f7f8fa', '#eef7f8']

const getCoverColor = (id) => coverPalette[((id || 1) - 1) % coverPalette.length]

const canEnroll = (course) => computeStatus(course.startDate, course.endDate) === '进行中'

const viewDetail = (course) => {
  router.push(`/student/courses/${course.id}`)
}

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
    ElMessage.success('选课成功')
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
    const res = await courseApi.list({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    if (res.data.code === 200) {
      const data = res.data.data
      courses.value = Array.isArray(data?.records)
        ? data.records
        : Array.isArray(data?.list)
          ? data.list
          : Array.isArray(data)
            ? data
            : []
      total.value = data?.total || courses.value.length
    }
  } catch (e) {
    ElMessage.error('课程列表加载失败')
  }
}

const onKeywordChange = () => {
  resetPage()
  fetchCourses()
}

onMounted(fetchCourses)
</script>

<style scoped>
.page-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 18px;
}

.course-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--bio-border);
  border-radius: 0;
  cursor: pointer;
  transition: border-color 160ms ease, background-color 160ms ease;
}

.course-card:hover {
  border-color: var(--bio-primary);
  background: #fcffff;
}

.card-cover {
  position: relative;
  height: 116px;
  overflow: hidden;
  border-bottom: 1px solid var(--bio-border);
}

.card-cover.has-cover {
  background-size: cover !important;
  background-position: center !important;
}

.card-body {
  flex: 1;
  min-width: 0;
  padding: 16px 16px 10px;
}

.card-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.card-title {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: var(--bio-ink);
  font-size: 17px;
  font-weight: 600;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  margin: 8px 0 0;
  color: var(--bio-ink-muted);
  font-size: 15px;
}

.card-footer {
  display: grid;
  gap: 10px;
  padding: 10px 16px 16px;
}

.btn-enter,
.btn-enroll,
.btn-unenroll,
.btn-detail {
  width: 100%;
  height: 38px;
  border: 1px solid transparent;
  border-radius: 4px;
  font-family: inherit;
  font-size: 15px;
  cursor: pointer;
  transition: background 160ms ease, border-color 160ms ease, color 160ms ease;
}

.btn-enter,
.btn-enroll {
  background: var(--bio-primary);
  color: #fff;
}

.btn-enter:hover,
.btn-enroll:hover {
  background: var(--bio-primary-dark);
}

.btn-detail {
  background: #f5f6f8;
  color: var(--bio-primary);
}

.btn-detail:hover {
  background: var(--bio-primary-soft);
}

.btn-unenroll {
  background: #fff;
  border-color: var(--bio-danger);
  color: var(--bio-danger);
}

.btn-unenroll:hover {
  background: #fff5f7;
}

.btn-enroll.btn-disabled {
  background: #f4f4f5;
  color: #a8abb2;
  cursor: not-allowed;
}

.empty-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 280px;
}
</style>
