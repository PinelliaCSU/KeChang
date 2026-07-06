<template>
  <div class="sidebar-wrapper" :class="{ collapsed }">
    <el-menu
      :default-active="activeMenu"
      :collapse="collapsed"
      :router="true"
      background-color="#001529"
      text-color="#ffffffa6"
      active-text-color="#1890ff"
      class="sidebar-menu"
    >
      <template v-for="item in menuItems" :key="item.path">
        <el-sub-menu v-if="item.children" :index="item.path">
          <template #title>
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </template>
          <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
            <span>{{ child.title }}</span>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-else :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </template>
    </el-menu>
    <div class="collapse-btn" @click="collapsed = !collapsed">
      <el-icon :size="18"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const collapsed = ref(false)
const activeMenu = computed(() => route.path)

const teacherMenus = [
  { path: '/teacher/dashboard', title: '仪表盘', icon: 'Odometer' },
  { path: '/teacher/courses', title: '课程管理', icon: 'Reading' },
  { path: '/teacher/questions', title: '试题管理', icon: 'EditPen' },
  { path: '/teacher/papers', title: '试卷管理', icon: 'Document' },
  { path: '/teacher/exams', title: '考试管理', icon: 'Timer' },
  { path: '/teacher/assignments', title: '作业管理', icon: 'Notebook' },
  { path: '/teacher/grades', title: '成绩管理', icon: 'TrendCharts' },
  { path: '/teacher/trainings', title: '实训管理', icon: 'Monitor' },
  { path: '/teacher/posts', title: '互动问答', icon: 'ChatDotRound' },
  { path: '/teacher/surveys', title: '问卷管理', icon: 'List' },
  { path: '/teacher/evaluations', title: '教学评价', icon: 'Star' }
]

const studentMenus = [
  { path: '/student/dashboard', title: '仪表盘', icon: 'Odometer' },
  { path: '/student/courses', title: '课程中心', icon: 'Reading' },
  { path: '/student/assignments', title: '我的作业', icon: 'Notebook' },
  { path: '/student/exams', title: '在线考试', icon: 'Timer' },
  { path: '/student/grades', title: '我的成绩', icon: 'TrendCharts' },
  { path: '/student/notes', title: '学习笔记', icon: 'Edit' },
  { path: '/student/trainings', title: '实训中心', icon: 'Monitor' },
  { path: '/student/posts', title: '互动问答', icon: 'ChatDotRound' },
  { path: '/student/knowledge', title: '知识库', icon: 'Collection' },
  { path: '/student/evaluations', title: '教学评价', icon: 'Star' }
]

const menuItems = computed(() => userStore.isTeacher ? teacherMenus : studentMenus)
</script>

<style scoped>
.sidebar-wrapper {
  width: 220px;
  min-height: calc(100vh - 60px);
  background: #001529;
  transition: width 0.3s;
  position: relative;
  display: flex;
  flex-direction: column;
}
.sidebar-wrapper.collapsed { width: 64px; }
.sidebar-menu { border-right: none; flex: 1; overflow-y: auto; }
.sidebar-menu:not(.el-menu--collapse) { width: 220px; }
.collapse-btn {
  height: 48px; display: flex; align-items: center; justify-content: center;
  cursor: pointer; color: #ffffffa6; border-top: 1px solid rgba(255,255,255,0.1);
}
.collapse-btn:hover { color: #1890ff; }
</style>
