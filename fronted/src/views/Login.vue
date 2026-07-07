<template>
  <div class="login-page">
    <section class="brand-panel">
      <img src="@/img/logo.jpg" alt="课畅" class="brand-logo" />
      <h1>课畅</h1>
      <p>智能教学平台 · 让课程管理更顺手</p>
    </section>

    <section class="form-panel">
      <div class="login-card">
        <template v-if="!isRegisterMode">
          <h2>欢迎回来</h2>
          <p class="subtitle">登录你的课畅账号</p>
          <el-form ref="formRef" :model="form" :rules="rules" size="large">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="用户名 / 邮箱" :prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">登录</el-button>
          </el-form>
          <p class="switch-text">还没有账号？<a href="javascript:;" @click="isRegisterMode = true">立即注册</a></p>
        </template>

        <template v-else>
          <h2>创建账号</h2>
          <p class="subtitle">完善基础资料，后续课程名单会更清楚</p>
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" size="large">
            <el-form-item prop="roleType">
              <el-segmented v-model="registerForm.roleType" :options="roleOptions" style="width:100%" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="邮箱" :prefix-icon="Message" clearable />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="昵称（可选）" :prefix-icon="User" clearable />
            </el-form-item>
            <el-form-item prop="realName">
              <el-input v-model="registerForm.realName" placeholder="真实姓名" :prefix-icon="User" clearable />
            </el-form-item>

            <template v-if="registerForm.roleType === 'student'">
              <el-form-item prop="studentNo">
                <el-input v-model="registerForm.studentNo" placeholder="学号（可选，不填自动生成）" clearable />
              </el-form-item>
              <el-form-item prop="grade">
                <el-input v-model="registerForm.grade" placeholder="年级，如 2024级" clearable />
              </el-form-item>
              <el-form-item prop="major">
                <el-input v-model="registerForm.major" placeholder="专业" clearable />
              </el-form-item>
              <el-form-item prop="className">
                <el-input v-model="registerForm.className" placeholder="班级" clearable />
              </el-form-item>
            </template>

            <template v-else>
              <el-form-item prop="department">
                <el-input v-model="registerForm.department" placeholder="院系/部门" clearable />
              </el-form-item>
              <el-form-item prop="title">
                <el-input v-model="registerForm.title" placeholder="职称" clearable />
              </el-form-item>
            </template>

            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码（至少6位）" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" show-password @keyup.enter="handleRegister" />
            </el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">注册并登录</el-button>
          </el-form>
          <p class="switch-text">已有账号？<a href="javascript:;" @click="isRegisterMode = false">立即登录</a></p>
        </template>
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
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) callback(new Error('请再次输入密码'))
  else if (value !== registerForm.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const registerRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await authApi.login({ username: form.username, password: form.password })
    if (res.data.code === 200) {
      userStore.setLoginData(res.data.data)
      ElMessage.success('登录成功')
      router.push('/dashboard')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '登录失败')
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
      router.push('/dashboard')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: #f7f8f2;
}
.brand-panel {
  background: linear-gradient(135deg, #4a5c38, #7f8c52);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
}
.brand-logo {
  width: 86px;
  height: 86px;
  border-radius: 50%;
  margin-bottom: 24px;
}
.brand-panel h1 {
  font-size: 42px;
  letter-spacing: 8px;
  margin: 0 0 12px;
}
.brand-panel p {
  color: rgba(255,255,255,.76);
  margin: 0;
}
.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
}
.login-card {
  width: 440px;
  max-height: calc(100vh - 64px);
  overflow-y: auto;
  background: #fff;
  border-radius: 12px;
  padding: 36px;
  box-shadow: 0 14px 40px rgba(44, 54, 30, .12);
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
.submit-btn {
  width: 100%;
  height: 44px;
}
.switch-text {
  margin: 22px 0 0;
  text-align: center;
  color: #606266;
}
.switch-text a {
  color: #409eff;
  text-decoration: none;
  font-weight: 600;
}
@media (max-width: 900px) {
  .login-page { grid-template-columns: 1fr; }
  .brand-panel { display: none; }
}
</style>
