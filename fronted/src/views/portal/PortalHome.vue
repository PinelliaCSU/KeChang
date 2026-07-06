<template>
  <div class="portal-home">
    <AppHeader @go-teacher="goTeacher" @go-student="goStudent"
               @show-login="showDialog('login')" @show-register="showDialog('register')" />

    <!-- Banner 轮播 -->
    <el-carousel :interval="3000" height="380px" arrow="always">
      <el-carousel-item v-for="b in banners" :key="b.title">
        <div class="banner-slide" :style="{ background: b.bg }">
          <div class="banner-content">
            <h2>{{ b.title }}</h2>
            <p>{{ b.subtitle }}</p>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>

    <!-- 快捷入口 -->
    <div class="entry-section">
      <div class="entry-cards">
        <div class="entry-card teacher-card" @click="goTeacher">
          <el-icon :size="48"><UserFilled /></el-icon>
          <h3>📚 教师端入口</h3>
          <p>课程管理 · 作业批改 · 成绩分析 · 智能组卷</p>
        </div>
        <div class="entry-card student-card" @click="goStudent">
          <el-icon :size="48"><User /></el-icon>
          <h3>🎓 学生端入口</h3>
          <p>在线学习 · 考试 · 实训 · 成绩查看</p>
        </div>
      </div>
    </div>

    <!-- 登录/注册弹窗 -->
    <el-dialog v-model="dialogVisible" :title="activeTab === 'login' ? '登录' : '注册'"
               width="420px" :close-on-click-modal="false" center>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名/邮箱" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码"
                        prefix-icon="Lock" size="large" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="handleLogin">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
          <p class="form-tip">没有账号？<el-link type="primary" @click="activeTab='register'">立即注册</el-link></p>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="0">
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="请输入邮箱" prefix-icon="Message" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="请输入密码(至少6位)"
                        prefix-icon="Lock" size="large" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请确认密码"
                        prefix-icon="Lock" size="large" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="success" size="large" style="width:100%" :loading="loading" @click="handleRegister">
                注 册
              </el-button>
            </el-form-item>
          </el-form>
          <p class="form-tip">已有账号？<el-link type="primary" @click="activeTab='login'">去登录</el-link></p>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <AppFooter />
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'

const router = useRouter()
const userStore = useUserStore()

const banners = [
  { title: '课畅 — 智能化在线教学支持服务平台', subtitle: '覆盖教学全流程，AI 赋能教育', bg: 'linear-gradient(135deg, #1890ff, #36cfc9)' },
  { title: '智能组卷 · 自动批阅', subtitle: '减轻教师负担，提升教学效率', bg: 'linear-gradient(135deg, #722ed1, #1890ff)' },
  { title: '随时随地 · 在线学习', subtitle: '视频课程 + 在线考试 + 实训管理一体化', bg: 'linear-gradient(135deg, #13c2c2, #52c41a)' }
]

const dialogVisible = ref(false)
const activeTab = ref('login')
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ email: '', password: '', confirmPassword: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const registerRules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }]
}

const loginFormRef = ref(null)
const registerFormRef = ref(null)

function showDialog(tab) {
  activeTab.value = tab
  dialogVisible.value = true
}

function handleTabChange() {
  loginForm.username = ''; loginForm.password = ''
  registerForm.email = ''; registerForm.password = ''; registerForm.confirmPassword = ''
}

async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await authApi.login(loginForm)
    if (res.data.code === 200) {
      userStore.setLoginData(res.data.data)
      ElMessage.success('登录成功')
      dialogVisible.value = false
      router.push(userStore.isTeacher ? '/teacher/dashboard' : '/student/dashboard')
    } else {
      ElMessage.error(res.data.message || '登录失败')
    }
  } catch (e) { /* handled by interceptor */ }
  finally { loading.value = false }
}

async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.error('两次密码输入不一致')
    return
  }
  loading.value = true
  try {
    const res = await authApi.register(registerForm)
    if (res.data.code === 200) {
      userStore.setLoginData(res.data.data)
      ElMessage.success('注册成功')
      dialogVisible.value = false
      router.push('/student/dashboard')
    } else {
      ElMessage.error(res.data.message || '注册失败')
    }
  } catch (e) { /* handled by interceptor */ }
  finally { loading.value = false }
}

function goTeacher() {
  if (!userStore.token) { showDialog('login'); return }
  router.push('/teacher/dashboard')
}
function goStudent() {
  if (!userStore.token) { showDialog('login'); return }
  router.push('/student/dashboard')
}
</script>

<style scoped>
.portal-home { min-height: 100vh; display: flex; flex-direction: column; background: #f0f5ff; }
.banner-slide { height: 100%; display: flex; align-items: center; justify-content: center; }
.banner-content { text-align: center; color: #fff; }
.banner-content h2 { font-size: 36px; margin-bottom: 12px; text-shadow: 0 2px 8px rgba(0,0,0,0.2); }
.banner-content p { font-size: 18px; opacity: 0.9; }

.entry-section { padding: 48px 24px; flex: 1; }
.entry-cards { max-width: 900px; margin: 0 auto; display: grid; grid-template-columns: 1fr 1fr; gap: 32px; }
.entry-card {
  background: #fff; border-radius: 12px; padding: 40px 24px; text-align: center;
  cursor: pointer; transition: all 0.3s; box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.entry-card:hover { transform: translateY(-6px); box-shadow: 0 8px 24px rgba(24,144,255,0.15); }
.entry-card h3 { font-size: 20px; margin: 16px 0 8px; }
.entry-card p { color: #909399; font-size: 14px; }
.teacher-card:hover { border: 2px solid #1890ff; }
.student-card:hover { border: 2px solid #52c41a; }

.form-tip { text-align: center; color: #909399; font-size: 13px; margin-top: 8px; }
</style>
