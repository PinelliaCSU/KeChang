<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">课程中心</h2>
      <div class="page-actions">
        <el-input v-model="keyword" placeholder="搜索课程..." size="default" style="width:240px" clearable />
      </div>
    </div>

    <div class="panel" v-if="courses.length > 0">
      <div class="course-grid">
        <div class="course-card" v-for="c in courses" :key="c.id" @click="enterLearn(c)">
          <div class="card-cover" :style="{ background: getCoverGradient(c.id) }">
            <svg class="cover-pattern" width="100%" height="100%" viewBox="0 0 200 100" preserveAspectRatio="none">
              <circle cx="160" cy="20" r="40" fill="rgba(255,255,255,0.08)" />
              <circle cx="30" cy="70" r="55" fill="rgba(255,255,255,0.05)" />
            </svg>
          </div>
          <div class="card-body">
            <h4 class="card-title" :title="c.courseName || c.name">{{ c.courseName || c.name }}</h4>
            <p class="card-meta">{{ c.teacherName || '未知教师' }}</p>
            <p class="card-code" v-if="c.courseCode">{{ c.courseCode }}</p>
          </div>
          <div class="card-footer">
            <button class="btn-enter">进入学习</button>
          </div>
        </div>
      </div>
    </div>

    <div class="panel empty-panel" v-else>
      <div class="empty-state">
        <svg class="empty-illustration" width="120" height="90" viewBox="0 0 120 90" fill="none">
          <rect x="20" y="10" width="80" height="70" rx="4" fill="#e8ecf1" />
          <rect x="28" y="20" width="28" height="4" rx="2" fill="#d0d5dd" />
          <rect x="28" y="30" width="50" height="3" rx="1.5" fill="#dce1e8" />
        </svg>
        <p class="empty-text">暂未选课，请前往课程广场选课</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { courseApi } from '@/api'

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
const getCoverGradient = (id) => gradientPalette[(id - 1) % gradientPalette.length]

const enterLearn = (c) => router.push(`/student/courses/${c.id}/learn`)

onMounted(async () => {
  try {
    const res = await courseApi.list({ page: 1, size: 50 })
    if (res.data.code === 200) {
      const data = res.data.data
      courses.value = Array.isArray(data?.records) ? data.records : Array.isArray(data?.list) ? data.list : Array.isArray(data) ? data : []
    }
  } catch (e) { /* ignore */ }
})
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
  cursor: pointer;
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
}

.btn-enter {
  width: 100%;
  height: 34px;
  border: none;
  border-radius: 8px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.btn-enter:hover {
  background: #d9ecff;
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
