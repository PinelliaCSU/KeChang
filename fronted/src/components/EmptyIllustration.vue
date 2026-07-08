<template>
    <div class="empty-state">
        <svg class="empty-illustration" width="120" height="90" viewBox="0 0 120 90" fill="none">
            <!-- 文档矩形底色 -->
            <rect x="20" y="10" width="80" height="70" rx="4" :fill="variant === 'detailed' ? '#eef0e7' : '#e8ecf1'" />

            <!-- 默认/星形/勾选：2行文字线 -->
            <template v-if="variant === 'default' || variant === 'star' || variant === 'check'">
                <rect x="28" y="20" width="28" height="4" rx="2" fill="#d0d5dd" />
                <rect x="28" y="30" width="50" height="3" rx="1.5" fill="#dce1e8" />
            </template>

            <!-- 横线变体（成绩页） -->
            <template v-if="variant === 'lines'">
                <line x1="32" y1="30" x2="88" y2="30" stroke="#d0d5dd" stroke-width="2" />
                <line x1="32" y1="42" x2="75" y2="42" stroke="#dce1e8" stroke-width="1.5" />
            </template>

            <!-- 详细变体（待办空态，含多行+按钮+勾选圈） -->
            <template v-if="variant === 'detailed'">
                <rect x="28" y="20" width="28" height="4" rx="2" fill="#d8ddce" />
                <rect x="28" y="30" width="50" height="3" rx="1.5" fill="#e2e7d8" />
                <rect x="28" y="38" width="42" height="3" rx="1.5" fill="#e2e7d8" />
                <rect x="28" y="46" width="36" height="3" rx="1.5" fill="#e2e7d8" />
                <rect x="28" y="56" width="20" height="6" rx="3" fill="#d0dac0" />
                <circle cx="95" cy="65" r="12" fill="#e8eddf" />
                <path d="M90 65l4 4 8-8" stroke="#8cb369" stroke-width="2" stroke-linecap="round"
                    stroke-linejoin="round" />
            </template>

            <!-- 星形装饰 -->
            <polygon v-if="variant === 'star'" points="60,28 68,48 90,48 73,60 79,80 60,70 41,80 47,60 30,48 52,48"
                fill="#dce1e8" />

            <!-- 勾选圈装饰 -->
            <template v-if="variant === 'check'">
                <circle cx="60" cy="45" r="20" fill="#dce1e8" />
                <path d="M54 45l4 4 8-8" stroke="#b0bec5" stroke-width="2" stroke-linecap="round"
                    stroke-linejoin="round" />
            </template>
        </svg>
        <p class="empty-text">{{ text }}</p>
    </div>
</template>

<script setup>
defineProps({
    /** 变体：default | star | check | detailed | lines */
    variant: { type: String, default: 'default' },
    /** 提示文字 */
    text: { type: String, default: '暂无数据' }
})
</script>

<style scoped>
.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
}

.empty-illustration {
    opacity: 0.6;
}

.empty-text {
    color: #c0c4cc;
    font-size: 14px;
}
</style>
