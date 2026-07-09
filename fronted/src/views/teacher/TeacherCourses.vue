<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">课程管理</h2>
      <el-button type="primary" @click="openCreateDialog">+ 创建课程</el-button>
    </div>

    <div class="panel">
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索课程名称" style="width:260px" clearable @change="fetchCourses"
          @clear="fetchCourses" />
        <el-select v-model="statusFilter" placeholder="状态筛选" style="width:140px" clearable @change="fetchCourses">
          <el-option label="进行中" value="进行中" />
          <el-option label="已结束" value="已结束" />
        </el-select>
      </div>

      <div class="course-grid" v-if="courses.length > 0">
        <div class="course-card" v-for="course in courses" :key="course.id">
          <div class="card-cover"
            :style="{ background: course.coverImg ? `url(${resolveFileUrl(course.coverImg)}) center/cover no-repeat` : getCoverGradient(course.id) }"
            :class="{ 'has-cover': course.coverImg }">
            <el-tag :type="statusTagType(computeStatus(course.startDate, course.endDate))" class="status-tag">
              {{ computeStatus(course.startDate, course.endDate) }}
            </el-tag>
          </div>
          <div class="card-body">
            <h4 class="card-title" :title="course.courseName">{{ course.courseName }}</h4>
            <p class="card-desc">{{ course.description || '暂无课程简介' }}</p>
            <div class="card-meta">
              <span>{{ course.studentCount || 0 }} 人选课</span>
              <span>{{ course.startDate || '--' }} 至 {{ course.endDate || '--' }}</span>
            </div>
          </div>
          <div class="card-actions">
            <el-button size="small" type="primary"
              @click="router.push(`/teacher/courses/${course.id}`)">课程详情</el-button>
            <el-button size="small" @click="router.push(`/teacher/courses/${course.id}/edit`)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteCourse(course)">删除</el-button>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无课程，点击上方按钮创建" :image-size="120" />

      <el-pagination v-if="total > pageSize" v-model:current-page="page" :page-size="pageSize" :total="total"
        layout="prev, pager, next" style="margin-top:20px;justify-content:center" @current-change="fetchCourses" />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑课程' : '创建课程'" width="520px">
      <el-form :model="courseForm" label-width="90px">
        <el-form-item label="课程名称" required>
          <el-input v-model="courseForm.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="课程简介">
          <el-input v-model="courseForm.description" type="textarea" :rows="3" placeholder="请输入课程简介" />
        </el-form-item>
        <el-form-item label="封面地址">
          <el-input v-model="courseForm.coverImg" placeholder="可选，图片 URL" />
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker v-model="courseForm.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束日期" required>
          <el-date-picker v-model="courseForm.endDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="课程状态">
          <el-select v-model="courseForm.status" style="width:100%">
            <el-option label="进行中" value="进行中" />
            <el-option label="已结束" value="已结束" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCourse">
          {{ editingId ? '保存修改' : '确定创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { courseApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computeStatus, statusTagType } from '@/utils/status'
import { resolveFileUrl } from '@/utils/fileUrl'

const router = useRouter()
const courses = ref([])
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref('')
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)

const defaultForm = () => ({
  courseName: '',
  coverImg: '',
  description: '',
  startDate: '',
  endDate: '',
  status: '进行中'
})

const courseForm = reactive(defaultForm())

const gradientPalette = [
  'linear-gradient(135deg, #3b5998 0%, #4c6da7 50%, #5b7ec2 100%)',
  'linear-gradient(135deg, #0d7377 0%, #14a3a8 50%, #20c997 100%)',
  'linear-gradient(135deg, #b45309 0%, #d97706 50%, #f59e0b 100%)',
  'linear-gradient(135deg, #9b1b30 0%, #c0392b 50%, #e74c3c 100%)',
  'linear-gradient(135deg, #2c3e50 0%, #3498db 50%, #2980b9 100%)'
]

const getCoverGradient = (id) => gradientPalette[((id || 1) - 1) % gradientPalette.length]

const resetForm = () => Object.assign(courseForm, defaultForm())

const fetchCourses = async () => {
  try {
    const res = await courseApi.list({
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value,
      status: statusFilter.value
    })
    if (res.data.code === 200) {
      courses.value = res.data.data?.records || []
      total.value = res.data.data?.total || 0
    }
  } catch (e) {
    ElMessage.error('课程列表加载失败')
  }
}

const openCreateDialog = () => {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (course) => {
  editingId.value = course.id
  Object.assign(courseForm, {
    courseName: course.courseName || '',
    coverImg: course.coverImg || '',
    description: course.description || '',
    startDate: course.startDate || '',
    endDate: course.endDate || '',
    status: course.status || '进行中'
  })
  dialogVisible.value = true
}

const saveCourse = async () => {
  if (!courseForm.courseName.trim()) {
    ElMessage.warning('请输入课程名称')
    return
  }
  if (!courseForm.startDate || !courseForm.endDate) {
    ElMessage.warning('请选择课程起止日期')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await courseApi.update(editingId.value, courseForm)
      ElMessage.success('课程已更新')
    } else {
      await courseApi.create(courseForm)
      ElMessage.success('课程创建成功')
    }
    dialogVisible.value = false
    fetchCourses()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存课程失败')
  } finally {
    saving.value = false
  }
}

const deleteCourse = async (course) => {
  try {
    await ElMessageBox.confirm(`确定删除课程“${course.courseName}”吗？`, '删除课程', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await courseApi.delete(course.id)
    ElMessage.success('课程已删除')
    fetchCourses()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除课程失败')
    }
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

.page-title-bar,
.search-bar,
.card-actions,
.card-meta {
  display: flex;
  align-items: center;
}

.page-title-bar {
  justify-content: space-between;
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
  gap: 12px;
  margin-bottom: 20px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.course-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.2s;
}

.course-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.card-cover {
  height: 110px;
  position: relative;
}

.card-cover.has-cover {
  background-size: cover !important;
  background-position: center !important;
}

.status-tag {
  position: absolute;
  top: 10px;
  right: 10px;
}

.card-body {
  padding: 14px 16px 8px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  height: 38px;
  color: #606266;
  font-size: 13px;
  line-height: 19px;
  margin: 0 0 10px;
  overflow: hidden;
}

.card-meta {
  justify-content: space-between;
  gap: 10px;
  color: #909399;
  font-size: 12px;
}

.card-actions {
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 16px 14px;
  border-top: 1px solid #f0f2f5;
}
</style>
