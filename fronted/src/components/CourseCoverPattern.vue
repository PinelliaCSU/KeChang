<template>
    <svg class="cover-pattern" width="100%" height="100%" viewBox="0 0 200 100" preserveAspectRatio="none">
        <circle cx="160" cy="20" r="40" fill="rgba(255,255,255,0.08)" />
        <circle cx="30" cy="70" r="55" fill="rgba(255,255,255,0.05)" />
        <rect v-if="extended" x="120" y="60" width="60" height="6" rx="3" fill="rgba(255,255,255,0.12)" />
        <rect v-if="extended" x="120" y="72" width="40" height="4" rx="2" fill="rgba(255,255,255,0.08)" />
    </svg>
</template>

<script setup>
defineProps({
    /** 是否显示扩展装饰（Dashboard 课程卡片使用） */
    extended: { type: Boolean, default: false }
})
</script>
