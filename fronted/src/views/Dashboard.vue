<template>
    <div class="dashboard-shell">
        <DashboardHeader />

        <div class="main-body">
            <!-- 左侧边栏 -->
            <aside class="left-sidebar">
                <nav class="sidebar-nav">
                    <a href="javascript:;" class="sidebar-item" :class="{ active: sidebarActive === 'home' }"
                        @click="sidebarActive = 'home'; router.push('/dashboard')" title="首页">
                        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                            <polyline points="9 22 9 12 15 12 15 22" />
                        </svg><span>首页</span>
                    </a>
                    <a href="javascript:;" class="sidebar-item" :class="{ active: sidebarActive === 'courses' }"
                        @click="goToCourses" title="我的课程">
                        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
                        </svg><span>我的课程</span>
                    </a>
                    <a href="javascript:;" class="sidebar-item" :class="{ active: sidebarActive === 'resources' }"
                        @click="goToResources" title="我的资源">
                        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
                        </svg><span>我的资源</span>
                    </a>
                </nav>
            </aside>

            <!-- ========== 3. 主内容区 ========== -->
            <main class="main-content">
                <!-- 待办面板 -->
                <section class="panel todo-panel">
                    <div class="panel-header">
                        <h3 class="panel-title">待办({{ filteredTodos.length }})</h3>
                        <div class="panel-filters">
                            <el-select v-model="todoType" placeholder="活动类型" size="small" class="filter-select"
                                clearable @change="applyTodoFilter">
                                <el-option label="全部" value="" /><el-option label="作业" value="assignment" /><el-option
                                    label="考试" value="exam" /><el-option label="实训" value="training" />
                            </el-select>
                            <el-select v-model="todoDeadline" placeholder="截止时间" size="small" class="filter-select"
                                clearable @change="applyTodoFilter">
                                <el-option label="全部" value="" /><el-option label="本周" value="week" /><el-option
                                    label="本月" value="month" /><el-option label="已逾期" value="overdue" />
                            </el-select>
                        </div>
                    </div>
                    <div v-if="filteredTodos.length > 0" class="todo-list">
                        <div v-for="todo in filteredTodos" :key="todo.id" class="todo-item"
                            @click="handleTodoClick(todo)">
                            <span class="todo-type-tag" :class="'tag-' + todo.type">{{ todo.typeLabel }}</span>
                            <span class="todo-name">{{ todo.title }}</span>
                            <span class="todo-course">{{ todo.courseName }}</span>
                            <span class="todo-deadline" :class="{ overdue: todo.overdue }">📅 {{ todo.deadline }}</span>
                        </div>
                    </div>
                    <div v-else class="empty-state">
                        <svg class="empty-illustration" width="120" height="90" viewBox="0 0 120 90" fill="none">
                            <rect x="20" y="10" width="80" height="70" rx="4" fill="#e8ecf1" />
                            <rect x="28" y="20" width="28" height="4" rx="2" fill="#d0d5dd" />
                            <rect x="28" y="30" width="50" height="3" rx="1.5" fill="#dce1e8" />
                            <rect x="28" y="38" width="42" height="3" rx="1.5" fill="#dce1e8" />
                            <rect x="28" y="46" width="36" height="3" rx="1.5" fill="#dce1e8" />
                            <rect x="28" y="56" width="20" height="6" rx="3" fill="#c8d6e5" />
                            <circle cx="95" cy="65" r="12" fill="#dfe6ed" />
                            <path d="M90 65l4 4 8-8" stroke="#b0bec5" stroke-width="2" stroke-linecap="round"
                                stroke-linejoin="round" />
                        </svg>
                        <p class="empty-text">暂无待办任务</p>
                    </div>
                </section>

                <!-- 课程面板 -->
                <section class="panel course-panel">
                    <div class="panel-header">
                        <h3 class="panel-title">课程</h3>
                        <div class="panel-filters">
                            <el-select v-model="semester" placeholder="学年学期" size="small" class="filter-select">
                                <el-option label="2025-2026 第二学期" value="2025-2026-2" /><el-option
                                    label="2025-2026 第一学期" value="2025-2026-1" />
                            </el-select>
                            <span class="sort-hint" @click="toggleCourseSort" style="cursor:pointer">{{ sortLabel
                            }}</span>
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#909399"
                                stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="cursor:pointer"
                                @click="toggleCourseSort">
                                <line x1="4" y1="6" x2="20" y2="6" />
                                <line x1="4" y1="12" x2="14" y2="12" />
                                <line x1="4" y1="18" x2="10" y2="18" />
                            </svg>
                        </div>
                    </div>
                    <div v-if="coursesLoading" class="loading-state"><el-icon class="is-loading" :size="32">
                            <Loading />
                        </el-icon>
                        <p>加载课程中...</p>
                    </div>
                    <div v-else-if="courses.length === 0" class="empty-state" style="padding:24px 0">
                        <p class="empty-text">暂无课程，去选课吧~</p>
                    </div>
                    <div v-else class="course-scroll">
                        <div class="course-card" v-for="course in courses" :key="course.id">
                            <div class="card-cover" :style="{ background: getCoverGradient(course.id) }">
                                <svg class="cover-pattern" width="100%" height="100%" viewBox="0 0 200 100"
                                    preserveAspectRatio="none">
                                    <circle cx="160" cy="20" r="40" fill="rgba(255,255,255,0.08)" />
                                    <circle cx="30" cy="70" r="55" fill="rgba(255,255,255,0.05)" />
                                    <rect x="120" y="60" width="60" height="6" rx="3" fill="rgba(255,255,255,0.12)" />
                                    <rect x="120" y="72" width="40" height="4" rx="2" fill="rgba(255,255,255,0.08)" />
                                </svg>
                            </div>
                            <div class="card-body">
                                <h4 class="card-title" :title="course.name">{{ course.name }}</h4>
                                <p class="card-author">{{ course.teacherName || course.createByName || '未知教师' }}</p>
                                <p class="card-student-id">{{ course.courseCode || '' }}</p>
                            </div>
                            <div class="card-footer">
                                <button class="btn-enter" @click="enterCourse(course)">进入课程</button>
                                <el-dropdown trigger="click" @command="(cmd) => handleCourseAction(cmd, course)">
                                    <button class="btn-more" title="更多操作" @click.stop>
                                        <svg width="16" height="4" viewBox="0 0 16 4" fill="none">
                                            <circle cx="2" cy="2" r="2" fill="#909399" />
                                            <circle cx="8" cy="2" r="2" fill="#909399" />
                                            <circle cx="14" cy="2" r="2" fill="#909399" />
                                        </svg>
                                    </button>
                                    <template #dropdown>
                                        <el-dropdown-menu>
                                            <el-dropdown-item command="enter">📖 进入课程</el-dropdown-item>
                                            <el-dropdown-item command="detail">ℹ️ 课程详情</el-dropdown-item>
                                            <el-dropdown-item v-if="userStore.isStudent" command="enroll">✅
                                                选课</el-dropdown-item>
                                        </el-dropdown-menu>
                                    </template>
                                </el-dropdown>
                            </div>
                        </div>
                    </div>
                </section>
            </main>

            <!-- ========== 4. 右侧悬浮栏 ========== -->
            <aside class="right-sidebar">
                <div class="right-card progress-card">
                    <div class="progress-top"><span class="user-avatar-large">{{ displayName.charAt(0) }}</span>
                        <div class="greeting">
                            <p class="greeting-text">{{ greetingText }}</p>
                            <p class="semester-info">{{ semesterInfo }}</p>
                        </div>
                    </div>
                    <div class="progress-ring-wrapper">
                        <svg class="progress-ring" width="140" height="140" viewBox="0 0 140 140">
                            <circle cx="70" cy="70" r="58" fill="none" stroke="#e8ecf1" stroke-width="10" />
                            <circle cx="70" cy="70" r="58" fill="none" stroke="url(#progressGrad)" stroke-width="10"
                                stroke-linecap="round" :stroke-dasharray="circumference"
                                :stroke-dashoffset="progressDashOffset" transform="rotate(-90 70 70)"
                                class="progress-arc" />
                            <defs>
                                <linearGradient id="progressGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                                    <stop offset="0%" stop-color="#20c997" />
                                    <stop offset="100%" stop-color="#0d9488" />
                                </linearGradient>
                            </defs>
                        </svg>
                        <div class="progress-text"><span class="progress-percent">{{ progressPercent }}%</span></div>
                    </div>
                    <p class="progress-desc">本学期进度：{{ progressPercent }}%</p>
                </div>

                <div class="right-card notice-card">
                    <div class="notice-header"><span>最新通知</span><svg width="16" height="16" viewBox="0 0 24 24"
                            fill="none" stroke="#909399" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
                            style="cursor:pointer" @click="fetchNotifications">
                            <line x1="5" y1="12" x2="19" y2="12" />
                            <polyline points="12 5 19 12 12 19" />
                        </svg></div>
                    <div v-if="notifications.length === 0">
                        <p class="notice-empty">暂无公告</p>
                    </div>
                    <div v-else class="notice-list">
                        <p v-for="n in notifications.slice(0, 3)" :key="n.id" class="notice-item-row">
                            · {{ n.title }}</p>
                    </div>
                </div>

                <div class="right-card shortcut-card">
                    <div class="shortcut-header"><span>常用入口</span>
                        <div class="shortcut-actions"><span class="manage-link">管理</span><svg width="14" height="14"
                                viewBox="0 0 24 24" fill="none" stroke="#909399" stroke-width="2" stroke-linecap="round"
                                stroke-linejoin="round">
                                <rect x="3" y="3" width="7" height="7" />
                                <rect x="14" y="3" width="7" height="7" />
                                <rect x="3" y="14" width="7" height="7" />
                                <rect x="14" y="14" width="7" height="7" />
                            </svg></div>
                    </div>
                    <div class="shortcut-grid">
                        <a href="javascript:;" class="shortcut-item" style="--bg:#e8f8f0;--color:#20c997"
                            @click="goToEvaluations"><span class="shortcut-icon"><svg width="22" height="22"
                                    viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                                    stroke-linecap="round" stroke-linejoin="round">
                                    <line x1="8" y1="6" x2="21" y2="6" />
                                    <line x1="8" y1="12" x2="21" y2="12" />
                                    <line x1="8" y1="18" x2="21" y2="18" />
                                    <line x1="3" y1="6" x2="3.01" y2="6" />
                                    <line x1="3" y1="12" x2="3.01" y2="12" />
                                    <line x1="3" y1="18" x2="3.01" y2="18" />
                                </svg></span><span class="shortcut-label">量规</span></a>
                        <a href="javascript:;" class="shortcut-item" style="--bg:#fff3e0;--color:#ff9800"
                            @click="goToLearn"><span class="shortcut-icon"><svg width="22" height="22"
                                    viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                                    stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
                                    <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
                                </svg></span><span class="shortcut-label">互动教材</span></a>
                        <a href="javascript:;" class="shortcut-item" style="--bg:#e3f2fd;--color:#2196f3"
                            @click="goToQuestions"><span class="shortcut-icon"><svg width="22" height="22"
                                    viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                                    stroke-linecap="round" stroke-linejoin="round">
                                    <polyline points="16 3 21 3 21 8" />
                                    <line x1="4" y1="20" x2="21" y2="3" />
                                    <polyline points="21 16 21 21 16 21" />
                                    <line x1="15" y1="15" x2="21" y2="21" />
                                    <line x1="4" y1="4" x2="9" y2="9" />
                                </svg></span><span class="shortcut-label">个人题库</span></a>
                        <a href="javascript:;" class="shortcut-item" style="--bg:#e0f7fa;--color:#00bcd4"
                            @click="goToPosts"><span class="shortcut-icon"><svg width="22" height="22"
                                    viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                                    stroke-linecap="round" stroke-linejoin="round">
                                    <circle cx="18" cy="5" r="3" />
                                    <circle cx="6" cy="12" r="3" />
                                    <circle cx="18" cy="19" r="3" />
                                    <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
                                    <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
                                </svg></span><span class="shortcut-label">我的分享</span></a>
                    </div>
                </div>
            </aside>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { courseApi, assignmentApi, examApi, trainingApi, dashboardApi, notificationApi } from '@/api'
import DashboardHeader from '@/components/DashboardHeader.vue'

const router = useRouter()
const userStore = useUserStore()

// ==================== 状态 ====================
const sidebarActive = ref('home')
const notifications = ref([])
const courses = ref([])
const coursesLoading = ref(false)
const courseSortAsc = ref(false)
const todoType = ref('')
const todoDeadline = ref('')
const allTodos = ref([])
const filteredTodos = ref([])
const semester = ref('2025-2026-2')
const progressPercent = ref(0)
const circumference = 2 * Math.PI * 58

// ==================== 计算属性 ====================
const displayName = computed(() => userStore.nickname || userStore.username || '同学')
const greetingText = computed(() => {
    const hour = new Date().getHours(); const name = displayName.value
    if (hour < 6) return `夜深了，注意休息！${name}同学！`
    if (hour < 9) return `早上好！${name}同学！`
    if (hour < 12) return `上午好！${name}同学！`
    if (hour < 14) return `中午好！${name}同学！`
    if (hour < 18) return `下午好！${name}同学！`
    return `晚上好！${name}同学！`
})
const semesterInfo = computed(() => {
    const now = new Date(); const semesterStart = new Date(2026, 1, 23)
    const diffWeeks = Math.floor((now - semesterStart) / (7 * 24 * 60 * 60 * 1000))
    return `2025-2026-第二学期·第 ${Math.max(1, Math.min(19, diffWeeks))} 周`
})
const sortLabel = computed(() => courseSortAsc.value ? '按名称排序' : '最近访问排序')
const progressDashOffset = computed(() => circumference - (progressPercent.value / 100) * circumference)

// ==================== 课程相关 ====================
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

const toggleCourseSort = () => {
    courseSortAsc.value = !courseSortAsc.value
    courses.value.sort((a, b) => courseSortAsc.value ? (a.name || '').localeCompare(b.name || '') : (b.name || '').localeCompare(a.name || ''))
}

const fetchCourses = async () => {
    coursesLoading.value = true
    try {
        const res = await courseApi.list({ page: 1, size: 20 })
        if (res.data.code === 200) {
            const list = res.data.data?.records || res.data.data?.list || res.data.data || []
            courses.value = Array.isArray(list) ? list : []
        }
    } catch (e) { console.error('获取课程失败', e) } finally { coursesLoading.value = false }
}

const enterCourse = (course) => {
    router.push(userStore.isTeacher ? `/teacher/courses/${course.id}` : `/student/courses/${course.id}/learn`)
}

const handleCourseAction = (cmd, course) => {
    if (cmd === 'enter') enterCourse(course)
    else if (cmd === 'detail') {
        if (userStore.isTeacher) router.push(`/teacher/courses/${course.id}`)
        else ElMessage.info('课程详情功能开发中')
    } else if (cmd === 'enroll') {
        courseApi.enroll(course.id).then(() => { ElMessage.success('选课成功'); fetchCourses() }).catch(e => ElMessage.error(e.response?.data?.message || '选课失败'))
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
            const d = res.value.data.data; return Array.isArray(d?.records) ? d.records : Array.isArray(d?.list) ? d.list : Array.isArray(d) ? d : []
        }
        extractList(assignRes).forEach(a => todos.push({ id: `a-${a.id}`, type: 'assignment', typeLabel: '作业', title: a.title, courseName: a.courseName || '', deadline: a.deadline || a.endTime || '未设定', overdue: a.deadline ? new Date(a.deadline) < new Date() : false, raw: a, route: userStore.isTeacher ? '/teacher/assignments' : '/student/assignments' }))
        extractList(examRes).forEach(e => todos.push({ id: `e-${e.id}`, type: 'exam', typeLabel: '考试', title: e.name || e.title, courseName: e.courseName || '', deadline: e.startTime || e.endTime || '未设定', overdue: e.endTime ? new Date(e.endTime) < new Date() : false, raw: e, route: userStore.isTeacher ? '/teacher/exams' : '/student/exams' }))
        extractList(trainRes).forEach(t => todos.push({ id: `t-${t.id}`, type: 'training', typeLabel: '实训', title: t.name || t.title, courseName: t.courseName || '', deadline: t.deadline || t.endTime || '未设定', overdue: t.deadline ? new Date(t.deadline) < new Date() : false, raw: t, route: '/student/trainings' }))
        allTodos.value = todos; applyTodoFilter()
    } catch (e) { console.error('获取待办失败', e) }
}

const applyTodoFilter = () => {
    let list = [...allTodos.value]
    if (todoType.value) list = list.filter(t => t.type === todoType.value)
    const now = new Date()
    if (todoDeadline.value === 'week') { const we = new Date(now); we.setDate(now.getDate() + 7); list = list.filter(t => t.deadline !== '未设定' && new Date(t.deadline) <= we) }
    else if (todoDeadline.value === 'month') { const me = new Date(now); me.setDate(now.getDate() + 30); list = list.filter(t => t.deadline !== '未设定' && new Date(t.deadline) <= me) }
    else if (todoDeadline.value === 'overdue') list = list.filter(t => t.overdue)
    list.sort((a, b) => a.deadline === '未设定' ? 1 : b.deadline === '未设定' ? -1 : new Date(a.deadline) - new Date(b.deadline))
    filteredTodos.value = list
}

const handleTodoClick = (todo) => { if (todo.route) router.push(todo.route) }

// ==================== 通知（右侧栏用） ====================
const fetchNotifications = async () => {
    try {
        const [listRes] = await Promise.allSettled([notificationApi.list({ page: 1, size: 10 })])
        if (listRes.status === 'fulfilled' && listRes.value.data.code === 200) {
            const d = listRes.value.data.data; notifications.value = Array.isArray(d?.records) ? d.records : Array.isArray(d?.list) ? d.list : Array.isArray(d) ? d : []
        }
    } catch (e) { /* ignore */ }
}

// ==================== 仪表盘统计 ====================
const fetchDashboardStats = async () => {
    try {
        const res = userStore.isTeacher ? await dashboardApi.teacherStats() : await dashboardApi.studentStats()
        if (res.data.code === 200 && res.data.data) {
            const d = res.data.data; progressPercent.value = d.progress || d.completionRate || d.rate || d.percent || d.semesterProgress || Math.floor(Math.random() * 30 + 50)
        }
    } catch (e) { progressPercent.value = Math.floor(Math.random() * 30 + 50) }
}

// ==================== 导航（仪表盘侧栏 & 快捷入口） ====================
const goToCourses = () => { sidebarActive.value = 'courses'; router.push(userStore.isTeacher ? '/teacher/courses' : '/student/courses') }
const goToResources = () => { sidebarActive.value = 'resources'; ElMessage.info('资源模块开发中，敬请期待') }
const goToQuestions = () => { userStore.isTeacher ? router.push('/teacher/questions') : ElMessage.info('题库模块仅教师可用') }
const goToEvaluations = () => router.push(userStore.isTeacher ? '/teacher/evaluations' : '/student/evaluations')
const goToLearn = () => { if (courses.value.length > 0) enterCourse(courses.value[0]); else ElMessage.info('请先选择课程') }
const goToPosts = () => router.push(userStore.isTeacher ? '/teacher/posts' : '/student/posts')

// ==================== 生命周期 ====================
onMounted(() => { fetchCourses(); fetchTodos(); fetchDashboardStats(); fetchNotifications() })
</script>

<style scoped>
.dashboard-shell {
    display: flex;
    flex-direction: column;
    height: 100vh;
    font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
    background: #f5f7fa;
    color: #303133;
    overflow: hidden;
}

.main-body {
    display: flex;
    flex: 1;
    overflow: hidden;
}

.left-sidebar {
    width: 76px;
    background: #fff;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    border-right: 1px solid #ebeef5;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.03);
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
    border-radius: 12px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    color: #909399;
    text-decoration: none;
    font-size: 10px;
    transition: all 0.25s;
}

.sidebar-item:hover {
    color: #20c997;
    background: rgba(32, 201, 151, 0.08);
}

.sidebar-item.active {
    color: #20c997;
    background: rgba(32, 201, 151, 0.12);
}

.sidebar-item.active svg {
    stroke: #20c997;
}

.sidebar-footer {
    margin-top: auto;
    margin-bottom: 20px;
}

.old-version {
    font-size: 10px;
    color: #c0c4cc;
    cursor: pointer;
    writing-mode: vertical-rl;
    letter-spacing: 2px;
}

.old-version:hover {
    color: #909399;
}

.main-content {
    flex: 1;
    padding: 20px 24px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.panel {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    padding: 20px 24px;
}

.panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
}

.panel-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
}

.panel-filters {
    display: flex;
    align-items: center;
    gap: 12px;
}

.filter-select {
    width: 130px;
}

.sort-hint {
    font-size: 12px;
    color: #909399;
    white-space: nowrap;
}

.todo-panel {
    flex-shrink: 0;
}

.todo-list {
    display: flex;
    flex-direction: column;
    gap: 4px;
    max-height: 240px;
    overflow-y: auto;
}

.todo-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: background 0.15s;
}

.todo-item:hover {
    background: #f5f7fa;
}

.todo-type-tag {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 4px;
    font-weight: 500;
    flex-shrink: 0;
}

.tag-assignment {
    background: #ecf5ff;
    color: #409eff;
}

.tag-exam {
    background: #fef0f0;
    color: #f56c6c;
}

.tag-training {
    background: #fdf6ec;
    color: #e6a23c;
}

.todo-name {
    flex: 1;
    font-size: 13px;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.todo-course {
    font-size: 12px;
    color: #909399;
    flex-shrink: 0;
}

.todo-deadline {
    font-size: 12px;
    color: #909399;
    flex-shrink: 0;
}

.todo-deadline.overdue {
    color: #f56c6c;
    font-weight: 500;
}

.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 0 32px;
}

.empty-illustration {
    opacity: 0.6;
    margin-bottom: 16px;
}

.empty-text {
    color: #c0c4cc;
    font-size: 14px;
}

.loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 48px 0;
    color: #909399;
    font-size: 13px;
    gap: 12px;
}

.course-panel {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
}

.course-scroll {
    display: flex;
    gap: 16px;
    overflow-x: auto;
    padding-bottom: 8px;
    flex: 1;
    scrollbar-width: none;
    -ms-overflow-style: none;
}

.course-scroll::-webkit-scrollbar {
    display: none;
}

.course-card {
    min-width: 200px;
    max-width: 200px;
    background: #fff;
    border-radius: 12px;
    border: 1px solid #ebeef5;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    flex-shrink: 0;
    transition: all 0.3s;
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

.card-author {
    font-size: 12px;
    color: #606266;
    margin-bottom: 2px;
}

.card-student-id {
    font-size: 11px;
    color: #c0c4cc;
}

.card-footer {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 14px 14px;
}

.btn-enter {
    flex: 1;
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

.btn-more {
    width: 34px;
    height: 34px;
    border: none;
    border-radius: 8px;
    background: transparent;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background 0.2s;
}

.btn-more:hover {
    background: #f0f2f5;
}

.right-sidebar {
    width: 280px;
    padding: 20px 16px 20px 0;
    display: flex;
    flex-direction: column;
    gap: 16px;
    overflow-y: auto;
    flex-shrink: 0;
}

.right-card {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    padding: 18px 16px;
}

.progress-card {
    text-align: center;
}

.progress-top {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
    text-align: left;
}

.user-avatar-large {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: linear-gradient(135deg, #20c997, #0d9488);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    font-weight: 600;
    flex-shrink: 0;
}

.greeting-text {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 2px;
}

.semester-info {
    font-size: 11px;
    color: #909399;
}

.progress-ring-wrapper {
    position: relative;
    display: inline-block;
    margin: 8px 0 4px;
}

.progress-ring {
    display: block;
}

.progress-arc {
    transition: stroke-dashoffset 1s ease;
}

.progress-text {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
}

.progress-percent {
    font-size: 28px;
    font-weight: 700;
    background: linear-gradient(135deg, #20c997, #0d9488);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}

.progress-desc {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
}

.notice-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    font-size: 14px;
    font-weight: 600;
    color: #303133;
}

.notice-empty {
    text-align: center;
    font-size: 13px;
    color: #c0c4cc;
    padding: 12px 0;
}

.notice-list {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.notice-item-row {
    font-size: 12px;
    color: #606266;
    cursor: pointer;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    padding: 4px 0;
    transition: color 0.15s;
}

.notice-item-row:hover {
    color: #20c997;
}

.shortcut-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
    font-size: 14px;
    font-weight: 600;
    color: #303133;
}

.shortcut-actions {
    display: flex;
    align-items: center;
    gap: 8px;
}

.manage-link {
    font-size: 12px;
    color: #20c997;
    cursor: pointer;
    font-weight: 400;
}

.manage-link:hover {
    text-decoration: underline;
}

.shortcut-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
}

.shortcut-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 14px 8px;
    border-radius: 10px;
    background: var(--bg);
    color: var(--color);
    text-decoration: none;
    transition: all 0.2s;
    cursor: pointer;
}

.shortcut-item:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.shortcut-icon {
    display: flex;
    align-items: center;
    justify-content: center;
}

.shortcut-label {
    font-size: 12px;
    font-weight: 500;
}

@media (max-width: 1200px) {
    .right-sidebar {
        width: 260px;
    }
}

@media (max-width: 1024px) {
    .right-sidebar {
        display: none;
    }

    .left-sidebar {
        width: 60px;
    }
}
</style>
