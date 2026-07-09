<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">课程公告</h2>
    </div>

    <div class="panel" v-if="announcements.length > 0">
      <div class="announcement-list">
        <div class="announcement-card" v-for="a in announcements" :key="a.id" @click="openDetail(a)">
          <div class="announcement-card-header">
            <h3>{{ a.title }}</h3>
            <el-tag :type="a.confirmed ? 'success' : 'warning'" size="small">
              {{ a.confirmed ? '已确认' : '待确认' }}
            </el-tag>
          </div>
          <p class="announcement-desc">{{ truncateContent(a.content) }}</p>
          <div class="announcement-meta">
            <span><el-icon><School /></el-icon> {{ a.courseName || '未关联课程' }}</span>
            <span><el-icon><Clock /></el-icon> {{ formatTime(a.publishTime) }}</span>
            <span><el-icon><User /></el-icon> {{ a.confirmCount || 0 }} 人已确认</span>
          </div>
          <div class="announcement-actions">
            <el-button v-if="!a.confirmed" type="primary" size="small" @click.stop="handleConfirm(a)">
              确认公告
            </el-button>
            <el-button v-else type="success" size="small" disabled>已确认</el-button>
          </div>
        </div>
      </div>
      <el-pagination v-if="total > pageSize" :current-page="page" :page-size="pageSize" :total="total"
        layout="prev, pager, next" @current-change="p => { page = p; fetchAnnouncements() }"
        style="margin-top:16px;justify-content:center" />
    </div>

    <div class="panel empty-panel" v-else-if="!loading">
      <el-empty description="暂无课程公告" :image-size="100" />
    </div>

    <!-- 公告详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" :title="currentAnnouncement.title" width="650px" destroy-on-close>
      <div class="detail-header">
        <div class="detail-meta">
          <span><strong>课程：</strong>{{ currentAnnouncement.courseName || '-' }}</span>
          <span><strong>发布时间：</strong>{{ formatTime(currentAnnouncement.publishTime) }}</span>
          <span><strong>确认人数：</strong>{{ currentAnnouncement.confirmCount || 0 }}</span>
        </div>
      </div>
      <el-divider />
      <div class="detail-content" v-html="formatContent(currentAnnouncement.content)"></div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button v-if="!currentAnnouncement.confirmed" type="primary" :loading="confirming" @click="handleConfirm(currentAnnouncement)">
          确认公告
        </el-button>
        <el-button v-else type="success" disabled>已确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { announcementApi } from '@/api'
import { ElMessage } from 'element-plus'
import { School, Clock, User } from '@element-plus/icons-vue'

const announcements = ref([]); const loading = ref(false)
const page = ref(1); const pageSize = ref(10); const total = ref(0)

// 详情弹窗
const detailDialogVisible = ref(false); const currentAnnouncement = ref({})
const confirming = ref(false)

const formatTime = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const truncateContent = (content) => {
  if (!content) return '暂无内容'
  // 去除 HTML 标签获取纯文本
  const text = content.replace(/<[^>]*>/g, '')
  return text.length > 150 ? text.substring(0, 150) + '...' : text
}

const formatContent = (content) => {
  if (!content) return '<p style="color:#909399">暂无内容</p>'
  // 将换行转为 <br>，保留基本格式
  return content.replace(/\n/g, '<br>')
}

const fetchAnnouncements = async () => {
  loading.value = true
  try {
    const res = await announcementApi.list({ page: page.value, size: pageSize.value })
    if (res.data.code === 200) {
      announcements.value = res.data.data.records
      total.value = res.data.data.total
    }
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

const openDetail = (announcement) => {
  currentAnnouncement.value = { ...announcement }
  detailDialogVisible.value = true
}

const handleConfirm = async (announcement) => {
  confirming.value = true
  try {
    await announcementApi.confirm(announcement.id)
    ElMessage.success('公告确认成功')
    announcement.confirmed = true
    announcement.confirmCount = (announcement.confirmCount || 0) + 1
    // 同时更新列表中的对应项
    const idx = announcements.value.findIndex(a => a.id === announcement.id)
    if (idx !== -1) {
      announcements.value[idx].confirmed = true
      announcements.value[idx].confirmCount = (announcements.value[idx].confirmCount || 0) + 1
    }
    if (detailDialogVisible.value) {
      currentAnnouncement.value.confirmed = true
      currentAnnouncement.value.confirmCount = (currentAnnouncement.value.confirmCount || 0) + 1
    }
  } catch (e) { /* ignore */ }
  finally { confirming.value = false }
}

onMounted(() => { fetchAnnouncements() })
</script>

<style scoped>
.page-shell { display: flex; flex-direction: column; gap: 20px; }
.page-title-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.page-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0; }
.panel { background: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); padding: 24px; }

.announcement-list { display: flex; flex-direction: column; gap: 16px; }

.announcement-card {
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.announcement-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.08); }

.announcement-card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.announcement-card-header h3 { margin: 0; font-size: 16px; color: #303133; }

.announcement-desc { font-size: 13px; color: #909399; margin-bottom: 12px; line-height: 1.6; }

.announcement-meta { display: flex; flex-wrap: wrap; gap: 16px; font-size: 12px; color: #909399; margin-bottom: 12px; }
.announcement-meta span { display: flex; align-items: center; gap: 4px; }

.announcement-actions { display: flex; align-items: center; justify-content: flex-end; }

.detail-header { margin-bottom: 4px; }
.detail-meta { display: flex; flex-wrap: wrap; gap: 20px; font-size: 13px; color: #606266; }

.detail-content {
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 50vh;
  overflow-y: auto;
  padding: 8px 0;
}

.empty-panel { display: flex; align-items: center; justify-content: center; padding: 60px 24px; }
</style>
