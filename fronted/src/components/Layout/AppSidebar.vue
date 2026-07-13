<template>
  <aside class="sidebar-wrapper" :class="{ collapsed, resizing: isResizing }"
    :style="{ width: collapsed ? '64px' : sidebarWidth + 'px' }">
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

    <!-- 拖拽调整宽度手柄 -->
    <div v-if="!collapsed" class="sidebar-resize-handle" @mousedown.prevent="startResize"></div>
  </aside>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const collapsed = ref(false)
const activeMenu = computed(() => route.path)

// 侧边栏拖拽调整宽度
const SIDEBAR_MIN_WIDTH = 112
const SIDEBAR_MAX_WIDTH = 400
const sidebarWidth = ref(SIDEBAR_MIN_WIDTH)
const isResizing = ref(false)

function startResize(e) {
  isResizing.value = true
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', stopResize)
  // 防止拖动时选中文本
  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'col-resize'
}

function onMouseMove(e) {
  if (!isResizing.value) return
  const newWidth = Math.min(SIDEBAR_MAX_WIDTH, Math.max(SIDEBAR_MIN_WIDTH, e.clientX))
  sidebarWidth.value = newWidth
}

function stopResize() {
  isResizing.value = false
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', stopResize)
  document.body.style.userSelect = ''
  document.body.style.cursor = ''
}

// 展开侧边栏时恢复为最小宽度
watch(collapsed, (val) => {
  if (!val) {
    sidebarWidth.value = SIDEBAR_MIN_WIDTH
  }
})

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
      { path: '/teacher/knowledge', title: '智能知识库' },
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
      { path: '/student/class-quizzes', title: '随堂测验' }
    ]
  },
  {
    path: 'practice-sub',
    title: '实践拓展',
    icon: 'TrendCharts',
    children: [
      { path: '/student/checkin', title: '课堂签到' },
      { path: '/student/trainings', title: '实训中心' },
      { path: '/student/course-knowledge', title: '智能知识库' },
      { path: '/student/knowledge', title: '反思' }
    ]
  },
  {
    path: 'interact-sub',
    title: '互动交流',
    icon: 'ChatDotRound',
    children: [
      { path: '/student/posts', title: '互动问答' },
      { path: '/student/messages', title: '私信' }
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
  { path: '/admin/dashboard', title: '首页', icon: 'Odometer' },
  {
    path: 'admin-teach',
    title: '教学管理',
    icon: 'Reading',
    children: [
      { path: '/admin/courses', title: '课程总览' },
      { path: '/admin/knowledge', title: '智能知识库' },
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
  },
  {
    path: 'admin-system',
    title: '系统管理',
    icon: 'Setting',
    children: [
      { path: '/admin/users', title: '账号管理' },
      { path: '/admin/announcements', title: '平台公告' }
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
  min-height: calc(100vh - 56px);
  background: #fff;
  border-right: 1px solid #dcdfe6;
  transition: width var(--bio-dur-slow) var(--bio-ease-out);
  position: relative;
  display: flex;
  flex-direction: column;
  box-shadow: none;
  flex-shrink: 0;
}

.sidebar-wrapper.resizing {
  transition: none;
  user-select: none;
}

.sidebar-wrapper.collapsed {
  width: 64px !important;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  padding: 20px 10px 8px;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 100%;
}

/* 拖拽调整宽度手柄 */
.sidebar-resize-handle {
  position: absolute;
  top: 0;
  right: -4px;
  width: 8px;
  height: 100%;
  cursor: col-resize;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background var(--bio-dur-normal) var(--bio-ease-out);
}

.sidebar-resize-handle::after {
  content: '';
  width: 2px;
  height: 32px;
  border-radius: 1px;
  background: #dcdfe6;
  transition: background var(--bio-dur-normal) var(--bio-ease-out), height var(--bio-dur-normal) var(--bio-ease-out);
}

.sidebar-resize-handle:hover::after,
.sidebar-resize-handle:active::after {
  background: #20c4cc;
  height: 48px;
}

.sidebar-wrapper.resizing .sidebar-resize-handle::after {
  background: #20c4cc;
  height: 48px;
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
  font-size: 15px;
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
  justify-content: center;
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
