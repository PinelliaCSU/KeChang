import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/', name: 'Root', meta: { title: '课畅' } },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '课畅 - 仪表盘', requiresAuth: true }
  },

  // ==================== 日历 ====================
  {
    path: '/calendar',
    name: 'Calendar',
    component: () => import('@/views/Calendar.vue'),
    meta: { title: '日历 - 课畅', requiresAuth: true }
  },

  // ==================== 个人中心 ====================
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人中心 - 课畅', requiresAuth: true }
  },

  // ==================== 登录 ====================
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录 - 课畅' }
  },

  // ==================== 教师端 ====================
  {
    path: '/teacher',
    redirect: '/teacher/dashboard',
    component: () => import('@/views/teacher/TeacherLayout.vue'),
    meta: { role: 'ROLE_TEACHER' },
    children: [
      { path: 'dashboard', name: 'TeacherDashboard', component: () => import('@/views/teacher/TeacherDashboard.vue'), meta: { title: '教师仪表盘' } },
      { path: 'courses', name: 'TeacherCourses', component: () => import('@/views/teacher/TeacherCourses.vue'), meta: { title: '课程管理' } },
      { path: 'courses/:id', name: 'TeacherCourseDetail', component: () => import('@/views/teacher/TeacherCourseDetail.vue'), meta: { title: '课程详情' } },
      { path: 'courses/:id/edit', name: 'TeacherCourseEdit', component: () => import('@/views/teacher/TeacherCourseEdit.vue'), meta: { title: '编辑课程' } },
      { path: 'questions', name: 'TeacherQuestions', component: () => import('@/views/teacher/TeacherQuestions.vue'), meta: { title: '试题管理' } },
      { path: 'papers', name: 'TeacherPapers', component: () => import('@/views/teacher/TeacherPapers.vue'), meta: { title: '试卷管理' } },
      { path: 'exams', name: 'TeacherExams', component: () => import('@/views/teacher/TeacherExams.vue'), meta: { title: '考试管理' } },
      { path: 'assignments', name: 'TeacherAssignments', component: () => import('@/views/teacher/TeacherAssignments.vue'), meta: { title: '作业管理' } },
      { path: 'grades', name: 'TeacherGrades', component: () => import('@/views/teacher/TeacherGrades.vue'), meta: { title: '成绩管理' } },
      { path: 'trainings', name: 'TeacherTrainings', component: () => import('@/views/teacher/TeacherTrainings.vue'), meta: { title: '实训管理' } },
      { path: 'surveys', name: 'TeacherSurveys', component: () => import('@/views/teacher/TeacherSurveys.vue'), meta: { title: '问卷管理' } },
      { path: 'evaluations', name: 'TeacherEvaluations', component: () => import('@/views/teacher/TeacherEvaluations.vue'), meta: { title: '教学评价' } },
      { path: 'posts', name: 'TeacherPosts', component: () => import('@/views/teacher/TeacherPosts.vue'), meta: { title: '互动问答' } },
      { path: 'announcements', name: 'TeacherAnnouncements', component: () => import('@/views/teacher/TeacherAnnouncements.vue'), meta: { title: '公告管理' } },
      { path: 'notifications/:id', name: 'TeacherNotificationDetail', component: () => import('@/views/student/StudentNotificationDetail.vue'), meta: { title: '通知详情' } },
      { path: 'messages', name: 'TeacherMessages', component: () => import('@/views/Messages.vue'), meta: { title: '私信' } }
    ]
  },

  // ==================== 学生端 ====================
  {
    path: '/admin',
    redirect: '/admin/courses',
    component: () => import('@/views/teacher/TeacherLayout.vue'),
    meta: { role: 'ROLE_ADMIN' },
    children: [
      { path: 'courses', name: 'AdminCourses', component: () => import('@/views/admin/AdminCourses.vue'), meta: { title: '课程总览' } },
      { path: 'grades', name: 'AdminGrades', component: () => import('@/views/admin/AdminGrades.vue'), meta: { title: '成绩概览' } },
      { path: 'surveys', name: 'AdminSurveys', component: () => import('@/views/admin/AdminSurveys.vue'), meta: { title: '问卷统计' } }
    ]
  },

  {
    path: '/student',
    redirect: '/student/dashboard',
    component: () => import('@/views/student/StudentLayout.vue'),
    meta: { role: 'ROLE_STUDENT' },
    children: [
      { path: 'dashboard', name: 'StudentDashboard', component: () => import('@/views/student/StudentDashboard.vue'), meta: { title: '学生仪表盘' } },
      { path: 'courses', name: 'StudentCourses', component: () => import('@/views/student/StudentCourses.vue'), meta: { title: '课程中心' } },
      { path: 'courses/:id', name: 'StudentCourseDetail', component: () => import('@/views/student/StudentCourseDetail.vue'), meta: { title: '课程详情' } },
      { path: 'courses/:id/learn', name: 'StudentLearn', component: () => import('@/views/student/StudentLearn.vue'), meta: { title: '在线学习' } },
      { path: 'assignments', name: 'StudentAssignments', component: () => import('@/views/student/StudentAssignments.vue'), meta: { title: '我的作业' } },
      { path: 'exams', name: 'StudentExams', component: () => import('@/views/student/StudentExams.vue'), meta: { title: '在线考试' } },
      { path: 'exams/:id', name: 'StudentExamTake', component: () => import('@/views/student/StudentExamTake.vue'), meta: { title: '考试作答' } },
      { path: 'grades', name: 'StudentGrades', component: () => import('@/views/student/StudentGrades.vue'), meta: { title: '我的成绩' } },
      { path: 'notes', name: 'StudentNotes', component: () => import('@/views/student/StudentNotes.vue'), meta: { title: '学习笔记' } },
      { path: 'trainings', name: 'StudentTrainings', component: () => import('@/views/student/StudentTrainings.vue'), meta: { title: '实训中心' } },
      { path: 'posts', name: 'StudentPosts', component: () => import('@/views/student/StudentPosts.vue'), meta: { title: '互动问答' } },
      { path: 'notifications/:id', name: 'StudentNotificationDetail', component: () => import('@/views/student/StudentNotificationDetail.vue'), meta: { title: '通知详情' } },
      { path: 'knowledge', name: 'StudentKnowledge', component: () => import('@/views/student/StudentKnowledge.vue'), meta: { title: '知识库' } },
      { path: 'evaluations', name: 'StudentEvaluations', component: () => import('@/views/student/StudentEvaluations.vue'), meta: { title: '教学评价' } },
      { path: 'surveys', name: 'StudentSurveys', component: () => import('@/views/student/StudentSurveys.vue'), meta: { title: '问卷调查' } },
      { path: 'announcements', name: 'StudentAnnouncements', component: () => import('@/views/student/StudentAnnouncements.vue'), meta: { title: '课程公告' } },
      { path: 'messages', name: 'StudentMessages', component: () => import('@/views/Messages.vue'), meta: { title: '私信' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title || '课畅'

  const userStore = useUserStore()
  const roleHome = () => {
    if (userStore.roleCode === 'ROLE_ADMIN') return '/admin/courses'
    if (userStore.roleCode === 'ROLE_TEACHER') return '/teacher/dashboard'
    if (userStore.roleCode === 'ROLE_STUDENT') return '/student/dashboard'
    return '/dashboard'
  }
  const requiredRole = to.meta.role || to.matched.find(route => route.meta.role)?.meta?.role

  // 访问根路径 / ：已登录→仪表盘，未登录→登录页
  if (to.path === '/') {
    next(userStore.token ? roleHome() : '/login')
    return
  }

  if (to.path === '/dashboard' && userStore.token && userStore.roleCode === 'ROLE_ADMIN') {
    next('/admin/courses')
    return
  }

  if (to.meta.requiresAuth && !userStore.token) {
    next('/login')
    return
  }

  if (to.path === '/login' && userStore.token) {
    next(roleHome())
    return
  }

  if (requiredRole) {
    if (!userStore.token) {
      next('/login')
      return
    }
    if (userStore.roleCode !== requiredRole) {
      next(roleHome())
      return
    }
  }

  next()
})

export default router
