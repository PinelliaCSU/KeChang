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
      <img
        v-if="previewType === 'image'"
        class="image-preview"
        :src="resolvedPreviewUrl"
        :alt="fileName"
        @load="loading = false"
        @error="loading = false"
      />
      <iframe v-else-if="previewType === 'pdf'" class="frame-preview" :src="resolvedPreviewUrl" @load="loading = false"></iframe>

      <div v-else-if="previewType === 'video'" class="video-workspace">
        <div class="video-stage">
          <video
            ref="videoRef"
            class="media-preview"
            :src="resolvedPreviewUrl"
            controls
            @loadeddata="onVideoLoaded"
            @timeupdate="onVideoTimeUpdate"
          ></video>
          <div class="video-meta">
            <span>当前 {{ formatTime(currentTime) }}</span>
            <span v-if="duration">总时长 {{ formatTime(duration) }}</span>
          </div>
        </div>

        <aside class="annotation-panel">
          <div class="annotation-head">
            <div>
              <h4>视频批注</h4>
              <p>批注会绑定当前播放位置，点击时间可跳转。</p>
            </div>
            <el-tag effect="plain">{{ annotations.length }} 条</el-tag>
          </div>

          <div class="annotation-compose">
            <button type="button" class="time-chip active" @click="seekTo(currentTime)">
              {{ formatTime(currentTime) }}
            </button>
            <el-input
              v-model="draftAnnotation"
              type="textarea"
              :rows="3"
              maxlength="240"
              show-word-limit
              placeholder="写下这个时间点的想法、问题或重点"
              @keydown.ctrl.enter.prevent="addAnnotation"
            />
            <el-button type="primary" :disabled="!draftAnnotation.trim()" @click="addAnnotation">
              添加批注
            </el-button>
          </div>

          <div v-if="annotations.length" class="annotation-list">
            <div v-for="note in sortedAnnotations" :key="note.id" class="annotation-item">
              <button type="button" class="time-chip" @click="seekTo(note.time)">
                {{ formatTime(note.time) }}
              </button>
              <div class="annotation-content">
                <p>{{ note.text }}</p>
                <span>{{ note.author || '我' }} · {{ formatDate(note.createdAt) }}</span>
              </div>
              <button type="button" class="delete-note" @click="removeAnnotation(note.id)">删除</button>
            </div>
          </div>
          <el-empty v-else description="暂无批注，播放到重点处添加一条" :image-size="80" />
        </aside>
      </div>

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
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { uploadApi, videoAnnotationApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { getFileExtension, getFileName, resolveFileUrl } from '@/utils/fileUrl'

const props = defineProps({
  url: { type: String, default: '' },
  name: { type: String, default: '' },
  type: { type: String, default: '' }
})

const userStore = useUserStore()

const loading = ref(false)
const textContent = ref('')
const previewUrl = ref('')
const previewFailed = ref(false)
const videoRef = ref(null)
const currentTime = ref(0)
const duration = ref(0)
const draftAnnotation = ref('')
const annotations = ref([])
let annotationSaveTimer = null

const resolvedOriginalUrl = computed(() => resolveFileUrl(props.url))
const effectivePreviewUrl = computed(() => previewUrl.value || props.url)
const resolvedPreviewUrl = computed(() => resolveFileUrl(effectivePreviewUrl.value))
const hasPreviewCopy = computed(() => previewUrl.value && previewUrl.value !== props.url)
const fileName = computed(() => props.name || getFileName(props.url))
const extension = computed(() => getFileExtension(fileName.value || props.url))
const previewExtension = computed(() => getFileExtension(effectivePreviewUrl.value || fileName.value))
const annotationFileKey = computed(() => props.url || fileName.value || resolvedPreviewUrl.value)
const annotationKey = computed(() => {
  const userKey = userStore.userId || userStore.username || 'guest'
  return `kechang_video_annotations:${userKey}:${annotationFileKey.value}`
})

const previewType = computed(() => {
  const ext = previewExtension.value || extension.value
  const rawType = String(props.type || '').toLowerCase()
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].includes(ext)) return 'image'
  if (ext === 'pdf' || rawType === 'pdf') return 'pdf'
  if (['mp4', 'webm', 'ogg', 'mov', 'm4v'].includes(ext) || rawType === 'video' || rawType.includes('视频')) return 'video'
  if (['mp3', 'wav', 'm4a', 'aac'].includes(ext)) return 'audio'
  if (['txt', 'md', 'json', 'xml', 'csv', 'log', 'java', 'js', 'css', 'html'].includes(ext)) return 'text'
  return 'other'
})

const sortedAnnotations = computed(() => [...annotations.value].sort((a, b) => a.time - b.time))

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

const readLocalAnnotations = () => {
  try {
    const raw = localStorage.getItem(annotationKey.value)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

const loadAnnotations = async () => {
  annotations.value = []
  if (previewType.value !== 'video' || !annotationFileKey.value) return
  try {
    const res = await videoAnnotationApi.list(annotationFileKey.value)
    if (res.data.code === 200) {
      const serverAnnotations = Array.isArray(res.data.data) ? res.data.data : []
      const localAnnotations = readLocalAnnotations()
      annotations.value = serverAnnotations.length ? serverAnnotations : localAnnotations
      localStorage.setItem(annotationKey.value, JSON.stringify(annotations.value))
      if (!serverAnnotations.length && localAnnotations.length) {
        await syncAnnotationsToServer(localAnnotations)
      }
      return
    }
  } catch {
    // Keep preview usable if the API is temporarily unavailable.
  }
  annotations.value = readLocalAnnotations()
}

const syncAnnotationsToServer = async (payload) => {
  if (!annotationFileKey.value) return
  try {
    await videoAnnotationApi.save(annotationFileKey.value, payload)
  } catch {
    // Local cache is already written; the next successful save will resync.
  }
}

const saveAnnotations = () => {
  if (previewType.value !== 'video' || !annotationFileKey.value) return
  const payload = sortedAnnotations.value
  localStorage.setItem(annotationKey.value, JSON.stringify(payload))
  if (annotationSaveTimer) window.clearTimeout(annotationSaveTimer)
  annotationSaveTimer = window.setTimeout(async () => {
    await syncAnnotationsToServer(payload)
    annotationSaveTimer = null
  }, 250)
}

watch(() => [props.url, fileName.value], loadPreview, { immediate: true })

watch(() => [resolvedPreviewUrl.value, previewType.value], () => {
  loading.value = ['image', 'pdf', 'video', 'audio'].includes(previewType.value)
  if (loading.value) {
    window.setTimeout(() => { loading.value = false }, 8000)
  }
  currentTime.value = 0
  duration.value = 0
  loadText()
  loadAnnotations()
}, { immediate: true })

const onVideoLoaded = () => {
  loading.value = false
  duration.value = Number(videoRef.value?.duration || 0)
}

const onVideoTimeUpdate = () => {
  currentTime.value = Number(videoRef.value?.currentTime || 0)
}

const addAnnotation = () => {
  const text = draftAnnotation.value.trim()
  if (!text) return
  const note = {
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    time: Math.max(0, Math.floor(currentTime.value || 0)),
    text,
    author: userStore.nickname || userStore.username || '我',
    createdAt: new Date().toISOString()
  }
  annotations.value = [...annotations.value, note]
  draftAnnotation.value = ''
  saveAnnotations()
}

const removeAnnotation = (id) => {
  annotations.value = annotations.value.filter(note => note.id !== id)
  saveAnnotations()
}

const seekTo = async (time) => {
  if (!videoRef.value) return
  videoRef.value.currentTime = Math.max(0, Number(time || 0))
  currentTime.value = videoRef.value.currentTime
  await videoRef.value.play().catch(() => {})
}

const formatTime = (seconds) => {
  const total = Math.max(0, Math.floor(Number(seconds || 0)))
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  const pad = (n) => String(n).padStart(2, '0')
  return h ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`
}

const formatDate = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getMonth() + 1}-${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

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

onBeforeUnmount(() => {
  if (annotationSaveTimer) {
    window.clearTimeout(annotationSaveTimer)
    syncAnnotationsToServer(sortedAnnotations.value)
  }
})
</script>

<style scoped>
.file-preview {
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--bio-border, #dcdfe6);
  border-radius: 10px;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--bio-border, #dcdfe6);
  background: #fafafa;
}

.file-main {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 10px;
}

.file-badge {
  flex: 0 0 auto;
  padding: 5px 10px;
  border-radius: 4px;
  background: var(--bio-primary-soft, #e7fbfc);
  color: var(--bio-primary, #20c4cc);
  font-size: 13px;
  font-weight: 700;
}

.file-info {
  min-width: 0;
}

.file-title {
  overflow: hidden;
  color: var(--bio-ink, #1f2937);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-url {
  max-width: 560px;
  margin-top: 2px;
  overflow: hidden;
  color: var(--bio-ink-muted, #8a94a6);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toolbar-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
}

.preview-body {
  min-height: 360px;
  background: #f5f7fa;
}

.image-preview {
  display: block;
  max-width: 100%;
  max-height: 620px;
  margin: 0 auto;
  object-fit: contain;
}

.frame-preview {
  width: 100%;
  height: 620px;
  border: 0;
  background: #fff;
}

.video-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  min-height: 520px;
  background: #fff;
}

.video-stage {
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #111827;
}

.media-preview {
  width: 100%;
  max-height: 620px;
  min-height: 420px;
  background: #111827;
}

.video-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  color: #d1d5db;
  font-size: 13px;
}

.annotation-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border-left: 1px solid var(--bio-border, #dcdfe6);
  background: #fff;
}

.annotation-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px;
  border-bottom: 1px solid var(--bio-border, #dcdfe6);
}

.annotation-head h4 {
  margin: 0;
  color: var(--bio-ink, #1f2937);
  font-size: 18px;
}

.annotation-head p {
  margin: 6px 0 0;
  color: var(--bio-ink-muted, #8a94a6);
  font-size: 13px;
  line-height: 1.45;
}

.annotation-compose {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--bio-border, #dcdfe6);
}

.annotation-list {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  padding: 14px 18px 18px;
  overflow: auto;
}

.annotation-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: start;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--bio-border, #dcdfe6);
  background: #fff;
}

.time-chip {
  min-width: 54px;
  height: 28px;
  padding: 0 8px;
  border: 1px solid var(--bio-primary, #20c4cc);
  border-radius: 4px;
  background: #fff;
  color: var(--bio-primary, #20c4cc);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
  cursor: pointer;
}

.time-chip:hover,
.time-chip.active {
  background: var(--bio-primary, #20c4cc);
  color: #fff;
}

.annotation-content {
  min-width: 0;
}

.annotation-content p {
  margin: 0;
  color: var(--bio-ink, #1f2937);
  font-size: 14px;
  line-height: 1.55;
  word-break: break-word;
}

.annotation-content span {
  display: block;
  margin-top: 4px;
  color: var(--bio-ink-muted, #8a94a6);
  font-size: 12px;
}

.delete-note {
  border: 0;
  background: transparent;
  color: var(--bio-danger, #ef476f);
  cursor: pointer;
  font-size: 13px;
}

.audio-preview {
  width: calc(100% - 48px);
  margin: 120px 24px;
}

.text-preview {
  min-height: 360px;
  max-height: 620px;
  margin: 0;
  padding: 18px;
  overflow: auto;
  background: #fff;
  color: var(--bio-ink, #1f2937);
  line-height: 1.7;
  white-space: pre-wrap;
}

.fallback-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  padding: 32px;
  color: var(--bio-ink-soft, #4b5563);
  text-align: center;
  gap: 10px;
}

.fallback-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 10px;
  background: #f0f2f5;
  color: var(--bio-ink-muted, #8a94a6);
  font-weight: 700;
}

.fallback-preview h4 {
  margin: 8px 0 0;
  color: var(--bio-ink, #1f2937);
}

.fallback-preview p {
  max-width: 520px;
  margin: 0;
  color: var(--bio-ink-muted, #8a94a6);
  line-height: 1.6;
}

.fallback-actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

@media (max-width: 980px) {
  .preview-toolbar,
  .annotation-head {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions {
    flex-wrap: wrap;
  }

  .video-workspace {
    grid-template-columns: 1fr;
  }

  .annotation-panel {
    border-top: 1px solid var(--bio-border, #dcdfe6);
    border-left: 0;
  }
}
</style>
