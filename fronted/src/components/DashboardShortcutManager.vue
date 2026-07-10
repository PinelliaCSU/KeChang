<template>
    <el-dialog v-model="dialogVisible" title="常用入口管理" width="680px" top="5vh" :close-on-click-modal="false"
        destroy-on-close class="shortcut-dialog" @closed="onDialogClosed">
        <div class="dialog-toolbar">
            <span class="toolbar-hint">长按卡片进入拖拽排序模式，点击眼睛按钮切换显示/隐藏</span>
            <div class="toolbar-actions">
                <el-tooltip content="重置为默认" placement="top">
                    <el-button icon="Refresh" size="small" circle @click="resetDefaults" />
                </el-tooltip>
            </div>
        </div>

        <div class="shortcut-grid" :class="{ 'is-dragging': dragEnabled }" @dragover.prevent="onDragOver"
            @drop.prevent="onDrop">
            <div v-for="(item, index) in localEntries" :key="item.id" class="shortcut-card" :class="{
                'is-hidden': !item.visible,
                'is-drag-target': dragEnabled,
                'is-dragging-current': draggingIndex === index
            }" :draggable="dragEnabled" @mouseenter="hoveredId = item.id" @mouseleave="hoveredId = null"
                @dragstart="onDragStart(index, $event)" @dragenter="onDragEnter(index)" @dragend="onDragEnd"
                @click="handleClick(item)">
                <!-- 长按监听（桌面端） -->
                <div class="long-press-trigger" @mousedown="startLongPress(index)" @mouseup="cancelLongPress"
                    @mouseleave="cancelLongPress"></div>

                <!-- 条目内容 -->
                <div class="shortcut-card-body" :style="{ '--item-color': item.color }">
                    <span class="shortcut-icon-wrap">
                        <el-icon :size="24">
                            <component :is="item.icon" />
                        </el-icon>
                    </span>
                    <span class="shortcut-label">{{ item.label }}</span>
                </div>

                <!-- 遮罩层（隐藏时） -->
                <div v-if="!item.visible" class="hidden-overlay">
                    <el-icon :size="20" color="var(--bio-ink-faint)">
                        <Hide />
                    </el-icon>
                </div>

                <!-- 眼睛切换按钮 -->
                <button class="eye-btn" :class="{ 'is-hidden': !item.visible }" :title="item.visible ? '点击隐藏' : '点击显示'"
                    @click.stop="toggleVisibility(index)">
                    <el-icon :size="16">
                        <View v-if="item.visible" />
                        <Hide v-else />
                    </el-icon>
                </button>


            </div>
        </div>

        <!-- 统计信息 -->
        <div class="shortcut-footer">
            <span class="footer-hint">
                已显示 {{ visibleCount }} / {{ localEntries.length }} 个
            </span>
            <span v-if="dragEnabled" class="drag-hint">🖐 拖拽卡片调整顺序，点击<span style="color:var(--bio-moss);cursor:pointer"
                    @click.stop="dragEnabled = false">完成排序</span></span>
        </div>
    </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const props = defineProps({
    modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const router = useRouter()
const userStore = useUserStore()

const dialogVisible = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
})

const onDialogClosed = () => {
    dragEnabled.value = false
    draggingIndex.value = -1
}

// 所有可能的入口定义（按角色分组）
const ENTRY_DEFS = {
    common: [
        { id: 'calendar', label: '日历', icon: 'Calendar', route: '/calendar', color: '#a5c4d4' },
        { id: 'profile', label: '个人中心', icon: 'User', route: '/profile', color: '#b08968' }
    ],
    student: [
        { id: 's-courses', label: '课程中心', icon: 'Reading', route: '/student/courses', color: '#7f8c52' },
        { id: 's-assignments', label: '我的作业', icon: 'Edit', route: '/student/assignments', color: '#b08968' },
        { id: 's-exams', label: '在线考试', icon: 'Timer', route: '/student/exams', color: '#c4665a' },
        { id: 's-grades', label: '我的成绩', icon: 'TrendCharts', route: '/student/grades', color: '#6b8e4e' },
        { id: 's-notes', label: '学习笔记', icon: 'Notebook', route: '/student/notes', color: '#5c7a8a' },
        { id: 's-trainings', label: '实训中心', icon: 'Tools', route: '/student/trainings', color: '#9c6644' },
        { id: 's-posts', label: '互动问答', icon: 'ChatDotRound', route: '/student/posts', color: '#7a9fb5' },
        { id: 's-knowledge', label: '知识库', icon: 'Collection', route: '/student/knowledge', color: '#606c38' },
        { id: 's-evaluations', label: '教学评价', icon: 'List', route: '/student/evaluations', color: '#c4956a' },
        { id: 's-surveys', label: '问卷调查', icon: 'Document', route: '/student/surveys', color: '#a3b18a' }
    ],
    teacher: [
        { id: 't-courses', label: '课程管理', icon: 'Reading', route: '/teacher/courses', color: '#7f8c52' },
        { id: 't-questions', label: '试题管理', icon: 'Connection', route: '/teacher/questions', color: '#5c7a8a' },
        { id: 't-papers', label: '试卷管理', icon: 'DocumentCopy', route: '/teacher/papers', color: '#b08968' },
        { id: 't-exams', label: '考试管理', icon: 'Timer', route: '/teacher/exams', color: '#c4665a' },
        { id: 't-assignments', label: '作业管理', icon: 'Edit', route: '/teacher/assignments', color: '#9c6644' },
        { id: 't-grades', label: '成绩管理', icon: 'TrendCharts', route: '/teacher/grades', color: '#6b8e4e' },
        { id: 't-trainings', label: '实训管理', icon: 'Tools', route: '/teacher/trainings', color: '#606c38' },
        { id: 't-surveys', label: '问卷管理', icon: 'Document', route: '/teacher/surveys', color: '#a3b18a' },
        { id: 't-evaluations', label: '教学评价', icon: 'List', route: '/teacher/evaluations', color: '#c4956a' },
        { id: 't-posts', label: '互动问答', icon: 'ChatDotRound', route: '/teacher/posts', color: '#7a9fb5' }
    ],
    admin: [
        { id: 'a-courses', label: '课程总览', icon: 'Reading', route: '/admin/courses', color: '#7f8c52' },
        { id: 'a-grades', label: '成绩概览', icon: 'TrendCharts', route: '/admin/grades', color: '#6b8e4e' },
        { id: 'a-surveys', label: '问卷统计', icon: 'Document', route: '/admin/surveys', color: '#a3b18a' }
    ]
}

// 获取当前角色对应的默认入口
const getDefaultEntries = () => {
    const common = ENTRY_DEFS.common.map(e => ({ ...e, visible: true }))
    let roleEntries = []
    if (userStore.isStudent) {
        roleEntries = ENTRY_DEFS.student.map(e => ({ ...e, visible: true }))
    } else if (userStore.isTeacher) {
        roleEntries = ENTRY_DEFS.teacher.map(e => ({ ...e, visible: true }))
    } else if (userStore.isAdmin) {
        roleEntries = ENTRY_DEFS.admin.map(e => ({ ...e, visible: true }))
    }
    return [...common, ...roleEntries]
}

// 存储 key
const STORAGE_KEY = 'kechang_shortcuts'

// 从 localStorage 加载，否则用默认
const loadEntries = () => {
    try {
        const saved = localStorage.getItem(STORAGE_KEY)
        if (saved) {
            const parsed = JSON.parse(saved)
            // 检查角色是否匹配，如果角色变了则用默认
            if (parsed.role === userStore.roleCode && Array.isArray(parsed.entries)) {
                return parsed.entries
            }
        }
    } catch (e) { /* ignore */ }
    return getDefaultEntries()
}

const localEntries = ref(loadEntries())
const dragEnabled = ref(false)
const draggingIndex = ref(-1)
const hoveredId = ref(null)
const longPressTimer = ref(null)

const visibleCount = computed(() => localEntries.value.filter(e => e.visible).length)

// 持久化保存
const saveEntries = () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
        role: userStore.roleCode,
        entries: localEntries.value
    }))
    // 派发自定义事件，通知同一标签页内其他组件（如右侧栏）刷新
    window.dispatchEvent(new CustomEvent('shortcuts-changed'))
}

watch(localEntries, saveEntries, { deep: true })

// ========== 可见性切换 ==========
const toggleVisibility = (index) => {
    localEntries.value[index].visible = !localEntries.value[index].visible
}

// ========== 跳转 ==========
const handleClick = (item) => {
    if (dragEnabled.value) return // 拖拽模式下不跳转
    if (!item.visible) return // 隐藏的不跳转
    router.push(item.route)
}

// ========== 拖拽排序 ==========
const onDragStart = (index, event) => {
    if (!dragEnabled.value) {
        event.preventDefault()
        return
    }
    draggingIndex.value = index
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(index))
}

const onDragEnter = (index) => {
    if (draggingIndex.value === -1 || draggingIndex.value === index) return
    const item = localEntries.value.splice(draggingIndex.value, 1)[0]
    localEntries.value.splice(index, 0, item)
    draggingIndex.value = index
}

const onDragOver = () => { }

const onDrop = () => {
    draggingIndex.value = -1
}

const onDragEnd = () => {
    draggingIndex.value = -1
}

// ========== 长按检测 ==========
const startLongPress = (index) => {
    if (dragEnabled.value) return
    longPressTimer.value = setTimeout(() => {
        dragEnabled.value = true
        longPressTimer.value = null
    }, 500)
}

const cancelLongPress = () => {
    if (longPressTimer.value) {
        clearTimeout(longPressTimer.value)
        longPressTimer.value = null
    }
}

// ========== 重置 ==========
const resetDefaults = () => {
    localEntries.value = getDefaultEntries()
    ElMessage.success('已重置为默认入口')
}
</script>

<style scoped>
.dialog-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 18px;
    gap: 12px;
}

.toolbar-hint {
    font-size: 12px;
    color: var(--bio-ink-faint);
    line-height: 1.5;
}

.toolbar-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
}

/* ---------- 网格 ---------- */
.shortcut-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
    gap: 12px;
    transition: gap var(--bio-dur-normal) var(--bio-ease-out);
}

.shortcut-grid.is-dragging {
    gap: 8px;
}

/* ---------- 卡片 ---------- */
.shortcut-card {
    position: relative;
    background: var(--bio-paper);
    border: 1px solid rgba(163, 177, 138, 0.12);
    border-radius: var(--bio-radius-md);
    padding: 16px 12px;
    cursor: pointer;
    transition:
        box-shadow var(--bio-dur-normal) var(--bio-ease-out),
        transform var(--bio-dur-fast) var(--bio-ease-out),
        opacity var(--bio-dur-normal) var(--bio-ease-out),
        filter var(--bio-dur-normal) var(--bio-ease-out),
        border-color var(--bio-dur-normal) var(--bio-ease-out),
        background var(--bio-dur-normal) var(--bio-ease-out);
    overflow: hidden;
    user-select: none;
}

.shortcut-card:hover:not(.is-drag-target) {
    box-shadow: var(--bio-shadow-md);
    transform: translateY(-2px);
    border-color: rgba(163, 177, 138, 0.25);
}

.shortcut-card:active:not(.is-drag-target):not(.is-hidden) {
    transform: translateY(0);
}

/* 隐藏状态 - 暗淡 */
.shortcut-card.is-hidden {
    opacity: 0.5;
    filter: grayscale(0.8);
    background: #f8f8f4;
    border-color: rgba(163, 177, 138, 0.06);
}

.shortcut-card.is-hidden:hover {
    opacity: 0.65;
    filter: grayscale(0.6);
}

/* 拖拽目标状态 */
.shortcut-card.is-drag-target {
    cursor: grab;
    border-style: dashed;
    border-color: var(--bio-leaf);
    background: rgba(163, 177, 138, 0.06);
}

.shortcut-card.is-drag-target:active {
    cursor: grabbing;
}

.shortcut-card.is-dragging-current {
    opacity: 0.4;
    box-shadow: var(--bio-shadow-lg);
    transform: scale(0.95) rotate(1deg);
}

/* ---------- 长按触发器（透明占位） ---------- */
.long-press-trigger {
    position: absolute;
    inset: 0;
    z-index: 1;
    cursor: inherit;
}

/* ---------- 卡片内容 ---------- */
.shortcut-card-body {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
    text-align: center;
    position: relative;
    z-index: 0;
    pointer-events: none;
}

.shortcut-icon-wrap {
    width: 48px;
    height: 48px;
    border-radius: var(--bio-radius-sm);
    display: flex;
    align-items: center;
    justify-content: center;
    background: color-mix(in srgb, var(--item-color) 18%, transparent);
    color: var(--item-color);
    transition:
        background var(--bio-dur-normal) var(--bio-ease-out),
        transform var(--bio-dur-normal) var(--bio-ease-out);
}

.shortcut-card:hover:not(.is-drag-target) .shortcut-icon-wrap {
    transform: scale(1.08);
}

.shortcut-label {
    font-size: 13px;
    font-weight: 500;
    color: var(--bio-ink);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 100%;
}

/* ---------- 隐藏遮罩 ---------- */
.hidden-overlay {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(248, 248, 244, 0.5);
    border-radius: var(--bio-radius-md);
    z-index: 2;
    pointer-events: none;
}

/* ---------- 眼睛按钮 ---------- */
.eye-btn {
    position: absolute;
    top: 6px;
    right: 6px;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    border: none;
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(4px);
    color: var(--bio-ink-muted);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 3;
    opacity: 0;
    transition:
        opacity var(--bio-dur-fast) var(--bio-ease-out),
        background var(--bio-dur-fast) var(--bio-ease-out),
        color var(--bio-dur-fast) var(--bio-ease-out);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.shortcut-card:hover .eye-btn,
.shortcut-card.is-drag-target .eye-btn {
    opacity: 1;
}

.eye-btn:hover {
    background: var(--bio-paper);
    color: var(--bio-moss);
}

.eye-btn.is-hidden {
    background: rgba(196, 102, 90, 0.12);
    color: var(--bio-danger);
}

.eye-btn.is-hidden:hover {
    background: rgba(196, 102, 90, 0.2);
    color: var(--bio-danger);
}

/* ---------- 拖拽手柄 ---------- */
.drag-handle {
    position: absolute;
    top: 6px;
    left: 6px;
    width: 24px;
    height: 24px;
    border-radius: 4px;
    background: rgba(163, 177, 138, 0.12);
    color: var(--bio-ink-muted);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 3;
    cursor: grab;
}

/* ---------- 底部统计 ---------- */
.shortcut-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 14px;
    font-size: 12px;
    color: var(--bio-ink-faint);
}

.drag-hint {
    color: var(--bio-moss);
    font-weight: 500;
    animation: pulseHint 2s ease-in-out infinite;
}

@keyframes pulseHint {

    0%,
    100% {
        opacity: 0.6;
    }

    50% {
        opacity: 1;
    }
}

/* ---------- 对话框覆盖样式 ---------- */
:deep(.el-dialog__header) {
    padding: 20px 24px 0;
    margin-right: 0;
}

:deep(.el-dialog__title) {
    font-size: 18px;
    font-weight: 600;
    color: var(--bio-ink);
}

:deep(.el-dialog__body) {
    padding: 16px 24px 20px;
    max-height: 70vh;
    overflow-y: auto;
}

:deep(.el-dialog__close) {
    color: var(--bio-ink-muted);
    transition: color var(--bio-dur-fast) var(--bio-ease-out);
}

:deep(.el-dialog__close:hover) {
    color: var(--bio-ink);
}
</style>
