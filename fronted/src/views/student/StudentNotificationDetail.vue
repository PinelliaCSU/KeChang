<template>
    <div class="page-shell">
        <div class="page-title-bar">
            <el-button text @click="goBack">
                <el-icon>
                    <ArrowLeft />
                </el-icon> 返回
            </el-button>
            <h2 class="page-title">通知详情</h2>
        </div>

        <div class="panel" v-loading="loading">
            <template v-if="notification">
                <div class="detail-header">
                    <h3>{{ notification.title }}</h3>
                    <div class="detail-meta">
                        <span><el-icon>
                                <Clock />
                            </el-icon> {{ formatTime(notification.sendTime || notification.createTime) }}</span>
                        <span>
                            <el-tag :type="notification.isRead ? '' : 'warning'" size="small">
                                {{ notification.isRead ? '已读' : '未读' }}
                            </el-tag>
                        </span>
                        <span class="notif-type">{{ notification.notificationType || '系统' }}</span>
                    </div>
                </div>
                <el-divider />
                <div class="detail-content" v-if="notification.content">
                    {{ notification.content }}
                </div>
                <div class="detail-content empty-content" v-else>
                    <p style="color:#909399">暂无内容</p>
                </div>
            </template>
            <div class="empty-panel" v-else-if="!loading">
                <el-empty description="未找到通知" :image-size="100" />
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { notificationApi } from '@/api'
import { ArrowLeft, Clock } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const notification = ref(null)

const formatTime = (t) => {
    if (!t) return '-'
    const d = new Date(t)
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const fetchDetail = async () => {
    loading.value = true
    try {
        const res = await notificationApi.detail(route.params.id)
        if (res.data.code === 200) {
            notification.value = res.data.data
            // 如果未读，自动标记为已读
            if (!notification.value.isRead) {
                await notificationApi.markRead(notification.value.id)
                notification.value.isRead = true
            }
        }
    } catch (e) { /* ignore */ }
    finally { loading.value = false }
}

const goBack = () => {
    router.back()
}

onMounted(() => { fetchDetail() })
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
    gap: 12px;
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
    min-height: 200px;
}

.detail-header {
    margin-bottom: 4px;
}

.detail-header h3 {
    margin: 0 0 12px;
    font-size: 18px;
    color: #303133;
}

.detail-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    font-size: 13px;
    color: #909399;
    align-items: center;
}

.detail-meta span {
    display: flex;
    align-items: center;
    gap: 4px;
}

.notif-type {
    background: #f0f9eb;
    color: #67c23a;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
}

.detail-content {
    font-size: 15px;
    line-height: 1.8;
    color: #303133;
    white-space: pre-wrap;
    word-break: break-word;
    min-height: 100px;
}

.empty-content {
    display: flex;
    align-items: center;
    justify-content: center;
}

.empty-panel {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 60px 24px;
}
</style>
