<template>
    <section class="panel todo-panel">
        <div class="panel-header">
            <h3 class="panel-title">待办({{ filteredTodos.length }})</h3>
            <div class="panel-filters">
                <el-select v-model="todoType" placeholder="活动类型" size="small" class="filter-select" clearable
                    @change="applyFilter">
                    <el-option label="全部" value="" /><el-option label="作业" value="assignment" /><el-option label="考试"
                        value="exam" /><el-option label="实训" value="training" />
                </el-select>
                <el-select v-model="todoDeadline" placeholder="截止时间" size="small" class="filter-select" clearable
                    @change="applyFilter">
                    <el-option label="全部" value="" /><el-option label="本周" value="week" /><el-option label="本月"
                        value="month" /><el-option label="已逾期" value="overdue" />
                </el-select>
            </div>
        </div>
        <div v-if="filteredTodos.length > 0" class="todo-list">
            <div v-for="todo in filteredTodos" :key="todo.id" class="todo-item" @click="$emit('todoClick', todo)">
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
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import EmptyIllustration from '@/components/EmptyIllustration.vue'

const props = defineProps({
    todos: { type: Array, default: () => [] }
})

defineEmits(['todoClick'])

const todoType = ref('')
const todoDeadline = ref('')

const filteredTodos = ref([])

const applyFilter = () => {
    let list = [...props.todos]
    if (todoType.value) list = list.filter(t => t.type === todoType.value)
    const now = new Date()
    if (todoDeadline.value === 'week') {
        const we = new Date(now); we.setDate(now.getDate() + 7)
        list = list.filter(t => t.deadline !== '未设定' && new Date(t.deadline) <= we)
    } else if (todoDeadline.value === 'month') {
        const me = new Date(now); me.setDate(now.getDate() + 30)
        list = list.filter(t => t.deadline !== '未设定' && new Date(t.deadline) <= me)
    } else if (todoDeadline.value === 'overdue') {
        list = list.filter(t => t.overdue)
    }
    list.sort((a, b) =>
        a.deadline === '未设定' ? 1 : b.deadline === '未设定' ? -1 : new Date(a.deadline) - new Date(b.deadline)
    )
    filteredTodos.value = list
}

watch(() => props.todos, applyFilter, { immediate: true })
</script>

<style scoped>
.todo-panel {
    flex-shrink: 0;
}

.todo-list {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-height: 300px;
    max-height: 300px;
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

.tag-qa {
    background: #e8eddf;
    color: var(--bio-moss);
}

.tag-evaluation {
    background: #f0e6f0;
    color: #8e6b8e;
}

.tag-survey {
    background: #e6f0e6;
    color: #6b8e6b;
}

.tag-announcement {
    background: #f0ece6;
    color: #8e7a6b;
}

.tag-message {
    background: #dce8f0;
    color: #5c7a8a;
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
</style>
