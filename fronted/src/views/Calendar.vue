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
                            :class="{ 'has-event': getEventsForDay(data.day).length > 0, 'is-today': isToday(data.day) }">
                            <span class="day-num">{{ data.day.split('-')[2] }}</span>
                            <div class="day-events" v-if="getEventsForDay(data.day).length > 0">
                                <span v-for="(ev, i) in getEventsForDay(data.day).slice(0, 2)" :key="i"
                                    class="event-dot" :class="'dot-' + ev.type" :title="ev.title">{{ ev.title }}</span>
                            </div>
                        </div>
                    </template>
                </el-calendar>
            </div>
            <!-- 当日事件列表 -->
            <div class="panel events-panel" v-if="todayEvents.length > 0">
                <h3 class="panel-title">📌 {{ formatDate(date) }} 日程</h3>
                <div class="event-list">
                    <div v-for="ev in todayEvents" :key="ev.id" class="event-item">
                        <span class="event-type-tag" :class="'tag-' + ev.type">{{ ev.typeLabel }}</span>
                        <span class="event-name">{{ ev.title }}</span>
                        <span class="event-course">{{ ev.courseName }}</span>
                        <span class="event-time">{{ ev.time }}</span>
                    </div>
                </div>
            </div>
            <div class="panel events-panel" v-else>
                <h3 class="panel-title">📌 {{ formatDate(date) }} 日程</h3>
                <div class="empty-state">当日暂无日程安排</div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { RefreshRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { assignmentApi, examApi, trainingApi } from '@/api'
import DashboardHeader from '@/components/DashBoard/DashboardHeader.vue'

const userStore = useUserStore()
const date = ref(new Date())
const allEvents = ref([])

// ==================== 计算属性 ====================
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

const getEventsForDay = (dayStr) => {
    return allEvents.value.filter(e => e.date === dayStr)
}

const todayEvents = computed(() => {
    const d = date.value
    const dayStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    return allEvents.value.filter(e => e.date === dayStr)
})

const formatDate = (d) => {
    return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

// ==================== 加载事件 ====================
const fetchEvents = async () => {
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
            const d = (a.deadline || a.endTime || '').split(' ')[0] || ''
            if (d) events.push({ id: `a-${a.id}`, type: 'assignment', typeLabel: '作业', title: a.title, courseName: a.courseName || '', date: d, time: (a.deadline || a.endTime || '').split(' ')[1] || '' })
        })
        extractList(examRes).forEach(e => {
            const d = (e.startTime || e.endTime || '').split(' ')[0] || ''
            if (d) events.push({ id: `ex-${e.id}`, type: 'exam', typeLabel: '考试', title: e.name || e.title, courseName: e.courseName || '', date: d, time: (e.startTime || e.endTime || '').split(' ')[1] || '' })
        })
        extractList(trainRes).forEach(t => {
            const d = (t.deadline || t.endTime || '').split(' ')[0] || ''
            if (d) events.push({ id: `t-${t.id}`, type: 'training', typeLabel: '实训', title: t.name || t.title, courseName: t.courseName || '', date: d, time: (t.deadline || t.endTime || '').split(' ')[1] || '' })
        })
        allEvents.value = events
    } catch (e) { console.error('获取日历事件失败', e) }
}

onMounted(() => { fetchEvents() })
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
}

.calendar-day:hover {
    background: #f0f5ff;
}

.calendar-day.is-today .day-num {
    background: #20c997;
    color: #fff;
    border-radius: 50%;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
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

.dot-assignment {
    background: #409eff;
}

.dot-exam {
    background: #e6a23c;
}

.dot-training {
    background: #67c23a;
}

.panel-title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 12px;
    color: #303133;
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

.event-type-tag {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 4px;
    color: #fff;
    flex-shrink: 0;
}

.tag-assignment {
    background: #409eff;
}

.tag-exam {
    background: #e6a23c;
}

.tag-training {
    background: #67c23a;
}

.event-name {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    flex: 1;
}

.event-course {
    font-size: 12px;
    color: #909399;
}

.event-time {
    font-size: 12px;
    color: #c0c4cc;
}

.empty-state {
    text-align: center;
    color: #c0c4cc;
    padding: 20px 0;
    font-size: 14px;
}
</style>
