<template>
    <div class="code-run-panel">
        <!-- 加载状态 -->
        <div v-if="loading" class="run-loading">
            <el-icon class="is-loading">
                <Loading />
            </el-icon>
            <span>编译运行中...</span>
        </div>

        <!-- 结果汇总 -->
        <div v-else class="run-summary">
            <div class="summary-bar" :class="summaryClass">
                <span class="summary-icon">{{ summaryIcon }}</span>
                <span class="summary-text">{{ summaryText }}</span>
                <span class="summary-detail">{{ passedCount }}/{{ totalCount }} 通过</span>
            </div>

            <!-- 逐个用例详情 -->
            <div class="test-cases">
                <div v-for="(result, index) in results" :key="index" class="test-case-item"
                    :class="{ passed: result.passed, failed: !result.passed }">
                    <div class="case-header">
                        <el-icon :size="16">
                            <CircleCheckFilled v-if="result.passed" />
                            <CircleCloseFilled v-else />
                        </el-icon>
                        <span class="case-title">测试用例 {{ index + 1 }}</span>
                        <el-tag :type="result.passed ? 'success' : 'danger'" size="small" effect="plain">
                            {{ result.status || (result.passed ? '通过' : '未通过') }}
                        </el-tag>
                        <span v-if="result.time" class="case-meta">{{ result.time }}ms</span>
                        <span v-if="result.memory" class="case-meta">{{ result.memory }}KB</span>
                    </div>

                    <!-- 输入 -->
                    <div v-if="result.input" class="case-io">
                        <div class="io-label">输入：</div>
                        <pre class="io-content">{{ result.input }}</pre>
                    </div>

                    <!-- 预期输出 -->
                    <div v-if="result.expectedOutput" class="case-io">
                        <div class="io-label">预期输出：</div>
                        <pre class="io-content">{{ result.expectedOutput }}</pre>
                    </div>

                    <!-- 实际输出 -->
                    <div v-if="result.actualOutput !== undefined" class="case-io">
                        <div class="io-label">实际输出：</div>
                        <pre class="io-content"
                            :class="{ 'output-error': !result.passed }">{{ result.actualOutput || '(无输出)' }}</pre>
                    </div>

                    <!-- 错误信息（编译错误/运行时错误） -->
                    <div v-if="result.error" class="case-error">
                        <div class="error-label">错误信息：</div>
                        <pre class="error-content">{{ result.error }}</pre>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { computed } from 'vue'
import { Loading, CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'

const props = defineProps({
    results: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false }
})

const totalCount = computed(() => props.results.length)
const passedCount = computed(() => props.results.filter(r => r.passed).length)
const allPassed = computed(() => totalCount.value > 0 && passedCount.value === totalCount.value)

const summaryClass = computed(() => ({
    'all-passed': allPassed.value,
    'has-failure': !allPassed.value && totalCount.value > 0
}))

const summaryIcon = computed(() => allPassed.value ? '✅' : totalCount.value > 0 ? '❌' : '')
const summaryText = computed(() => {
    if (props.loading) return '运行中...'
    if (totalCount.value === 0) return '暂无结果'
    return allPassed.value ? '全部通过' : '存在未通过的用例'
})
</script>

<style scoped>
.code-run-panel {
    background: #fafbfc;
}

.run-loading {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 16px;
    color: var(--el-text-color-secondary);
    font-size: 14px;
}

.run-summary {
    padding: 0;
}

.summary-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
    font-size: 14px;
    font-weight: 500;
    border-bottom: 1px solid var(--el-border-color-lighter);
}

.summary-bar.all-passed {
    background: #f0fdf4;
    color: #16a34a;
}

.summary-bar.has-failure {
    background: #fef2f2;
    color: #dc2626;
}

.summary-detail {
    margin-left: auto;
    font-size: 12px;
    opacity: 0.8;
}

.test-cases {
    max-height: 400px;
    overflow-y: auto;
}

.test-case-item {
    padding: 12px 16px;
    border-bottom: 1px solid var(--el-border-color-lighter);
}

.test-case-item:last-child {
    border-bottom: none;
}

.test-case-item.passed {
    border-left: 3px solid #16a34a;
}

.test-case-item.failed {
    border-left: 3px solid #dc2626;
}

.case-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
}

.case-header .el-icon {
    color: inherit;
}

.test-case-item.passed .case-header .el-icon {
    color: #16a34a;
}

.test-case-item.failed .case-header .el-icon {
    color: #dc2626;
}

.case-title {
    font-weight: 500;
    font-size: 13px;
}

.case-meta {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-left: 4px;
}

.case-io {
    margin-top: 6px;
}

.io-label {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-bottom: 2px;
}

.io-content {
    background: #f5f5f5;
    border-radius: 4px;
    padding: 6px 10px;
    margin: 0;
    font-size: 13px;
    font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
    white-space: pre-wrap;
    word-break: break-all;
}

.io-content.output-error {
    background: #fef2f2;
    color: #dc2626;
}

.case-error {
    margin-top: 6px;
}

.error-label {
    font-size: 12px;
    color: #dc2626;
    margin-bottom: 2px;
}

.error-content {
    background: #fef2f2;
    border-radius: 4px;
    padding: 6px 10px;
    margin: 0;
    font-size: 12px;
    font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
    white-space: pre-wrap;
    word-break: break-all;
    color: #dc2626;
    max-height: 150px;
    overflow-y: auto;
}
</style>
