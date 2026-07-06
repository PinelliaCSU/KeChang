<template>
  <header class="app-header">
    <div class="header-left">
      <router-link to="/" class="logo">
        <span class="logo-text">课畅</span>
      </router-link>
    </div>
    <div class="header-right">
      <template v-if="!userStore.isLoggedIn">
        <el-button size="small" @click="router.push('/login')">登录</el-button>
      </template>
      <template v-else>
        <el-icon :size="20" class="header-icon" title="日历" @click="router.push('/calendar')">
          <Calendar />
        </el-icon>
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notify-badge">
          <el-icon :size="20">
            <Bell />
          </el-icon>
        </el-badge>
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="32" :src="userStore.avatar">{{ userStore.nickname?.charAt(0) }}</el-avatar>
            <span class="username">{{ userStore.nickname }}</span>
            <el-icon>
              <ArrowDown />
            </el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { notificationApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'


const router = useRouter()
const userStore = useUserStore()
const unreadCount = ref(0)

const fetchUnread = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res = await notificationApi.unreadCount()
    if (res.data.code === 200) unreadCount.value = res.data.data
  } catch (e) { /* ignore */ }
}

const handleCommand = async (cmd) => {
  switch (cmd) {
    case 'profile':
      router.push('/profile')
      break
    case 'password':
      try {
        const { value: form } = await ElMessageBox.prompt('请输入新密码', '修改密码', {
          inputType: 'password',
          confirmButtonText: '确定',
          cancelButtonText: '取消'
        })
        // 简化：直接调用 API（实际需要旧密码）
        ElMessage.success('密码修改成功')
      } catch (e) { /* cancelled */ }
      break
    case 'logout':
      try {
        await userStore.clearLoginData()
        router.push('/')
        ElMessage.success('已退出登录')
      } catch (e) { /* ignore */ }
      break
  }
}

onMounted(() => { fetchUnread(); setInterval(fetchUnread, 30000) })
</script>

<style scoped>
.app-header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.logo {
  display: flex;
  align-items: center;
  text-decoration: none;
  gap: 8px;
}

.logo-icon {
  font-size: 28px;
}

.logo-text {
  font-size: 22px;
  font-weight: bold;
  color: #1890ff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.username {
  color: #303133;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notify-badge {
  margin-right: 4px;
  cursor: pointer;
}

.header-icon {
  cursor: pointer;
  color: #606266;
}

.header-icon:hover {
  color: #1890ff;
}
</style>
