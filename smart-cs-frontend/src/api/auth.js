import request from './request'

export const authApi = {
  // 用户注册
  register(data) {
    return request.post('/auth/register', data)
  },

  // 管理员注册（需邀请码）
  registerAdmin(data) {
    return request.post('/auth/register/admin', data)
  },

  // 用户登录
  login(username, password) {
    return request.post('/auth/login', { username, password })
  },

  // 获取用户信息
  getProfile() {
    return request.get('/users/profile')
  }
}
