<template>
  <div class="login-page">
    <section class="brand-panel">
      <!-- 装饰浮球 -->
      <div class="float-orb orb-1"></div>
      <div class="float-orb orb-2"></div>
      <div class="float-orb orb-3"></div>
      <div class="float-orb orb-4"></div>

      <img src="@/img/logo.jpg" alt="课畅" class="brand-logo" />
      <h1><span class="char char-ke">课</span><span class="char char-chang">畅</span></h1>
      <p class="brand-sub">智能教学平台 · 让课程管理更顺手</p>
    </section>

    <section class="form-panel">
      <div class="login-card">
        <transition name="form-swap" mode="out-in">
          <div v-if="!isRegisterMode" key="login">
            <h2>欢迎回来</h2>
            <p class="subtitle">登录你的课畅账号</p>
            <el-form ref="formRef" :model="form" :rules="rules" size="large">
              <el-form-item prop="username">
                <el-input v-model="form.username" placeholder="用户名 / 邮箱" :prefix-icon="User" clearable class="input-animated" />
              </el-form-item>
              <el-form-item prop="password">
                <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password
                  @keyup.enter="handleLogin" class="input-animated" />
              </el-form-item>
              <el-button type="primary" class="submit-btn btn-shimmer" :loading="loading" @click="handleLogin">登录</el-button>
            </el-form>
            <p class="switch-text">还没有账号？<a href="javascript:;" @click="isRegisterMode = true">立即注册</a></p>
          </div>

          <div v-else key="register">
            <h2>创建账号</h2>
            <p class="subtitle">完善基础资料，后续课程名单会更清楚</p>
            <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" size="large">
              <el-form-item prop="roleType">
                <el-segmented v-model="registerForm.roleType" :options="roleOptions" style="width:100%" />
              </el-form-item>
              <el-form-item prop="email">
                <el-input v-model="registerForm.email" placeholder="邮箱" :prefix-icon="Message" clearable class="input-animated" />
              </el-form-item>
              <el-form-item prop="nickname">
                <el-input v-model="registerForm.nickname" placeholder="昵称（可选）" :prefix-icon="User" clearable class="input-animated" />
              </el-form-item>
              <el-form-item prop="realName">
                <el-input v-model="registerForm.realName" placeholder="真实姓名" :prefix-icon="User" clearable class="input-animated" />
              </el-form-item>

              <template v-if="registerForm.roleType === 'student'">
                <el-form-item prop="studentNo">
                  <el-input v-model="registerForm.studentNo" placeholder="学号（可选，不填自动生成）" clearable class="input-animated" />
                </el-form-item>
                <el-form-item prop="grade">
                  <el-input v-model="registerForm.grade" placeholder="年级，如 2024级" clearable class="input-animated" />
                </el-form-item>
                <el-form-item prop="major">
                  <el-input v-model="registerForm.major" placeholder="专业" clearable class="input-animated" />
                </el-form-item>
                <el-form-item prop="className">
                  <el-input v-model="registerForm.className" placeholder="班级" clearable class="input-animated" />
                </el-form-item>
              </template>

              <template v-else>
                <el-form-item prop="department">
                  <el-input v-model="registerForm.department" placeholder="院系/部门" clearable class="input-animated" />
                </el-form-item>
                <el-form-item prop="title">
                  <el-input v-model="registerForm.title" placeholder="职称" clearable class="input-animated" />
                </el-form-item>
              </template>

              <el-form-item prop="password">
                <el-input v-model="registerForm.password" type="password" placeholder="密码（至少6位）" :prefix-icon="Lock"
                  show-password class="input-animated" />
              </el-form-item>
              <el-form-item prop="confirmPassword">
                <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock"
                  show-password @keyup.enter="handleRegister" class="input-animated" />
              </el-form-item>
              <el-button type="primary" class="submit-btn btn-shimmer" :loading="loading" @click="handleRegister">注册并登录</el-button>
            </el-form>
            <p class="switch-text">已有账号？<a href="javascript:;" @click="isRegisterMode = false">立即登录</a></p>
          </div>
        </transition>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'
import { required, email, password, confirmPassword, validateForm } from '@/utils/validators'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const registerFormRef = ref(null)
const loading = ref(false)
const isRegisterMode = ref(false)

const roleOptions = [
  { label: '学生', value: 'student' },
  { label: '教师', value: 'teacher' }
]

const form = reactive({ username: '', password: '' })

const registerForm = reactive({
  roleType: 'student',
  email: '',
  nickname: '',
  realName: '',
  studentNo: '',
  grade: '',
  major: '',
  className: '',
  department: '',
  title: '',
  password: '',
  confirmPassword: ''
})

const rules = {
  username: [required('请输入用户名')],
  password: [required('请输入密码')]
}

const registerRules = {
  ...(() => {
    const getPwd = () => registerForm.password
    return {
      email: email(),
      realName: [required('请输入真实姓名')],
      password: password(),
      confirmPassword: confirmPassword(getPwd)
    }
  })()
}

const handleLogin = async () => {
  if (!await validateForm(formRef.value)) return
  loading.value = true
  try {
    const res = await authApi.login({ username: form.username, password: form.password })
    if (res.data.code === 200) {
      userStore.setLoginData(res.data.data)
      ElMessage.success('登录成功')
      router.push(roleHome())
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!await validateForm(registerFormRef.value)) return
  loading.value = true
  try {
    const res = await authApi.register({
      roleType: registerForm.roleType,
      email: registerForm.email,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      nickname: registerForm.nickname || undefined,
      realName: registerForm.realName || undefined,
      studentNo: registerForm.studentNo || undefined,
      grade: registerForm.grade || undefined,
      major: registerForm.major || undefined,
      className: registerForm.className || undefined,
      department: registerForm.department || undefined,
      title: registerForm.title || undefined
    })
    if (res.data.code === 200) {
      userStore.setLoginData(res.data.data)
      ElMessage.success('注册成功')
      router.push(roleHome())
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '注册失败')
  } finally {
    loading.value = false
  }
}

const roleHome = () => {
  if (userStore.roleCode === 'ROLE_ADMIN') return '/admin/courses'
  if (userStore.roleCode === 'ROLE_TEACHER') return '/teacher/dashboard'
  if (userStore.roleCode === 'ROLE_STUDENT') return '/student/dashboard'
  return '/dashboard'
}
</script>

<style scoped>
/* ============================================================
   登录页样式 + 创意动画
   ============================================================ */

/* ---------- 页面整体入场 ---------- */
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: #f7f8f2;
  animation: page-in 0.4s ease-out both;
}

@keyframes page-in {
  from { opacity: 0; }
  to   { opacity: 1; }
}

/* ---------- 品牌面板 ---------- */
.brand-panel {
  background: linear-gradient(135deg, #4a5c38, #5a6e42, #7f8c52);
  background-size: 200% 200%;
  animation: bg-breathe 8s ease-in-out infinite;
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  position: relative;
  overflow: hidden;
}

/* 背景呼吸 — 渐变缓慢流动 */
@keyframes bg-breathe {
  0%, 100% { background-position: 0% 50%; }
  50%      { background-position: 100% 50%; }
}

/* ---------- 装饰浮球 ---------- */
.float-orb {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  pointer-events: none;
  animation: float-orb 6s ease-in-out infinite;
}

.orb-1 {
  width: 160px; height: 160px;
  top: 10%; left: -10%;
  animation-delay: 0s;
  animation-duration: 8s;
}

.orb-2 {
  width: 100px; height: 100px;
  bottom: 25%; right: -5%;
  animation-delay: 1.5s;
  animation-duration: 7s;
}

.orb-3 {
  width: 200px; height: 200px;
  top: 50%; left: 40%;
  animation-delay: 3s;
  animation-duration: 9s;
}

.orb-4 {
  width: 60px; height: 60px;
  top: 5%; right: 20%;
  animation-delay: 4s;
  animation-duration: 6.5s;
}

@keyframes float-orb {
  0%, 100% { transform: translateY(0) translateX(0) scale(1); }
  25%      { transform: translateY(-20px) translateX(10px) scale(1.05); }
  50%      { transform: translateY(-8px) translateX(-8px) scale(0.95); }
  75%      { transform: translateY(-25px) translateX(5px) scale(1.08); }
}

/* ---------- Logo 弹性入场 ---------- */
.brand-logo {
  width: 86px;
  height: 86px;
  border-radius: 50%;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
  animation: logo-bounce 0.7s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

@keyframes logo-bounce {
  from {
    opacity: 0;
    transform: scale(0.3) rotate(-10deg);
  }
  to {
    opacity: 1;
    transform: scale(1) rotate(0deg);
  }
}

/* ---------- 标题逐字动画 ---------- */
.brand-panel h1 {
  font-size: 42px;
  letter-spacing: 8px;
  margin: 0 0 12px;
  position: relative;
  z-index: 1;
}

.char {
  display: inline-block;
  animation: char-rise 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

.char-ke  { animation-delay: 0.15s; }
.char-chang { animation-delay: 0.3s; }

@keyframes char-rise {
  from {
    opacity: 0;
    transform: translateY(24px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 副标题淡入 */
.brand-sub {
  color: rgba(255, 255, 255, .76);
  margin: 0;
  position: relative;
  z-index: 1;
  animation: fade-up 0.5s ease-out 0.4s both;
}

@keyframes fade-up {
  from { opacity: 0; transform: translateY(10px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ---------- 表单面板 ---------- */
.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
}

/* ---------- 表单卡片滑入 ---------- */
.login-card {
  width: 440px;
  max-height: calc(100vh - 64px);
  overflow-y: auto;
  background: #fff;
  border-radius: 12px;
  padding: 36px;
  box-shadow: 0 14px 40px rgba(44, 54, 30, .12);
  animation: card-slide-up 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) 0.2s both;
}

@keyframes card-slide-up {
  from {
    opacity: 0;
    transform: translateY(50px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-card h2 {
  margin: 0 0 8px;
  font-size: 28px;
  color: #303133;
}

.subtitle {
  margin: 0 0 28px;
  color: #909399;
}

/* ---------- 输入框聚焦微动效 ---------- */
.input-animated :deep(.el-input__wrapper) {
  transition: transform 0.25s var(--bio-ease-out, cubic-bezier(0.25, 0.8, 0.25, 1)),
              box-shadow 0.25s var(--bio-ease-out, cubic-bezier(0.25, 0.8, 0.25, 1));
}

.input-animated :deep(.el-input__wrapper):focus-within {
  transform: translateY(-2px);
  box-shadow: 0 4px 18px rgba(74, 92, 56, 0.12) !important;
}

/* ---------- 提交按钮微光扫过 ---------- */
.submit-btn {
  width: 100%;
  height: 44px;
}

.btn-shimmer {
  position: relative;
  overflow: hidden;
}

.btn-shimmer::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    105deg,
    transparent 30%,
    rgba(255, 255, 255, 0.25) 50%,
    transparent 70%
  );
  transform: translateX(-100%);
  pointer-events: none;
}

.btn-shimmer:hover::after {
  animation: shimmer-slide 0.6s ease-out forwards;
}

@keyframes shimmer-slide {
  from { transform: translateX(-100%); }
  to   { transform: translateX(100%); }
}

/* ---------- 底部切换文字 ---------- */
.switch-text {
  margin: 22px 0 0;
  text-align: center;
  color: #606266;
}

.switch-text a {
  color: #409eff;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.2s;
}

.switch-text a:hover {
  color: #337ecc;
}

/* ---------- 登录/注册切换过渡 ---------- */
.form-swap-enter-active {
  animation: swap-in 0.35s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

.form-swap-leave-active {
  animation: swap-out 0.2s ease-in both;
}

@keyframes swap-in {
  from {
    opacity: 0;
    transform: scale(0.94) translateY(12px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes swap-out {
  from {
    opacity: 1;
    transform: scale(1);
  }
  to {
    opacity: 0;
    transform: scale(0.96) translateY(-8px);
  }
}

/* ---------- 响应式 ---------- */
@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    display: none;
  }

  .login-card {
    animation: card-slide-up 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) both;
  }
}
</style>
