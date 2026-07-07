import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const stored = localStorage.getItem('userInfo')
  const userInfo = ref(stored && stored !== 'undefined' && stored !== 'null' ? JSON.parse(stored) : {})

  const isLoggedIn = computed(() => !!token.value)

  // 登录
  async function login(username, password) {
    try {
      const res = await authApi.login(username, password)
      token.value = res.token
      // 后端返回结构: { token, username, nickname, role, expiresIn }
      userInfo.value = {
        username: res.username,
        nickname: res.nickname,
        role: res.role,
        id: res.id
      }
      localStorage.setItem('token', res.token)
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
      return { success: true }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 注册
  async function register(data) {
    try {
      const res = await authApi.register(data)
      return { success: true, data: res }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  // 登出
  function logout() {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  // 获取用户信息
  async function fetchUserInfo() {
    if (!token.value) return null
    try {
      const res = await userApi.me()
      userInfo.value = res
      localStorage.setItem('userInfo', JSON.stringify(res))
      return res
    } catch (error) {
      logout()
      throw error
    }
  }

  // 更新用户信息
  async function updateProfile(data) {
    if (!userInfo.value.id) throw new Error('用户未登录')
    const res = await userApi.update(userInfo.value.id, {
      nickname: data.nickname,
      email: data.email
    })
    userInfo.value = { ...userInfo.value, ...res }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    return res
  }

  // 修改密码
  async function changePassword(currentPassword, newPassword) {
    await userApi.changePassword({
      currentPassword,
      newPassword
    })
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    register,
    logout,
    fetchUserInfo,
    updateProfile,
    changePassword
  }
})