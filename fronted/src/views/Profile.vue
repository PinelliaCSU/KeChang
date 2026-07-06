<template>
    <div class="profile-shell">
        <DashboardHeader />
        <div class="profile-body">
            <div class="page-title-bar">
                <h2 class="page-title">👤 个人中心</h2>
            </div>

            <div class="profile-grid">
                <!-- 左侧：基本信息卡片 -->
                <div class="panel info-card">
                    <div class="avatar-section">
                        <el-avatar :size="80" class="profile-avatar">{{ displayName.charAt(0) }}</el-avatar>
                        <div class="avatar-info">
                            <h3 class="user-display-name">{{ displayName }}</h3>
                            <el-tag size="small" :type="userStore.isTeacher ? 'warning' : 'success'">
                                {{ userStore.isTeacher ? '教师' : '学生' }}
                            </el-tag>
                        </div>
                    </div>
                    <el-divider />
                    <el-form :model="profileForm" label-width="80px" class="profile-form">
                        <el-form-item label="用户名">
                            <el-input v-model="profileForm.username" disabled />
                        </el-form-item>
                        <el-form-item label="昵称">
                            <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="20"
                                show-word-limit />
                        </el-form-item>
                        <el-form-item label="手机号">
                            <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
                        </el-form-item>
                        <el-form-item label="邮箱">
                            <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" @click="handleSaveProfile" :loading="saving">保存修改</el-button>
                        </el-form-item>
                    </el-form>
                </div>

                <!-- 右侧：修改密码 + 账户统计 -->
                <div class="right-col">
                    <div class="panel password-card">
                        <h3 class="card-title">🔒 修改密码</h3>
                        <el-form :model="pwdForm" label-width="80px" class="profile-form">
                            <el-form-item label="原密码">
                                <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码"
                                    show-password />
                            </el-form-item>
                            <el-form-item label="新密码">
                                <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码（至少6位）"
                                    show-password />
                            </el-form-item>
                            <el-form-item label="确认密码">
                                <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码"
                                    show-password />
                            </el-form-item>
                            <el-form-item>
                                <el-button type="warning" @click="handleChangePwd"
                                    :loading="changingPwd">修改密码</el-button>
                            </el-form-item>
                        </el-form>
                    </div>

                    <div class="panel stats-card">
                        <h3 class="card-title">📊 账户统计</h3>
                        <div class="stats-grid">
                            <div class="stat-item">
                                <span class="stat-value">{{ stats.courseCount }}</span>
                                <span class="stat-label">课程数</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-value">{{ stats.todoCount }}</span>
                                <span class="stat-label">待办数</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-value">{{ stats.completedCount }}</span>
                                <span class="stat-label">已完成</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi, courseApi, assignmentApi, examApi } from '@/api'
import DashboardHeader from '@/components/DashboardHeader.vue'

const router = useRouter()
const userStore = useUserStore()

const saving = ref(false)
const changingPwd = ref(false)

const displayName = computed(() => userStore.nickname || userStore.username || '同学')

const profileForm = reactive({
    username: '',
    nickname: '',
    phone: '',
    email: ''
})

const pwdForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})

const stats = reactive({
    courseCount: 0,
    todoCount: 0,
    completedCount: 0
})

// ==================== 加载数据 ====================
const loadProfile = () => {
    profileForm.username = userStore.username || ''
    profileForm.nickname = userStore.nickname || ''
    profileForm.phone = userStore.phone || ''
    profileForm.email = userStore.email || ''
}

const loadStats = async () => {
    try {
        const [courseRes, assignRes, examRes] = await Promise.allSettled([
            courseApi.list({ page: 1, size: 1 }),
            assignmentApi.list({ page: 1, size: 1 }),
            examApi.listExams({ page: 1, size: 1 })
        ])
        const getTotal = (res) => {
            if (res.status !== 'fulfilled') return 0
            return res.value.data?.data?.total || 0
        }
        stats.courseCount = getTotal(courseRes)
        stats.todoCount = getTotal(assignRes) + getTotal(examRes)
        stats.completedCount = Math.floor(Math.random() * 10) // 占位，后续接真实API
    } catch (e) { /* ignore */ }
}

// ==================== 保存 ====================
const handleSaveProfile = async () => {
    saving.value = true
    try {
        userStore.nickname = profileForm.nickname
        localStorage.setItem('nickname', profileForm.nickname)
        // TODO: 调用后端更新 profile API
        ElMessage.success('个人信息已更新')
    } catch (e) {
        ElMessage.error('保存失败')
    } finally { saving.value = false }
}

const handleChangePwd = async () => {
    if (!pwdForm.oldPassword) { ElMessage.warning('请输入原密码'); return }
    if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) { ElMessage.warning('新密码至少6位'); return }
    if (pwdForm.newPassword !== pwdForm.confirmPassword) { ElMessage.warning('两次密码不一致'); return }

    changingPwd.value = true
    try {
        await authApi.changePassword({ password: pwdForm.newPassword })
        ElMessage.success('密码修改成功，请重新登录')
        userStore.clearLoginData()
        router.push('/login')
    } catch (e) {
        ElMessage.error(e.response?.data?.message || '密码修改失败')
    } finally { changingPwd.value = false }
}

onMounted(() => { loadProfile(); loadStats() })
</script>

<style scoped>
.profile-shell {
    display: flex;
    flex-direction: column;
    height: 100vh;
    font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
    background: #f5f7fa;
    color: #303133;
    overflow: hidden;
}

.profile-body {
    flex: 1;
    overflow-y: auto;
    padding: 24px;
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.page-title-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.page-title {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    margin: 0;
}

.profile-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
    align-items: start;
}

@media (max-width: 900px) {
    .profile-grid {
        grid-template-columns: 1fr;
    }
}

.panel {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    padding: 24px;
}

.right-col {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.card-title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 16px;
    color: #303133;
}

.avatar-section {
    display: flex;
    align-items: center;
    gap: 20px;
    margin-bottom: 8px;
}

.profile-avatar {
    background: linear-gradient(135deg, #20c997, #0d9488);
    color: #fff;
    font-size: 32px;
    font-weight: 600;
}

.avatar-info h3 {
    font-size: 20px;
    margin: 0 0 6px 0;
    color: #303133;
}

.profile-form {
    max-width: 400px;
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
}

.stat-item {
    text-align: center;
    padding: 16px 8px;
    border-radius: 10px;
    background: #f8f9fb;
}

.stat-value {
    display: block;
    font-size: 28px;
    font-weight: 700;
    color: #20c997;
}

.stat-label {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
}
</style>
