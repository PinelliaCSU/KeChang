<template>
    <div class="dashboard-shell">
        <DashboardHeader />

        <div class="main-body">
            <!-- 左侧边栏 -->
            <aside class="left-sidebar">
                <nav class="sidebar-nav">
                    <a href="javascript:;" class="sidebar-item" :class="{ active: sidebarActive === 'home' }"
                        @click="sidebarActive = 'home'; router.push('/dashboard')" title="首页">
                        <el-icon :size="22">
                            <HomeFilled />
                        </el-icon><span>首页</span>
                    </a>
                    <a href="javascript:;" class="sidebar-item" :class="{ active: sidebarActive === 'courses' }"
                        @click="goToCourses" title="我的课程">
                        <el-icon :size="22">
                            <Reading />
                        </el-icon><span>我的课程</span>
                    </a>
                    <a href="javascript:;" class="sidebar-item" :class="{ active: sidebarActive === 'resources' }"
                        @click="goToResources" title="我的资源">
                        <el-icon :size="22">
                            <Folder />
                        </el-icon><span>我的资源</span>
                    </a>
                </nav>
            </aside>

            <!-- 主内容区 -->
            <main class="main-content">
                <DashboardTodoPanel :todos="allTodos" @todo-click="handleTodoClick" />
                <DashboardCoursePanel :courses="courses" :loading="coursesLoading" :is-teacher="userStore.isTeacher"
                    :empty-text="userStore.isTeacher ? '暂无课程，去创建课程吧~' : '暂无课程，去选课吧~'" @enter-course="enterCourse"
                    @course-action="handleCourseAction" />
            </main>

            <!-- 右侧栏 -->
            <DashboardRightSidebar :display-name="displayName" :role-suffix="roleSuffix"
                :progress-percent="progressPercent" :notifications="notifications" @go-to="handleShortcut"
                @refresh-notices="fetchNotifications" />
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { courseApi, assignmentApi, examApi, trainingApi, dashboardApi, notificationApi } from '@/api'
import DashboardHeader from '@/components/DashboardHeader.vue'
import DashboardTodoPanel from '@/components/DashboardTodoPanel.vue'
import DashboardCoursePanel from '@/components/DashboardCoursePanel.vue'
import DashboardRightSidebar from '@/components/DashboardRightSidebar.vue'

const router = useRouter()
const userStore = useUserStore()

// ==================== 状态 ====================
const sidebarActive = ref('home')
const notifications = ref([])
const courses = ref([])
const coursesLoading = ref(false)
const allTodos = ref([])
const progressPercent = ref(0)

// ==================== 计算属性 ====================
const displayName = computed(() => userStore.nickname || userStore.username || (userStore.isTeacher ? '老师' : '同学'))
const roleSuffix = computed(() => userStore.isTeacher ? '老师' : '同学')

// ==================== 课程相关 ====================
const fetchCourses = async () => {
    coursesLoading.value = true
    try {
        const res = await courseApi.list({
            page: 1, size: 20,
            onlyEnrolled: userStore.isStudent ? true : undefined
        })
        if (res.data.code === 200) {
            const list = res.data.data?.records || res.data.data?.list || res.data.data || []
            courses.value = Array.isArray(list)
                ? list.map(course => ({ ...course, name: course.courseName || course.name }))
                : []
        }
    } catch (e) { console.error('获取课程失败', e) } finally { coursesLoading.value = false }
}

const enterCourse = (course) => {
    if (userStore.isStudent && !course.enrolled) {
        ElMessage.warning('请先在课程中心加入课程')
        return
    }
    router.push(userStore.isTeacher ? `/teacher/courses/${course.id}` : `/student/courses/${course.id}/learn`)
}

const handleCourseAction = (cmd, course) => {
    if (cmd === 'enter') enterCourse(course)
    else if (cmd === 'detail') {
        if (userStore.isTeacher) router.push(`/teacher/courses/${course.id}`)
        else router.push(`/student/courses/${course.id}`)
    } else if (cmd === 'edit') {
        router.push(`/teacher/courses/${course.id}`)
    } else if (cmd === 'enroll') {
        if (course.enrolled) { ElMessage.info('已选该课程'); return }
        courseApi.enroll(course.id).then(() => {
            course.enrolled = true
            course.studentCount = Number(course.studentCount || 0) + 1
            ElMessage.success('选课成功')
        }).catch(e => ElMessage.error(e.response?.data?.message || '选课失败'))
    }
}

// ==================== 待办相关 ====================
const fetchTodos = async () => {
    try {
        const [assignRes, examRes, trainRes] = await Promise.allSettled([
            assignmentApi.list({ page: 1, size: 50 }),
            examApi.listExams({ page: 1, size: 50 }),
            trainingApi.listProjects({ page: 1, size: 50 })
        ])
        const todos = []
        const extractList = (res) => {
            if (res.status !== 'fulfilled' || res.value.data.code !== 200) return []
            const d = res.value.data.data
            return Array.isArray(d?.records) ? d.records : Array.isArray(d?.list) ? d.list : Array.isArray(d) ? d : []
        }
        extractList(assignRes).forEach(a => todos.push({
            id: `a-${a.id}`, type: 'assignment', typeLabel: '作业', title: a.title,
            courseName: a.courseName || '', deadline: a.deadline || a.endTime || '未设定',
            overdue: a.deadline ? new Date(a.deadline) < new Date() : false,
            raw: a, route: userStore.isTeacher ? '/teacher/assignments' : '/student/assignments'
        }))
        extractList(examRes).forEach(e => todos.push({
            id: `e-${e.id}`, type: 'exam', typeLabel: '考试', title: e.name || e.title,
            courseName: e.courseName || '', deadline: e.startTime || e.endTime || '未设定',
            overdue: e.endTime ? new Date(e.endTime) < new Date() : false,
            raw: e, route: userStore.isTeacher ? '/teacher/exams' : '/student/exams'
        }))
        extractList(trainRes).forEach(t => todos.push({
            id: `t-${t.id}`, type: 'training', typeLabel: '实训', title: t.name || t.title,
            courseName: t.courseName || '', deadline: t.deadline || t.endTime || '未设定',
            overdue: t.deadline ? new Date(t.deadline) < new Date() : false,
            raw: t, route: '/student/trainings'
        }))
        allTodos.value = todos
    } catch (e) { console.error('获取待办失败', e) }
}

const handleTodoClick = (todo) => { if (todo.route) router.push(todo.route) }

// ==================== 通知 ====================
const fetchNotifications = async () => {
    try {
        const [listRes] = await Promise.allSettled([notificationApi.list({ page: 1, size: 10 })])
        if (listRes.status === 'fulfilled' && listRes.value.data.code === 200) {
            const d = listRes.value.data.data
            notifications.value = Array.isArray(d?.records) ? d.records : Array.isArray(d?.list) ? d.list : Array.isArray(d) ? d : []
        }
    } catch (e) { /* ignore */ }
}

// ==================== 仪表盘统计 ====================
const fetchDashboardStats = async () => {
    try {
        const res = userStore.isTeacher ? await dashboardApi.teacherStats() : await dashboardApi.studentStats()
        if (res.data.code === 200 && res.data.data) {
            const d = res.data.data
            progressPercent.value = d.progress ?? d.completionRate ?? d.rate ?? d.percent ?? d.semesterProgress ?? 0
        }
    } catch (e) { /* ignore */ }
}

// ==================== 导航 ====================
const goToCourses = () => { sidebarActive.value = 'courses'; router.push(userStore.isTeacher ? '/teacher/courses' : '/student/courses') }
const goToResources = () => { sidebarActive.value = 'resources'; ElMessage.info('资源模块开发中，敬请期待') }
const handleShortcut = (target) => {
    if (target === 'evaluations') router.push(userStore.isTeacher ? '/teacher/evaluations' : '/student/evaluations')
    else if (target === 'learn') {
        if (courses.value.length > 0) enterCourse(courses.value[0])
        else ElMessage.info('暂无课程')
    } else if (target === 'questions') {
        userStore.isTeacher ? router.push('/teacher/questions') : ElMessage.info('题库模块仅教师可用')
    } else if (target === 'posts') {
        router.push(userStore.isTeacher ? '/teacher/posts' : '/student/posts')
    }
}

onMounted(() => { fetchCourses(); fetchTodos(); fetchDashboardStats(); fetchNotifications() })
</script>

<style scoped>
.dashboard-shell {
    display: flex;
    flex-direction: column;
    height: 100vh;
    font-family: var(--bio-font);
    background: var(--bio-cream);
    color: var(--bio-ink);
    overflow: hidden;
}

.main-body {
    display: flex;
    flex: 1;
    overflow: hidden;
}

/* ========== 左侧快捷导航 ========== */
.left-sidebar {
    width: 76px;
    background: linear-gradient(180deg, #fdfbf7 0%, #f8f5ed 100%);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    border-right: 1px solid rgba(163, 177, 138, 0.12);
    box-shadow: 2px 0 12px rgba(44, 54, 30, 0.03);
}

.sidebar-nav {
    display: flex;
    flex-direction: column;
    gap: 16px;
    align-items: center;
}

.sidebar-item {
    width: 52px;
    height: 52px;
    border-radius: var(--bio-radius-md);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    color: var(--bio-ink-muted);
    text-decoration: none;
    font-size: 10px;
    transition: color var(--bio-dur-normal) var(--bio-ease-out),
        background var(--bio-dur-normal) var(--bio-ease-out);
}

.sidebar-item:hover {
    color: var(--bio-moss);
    background: rgba(163, 177, 138, 0.10);
    filter: brightness(1.05);
}

.sidebar-item.active {
    color: var(--bio-moss);
    background: linear-gradient(135deg, rgba(163, 177, 138, 0.16), rgba(163, 177, 138, 0.08));
    font-weight: 500;
}

.sidebar-item.active svg {
    stroke: var(--bio-moss);
}

/* ========== 主内容区 ========== */
.main-content {
    flex: 1;
    padding: 20px 24px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 20px;
    min-width: 0;
}

/* 待办面板和课程面板都占满宽度，上下排列 */
.main-content> :first-child
.main-content> :last-child {
    width: 100%;
}

@media (max-width: 1024px) {
    .left-sidebar {
        width: 60px;
    }
}
</style>
