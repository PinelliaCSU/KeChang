<template>
  <div class="file-preview">
    <div class="preview-toolbar">
      <div class="file-main">
        <span class="file-badge">{{ typeLabel }}</span>
        <div class="file-info">
          <div class="file-title" :title="fileName">{{ fileName }}</div>
          <div class="file-url" :title="resolvedPreviewUrl">{{ resolvedPreviewUrl }}</div>
        </div>
      </div>
      <div class="toolbar-actions">
        <el-button size="small" @click="openNewWindow">新窗口</el-button>
        <el-button v-if="hasPreviewCopy" size="small" @click="openOriginal">原文件</el-button>
        <el-button size="small" type="primary" plain @click="downloadFile">下载</el-button>
      </div>
    </div>

    <div class="preview-body" v-loading="loading">
      <img v-if="previewType === 'image'" class="image-preview" :src="resolvedPreviewUrl" :alt="fileName" @load="loading = false" @error="loading = false" />
      <iframe v-else-if="previewType === 'pdf'" class="frame-preview" :src="resolvedPreviewUrl" @load="loading = false"></iframe>
      <video v-else-if="previewType === 'video'" class="media-preview" :src="resolvedPreviewUrl" controls @loadeddata="loading = false"></video>
      <audio v-else-if="previewType === 'audio'" class="audio-preview" :src="resolvedPreviewUrl" controls @loadeddata="loading = false"></audio>
      <pre v-else-if="previewType === 'text'" class="text-preview">{{ textContent }}</pre>
      <div v-else class="fallback-preview">
        <div class="fallback-icon">{{ typeLabel }}</div>
        <h4>{{ unsupportedTitle }}</h4>
        <p>{{ unsupportedTip }}</p>
        <div class="fallback-actions">
          <el-button size="small" @click="openNewWindow">新窗口打开</el-button>
          <el-button size="small" type="primary" @click="downloadFile">下载文件</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { uploadApi } from '@/api'
import { getFileExtension, getFileName, resolveFileUrl } from '@/utils/fileUrl'

const props = defineProps({
  url: { type: String, default: '' },
  name: { type: String, default: '' },
  type: { type: String, default: '' }
})

const loading = ref(false)
const textContent = ref('')
const previewUrl = ref('')
const previewFailed = ref(false)

const resolvedOriginalUrl = computed(() => resolveFileUrl(props.url))
const effectivePreviewUrl = computed(() => previewUrl.value || props.url)
const resolvedPreviewUrl = computed(() => resolveFileUrl(effectivePreviewUrl.value))
const hasPreviewCopy = computed(() => previewUrl.value && previewUrl.value !== props.url)
const fileName = computed(() => props.name || getFileName(props.url))
const extension = computed(() => getFileExtension(fileName.value || props.url))
const previewExtension = computed(() => getFileExtension(effectivePreviewUrl.value || fileName.value))

const previewType = computed(() => {
  const ext = previewExtension.value || extension.value
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].includes(ext)) return 'image'
  if (ext === 'pdf' || props.type === 'PDF') return 'pdf'
  if (['mp4', 'webm', 'ogg', 'mov'].includes(ext) || props.type === 'VIDEO') return 'video'
  if (['mp3', 'wav', 'm4a', 'aac'].includes(ext)) return 'audio'
  if (['txt', 'md', 'json', 'xml', 'csv', 'log', 'java', 'js', 'css', 'html'].includes(ext)) return 'text'
  return 'other'
})

const typeLabel = computed(() => {
  const map = { image: '图片', pdf: 'PDF', video: '视频', audio: '音频', text: '文本', other: '文件' }
  return map[previewType.value]
})

const unsupportedTitle = computed(() => {
  if (previewFailed.value) return '预览生成失败'
  if (['doc', 'docx'].includes(extension.value)) return 'Word 文件正在转换为 PDF 预览'
  if (['ppt', 'pptx', 'xls', 'xlsx'].includes(extension.value)) return 'Office 文件暂不支持浏览器内嵌预览'
  return '该文件类型暂不支持内嵌预览'
})

const unsupportedTip = computed(() => {
  if (previewFailed.value) return '当前文件无法生成在线预览，请下载原文件查看。'
  if (['doc', 'docx'].includes(extension.value)) return '转换完成后会显示 PDF 预览，原 Word 文件仍可下载归档。'
  return '可以在新窗口打开，或下载到本地查看。'
})

const loadPreview = async () => {
  previewUrl.value = ''
  previewFailed.value = false
  if (!['doc', 'docx'].includes(extension.value) || !props.url) return
  loading.value = true
  try {
    const res = await uploadApi.preview({ url: props.url, name: fileName.value })
    if (res.data.code === 200) {
      previewUrl.value = res.data.data?.url || ''
      previewFailed.value = !previewUrl.value
    } else {
      previewFailed.value = true
    }
  } catch {
    previewFailed.value = true
  } finally {
    loading.value = false
  }
}

const loadText = async () => {
  textContent.value = ''
  if (previewType.value !== 'text' || !resolvedPreviewUrl.value) return
  loading.value = true
  try {
    const res = await fetch(resolvedPreviewUrl.value)
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    textContent.value = await res.text()
  } catch (e) {
    textContent.value = '文本加载失败，请在新窗口打开或下载后查看。'
  } finally {
    loading.value = false
  }
}

watch(() => [props.url, fileName.value], loadPreview, { immediate: true })

watch(() => [resolvedPreviewUrl.value, previewType.value], () => {
  loading.value = ['image', 'pdf', 'video', 'audio'].includes(previewType.value)
  if (loading.value) {
    window.setTimeout(() => { loading.value = false }, 8000)
  }
  loadText()
}, { immediate: true })

const openNewWindow = () => {
  if (resolvedPreviewUrl.value) window.open(resolvedPreviewUrl.value, '_blank', 'noopener,noreferrer')
}

const openOriginal = () => {
  if (resolvedOriginalUrl.value) window.open(resolvedOriginalUrl.value, '_blank', 'noopener,noreferrer')
}

const downloadFile = () => {
  if (!resolvedOriginalUrl.value) return
  const link = document.createElement('a')
  link.href = resolvedOriginalUrl.value
  link.download = fileName.value
  link.target = '_blank'
  link.rel = 'noopener noreferrer'
  link.click()
}
</script>

<style scoped>
.file-preview { border: 1px solid #e4e7ed; border-radius: 8px; background: #fff; overflow: hidden; }
.preview-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 14px; border-bottom: 1px solid #ebeef5; background: #fafafa; }
.file-main { min-width: 0; display: flex; align-items: center; gap: 10px; }
.file-badge { flex: 0 0 auto; padding: 4px 8px; border-radius: 6px; background: #ecf5ff; color: #409eff; font-size: 12px; }
.file-info { min-width: 0; }
.file-title { color: #303133; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-url { max-width: 560px; margin-top: 2px; color: #909399; font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.toolbar-actions { flex: 0 0 auto; display: flex; gap: 8px; }
.preview-body { min-height: 360px; background: #f5f7fa; }
.image-preview { display: block; max-width: 100%; max-height: 620px; margin: 0 auto; object-fit: contain; }
.frame-preview { width: 100%; height: 620px; border: 0; background: #fff; }
.media-preview { width: 100%; max-height: 620px; background: #111; }
.audio-preview { width: calc(100% - 48px); margin: 120px 24px; }
.text-preview { min-height: 360px; max-height: 620px; margin: 0; padding: 18px; overflow: auto; background: #fff; color: #303133; line-height: 1.7; white-space: pre-wrap; }
.fallback-preview { min-height: 360px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #606266; text-align: center; gap: 10px; padding: 32px; }
.fallback-icon { width: 72px; height: 72px; border-radius: 12px; display: flex; align-items: center; justify-content: center; background: #f0f2f5; color: #909399; font-weight: 700; }
.fallback-preview h4 { margin: 8px 0 0; color: #303133; }
.fallback-preview p { max-width: 520px; margin: 0; color: #909399; line-height: 1.6; }
.fallback-actions { display: flex; gap: 10px; margin-top: 8px; }
</style>
