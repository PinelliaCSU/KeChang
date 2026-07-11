<template>
  <aside class="sidebar-wrapper" :class="{ collapsed }">
    <el-menu :default-active="activeMenu" :default-openeds="openMenus" :collapse="collapsed" :router="true"
      background-color="transparent" text-color="#4b5563" active-text-color="#20c4cc" class="sidebar-menu">
      <template v-for="item in menuItems" :key="item.path">
        <el-sub-menu v-if="item.children" :index="item.path">
          <template #title>
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
            <span>{{ item.title }}</span>
          </template>
          <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
            <span>{{ child.title }}</span>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-else :index="item.path">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </template>
    </el-menu>

    <button class="collapse-btn" type="button" @click="collapsed = !collapsed">
      <el-icon :size="18">
        <Fold v-if="!collapsed" />
        <Expand v-else />
      </el-icon>
    </button>
  </aside>
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
  { path: '/teacher/dashboard', title: '首页', icon: 'Odometer' },
  {
    path: 'teach-sub',
    title: '教学管理',
    icon: 'Reading',
    children: [
      { path: '/teacher/courses', title: '课程管理' },
      { path: '/teacher/assignments', title: '作业管理' },
      { path: '/teacher/grades', title: '成绩管理' },
      { path: '/teacher/trainings', title: '实训管理' },
      { path: '/teacher/evaluations', title: '教学评价' }
    ]
  },
  {
    path: 'res-sub',
    title: '资源管理',
    icon: 'Folder',
    children: [
      { path: '/teacher/questions', title: '试题管理' },
      { path: '/teacher/papers', title: '试卷管理' }
    ]
  },
  {
    path: 'exam-sub',
    title: '考试测验',
    icon: 'Timer',
    children: [
      { path: '/teacher/exams', title: '考试管理' },
      { path: '/teacher/class-quizzes', title: '随堂测验' }
    ]
  },
  {
    path: 'interact-sub',
    title: '互动管理',
    icon: 'ChatDotRound',
    children: [
      { path: '/teacher/roll-call', title: '随机点名' },
      { path: '/teacher/checkin', title: '课堂签到' },
      { path: '/teacher/posts', title: '互动问答' },
      { path: '/teacher/surveys', title: '问卷管理' },
      { path: '/teacher/messages', title: '私信' }
    ]
  }
]

const studentMenus = [
  { path: '/student/dashboard', title: '首页', icon: 'Odometer' },
  {
    path: 'study-sub',
    title: '学习中心',
    icon: 'Reading',
    children: [
      { path: '/student/courses', title: '课程中心' },
      { path: '/student/assignments', title: '我的作业' },
      { path: '/student/exams', title: '在线考试' },
      { path: '/student/class-quizzes', title: '随堂测验' },
      { path: '/student/checkin', title: '课堂签到' },
      { path: '/student/trainings', title: '实训中心' },
      { path: '/student/notes', title: '学习笔记' }
    ]
  },
  {
    path: 'interact-sub',
    title: '互动交流',
    icon: 'ChatDotRound',
    children: [
      { path: '/student/posts', title: '互动问答' },
      { path: '/student/messages', title: '私信' },
      { path: '/student/knowledge', title: '知识库' }
    ]
  },
  {
    path: 'evaluate-sub',
    title: '我的评价',
    icon: 'Star',
    children: [
      { path: '/student/grades', title: '我的成绩' },
      { path: '/student/evaluations', title: '教学评价' },
      { path: '/student/surveys', title: '问卷调查' }
    ]
  }
]

const adminMenus = [
  {
    path: 'admin-teach',
    title: '教学管理',
    icon: 'Reading',
    children: [
      { path: '/admin/courses', title: '课程总览' },
      { path: '/admin/grades', title: '成绩概览' }
    ]
  },
  {
    path: 'admin-survey',
    title: '数据统计',
    icon: 'DataAnalysis',
    children: [
      { path: '/admin/surveys', title: '问卷统计' },
      { path: '/admin/operation-logs', title: '操作日志' }
    ]
  }
]

const menuItems = computed(() => {
  if (userStore.isTeacher) return teacherMenus
  if (userStore.isAdmin) return adminMenus
  return studentMenus
})

const openMenus = computed(() => {
  const path = route.path
  return menuItems.value
    .filter(item => item.children?.some(c => path === c.path || path.startsWith(c.path + '/')))
    .map(item => item.path)
})
</script>

<style scoped>
.sidebar-wrapper {
  width: 112px;
  min-height: calc(100vh - 56px);
  background: #fff;
  border-right: 1px solid #dcdfe6;
  transition: width var(--bio-dur-slow) var(--bio-ease-out);
  position: relative;
  display: flex;
  flex-direction: column;
  box-shadow: none;
}

.sidebar-wrapper.collapsed {
  width: 64px;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  padding: 20px 10px 8px;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 112px;
}

.sidebar-menu :deep(.el-menu),
.sidebar-menu :deep(.el-sub-menu .el-menu) {
  background: transparent;
  border-right: none;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 62px;
  line-height: 1.2;
  margin: 0 0 10px;
  padding: 8px 4px !important;
  border-radius: 8px;
  color: #303846;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 7px;
  font-size: 13px;
  transition: background var(--bio-dur-normal) var(--bio-ease-out), color var(--bio-dur-normal) var(--bio-ease-out);
}

.sidebar-menu :deep(.el-menu-item span),
.sidebar-menu :deep(.el-sub-menu__title span) {
  line-height: 1.2;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover),
.sidebar-menu :deep(.el-menu-item.is-active) {
  color: #20c4cc;
  background: #e7fbfc !important;
  font-weight: 500;
}

.sidebar-menu :deep(.el-menu-item.is-active .el-icon),
.sidebar-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title .el-icon) {
  color: #20c4cc;
}

.sidebar-menu :deep(.el-icon) {
  margin: 0 !important;
  font-size: 22px;
  color: inherit;
}

.sidebar-menu :deep(.el-sub-menu__icon-arrow) {
  display: none;
}

.sidebar-menu :deep(.el-menu--inline .el-menu-item) {
  height: 38px;
  align-items: center;
  flex-direction: row;
  justify-content: flex-start;
  padding-left: 10px !important;
  font-size: 13px;
}

.collapse-btn {
  height: 52px;
  border: none;
  border-top: 1px solid #e5e7eb;
  background: #fff;
  color: #6b7280;
  cursor: pointer;
}

.collapse-btn:hover {
  color: #20c4cc;
  background: #f7feff;
}
</style>
