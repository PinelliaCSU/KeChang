<template>
  <div class="page-container">
    <h2>📚 课程中心</h2>
    <el-row :gutter="20">
      <el-col v-for="c in courses" :key="c.id" :span="6" style="margin-bottom:20px">
        <el-card shadow="hover" class="course-card" @click="router.push(`/student/courses/${c.id}/learn`)">
          <div class="course-cover" style="background:linear-gradient(135deg, #1890ff, #36cfc9);height:120px;border-radius:8px 8px 0 0" />
          <div class="course-info" style="padding:12px 0">
            <h4>{{ c.courseName }}</h4>
            <p>{{ c.teacherName }} · {{ c.studentCount }}人</p>
            <el-progress :percentage="c.progress || 0" :stroke-width="8" />
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="courses.length === 0" description="暂未选课，请前往课程广场选课" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { courseApi } from '@/api'

const router = useRouter()
const courses = ref([])

onMounted(async () => {
  try {
    const res = await courseApi.list({ page: 1, size: 50 })
    if (res.data.code === 200) courses.value = res.data.data.records || []
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.page-container h2 { margin-bottom: 20px; color: #303133; }
.course-card { cursor: pointer; transition: all 0.3s; }
.course-card:hover { transform: translateY(-4px); }
</style>
