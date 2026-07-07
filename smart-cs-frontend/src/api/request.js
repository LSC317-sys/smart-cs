import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 重试配置
const MAX_RETRY = 3
const RETRY_DELAY = 1000
const RETRY_STATUS_CODES = [502, 503, 504, 408] // 需要重试的状态码

// 创建 axios 实例
const request = axios.create({
  baseURL: window.location.origin + '/api',
  timeout: 30000
})

// 延迟函数
const delay = ms => new Promise(resolve => setTimeout(resolve, ms))

// 带重试的请求
async function requestWithRetry(config, retryCount = 0) {
  try {
    const response = await axios(config)
    return response.data
  } catch (error) {
    const shouldRetry = (
      retryCount < MAX_RETRY &&
      (error.code === 'ECONNABORTED' || // 超时
       error.code === 'ERR_NETWORK' ||  // 网络错误
       (error.response && RETRY_STATUS_CODES.includes(error.response.status)))
    )
    
    if (shouldRetry) {
      console.log(`请求失败，${RETRY_DELAY}ms 后进行第 ${retryCount + 1} 次重试...`, config.url)
      await delay(RETRY_DELAY * (retryCount + 1)) // 指数退避
      return requestWithRetry(config, retryCount + 1)
    }
    
    throw error
  }
}

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    return response.data
  },
  async error => {
    const config = error.config
    
    // 检查是否需要重试
    const retryCount = config?.__retryCount || 0
    const shouldRetry = (
      retryCount < MAX_RETRY &&
      (error.code === 'ECONNABORTED' ||
       error.code === 'ERR_NETWORK' ||
       (error.response && RETRY_STATUS_CODES.includes(error.response.status)))
    )
    
    if (shouldRetry && config) {
      config.__retryCount = retryCount + 1
      console.log(`请求失败，${RETRY_DELAY * config.__retryCount}ms 后进行第 ${config.__retryCount} 次重试...`, config.url)
      await delay(RETRY_DELAY * config.__retryCount)
      return request(config)
    }
    
    // 不重试，显示错误信息
    if (error.response) {
      switch (error.response.status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request
