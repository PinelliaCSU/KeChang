<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">💬 互动问答</h2>
    </div>

    <div class="panel">
      <div class="search-bar">
        <el-select v-model="courseFilter" placeholder="按课程筛选" clearable style="width:200px" @change="fetchPosts">
          <el-option v-for="c in courseList" :key="c.id" :label="c.courseName" :value="c.id" />
        </el-select>
        <el-select v-model="orderBy" style="width:140px" @change="fetchPosts">
          <el-option label="最新发布" value="time" /><el-option label="最多回复" value="reply" /><el-option label="最多浏览"
            value="view" />
        </el-select>
      </div>

      <div class="post-list" v-if="posts.length > 0">
        <div class="post-item" v-for="p in posts" :key="p.id" @click="viewPost(p)">
          <div class="post-main">
            <div class="post-title">
              <el-tag size="small" :type="p.postType === '提问' ? 'warning' : 'info'" style="margin-right:8px">{{
                p.postType || '提问' }}</el-tag>
              {{ p.title }}
            </div>
            <div class="post-meta">
              <span>👤 {{ p.createByName || '匿名' }}</span>
              <span>👁 {{ p.viewCount || 0 }}</span>
              <span>💬 {{ p.replyCount || 0 }}</span>
              <span>🕐 {{ p.createTime }}</span>
            </div>
          </div>
          <div class="post-badge" v-if="p.replyCount === 0">
            <el-tag type="danger" size="small">待回复</el-tag>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无问答帖子" :image-size="100" />

      <el-pagination v-if="total > pageSize" :current-page="page" :page-size="pageSize" :total="total"
        layout="prev, pager, next" @current-change="p => { page = p; fetchPosts() }"
        style="margin-top:16px;justify-content:center" />
    </div>

    <!-- 帖子详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="currentPost?.title" width="750px">
      <div v-if="currentPost" class="post-detail">
        <div class="post-content" v-html="currentPost.content || '暂无内容'"></div>
        <div class="post-info">
          <span>👤 {{ currentPost.createByName || '匿名' }}</span>
          <span>🕐 {{ currentPost.createTime }}</span>
          <span>👁 {{ (currentPost.viewCount || 0) + 1 }}</span>
        </div>

        <el-divider>回答 ({{ answers.length }})</el-divider>

        <div class="answer-list" v-if="answers.length > 0">
          <div v-for="a in answers" :key="a.id" class="answer-item" :class="{ accepted: a.isAccepted === 1 }">
            <div class="answer-content">{{ a.content }}</div>
            <div class="answer-footer">
              <span>👤 {{ a.createByName || '匿名' }}</span>
              <span>🕐 {{ a.createTime }}</span>
              <el-tag v-if="a.isAccepted === 1" type="success" size="small">✅ 已采纳</el-tag>
              <el-button v-else size="small" type="success" @click="doAccept(a.id)">采纳</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无回答" :image-size="60" />

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
import { ref, onMounted } from 'vue'
import { qaApi, courseApi } from '@/api'
import { ElMessage } from 'element-plus'

const posts = ref([]); const page = ref(1); const pageSize = ref(10); const total = ref(0)
const courseFilter = ref(null); const orderBy = ref('time')
const courseList = ref([])

// 详情
const detailVisible = ref(false); const currentPost = ref(null)
const answers = ref([]); const answerContent = ref(''); const answering = ref(false)

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

const viewPost = async (p) => {
  currentPost.value = p
  detailVisible.value = true
  answerContent.value = ''
  try {
    const [postRes, ansRes] = await Promise.all([
      qaApi.getPost(p.id),
      qaApi.listAnswers(p.id)
    ])
    if (postRes.data.code === 200) currentPost.value = postRes.data.data
    if (ansRes.data.code === 200) answers.value = ansRes.data.data.records || ansRes.data.data || []
  } catch (e) { /* ignore */ }
}

const doAnswer = async () => {
  if (!answerContent.value.trim()) { ElMessage.warning('请输入回答内容'); return }
  answering.value = true
  try {
    await qaApi.createAnswer(currentPost.value.id, { content: answerContent.value })
    ElMessage.success('回答发表成功')
    answerContent.value = ''
    // 刷新回答
    const res = await qaApi.listAnswers(currentPost.value.id)
    if (res.data.code === 200) answers.value = res.data.data.records || res.data.data || []
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
    viewPost(currentPost.value)
  } catch (e) { /* ignore */ }
}

onMounted(() => { fetchCourses(); fetchPosts() })
</script>

<style scoped>
.page-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 24px;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.post-list {
  display: flex;
  flex-direction: column;
}

.post-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.2s;
}

.post-item:hover {
  background: #f5f7fa;
  margin: 0 -24px;
  padding: 16px 24px;
}

.post-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.post-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.post-content {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 12px;
  line-height: 1.6;
}

.post-info {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.answer-item {
  padding: 12px;
  margin-bottom: 8px;
  background: #fafafa;
  border-radius: 8px;
  border-left: 3px solid #e4e7ed;
}

.answer-item.accepted {
  border-left-color: #67c23a;
  background: #f0f9eb;
}

.answer-content {
  line-height: 1.5;
  margin-bottom: 8px;
}

.answer-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.reply-box {
  margin-top: 16px;
}
</style>
