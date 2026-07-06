import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(Number(localStorage.getItem('userId')) || null)
  const username = ref(localStorage.getItem('username') || '')
  const nickname = ref(localStorage.getItem('nickname') || '')
  const roleCode = ref(localStorage.getItem('roleCode') || '')
  const roleName = ref(localStorage.getItem('roleName') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isTeacher = computed(() => roleCode.value === 'ROLE_TEACHER')
  const isStudent = computed(() => roleCode.value === 'ROLE_STUDENT')

  function setLoginData(data) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    nickname.value = data.nickname
    roleCode.value = data.roleCode
    roleName.value = data.roleName
    avatar.value = data.avatar || ''

    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    localStorage.setItem('nickname', data.nickname)
    localStorage.setItem('roleCode', data.roleCode)
    localStorage.setItem('roleName', data.roleName)
    localStorage.setItem('avatar', data.avatar || '')
  }

  function clearLoginData() {
    token.value = ''
    userId.value = null
    username.value = ''
    nickname.value = ''
    roleCode.value = ''
    roleName.value = ''
    avatar.value = ''

    localStorage.clear()
  }

  async function fetchUserInfo() {
    try {
      const res = await authApi.me()
      if (res.data.code === 200) {
        const user = res.data.data
        nickname.value = user.nickname
        avatar.value = user.avatar
        roleCode.value = user.roleCode
        roleName.value = user.roleName
        localStorage.setItem('nickname', user.nickname)
        localStorage.setItem('avatar', user.avatar || '')
        localStorage.setItem('roleCode', user.roleCode)
        localStorage.setItem('roleName', user.roleName)
      }
    } catch (e) {
      console.error('获取用户信息失败', e)
    }
  }

  return {
    token, userId, username, nickname, roleCode, roleName, avatar,
    isLoggedIn, isTeacher, isStudent,
    setLoginData, clearLoginData, fetchUserInfo
  }
})
