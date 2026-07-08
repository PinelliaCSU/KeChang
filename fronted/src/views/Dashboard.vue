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
                            <span class="todo-deadline" :class="{ overdue: todo.overdue }">{{ todo.deadline }}</span>
                        </div>
                    </div>
                    <div v-else class="empty-state">
                        <EmptyIllustration variant="detailed" text="暂无待办任务" />
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
                            <el-icon :size="14" color="#909399" style="cursor:pointer" @click="toggleCourseSort">
                                <Sort />
                            </el-icon>
                        </div>
                    </div>
                    <div v-if="coursesLoading" class="loading-state"><el-icon class="is-loading" :size="32">
                            <Loading />
                        </el-icon>
                        <p>加载课程中...</p>
                    </div>
                    <div v-else-if="courses.length === 0" class="empty-state" style="padding:24px 0">
                        <p class="empty-text">{{ userStore.isTeacher ? '暂无课程，去创建课程吧~' : '暂无课程，去选课吧~' }}</p>
                    </div>
                    <div v-else class="course-scroll">
                        <div class="course-card" v-for="course in courses" :key="course.id">
                            <div class="card-cover" :style="{ background: getCoverGradient(course.id) }">
                                <CourseCoverPattern extended />
                            </div>
                            <div class="card-body">
                                <h4 class="card-title" :title="course.name">{{ course.name }}</h4>
                                <p class="card-author">{{ course.teacherName || course.createByName || '未知教师' }}</p>
                                <p class="card-student-id">{{ course.courseCode || '' }}</p>
                            </div>
                            <div class="card-footer">
                                <button class="btn-enter" @click="enterCourse(course)">{{ userStore.isTeacher ? '管理课程' :
                                    '进入课程' }}</button>
                                <el-dropdown trigger="click" @command="(cmd) => handleCourseAction(cmd, course)">
                                    <button class="btn-more" title="更多操作" @click.stop>
                                        <el-icon :size="16">
                                            <MoreFilled />
                                        </el-icon>
                                    </button>
                                    <template #dropdown>
                                        <el-dropdown-menu>
                                            <el-dropdown-item command="enter">
                                                {{ userStore.isTeacher ? '管理课程' : '进入课程' }}
                                            </el-dropdown-item>
                                            <el-dropdown-item command="detail">课程详情</el-dropdown-item>
                                            <el-dropdown-item v-if="userStore.isStudent" command="enroll"
                                                :disabled="course.enrolled">
                                                {{ course.enrolled ? '已选课' : '选课' }}
                                            </el-dropdown-item>
                                            <el-dropdown-item v-if="userStore.isTeacher"
                                                command="edit">编辑课程</el-dropdown-item>
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
                        <ProgressRing :percent="progressPercent" />
                    </div>
                    <p class="progress-desc">本学期进度：{{ progressPercent }}%</p>
                </div>

                <div class="right-card notice-card">
                    <div class="notice-header"><span>最新通知</span><el-icon :size="16" color="#909399"
                            style="cursor:pointer" @click="fetchNotifications">
                            <ArrowRight />
                        </el-icon></div>
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
                        <div class="shortcut-actions"><span class="manage-link">管理</span><el-icon :size="14"
                                color="#909399">
                                <Grid />
                            </el-icon></div>
                    </div>
                    <div class="shortcut-grid">
                        <a href="javascript:;" class="shortcut-item" style="--bg:#e8eddf;--color:#606c38"
                            @click="goToEvaluations"><span class="shortcut-icon"><el-icon :size="22">
                                    <List />
                                </el-icon></span><span class="shortcut-label">量规</span></a>
                        <a href="javascript:;" class="shortcut-item" style="--bg:#eddfd0;--color:#9c6644"
                            @click="goToLearn"><span class="shortcut-icon"><el-icon :size="22">
                                    <Notebook />
                                </el-icon></span><span class="shortcut-label">互动教材</span></a>
                        <a href="javascript:;" class="shortcut-item" style="--bg:#dce8f0;--color:#5c7a8a"
                            @click="goToQuestions"><span class="shortcut-icon"><el-icon :size="22">
                                    <Connection />
                                </el-icon></span><span class="shortcut-label">个人题库</span></a>
                        <a href="javascript:;" class="shortcut-item" style="--bg:#dde8d6;--color:#6b8e4e"
                            @click="goToPosts"><span class="shortcut-icon"><el-icon :size="22">
                                    <Share />
                                </el-icon></span><span class="shortcut-label">我的分享</span></a>
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
import { useUserStore } from '@/stores/user'
import { courseApi, assignmentApi, examApi, trainingApi, dashboardApi, notificationApi } from '@/api'
import DashboardHeader from '@/components/DashboardHeader.vue'
import EmptyIllustration from '@/components/EmptyIllustration.vue'
import CourseCoverPattern from '@/components/CourseCoverPattern.vue'
import ProgressRing from '@/components/ProgressRing.vue'

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

// ==================== 计算属性 ====================
const displayName = computed(() => userStore.nickname || userStore.username || (userStore.isTeacher ? '老师' : '同学'))
const roleSuffix = computed(() => userStore.isTeacher ? '老师' : '同学')
const greetingText = computed(() => {
    const hour = new Date().getHours(); const name = displayName.value; const suffix = roleSuffix.value
    if (hour < 6) return `夜深了，注意休息！${name}${suffix}！`
    if (hour < 9) return `早上好！${name}${suffix}！`
    if (hour < 12) return `上午好！${name}${suffix}！`
    if (hour < 14) return `中午好！${name}${suffix}！`
    if (hour < 18) return `下午好！${name}${suffix}！`
    return `晚上好！${name}${suffix}！`
})
const semesterInfo = computed(() => {
    const now = new Date(); const semesterStart = new Date(2026, 1, 23)
    const diffWeeks = Math.floor((now - semesterStart) / (7 * 24 * 60 * 60 * 1000))
    return `2025-2026-第二学期·第 ${Math.max(1, Math.min(19, diffWeeks))} 周`
})
const sortLabel = computed(() => courseSortAsc.value ? '按名称排序' : '最近访问排序')

// ==================== 课程相关 ====================
const gradientPalette = [
    'linear-gradient(135deg, #5a7040 0%, #6b8e4e 50%, #8cb369 100%)',
    'linear-gradient(135deg, #4a5c38 0%, #606c38 50%, #7f8c52 100%)',
    'linear-gradient(135deg, #3b5e4a 0%, #4c7a5e 50%, #6b9e7a 100%)',
    'linear-gradient(135deg, #5c4a3a 0%, #7a6048 50%, #9c7a5c 100%)',
    'linear-gradient(135deg, #4a5a6e 0%, #5c7a8a 50%, #7a9fb5 100%)',
    'linear-gradient(135deg, #6b5e4a 0%, #8a7a5c 50%, #a8986e 100%)',
    'linear-gradient(135deg, #4e5a3a 0%, #667a48 50%, #8c9e6a 100%)',
    'linear-gradient(135deg, #5a4a3e 0%, #7a6250 50%, #a08068 100%)'
]
const getCoverGradient = (id) => gradientPalette[(id - 1) % gradientPalette.length]

const toggleCourseSort = () => {
    courseSortAsc.value = !courseSortAsc.value
    courses.value.sort((a, b) => courseSortAsc.value ? (a.name || '').localeCompare(b.name || '') : (b.name || '').localeCompare(a.name || ''))
}

const fetchCourses = async () => {
    coursesLoading.value = true
    try {
        const res = await courseApi.list({
            page: 1,
            size: 20,
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
        else ElMessage.info('课程详情功能开发中')
    } else if (cmd === 'edit') {
        router.push(`/teacher/courses/${course.id}`)
    } else if (cmd === 'enroll') {
        if (course.enrolled) {
            ElMessage.info('已选该课程')
            return
        }
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
const goToLearn = () => { if (courses.value.length > 0) enterCourse(courses.value[0]); else ElMessage.info('暂无课程') }
const goToPosts = () => router.push(userStore.isTeacher ? '/teacher/posts' : '/student/posts')

// ==================== 生命周期 ====================
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

.sidebar-footer {
    margin-top: auto;
    margin-bottom: 20px;
}

.old-version {
    font-size: 10px;
    color: var(--bio-ink-faint);
    cursor: pointer;
    writing-mode: vertical-rl;
    letter-spacing: 2px;
    transition: color var(--bio-dur-fast) var(--bio-ease-out);
}

.old-version:hover {
    color: var(--bio-ink-muted);
}

.main-content {
    flex: 1;
    padding: 20px 24px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 20px;
}

/* ========== 面板通用 ========== */
.panel {
    background: var(--bio-paper);
    border-radius: var(--bio-radius-md);
    box-shadow: var(--bio-shadow-sm);
    padding: 20px 24px;
    border: 1px solid rgba(163, 177, 138, 0.08);
    transition: box-shadow var(--bio-dur-slow) var(--bio-ease-out);
}

.panel:hover {
    box-shadow: var(--bio-shadow-md);
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
    color: var(--bio-ink);
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
    color: var(--bio-ink-muted);
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
    border-radius: var(--bio-radius-sm);
    cursor: pointer;
    transition: background var(--bio-dur-fast) var(--bio-ease-out),
        filter var(--bio-dur-fast) var(--bio-ease-out);
}

.todo-item:hover {
    background: #f7f8f2;
    filter: brightness(1.02);
}

.todo-item:active {
    transform: translateY(1px);
}

.todo-type-tag {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 4px;
    font-weight: 500;
    flex-shrink: 0;
}

.tag-assignment {
    background: #e8eddf;
    color: var(--bio-moss);
}

.tag-exam {
    background: #f4e8e0;
    color: #c4665a;
}

.tag-training {
    background: #f0ebe0;
    color: var(--bio-earth-warm);
}

.todo-name {
    flex: 1;
    font-size: 13px;
    color: var(--bio-ink);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.todo-course {
    font-size: 12px;
    color: var(--bio-ink-muted);
    flex-shrink: 0;
}

.todo-deadline {
    font-size: 12px;
    color: var(--bio-ink-muted);
    flex-shrink: 0;
}

.todo-deadline.overdue {
    color: #c4665a;
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
    opacity: 0.5;
    margin-bottom: 16px;
}

.empty-text {
    color: var(--bio-ink-faint);
    font-size: 14px;
}

.loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 48px 0;
    color: var(--bio-ink-muted);
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
    background: var(--bio-paper);
    border-radius: var(--bio-radius-md);
    border: 1px solid rgba(163, 177, 138, 0.1);
    overflow: hidden;
    display: flex;
    flex-direction: column;
    flex-shrink: 0;
    transition: box-shadow var(--bio-dur-normal) var(--bio-ease-out),
        transform var(--bio-dur-normal) var(--bio-ease-out);
}

.course-card:hover {
    box-shadow: var(--bio-shadow-lg);
    transform: translateY(-3px);
}

.course-card:active {
    transform: translateY(0px);
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
    color: var(--bio-ink);
    margin-bottom: 6px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.card-author {
    font-size: 12px;
    color: var(--bio-ink-soft);
    margin-bottom: 2px;
}

.card-student-id {
    font-size: 11px;
    color: var(--bio-ink-faint);
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
    border-radius: var(--bio-radius-sm);
    background: linear-gradient(135deg, rgba(163, 177, 138, 0.15), rgba(163, 177, 138, 0.08));
    color: var(--bio-moss);
    font-size: 13px;
    cursor: pointer;
    transition: filter var(--bio-dur-fast) var(--bio-ease-out),
        transform var(--bio-dur-fast) var(--bio-ease-out);
    font-family: inherit;
}

.btn-enter:hover {
    filter: brightness(1.1);
}

.btn-enter:active {
    transform: translateY(1px);
}

.btn-more {
    width: 34px;
    height: 34px;
    border: none;
    border-radius: var(--bio-radius-sm);
    background: transparent;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background var(--bio-dur-fast) var(--bio-ease-out);
}

.btn-more:hover {
    background: rgba(163, 177, 138, 0.08);
}

/* ========== 右侧面板 ========== */
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
    background: var(--bio-paper);
    border-radius: var(--bio-radius-md);
    box-shadow: var(--bio-shadow-sm);
    padding: 18px 16px;
    border: 1px solid rgba(163, 177, 138, 0.08);
    transition: box-shadow var(--bio-dur-slow) var(--bio-ease-out);
}

.right-card:hover {
    box-shadow: var(--bio-shadow-md);
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
    background: linear-gradient(135deg, #8cb369, #6b8e4e);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    font-weight: 600;
    flex-shrink: 0;
    box-shadow: 0 2px 10px rgba(96, 108, 56, 0.15);
}

.greeting-text {
    font-size: 14px;
    font-weight: 600;
    color: var(--bio-ink);
    margin-bottom: 2px;
}

.semester-info {
    font-size: 11px;
    color: var(--bio-ink-muted);
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
    background: linear-gradient(135deg, var(--bio-moss), var(--bio-leaf));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}

.progress-desc {
    font-size: 12px;
    color: var(--bio-ink-muted);
    margin-top: 4px;
}

.notice-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    font-size: 14px;
    font-weight: 600;
    color: var(--bio-ink);
}

.notice-empty {
    text-align: center;
    font-size: 13px;
    color: var(--bio-ink-faint);
    padding: 12px 0;
}

.notice-list {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.notice-item-row {
    font-size: 12px;
    color: var(--bio-ink-soft);
    cursor: pointer;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    padding: 4px 0;
    transition: color var(--bio-dur-fast) var(--bio-ease-out);
}

.notice-item-row:hover {
    color: var(--bio-moss);
}

.shortcut-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
    font-size: 14px;
    font-weight: 600;
    color: var(--bio-ink);
}

.shortcut-actions {
    display: flex;
    align-items: center;
    gap: 8px;
}

.manage-link {
    font-size: 12px;
    color: var(--bio-moss);
    cursor: pointer;
    font-weight: 400;
    transition: filter var(--bio-dur-fast) var(--bio-ease-out);
}

.manage-link:hover {
    filter: brightness(1.2);
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
    border-radius: var(--bio-radius-md);
    background: var(--bg);
    color: var(--color);
    text-decoration: none;
    transition: filter var(--bio-dur-normal) var(--bio-ease-out),
        transform var(--bio-dur-fast) var(--bio-ease-out);
    cursor: pointer;
}

.shortcut-item:hover {
    filter: brightness(1.08);
    transform: translateY(-1px);
}

.shortcut-item:active {
    transform: translateY(0px);
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
