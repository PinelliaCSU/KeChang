<template>
  <div class="rag-page">
    <div class="page-head">
      <div>
        <h2>课程智能知识库</h2>
        <p>{{ isTeacher ? '维护课程资料沉淀，支持学生基于课程内容智能答疑' : '从已选课程知识库中提问，答案会附带参考来源' }}</p>
      </div>
      <el-button v-if="isTeacher && activeCourse" type="primary" :loading="rebuilding" :disabled="stats?.storageReady === false" @click="rebuild">
        重建知识库
      </el-button>
    </div>

    <div class="rag-layout">
      <aside class="course-panel">
        <div class="panel-title">课程范围</div>
        <div v-if="courses.length" class="course-list">
          <button v-for="course in courses" :key="course.courseId" type="button" class="course-item"
            :class="{ active: activeCourseId === course.courseId }" @click="selectCourse(course.courseId)">
            <strong>{{ course.courseName }}</strong>
            <span>来源 {{ sourceTotal(course) }} 项 · 入库 {{ course.documentCount || 0 }} 文档</span>
          </button>
        </div>
        <el-empty v-else description="暂无可访问课程" :image-size="80" />
      </aside>

      <section class="chat-panel">
        <div class="chat-head">
          <div>
            <h3>{{ activeCourse?.courseName || '请选择课程' }}</h3>
            <p>{{ activeSourceText }}</p>
          </div>
          <el-tag v-if="stats" effect="plain" :type="stats.storageReady === false ? 'warning' : ''">
            {{ stats.storageReady === false ? '未初始化' : `${stats.readyDocumentCount || 0} 已就绪` }}
          </el-tag>
        </div>

        <el-alert v-if="stats?.storageReady === false" class="storage-alert" type="warning" show-icon :closable="false"
          title="课程知识库表还没有初始化，请先执行 database/migration_add_course_knowledge_rag.sql，再重建知识库。" />

        <div class="answer-box" v-loading="asking">
          <div v-if="answer" class="answer-content" v-html="renderMarkdown(answer)"></div>
          <el-empty v-else description="选择课程后输入问题，开始课程知识库问答" :image-size="96" />
        </div>

        <div v-if="references.length" class="reference-list">
          <div class="reference-title">参考来源</div>
          <div v-for="ref in references" :key="ref.chunkId" class="reference-item">
            <div>
              <strong>{{ ref.title }}</strong>
              <span>{{ sourceLabel(ref.sourceType) }} · 相似度 {{ Math.round((ref.score || 0) * 100) }}%</span>
            </div>
            <p>{{ ref.snippet }}</p>
          </div>
        </div>

        <div class="ask-bar">
          <el-input v-model="question" type="textarea" :rows="3" resize="none"
            placeholder="比如：这节课的重点是什么？某道题为什么选这个答案？作业要求里需要注意什么？"
            @keydown.ctrl.enter.prevent="ask" />
          <el-button type="primary" :disabled="!activeCourseId || !question.trim() || stats?.storageReady === false" :loading="asking" @click="ask">
            提问
          </el-button>
        </div>
      </section>

      <aside v-if="isTeacher" class="manage-panel">
        <div class="panel-title">知识内容维护</div>
        <div class="manual-box">
          <el-input v-model="manual.title" placeholder="手动知识标题" />
          <el-input v-model="manual.content" type="textarea" :rows="5" resize="none" placeholder="补充课程说明、课堂经验、常见问题答案..." />
          <el-button type="primary" plain :disabled="!activeCourseId || !manual.title.trim() || !manual.content.trim() || stats?.storageReady === false"
            :loading="adding" @click="addManual">
            添加到知识库
          </el-button>
        </div>

        <div class="doc-list" v-loading="loadingDocs">
          <div v-for="doc in documents" :key="doc.id" class="doc-item">
            <div>
              <strong>{{ doc.title }}</strong>
              <span>{{ sourceLabel(doc.sourceType) }} · {{ doc.chunkCount || 0 }} 片段</span>
            </div>
            <el-tag size="small" :type="doc.status === 'READY' ? 'success' : doc.status === 'FAILED' ? 'danger' : 'warning'">
              {{ statusLabel(doc.status) }}
            </el-tag>
            <el-button v-if="doc.sourceType === 'MANUAL'" size="small" text type="danger" @click="removeDoc(doc.id)">
              删除
            </el-button>
          </div>
          <el-empty v-if="!documents.length && !loadingDocs" description="暂无知识文档" :image-size="80" />
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { courseApi, courseKnowledgeApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isTeacher = computed(() => userStore.roleCode === 'ROLE_TEACHER')

const courses = ref([])
const documents = ref([])
const stats = ref(null)
const activeCourseId = ref(null)
const question = ref('')
const answer = ref('')
const references = ref([])
const asking = ref(false)
const rebuilding = ref(false)
const adding = ref(false)
const loadingDocs = ref(false)
const manual = reactive({ title: '', content: '' })

const activeCourse = computed(() => courses.value.find(item => item.courseId === activeCourseId.value))
const activeSourceText = computed(() => {
  const item = stats.value || activeCourse.value
  if (!item) return '智能回答会优先引用课程资源、作业说明、题库解析和互动问答。'
  return `课程来源：${item.sourceResourceCount || 0} 个资源、${item.sourceAssignmentCount || 0} 个作业、${item.sourceQuestionCount || 0} 道题、${item.sourceQaCount || 0} 条问答。入库后会用于智能问答。`
})

const toKnowledgeCourse = (course) => ({
  courseId: course.courseId || course.id,
  courseName: course.courseName,
  documentCount: course.documentCount || 0,
  chunkCount: course.chunkCount || 0,
  readyDocumentCount: course.readyDocumentCount || 0,
  failedDocumentCount: course.failedDocumentCount || 0,
  sourceResourceCount: course.sourceResourceCount || 0,
  sourceAssignmentCount: course.sourceAssignmentCount || 0,
  sourceQuestionCount: course.sourceQuestionCount || 0,
  sourceQaCount: course.sourceQaCount || 0,
  storageReady: course.storageReady
})

const sourceTotal = (course) =>
  (course.sourceResourceCount || 0) + (course.sourceAssignmentCount || 0) + (course.sourceQuestionCount || 0) + (course.sourceQaCount || 0)

const assertSuccess = (res, fallback = '操作失败') => {
  if (res.data?.code && res.data.code !== 200) {
    throw new Error(res.data.message || fallback)
  }
  return res.data?.data
}

const mergeCourseStats = (data) => {
  if (!data?.courseId) return
  const index = courses.value.findIndex(item => item.courseId === data.courseId)
  if (index >= 0) {
    courses.value[index] = { ...courses.value[index], ...toKnowledgeCourse(data) }
  }
}

const loadCourseFallback = async () => {
  const params = userStore.roleCode === 'ROLE_STUDENT'
    ? { page: 1, size: 200, onlyEnrolled: true }
    : { page: 1, size: 200 }
  const res = await courseApi.list(params)
  if (res.data.code === 200) {
    courses.value = (res.data.data?.records || []).map(toKnowledgeCourse)
  }
}

const loadCourses = async () => {
  try {
    const res = await courseKnowledgeApi.courses()
    courses.value = (assertSuccess(res, '课程知识库课程加载失败') || []).map(toKnowledgeCourse)
    if (!courses.value.length) {
      await loadCourseFallback()
    }
  } catch (e) {
    await loadCourseFallback()
  }
  if (!activeCourseId.value && courses.value.length) {
    await selectCourse(courses.value[0].courseId)
  }
}

const selectCourse = async (courseId) => {
  activeCourseId.value = courseId
  answer.value = ''
  references.value = []
  await Promise.all([loadStats(), loadDocuments()])
}

const loadStats = async () => {
  if (!activeCourseId.value) return
  try {
    const res = await courseKnowledgeApi.stats(activeCourseId.value)
    stats.value = assertSuccess(res, '课程知识库统计加载失败')
    mergeCourseStats(stats.value)
  } catch (e) {
    stats.value = activeCourse.value || null
  }
}

const loadDocuments = async () => {
  if (!activeCourseId.value) return
  loadingDocs.value = true
  try {
    const res = await courseKnowledgeApi.documents(activeCourseId.value)
    documents.value = assertSuccess(res, '知识文档加载失败') || []
  } catch (e) {
    documents.value = []
  } finally {
    loadingDocs.value = false
  }
}

const rebuild = async () => {
  if (!activeCourseId.value) return
  await ElMessageBox.confirm('将重新抽取课程资源、作业、题库和问答内容，旧向量会被覆盖。继续吗？', '重建知识库', { type: 'warning' })
  rebuilding.value = true
  try {
    const res = await courseKnowledgeApi.rebuild(activeCourseId.value)
    const data = assertSuccess(res, '重建知识库失败') || {}
    ElMessage.success(`已重建：${data.documents || 0} 个文档，${data.chunks || 0} 个片段`)
    await Promise.all([loadCourses(), loadDocuments(), loadStats()])
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '重建知识库失败')
  } finally {
    rebuilding.value = false
  }
}

const addManual = async () => {
  adding.value = true
  try {
    const res = await courseKnowledgeApi.addManual({
      courseId: activeCourseId.value,
      title: manual.title,
      content: manual.content
    })
    assertSuccess(res, '添加知识失败')
    manual.title = ''
    manual.content = ''
    ElMessage.success('已添加到知识库')
    await Promise.all([loadCourses(), loadDocuments(), loadStats()])
  } catch (e) {
    ElMessage.error(e.message || '添加知识失败')
  } finally {
    adding.value = false
  }
}

const removeDoc = async (id) => {
  await ElMessageBox.confirm('确定删除这条手动知识吗？', '删除知识', { type: 'warning' })
  const res = await courseKnowledgeApi.deleteDocument(id)
  assertSuccess(res, '删除知识失败')
  ElMessage.success('已删除')
  await Promise.all([loadCourses(), loadDocuments(), loadStats()])
}

const ask = async () => {
  if (!activeCourseId.value || !question.value.trim()) return
  asking.value = true
  try {
    const res = await courseKnowledgeApi.ask({
      courseId: activeCourseId.value,
      question: question.value,
      topK: 5
    })
    const data = assertSuccess(res, '提问失败') || {}
    answer.value = data.answer || ''
    references.value = data.references || []
  } catch (e) {
    ElMessage.error(e.message || '提问失败')
  } finally {
    asking.value = false
  }
}

const renderMarkdown = (text) => {
  let html = String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/^\d+\.\s+(.+)$/gm, '<li>$1</li>')
    .replace(/^[-*]\s+(.+)$/gm, '<li>$1</li>')
    .replace(/\n\n/g, '</p><p>')
    .replace(/\n/g, '<br>')
  html = `<p>${html}</p>`.replace(/((?:<li>.*?<\/li>\s*)+)/g, '<ul>$1</ul>')
  return html.replace(/<p><ul>/g, '<ul>').replace(/<\/ul><\/p>/g, '</ul>')
}

const sourceLabel = (type) => ({
  RESOURCE: '课程资源',
  ASSIGNMENT: '作业说明',
  QUESTION: '题库解析',
  QA: '互动问答',
  MANUAL: '手动知识'
}[type] || type || '未知来源')

const statusLabel = (status) => ({
  READY: '已就绪',
  FAILED: '失败',
  PROCESSING: '处理中'
}[status] || status || '未知')

onMounted(loadCourses)
</script>

<style scoped>
.rag-page {
  color: var(--bio-ink, #1f2937);
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-head h2,
.chat-head h3 {
  margin: 0;
  font-size: 22px;
}

.page-head p,
.chat-head p {
  margin: 6px 0 0;
  color: var(--bio-ink-muted, #8a94a6);
}

.rag-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: start;
}

.course-panel,
.chat-panel,
.manage-panel {
  border: 1px solid var(--bio-border, #dcdfe6);
  background: #fff;
  border-radius: 8px;
}

.course-panel,
.manage-panel {
  padding: 16px;
}

.panel-title,
.reference-title {
  margin-bottom: 12px;
  font-weight: 700;
}

.course-list,
.doc-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.course-item,
.doc-item,
.reference-item {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 6px;
}

.course-item {
  width: 100%;
  padding: 12px;
  text-align: left;
  cursor: pointer;
}

.course-item.active {
  border-color: var(--bio-primary, #20c4cc);
  background: var(--bio-primary-soft, #e7fbfc);
}

.course-item strong,
.doc-item strong,
.reference-item strong {
  display: block;
  margin-bottom: 5px;
}

.course-item span,
.doc-item span,
.reference-item span {
  color: var(--bio-ink-muted, #8a94a6);
  font-size: 13px;
}

.chat-panel {
  min-height: 680px;
  overflow: hidden;
}

.chat-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 18px;
  border-bottom: 1px solid var(--bio-border, #dcdfe6);
}

.answer-box {
  min-height: 260px;
  padding: 18px;
}

.answer-content {
  line-height: 1.75;
}

.answer-content :deep(ul) {
  padding-left: 20px;
}

.reference-list {
  padding: 0 18px 14px;
}

.reference-item {
  margin-bottom: 8px;
  padding: 12px;
}

.reference-item p {
  margin: 6px 0 0;
  color: #4b5563;
  line-height: 1.6;
}

.ask-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 88px;
  gap: 10px;
  padding: 16px 18px 18px;
  border-top: 1px solid var(--bio-border, #dcdfe6);
}

.manual-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.doc-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: start;
  padding: 12px;
}

.doc-item .el-button {
  grid-column: 1 / -1;
  justify-self: end;
}

@media (max-width: 1180px) {
  .rag-layout {
    grid-template-columns: 1fr;
  }

  .chat-panel {
    min-height: 520px;
  }
}
</style>
