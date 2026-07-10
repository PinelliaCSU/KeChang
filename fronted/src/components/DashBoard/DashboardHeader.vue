<template>
    <header class="top-header">
        <div class="header-left">
            <div class="logo-area" @click="router.push(roleHome())" style="cursor:pointer">
                <img src="@/img/logo.jpg" alt="Logo" class="logo-img" />
            </div>
        </div>

        <div class="header-right">
            <!-- 我的主页 -->
            <el-dropdown trigger="click" @command="handleMyHomeCmd">
                <span class="header-action">我的主页<el-icon style="margin-left:4px">
                        <ArrowDown />
                    </el-icon></span>
                <template #dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item command="dashboard">主页</el-dropdown-item>
                        <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                        <el-dropdown-item command="password">修改密码</el-dropdown-item>
                        <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>

            <!-- 日历 -->
            <span class="header-action header-icon" title="日历" @click="router.push('/calendar')" style="cursor:pointer">
                <el-icon :size="18">
                    <Calendar />
                </el-icon>
            </span>

            <!-- 签到 -->
            <el-popover ref="checkinPopover" placement="bottom" :width="340" trigger="click"
                @show="onCheckinPopoverShow">
                <template #reference>
                    <span class="header-action header-icon" title="课堂签到">
                        <el-icon :size="18">
                            <Checked />
                        </el-icon>
                        <span class="badge-dot checkin-badge" v-if="activeCheckinCount > 0">{{ activeCheckinCount
                            }}</span>
                    </span>
                </template>
                <CheckinHeaderEntry ref="checkinEntryRef" />
            </el-popover>

            <!-- 通知 -->
            <el-popover ref="notifyPopover" placement="bottom" :width="340" trigger="click" @show="fetchNotifications">
                <template #reference>
                    <span class="header-action header-icon" title="通知">
                        <el-icon :size="18">
                            <Bell />
                        </el-icon>
                        <span class="badge-dot" v-if="unreadCount > 0"></span>
                    </span>
                </template>
                <div class="notify-popover">
                    <div class="notify-pop-header"><span>消息通知</span><a href="javascript:;" @click="markAllRead"
                            v-if="unreadCount > 0">全部已读</a></div>
                    <div v-if="notifications.length === 0" class="notify-empty">暂无通知</div>
                    <div v-for="n in notifications.slice(0, 5)" :key="n.id" class="notify-item"
                        @click="handleNotifyClick(n)">
                        <span class="notify-dot" :class="{ unread: !n.isRead }"></span>
                        <div class="notify-content">
                            <p class="notify-title">{{ n.title }}</p>
                            <p class="notify-time">{{ formatTime(n.createTime) }}</p>
                        </div>
                    </div>
                </div>
            </el-popover>

            <!-- 帮助 -->
            <el-popover placement="bottom" :width="200" trigger="click">
                <template #reference>
                    <span class="header-action header-icon" title="帮助">
                        <el-icon :size="18">
                            <QuestionFilled />
                        </el-icon>
                    </span>
                </template>
                <div class="help-popover">
                    <p><strong>课畅智能教学平台</strong></p>
                    <p style="margin-top:8px;color:var(--bio-ink-muted);font-size:13px">使用手册</p>
                    <p style="color:var(--bio-ink-muted);font-size:13px">在线客服</p>
                    <p style="color:var(--bio-ink-muted);font-size:13px">kechang@csu.edu.cn</p>
                </div>
            </el-popover>

            <!-- 用户 -->
            <el-dropdown trigger="click" @command="handleUserCmd">
                <span class="user-area"><span class="user-avatar">{{ displayName.charAt(0) }}</span><span
                        class="user-name">{{
                            displayName }}</span></span>
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

// ==================== 通知相关 ====================
const fetchNotifications = async () => {
    if (!userStore.isLoggedIn) return
    try {
        const [listRes, countRes] = await Promise.allSettled([notificationApi.list({ page: 1, size: 10 }), notificationApi.unreadCount()])
        if (listRes.status === 'fulfilled' && listRes.value.data.code === 200) {
            const d = listRes.value.data.data; notifications.value = Array.isArray(d?.records) ? d.records : Array.isArray(d?.list) ? d.list : Array.isArray(d) ? d : []
        }
        if (countRes.status === 'fulfilled' && countRes.value.data.code === 200) unreadCount.value = countRes.value.data.data || 0
    } catch (e) { /* ignore */ }
}

const markAllRead = async () => {
    try { await notificationApi.markAllRead(); unreadCount.value = 0; notifications.value = notifications.value.map(n => ({ ...n, isRead: true })); ElMessage.success('已全部标为已读') } catch (e) { ElMessage.error('操作失败') }
}

const handleNotifyClick = (n) => {
    // 关闭通知悬窗
    if (notifyPopover.value) notifyPopover.value.hide()
    // 若未读，标记为已读
    if (!n.isRead) {
        notificationApi.markRead(n.id).catch(() => { })
        n.isRead = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
    // 跳转到通知详情页
    const prefix = userStore.isTeacher ? '/teacher' : '/student'
    router.push(`${prefix}/notifications/${n.id}`)
}

const formatTime = (t) => {
    if (!t) return ''; const d = new Date(t); const now = new Date(); const diff = now - d
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
    return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ==================== 用户操作 ====================
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
        const { value: newPwd } = await ElMessageBox.prompt('请输入新密码（至少6位）', '修改密码', { inputType: 'password', confirmButtonText: '确定', cancelButtonText: '取消', inputValidator: passwordInputValidator() })
        await authApi.changePassword({ password: newPwd }); ElMessage.success('密码修改成功，请重新登录')
        userStore.clearLoginData(); router.push('/login')
    } catch (e) { /* cancelled */ }
}

const handleLogout = () => { userStore.clearLoginData(); ElMessage.success('已退出登录'); router.push('/login') }

// ==================== 签到相关 ====================
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
    } catch { activeCheckinCount.value = 0 }
}

const onCheckinPopoverShow = () => {
    if (checkinEntryRef.value) checkinEntryRef.value.fetchSessions()
}

onMounted(() => {
    fetchNotifications()
    fetchCheckinCount()
    notifyTimer = setInterval(fetchNotifications, 30000)
    checkinTimer = setInterval(fetchCheckinCount, 30000)
})
onUnmounted(() => {
    if (notifyTimer) { clearInterval(notifyTimer); notifyTimer = null }
    if (checkinTimer) { clearInterval(checkinTimer); checkinTimer = null }
})
</script>

<style scoped>
.top-header {
    height: 60px;
    background: linear-gradient(135deg, #3b4a2e 0%, #4a5c38 40%, #5a6e42 100%);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
    flex-shrink: 0;
    z-index: 100;
    box-shadow: 0 2px 12px rgba(44, 54, 30, 0.18);
    position: relative;
    overflow: hidden;
}

/* 树冠光斑 */
.top-header::after {
    content: '';
    position: absolute;
    inset: 0;
    background:
        radial-gradient(circle at 15% 50%, rgba(255, 255, 255, 0.04) 0%, transparent 50%),
        radial-gradient(circle at 70% 30%, rgba(255, 255, 255, 0.03) 0%, transparent 40%),
        radial-gradient(circle at 90% 70%, rgba(163, 177, 138, 0.15) 0%, transparent 35%);
    pointer-events: none;
}

.header-left {
    display: flex;
    align-items: center;
    gap: 32px;
    position: relative;
    z-index: 1;
}

.logo-area {
    display: flex;
    align-items: center;
    gap: 10px;
}

.logo-img {
    width: 36px;
    height: 36px;
    background-color: transparent;
    border-radius: 50%;
}

.header-right {
    display: flex;
    align-items: center;
    gap: 20px;
    position: relative;
    z-index: 1;
}

.header-action {
    color: rgba(255, 255, 255, 0.78);
    font-size: 13px;
    cursor: pointer;
    display: flex;
    align-items: center;
    transition: color var(--bio-dur-normal) var(--bio-ease-out);
    white-space: nowrap;
}

.header-action:hover {
    color: #d4e8c0;
    filter: brightness(1.1);
}

.header-icon {
    position: relative;
}

.badge-dot {
    position: absolute;
    top: -2px;
    right: -4px;
    width: 14px;
    height: 14px;
    background: #e8a87c;
    border-radius: 50%;
    border: 1.5px solid #4a5c38;
}

.checkin-badge {
    width: auto;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    border-radius: 8px;
    font-size: 10px;
    color: #fff;
    line-height: 16px;
    text-align: center;
    top: -4px;
    right: -8px;
}

.user-area {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 4px 8px;
    border-radius: var(--bio-radius-sm);
    transition: background var(--bio-dur-normal) var(--bio-ease-out);
}

.user-area:hover {
    background: rgba(255, 255, 255, 0.08);
}

.user-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: linear-gradient(135deg, #8cb369, #6b8e4e);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
}

.user-name {
    color: #fff;
    font-size: 14px;
}

.notify-popover {
    max-height: 320px;
    overflow-y: auto;
}

.notify-pop-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 8px;
    border-bottom: 1px solid #e8ecdf;
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 600;
    color: var(--bio-ink);
}

.notify-pop-header a {
    font-size: 12px;
    color: var(--bio-moss);
    text-decoration: none;
    font-weight: 400;
}

.notify-pop-header a:hover {
    filter: brightness(1.15);
}

.notify-empty {
    text-align: center;
    color: var(--bio-ink-faint);
    padding: 20px 0;
    font-size: 13px;
}

.notify-item {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 8px 4px;
    cursor: pointer;
    border-radius: var(--bio-radius-sm);
    transition: background var(--bio-dur-fast) var(--bio-ease-out);
}

.notify-item:hover {
    background: #f7f8f2;
}

.notify-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #d4d8c8;
    margin-top: 6px;
    flex-shrink: 0;
}

.notify-dot.unread {
    background: var(--bio-leaf);
}

.notify-content {
    flex: 1;
    min-width: 0;
}

.notify-title {
    font-size: 13px;
    color: var(--bio-ink);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.notify-time {
    font-size: 11px;
    color: var(--bio-ink-faint);
    margin-top: 2px;
}

.help-popover p {
    margin: 4px 0;
    font-size: 13px;
    color: var(--bio-ink-soft);
}
</style>
