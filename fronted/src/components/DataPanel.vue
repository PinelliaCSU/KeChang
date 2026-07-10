<template>
    <div class="data-panel" :style="{ maxWidth: maxWidth }">
        <!-- 标题栏 -->
        <div v-if="$slots.header || title" class="data-panel__header">
            <div class="data-panel__header-left">
                <h3 v-if="title" class="data-panel__title">{{ title }}</h3>
                <p v-if="description" class="data-panel__desc">{{ description }}</p>
            </div>
            <div class="data-panel__header-right">
                <slot name="header" />
            </div>
        </div>

        <!-- 搜索/操作栏 -->
        <div v-if="$slots.toolbar" class="data-panel__toolbar">
            <slot name="toolbar" />
        </div>

        <!-- 主体内容 -->
        <div class="data-panel__body" :class="{ 'data-panel__body--flush': flush }">
            <slot />
        </div>

        <!-- 底部 -->
        <div v-if="$slots.footer" class="data-panel__footer">
            <slot name="footer" />
        </div>
    </div>
</template>

<script setup>
defineProps({
    title: { type: String, default: '' },
    description: { type: String, default: '' },
    maxWidth: { type: String, default: '1200px' },
    flush: { type: Boolean, default: false }
})
</script>

<style scoped>
.data-panel {
    background: var(--bio-paper, #fefefc);
    border-radius: var(--bio-radius-md, 14px);
    box-shadow: var(--bio-shadow-sm, 0 2px 8px rgba(44, 54, 48, 0.06));
    border: 1px solid rgba(163, 177, 138, 0.12);
    width: 100%;
    margin: 0 auto;
    overflow: hidden;
    transition: box-shadow var(--bio-dur-normal, 200ms) var(--bio-ease-out, cubic-bezier(0.25, 0.8, 0.25, 1));
}

/* 标题栏 */
.data-panel__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    padding: 20px 28px 0;
}

.data-panel__header-left {
    flex: 1;
    min-width: 0;
}

.data-panel__title {
    margin: 0;
    font-size: 17px;
    font-weight: 600;
    color: var(--bio-ink, #2c3630);
    line-height: 1.4;
}

.data-panel__desc {
    margin: 4px 0 0;
    font-size: 13px;
    color: var(--bio-ink-muted, #7a8278);
    line-height: 1.5;
}

.data-panel__header-right {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 8px;
}

/* 工具栏 */
.data-panel__toolbar {
    padding: 16px 28px 0;
}

/* 主体内容 */
.data-panel__body {
    padding: 20px 28px 28px;
}

.data-panel__body--flush {
    padding: 0;
}

/* 底部 */
.data-panel__footer {
    padding: 0 28px 20px;
    display: flex;
    justify-content: flex-end;
}
</style>
