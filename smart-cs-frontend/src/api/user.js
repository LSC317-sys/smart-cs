import request from './request'

export const userApi = {
  // 获取当前用户信息
  me() {
    return request.get('/users/me')
  },
  
  // 更新用户信息
  update(id, data) {
    return request.put('/users/' + id, data)
  },
  
  // 修改密码
  changePassword(data) {
    return request.post('/users/change-password', data)
  }
}