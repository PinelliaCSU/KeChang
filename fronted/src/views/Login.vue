<template>
    <div class="login-container">
        <!-- 左侧品牌区 -->
        <div class="login-left">
            <div class="brand-content">
                <div class="brand-logo">
                    <img src="@/img/logo.jpg" alt="课畅" class="login-logo-img" />
                </div>
                <h1 class="brand-title">课畅</h1>
                <p class="brand-desc">智能教学平台 · 让知识流动起来</p>
            </div>
            <div class="decor-circles">
                <span class="circle c1"></span>
                <span class="circle c2"></span>
                <span class="circle c3"></span>
            </div>
        </div>

        <!-- 右侧登录表单 -->
        <div class="login-right">
            <!-- 登录表单 -->
            <div class="login-card" v-if="!isRegisterMode">
                <h2 class="login-title">欢迎回来</h2>
                <p class="login-subtitle">登录您的课畅账号</p>

                <el-form ref="formRef" :model="form" :rules="rules" size="large" class="login-form">
                    <el-form-item prop="username">
                        <el-input v-model="form.username" placeholder="请输入用户名 / 学号 / 工号" :prefix-icon="User"
                            clearable />
                    </el-form-item>

                    <el-form-item prop="password">
                        <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock"
                            show-password @keyup.enter="handleLogin" />
                    </el-form-item>

                    <el-form-item>
                        <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
                            {{ loading ? '登录中...' : '登 录' }}
                        </el-button>
                    </el-form-item>
                </el-form>

                <div class="login-extra">
                    <label class="remember-me">
                        <input type="checkbox" v-model="rememberMe" /> 记住密码
                    </label>
                    <a href="javascript:;" class="forgot-link">忘记密码？</a>
                </div>

                <div class="login-footer-text">
                    还没有账号？<a href="javascript:;" class="register-link" @click="isRegisterMode = true">立即注册</a>
                </div>
            </div>

            <!-- 注册表单 -->
            <div class="login-card" v-else>
                <h2 class="login-title">创建账号</h2>
                <p class="login-subtitle">加入课畅，开启学习之旅</p>

                <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" size="large"
                    class="login-form">
                    <el-form-item prop="roleType">
                        <div class="role-selector">
                            <div class="role-option" :class="{ active: registerForm.roleType === 'student' }"
                                @click="registerForm.roleType = 'student'">
                                <span class="role-icon">
                                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                        stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M22 10v6M2 10l10-5 10 5-10 5z" />
                                        <path d="M6 12v5c0 1.1 2.7 2 6 2s6-.9 6-2v-5" />
                                    </svg>
                                </span>
                                <span class="role-label">学生</span>
                            </div>
                            <div class="role-option" :class="{ active: registerForm.roleType === 'teacher' }"
                                @click="registerForm.roleType = 'teacher'">
                                <span class="role-icon">
                                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                        stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                                        <circle cx="12" cy="8" r="4" />
                                        <path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" />
                                    </svg>
                                </span>
                                <span class="role-label">教师</span>
                            </div>
                        </div>
                    </el-form-item>

                    <el-form-item prop="email">
                        <el-input v-model="registerForm.email" placeholder="请输入邮箱" :prefix-icon="Message" clearable />
                    </el-form-item>

                    <el-form-item prop="nickname">
                        <el-input v-model="registerForm.nickname" placeholder="请输入昵称（选填）" :prefix-icon="User"
                            clearable />
                    </el-form-item>

                    <el-form-item prop="password">
                        <el-input v-model="registerForm.password" type="password" placeholder="请设置密码（至少6位）"
                            :prefix-icon="Lock" show-password />
                    </el-form-item>

                    <el-form-item prop="confirmPassword">
                        <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码"
                            :prefix-icon="Lock" show-password @keyup.enter="handleRegister" />
                    </el-form-item>

                    <el-form-item>
                        <el-button type="primary" class="login-btn" :loading="loading" @click="handleRegister">
                            {{ loading ? '注册中...' : '注 册' }}
                        </el-button>
                    </el-form-item>
                </el-form>

                <div class="login-footer-text">
                    已有账号？<a href="javascript:;" class="register-link" @click="isRegisterMode = false">立即登录</a>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const registerFormRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)
const isRegisterMode = ref(false)

const form = reactive({
    username: '',
    password: ''
})

const registerForm = reactive({
    roleType: 'student',
    email: '',
    nickname: '',
    password: '',
    confirmPassword: ''
})

const rules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const validateConfirmPassword = (rule, value, callback) => {
    if (value === '') {
        callback(new Error('请再次输入密码'))
    } else if (value !== registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
    } else {
        callback()
    }
}

const registerRules = {
    email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请设置密码', trigger: 'blur' },
        { min: 6, message: '密码长度至少6位', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, validator: validateConfirmPassword, trigger: 'blur' }
    ]
}

const handleLogin = async () => {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return

    loading.value = true
    try {
        const res = await authApi.login({
            username: form.username,
            password: form.password
        })
        if (res.data.code === 200) {
            userStore.setLoginData(res.data.data)
            ElMessage.success('登录成功，欢迎回来！')
            router.push('/dashboard')
        } else {
            ElMessage.error(res.data.message || '登录失败')
        }
    } catch (e) {
        ElMessage.error(e.response?.data?.message || '登录失败，请检查网络连接')
    } finally {
        loading.value = false
    }
}

const handleRegister = async () => {
    const valid = await registerFormRef.value.validate().catch(() => false)
    if (!valid) return

    loading.value = true
    try {
        const res = await authApi.register({
            roleType: registerForm.roleType,
            email: registerForm.email,
            password: registerForm.password,
            confirmPassword: registerForm.confirmPassword,
            nickname: registerForm.nickname || undefined
        })
        if (res.data.code === 200) {
            ElMessage.success('注册成功，已自动登录！')
            userStore.setLoginData(res.data.data)
            router.push('/dashboard')
        } else {
            ElMessage.error(res.data.message || '注册失败')
        }
    } catch (e) {
        ElMessage.error(e.response?.data?.message || '注册失败，请检查网络连接')
    } finally {
        loading.value = false
    }
}

</script>

<style scoped>
.login-container {
    display: flex;
    min-height: 100vh;
    font-family: var(--bio-font);
}

/* ========== 左侧品牌区 — 森林冠层 ========== */
.login-left {
    flex: 1;
    background: linear-gradient(165deg, #3b4a2e 0%, #4a5c38 30%, #5a7040 60%, #4a5c38 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: hidden;
}

/* 林间光斑 */
.login-left::before {
    content: '';
    position: absolute;
    inset: 0;
    background:
        radial-gradient(ellipse 50% 40% at 25% 30%, rgba(163, 177, 138, 0.25) 0%, transparent 60%),
        radial-gradient(ellipse 30% 50% at 70% 60%, rgba(212, 232, 192, 0.18) 0%, transparent 55%),
        radial-gradient(circle at 85% 15%, rgba(255, 255, 255, 0.06) 0%, transparent 40%),
        radial-gradient(circle at 10% 80%, rgba(255, 255, 255, 0.04) 0%, transparent 35%);
    pointer-events: none;
}

/* 叶脉装饰 SVG */
.login-left::after {
    content: '';
    position: absolute;
    inset: 0;
    background-image:
        url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 600 800'%3E%3Cpath d='M120 700 Q200 600 180 500 Q160 400 220 300 Q280 200 260 100' fill='none' stroke='rgba(180,200,150,0.1)' stroke-width='1.5'/%3E%3Cpath d='M140 680 Q210 590 190 490' fill='none' stroke='rgba(180,200,150,0.08)' stroke-width='1'/%3E%3Cpath d='M480 150 Q400 250 420 350 Q440 450 380 550 Q320 650 340 750' fill='none' stroke='rgba(180,200,150,0.1)' stroke-width='1.5'/%3E%3Cpath d='M500 170 Q430 260 450 360' fill='none' stroke='rgba(180,200,150,0.08)' stroke-width='1'/%3E%3Ccircle cx='55' cy='120' r='3' fill='rgba(210,230,190,0.12)'/%3E%3Ccircle cx='530' cy='680' r='4' fill='rgba(210,230,190,0.1)'/%3E%3Ccircle cx='500' cy='90' r='2' fill='rgba(255,255,255,0.06)'/%3E%3Ccircle cx='80' cy='600' r='3' fill='rgba(200,220,180,0.08)'/%3E%3C/svg%3E");
    background-size: cover;
    pointer-events: none;
}

.brand-content {
    text-align: center;
    z-index: 2;
}

.brand-logo {
    margin-bottom: 24px;
    display: inline-block;
}

.login-logo-img {
    width: 72px;
    height: 72px;
    background: transparent;
    border-radius: 50%;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.brand-title {
    font-size: 42px;
    font-weight: 700;
    color: #f5f8ef;
    letter-spacing: 8px;
    margin-bottom: 12px;
}

.brand-desc {
    font-size: 16px;
    color: rgba(235, 242, 220, 0.7);
    letter-spacing: 2px;
}

/* 装饰圆 — 有机叶形 */
.decor-circles .circle {
    position: absolute;
    border-radius: 50%;
    border: 2px solid rgba(180, 200, 150, 0.1);
}

.c1 {
    width: 400px;
    height: 400px;
    top: -120px;
    right: -120px;
}

.c2 {
    width: 280px;
    height: 280px;
    bottom: -100px;
    left: -80px;
}

.c3 {
    width: 160px;
    height: 160px;
    top: 55%;
    left: 25%;
    border-color: rgba(180, 200, 150, 0.18);
}

/* ========== 右侧登录区 — 纸纤维质感 ========== */
.login-right {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    background:
        radial-gradient(ellipse 70% 50% at 20% 30%, rgba(163, 177, 138, 0.05) 0%, transparent 60%),
        radial-gradient(ellipse 50% 60% at 80% 70%, rgba(165, 196, 212, 0.05) 0%, transparent 60%),
        #fdfbf7;
}

.login-card {
    width: 420px;
    background: var(--bio-paper);
    border-radius: var(--bio-radius-lg);
    padding: 48px 40px;
    box-shadow: var(--bio-shadow-md);
    border: 1px solid rgba(163, 177, 138, 0.1);
    transition: box-shadow var(--bio-dur-slow) var(--bio-ease-out);
}

.login-card:hover {
    box-shadow: var(--bio-shadow-lg);
}

.login-title {
    font-size: 28px;
    font-weight: 700;
    color: var(--bio-ink);
    margin-bottom: 8px;
}

.login-subtitle {
    font-size: 14px;
    color: var(--bio-ink-muted);
    margin-bottom: 36px;
}

.login-form :deep(.el-input__wrapper) {
    border-radius: var(--bio-radius-md);
    background: #f8f6f1;
    box-shadow: none;
    padding: 4px 16px;
    border: 1px solid rgba(163, 177, 138, 0.1);
    transition: all var(--bio-dur-normal) var(--bio-ease-out);
}

.login-form :deep(.el-input__wrapper:hover) {
    border-color: rgba(124, 140, 90, 0.3);
    filter: brightness(1.02);
}

.login-form :deep(.el-input__wrapper.is-focus) {
    border-color: var(--bio-leaf) !important;
    box-shadow: 0 0 0 3px rgba(127, 140, 82, 0.1) !important;
}

.login-btn {
    width: 100%;
    height: 48px;
    border-radius: var(--bio-radius-md);
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 4px;
    background: linear-gradient(135deg, var(--bio-moss) 0%, var(--bio-leaf) 100%);
    border: none;
    transition: filter var(--bio-dur-normal) var(--bio-ease-out),
        transform var(--bio-dur-fast) var(--bio-ease-out);
}

.login-btn:hover {
    filter: brightness(1.12);
}

.login-btn:active {
    transform: translateY(1px);
    filter: brightness(0.96);
}

.login-extra {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: -8px;
    margin-bottom: 24px;
    font-size: 13px;
}

.remember-me {
    color: var(--bio-ink-muted);
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 6px;
}

.remember-me input {
    accent-color: var(--bio-leaf);
}

.forgot-link {
    color: var(--bio-moss);
    text-decoration: none;
    transition: filter var(--bio-dur-fast) var(--bio-ease-out);
}

.forgot-link:hover {
    filter: brightness(1.2);
}

.login-footer-text {
    text-align: center;
    font-size: 14px;
    color: var(--bio-ink-muted);
    padding-top: 24px;
    border-top: 1px solid rgba(163, 177, 138, 0.1);
}

.register-link {
    color: var(--bio-moss);
    font-weight: 600;
    text-decoration: none;
    transition: filter var(--bio-dur-fast) var(--bio-ease-out);
}

.register-link:hover {
    filter: brightness(1.2);
}

/* 角色选择器 — 有机卡片 */
.role-selector {
    display: flex;
    gap: 16px;
    width: 100%;
}

.role-option {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 16px 8px;
    border: 2px solid rgba(163, 177, 138, 0.15);
    border-radius: var(--bio-radius-md);
    cursor: pointer;
    transition: all var(--bio-dur-normal) var(--bio-ease-out);
    background: #fdfbf7;
}

.role-option:hover {
    border-color: rgba(124, 140, 90, 0.35);
    background: #f7f8f2;
    filter: brightness(1.02);
}

.role-option.active {
    border-color: var(--bio-leaf);
    background: linear-gradient(135deg, rgba(163, 177, 138, 0.12), rgba(163, 177, 138, 0.06));
    box-shadow: 0 0 0 3px rgba(127, 140, 82, 0.08);
}

.role-option:active {
    transform: translateY(1px);
}

.role-icon {
    font-size: 28px;
}

.role-label {
    font-size: 14px;
    font-weight: 600;
    color: var(--bio-ink-soft);
}

.role-option.active .role-label {
    color: var(--bio-moss);
}
</style>
