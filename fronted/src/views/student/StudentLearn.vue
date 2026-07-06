<template>
  <div class="learn-page">
    <h2>📚 在线学习</h2>
    <div class="learn-layout">
      <div class="video-area">
        <div class="video-placeholder">
          <el-icon :size="64"><VideoPlay /></el-icon>
          <p>视频播放器区域</p>
          <p style="color:#909399;font-size:13px">支持播放/暂停、进度拖拽、倍速、全屏</p>
        </div>
        <div class="resources" style="margin-top:12px">
          <h4>📄 课件下载</h4>
          <el-link type="primary">第二章PPT.pdf</el-link>
          <el-link type="primary" style="margin-left:16px">第二章讲义.docx</el-link>
        </div>
        <div class="notes" style="margin-top:16px">
          <h4>✏️ 记笔记</h4>
          <el-input v-model="noteContent" type="textarea" :rows="3" placeholder="在此输入笔记内容..." />
          <div style="display:flex;gap:8px;margin-top:8px">
            <el-input v-model="timestamp" placeholder="视频时间点(如02:35)" style="width:160px" />
            <el-button type="primary" @click="saveNote">保存</el-button>
            <el-button @click="router.push('/student/notes')">📋 查看我的笔记</el-button>
          </div>
        </div>
      </div>
      <div class="outline-area">
        <el-card shadow="hover">
          <template #header>📖 章节大纲</template>
          <div class="outline-list">
            <div v-for="ch in chapters" :key="ch.id" :class="['outline-item', ch.active ? 'active' : '']">
              <span>{{ ch.sortOrder }}. {{ ch.chapterName }}</span>
              <el-tag v-if="ch.done" type="success" size="small">✅</el-tag>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const noteContent = ref('')
const timestamp = ref('')
const chapters = ref([
  { id: 1, sortOrder: 1, chapterName: '第一章 入门', done: true },
  { id: 2, sortOrder: 2, chapterName: '第二章 面向对象', done: false, active: true },
  { id: 3, sortOrder: 3, chapterName: '第三章 异常处理', done: false }
])

const saveNote = () => {
  if (!noteContent.value.trim()) { ElMessage.warning('请输入笔记内容'); return }
  ElMessage.success('笔记已保存')
  noteContent.value = ''
  timestamp.value = ''
}
</script>

<style scoped>
.learn-page h2 { margin-bottom: 20px; color: #303133; }
.learn-layout { display: flex; gap: 20px; }
.video-area { flex: 1; }
.video-placeholder { height: 400px; background: #1a1a2e; border-radius: 8px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #fff; gap: 8px; }
.outline-area { width: 260px; flex-shrink: 0; }
.outline-item { padding: 10px 0; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; cursor: pointer; font-size: 14px; }
.outline-item:hover, .outline-item.active { color: #1890ff; }
</style>
