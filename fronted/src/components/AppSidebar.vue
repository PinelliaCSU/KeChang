<template>
  <div class="sidebar-wrapper" :class="{ collapsed }">
    <el-menu :default-active="activeMenu" :collapse="collapsed" :router="true" background-color="transparent"
      text-color="#4a544c" active-text-color="#606c38" class="sidebar-menu">
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
    <div class="collapse-btn" @click="collapsed = !collapsed">
      <el-icon :size="18">
        <Fold v-if="!collapsed" />
        <Expand v-else />
      </el-icon>
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
  background: linear-gradient(180deg, #fdfbf7 0%, #f7f5ed 100%);
  border-right: 1px solid rgba(163, 177, 138, 0.15);
  transition: width var(--bio-dur-slow) var(--bio-ease-out);
  position: relative;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 16px rgba(44, 54, 30, 0.04);
}

/* 叶脉纹理装饰 */
.sidebar-wrapper::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 90% 20%, rgba(163, 177, 138, 0.06) 0%, transparent 50%),
    radial-gradient(circle at 20% 85%, rgba(165, 196, 212, 0.05) 0%, transparent 40%);
  pointer-events: none;
}

.sidebar-wrapper.collapsed {
  width: 64px;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  background: transparent;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 220px;
}

/* Element Plus 菜单覆盖 — 纸纤维风格 */
.sidebar-menu :deep(.el-menu) {
  background: transparent;
  border-right: none;
}

.sidebar-menu :deep(.el-menu-item) {
  color: var(--bio-ink-soft);
  font-size: 14px;
  height: 46px;
  line-height: 46px;
  margin: 3px 10px;
  border-radius: var(--bio-radius-md);
  transition: all var(--bio-dur-normal) var(--bio-ease-out);
  position: relative;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  color: var(--bio-moss);
  background: rgba(163, 177, 138, 0.12);
  filter: brightness(1.05);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  color: var(--bio-moss);
  background: linear-gradient(135deg, rgba(163, 177, 138, 0.18), rgba(163, 177, 138, 0.10));
  font-weight: 600;
}

.sidebar-menu :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--bio-leaf);
}

.sidebar-menu :deep(.el-sub-menu__title) {
  color: var(--bio-ink-soft);
  font-size: 14px;
  height: 46px;
  line-height: 46px;
  margin: 3px 10px;
  border-radius: var(--bio-radius-md);
  transition: all var(--bio-dur-normal) var(--bio-ease-out);
}

.sidebar-menu :deep(.el-sub-menu__title:hover) {
  color: var(--bio-moss);
  background: rgba(163, 177, 138, 0.10);
}

.sidebar-menu :deep(.el-icon) {
  color: inherit;
}

.collapse-btn {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--bio-ink-muted);
  border-top: 1px solid rgba(163, 177, 138, 0.12);
  transition: color var(--bio-dur-normal) var(--bio-ease-out),
    background var(--bio-dur-normal) var(--bio-ease-out);
}

.collapse-btn:hover {
  color: var(--bio-moss);
  background: rgba(163, 177, 138, 0.08);
  filter: brightness(1.05);
}

.collapse-btn:active {
  transform: translateY(1px);
}
</style>
