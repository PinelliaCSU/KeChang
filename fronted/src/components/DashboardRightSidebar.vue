<template>
    <aside class="right-sidebar">
        <div class="right-card progress-card">
            <div class="progress-top">
                <span class="user-avatar-large">{{ displayName.charAt(0) }}</span>
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
            <div class="notice-header">
                <span>最新通知</span>
                <el-icon :size="16" color="#909399" style="cursor:pointer" @click="$emit('refreshNotices')">
                    <ArrowRight />
                </el-icon>
            </div>
            <div v-if="notifications.length === 0">
                <p class="notice-empty">暂无公告</p>
            </div>
            <div v-else class="notice-list">
                <p v-for="n in notifications.slice(0, 3)" :key="n.id" class="notice-item-row">· {{ n.title }}</p>
            </div>
        </div>

        <div class="right-card shortcut-card">
            <div class="shortcut-header">
                <span>常用入口</span>
                <el-icon :size="14" color="#909399" style="cursor:pointer" @click="$emit('manage')">
                    <Grid />
                </el-icon>
            </div>
            <div v-if="visibleShortcuts.length === 0" class="shortcut-empty">
                暂无已显示的入口，<a href="javascript:;" @click="$emit('manage')" class="shortcut-set-link">去设置</a>
            </div>
            <div v-else class="shortcut-grid">
                <a v-for="item in visibleShortcuts.slice(0, 8)" :key="item.id" href="javascript:;" class="shortcut-item"
                    :style="{ '--bg': item.color + '22', '--color': item.color }" @click="goToRoute(item.route)">
                    <span class="shortcut-icon"><el-icon :size="20">
                            <component :is="item.icon" />
                        </el-icon></span>
                    <span class="shortcut-label">{{ item.label }}</span>
                </a>
            </div>
        </div>
    </aside>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import ProgressRing from '@/components/ProgressRing.vue'

const router = useRouter()
const userStore = useUserStore()

const props = defineProps({
    displayName: { type: String, default: '用户' },
    roleSuffix: { type: String, default: '同学' },
    progressPercent: { type: Number, default: 0 },
    notifications: { type: Array, default: () => [] }
})

defineEmits(['goTo', 'refreshNotices', 'manage'])

const shortcuts = ref([])

const STORAGE_KEY = 'kechang_shortcuts'

// 获取当前角色的默认入口
const getDefaultEntries = () => {
    const common = [
        { id: 'calendar', label: '日历', icon: 'Calendar', route: '/calendar', color: '#a5c4d4', visible: true },
        { id: 'profile', label: '个人中心', icon: 'User', route: '/profile', color: '#b08968', visible: true }
    ]
    let roleEntries = []
    if (userStore.isStudent) {
        roleEntries = [
            { id: 's-courses', label: '课程中心', icon: 'Reading', route: '/student/courses', color: '#7f8c52', visible: true },
            { id: 's-assignments', label: '我的作业', icon: 'Edit', route: '/student/assignments', color: '#b08968', visible: true },
            { id: 's-exams', label: '在线考试', icon: 'Timer', route: '/student/exams', color: '#c4665a', visible: true },
            { id: 's-grades', label: '我的成绩', icon: 'TrendCharts', route: '/student/grades', color: '#6b8e4e', visible: true },
            { id: 's-notes', label: '学习笔记', icon: 'Notebook', route: '/student/notes', color: '#5c7a8a', visible: true },
            { id: 's-trainings', label: '实训中心', icon: 'Tools', route: '/student/trainings', color: '#9c6644', visible: true },
            { id: 's-posts', label: '互动问答', icon: 'ChatDotRound', route: '/student/posts', color: '#7a9fb5', visible: true },
            { id: 's-knowledge', label: '知识库', icon: 'Collection', route: '/student/knowledge', color: '#606c38', visible: true },
            { id: 's-evaluations', label: '教学评价', icon: 'List', route: '/student/evaluations', color: '#c4956a', visible: true },
            { id: 's-surveys', label: '问卷调查', icon: 'Document', route: '/student/surveys', color: '#a3b18a', visible: true }
        ]
    } else if (userStore.isTeacher) {
        roleEntries = [
            { id: 't-courses', label: '课程管理', icon: 'Reading', route: '/teacher/courses', color: '#7f8c52', visible: true },
            { id: 't-questions', label: '试题管理', icon: 'Connection', route: '/teacher/questions', color: '#5c7a8a', visible: true },
            { id: 't-papers', label: '试卷管理', icon: 'DocumentCopy', route: '/teacher/papers', color: '#b08968', visible: true },
            { id: 't-exams', label: '考试管理', icon: 'Timer', route: '/teacher/exams', color: '#c4665a', visible: true },
            { id: 't-assignments', label: '作业管理', icon: 'Edit', route: '/teacher/assignments', color: '#9c6644', visible: true },
            { id: 't-grades', label: '成绩管理', icon: 'TrendCharts', route: '/teacher/grades', color: '#6b8e4e', visible: true },
            { id: 't-trainings', label: '实训管理', icon: 'Tools', route: '/teacher/trainings', color: '#606c38', visible: true },
            { id: 't-surveys', label: '问卷管理', icon: 'Document', route: '/teacher/surveys', color: '#a3b18a', visible: true },
            { id: 't-evaluations', label: '教学评价', icon: 'List', route: '/teacher/evaluations', color: '#c4956a', visible: true },
            { id: 't-posts', label: '互动问答', icon: 'ChatDotRound', route: '/teacher/posts', color: '#7a9fb5', visible: true }
        ]
    } else if (userStore.isAdmin) {
        roleEntries = [
            { id: 'a-courses', label: '课程总览', icon: 'Reading', route: '/admin/courses', color: '#7f8c52', visible: true },
            { id: 'a-grades', label: '成绩概览', icon: 'TrendCharts', route: '/admin/grades', color: '#6b8e4e', visible: true },
            { id: 'a-surveys', label: '问卷统计', icon: 'Document', route: '/admin/surveys', color: '#a3b18a', visible: true }
        ]
    }
    return [...common, ...roleEntries]
}

const loadShortcuts = () => {
    try {
        const saved = localStorage.getItem(STORAGE_KEY)
        if (saved) {
            const parsed = JSON.parse(saved)
            if (parsed.role === userStore.roleCode && Array.isArray(parsed.entries)) {
                shortcuts.value = parsed.entries
                return
            }
        }
    } catch (e) { /* ignore */ }
    // 无缓存数据时使用默认入口，确保首次打开就有显示
    shortcuts.value = getDefaultEntries()
}

const visibleShortcuts = computed(() => shortcuts.value.filter(s => s.visible))

const goToRoute = (route) => {
    if (route) router.push(route)
}

onMounted(() => {
    loadShortcuts()
    // 监听 localStorage 变化（跨标签页）
    window.addEventListener('storage', (e) => {
        if (e.key === STORAGE_KEY) loadShortcuts()
    })
    // 监听自定义事件（同一标签页内，管理对话框保存时触发）
    window.addEventListener('shortcuts-changed', loadShortcuts)
})

const greetingText = computed(() => {
    const hour = new Date().getHours()
    const name = props.displayName
    const suffix = props.roleSuffix
    if (hour < 6) return `夜深了，注意休息！${name}${suffix}！`
    if (hour < 9) return `早上好！${name}${suffix}！`
    if (hour < 12) return `上午好！${name}${suffix}！`
    if (hour < 14) return `中午好！${name}${suffix}！`
    if (hour < 18) return `下午好！${name}${suffix}！`
    return `晚上好！${name}${suffix}！`
})

const semesterInfo = computed(() => {
    const now = new Date()
    const semesterStart = new Date(2026, 1, 23)
    const diffWeeks = Math.floor((now - semesterStart) / (7 * 24 * 60 * 60 * 1000))
    return `2025-2026-第二学期·第 ${Math.max(1, Math.min(19, diffWeeks))} 周`
})
</script>

<style scoped>
.right-sidebar {
    width: 280px;
    flex-shrink: 0;
    padding: 20px 12px 20px 0;
    display: flex;
    flex-direction: column;
    gap: 16px;
    overflow-y: auto;
}

.right-card {
    background: var(--bio-paper);
    border-radius: var(--bio-radius-md);
    box-shadow: var(--bio-shadow-sm);
    padding: 20px;
    border: 1px solid rgba(163, 177, 138, 0.08);
    transition: box-shadow var(--bio-dur-slow) var(--bio-ease-out);
}

.right-card:hover {
    box-shadow: var(--bio-shadow-md);
}

.progress-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
}

.progress-top {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
    width: 100%;
}

.user-avatar-large {
    width: 46px;
    height: 46px;
    border-radius: 50%;
    background: linear-gradient(135deg, #a3b18a, #7f8c52);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    font-weight: 600;
    flex-shrink: 0;
}

.greeting {
    flex: 1;
    text-align: left;
}

.greeting-text {
    font-size: 14px;
    font-weight: 500;
    color: var(--bio-ink);
    margin: 0;
}

.semester-info {
    font-size: 11px;
    color: var(--bio-ink-faint);
    margin: 4px 0 0;
}

.progress-ring-wrapper {
    display: flex;
    justify-content: center;
    margin: 8px 0;
}

.progress-desc {
    font-size: 13px;
    color: var(--bio-ink-muted);
    margin: 0;
}

.notice-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 14px;
    font-weight: 600;
    color: var(--bio-ink);
    margin-bottom: 12px;
}

.notice-empty {
    color: var(--bio-ink-faint);
    font-size: 13px;
    text-align: center;
    padding: 16px 0;
}

.notice-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.notice-item-row {
    font-size: 13px;
    color: var(--bio-ink-soft);
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.shortcut-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 14px;
    font-weight: 600;
    color: var(--bio-ink);
    margin-bottom: 14px;
}

.shortcut-empty {
    font-size: 13px;
    color: var(--bio-ink-faint);
    text-align: center;
    padding: 12px 0;
}

.shortcut-set-link {
    color: var(--bio-moss);
    text-decoration: none;
    font-weight: 500;
}

.shortcut-set-link:hover {
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
    gap: 6px;
    padding: 12px 8px;
    border-radius: var(--bio-radius-sm);
    background: var(--bg, #f5f5f5);
    text-decoration: none;
    transition: filter var(--bio-dur-fast) var(--bio-ease-out), transform var(--bio-dur-fast) var(--bio-ease-out);
}

.shortcut-item:hover {
    filter: brightness(1.08);
    transform: translateY(-1px);
}

.shortcut-icon {
    color: var(--color, #606c38);
}

.shortcut-label {
    font-size: 11px;
    color: var(--bio-ink-soft);
}
</style>
