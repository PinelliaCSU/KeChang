<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">在线学习</h2>
      <el-button @click="router.push('/student/notes')">查看我的笔记</el-button>
    </div>

    <div class="learn-layout">
      <!-- 左侧：视频+资源+笔记 -->
      <div class="learn-main">
        <div class="panel video-panel">
          <div class="video-placeholder">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,0.6)"
              stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="5 3 19 12 5 21 5 3" fill="rgba(255,255,255,0.2)" />
            </svg>
            <p style="color:#fff;font-size:16px;font-weight:500">视频播放器区域</p>
            <p style="color:rgba(255,255,255,0.5);font-size:13px">支持播放/暂停、进度拖拽、倍速、全屏</p>
          </div>
        </div>

        <div class="panel">
          <h4 class="section-title">课件下载</h4>
          <div class="resource-links">
            <el-link type="primary" :underline="false">第二章PPT.pdf</el-link>
            <el-link type="primary" :underline="false">第二章讲义.docx</el-link>
          </div>
        </div>

        <div class="panel">
          <h4 class="section-title">记笔记</h4>
          <el-input v-model="noteContent" type="textarea" :rows="3" placeholder="在此输入笔记内容..." />
          <div class="note-actions">
            <el-input v-model="timestamp" placeholder="视频时间点(如02:35)" style="width:160px" size="default" />
            <el-button type="primary" @click="saveNote">保存笔记</el-button>
          </div>
        </div>
      </div>

      <!-- 右侧：章节大纲 -->
      <div class="learn-sidebar">
        <div class="panel outline-panel">
          <h4 class="section-title">章节大纲</h4>
          <div class="outline-list">
            <div v-for="ch in chapters" :key="ch.id" :class="['outline-item', ch.active ? 'active' : '']">
              <span>{{ ch.sortOrder }}. {{ ch.chapterName }}</span>
              <el-tag v-if="ch.done" type="success" size="small">已完成</el-tag>
            </div>
          </div>
        </div>
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
  margin-bottom: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 14px 0;
}

.learn-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.learn-main {
  flex: 1;
  min-width: 0;
}

.learn-sidebar {
  width: 260px;
  flex-shrink: 0;
}

.video-panel {
  padding: 0;
  overflow: hidden;
}

.video-placeholder {
  height: 400px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.resource-links {
  display: flex;
  gap: 20px;
}

.note-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.outline-panel {
  position: sticky;
  top: 84px;
}

.outline-item {
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: color 0.2s;
}

.outline-item:last-child {
  border-bottom: none;
}

.outline-item:hover,
.outline-item.active {
  color: #409eff;
}
</style>
