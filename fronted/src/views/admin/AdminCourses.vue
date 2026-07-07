<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div>
        <h2 class="page-title">课程总览</h2>
        <p class="page-subtitle">查看平台课程总量、状态分布和选课情况</p>
      </div>
      <el-button @click="fetchCourses">刷新</el-button>
    </div>

    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-num">{{ total }}</div>
        <div class="stat-label">课程总量</div>
      </div>
      <div class="stat-card">
        <div class="stat-num success">{{ runningCount }}</div>
        <div class="stat-label">进行中</div>
      </div>
      <div class="stat-card">
        <div class="stat-num muted">{{ endedCount }}</div>
        <div class="stat-label">已结束</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ totalStudents }}</div>
        <div class="stat-label">累计选课人次</div>
      </div>
    </div>

    <div class="panel">
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索课程名称" clearable style="width:260px" @change="fetchCourses" @clear="fetchCourses" />
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:150px" @change="fetchCourses">
          <el-option label="进行中" value="进行中" />
          <el-option label="已结束" value="已结束" />
        </el-select>
      </div>

      <el-table :data="courses" border stripe v-loading="loading">
        <el-table-column prop="courseName" label="课程名称" min-width="180" />
        <el-table-column prop="teacherName" label="授课教师" width="140" />
        <el-table-column prop="studentCount" label="选课人数" width="100" />
        <el-table-column prop="startDate" label="开始日期" width="130" />
        <el-table-column prop="endDate" label="结束日期" width="130" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === '进行中' ? 'success' : 'info'">{{ row.status || '进行中' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="简介" min-width="220" show-overflow-tooltip />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { courseApi } from '@/api'
import { ElMessage } from 'element-plus'

const courses = ref([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref('')

const runningCount = computed(() => courses.value.filter(course => (course.status || '进行中') === '进行中').length)
const endedCount = computed(() => courses.value.filter(course => course.status === '已结束').length)
const totalStudents = computed(() => courses.value.reduce((sum, course) => sum + Number(course.studentCount || 0), 0))

const fetchCourses = async () => {
  loading.value = true
  try {
    const res = await courseApi.list({
      page: 1,
      size: 500,
      keyword: keyword.value,
      status: statusFilter.value
    })
    if (res.data.code === 200) {
      const data = res.data.data
      courses.value = data?.records || []
      total.value = data?.total || courses.value.length
    }
  } catch (e) {
    ElMessage.error('课程总览加载失败')
  } finally {
    loading.value = false
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
.stat-row {
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

.page-subtitle {
  color: #909399;
  font-size: 13px;
  margin: 6px 0 0;
}

.stat-row {
  gap: 16px;
}

.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border: 1px solid #ebeef5;
}

.stat-num {
  color: #303133;
  font-size: 28px;
  font-weight: 700;
}

.stat-num.success {
  color: #67c23a;
}

.stat-num.muted {
  color: #909399;
}

.stat-label {
  color: #606266;
  font-size: 13px;
  margin-top: 6px;
}

.panel {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 24px;
}

.search-bar {
  gap: 12px;
  margin-bottom: 16px;
}
</style>
