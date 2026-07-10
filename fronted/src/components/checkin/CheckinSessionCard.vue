<template>
    <div class="session-card" :class="{ active: isActive, clickable: clickable }"
        @click="clickable && $emit('click', session)">
        <div class="card-header">
            <div class="card-type-tag">
                <el-tag :type="typeTagType" size="small" effect="plain">
                    <el-icon :size="14">
                        <component :is="typeIcon" />
                    </el-icon>
                    {{ typeLabel }}
                </el-tag>
            </div>
            <el-tag v-if="isActive" type="danger" size="small" effect="dark">
                <span class="pulsing-dot"></span>签到中
            </el-tag>
            <el-tag v-else-if="session.status === 'ENDED'" type="info" size="small">已结束</el-tag>
        </div>

        <h4 class="card-title">{{ session.title || '课堂签到' }}</h4>
        <p class="card-course">{{ session.courseName }}</p>

        <div class="card-meta">
            <span><el-icon :size="14">
                    <Clock />
                </el-icon>{{ formatTime(session.startTime) }}</span>
            <span v-if="session.endTime"><el-icon :size="14">
                    <Timer />
                </el-icon>{{ countdownText }}</span>
        </div>

        <div v-if="showStats && session.totalStudents" class="card-stats-bar">
            <div class="stat-bar-seg success" :style="{ width: successPercent + '%' }"></div>
            <div class="stat-bar-seg late" :style="{ width: latePercent + '%' }"></div>
            <div class="stat-bar-seg absent" :style="{ width: absentPercent + '%' }"></div>
        </div>
        <div v-if="showStats && session.totalStudents" class="card-stats-text">
            <span>{{ session.checkedInCount || 0 }}/{{ session.totalStudents }} 已签到</span>
            <span v-if="session.successCount != null" class="stat-detail">
                <span class="dot success-dot"></span>{{ session.successCount }}正常
                <span class="dot late-dot"></span>{{ session.lateCount || 0 }}迟到
            </span>
        </div>

        <!-- 学生端：我的签到状态 -->
        <div v-if="session.checkedIn" class="card-my-status success">
            <el-icon>
                <CircleCheckFilled />
            </el-icon>已签到 ({{ statusLabel(session.myStatus) }})
        </div>
        <div v-else-if="session.myStatus === 'ABSENT'" class="card-my-status absent">
            <el-icon>
                <CircleCloseFilled />
            </el-icon>缺勤
        </div>
    </div>
</template>

<script setup>
import { computed } from 'vue'
import {
    Clock, Timer, CircleCheckFilled, CircleCloseFilled,
    PictureFilled, Iphone, Location
} from '@element-plus/icons-vue'

const props = defineProps({
    session: { type: Object, required: true },
    clickable: { type: Boolean, default: true },
    showStats: { type: Boolean, default: false }
})

defineEmits(['click'])

const isActive = computed(() => props.session.status === 'ACTIVE')

const typeConfig = computed(() => {
    const map = {
        QR_CODE: { icon: PictureFilled, label: '二维码签到', tag: 'success' },
        NUMERIC_CODE: { icon: Iphone, label: '数字码签到', tag: '' },
        LOCATION: { icon: Location, label: '位置签到', tag: 'warning' }
    }
    return map[props.session.checkinType] || map.NUMERIC_CODE
})

const typeIcon = computed(() => typeConfig.value.icon)
const typeLabel = computed(() => typeConfig.value.label)
const typeTagType = computed(() => typeConfig.value.tag)

const countdownText = computed(() => {
    if (!props.session.endTime) return ''
    const now = new Date()
    const end = new Date(props.session.endTime)
    if (now > end) return '已截止'
    const diff = Math.max(0, end - now)
    const m = Math.floor(diff / 60000)
    const s = Math.floor((diff % 60000) / 1000)
    if (m > 0) return `剩余${m}分${s}秒`
    return `剩余${s}秒`
})

const successPercent = computed(() => {
    if (!props.session.totalStudents) return 0
    return ((props.session.successCount || 0) / props.session.totalStudents) * 100
})

const latePercent = computed(() => {
    if (!props.session.totalStudents) return 0
    return ((props.session.lateCount || 0) / props.session.totalStudents) * 100
})

const absentPercent = computed(() => {
    if (!props.session.totalStudents) return 0
    const absent = props.session.totalStudents - (props.session.checkedInCount || 0)
    return Math.max(0, (absent / props.session.totalStudents) * 100)
})

const formatTime = (t) => {
    if (!t) return ''
    const d = new Date(t)
    return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const statusLabel = (s) => ({ SUCCESS: '正常', LATE: '迟到', ABSENT: '缺勤' })[s] || s
</script>

<style scoped>
.session-card {
    background: var(--bio-paper, #fff);
    border-radius: var(--bio-radius-md, 14px);
    padding: 16px 20px;
    box-shadow: var(--bio-shadow-sm, 0 1px 4px rgba(0, 0, 0, .06));
    transition: all var(--bio-dur-normal, .15s) var(--bio-ease-out, ease);
    border: 1.5px solid transparent;
}

.session-card.clickable {
    cursor: pointer;
}

.session-card:hover {
    box-shadow: var(--bio-shadow-md, 0 4px 12px rgba(0, 0, 0, .1));
    border-color: var(--bio-primary);
}

.session-card.active {
    border-color: #e8a87c;
    background: #fff;
}

.card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
}

.card-type-tag .el-tag {
    font-size: 12px;
    display: inline-flex;
    align-items: center;
    gap: 4px;
}

.card-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--bio-ink, #2c3630);
    margin: 0 0 2px;
}

.card-course {
    font-size: 13px;
    color: var(--bio-ink-muted, #7a8278);
    margin: 0 0 10px;
}

.card-meta {
    display: flex;
    gap: 16px;
    font-size: 12px;
    color: var(--bio-ink-muted, #7a8278);
}

.card-meta span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
}

.card-stats-bar {
    display: flex;
    height: 6px;
    border-radius: 3px;
    overflow: hidden;
    margin-top: 12px;
    background: #eee;
}

.stat-bar-seg.success {
    background: var(--bio-primary);
}

.stat-bar-seg.late {
    background: var(--bio-warning, #c4956a);
}

.stat-bar-seg.absent {
    background: #e0e0e0;
}

.card-stats-text {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: var(--bio-ink-muted, #7a8278);
    margin-top: 4px;
}

.stat-detail {
    display: inline-flex;
    align-items: center;
    gap: 4px;
}

.dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    display: inline-block;
}

.success-dot {
    background: var(--bio-primary);
}

.late-dot {
    background: var(--bio-warning, #c4956a);
}

.card-my-status {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    margin-top: 10px;
    font-weight: 500;
}

.card-my-status.success {
    color: var(--bio-primary);
}

.card-my-status.absent {
    color: var(--bio-danger, #c4665a);
}

.pulsing-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #fff;
    display: inline-block;
    margin-right: 2px;
    animation: pulse 1.5s infinite;
}

@keyframes pulse {

    0%,
    100% {
        opacity: 1;
    }

    50% {
        opacity: .3;
    }
}
</style>
