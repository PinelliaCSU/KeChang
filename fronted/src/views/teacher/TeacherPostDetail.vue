<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <div>
        <h2 class="page-title">{{ post.title || '帖子详情' }}</h2>
        <p class="page-subtitle">
          <el-tag size="small" :type="post.postType === '提问' ? 'warning' : 'info'">{{ post.postType || '提问' }}</el-tag>
          <span v-if="post.courseName" style="margin-left:8px;color:#7a9fb5">{{ post.courseName }}</span>
        </p>
      </div>
      <div class="title-actions">
        <el-button v-if="canDeletePost" type="danger" plain @click="doDeletePost">删除帖子</el-button>
        <el-button @click="router.back()">返回列表</el-button>
      </div>
    </div>

    <!-- 帖子内容 -->
    <div class="panel">
      <div class="post-content" v-html="post.content || '暂无内容'"></div>
      <div class="post-meta">
        <span><el-icon><User /></el-icon> {{ post.createByName || '匿名' }}</span>
        <span><el-icon><Clock /></el-icon> {{ post.createTime }}</span>
        <span><el-icon><View /></el-icon> {{ post.viewCount || 0 }} 浏览</span>
      </div>
    </div>

    <!-- 回答区域 -->
    <div class="panel">
      <div class="section-header">
        <h3 class="section-title">回答 ({{ answerTotal }})</h3>
      </div>

      <div class="answer-list" v-if="answers.length > 0">
        <div v-for="a in answers" :key="a.id" class="answer-card" :class="{ accepted: a.isAccepted === 1 }">
          <div class="answer-body">
            <div class="answer-content">{{ a.content }}</div>
            <div class="answer-footer">
              <span class="answer-author"><el-icon><User /></el-icon> {{ a.createByName || '匿名' }}</span>
              <span class="answer-time"><el-icon><Clock /></el-icon> {{ a.createTime }}</span>
              <el-tag v-if="a.isAccepted === 1" type="success" size="small"><el-icon><CircleCheck /></el-icon> 已采纳</el-tag>
              <el-button v-else size="small" type="success" @click="doAccept(a.id)">采纳</el-button>
              <el-button v-if="canDeleteAnswer(a)" size="small" type="danger" plain @click="doDeleteAnswer(a.id)">删除</el-button>
            </div>

            <!-- 评论 -->
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
      </div>
      <el-empty v-else description="暂无回答" :image-size="80" />

      <PaginationWrapper v-if="answerTotal > answerSize" v-model:page="answerPage" v-model:size="answerSize" :total="answerTotal" :page-sizes="[5, 10, 20]" @change="fetchAnswers" />
    </div>

    <!-- 写回答 -->
    <div class="panel">
      <div class="section-header">
        <h3 class="section-title">发表回答</h3>
      </div>
      <el-input v-model="answerContent" type="textarea" :rows="4" placeholder="输入你的回答..." />
      <div class="reply-actions">
        <el-button plain type="primary" @click="doAiAnswer">智能辅助</el-button>
        <el-button type="primary" :loading="answering" @click="doAnswer">发表回答</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { qaApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePagination } from '@/composables/usePagination'
import PaginationWrapper from '@/components/PaginationWrapper.vue'
import { ArrowDown, ArrowRight, User, Clock, View, CircleCheck } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const post = ref({})
const answers = ref([])
const answerContent = ref('')
const answering = ref(false)
const { page: answerPage, size: answerSize, total: answerTotal } = usePagination({ defaultSize: 5, pageSizes: [5, 10, 20] })

const isPostOwner = computed(() => post.value?.createBy === userStore.userId)
const isTeacher = computed(() => userStore.isTeacher)
const canDeletePost = computed(() => isPostOwner.value || isTeacher.value)
const canDeleteAnswer = (a) => a.createBy === userStore.userId || isTeacher.value

const fetchPost = async () => {
  try {
    const res = await qaApi.getPost(route.params.id)
    if (res.data.code === 200) post.value = res.data.data
  } catch (e) { /* ignore */ }
}

const fetchAnswers = async () => {
  if (!route.params.id) return
  try {
    const res = await qaApi.listAnswers(route.params.id, { page: answerPage.value, size: answerSize.value })
    if (res.data.code === 200) {
      const d = res.data.data
      answers.value = (d.records || []).map(a => ({
        ...a, _comments: [], _showComments: false, _commentCount: a.commentCount || 0, _commentText: ''
      }))
      answerTotal.value = d.total || 0
    }
  } catch (e) { /* ignore */ }
}

const doAnswer = async () => {
  if (!answerContent.value.trim()) { ElMessage.warning('请输入回答内容'); return }
  answering.value = true
  try {
    await qaApi.createAnswer(post.value.id, { content: answerContent.value })
    ElMessage.success('回答发表成功')
    answerContent.value = ''
    fetchAnswers()
  } catch (e) { /* ignore */ }
  finally { answering.value = false }
}

const doAiAnswer = async () => {
  try {
    const res = await qaApi.aiAnswer(post.value.id)
    if (res.data.code === 200) answerContent.value = res.data.data.content || 'AI 暂时无法回答此问题'
  } catch (e) { /* ignore */ }
}

const doAccept = async (answerId) => {
  try {
    await qaApi.acceptAnswer(answerId)
    ElMessage.success('已采纳该回答')
    fetchAnswers()
  } catch (e) { /* ignore */ }
}

const doDeletePost = async () => {
  try {
    await ElMessageBox.confirm('确定要删除此帖子吗？', '确认删除', { type: 'warning' })
    await qaApi.deletePost(post.value.id)
    ElMessage.success('帖子已删除')
    router.push('/teacher/posts')
  } catch (e) { /* ignore */ }
}

const doDeleteAnswer = async (answerId) => {
  try {
    await ElMessageBox.confirm('确定要删除此回答吗？', '确认删除', { type: 'warning' })
    await qaApi.deleteAnswer(answerId)
    ElMessage.success('回答已删除')
    fetchAnswers()
  } catch (e) { /* ignore */ }
}

// 评论
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
    const res = await qaApi.listComments('ANSWER', a.id, { page: 1, size: 50 })
    if (res.data.code === 200) {
      a._comments = res.data.data.records || []
      a._commentCount = res.data.data.total || 0
    }
  } catch (e) { /* ignore */ }
}

onMounted(() => { fetchPost(); fetchAnswers() })
</script>

<style scoped>
.page-shell { display: flex; flex-direction: column; gap: 20px; padding-bottom: 24px; }

.page-title-bar { display: flex; align-items: flex-start; justify-content: space-between; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0 0 6px; }
.page-subtitle { font-size: 13px; color: #909399; margin: 0; display: flex; align-items: center; }
.title-actions { display: flex; gap: 8px; flex-shrink: 0; }

.panel { background: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,.05); padding: 24px; }

.post-content { padding: 16px; background: #f5f7fa; border-radius: 8px; line-height: 1.7; font-size: 14px; color: #303133; }
.post-meta { display: flex; gap: 20px; font-size: 13px; color: #909399; margin-top: 16px; padding-top: 16px; border-top: 1px solid #ebeef5; }

.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.section-title { font-size: 16px; font-weight: 600; color: #303133; margin: 0; }

.answer-list { display: flex; flex-direction: column; gap: 12px; }
.answer-card { padding: 16px; background: #fafafa; border-radius: 8px; border-left: 3px solid #e4e7ed; transition: all 0.2s; }
.answer-card.accepted { border-left-color: #67c23a; background: #f0f9eb; }
.answer-body { display: flex; flex-direction: column; gap: 10px; }
.answer-content { line-height: 1.6; color: #303133; white-space: pre-wrap; }
.answer-footer { display: flex; align-items: center; gap: 14px; font-size: 13px; color: #909399; flex-wrap: wrap; }
.answer-author, .answer-time { display: flex; align-items: center; gap: 4px; }

.comment-section { margin-top: 4px; padding-top: 10px; border-top: 1px dashed #ebeef5; }
.comment-box { margin-top: 8px; }
.comment-list { margin-bottom: 8px; }
.comment-item { padding: 4px 0; font-size: 13px; color: #606266; line-height: 1.5; }
.comment-author { color: #7a9fb5; font-weight: 500; }
.comment-time { font-size: 11px; color: #c0c4cc; margin-left: 8px; }
.comment-empty { font-size: 12px; color: #c0c4cc; padding: 8px 0; }
.comment-input-row { display: flex; gap: 8px; margin-top: 8px; }

.reply-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }
</style>
