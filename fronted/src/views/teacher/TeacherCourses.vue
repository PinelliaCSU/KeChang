<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">📚 课程管理</h2>
      <el-button type="primary" @click="showCreateDialog">+ 创建课程</el-button>
    </div>

    <div class="panel">
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索课程名称" style="width:260px" clearable @change="fetchCourses" />
        <el-select v-model="statusFilter" placeholder="状态筛选" style="width:140px" clearable @change="fetchCourses">
          <el-option label="进行中" value="进行中" />
          <el-option label="已结束" value="已结束" />
        </el-select>
      </div>

      <div class="course-grid" v-if="courses.length > 0">
        <div class="course-card" v-for="c in courses" :key="c.id" @click="router.push(`/teacher/courses/${c.id}`)">
          <div class="card-cover" :style="{ background: c.coverImg ? `url(${c.coverImg})` : getCoverGradient(c.id) }">
            <el-tag :type="c.status === '进行中' ? 'success' : 'info'" class="status-tag">{{ c.status }}</el-tag>
          </div>
          <div class="card-body">
            <h4 class="card-title">{{ c.courseName }}</h4>
            <p class="card-meta">📊 {{ c.studentCount || 0 }}人 · {{ c.teacherName }}</p>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无课程，点击上方按钮创建" :image-size="120" />

      <el-pagination v-if="total > pageSize" :current-page="page" :page-size="pageSize" :total="total"
        layout="prev, pager, next" @current-change="p => { page = p; fetchCourses() }"
        style="margin-top:20px;justify-content:center" />
    </div>

    <!-- 创建课程弹窗 -->
    <el-dialog v-model="dialogVisible" title="创建课程" width="500px">
      <el-form :model="courseForm" label-width="90px">
        <el-form-item label="课程名称" required><el-input v-model="courseForm.courseName" /></el-form-item>
        <el-form-item label="课程简介"><el-input v-model="courseForm.description" type="textarea"
            :rows="3" /></el-form-item>
        <el-form-item label="开始日期"><el-date-picker v-model="courseForm.startDate" type="date"
            style="width:100%" /></el-form-item>
        <el-form-item label="结束日期"><el-date-picker v-model="courseForm.endDate" type="date"
            style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createCourse">确定创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { courseApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const courses = ref([])
const page = ref(1); const pageSize = ref(12); const total = ref(0)
const keyword = ref(''); const statusFilter = ref('')
const dialogVisible = ref(false); const creating = ref(false)

const courseForm = reactive({
  courseName: '', description: '', startDate: '', endDate: ''
})

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
const getCoverGradient = (id) => gradientPalette[(id - 1) % gradientPalette.length]

const fetchCourses = async () => {
  try {
    const res = await courseApi.list({ page: page.value, size: pageSize.value, keyword: keyword.value, status: statusFilter.value })
    if (res.data.code === 200) {
      courses.value = res.data.data.records
      total.value = res.data.data.total
    }
  } catch (e) { /* ignore */ }
}

const showCreateDialog = () => { dialogVisible.value = true }
const createCourse = async () => {
  if (!courseForm.courseName) { ElMessage.warning('请输入课程名称'); return }
  creating.value = true
  try {
    await courseApi.create(courseForm)
    ElMessage.success('课程创建成功')
    dialogVisible.value = false
    fetchCourses()
  } catch (e) { /* ignore */ }
  finally { creating.value = false }
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

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.course-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.course-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transform: translateY(-4px);
}

.card-cover {
  height: 120px;
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  padding: 12px;
}

.status-tag {
  position: absolute;
  top: 10px;
  right: 10px;
}

.card-body {
  padding: 14px 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.card-meta {
  color: #909399;
  font-size: 13px;
}
</style>
