import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router/router'

const request = axios.create({
  baseURL: '/api/audit/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器 —— 自动附加 Token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 判断是否业务成功 —— 兼容常见后端 code 约定
const isSuccess = (code) => {
  // code === 200 或 code === 0 或 code === '200' 都算成功
  return code === 200 || code === 0 || code === '200' || code === '0'
}

// 响应拦截器
request.interceptors.response.use(
  response => {
    const body = response.data

    // 如果后端直接返回数据（无 code 包裹），直接透传
    if (body && typeof body === 'object' && !('code' in body)) {
      return body
    }

    const { code, message, data } = body

    // 打印完整响应便于排查
    console.log('[API Response]', response.config.url, 'code:', code, 'data:', data)

    if (code === undefined || isSuccess(code)) {
      // data 为 undefined 时返回整个 body（有些接口不包 data 层）
      return data !== undefined ? data : body
    }

    // 业务错误
    ElMessage.error(message || `请求失败 (code: ${code})`)
    return Promise.reject(new Error(message || `code: ${code}`))
  },
  error => {
    if (error.response) {
      const { status } = error.response
      switch (status) {
        case 401:
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          router.push('/login')
          ElMessage.error('登录已过期，请重新登录')
          break
        case 403:
          ElMessage.error('无权限执行此操作')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(error.response.data?.message || `请求失败 (HTTP ${status})`)
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查后端服务是否启动')
    } else {
      ElMessage.error('网络连接失败，请确认后端服务已启动')
    }
    return Promise.reject(error)
  }
)

export default request
