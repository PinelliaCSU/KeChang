<template>
  <div class="teacher-courses">
    <div class="page-header">
      <h2>📚 课程管理</h2>
      <el-button type="primary" @click="showCreateDialog">+ 创建课程</el-button>
    </div>

    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索课程名称" style="width:260px" clearable @change="fetchCourses" />
      <el-select v-model="statusFilter" placeholder="状态筛选" style="width:140px" clearable @change="fetchCourses">
        <el-option label="进行中" value="进行中" />
        <el-option label="已结束" value="已结束" />
      </el-select>
    </div>

    <el-row :gutter="20">
      <el-col v-for="c in courses" :key="c.id" :span="6" style="margin-bottom:20px">
        <el-card shadow="hover" class="course-card" @click="router.push(`/teacher/courses/${c.id}`)">
          <div class="course-cover" :style="{ background: c.coverImg ? `url(${c.coverImg})` : 'linear-gradient(135deg, #1890ff, #36cfc9)' }">
            <el-tag :type="c.status === '进行中' ? 'success' : 'info'" class="status-tag">{{ c.status }}</el-tag>
          </div>
          <div class="course-info">
            <h4>{{ c.courseName }}</h4>
            <p>📊 {{ c.studentCount }}人 · {{ c.teacherName }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="24" v-if="courses.length === 0">
        <el-empty description="暂无课程，点击上方按钮创建" />
      </el-col>
    </el-row>

    <el-pagination v-if="total > pageSize" :current-page="page" :page-size="pageSize"
                   :total="total" layout="prev, pager, next" @current-change="p => { page = p; fetchCourses() }" />

    <!-- 创建课程弹窗 -->
    <el-dialog v-model="dialogVisible" title="创建课程" width="500px">
      <el-form :model="courseForm" label-width="90px">
        <el-form-item label="课程名称" required><el-input v-model="courseForm.courseName" /></el-form-item>
        <el-form-item label="课程简介"><el-input v-model="courseForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="开始日期"><el-date-picker v-model="courseForm.startDate" type="date" style="width:100%" /></el-form-item>
        <el-form-item label="结束日期"><el-date-picker v-model="courseForm.endDate" type="date" style="width:100%" /></el-form-item>
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
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { color: #303133; }
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; }
.course-card { cursor: pointer; transition: all 0.3s; }
.course-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(24,144,255,0.15); }
.course-cover { height: 140px; border-radius: 8px 8px 0 0; position: relative; display: flex; align-items: flex-end; justify-content: flex-end; padding: 12px; }
.status-tag { position: absolute; top: 10px; right: 10px; }
.course-info { padding: 12px 0 4px; }
.course-info h4 { margin-bottom: 4px; color: #303133; }
.course-info p { color: #909399; font-size: 13px; }
</style>
