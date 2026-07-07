<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">互动问答</h2>
      <el-button type="primary" size="default" @click="showCreate = true">+ 发布问题</el-button>
    </div>

    <div class="panel" v-if="posts.length > 0">
      <div class="post-list">
        <div class="post-item" v-for="p in posts" :key="p.id">
          <div class="post-avatar">{{ (p.createByName || p.createBy || '?').toString().charAt(0) }}</div>
          <div class="post-body">
            <h4 class="post-title">{{ p.title }}</h4>
            <p class="post-preview">{{ (p.content || '').substring(0, 150) }}</p>
            <div class="post-meta">
              <span>{{ p.createByName || p.createBy }}</span>
              <span>{{ p.createTime }}</span>
              <span>{{ p.replyCount || 0 }} 回复</span>
              <span>{{ p.viewCount || 0 }} 浏览</span>
            </div>
          </div>
          <el-tag v-if="p.isResolved" type="success" size="small">已解决</el-tag>
        </div>
      </div>
    </div>

    <div class="panel empty-panel" v-else>
      <div class="empty-state">
        <svg class="empty-illustration" width="120" height="90" viewBox="0 0 120 90" fill="none">
          <circle cx="60" cy="45" r="25" fill="#e8ecf1" />
          <path d="M50 40h20M50 48h14" stroke="#d0d5dd" stroke-width="2" stroke-linecap="round" />
        </svg>
        <p class="empty-text">暂无讨论帖</p>
      </div>
    </div>

    <!-- 发布问题弹窗 -->
    <el-dialog v-model="showCreate" title="发布问题" width="500px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="标题" required><el-input v-model="createForm.title" placeholder="请输入问题标题" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="createForm.content" type="textarea" :rows="4"
            placeholder="描述你的问题..." /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="createPost">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { qaApi } from '@/api'
import { ElMessage } from 'element-plus'

const posts = ref([])
const showCreate = ref(false)
const createForm = ref({ title: '', content: '', courseId: null })
const courseList = ref([])
const submitting = ref(false)

const fetchPosts = async () => {
  try {
    const res = await qaApi.listPosts({ page: 1, size: 50 })
    if (res.data.code === 200) {
      const d = res.data.data
      posts.value = Array.isArray(d?.records) ? d.records : Array.isArray(d) ? d : []
    }
  } catch (e) { /* ignore */ }
}

const createPost = async () => {
  if (!createForm.value.title.trim()) { ElMessage.warning('请输入标题'); return }
  submitting.value = true
  try {
    await qaApi.createPost(createForm.value)
    ElMessage.success('发布成功')
    showCreate.value = false
    createForm.value = { title: '', content: '', courseId: null }
    fetchPosts()
  } catch (e) { ElMessage.error('发布失败') }
  submitting.value = false
}

onMounted(fetchPosts)
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

.post-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.post-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;
}

.post-item:last-child {
  border-bottom: none;
}

.post-item:hover {
  background: #fafbfc;
  margin: 0 -24px;
  padding: 16px 24px;
  border-radius: 8px;
}

.post-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #20c997, #0d9488);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}

.post-body {
  flex: 1;
  min-width: 0;
}

.post-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.post-preview {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
  margin-bottom: 8px;
}

.post-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #c0c4cc;
}

.empty-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.empty-illustration {
  opacity: 0.6;
}

.empty-text {
  color: #c0c4cc;
  font-size: 14px;
}
</style>
