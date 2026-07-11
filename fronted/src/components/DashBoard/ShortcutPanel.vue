<template>
    <div class="shortcut-panel panel">
        <div class="section-head">
            <h4 class="section-title">常用入口</h4>
            <el-button size="small" circle icon="Setting" @click="dialogVisible = true" />
        </div>

        <!-- 默认只展示前 8 个可见的入口 -->
        <div class="shortcut-grid">
            <div v-for="item in displayItems" :key="item.id" class="shortcut-item"
                @click="item.visible !== false && item.route && router.push(item.route)">
                <el-icon :size="22" class="shortcut-icon">
                    <component :is="item.icon" />
                </el-icon>
                <span class="shortcut-label">{{ item.label }}</span>
            </div>
        </div>

        <!-- ========== 管理浮窗 ========== -->
        <el-dialog v-model="dialogVisible" title="管理常用入口" width="620px" top="6vh" :close-on-click-modal="false"
            destroy-on-close class="shortcut-dialog" @closed="onDialogClosed">
            <div class="dialog-toolbar">
                <span class="toolbar-hint">拖拽卡片调整顺序，点击眼睛按钮切换显示/隐藏</span>
                <el-button icon="Refresh" size="small" circle @click="resetDefaults" />
            </div>

            <div class="dialog-grid" @dragover.prevent="onDragOver" @drop.prevent="onDrop">
                <div v-for="(item, index) in items" :key="item.id" class="dialog-card" :class="{
                    'is-hidden': !item.visible,
                    'is-dragging': draggingIndex === index
                }" :draggable="true" @dragstart="onDragStart(index, $event)" @dragenter="onDragEnter(index, $event)"
                    @dragend="onDragEnd">

                    <!-- 隐藏遮罩 -->
                    <div v-if="!item.visible" class="card-hidden-overlay">
                        <el-icon :size="18">
                            <Hide />
                        </el-icon>
                    </div>

                    <!-- 内容 -->
                    <div class="dialog-card-body">
                        <span class="card-icon-wrap">
                            <el-icon :size="20">
                                <component :is="item.icon" />
                            </el-icon>
                        </span>
                        <span class="card-label">{{ item.label }}</span>
                    </div>

                    <!-- 可见性切换 -->
                    <button class="card-eye" :class="{ 'is-hidden': !item.visible }" :title="item.visible ? '隐藏' : '显示'"
                        @click.stop="toggleVisibility(index)">
                        <el-icon :size="15">
                            <View v-if="item.visible" />
                            <Hide v-else />
                        </el-icon>
                    </button>
                </div>
            </div>

            <div class="dialog-footer">
                <span>已显示 {{ visibleCount }} / {{ items.length }} 个</span>
                <el-button type="primary" size="small" @click="dialogVisible = false">完成</el-button>
            </div>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// ==================== 入口定义 ====================
const ENTRY_DEFS = {
    common: [
        { id: 'calendar', label: '日历', icon: 'Calendar', route: '/calendar', color: '#a5c4d4' },
        { id: 'profile', label: '个人中心', icon: 'User', route: '/profile', color: '#b08968' }
    ],
    student: [
        { id: 's-courses', label: '课程中心', icon: 'Reading', route: '/student/courses', color: '#20c4cc' },
        { id: 's-assignments', label: '我的作业', icon: 'Edit', route: '/student/assignments', color: '#b08968' },
        { id: 's-exams', label: '在线考试', icon: 'Timer', route: '/student/exams', color: '#c4665a' },
        { id: 's-class-quizzes', label: '随堂测验', icon: 'Connection', route: '/student/class-quizzes', color: '#5c7a8a' },
        { id: 's-checkin', label: '课堂签到', icon: 'Checked', route: '/student/checkin', color: '#20c4cc' },
        { id: 's-grades', label: '我的成绩', icon: 'TrendCharts', route: '/student/grades', color: '#20c4cc' },
        { id: 's-notes', label: '学习笔记', icon: 'Notebook', route: '/student/notes', color: '#5c7a8a' },
        { id: 's-trainings', label: '实训中心', icon: 'Tools', route: '/student/trainings', color: '#9c6644' },
        { id: 's-posts', label: '互动问答', icon: 'ChatDotRound', route: '/student/posts', color: '#7a9fb5' },
        { id: 's-knowledge', label: '反思', icon: 'Collection', route: '/student/knowledge', color: '#20c4cc' },
        { id: 's-evaluations', label: '教学评价', icon: 'List', route: '/student/evaluations', color: '#c4956a' },
        { id: 's-surveys', label: '问卷调查', icon: 'Document', route: '/student/surveys', color: '#20c4cc' },
        { id: 's-messages', label: '私信', icon: 'ChatLineSquare', route: '/student/messages', color: '#20c4cc' }
    ],
    teacher: [
        { id: 't-courses', label: '课程管理', icon: 'Reading', route: '/teacher/courses', color: '#20c4cc' },
        { id: 't-questions', label: '试题管理', icon: 'Connection', route: '/teacher/questions', color: '#5c7a8a' },
        { id: 't-papers', label: '试卷管理', icon: 'DocumentCopy', route: '/teacher/papers', color: '#b08968' },
        { id: 't-exams', label: '考试管理', icon: 'Timer', route: '/teacher/exams', color: '#c4665a' },
        { id: 't-class-quizzes', label: '随堂测验', icon: 'Connection', route: '/teacher/class-quizzes', color: '#5c7a8a' },
        { id: 't-checkin', label: '课堂签到', icon: 'Checked', route: '/teacher/checkin', color: '#20c4cc' },
        { id: 't-roll-call', label: '随机点名', icon: 'User', route: '/teacher/roll-call', color: '#9c6644' },
        { id: 't-assignments', label: '作业管理', icon: 'Edit', route: '/teacher/assignments', color: '#9c6644' },
        { id: 't-grades', label: '成绩管理', icon: 'TrendCharts', route: '/teacher/grades', color: '#20c4cc' },
        { id: 't-trainings', label: '实训管理', icon: 'Tools', route: '/teacher/trainings', color: '#20c4cc' },
        { id: 't-surveys', label: '问卷管理', icon: 'Document', route: '/teacher/surveys', color: '#20c4cc' },
        { id: 't-evaluations', label: '教学评价', icon: 'List', route: '/teacher/evaluations', color: '#c4956a' },
        { id: 't-posts', label: '互动问答', icon: 'ChatDotRound', route: '/teacher/posts', color: '#7a9fb5' },
        { id: 't-messages', label: '私信', icon: 'ChatLineSquare', route: '/teacher/messages', color: '#20c4cc' }
    ],
    admin: [
        { id: 'a-courses', label: '课程总览', icon: 'Reading', route: '/admin/courses', color: '#20c4cc' },
        { id: 'a-grades', label: '成绩概览', icon: 'TrendCharts', route: '/admin/grades', color: '#20c4cc' },
        { id: 'a-surveys', label: '问卷统计', icon: 'Document', route: '/admin/surveys', color: '#20c4cc' },
        { id: 'a-operation-logs', label: '操作日志', icon: 'List', route: '/admin/operation-logs', color: '#5c7a8a' }
    ]
}

// ==================== 状态 ====================
const dialogVisible = ref(false)
const items = ref([])
const draggingIndex = ref(-1)

const STORAGE_KEY = 'kechang_shortcuts'
const MAX_DISPLAY = 8

// ==================== 计算属性 ====================
const visibleCount = computed(() => items.value.filter(e => e.visible).length)

// 只取前 8 个可见的入口展示
const displayItems = computed(() =>
    items.value.filter(e => e.visible).slice(0, MAX_DISPLAY)
)

// ==================== 初始化 ====================
const getDefaultEntries = () => {
    const common = ENTRY_DEFS.common.map(e => ({ ...e, visible: true }))
    let roleEntries = []
    if (userStore.isStudent) roleEntries = ENTRY_DEFS.student.map(e => ({ ...e, visible: true }))
    else if (userStore.isTeacher) roleEntries = ENTRY_DEFS.teacher.map(e => ({ ...e, visible: true }))
    else if (userStore.isAdmin) roleEntries = ENTRY_DEFS.admin.map(e => ({ ...e, visible: true }))
    return [...common, ...roleEntries]
}

const loadEntries = () => {
    try {
        const saved = localStorage.getItem(STORAGE_KEY)
        if (saved) {
            const parsed = JSON.parse(saved)
            if (parsed.role === userStore.roleCode && Array.isArray(parsed.entries)) {
                items.value = parsed.entries
                return
            }
        }
    } catch { /* ignore */ }
    items.value = getDefaultEntries()
}

const saveEntries = () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
        role: userStore.roleCode,
        entries: items.value
    }))
}

// ==================== 交互 ====================
const toggleVisibility = (index) => {
    items.value[index].visible = !items.value[index].visible
    saveEntries()
}

// ===== 拖拽 =====
const onDragStart = (index, event) => {
    draggingIndex.value = index
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(index))
}

const onDragEnter = (index, event) => {
    event.preventDefault()
    if (draggingIndex.value === -1 || draggingIndex.value === index) return
    const item = items.value.splice(draggingIndex.value, 1)[0]
    items.value.splice(index, 0, item)
    draggingIndex.value = index
}

const onDragOver = () => { }

const onDrop = () => {
    draggingIndex.value = -1
    saveEntries()
}

const onDragEnd = () => {
    draggingIndex.value = -1
}

// ===== 重置 =====
const resetDefaults = () => {
    items.value = getDefaultEntries()
    saveEntries()
    ElMessage.success('已重置为默认入口')
}

const onDialogClosed = () => {
    draggingIndex.value = -1
}

// ==================== 生命周期 ====================
onMounted(() => {
    loadEntries()
    window.addEventListener('storage', (e) => {
        if (e.key === STORAGE_KEY) loadEntries()
    })
})
</script>

<style scoped>
.panel {
    background: rgba(255, 255, 255, 0.94);
    border-radius: 14px;
    box-shadow: var(--bio-shadow-sm);
    padding: 24px;
    border: 1px solid var(--bio-border);
}

.section-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
}

.section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--bio-ink);
    margin: 0;
}

/* ===== 展示网格 4列 ===== */
.shortcut-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;
}

.shortcut-item {
    border: 1px solid var(--bio-border);
    border-radius: 12px;
    background: #fff;
    color: var(--bio-ink-soft);
    padding: 14px 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    transition: all var(--bio-dur-fast) var(--bio-ease-out);
    user-select: none;
}

.shortcut-item:hover {
    color: var(--bio-primary);
    background: var(--bio-primary-soft);
    transform: translateY(-1px);
}

.shortcut-icon {
    color: var(--bio-ink-soft);
    transition: color var(--bio-dur-fast) var(--bio-ease-out);
}

.shortcut-item:hover .shortcut-icon {
    color: var(--bio-primary);
}

.shortcut-label {
    font-size: 12px;
    font-weight: 500;
    line-height: 1.3;
    text-align: center;
    word-break: keep-all;
}

/* ========== 浮窗样式 ========== */
.dialog-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 18px;
}

.toolbar-hint {
    font-size: 12px;
    color: var(--bio-ink-faint);
}

.dialog-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 12px;
    max-height: 420px;
    overflow-y: auto;
    padding: 4px 2px;
}

.dialog-card {
    position: relative;
    background: var(--bio-paper);
    border: 1px solid var(--bio-border);
    border-radius: var(--bio-radius-md);
    padding: 14px 12px;
    cursor: grab;
    transition: box-shadow var(--bio-dur-normal), transform var(--bio-dur-fast), opacity var(--bio-dur-normal);
    user-select: none;
    display: flex;
    align-items: center;
    gap: 10px;
}

.dialog-card:hover {
    box-shadow: var(--bio-shadow-md);
    border-color: var(--bio-primary);
}

.dialog-card:active {
    cursor: grabbing;
}

.dialog-card.is-hidden {
    opacity: 0.45;
    filter: grayscale(0.7);
    background: #f8f8f4;
}

.dialog-card.is-dragging {
    opacity: 0.35;
    transform: scale(0.95) rotate(1deg);
    box-shadow: var(--bio-shadow-lg);
}

.dialog-card-body {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 0;
    pointer-events: none;
}

.card-icon-wrap {
    width: 36px;
    height: 36px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bio-primary-soft);
    color: var(--bio-primary);
    flex-shrink: 0;
}

.card-label {
    font-size: 13px;
    font-weight: 500;
    color: var(--bio-ink);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.card-hidden-overlay {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(248, 248, 244, 0.4);
    border-radius: var(--bio-radius-md);
    z-index: 2;
    pointer-events: none;
}

.card-eye {
    position: absolute;
    top: 4px;
    right: 4px;
    z-index: 3;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    border: none;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    background: var(--bio-primary-soft);
    color: var(--bio-primary);
    transition: all var(--bio-dur-fast);
    opacity: 0;
}

.dialog-card:hover .card-eye {
    opacity: 1;
}

.card-eye.is-hidden {
    background: rgba(185, 185, 175, 0.2);
    color: var(--bio-ink-faint);
}

.card-eye:hover {
    transform: scale(1.15);
}

.dialog-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 16px;
    font-size: 12px;
    color: var(--bio-ink-faint);
}
</style>
