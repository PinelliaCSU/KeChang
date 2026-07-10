<template>
    <div class="profile-shell">
        <DashboardHeader />
        <div class="profile-body">
            <!-- 页面标题 + 编辑按钮 -->
            <div class="page-title-bar">
                <h2 class="page-title">个人中心</h2>
                <el-button type="primary" @click="showEditDialog = true">
                    <el-icon style="margin-right:4px">
                        <Edit />
                    </el-icon>编辑资料
                </el-button>
            </div>

            <!-- 信息展示卡片（流式布局） -->
            <div class="panel info-display-card">
                <!-- 用户头像 -->
                <div class="info-row avatar-row">
                    <span class="info-label">头像</span>
                    <div class="info-value">
                        <el-avatar :size="72" class="display-avatar">
                            {{ displayName.charAt(0) }}
                        </el-avatar>
                    </div>
                </div>
                <el-divider class="info-divider" />

                <!-- 用户姓名 -->
                <div class="info-row">
                    <span class="info-label">用户姓名</span>
                    <span class="info-value">{{ displayName }}</span>
                </div>
                <el-divider class="info-divider" />

                <!-- 角色 -->
                <div class="info-row">
                    <span class="info-label">角色</span>
                    <span class="info-value">
                        <el-tag size="small" :type="userStore.isTeacher ? 'warning' : 'success'">
                            {{ roleDisplayName }}
                        </el-tag>
                    </span>
                </div>
                <el-divider class="info-divider" />

                <!-- 用户名 -->
                <div class="info-row">
                    <span class="info-label">用户名</span>
                    <span class="info-value">{{ userStore.username || '-' }}</span>
                </div>
                <el-divider class="info-divider" />

                <!-- 昵称 -->
                <div class="info-row">
                    <span class="info-label">昵称</span>
                    <span class="info-value">{{ userStore.nickname || '-' }}</span>
                </div>
                <el-divider class="info-divider" />

                <!-- 手机号 -->
                <div class="info-row">
                    <span class="info-label">手机号</span>
                    <span class="info-value">{{ userStore.phone || profileForm.phone || '-' }}</span>
                </div>
                <el-divider class="info-divider" />

                <!-- 邮箱 -->
                <div class="info-row">
                    <span class="info-label">邮箱</span>
                    <span class="info-value">{{ userStore.email || profileForm.email || '-' }}</span>
                </div>

                <!-- 学生/教师扩展信息 -->
                <template v-if="userExtInfo.realName">
                    <el-divider class="info-divider" />
                    <div class="info-row">
                        <span class="info-label">真实姓名</span>
                        <span class="info-value">{{ userExtInfo.realName }}</span>
                    </div>
                </template>
                <template v-if="userExtInfo.studentNo">
                    <el-divider class="info-divider" />
                    <div class="info-row">
                        <span class="info-label">学号</span>
                        <span class="info-value">{{ userExtInfo.studentNo }}</span>
                    </div>
                </template>
                <template v-if="userExtInfo.grade">
                    <el-divider class="info-divider" />
                    <div class="info-row">
                        <span class="info-label">年级</span>
                        <span class="info-value">{{ userExtInfo.grade }}</span>
                    </div>
                </template>
                <template v-if="userExtInfo.major">
                    <el-divider class="info-divider" />
                    <div class="info-row">
                        <span class="info-label">专业</span>
                        <span class="info-value">{{ userExtInfo.major }}</span>
                    </div>
                </template>
                <template v-if="userExtInfo.className">
                    <el-divider class="info-divider" />
                    <div class="info-row">
                        <span class="info-label">班级</span>
                        <span class="info-value">{{ userExtInfo.className }}</span>
                    </div>
                </template>
                <template v-if="userExtInfo.title">
                    <el-divider class="info-divider" />
                    <div class="info-row">
                        <span class="info-label">职称</span>
                        <span class="info-value">{{ userExtInfo.title }}</span>
                    </div>
                </template>
                <template v-if="userExtInfo.department">
                    <el-divider class="info-divider" />
                    <div class="info-row">
                        <span class="info-label">院系</span>
                        <span class="info-value">{{ userExtInfo.department }}</span>
                    </div>
                </template>
            </div>

            <!-- 账户统计 -->
            <div class="panel stats-card">
                <h3 class="card-title">账户统计</h3>
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

        <!-- 编辑资料弹窗组件 -->
        <ProfileEditDialog v-model="showEditDialog" :user-info="editUserInfo" @saved="onProfileSaved" />
    </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { Edit } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { authApi, courseApi, assignmentApi, examApi } from '@/api'
import DashboardHeader from '@/components/DashBoard/DashboardHeader.vue'
import ProfileEditDialog from '@/components/ProfileEditDialog.vue'

const userStore = useUserStore()

const showEditDialog = ref(false)

const displayName = computed(() => userStore.nickname || userStore.username || '同学')

const roleDisplayName = computed(() => {
    if (userStore.isTeacher) return '教师'
    if (userStore.isStudent) return '学生'
    return userStore.roleName || '-'
})

// 用于传递给编辑弹窗的当前用户信息
const editUserInfo = computed(() => ({
    username: userStore.username || '',
    nickname: userStore.nickname || '',
    phone: userStore.phone || '',
    email: userStore.email || '',
    avatar: userStore.avatar || '',
    realName: userExtInfo.value.realName
}))

// 本地缓存的表单数据（用于展示 phone/email 等 store 中可能不存在的字段）
const profileForm = reactive({
    phone: '',
    email: ''
})

// 从后端获取的扩展信息
const userExtInfo = ref({})

const stats = reactive({
    courseCount: 0,
    todoCount: 0,
    completedCount: 0
})

// ==================== 加载数据 ====================
const loadFullProfile = async () => {
    try {
        const res = await authApi.me()
        if (res.data?.code === 200) {
            const user = res.data.data
            profileForm.phone = user.phone || ''
            profileForm.email = user.email || ''
            userExtInfo.value = {
                realName: user.realName || '',
                studentNo: user.studentNo || '',
                grade: user.grade || '',
                major: user.major || '',
                className: user.className || '',
                title: user.title || '',
                department: user.department || ''
            }
            // 同步到 store
            if (user.nickname) {
                userStore.nickname = user.nickname
                localStorage.setItem('nickname', user.nickname)
            }
            if (user.avatar) {
                userStore.avatar = user.avatar
                localStorage.setItem('avatar', user.avatar)
            }
        }
    } catch (e) {
        // 请求失败时使用 store 中的缓存数据
        profileForm.phone = localStorage.getItem('phone') || ''
        profileForm.email = localStorage.getItem('email') || ''
    }
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

// ==================== 编辑保存回调 ====================
const onProfileSaved = (savedData) => {
    // 更新本地缓存的展示数据
    if (savedData.phone !== undefined) profileForm.phone = savedData.phone
    if (savedData.email !== undefined) profileForm.email = savedData.email
    // 同步到 localStorage
    if (savedData.phone) localStorage.setItem('phone', savedData.phone)
    if (savedData.email) localStorage.setItem('email', savedData.email)
}

onMounted(() => { loadFullProfile(); loadStats() })
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
    padding: 24px 32px;
    display: flex;
    flex-direction: column;
    gap: 20px;
    max-width: 720px;
    margin: 0 auto;
    width: 100%;
}

/* ========== 页面标题栏 ========== */
.page-title-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.page-title {
    font-size: 22px;
    font-weight: 700;
    color: #1d2129;
    margin: 0;
}

/* ========== 面板通用 ========== */
.panel {
    background: #fff;
    border-radius: 14px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
    padding: 28px 32px;
}

/* ========== 信息展示卡片（流式布局） ========== */
.info-display-card {
    display: flex;
    flex-direction: column;
}

.info-row {
    display: flex;
    align-items: center;
    padding: 6px 0;
    min-height: 44px;
}

.info-label {
    width: 90px;
    flex-shrink: 0;
    font-size: 14px;
    color: #86909c;
    font-weight: 500;
}

.info-value {
    flex: 1;
    font-size: 15px;
    color: #1d2129;
    display: flex;
    align-items: center;
}

.avatar-row {
    padding: 4px 0 12px;
}

.display-avatar {
    background: linear-gradient(135deg, #20c997, #0d9488);
    color: #fff;
    font-size: 28px;
    font-weight: 600;
}

.info-divider {
    margin: 4px 0;
}

/* ========== 统计卡片 ========== */
.card-title {
    font-size: 16px;
    font-weight: 600;
    margin: 0 0 20px;
    color: #1d2129;
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
}

.stat-item {
    text-align: center;
    padding: 20px 12px;
    border-radius: 12px;
    background: #f8f9fb;
    transition: transform 0.2s, box-shadow 0.2s;
}

.stat-item:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(32, 201, 151, 0.1);
}

.stat-value {
    display: block;
    font-size: 30px;
    font-weight: 700;
    color: #20c997;
    line-height: 1.3;
}

.stat-label {
    font-size: 13px;
    color: #86909c;
    margin-top: 4px;
    display: block;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
    .profile-body {
        padding: 16px;
        max-width: 100%;
    }

    .panel {
        padding: 20px 16px;
    }

    .info-label {
        width: 70px;
        font-size: 13px;
    }

    .info-value {
        font-size: 14px;
    }

    .stats-grid {
        grid-template-columns: repeat(3, 1fr);
        gap: 10px;
    }

    .stat-value {
        font-size: 24px;
    }
}
</style>
