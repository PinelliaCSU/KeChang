<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">互动问答</h2>
      <el-button type="primary" size="default" @click="openCreate">+ 发布问题</el-button>
    </div>

    <!-- 筛选与排序 -->
    <div class="panel">
      <div class="search-bar">
        <el-select v-model="courseFilter" placeholder="按课程筛选" clearable style="width:200px" @change="fetchPosts">
          <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
        <el-select v-model="orderBy" style="width:140px" @change="fetchPosts">
          <el-option label="最新发布" value="time" />
          <el-option label="最多回复" value="reply" />
          <el-option label="最多浏览" value="view" />
        </el-select>
      </div>

      <!-- 帖子列表 -->
      <div v-if="posts.length > 0" class="post-list">
        <div class="post-item" v-for="p in posts" :key="p.id" @click="viewPost(p)">
          <div class="post-avatar">{{ (p.createByName || '?').toString().charAt(0) }}</div>
          <div class="post-body">
            <h4 class="post-title">
              <el-tag size="small" :type="p.postType === '提问' ? 'warning' : 'info'" style="margin-right:6px">{{ p.postType || '提问' }}</el-tag>
              {{ p.title }}
            </h4>
            <p class="post-preview">{{ (p.content || '').replace(/<[^>]+>/g, '').substring(0, 120) }}</p>
            <div class="post-meta">
              <span>{{ p.createByName || '匿名' }}</span>
              <span v-if="p.courseName" class="course-tag">📚 {{ p.courseName }}</span>
              <span>{{ p.createTime }}</span>
              <span>💬 {{ p.replyCount || 0 }} 回复</span>
              <span>👁 {{ p.viewCount || 0 }} 浏览</span>
            </div>
          </div>
          <el-tag v-if="p.replyCount === 0" type="danger" size="small">待回复</el-tag>
        </div>
      </div>
      <el-empty v-else description="暂无问答帖子" :image-size="100" />

      <el-pagination v-if="total > pageSize" :current-page="page" :page-size="pageSize" :total="total"
        layout="prev, pager, next" @current-change="p => { page = p; fetchPosts() }"
        style="margin-top:16px;justify-content:center" />
    </div>

    <!-- 发布问题弹窗 -->
    <el-dialog v-model="showCreate" title="发布问题" width="520px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="所属课程" required>
          <el-select v-model="createForm.courseId" placeholder="请选择课程" style="width:100%">
            <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="帖子类型" required>
          <el-radio-group v-model="createForm.postType">
            <el-radio value="提问">提问</el-radio>
            <el-radio value="讨论">讨论</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="createForm.title" placeholder="请输入问题标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="createForm.content" type="textarea" :rows="5" placeholder="描述你的问题..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="createPost">发布</el-button>
      </template>
    </el-dialog>

    <!-- 帖子详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="currentPost?.title || '帖子详情'" width="750px" destroy-on-close>
      <div v-if="currentPost" class="post-detail">
        <!-- 帖子头部 -->
        <div class="detail-header">
          <el-tag size="small" :type="currentPost.postType === '提问' ? 'warning' : 'info'">{{ currentPost.postType || '提问' }}</el-tag>
          <span v-if="currentPost.courseName" class="course-badge">📚 {{ currentPost.courseName }}</span>
          <el-button v-if="canDeletePost" size="small" type="danger" plain @click="doDeletePost" style="margin-left:auto">删除帖子</el-button>
        </div>

        <!-- 帖子内容 -->
        <div class="post-content" v-html="currentPost.content || '暂无内容'"></div>
        <div class="post-info">
          <span>👤 {{ currentPost.createByName || '匿名' }}</span>
          <span>🕐 {{ currentPost.createTime }}</span>
          <span>👁 {{ currentPost.viewCount || 0 }}</span>
        </div>

        <el-divider>回答 ({{ answerTotal }})</el-divider>

        <!-- 回答列表 -->
        <div class="answer-list" v-if="answers.length > 0">
          <div v-for="a in answers" :key="a.id" class="answer-item" :class="{ accepted: a.isAccepted === 1 }">
            <div class="answer-content">{{ a.content }}</div>
            <div class="answer-footer">
              <span>👤 {{ a.createByName || '匿名' }}</span>
              <span>🕐 {{ a.createTime }}</span>
              <el-tag v-if="a.isAccepted === 1" type="success" size="small">✅ 已采纳</el-tag>
              <el-button v-else-if="isPostOwner" size="small" type="success" @click="doAccept(a.id)">采纳</el-button>
              <el-button v-if="canDeleteAnswer(a)" size="small" type="danger" plain @click="doDeleteAnswer(a.id)">删除</el-button>
            </div>

            <!-- 评论区域 -->
            <div class="comment-section">
              <el-button link type="primary" size="small" @click="toggleComments(a)">
                评论 ({{ a._commentCount || 0 }})
                <el-icon><ArrowDown v-if="a._showComments" /><ArrowRight v-else /></el-icon>
              </el-button>
              <div v-if="a._showComments" class="comment-box">
                <div v-if="a._comments && a._comments.length > 0" class="comment-list">
                  <div v-for="c in a._comments" :key="c.id" class="comment-item">
                    <span class="comment-author">{{ c.createByName || '匿名' }}</span>：
                    <span>{{ c.content }}</span>
                    <span class="comment-time">{{ c.createTime }}</span>
                  </div>
                </div>
                <div v-else class="comment-empty">暂无评论</div>
                <div class="comment-input-row">
                  <el-input v-model="a._commentText" size="small" placeholder="添加评论..." @keyup.enter="doAddComment(a)" />
                  <el-button size="small" type="primary" @click="doAddComment(a)">评论</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无回答" :image-size="60" />

        <el-pagination v-if="answerTotal > answerPageSize" :current-page="answerPage" :page-size="answerPageSize"
          :total="answerTotal" layout="prev, pager, next" @current-change="p => { answerPage = p; fetchAnswers() }"
          style="margin-top:12px;justify-content:center" small />

        <!-- 写回答 -->
        <div class="reply-box">
          <el-input v-model="answerContent" type="textarea" :rows="3" placeholder="输入你的回答..." />
          <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:8px">
            <el-button type="warning" @click="doAiAnswer">🤖 AI 智能回复</el-button>
            <el-button type="primary" :loading="answering" @click="doAnswer">发表回答</el-button>
          </div>
        </div>
      </div>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { qaApi, courseApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, ArrowRight } from '@element-plus/icons-vue'

const userStore = useUserStore()

// ========== 列表 ==========
const posts = ref([]); const page = ref(1); const pageSize = ref(10); const total = ref(0)
const courseFilter = ref(null); const orderBy = ref('time')
const courseList = ref([])

const fetchPosts = async () => {
  try {
    const res = await qaApi.listPosts({ page: page.value, size: pageSize.value, courseId: courseFilter.value || undefined, orderBy: orderBy.value })
    if (res.data.code === 200) { posts.value = res.data.data.records; total.value = res.data.data.total }
  } catch (e) { /* ignore */ }
}

const fetchCourses = async () => {
  try {
    const res = await courseApi.list({ page: 1, size: 100 })
    if (res.data.code === 200) courseList.value = res.data.data.records
  } catch (e) { /* ignore */ }
}

// ========== 发布 ==========
const showCreate = ref(false); const submitting = ref(false)
const createForm = ref({ title: '', content: '', courseId: null, postType: '提问' })

const openCreate = () => { showCreate.value = true }

const createPost = async () => {
  if (!createForm.value.courseId) { ElMessage.warning('请选择所属课程'); return }
  if (!createForm.value.title.trim()) { ElMessage.warning('请输入标题'); return }
  submitting.value = true
  try {
    await qaApi.createPost(createForm.value)
    ElMessage.success('发布成功')
    showCreate.value = false
    createForm.value = { title: '', content: '', courseId: null, postType: '提问' }
    fetchPosts()
  } catch (e) { /* ignore */ }
  submitting.value = false
}

// ========== 详情 ==========
const detailVisible = ref(false); const currentPost = ref(null)
const answers = ref([]); const answerContent = ref(''); const answering = ref(false)
const answerPage = ref(1); const answerPageSize = ref(5); const answerTotal = ref(0)

const isPostOwner = computed(() => currentPost.value?.createBy === userStore.userId)
const isTeacher = computed(() => userStore.isTeacher)
const canDeletePost = computed(() => isPostOwner.value || isTeacher.value)
const canDeleteAnswer = (a) => a.createBy === userStore.userId || isTeacher.value

const viewPost = async (p) => {
  currentPost.value = p
  detailVisible.value = true
  answerContent.value = ''
  answerPage.value = 1
  try {
    const [postRes] = await Promise.all([
      qaApi.getPost(p.id),
      fetchAnswers()
    ])
    if (postRes.data.code === 200) {
      currentPost.value = postRes.data.data
      // 同步浏览量到列表
      const idx = posts.value.findIndex(item => item.id === p.id)
      if (idx !== -1) posts.value[idx].viewCount = postRes.data.data.viewCount
    }
  } catch (e) { /* ignore */ }
}

const fetchAnswers = async () => {
  if (!currentPost.value) return
  try {
    const res = await qaApi.listAnswers(currentPost.value.id, { page: answerPage.value, size: answerPageSize.value })
    if (res.data.code === 200) {
      const d = res.data.data
      answers.value = (d.records || []).map(a => ({
        ...a, _comments: [], _showComments: false, _commentCount: a.commentCount || 0, _commentText: ''
      }))
      answerTotal.value = d.total || 0
    }
  } catch (e) { /* ignore */ }
}

// ========== 回答 ==========
const doAnswer = async () => {
  if (!answerContent.value.trim()) { ElMessage.warning('请输入回答内容'); return }
  answering.value = true
  try {
    await qaApi.createAnswer(currentPost.value.id, { content: answerContent.value })
    ElMessage.success('回答发表成功')
    answerContent.value = ''
    fetchAnswers()
    fetchPosts()
  } catch (e) { /* ignore */ }
  finally { answering.value = false }
}

const doAiAnswer = async () => {
  try {
    const res = await qaApi.aiAnswer(currentPost.value.id)
    if (res.data.code === 200) {
      answerContent.value = res.data.data.content || 'AI 暂时无法回答此问题'
      ElMessage.success('AI 回复已生成')
    }
  } catch (e) { /* ignore */ }
}

const doAccept = async (answerId) => {
  try {
    await qaApi.acceptAnswer(answerId)
    ElMessage.success('已采纳该回答')
    fetchAnswers()
    fetchPosts()
  } catch (e) { /* ignore */ }
}

const doDeletePost = async () => {
  try {
    await ElMessageBox.confirm('确定要删除此帖子吗？', '确认删除', { type: 'warning' })
    await qaApi.deletePost(currentPost.value.id)
    ElMessage.success('帖子已删除')
    detailVisible.value = false
    fetchPosts()
  } catch (e) { /* ignore */ }
}

const doDeleteAnswer = async (answerId) => {
  try {
    await ElMessageBox.confirm('确定要删除此回答吗？', '确认删除', { type: 'warning' })
    await qaApi.deleteAnswer(answerId)
    ElMessage.success('回答已删除')
    fetchAnswers()
    fetchPosts()
  } catch (e) { /* ignore */ }
}

// ========== 评论 ==========
const toggleComments = async (a) => {
  if (a._showComments) { a._showComments = false; return }
  a._showComments = true
  try {
    const res = await qaApi.listComments('ANSWER', a.id, { page: 1, size: 50 })
    if (res.data.code === 200) {
      a._comments = res.data.data.records || []
      a._commentCount = res.data.data.total || 0
    }
  } catch (e) { /* ignore */ }
}

const doAddComment = async (a) => {
  if (!a._commentText || !a._commentText.trim()) return
  try {
    await qaApi.addComment(a.id, { content: a._commentText })
    a._commentText = ''
    // 刷新评论
    const res = await qaApi.listComments('ANSWER', a.id, { page: 1, size: 50 })
    if (res.data.code === 200) {
      a._comments = res.data.data.records || []
      a._commentCount = res.data.data.total || 0
    }
  } catch (e) { /* ignore */ }
}

onMounted(() => { fetchCourses(); fetchPosts() })
</script>

<style scoped>
.page-shell { display: flex; flex-direction: column; gap: 20px; }
.page-title-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.panel { background: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); padding: 24px; }
.search-bar { display: flex; gap: 12px; margin-bottom: 16px; }

.post-list { display: flex; flex-direction: column; }
.post-item { display: flex; align-items: flex-start; gap: 14px; padding: 16px 0; border-bottom: 1px solid #f0f0f0; cursor: pointer; transition: background 0.15s; }
.post-item:last-child { border-bottom: none; }
.post-item:hover { background: #fafbfc; margin: 0 -24px; padding: 16px 24px; border-radius: 8px; }
.post-avatar { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, var(--bio-primary), var(--bio-primary-dark)); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 600; flex-shrink: 0; }
.post-body { flex: 1; min-width: 0; }
.post-title { font-size: 15px; font-weight: 600; color: #303133; margin: 0 0 6px 0; display: flex; align-items: center; }
.post-preview { font-size: 13px; color: #909399; line-height: 1.5; margin: 0 0 8px 0; }
.post-meta { display: flex; gap: 14px; font-size: 12px; color: #c0c4cc; flex-wrap: wrap; }
.course-tag { color: #7a9fb5; }

/* 详情 */
.detail-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.course-badge { font-size: 13px; color: #7a9fb5; }
.post-content { padding: 16px; background: #f5f7fa; border-radius: 8px; margin-bottom: 12px; line-height: 1.6; }
.post-info { display: flex; gap: 16px; font-size: 13px; color: #909399; margin-bottom: 8px; }

.answer-item { padding: 12px; margin-bottom: 8px; background: #fafafa; border-radius: 8px; border-left: 3px solid #e4e7ed; }
.answer-item.accepted { border-left-color: var(--bio-primary); background: var(--bio-primary-soft); }
.answer-content { line-height: 1.5; margin-bottom: 8px; }
.answer-footer { display: flex; align-items: center; gap: 10px; font-size: 12px; color: #909399; flex-wrap: wrap; }

/* 评论 */
.comment-section { margin-top: 8px; padding-top: 8px; border-top: 1px dashed #ebeef5; }
.comment-box { margin-top: 8px; }
.comment-list { margin-bottom: 8px; }
.comment-item { padding: 4px 0; font-size: 13px; color: #606266; line-height: 1.5; }
.comment-author { color: #7a9fb5; font-weight: 500; }
.comment-time { font-size: 11px; color: #c0c4cc; margin-left: 8px; }
.comment-empty { font-size: 12px; color: #c0c4cc; padding: 8px 0; }
.comment-input-row { display: flex; gap: 8px; margin-top: 8px; }

.reply-box { margin-top: 16px; }
</style>
