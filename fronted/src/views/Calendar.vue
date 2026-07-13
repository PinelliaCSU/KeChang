<template>
    <div class="calendar-shell">
        <DashboardHeader />
        <div class="calendar-body">
            <div class="page-title-bar">
                <h2 class="page-title">我的日历</h2>
                <div class="title-actions">
                    <el-tag size="small" type="success" effect="plain">学期第 {{ currentWeek }} 周</el-tag>
                    <el-button size="small" @click="date = new Date()" :icon="RefreshRight">今天</el-button>
                </div>
            </div>
            <div class="panel calendar-panel">
                <el-calendar v-model="date">
                    <template #date-cell="{ data }">
                        <div class="calendar-day"
                            :class="{ 'has-event': getEventsForDay(data.day).length > 0, 'is-today': isToday(data.day) }"
                            @click="selectDate(data.day)">
                            <span class="day-num">{{ data.day.split('-')[2] }}</span>
                            <div class="day-events" v-if="getEventsForDay(data.day).length > 0">
                                <span v-for="(ev, i) in getEventsForDay(data.day).slice(0, 2)" :key="i"
                                    class="event-dot" :style="{ background: ev.color || getDotColor(ev.type) }"
                                    :title="ev.title">{{ ev.title }}</span>
                            </div>
                            <span v-if="getEventsForDay(data.day).length > 2" class="event-more">
                                +{{ getEventsForDay(data.day).length - 2 }}
                            </span>
                        </div>
                    </template>
                </el-calendar>
            </div>

            <!-- 当日事件列表 -->
            <div class="panel events-panel">
                <div class="panel-title-bar">
                    <h3 class="panel-title">📌 {{ formatDate(date) }} 日程</h3>
                    <el-button type="primary" size="small" :icon="Plus" @click="openAddDialog">添加日程</el-button>
                </div>

                <!-- 系统事件（作业/考试/实训） -->
                <div v-if="systemEvents.length > 0" class="event-section">
                    <div class="section-label">系统日程</div>
                    <div class="event-list">
                        <div v-for="ev in systemEvents" :key="ev.id" class="event-item">
                            <span class="event-type-tag" :style="{ background: getDotColor(ev.type) }">{{ ev.typeLabel
                                }}</span>
                            <span class="event-name">{{ ev.title }}</span>
                            <span class="event-course">{{ ev.courseName }}</span>
                            <span class="event-time">{{ ev.time }}</span>
                        </div>
                    </div>
                </div>

                <!-- 个人日程 -->
                <div v-if="personalSchedules.length > 0" class="event-section">
                    <div class="section-label">个人日程</div>
                    <div class="event-list">
                        <div v-for="sc in personalSchedules" :key="'s-' + sc.id" class="event-item schedule-item">
                            <span class="event-dot-bar" :style="{ background: sc.color || '#409EFF' }"></span>
                            <span class="event-name">{{ sc.title }}</span>
                            <span class="event-time" v-if="sc.startTime">
                                {{ sc.startTime }}{{ sc.endTime ? ' ~ ' + sc.endTime : '' }}
                            </span>
                            <span class="event-content-preview" v-if="sc.content">{{ sc.content }}</span>
                            <div class="event-actions">
                                <el-button link type="primary" size="small" :icon="Edit"
                                    @click="openEditDialog(sc)">编辑</el-button>
                                <el-button link type="danger" size="small" :icon="Delete"
                                    @click="handleDelete(sc.id)">删除</el-button>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 空状态 -->
                <div v-if="systemEvents.length === 0 && personalSchedules.length === 0" class="empty-state">
                    当日暂无日程安排，点击"添加日程"创建个人日程
                </div>
            </div>
        </div>

        <!-- 添加/编辑日程对话框 -->
        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
            <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
                <el-form-item label="日程标题" prop="title">
                    <el-input v-model="form.title" placeholder="请输入日程标题" maxlength="200" show-word-limit />
                </el-form-item>
                <el-form-item label="日程日期" prop="scheduleDate">
                    <el-date-picker v-model="form.scheduleDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD"
                        style="width: 100%" />
                </el-form-item>
                <el-form-item label="时间">
                    <div class="time-row">
                        <el-time-picker v-model="form.startTime" placeholder="开始时间" value-format="HH:mm:ss"
                            style="width: 180px" />
                        <span class="time-sep">至</span>
                        <el-time-picker v-model="form.endTime" placeholder="结束时间" value-format="HH:mm:ss"
                            style="width: 180px" />
                    </div>
                </el-form-item>
                <el-form-item label="日程类型">
                    <el-select v-model="form.type" placeholder="选择类型" style="width: 100%">
                        <el-option label="个人" value="PERSONAL" />
                        <el-option label="课程" value="COURSE" />
                        <el-option label="考试" value="EXAM" />
                        <el-option label="实训" value="TRAINING" />
                    </el-select>
                </el-form-item>
                <el-form-item label="标记颜色">
                    <div class="color-picker-row">
                        <span v-for="c in colorOptions" :key="c" class="color-dot" :class="{ active: form.color === c }"
                            :style="{ background: c }" @click="form.color = c"></span>
                        <el-color-picker v-model="form.color" size="small" :predefine="colorOptions" />
                    </div>
                </el-form-item>
                <el-form-item label="详细内容">
                    <el-input v-model="form.content" type="textarea" :rows="3" placeholder="可选，填写日程详细描述" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSubmit" :loading="submitting">
                    {{ isEditing ? '保存修改' : '添加日程' }}
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { RefreshRight, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { assignmentApi, examApi, trainingApi, scheduleApi } from '@/api'
import DashboardHeader from '@/components/DashBoard/DashboardHeader.vue'

const userStore = useUserStore()
const date = ref(new Date())
const allSystemEvents = ref([])
const allSchedules = ref([])
const dialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

// ==================== 表单数据 ====================
const toDateStr = (d) => {
    const dt = d instanceof Date ? d : new Date(d)
    return `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}`
}

const getDefaultForm = () => ({
    title: '',
    scheduleDate: toDateStr(date.value),
    startTime: null,
    endTime: null,
    type: 'PERSONAL',
    color: '#409EFF',
    content: ''
})

const form = ref(getDefaultForm())

const formRules = {
    title: [{ required: true, message: '请输入日程标题', trigger: 'blur' }],
    scheduleDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

const colorOptions = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#8B5CF6', '#EC4899', '#14B8A6']

// ==================== 计算属性 ====================
const dialogTitle = computed(() => isEditing.value ? '编辑日程' : '添加日程')

const currentWeek = computed(() => {
    const now = new Date()
    const semesterStart = new Date(2026, 1, 23)
    const diffWeeks = Math.floor((now - semesterStart) / (7 * 24 * 60 * 60 * 1000))
    return Math.max(1, Math.min(19, diffWeeks))
})

const isToday = (dayStr) => {
    const today = new Date()
    const d = new Date(dayStr)
    return d.toDateString() === today.toDateString()
}

const formatDate = (d) => {
    const dt = d instanceof Date ? d : new Date(d)
    return `${dt.getFullYear()}年${dt.getMonth() + 1}月${dt.getDate()}日`
}

const selectedDateStr = computed(() => toDateStr(date.value))

// 合并所有事件（系统 + 个人日程）
const eventsByDay = computed(() => {
    const map = new Map()

    // 系统事件
    for (const e of allSystemEvents.value) {
        const arr = map.get(e.date) || []
        arr.push(e)
        map.set(e.date, arr)
    }

    // 个人日程
    for (const s of allSchedules.value) {
        const arr = map.get(s.scheduleDate) || []
        arr.push({
            id: 's-' + s.id,
            type: s.type ? s.type.toLowerCase() : 'personal',
            typeLabel: getTypeLabel(s.type),
            title: s.title,
            date: s.scheduleDate,
            time: s.startTime || '',
            color: s.color || '#409EFF',
            isSchedule: true,
            rawId: s.id
        })
        map.set(s.scheduleDate, arr)
    }

    return map
})

const getEventsForDay = (dayStr) => {
    return eventsByDay.value.get(dayStr) || []
}

const todayAllEvents = computed(() => {
    return eventsByDay.value.get(selectedDateStr.value) || []
})

const systemEvents = computed(() => {
    return todayAllEvents.value.filter(e => !e.isSchedule)
})

const personalSchedules = computed(() => {
    return allSchedules.value.filter(s => s.scheduleDate === selectedDateStr.value)
})

const getDotColor = (type) => {
    const map = { assignment: '#409EFF', exam: '#E6A23C', training: '#67C23A', personal: '#409EFF', course: '#67C23A' }
    return map[type] || '#409EFF'
}

const getTypeLabel = (type) => {
    const map = { PERSONAL: '个人', COURSE: '课程', EXAM: '考试', TRAINING: '实训' }
    return map[type] || '个人'
}

// ==================== 日期选择 ====================
const selectDate = (dayStr) => {
    date.value = new Date(dayStr)
}

// 监听月份变化，重新加载该月的个人日程
watch(date, (newVal) => {
    if (newVal) {
        loadMonthSchedules(newVal.getFullYear(), newVal.getMonth() + 1)
    }
})

// ==================== 加载数据 ====================
const loadMonthSchedules = async (year, month) => {
    try {
        const res = await scheduleApi.listByMonth(year, month)
        if (res.data.code === 200) {
            allSchedules.value = res.data.data || []
        }
    } catch (e) {
        console.error('加载日程失败', e)
    }
}

const fetchSystemEvents = async () => {
    try {
        const [assignRes, examRes, trainRes] = await Promise.allSettled([
            assignmentApi.list({ page: 1, size: 50 }),
            examApi.listExams({ page: 1, size: 50 }),
            trainingApi.listProjects({ page: 1, size: 50 })
        ])
        const events = []
        const extractList = (res) => {
            if (res.status !== 'fulfilled' || res.value.data.code !== 200) return []
            const d = res.value.data.data
            return Array.isArray(d?.records) ? d.records : Array.isArray(d?.list) ? d.list : Array.isArray(d) ? d : []
        }
        extractList(assignRes).forEach(a => {
            const parts = (a.deadline || a.endTime || '').split(' ')
            const d = parts[0] || ''
            if (d) events.push({
                id: `a-${a.id}`, type: 'assignment', typeLabel: '作业', title: a.title,
                courseName: a.courseName || '', date: d, time: parts[1] || '', color: '#409EFF'
            })
        })
        extractList(examRes).forEach(e => {
            const parts = (e.startTime || e.endTime || '').split(' ')
            const d = parts[0] || ''
            if (d) events.push({
                id: `ex-${e.id}`, type: 'exam', typeLabel: '考试', title: e.name || e.title,
                courseName: e.courseName || '', date: d, time: parts[1] || '', color: '#E6A23C'
            })
        })
        extractList(trainRes).forEach(t => {
            const parts = (t.deadline || t.endTime || '').split(' ')
            const d = parts[0] || ''
            if (d) events.push({
                id: `t-${t.id}`, type: 'training', typeLabel: '实训', title: t.name || t.title,
                courseName: t.courseName || '', date: d, time: parts[1] || '', color: '#67C23A'
            })
        })
        allSystemEvents.value = events
    } catch (e) {
        console.error('获取日历事件失败', e)
    }
}

// ==================== 日程 CRUD ====================
const openAddDialog = () => {
    isEditing.value = false
    editingId.value = null
    form.value = {
        ...getDefaultForm(),
        scheduleDate: selectedDateStr.value
    }
    dialogVisible.value = true
}

const openEditDialog = (schedule) => {
    isEditing.value = true
    editingId.value = schedule.id
    form.value = {
        title: schedule.title,
        scheduleDate: schedule.scheduleDate,
        startTime: schedule.startTime,
        endTime: schedule.endTime,
        type: schedule.type || 'PERSONAL',
        color: schedule.color || '#409EFF',
        content: schedule.content || ''
    }
    dialogVisible.value = true
}

const handleSubmit = async () => {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return

    submitting.value = true
    try {
        const payload = {
            title: form.value.title,
            scheduleDate: form.value.scheduleDate,
            startTime: form.value.startTime || null,
            endTime: form.value.endTime || null,
            type: form.value.type,
            color: form.value.color,
            content: form.value.content || null
        }

        if (isEditing.value) {
            await scheduleApi.update(editingId.value, payload)
            ElMessage.success('日程已更新')
        } else {
            await scheduleApi.create(payload)
            ElMessage.success('日程已添加')
        }

        dialogVisible.value = false
        // 重新加载当前月份的日程
        const y = date.value.getFullYear()
        const m = date.value.getMonth() + 1
        await loadMonthSchedules(y, m)
    } catch (e) {
        ElMessage.error(e?.response?.data?.message || '操作失败')
    } finally {
        submitting.value = false
    }
}

const handleDelete = async (id) => {
    try {
        await ElMessageBox.confirm('确定要删除该日程吗？', '确认删除', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })
        await scheduleApi.delete(id)
        ElMessage.success('日程已删除')
        const y = date.value.getFullYear()
        const m = date.value.getMonth() + 1
        await loadMonthSchedules(y, m)
    } catch (e) {
        if (e !== 'cancel' && e?.response) {
            ElMessage.error(e.response.data?.message || '删除失败')
        }
    }
}

onMounted(() => {
    fetchSystemEvents()
    loadMonthSchedules(date.value.getFullYear(), date.value.getMonth() + 1)
})
</script>

<style scoped>
.calendar-shell {
    display: flex;
    flex-direction: column;
    height: 100vh;
    font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
    background: #f5f7fa;
    color: #303133;
    overflow: hidden;
}

.calendar-body {
    flex: 1;
    overflow-y: auto;
    padding: 24px;
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.page-title-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.title-actions {
    display: flex;
    align-items: center;
    gap: 8px;
}

.page-title {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    margin: 0;
}

.panel {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    padding: 24px;
}

.calendar-panel {
    padding: 12px;
}

.calendar-day {
    min-height: 60px;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 4px;
    cursor: pointer;
    border-radius: 6px;
    transition: background 0.15s;
    position: relative;
}

.calendar-day:hover {
    background: #f0f5ff;
}

.calendar-day.is-today .day-num {
    background: var(--bio-primary, #409EFF);
    color: #fff;
    border-radius: 50%;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.calendar-day.has-event {
    font-weight: 500;
}

.day-num {
    font-size: 14px;
    font-weight: 500;
}

.day-events {
    display: flex;
    flex-direction: column;
    gap: 2px;
    margin-top: 2px;
    width: 100%;
    overflow: hidden;
}

.event-dot {
    font-size: 10px;
    padding: 1px 4px;
    border-radius: 3px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    color: #fff;
}

.event-more {
    font-size: 10px;
    color: #909399;
    margin-top: 1px;
}

/* ---- 当日事件面板 ---- */
.panel-title-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
}

.panel-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin: 0;
}

.section-label {
    font-size: 12px;
    color: #909399;
    margin-bottom: 8px;
    padding-left: 4px;
    border-left: 3px solid #dcdfe6;
    padding: 2px 0 2px 8px;
}

.event-section {
    margin-bottom: 16px;
}

.event-section:last-child {
    margin-bottom: 0;
}

.event-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.event-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    border-radius: 8px;
    background: #f8f9fb;
    transition: background 0.15s;
}

.event-item:hover {
    background: #f0f5ff;
}

.schedule-item {
    flex-wrap: wrap;
}

.event-type-tag {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 4px;
    color: #fff;
    flex-shrink: 0;
}

.event-dot-bar {
    width: 4px;
    height: 18px;
    border-radius: 2px;
    flex-shrink: 0;
}

.event-name {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    flex: 1;
    min-width: 120px;
}

.event-course {
    font-size: 12px;
    color: #909399;
}

.event-time {
    font-size: 12px;
    color: #909399;
    flex-shrink: 0;
}

.event-content-preview {
    font-size: 12px;
    color: #c0c4cc;
    width: 100%;
    margin-top: 4px;
    padding-left: 16px;
}

.event-actions {
    display: flex;
    gap: 4px;
    flex-shrink: 0;
    margin-left: auto;
}

.empty-state {
    text-align: center;
    color: #c0c4cc;
    padding: 24px 0;
    font-size: 14px;
}

/* ---- 对话框 ---- */
.time-row {
    display: flex;
    align-items: center;
    gap: 8px;
}

.time-sep {
    color: #909399;
    font-size: 13px;
}

.color-picker-row {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}

.color-dot {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    cursor: pointer;
    border: 2px solid transparent;
    transition: all 0.15s;
    box-sizing: border-box;
}

.color-dot:hover {
    transform: scale(1.15);
}

.color-dot.active {
    border-color: #303133;
    transform: scale(1.1);
}
</style>
