<template>
    <div class="code-display" :class="{ fullscreen: fullscreen }">
        <div class="display-header" v-if="fullscreen">
            <h2>{{ session?.courseName }} — {{ session?.title || '课堂签到' }}</h2>
            <el-button @click="$emit('close')" circle><el-icon>
                    <Close />
                </el-icon></el-button>
        </div>

        <div class="display-body">
            <!-- 倒计时 -->
            <div class="countdown-ring">
                <svg class="ring-svg" viewBox="0 0 120 120">
                    <circle class="ring-bg" cx="60" cy="60" r="52" />
                    <circle class="ring-progress" cx="60" cy="60" r="52" :stroke-dasharray="circumference"
                        :stroke-dashoffset="dashOffset" />
                </svg>
                <div class="ring-text">
                    <span class="time-num">{{ countdown.minutes }}</span>
                    <span class="time-sep">:</span>
                    <span class="time-num">{{ countdown.seconds }}</span>
                </div>
            </div>

            <!-- QR码签到 -->
            <template v-if="session?.checkinType === 'QR_CODE'">
                <div class="qr-wrapper">
                    <canvas ref="qrCanvas" class="qr-canvas"></canvas>
                </div>
                <p class="code-hint">请使用手机扫描二维码签到</p>
            </template>

            <!-- 数字码签到 -->
            <div v-if="session?.checkinType !== 'LOCATION'" class="code-box">
                <span v-for="(ch, i) in codeChars" :key="i" class="code-char">{{ ch }}</span>
            </div>
            <p v-if="session?.checkinType === 'NUMERIC_CODE'" class="code-hint">请在手机端输入上方数字码完成签到</p>
            <p v-if="session?.checkinType === 'QR_CODE'" class="code-hint">或输入上方数字码完成签到</p>

            <!-- 位置签到提示 -->
            <div v-if="session?.checkinType === 'LOCATION'" class="loc-info">
                <el-icon :size="28">
                    <Location />
                </el-icon>
                <p>已设定签到位置</p>
                <p class="loc-detail">有效范围：{{ session?.locationRadius || 100 }}米</p>
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
                <el-icon>
                    <SwitchButton />
                </el-icon>结束签到
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

const codeChars = computed(() => {
    const code = props.session?.code || ''
    return code.split('')
})

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
            color: { dark: '#2c3630', light: '#ffffff' }
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
    background: linear-gradient(135deg, #3b4a2e 0%, #4a5c38 40%, #5a6e42 100%);
    padding: 32px;
    color: #fff;
}

.display-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    max-width: 800px;
}

.display-header h2 {
    color: #fff;
    font-size: 20px;
    margin: 0;
}

.display-body {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    flex: 1;
    justify-content: center;
}

/* 倒计时环 */
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
    stroke: rgba(255, 255, 255, .15);
    stroke-width: 6;
}

.ring-progress {
    fill: none;
    stroke: #8cb369;
    stroke-width: 6;
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
    font-size: 28px;
    font-weight: 700;
    font-family: 'JetBrains Mono', monospace;
    color: #fff;
    min-width: 36px;
    text-align: center;
}

.time-sep {
    font-size: 24px;
    color: rgba(255, 255, 255, .6);
}

/* QR */
.qr-wrapper {
    background: #fff;
    padding: 12px;
    border-radius: var(--bio-radius-md, 14px);
}

.qr-canvas {
    display: block;
    width: 200px;
    height: 200px;
}

/* 数字码 */
.code-box {
    display: flex;
    gap: 10px;
}

.code-char {
    width: 48px;
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, .15);
    border-radius: var(--bio-radius-sm, 8px);
    font-size: 32px;
    font-weight: 800;
    color: #fff;
    font-family: 'JetBrains Mono', monospace;
    border: 1.5px solid rgba(255, 255, 255, .25);
}

.code-hint {
    font-size: 13px;
    color: rgba(255, 255, 255, .7);
    margin: 0;
}

.loc-info {
    text-align: center;
    color: #fff;
}

.loc-info p {
    margin: 4px 0;
}

.loc-detail {
    font-size: 13px;
    opacity: .7;
}

.display-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    max-width: 800px;
    gap: 20px;
}

.stats-inline {
    display: flex;
    gap: 32px;
}

.stat-item {
    text-align: center;
}

.stat-num {
    font-size: 28px;
    font-weight: 700;
    display: block;
}

.stat-label {
    font-size: 12px;
    opacity: .7;
}

/* 非全屏模式 */
.code-display:not(.fullscreen) .countdown-ring {
    width: 90px;
    height: 90px;
}

.code-display:not(.fullscreen) .ring-svg {
    width: 90px;
    height: 90px;
}

.code-display:not(.fullscreen) .time-num {
    font-size: 20px;
    min-width: 26px;
    color: var(--bio-ink, #2c3630);
}

.code-display:not(.fullscreen) .time-sep {
    font-size: 18px;
    color: var(--bio-ink-muted, #7a8278);
}

.code-display:not(.fullscreen) .ring-bg {
    stroke: #e8ecdf;
}

.code-display:not(.fullscreen) .code-char {
    width: 36px;
    height: 48px;
    font-size: 24px;
    background: var(--bio-paper, #fff);
    color: var(--bio-ink, #2c3630);
    border-color: #e8ecdf;
}

.code-display:not(.fullscreen) .code-hint {
    color: var(--bio-ink-muted, #7a8278);
}

.code-display:not(.fullscreen) .loc-info {
    color: var(--bio-ink, #2c3630);
}
</style>
