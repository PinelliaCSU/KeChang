<template>
    <div class="result-panel">
        <!-- 统计概览 -->
        <div class="stats-overview">
            <div class="stat-card success">
                <span class="stat-num">{{ stats.successCount || 0 }}</span>
                <span class="stat-label">已签到</span>
            </div>
            <div class="stat-card late">
                <span class="stat-num">{{ stats.lateCount || 0 }}</span>
                <span class="stat-label">迟到</span>
            </div>
            <div class="stat-card absent">
                <span class="stat-num">{{ stats.absentCount || 0 }}</span>
                <span class="stat-label">缺勤</span>
            </div>
            <div class="stat-card total">
                <span class="stat-num">{{ stats.totalStudents || 0 }}</span>
                <span class="stat-label">应到</span>
            </div>
        </div>

        <!-- 签到率进度环 -->
        <div class="rate-section" v-if="stats.totalStudents">
            <div class="rate-ring-wrap">
                <svg viewBox="0 0 120 120" class="rate-ring">
                    <circle cx="60" cy="60" r="52" fill="none" stroke="#e8ecdf" stroke-width="8" />
                    <circle cx="60" cy="60" r="52" fill="none" stroke="#6b8e4e" stroke-width="8" stroke-linecap="round"
                        :stroke-dasharray="rateCircumference" :stroke-dashoffset="rateOffset"
                        transform="rotate(-90 60 60)" />
                </svg>
                <div class="rate-text">
                    <span class="rate-pct">{{ (stats.checkinRate || 0).toFixed(0) }}%</span>
                    <span class="rate-sub">签到率</span>
                </div>
            </div>
        </div>

        <!-- 已签到学生名单 -->
        <div v-if="stats.checkedInStudents?.length" class="student-list-section">
            <h4>已签到 ({{ stats.checkedInStudents.length }}人)</h4>
            <div class="student-list">
                <div v-for="s in stats.checkedInStudents" :key="s.studentId" class="student-item">
                    <el-avatar :size="32" :src="s.avatar || ''">{{ (s.realName || s.studentName || '?').charAt(0)
                        }}</el-avatar>
                    <div class="student-info">
                        <span class="s-name">{{ s.realName || s.studentName }}</span>
                        <span class="s-no">{{ s.studentNo || '' }}</span>
                    </div>
                    <el-tag :type="s.status === 'SUCCESS' ? 'success' : 'warning'" size="small">
                        {{ s.status === 'SUCCESS' ? '正常' : '迟到' }}
                    </el-tag>
                    <span class="s-time">{{ formatTime(s.checkinTime) }}</span>
                </div>
            </div>
        </div>

        <!-- 缺勤学生名单 -->
        <div v-if="stats.absentStudents?.length" class="student-list-section">
            <h4>未签到 ({{ stats.absentStudents.length }}人)</h4>
            <div class="student-list">
                <div v-for="s in stats.absentStudents" :key="s.studentId" class="student-item absent-item">
                    <el-avatar :size="32" :src="s.avatar || ''">{{ (s.realName || s.studentName || '?').charAt(0)
                        }}</el-avatar>
                    <div class="student-info">
                        <span class="s-name">{{ s.realName || s.studentName }}</span>
                        <span class="s-no">{{ s.studentNo || '' }}</span>
                    </div>
                    <el-tag type="danger" size="small">缺勤</el-tag>
                </div>
            </div>
        </div>

        <el-empty v-if="!stats.checkedInStudents?.length && !stats.absentStudents?.length" description="暂无签到数据"
            :image-size="80" />
    </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
    stats: { type: Object, default: () => ({}) }
})

const rateCircumference = 2 * Math.PI * 52
const rateOffset = computed(() => {
    const rate = (props.stats.checkinRate || 0) / 100
    return rateCircumference * (1 - rate)
})

const formatTime = (t) => {
    if (!t) return ''
    const d = new Date(t)
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}
</script>

<style scoped>
.result-panel {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.stats-overview {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
}

.stat-card {
    text-align: center;
    padding: 16px 8px;
    border-radius: var(--bio-radius-md, 14px);
    background: var(--bio-paper, #fff);
}

.stat-num {
    font-size: 28px;
    font-weight: 700;
    display: block;
}

.stat-label {
    font-size: 12px;
    color: var(--bio-ink-muted, #7a8278);
}

.stat-card.success .stat-num {
    color: var(--bio-success, #6b8e4e);
}

.stat-card.late .stat-num {
    color: var(--bio-warning, #c4956a);
}

.stat-card.absent .stat-num {
    color: var(--bio-danger, #c4665a);
}

.stat-card.total .stat-num {
    color: var(--bio-ink, #2c3630);
}

.rate-section {
    display: flex;
    justify-content: center;
}

.rate-ring-wrap {
    position: relative;
    width: 100px;
    height: 100px;
}

.rate-ring {
    width: 100px;
    height: 100px;
}

.rate-text {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
}

.rate-pct {
    font-size: 22px;
    font-weight: 700;
    color: var(--bio-moss, #606c38);
}

.rate-sub {
    font-size: 11px;
    color: var(--bio-ink-muted, #7a8278);
}

.student-list-section h4 {
    font-size: 14px;
    font-weight: 600;
    color: var(--bio-ink, #2c3630);
    margin: 0 0 8px;
}

.student-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    max-height: 300px;
    overflow-y: auto;
}

.student-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 12px;
    border-radius: var(--bio-radius-sm, 8px);
    background: var(--bio-paper, #fff);
}

.student-item.absent-item {
    opacity: .65;
    background: #fafafa;
}

.student-info {
    flex: 1;
    min-width: 0;
}

.s-name {
    font-size: 13px;
    font-weight: 500;
    color: var(--bio-ink, #2c3630);
    display: block;
}

.s-no {
    font-size: 11px;
    color: var(--bio-ink-muted, #7a8278);
}

.s-time {
    font-size: 11px;
    color: var(--bio-ink-muted, #7a8278);
    font-family: monospace;
}
</style>
