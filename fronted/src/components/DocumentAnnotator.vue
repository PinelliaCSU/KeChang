<template>
  <div class="pdf-editor">
    <div class="editor-toolbar">
      <div class="file-main">
        <span class="file-badge">PDF</span>
        <div class="file-info">
          <div class="file-title" :title="fileName">{{ fileName }}</div>
          <div class="file-hint" v-if="!readonly">左键打对钩，右键打错号；滚动到哪一页就在哪一页批注。</div>
          <div class="file-hint" v-else>教师生成的批注结果</div>
        </div>
      </div>

      <div class="toolbar-actions">
        <template v-if="!readonly">
          <el-button size="small" :type="textMode ? 'primary' : 'default'" plain @click="textMode = !textMode">文字</el-button>
          <el-input v-if="textMode" v-model="draftText" size="small" class="draft-input" placeholder="左键贴文字" />
          <span class="hint">已批注 {{ currentFileMarks.length }} 处</span>
        </template>
        <el-button size="small" @click="openPreviewPdf">PDF预览</el-button>
        <el-button v-if="hasPreviewCopy" size="small" @click="openOriginal">原文件</el-button>
        <el-button size="small" type="primary" plain @click="downloadOriginal">下载原文件</el-button>
      </div>
    </div>

    <div class="editor-body" v-loading="loading">
      <el-empty v-if="!loading && !pages.length" description="暂时无法生成 PDF 编辑页面" :image-size="86" />
      <div
        v-for="page in pages"
        :key="page.page"
        class="page-card"
      >
        <div class="page-label">第 {{ page.page }} 页</div>
        <div
          class="page-canvas"
          @click.left="addMark($event, page, textMode ? 'text' : 'check')"
          @contextmenu.prevent="addMark($event, page, 'wrong')"
        >
          <img class="page-image" :src="resolveFileUrl(page.url)" :alt="`第${page.page}页`" />
          <div class="mark-layer">
            <div
              v-for="mark in marksForPage(page.page)"
              :key="mark.id"
              class="mark-sticker"
              :class="`mark-${mark.type}`"
              :style="{ left: `${mark.x}%`, top: `${mark.y}%` }"
              @click.stop
              @contextmenu.prevent.stop
            >
              <span class="mark-symbol">{{ markSymbol(mark.type) }}</span>
              <input
                v-if="mark.type === 'text' && !readonly"
                class="mark-text-input"
                :value="mark.text"
                placeholder="文字批注"
                @input="updateMarkText(mark, $event.target.value)"
              />
              <span v-else-if="mark.type === 'text'" class="mark-text">{{ mark.text || '文字批注' }}</span>
              <button v-if="!readonly" class="mark-delete" type="button" @click.stop="removeMark(mark.id)">删除</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { uploadApi } from '@/api'
import { getFileName, resolveFileUrl } from '@/utils/fileUrl'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  url: { type: String, default: '' },
  name: { type: String, default: '' },
  fileKey: { type: String, default: '' },
  readonly: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])

const loading = ref(false)
const pages = ref([])
const previewPdfUrl = ref('')
const textMode = ref(false)
const draftText = ref('')

const fileName = computed(() => props.name || getFileName(props.url))
const currentFileKey = computed(() => props.fileKey || props.url || fileName.value)
const hasPreviewCopy = computed(() => previewPdfUrl.value && previewPdfUrl.value !== props.url)
const currentFileMarks = computed(() => props.modelValue.filter(isCurrentFileMark))

const isCurrentFileMark = (mark) => {
  if (!mark) return false
  if (!mark.fileKey && !mark.fileUrl) return currentFileKey.value === (props.url || fileName.value)
  return mark.fileKey === currentFileKey.value || mark.fileUrl === props.url
}

const marksForPage = (page) => currentFileMarks.value.filter(mark => (mark.page || 1) === page)

const loadPages = async () => {
  pages.value = []
  previewPdfUrl.value = ''
  if (!props.url) return
  loading.value = true
  try {
    const res = await uploadApi.pdfPages({ url: props.url, name: fileName.value })
    if (res.data.code === 200) {
      previewPdfUrl.value = res.data.data?.pdfUrl || props.url
      pages.value = res.data.data?.pages || []
    }
  } finally {
    loading.value = false
  }
}

watch(() => [props.url, fileName.value], loadPages, { immediate: true })

const addMark = (event, page, type) => {
  if (props.readonly) return
  const rect = event.currentTarget.getBoundingClientRect()
  const x = Math.max(0, Math.min(100, ((event.clientX - rect.left) / rect.width) * 100))
  const y = Math.max(0, Math.min(100, ((event.clientY - rect.top) / rect.height) * 100))
  const mark = {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    type,
    text: type === 'text' ? draftText.value : '',
    x: Number(x.toFixed(2)),
    y: Number(y.toFixed(2)),
    page: page.page,
    fileKey: currentFileKey.value,
    fileUrl: props.url,
    fileName: fileName.value
  }
  emit('update:modelValue', [...props.modelValue, mark])
}

const removeMark = (id) => {
  emit('update:modelValue', props.modelValue.filter(mark => mark.id !== id))
}

const updateMarkText = (targetMark, text) => {
  emit('update:modelValue', props.modelValue.map(mark => mark.id === targetMark.id ? { ...mark, text } : mark))
}

const markSymbol = (type) => {
  if (type === 'check') return '✓'
  if (type === 'wrong') return '×'
  return '批'
}

const openPreviewPdf = () => {
  window.open(resolveFileUrl(previewPdfUrl.value || props.url), '_blank', 'noopener,noreferrer')
}

const openOriginal = () => {
  window.open(resolveFileUrl(props.url), '_blank', 'noopener,noreferrer')
}

const downloadOriginal = () => {
  const link = document.createElement('a')
  link.href = resolveFileUrl(props.url)
  link.download = fileName.value
  link.target = '_blank'
  link.rel = 'noopener noreferrer'
  link.click()
}
</script>

<style scoped>
.pdf-editor {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.editor-toolbar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #e4e7ed;
  background: rgba(255, 255, 255, .96);
}

.file-main {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.file-badge {
  flex: 0 0 auto;
  padding: 4px 8px;
  border-radius: 6px;
  background: var(--bio-primary-soft);
  color: var(--bio-primary);
  font-size: 12px;
  font-weight: 700;
}

.file-info {
  min-width: 0;
}

.file-title {
  color: #303133;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-hint {
  margin-top: 2px;
  color: #909399;
  font-size: 12px;
}

.toolbar-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.draft-input {
  width: 180px;
}

.hint {
  color: #909399;
  font-size: 12px;
}

.editor-body {
  max-height: 68vh;
  overflow: auto;
  padding: 18px;
  background: #eef1f5;
}

.page-card {
  width: min(920px, 100%);
  margin: 0 auto 18px;
}

.page-label {
  margin-bottom: 8px;
  color: #606266;
  font-size: 13px;
  font-weight: 600;
}

.page-canvas {
  position: relative;
  background: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, .14);
  cursor: crosshair;
}

.page-image {
  display: block;
  width: 100%;
  height: auto;
  user-select: none;
  pointer-events: none;
}

.mark-layer {
  position: absolute;
  inset: 0;
}

.mark-sticker {
  position: absolute;
  transform: translate(-50%, -50%);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  padding: 4px 7px;
  border-radius: 8px;
  background: rgba(255, 255, 255, .95);
  box-shadow: 0 6px 14px rgba(0, 0, 0, .14);
}

.mark-symbol {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  font-size: 18px;
  font-weight: 800;
}

.mark-check .mark-symbol {
  background: #ecf9e8;
  color: #47a447;
}

.mark-wrong .mark-symbol {
  background: #fff0f0;
  color: #f05252;
}

.mark-text .mark-symbol {
  background: var(--bio-primary-soft);
  color: var(--bio-primary);
}

.mark-text-input {
  width: 160px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 5px 8px;
  outline: none;
}

.mark-text {
  color: #303133;
  font-size: 13px;
}

.mark-delete {
  border: 0;
  background: transparent;
  color: #f56c6c;
  cursor: pointer;
  font-size: 12px;
}

@media (max-width: 900px) {
  .editor-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-actions {
    flex-wrap: wrap;
  }
}
</style>
