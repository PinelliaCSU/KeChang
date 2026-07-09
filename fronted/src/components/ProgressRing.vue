<template>
    <div class="progress-ring-wrapper">
        <svg class="progress-ring" :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`">
            <!-- 背景圆环 -->
            <circle :cx="center" :cy="center" :r="radius" fill="none" stroke="#e8ecf1" :stroke-width="strokeWidth" />
            <!-- 进度弧 -->
            <circle :cx="center" :cy="center" :r="radius" fill="none" :stroke="`url(#${gradientId})`"
                :stroke-width="strokeWidth" stroke-linecap="round" :stroke-dasharray="circumference"
                :stroke-dashoffset="dashOffset" :transform="`rotate(-90 ${center} ${center})`" class="progress-arc" />
            <defs>
                <linearGradient :id="gradientId" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stop-color="#8cb369" />
                    <stop offset="100%" stop-color="#5a7040" />
                </linearGradient>
            </defs>
        </svg>
        <div class="progress-text">
            <span class="progress-percent">{{ percent }}%</span>
        </div>
    </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
    /** 进度百分比 0-100 */
    percent: { type: Number, default: 0 },
    /** SVG 画布尺寸 */
    size: { type: Number, default: 140 },
    /** 圆环粗细 */
    strokeWidth: { type: Number, default: 10 }
})

const center = computed(() => props.size / 2)
const radius = computed(() => props.size / 2 - props.strokeWidth - 2)
const circumference = computed(() => 2 * Math.PI * radius.value)
const dashOffset = computed(() => circumference.value - (props.percent / 100) * circumference.value)

// 唯一渐变 ID，使用稳定随机字符串防止多实例冲突
const instanceId = `pgr_${Math.random().toString(36).slice(2, 10)}`
const gradientId = computed(() => `progressGrad_${instanceId}`)
</script>

<style scoped>
.progress-ring-wrapper {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
}

.progress-ring {
    display: block;
}

.progress-arc {
    transition: stroke-dashoffset 0.8s ease;
}

.progress-text {
    position: absolute;
    display: flex;
    align-items: center;
    justify-content: center;
}

.progress-percent {
    font-size: 24px;
    font-weight: 700;
    color: #303133;
}
</style>
