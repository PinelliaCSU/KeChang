<template>
    <div class="login-container">
        <!-- 左侧品牌区 -->
        <div class="login-left">
            <div class="brand-content">
                <div class="brand-logo">
                    <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
                        <rect width="64" height="64" rx="16" fill="url(#logoGrad)" />
                        <path d="M20 22h24v4H20zM20 30h24v4H20zM20 38h16v4H20z" fill="#fff" opacity="0.9" />
                        <defs>
                            <linearGradient id="logoGrad" x1="0" y1="0" x2="64" y2="64">
                                <stop offset="0%" stop-color="#20c997" />
                                <stop offset="100%" stop-color="#35495e" />
                            </linearGradient>
                        </defs>
                    </svg>
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
            <div class="login-card">
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
                    还没有账号？<a href="javascript:;" class="register-link" @click="goRegister">立即注册</a>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)

const form = reactive({
    username: '',
    password: ''
})

const rules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
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

const goRegister = () => {
    router.push('/')
}
</script>

<style scoped>
.login-container {
    display: flex;
    min-height: 100vh;
    font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ========== 左侧品牌区 ========== */
.login-left {
    flex: 1;
    background: linear-gradient(135deg, #2b3a4a 0%, #35495e 50%, #1a2a38 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    overflow: hidden;
}

.brand-content {
    text-align: center;
    z-index: 2;
}

.brand-logo {
    margin-bottom: 24px;
    display: inline-block;
}

.brand-title {
    font-size: 42px;
    font-weight: 700;
    color: #fff;
    letter-spacing: 8px;
    margin-bottom: 12px;
}

.brand-desc {
    font-size: 16px;
    color: rgba(255, 255, 255, 0.7);
    letter-spacing: 2px;
}

.decor-circles .circle {
    position: absolute;
    border-radius: 50%;
    border: 2px solid rgba(255, 255, 255, 0.08);
}

.c1 {
    width: 400px;
    height: 400px;
    top: -100px;
    right: -100px;
}

.c2 {
    width: 250px;
    height: 250px;
    bottom: -80px;
    left: -60px;
}

.c3 {
    width: 150px;
    height: 150px;
    top: 50%;
    left: 20%;
    border-color: rgba(32, 201, 151, 0.15);
}

/* ========== 右侧登录区 ========== */
.login-right {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f7fa;
}

.login-card {
    width: 420px;
    background: #fff;
    border-radius: 16px;
    padding: 48px 40px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
}

.login-title {
    font-size: 28px;
    font-weight: 700;
    color: #2b3a4a;
    margin-bottom: 8px;
}

.login-subtitle {
    font-size: 14px;
    color: #909399;
    margin-bottom: 36px;
}

.login-form :deep(.el-input__wrapper) {
    border-radius: 10px;
    background: #f7f8fa;
    box-shadow: none;
    padding: 4px 16px;
}

.login-form :deep(.el-input__wrapper:hover) {
    box-shadow: 0 0 0 1px #20c997 inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px #20c997 inset !important;
}

.login-btn {
    width: 100%;
    height: 48px;
    border-radius: 10px;
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 4px;
    background: linear-gradient(135deg, #35495e, #2b3a4a);
    border: none;
    transition: all 0.3s;
}

.login-btn:hover {
    background: linear-gradient(135deg, #20c997, #1aa87e);
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(32, 201, 151, 0.3);
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
    color: #909399;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 6px;
}

.remember-me input {
    accent-color: #20c997;
}

.forgot-link {
    color: #20c997;
    text-decoration: none;
}

.forgot-link:hover {
    text-decoration: underline;
}

.login-footer-text {
    text-align: center;
    font-size: 14px;
    color: #909399;
    padding-top: 24px;
    border-top: 1px solid #f0f0f0;
}

.register-link {
    color: #20c997;
    font-weight: 600;
    text-decoration: none;
}

.register-link:hover {
    text-decoration: underline;
}
</style>
