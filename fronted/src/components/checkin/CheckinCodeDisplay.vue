<template>
    <div class="code-display" :class="{ fullscreen }">
        <div class="display-header" v-if="fullscreen">
            <h2>{{ session?.courseName }} - {{ session?.title || '课堂签到' }}</h2>
            <el-button @click="$emit('close')" circle>
                <el-icon><Close /></el-icon>
            </el-button>
        </div>

        <div class="display-body">
            <div class="countdown-ring">
                <svg class="ring-svg" viewBox="0 0 120 120">
                    <circle class="ring-bg" cx="60" cy="60" r="52" />
                    <circle
                        class="ring-progress"
                        cx="60"
                        cy="60"
                        r="52"
                        :stroke-dasharray="circumference"
                        :stroke-dashoffset="dashOffset"
                    />
                </svg>
                <div class="ring-text">
                    <span class="time-num">{{ countdown.minutes }}</span>
                    <span class="time-sep">:</span>
                    <span class="time-num">{{ countdown.seconds }}</span>
                </div>
            </div>

            <template v-if="session?.checkinType === 'QR_CODE'">
                <div class="qr-wrapper">
                    <canvas ref="qrCanvas" class="qr-canvas"></canvas>
                </div>
                <p class="code-hint">请使用手机扫描二维码签到</p>
            </template>

            <div v-if="session?.checkinType !== 'LOCATION'" class="code-box">
                <span v-for="(ch, i) in codeChars" :key="i" class="code-char">{{ ch }}</span>
            </div>
            <p v-if="session?.checkinType === 'NUMERIC_CODE'" class="code-hint">请在手机端输入上方数字码完成签到</p>
            <p v-if="session?.checkinType === 'QR_CODE'" class="code-hint">也可以输入上方数字码完成签到</p>

            <div v-if="session?.checkinType === 'LOCATION'" class="loc-info">
                <el-icon :size="28"><Location /></el-icon>
                <p>已设定签到位置</p>
                <p class="loc-detail">有效范围：{{ session?.locationRadius || 100 }} 米</p>
            </div>
        </div>

        <div class="display-footer" v-if="fullscreen">
            <div class="stats-inline">
                <div class="stat-item">
                    <span class="stat-num">{{ stats?.successCount || 0 }}</span>
                    <span class="stat-label">已签到</span>
                </div>
                <div class="stat-item">
                    <span class="stat-num">{{ stats?.lateCount || 0 }}</span>
                    <span class="stat-label">迟到</span>
                </div>
                <div class="stat-item">
                    <span class="stat-num">{{ stats?.absentCount || 0 }}</span>
                    <span class="stat-label">缺勤</span>
                </div>
                <div class="stat-item">
                    <span class="stat-num">{{ stats?.totalStudents || 0 }}</span>
                    <span class="stat-label">应到</span>
                </div>
            </div>
            <el-button type="danger" @click="$emit('closeCheckin')" :loading="closing">
                <el-icon><SwitchButton /></el-icon>
                结束签到
            </el-button>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { Close, Location, SwitchButton } from '@element-plus/icons-vue'
import QRCode from 'qrcode'

const props = defineProps({
    session: { type: Object, default: null },
    fullscreen: { type: Boolean, default: false },
    stats: { type: Object, default: () => ({}) },
    closing: { type: Boolean, default: false }
})

defineEmits(['close', 'closeCheckin'])

const qrCanvas = ref(null)
const now = ref(new Date())
let timer = null

const codeChars = computed(() => String(props.session?.code || '').split(''))
const circumference = 2 * Math.PI * 52

const countdown = computed(() => {
    if (!props.session?.endTime) return { minutes: '--', seconds: '--', progress: 1 }
    const end = new Date(props.session.endTime)
    const start = new Date(props.session.startTime)
    const total = Math.max(1, end - start)
    const remaining = Math.max(0, end - now.value)
    const progress = remaining / total
    const mins = Math.floor(remaining / 60000)
    const secs = Math.floor((remaining % 60000) / 1000)
    return {
        minutes: String(mins).padStart(2, '0'),
        seconds: String(secs).padStart(2, '0'),
        progress
    }
})

const dashOffset = computed(() => circumference * (1 - countdown.value.progress))

const generateQR = async () => {
    if (!props.session?.code || !qrCanvas.value) return
    const url = `${window.location.origin}/student/checkin?code=${props.session.code}`
    try {
        await QRCode.toCanvas(qrCanvas.value, url, {
            width: 200,
            margin: 1,
            color: { dark: '#1f2937', light: '#ffffff' }
        })
    } catch { /* ignore */ }
}

watch(() => props.session?.code, generateQR)
watch(() => props.session?.checkinType, (type) => {
    if (type === 'QR_CODE') setTimeout(generateQR, 100)
})

onMounted(() => {
    timer = setInterval(() => { now.value = new Date() }, 1000)
    if (props.session?.checkinType === 'QR_CODE') setTimeout(generateQR, 100)
})

onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.code-display {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;
    padding: 24px;
}

.code-display.fullscreen {
    position: fixed;
    inset: 0;
    z-index: 1000;
    background: #3b465f;
    padding: 32px;
    color: #fff;
}

.display-header,
.display-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    max-width: 860px;
}

.display-header h2 {
    color: #fff;
    font-size: 22px;
    font-weight: 500;
    margin: 0;
}

.display-body {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 18px;
    flex: 1;
    justify-content: center;
}

.countdown-ring {
    position: relative;
    width: 120px;
    height: 120px;
}

.ring-svg {
    width: 120px;
    height: 120px;
    transform: rotate(-90deg);
}

.ring-bg {
    fill: none;
    stroke: rgba(255, 255, 255, .18);
    stroke-width: 7;
}

.ring-progress {
    fill: none;
    stroke: #20c4cc;
    stroke-width: 7;
    stroke-linecap: round;
    transition: stroke-dashoffset .3s linear;
}

.ring-text {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 2px;
}

.time-num {
    min-width: 36px;
    color: #fff;
    font-size: 28px;
    font-weight: 700;
    text-align: center;
    font-family: 'JetBrains Mono', Consolas, monospace;
}

.time-sep {
    color: rgba(255, 255, 255, .7);
    font-size: 24px;
}

.qr-wrapper {
    background: #fff;
    padding: 12px;
    border-radius: 0;
}

.qr-canvas {
    display: block;
    width: 200px;
    height: 200px;
}

.code-box {
    display: flex;
    gap: 10px;
}

.code-char {
    width: 50px;
    height: 62px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, .12);
    border: 1px solid rgba(255, 255, 255, .26);
    border-radius: 0;
    color: #fff;
    font-size: 32px;
    font-weight: 800;
    font-family: 'JetBrains Mono', Consolas, monospace;
}

.code-hint,
.loc-detail {
    margin: 0;
    color: rgba(255, 255, 255, .72);
    font-size: 14px;
}

.loc-info {
    text-align: center;
    color: #fff;
}

.stats-inline {
    display: flex;
    gap: 34px;
}

.stat-item {
    text-align: center;
}

.stat-num {
    display: block;
    font-size: 28px;
    font-weight: 700;
}

.stat-label {
    font-size: 12px;
    opacity: .72;
}

.code-display:not(.fullscreen) .countdown-ring,
.code-display:not(.fullscreen) .ring-svg {
    width: 90px;
    height: 90px;
}

.code-display:not(.fullscreen) .time-num {
    min-width: 26px;
    color: #1f2937;
    font-size: 20px;
}

.code-display:not(.fullscreen) .time-sep {
    color: #8a94a6;
    font-size: 18px;
}

.code-display:not(.fullscreen) .ring-bg {
    stroke: #e5e7eb;
}

.code-display:not(.fullscreen) .code-char {
    width: 36px;
    height: 48px;
    color: #1f2937;
    background: #fff;
    border-color: #dcdfe6;
    font-size: 24px;
}

.code-display:not(.fullscreen) .code-hint,
.code-display:not(.fullscreen) .loc-detail,
.code-display:not(.fullscreen) .loc-info {
    color: #4b5563;
}
</style>
