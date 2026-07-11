<template>
    <div class="code-editor-wrapper">
        <!-- 工具栏 -->
        <div class="editor-toolbar">
            <div class="toolbar-left">
                <el-select v-model="currentLanguage" :disabled="readonly || !allowLanguageSwitch" size="small"
                    style="width: 120px" @change="onLanguageChange">
                    <el-option label="C" value="c" />
                    <el-option label="C++" value="cpp" />
                    <el-option label="Python" value="python" />
                </el-select>
                <span v-if="templateAvailable" class="template-hint">
                    <el-button link type="primary" size="small" @click="resetToTemplate">
                        重置模板
                    </el-button>
                </span>
            </div>
            <div class="toolbar-right">
                <el-button v-if="showRunButton" type="primary" size="small" :loading="running" :disabled="!code.trim()"
                    @click="handleRun">
                    <el-icon>
                        <CaretRight />
                    </el-icon>
                    运行测试
                </el-button>
            </div>
        </div>

        <!-- Monaco 编辑器容器 -->
        <div ref="editorContainer" class="editor-container"></div>

        <!-- 运行结果 -->
        <div v-if="showResults && runResults.length" class="run-results">
            <CodeRunPanel :results="runResults" :loading="running" />
        </div>
    </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { CaretRight } from '@element-plus/icons-vue'
import CodeRunPanel from './CodeRunPanel.vue'
import * as monaco from 'monaco-editor'

const props = defineProps({
    modelValue: { type: String, default: '' },
    language: { type: String, default: 'cpp' },       // c / cpp / python
    readonly: { type: Boolean, default: false },
    showRunButton: { type: Boolean, default: false },
    showResults: { type: Boolean, default: false },
    runResults: { type: Array, default: () => [] },
    running: { type: Boolean, default: false },
    templateCode: { type: String, default: '' },
    allowLanguageSwitch: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'run', 'languageChange'])

const editorContainer = ref(null)
const currentLanguage = ref(props.language)
const code = computed(() => props.modelValue || '')
let editor = null

const templateAvailable = ref(!!props.templateCode)

// Monaco 语言映射
const langMap = { c: 'c', cpp: 'cpp', python: 'python' }

function initEditor() {
    if (!editorContainer.value) return

    editor = monaco.editor.create(editorContainer.value, {
        value: props.modelValue || props.templateCode || '',
        language: langMap[currentLanguage.value] || 'cpp',
        theme: 'vs',
        fontSize: 14,
        lineNumbers: 'on',
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        automaticLayout: true,
        tabSize: 4,
        insertSpaces: true,
        readOnly: props.readonly,
        wordWrap: 'on',
        renderWhitespace: 'selection',
        bracketPairColorization: { enabled: true },
        padding: { top: 12, bottom: 12 }
    })

    // 监听内容变化
    editor.onDidChangeModelContent(() => {
        const value = editor.getValue()
        emit('update:modelValue', value)
    })
}

function handleRun() {
    emit('run', {
        code: editor ? editor.getValue() : props.modelValue,
        language: currentLanguage.value
    })
}

function onLanguageChange(lang) {
    if (editor) {
        monaco.editor.setModelLanguage(editor.getModel(), langMap[lang] || 'cpp')
    }
    emit('languageChange', lang)
}

function resetToTemplate() {
    if (editor && props.templateCode) {
        editor.setValue(props.templateCode)
    }
}

// 外部设置代码内容
function setCode(code) {
    if (editor) {
        editor.setValue(code)
    }
}

// 暴露方法给父组件
defineExpose({ setCode })

watch(() => props.language, (val) => {
    currentLanguage.value = val
    if (editor) {
        monaco.editor.setModelLanguage(editor.getModel(), langMap[val] || 'cpp')
    }
})

watch(() => props.modelValue, (val) => {
    if (editor && val !== editor.getValue()) {
        editor.setValue(val)
    }
})

watch(() => props.templateCode, (val) => {
    templateAvailable.value = !!val
})

watch(() => props.readonly, (val) => {
    if (editor) {
        editor.updateOptions({ readOnly: val })
    }
})

onMounted(async () => {
    await nextTick()
    initEditor()
})

onBeforeUnmount(() => {
    if (editor) {
        editor.dispose()
        editor = null
    }
})
</script>

<style scoped>
.code-editor-wrapper {
    border: 1px solid var(--el-border-color);
    border-radius: 8px;
    overflow: hidden;
    background: #fff;
}

.editor-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 12px;
    background: #fafafa;
    border-bottom: 1px solid var(--el-border-color-lighter);
}

.toolbar-left {
    display: flex;
    align-items: center;
    gap: 8px;
}

.toolbar-right {
    display: flex;
    align-items: center;
    gap: 8px;
}

.template-hint {
    font-size: 12px;
    color: var(--el-text-color-secondary);
}

.editor-container {
    height: 400px;
    min-height: 300px;
}

.run-results {
    border-top: 1px solid var(--el-border-color-lighter);
}
</style>
