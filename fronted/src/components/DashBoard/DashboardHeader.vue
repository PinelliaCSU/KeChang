<template>
    <header class="top-header">
        <div class="header-left">
            <button class="app-switch" type="button" aria-label="应用菜单">
                <span></span><span></span><span></span><span></span>
            </button>
            <div class="logo-area" @click="router.push(roleHome())">
                <img src="@/img/logo.jpg" alt="Logo" class="logo-img" />
                <span class="brand-name">中南大学</span>
            </div>
        </div>

        <div class="header-right">
            <el-dropdown trigger="click" @command="handleMyHomeCmd">
                <span class="header-action">我的主页<el-icon><ArrowDown /></el-icon></span>
                <template #dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item command="dashboard">主页</el-dropdown-item>
                        <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                        <el-dropdown-item command="password">修改密码</el-dropdown-item>
                        <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>

            <span class="divider"></span>

            <span class="header-action header-icon" title="日历" @click="router.push('/calendar')">
                <el-icon :size="18"><Calendar /></el-icon>
            </span>

            <el-popover ref="checkinPopover" placement="bottom" :width="340" trigger="click" @show="onCheckinPopoverShow">
                <template #reference>
                    <span class="header-action header-icon" title="课堂签到">
                        <el-icon :size="18"><Checked /></el-icon>
                        <span class="badge-dot checkin-badge" v-if="activeCheckinCount > 0">{{ activeCheckinCount }}</span>
                    </span>
                </template>
                <CheckinHeaderEntry ref="checkinEntryRef" />
            </el-popover>

            <el-popover ref="notifyPopover" placement="bottom" :width="360" trigger="click" @show="fetchNotifications">
                <template #reference>
                    <span class="header-action header-icon" title="通知">
                        <el-icon :size="18"><Bell /></el-icon>
                        <span class="badge-num" v-if="unreadCount > 0">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
                    </span>
                </template>
                <div class="notify-popover">
                    <div class="notify-pop-header">
                        <span>消息通知</span>
                        <div class="notify-header-actions">
                            <a href="javascript:;" @click="markAllRead" v-if="unreadCount > 0">全部已读</a>
                            <span v-if="unreadCount > 0 && notifications.length > 0" class="action-divider">|</span>
                            <a href="javascript:;" @click="handleClearAllNotifies" v-if="notifications.length > 0" class="clear-all-link">清空全部</a>
                        </div>
                    </div>
                    <div v-if="notifications.length === 0" class="notify-empty">暂无通知</div>
                    <div v-for="n in notifications.slice(0, 5)" :key="n.id" class="notify-item">
                        <span class="notify-dot" :class="{ unread: !n.isRead }"></span>
                        <div class="notify-content" @click="handleNotifyClick(n)">
                            <p class="notify-title">{{ n.title }}</p>
                            <p class="notify-time">{{ formatTime(n.createTime) }}</p>
                        </div>
                        <el-tooltip content="删除通知" placement="left">
                            <el-button icon="Delete" size="small" circle class="notify-delete-btn" @click.stop="handleDeleteNotify(n)" />
                        </el-tooltip>
                    </div>
                </div>
            </el-popover>

            <span class="header-action header-icon" title="帮助">
                <el-icon :size="18"><QuestionFilled /></el-icon>
            </span>

            <span class="divider"></span>

            <el-dropdown trigger="click" @command="handleUserCmd">
                <span class="user-area">
                    <span class="user-avatar">{{ displayName.charAt(0) }}</span>
                    <span class="user-name">{{ displayName }}</span>
                    <el-icon><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                        <el-dropdown-item command="password">修改密码</el-dropdown-item>
                        <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </div>
    </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi, notificationApi, checkinApi } from '@/api'
import { passwordInputValidator } from '@/utils/validators'
import CheckinHeaderEntry from '@/components/checkin/CheckinHeaderEntry.vue'

const router = useRouter()
const userStore = useUserStore()

const unreadCount = ref(0)
const notifications = ref([])
const notifyPopover = ref(null)
const checkinPopover = ref(null)
const checkinEntryRef = ref(null)
const activeCheckinCount = ref(0)
let notifyTimer = null
let checkinTimer = null

const displayName = computed(() => userStore.nickname || userStore.username || '同学')

const roleHome = () => {
    if (userStore.isAdmin) return '/admin/courses'
    if (userStore.isTeacher) return '/teacher/dashboard'
    if (userStore.isStudent) return '/student/dashboard'
    return '/dashboard'
}

const fetchNotifications = async () => {
    if (!userStore.isLoggedIn) return
    try {
        const [listRes, countRes] = await Promise.allSettled([
            notificationApi.list({ page: 1, size: 10 }),
            notificationApi.unreadCount()
        ])
        if (listRes.status === 'fulfilled' && listRes.value.data.code === 200) {
            const d = listRes.value.data.data
            notifications.value = Array.isArray(d?.records) ? d.records : Array.isArray(d?.list) ? d.list : Array.isArray(d) ? d : []
        }
        if (countRes.status === 'fulfilled' && countRes.value.data.code === 200) {
            unreadCount.value = countRes.value.data.data || 0
        }
    } catch { /* ignore */ }
}

const markAllRead = async () => {
    try {
        await notificationApi.markAllRead()
        unreadCount.value = 0
        notifications.value = notifications.value.map(n => ({ ...n, isRead: true }))
        ElMessage.success('已全部标为已读')
    } catch {
        ElMessage.error('操作失败')
    }
}

const handleNotifyClick = (n) => {
    notifyPopover.value?.hide()
    if (!n.isRead) {
        notificationApi.markRead(n.id).catch(() => { })
        n.isRead = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
    const prefix = userStore.isTeacher ? '/teacher' : '/student'
    router.push(`${prefix}/notifications/${n.id}`)
}

const handleDeleteNotify = async (n) => {
    try {
        await notificationApi.deleteNotify(n.id)
        notifications.value = notifications.value.filter(item => item.id !== n.id)
        if (!n.isRead) unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch { /* ignore */ }
}

const handleClearAllNotifies = async () => {
    try {
        await ElMessageBox.confirm('确定清空全部消息通知吗？', '清空通知', {
            confirmButtonText: '清空',
            cancelButtonText: '取消',
            type: 'warning'
        })
        await notificationApi.clearAll()
        notifications.value = []
        unreadCount.value = 0
        ElMessage.success('已清空全部通知')
    } catch { /* cancelled */ }
}

const formatTime = (t) => {
    if (!t) return ''
    const d = new Date(t)
    const now = new Date()
    const diff = now - d
    if (diff < 3600000) return `${Math.max(1, Math.floor(diff / 60000))}分钟前`
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
    return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const handleMyHomeCmd = (cmd) => {
    if (cmd === 'dashboard') router.push(roleHome())
    else if (cmd === 'profile') router.push('/profile')
    else if (cmd === 'password') changePassword()
    else if (cmd === 'logout') handleLogout()
}

const handleUserCmd = (cmd) => {
    if (cmd === 'profile') router.push('/profile')
    else if (cmd === 'password') changePassword()
    else if (cmd === 'logout') handleLogout()
}

const changePassword = async () => {
    try {
        const { value: newPwd } = await ElMessageBox.prompt('请输入新密码（至少 6 位）', '修改密码', {
            inputType: 'password',
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputValidator: passwordInputValidator()
        })
        await authApi.changePassword({ password: newPwd })
        ElMessage.success('密码修改成功，请重新登录')
        userStore.clearLoginData()
        router.push('/login')
    } catch { /* cancelled */ }
}

const handleLogout = () => {
    userStore.clearLoginData()
    ElMessage.success('已退出登录')
    router.push('/login')
}

const fetchCheckinCount = async () => {
    if (!userStore.isLoggedIn) return
    try {
        if (userStore.isTeacher) {
            const res = await checkinApi.teacherList({})
            const data = res.data.data || []
            activeCheckinCount.value = data.filter(s => s.status === 'ACTIVE').length
        } else {
            const res = await checkinApi.studentActiveList()
            activeCheckinCount.value = (res.data.data || []).length
        }
    } catch {
        activeCheckinCount.value = 0
    }
}

const onCheckinPopoverShow = () => {
    checkinEntryRef.value?.fetchSessions()
}

onMounted(() => {
    fetchNotifications()
    fetchCheckinCount()
    notifyTimer = setInterval(fetchNotifications, 30000)
    checkinTimer = setInterval(fetchCheckinCount, 30000)
})

onUnmounted(() => {
    if (notifyTimer) clearInterval(notifyTimer)
    if (checkinTimer) clearInterval(checkinTimer)
})
</script>

<style scoped>
.top-header {
    height: 56px;
    background: #3b465f;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 22px 0 0;
    flex-shrink: 0;
    z-index: 100;
    box-shadow: none;
}

.header-left,
.header-right,
.logo-area,
.top-nav,
.header-action,
.user-area {
    display: flex;
    align-items: center;
}

.header-left {
    gap: 20px;
    height: 100%;
}

.app-switch {
    width: 56px;
    height: 56px;
    border: 0;
    background: #334058;
    display: grid;
    grid-template-columns: repeat(2, 10px);
    grid-template-rows: repeat(2, 10px);
    gap: 6px;
    place-content: center;
}

.app-switch span {
    border: 2px solid #20c4cc;
    border-radius: 3px;
}

.logo-area {
    gap: 12px;
    cursor: pointer;
}

.logo-img {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    background: #fff;
}

.brand-name {
    color: rgba(255, 255, 255, .72);
    font-size: 15px;
    font-weight: 500;
}

.header-right {
    gap: 18px;
}

.header-action {
    gap: 4px;
    color: rgba(255, 255, 255, .70);
    font-size: 14px;
    cursor: pointer;
    white-space: nowrap;
    transition: color .16s ease;
}

.header-action:hover,
.user-area:hover {
    color: #fff;
}

.divider {
    width: 1px;
    height: 18px;
    background: rgba(255, 255, 255, .18);
}

.header-icon {
    position: relative;
}

.badge-dot,
.badge-num {
    position: absolute;
    top: -8px;
    right: -10px;
    min-width: 16px;
    height: 16px;
    padding: 0 3px;
    border-radius: 8px;
    background: #ff2f3d;
    color: #fff;
    font-size: 10px;
    line-height: 16px;
    text-align: center;
}

.checkin-badge {
    background: #20c4cc;
}

.user-area {
    gap: 7px;
    color: rgba(255, 255, 255, .72);
    cursor: pointer;
}

.user-avatar {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: #20c4cc;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    font-weight: 600;
}

.user-name {
    font-size: 14px;
}

.notify-popover {
    max-height: 340px;
    overflow-y: auto;
}

.notify-pop-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 10px;
    border-bottom: 1px solid #e5e7eb;
    margin-bottom: 8px;
    font-size: 15px;
    font-weight: 600;
    color: #1f2937;
}

.notify-header-actions {
    display: flex;
    align-items: center;
    gap: 6px;
}

.notify-pop-header a {
    font-size: 12px;
    color: #20c4cc;
    text-decoration: none;
    font-weight: 400;
}

.clear-all-link {
    color: #f59e0b !important;
}

.notify-empty {
    text-align: center;
    color: #8a94a6;
    padding: 22px 0;
    font-size: 13px;
}

.notify-item {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 9px 4px;
    cursor: pointer;
    border-radius: 4px;
}

.notify-item:hover {
    background: #ecfbfc;
}

.notify-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #cbd5e1;
    margin-top: 7px;
    flex-shrink: 0;
}

.notify-dot.unread {
    background: #20c4cc;
}

.notify-content {
    flex: 1;
    min-width: 0;
}

.notify-title {
    font-size: 13px;
    color: #1f2937;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.notify-time {
    font-size: 11px;
    color: #8a94a6;
    margin-top: 2px;
}

.notify-delete-btn {
    flex-shrink: 0;
    width: 22px;
    height: 22px;
    color: #aeb6c4;
    background: transparent;
    border: none;
    opacity: 0;
}

.notify-item:hover .notify-delete-btn {
    opacity: 1;
}
</style>
