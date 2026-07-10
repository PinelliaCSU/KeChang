<template>
    <svg class="cover-pattern" viewBox="0 0 320 150" role="img" aria-label="课程封面插画" preserveAspectRatio="xMidYMid meet">
        <rect width="320" height="150" rx="0" fill="#f5f7fa" />
        <path d="M0 118 C54 94 82 138 135 111 C180 88 213 92 320 56 L320 150 L0 150 Z" fill="#e7fbfc" />
        <path d="M42 119 H278" stroke="#d8e1ea" stroke-width="2" />

        <g transform="translate(88 38)">
            <rect x="0" y="18" width="112" height="70" rx="8" fill="#ffffff" stroke="#d8e1ea" stroke-width="3" />
            <rect x="18" y="0" width="76" height="24" rx="6" fill="#ffffff" stroke="#d8e1ea" stroke-width="3" />
            <rect x="20" y="38" width="28" height="8" rx="3" fill="#e9eef3" />
            <rect x="20" y="55" width="42" height="8" rx="3" fill="#e9eef3" />
            <rect x="72" y="37" width="27" height="27" rx="4" fill="#20c4cc" />
            <path d="M77 57 L84 49 L90 55 L94 51 L100 59" fill="none" stroke="#ffffff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M16 20 V2 L27 0 V20 L22 16 Z" fill="#f59e0b" opacity=".9" />
        </g>

        <g transform="translate(55 84)">
            <rect x="0" y="15" width="10" height="28" fill="#8fe7eb" />
            <rect x="12" y="8" width="12" height="35" fill="#20c4cc" opacity=".75" />
            <rect x="27" y="0" width="11" height="43" fill="#8a94a6" opacity=".35" />
            <rect x="-8" y="43" width="56" height="7" fill="#3b465f" opacity=".18" />
        </g>

        <g transform="translate(218 51)" opacity=".95">
            <circle cx="22" cy="22" r="22" fill="#dff8fa" />
            <path d="M11 28 L28 11" stroke="#20c4cc" stroke-width="3" stroke-linecap="round" />
            <path d="M25 11 H30 V16" stroke="#20c4cc" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
        </g>

        <g v-if="extended" opacity=".8">
            <rect x="212" y="112" width="58" height="7" rx="3.5" fill="#d8e1ea" />
            <rect x="212" y="127" width="38" height="5" rx="2.5" fill="#d8e1ea" />
        </g>
    </svg>
</template>

<script setup>
defineProps({
    extended: { type: Boolean, default: false }
})
</script>

<style scoped>
.cover-pattern {
    display: block;
    width: 100%;
    height: 100%;
}
</style>
