<template>
    <div class="header-entry">
        <div class="entry-header">
            <h4>课堂签到</h4>
            <el-button v-if="isTeacher" type="primary" size="small" @click="handleCreate">
                <el-icon>
                    <Plus />
                </el-icon>发起签到
            </el-button>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="entry-loading">
            <el-icon class="is-loading">
                <Loading />
            </el-icon>
        </div>

        <!-- 无活跃签到 -->
        <div v-else-if="!sessions.length" class="entry-empty">
            <p>{{ isTeacher ? '暂无活跃签到' : '当前没有可签到的场次' }}</p>
        </div>

        <!-- 签到列表 -->
        <div v-else class="entry-list">
            <div v-for="s in sessions" :key="s.id" class="entry-item"
                :class="{ urgent: isUrgent(s), checked: s.checkedIn }" @click="handleClick(s)">
                <div class="entry-item-left">
                    <span class="entry-course">{{ s.courseName }}</span>
                    <span class="entry-title">{{ s.title }}</span>
                </div>
                <div class="entry-item-right">
                    <span class="entry-countdown" :class="{ late: s.isLateWindow }">
                        {{ countdownShort(s.endTime) }}
                    </span>
                    <el-tag v-if="s.checkedIn" type="success" size="small">已签</el-tag>
                    <el-tag v-else-if="s.isLateWindow" type="warning" size="small">迟到</el-tag>
                </div>
            </div>
        </div>

        <!-- 底部操作 -->
        <div class="entry-footer">
            <a href="javascript:;" @click="goToFullPage">
                查看全部 <el-icon>
                    <ArrowRight />
                </el-icon>
            </a>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Loading, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { checkinApi, courseApi } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const sessions = ref([])
const loading = ref(false)

const isTeacher = computed(() => userStore.isTeacher)

const fetchSessions = async () => {
    loading.value = true
    try {
        if (isTeacher.value) {
            const res = await checkinApi.teacherList({})
            const data = res.data.data || []
            sessions.value = data.filter(s => s.status === 'ACTIVE').slice(0, 5)
        } else {
            const res = await checkinApi.studentActiveList()
            sessions.value = (res.data.data || []).slice(0, 5)
        }
    } catch { sessions.value = [] }
    finally { loading.value = false }
}

const isUrgent = (s) => {
    if (!s.endTime) return false
    const remaining = new Date(s.endTime) - new Date()
    return remaining > 0 && remaining < 60000 // 1分钟内
}

const countdownShort = (endTime) => {
    if (!endTime) return ''
    const remaining = Math.max(0, new Date(endTime) - new Date())
    if (remaining <= 0) return '已截止'
    const m = Math.floor(remaining / 60000)
    const s = Math.floor((remaining % 60000) / 1000)
    if (m > 0) return `${m}分${s}秒`
    return `${s}秒`
}

const handleCreate = () => {
    router.push('/teacher/checkin?action=create')
}

const handleClick = (session) => {
    if (isTeacher.value) {
        router.push('/teacher/checkin')
    } else {
        router.push(`/student/checkin?sessionId=${session.id}`)
    }
}

const goToFullPage = () => {
    const path = isTeacher.value ? '/teacher/checkin' : '/student/checkin'
    router.push(path)
}

defineExpose({ fetchSessions })

onMounted(() => { fetchSessions() })
</script>

<style scoped>
.header-entry {
    min-width: 280px;
    max-width: 360px;
}

.entry-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 12px;
    border-bottom: 1px solid #e8ecdf;
    margin-bottom: 8px;
}

.entry-header h4 {
    font-size: 14px;
    font-weight: 600;
    color: var(--bio-ink, #2c3630);
    margin: 0;
}

.entry-loading {
    text-align: center;
    padding: 24px;
    color: var(--bio-ink-muted, #7a8278);
}

.entry-empty {
    text-align: center;
    padding: 20px;
}

.entry-empty p {
    font-size: 13px;
    color: var(--bio-ink-muted, #7a8278);
    margin: 0;
}

.entry-list {
    display: flex;
    flex-direction: column;
    gap: 6px;
    max-height: 300px;
    overflow-y: auto;
}

.entry-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 12px;
    border-radius: var(--bio-radius-sm, 8px);
    cursor: pointer;
    transition: background .15s ease;
    border: 1.5px solid transparent;
}

.entry-item:hover {
    background: #f8faf5;
    border-color: #e8ecdf;
}

.entry-item.urgent {
    border-color: #e8a87c;
    background: #fef9f5;
}

.entry-item.checked {
    opacity: .65;
}

.entry-item-left {
    display: flex;
    flex-direction: column;
    min-width: 0;
}

.entry-course {
    font-size: 12px;
    color: var(--bio-ink-muted, #7a8278);
}

.entry-title {
    font-size: 13px;
    font-weight: 500;
    color: var(--bio-ink, #2c3630);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.entry-item-right {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
}

.entry-countdown {
    font-size: 12px;
    color: var(--bio-moss, #606c38);
    font-weight: 500;
    white-space: nowrap;
}

.entry-countdown.late {
    color: var(--bio-warning, #c4956a);
}

.entry-footer {
    padding-top: 10px;
    border-top: 1px solid #e8ecdf;
    margin-top: 8px;
    text-align: center;
}

.entry-footer a {
    font-size: 12px;
    color: var(--bio-moss, #606c38);
    text-decoration: none;
    display: inline-flex;
    align-items: center;
    gap: 4px;
}

.entry-footer a:hover {
    filter: brightness(1.15);
}
</style>
