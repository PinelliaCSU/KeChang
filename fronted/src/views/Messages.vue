<template>
  <div class="page-shell">
    <div class="page-title-bar">
      <h2 class="page-title">私信</h2>
    </div>

    <div class="message-container">
      <!-- 左侧：联系人列表 -->
      <div class="contact-sidebar">
        <div class="search-box">
          <el-input v-model="searchKeyword" placeholder="搜索联系人..." clearable @input="onSearchInput"
            @clear="clearSearch">
            <template #prefix><el-icon>
                <Search />
              </el-icon></template>
          </el-input>
        </div>

        <div class="contact-list">
          <div class="section-header" v-if="searchKeyword.trim() && apiSearchResults.length > 0">
            搜索结果
          </div>
          <!-- API 额外搜索结果 -->
          <div class="contact-item" v-for="c in apiSearchResults" :key="'search-' + c.userId"
            :class="{ active: activePartner?.userId === c.userId }" @click="startChat(c)">
            <el-avatar :size="40" :src="c.avatar">{{ (c.name || c.username)?.charAt(0) }}</el-avatar>
            <div class="contact-info">
              <div class="contact-name-row">
                <span class="contact-name">{{ c.name || c.username }}</span>
                <el-tag size="small"
                  :type="c.type === 'teacher' ? 'success' : c.type === 'student' ? 'warning' : 'info'">
                  {{ c.type === 'teacher' ? '教师' : c.type === 'student' ? '学生' : '用户' }}
                </el-tag>
              </div>
              <span class="contact-course" v-if="c.courseName">{{ c.courseName }}</span>
            </div>
          </div>

          <div class="section-header" v-if="searchKeyword.trim() && apiSearchResults.length > 0">
            我的联系人
          </div>
          <!-- 课程关联 + 已有会话的联系人 -->
          <div class="contact-item" v-for="c in filteredContacts" :key="c.userId"
            :class="{ active: activePartner?.userId === c.userId }" @click="openChat(c)">
            <el-badge :value="c.unreadCount" :hidden="!c.unreadCount" :max="99">
              <el-avatar :size="40" :src="c.avatar">{{ (c.name || c.username)?.charAt(0) }}</el-avatar>
            </el-badge>
            <div class="contact-info">
              <div class="contact-name-row">
                <span class="contact-name">{{ c.name || c.username }}</span>
                <span class="contact-time" v-if="c.lastTime">{{ formatConvTime(c.lastTime) }}</span>
              </div>
              <div class="contact-preview-row">
                <p class="contact-preview" v-if="c.lastContent">
                  {{ truncateText(c.lastContent) }}
                </p>
                <span class="contact-course" v-if="!c.lastContent && c.courseName">{{ c.courseName }}</span>
              </div>
            </div>
          </div>
          <el-empty v-if="!loading && filteredContacts.length === 0 && apiSearchResults.length === 0"
            :description="searchKeyword.trim() ? '未找到匹配的联系人' : '暂无联系人，请先选课或等待学生选课'" :image-size="80" />
        </div>
      </div>

      <!-- 右侧：聊天区 -->
      <div class="chat-area">
        <div class="chat-empty" v-if="!activePartner">
          <el-empty description="选择一位联系人开始私信" :image-size="120" />
        </div>

        <div class="chat-active" v-else>
          <div class="chat-header">
            <el-avatar :size="36" :src="activePartner.avatar">{{ (activePartner.name ||
              activePartner.username)?.charAt(0)
            }}</el-avatar>
            <div class="chat-header-info">
              <span class="chat-partner-name">{{ activePartner.name || activePartner.username }}</span>
              <span class="chat-partner-course" v-if="activePartner.courseName">{{ activePartner.courseName }}</span>
            </div>
            <el-tooltip content="清空聊天记录" placement="top">
              <el-button size="small" circle class="clear-btn" @click="handleClearChat">
                <el-icon>
                  <Delete />
                </el-icon>
              </el-button>
            </el-tooltip>
          </div>

          <div class="chat-messages" ref="msgContainer">
            <div class="message-bubble" v-for="m in displayMessages" :key="m.id"
              :class="m.isMine ? 'sent' : 'received'">
              <div class="bubble-content">{{ m.content }}</div>
              <div class="bubble-time">
                {{ formatMsgTime(m.sendTime) }}
                <span v-if="m.isMine" class="read-status">{{ m.isRead ? '已读' : '未读' }}</span>
              </div>
            </div>
            <div ref="msgBottom" />
          </div>

          <div class="chat-input">
            <el-input v-model="inputText" type="textarea" :rows="3" placeholder="输入消息... Ctrl+Enter 发送"
              @keydown.enter.ctrl.prevent="sendMessage" resize="none" />
            <el-button type="primary" :disabled="!inputText.trim()" @click="sendMessage" :loading="sending">
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { messageApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Delete } from '@element-plus/icons-vue'

const userStore = useUserStore()
const userId = userStore.userId

// 联系人列表（课程关联 + 已有会话合并）
const contacts = ref([])
const loading = ref(false)

// 当前聊天
const activePartner = ref(null)
const messages = ref([])
const inputText = ref('')
const sending = ref(false)
const msgContainer = ref(null)
const msgBottom = ref(null)

// 搜索
const searchKeyword = ref('')
const apiSearchResults = ref([])
let searchTimer = null

// 轮询
let pollTimer = null

// 本地过滤联系人（基于已排序的列表）
const filteredContacts = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return sortedContacts.value
  return sortedContacts.value.filter(c => {
    const name = (c.name || '').toLowerCase()
    const uname = (c.username || '').toLowerCase()
    const course = (c.courseName || '').toLowerCase()
    return name.includes(kw) || uname.includes(kw) || course.includes(kw)
  })
})

// 按时间升序展示消息
const displayMessages = computed(() => {
  return [...messages.value].reverse()
})

// 使用 computed 排序联系人，避免轮询时全量重建数组触发重渲染
const sortedContacts = computed(() => {
  return [...contacts.value].sort((a, b) => {
    if (a.unreadCount > 0 && b.unreadCount === 0) return -1
    if (b.unreadCount > 0 && a.unreadCount === 0) return 1
    if (a.hasHistory && b.hasHistory) {
      const ta = a.lastTime ? new Date(a.lastTime).getTime() : 0
      const tb = b.lastTime ? new Date(b.lastTime).getTime() : 0
      return tb - ta
    }
    if (a.hasHistory && !b.hasHistory) return -1
    if (!a.hasHistory && b.hasHistory) return 1
    return (a.name || a.username || '').localeCompare(b.name || b.username || '', 'zh')
  })
})

// ==================== 数据加载 ====================

// 加载课程关联联系人
const loadContacts = async () => {
  try {
    const res = await messageApi.contacts({})
    if (res.data.code === 200) {
      return res.data.data || []
    }
  } catch (e) { /* ignore */ }
  return []
}

// 加载已有会话
const loadConversationData = async () => {
  try {
    const res = await messageApi.conversations({ page: 1, size: 200 })
    if (res.data.code === 200) {
      return res.data.data.records || []
    }
  } catch (e) { /* ignore */ }
  return []
}

// 合并联系人与会话
const mergeAll = (contactList, conversationList) => {
  const map = new Map()

  // 先添加课程关联联系人
  for (const c of contactList) {
    map.set(c.userId, {
      userId: c.userId,
      name: c.name || '',
      avatar: c.avatar || '',
      username: c.username || '',
      type: c.type || '',
      courseName: c.courseName || '',
      lastContent: '',
      lastTime: null,
      unreadCount: 0,
      hasHistory: false
    })
  }

  // 合并会话数据
  for (const conv of conversationList) {
    const existing = map.get(conv.partnerId)
    if (existing) {
      existing.lastContent = conv.lastContent || ''
      existing.lastTime = conv.lastTime || null
      existing.unreadCount = conv.unreadCount || 0
      existing.hasHistory = true
      // 以会话中的名字为准（可能更新过）
      if (conv.partnerName) existing.name = conv.partnerName
      if (conv.partnerAvatar) existing.avatar = conv.partnerAvatar
    } else {
      // 有聊天记录但不在课程联系人中（可能是旧数据或跨角色）
      map.set(conv.partnerId, {
        userId: conv.partnerId,
        name: conv.partnerName || '',
        avatar: conv.partnerAvatar || '',
        username: conv.partnerUsername || '',
        type: '',
        courseName: '',
        lastContent: conv.lastContent || '',
        lastTime: conv.lastTime || null,
        unreadCount: conv.unreadCount || 0,
        hasHistory: true
      })
    }
  }

  // 排序：未读置顶 → 有历史按时间降序 → 纯联系人按名称
  return Array.from(map.values()).sort((a, b) => {
    if (a.unreadCount > 0 && b.unreadCount === 0) return -1
    if (b.unreadCount > 0 && a.unreadCount === 0) return 1
    if (a.hasHistory && b.hasHistory) {
      const ta = a.lastTime ? new Date(a.lastTime).getTime() : 0
      const tb = b.lastTime ? new Date(b.lastTime).getTime() : 0
      return tb - ta
    }
    if (a.hasHistory && !b.hasHistory) return -1
    if (!a.hasHistory && b.hasHistory) return 1
    return (a.name || a.username || '').localeCompare(b.name || b.username || '', 'zh')
  })
}

// 刷新全部数据
const refreshAll = async () => {
  const [contactList, conversationList] = await Promise.all([
    loadContacts(),
    loadConversationData()
  ])
  contacts.value = mergeAll(contactList, conversationList)
}

// ==================== 搜索 ====================

const onSearchInput = () => {
  // 清除之前的计时器
  if (searchTimer) clearTimeout(searchTimer)
  const kw = searchKeyword.value.trim()

  if (!kw) {
    apiSearchResults.value = []
    return
  }

  // 防抖 300ms 调用 API
  searchTimer = setTimeout(async () => {
    try {
      const res = await messageApi.contacts({ keyword: kw })
      if (res.data.code === 200) {
        const allResults = res.data.data || []
        // 只显示不在本地联系人中的结果
        const localIds = new Set(contacts.value.map(c => c.userId))
        apiSearchResults.value = allResults.filter(r => !localIds.has(r.userId))
      }
    } catch (e) { /* ignore */ }
  }, 300)
}

const clearSearch = () => {
  searchKeyword.value = ''
  apiSearchResults.value = []
}

// 从 API 搜索结果开始聊天
const startChat = (contact) => {
  // 先加入本地联系人
  const exists = contacts.value.find(c => c.userId === contact.userId)
  if (!exists) {
    contacts.value.unshift({
      userId: contact.userId,
      name: contact.name || '',
      avatar: contact.avatar || '',
      username: contact.username || '',
      type: contact.type || '',
      courseName: contact.courseName || '',
      lastContent: '',
      lastTime: null,
      unreadCount: 0,
      hasHistory: false
    })
  }
  clearSearch()
  activePartner.value = contacts.value.find(c => c.userId === contact.userId)
  messages.value = []
  // 如果有历史，加载消息
  loadMessages(true)
}

// ==================== 聊天 ====================

// 从联系人列表打开聊天
const openChat = (contact) => {
  activePartner.value = { ...contact }
  messages.value = []
  loadMessages(true)
  // 清除未读
  contact.unreadCount = 0
}

// 加载消息
const loadMessages = async (silent) => {
  if (!activePartner.value) return
  try {
    const res = await messageApi.messages(activePartner.value.userId, { page: 1, size: 50 })
    if (res.data.code === 200) {
      messages.value = res.data.data.records || []
    }
  } catch (e) {
    if (!silent) { /* ignore */ }
  }
}

// 发送消息
const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || !activePartner.value) return
  sending.value = true
  try {
    const res = await messageApi.send({ receiverId: activePartner.value.userId, content: text })
    if (res.data.code === 200) {
      messages.value.unshift(res.data.data)
      inputText.value = ''
      // 更新本地联系人数据
      const contact = contacts.value.find(c => c.userId === activePartner.value.userId)
      if (contact) {
        contact.lastContent = text
        contact.lastTime = new Date().toISOString()
        contact.hasHistory = true
      }
      await nextTick()
      scrollToBottom()
    }
  } catch (e) { /* ignore */ }
  finally { sending.value = false }
}

// 清空聊天记录
const handleClearChat = async () => {
  if (!activePartner.value) return
  try {
    await ElMessageBox.confirm(
      `确定要清空与「${activePartner.value.name || activePartner.value.username}」的聊天记录吗？此操作不可撤销。`,
      '清空聊天记录',
      { confirmButtonText: '清空', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await messageApi.clearConversation(activePartner.value.userId)
    if (res.data.code === 200) {
      messages.value = []
      // 更新联系人列表中的预览
      const contact = contacts.value.find(c => c.userId === activePartner.value.userId)
      if (contact) {
        contact.lastContent = ''
        contact.lastTime = null
        contact.hasHistory = false
      }
      ElMessage.success('聊天记录已清空')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('清空失败')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (msgBottom.value) {
    msgBottom.value.scrollIntoView({ behavior: 'smooth' })
  }
}

// ==================== 格式化 ====================

const formatConvTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  const timeStr = `${pad(d.getHours())}:${pad(d.getMinutes())}`

  if (d.toDateString() === now.toDateString()) return timeStr

  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) return '昨天'

  if (d.getFullYear() === now.getFullYear()) {
    return `${d.getMonth() + 1}-${pad(d.getDate())}`
  }

  return `${d.getFullYear()}-${d.getMonth() + 1}-${pad(d.getDate())}`
}

const formatMsgTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const pad = n => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const truncateText = (text) => {
  if (!text) return ''
  return text.length > 30 ? text.substring(0, 30) + '...' : text
}

// ==================== 生命周期 ====================

// 新消息自动滚动
watch(() => messages.value.length, async () => {
  await nextTick()
  scrollToBottom()
})

onMounted(async () => {
  loading.value = true
  await refreshAll()
  loading.value = false

  // 轮询：刷新当前聊天消息 + 更新未读数/最新消息
  pollTimer = setInterval(async () => {
    // 发送中跳过消息刷新，避免覆盖刚本地插入的新消息
    if (activePartner.value && !sending.value) {
      await loadMessages(true)
    }
    // 增量更新会话数据（未读数、最新消息预览）
    const conversations = await loadConversationData()
    for (const conv of conversations) {
      const contact = contacts.value.find(c => c.userId === conv.partnerId)
      if (contact) {
        contact.unreadCount = conv.unreadCount || 0
        contact.lastContent = conv.lastContent || contact.lastContent
        contact.lastTime = conv.lastTime || contact.lastTime
        contact.hasHistory = true
      } else if (conv.lastContent) {
        // 有新会话（不在课程联系人中的用户主动发来消息）
        contacts.value.unshift({
          userId: conv.partnerId,
          name: conv.partnerName || '',
          avatar: conv.partnerAvatar || '',
          username: conv.partnerUsername || '',
          type: '',
          courseName: '',
          lastContent: conv.lastContent || '',
          lastTime: conv.lastTime || null,
          unreadCount: conv.unreadCount || 0,
          hasHistory: true
        })
      }
    }
    // 排序由 sortedContacts computed 自动处理，无需手动重建数组触发重渲染
  }, 10000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  if (searchTimer) clearTimeout(searchTimer)
})
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

.message-container {
  display: flex;
  height: calc(100vh - 180px);
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

/* 左侧联系人列表 */
.contact-sidebar {
  width: 320px;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}

.search-box {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
}

.contact-list {
  flex: 1;
  overflow-y: auto;
}

.section-header {
  padding: 10px 16px;
  font-size: 13px;
  color: #909399;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid #f5f5f5;
}

.contact-item:hover {
  background: #f8f9fa;
}

.contact-item.active {
  background: rgba(32, 196, 204, 0.12);
  border-left: 3px solid var(--bio-primary);
}

.contact-info {
  flex: 1;
  overflow: hidden;
  min-width: 0;
}

.contact-name-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
}

.contact-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.contact-time {
  font-size: 11px;
  color: #c0c4cc;
  flex-shrink: 0;
  margin-left: 8px;
}

.contact-preview-row {
  display: flex;
  align-items: center;
}

.contact-preview {
  margin: 0;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.contact-course {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

/* 右侧聊天区 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-empty,
.chat-active {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-empty {
  align-items: center;
  justify-content: center;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
}

.chat-header-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chat-partner-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.chat-partner-course {
  font-size: 12px;
  color: #909399;
}

.chat-header .clear-btn {
  flex-shrink: 0;
  color: #c0c4cc;
  transition: color 0.15s, background 0.15s;
}

.chat-header .clear-btn:hover {
  color: #e6a23c;
  background: #fdf6ec;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #f9fafb;
}

.message-bubble {
  max-width: 65%;
  display: flex;
  flex-direction: column;
}

.message-bubble.sent {
  align-self: flex-end;
}

.message-bubble.received {
  align-self: flex-start;
}

.bubble-content {
  padding: 10px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-bubble.sent .bubble-content {
  background: var(--bio-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-bubble.received .bubble-content {
  background: #e9ebed;
  color: #303133;
  border-bottom-left-radius: 4px;
}

.bubble-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
  padding: 0 4px;
}

.message-bubble.sent .bubble-time {
  text-align: right;
}

.read-status {
  margin-left: 6px;
}

.chat-input {
  display: flex;
  gap: 12px;
  padding: 14px 20px;
  border-top: 1px solid #ebeef5;
  align-items: flex-end;
  background: #fff;
}

.chat-input :deep(.el-textarea__inner) {
  resize: none;
}

.chat-input .el-button {
  height: 40px;
}
</style>
