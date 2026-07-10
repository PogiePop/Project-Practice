import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

// ==================== 全局过期防抖标记 ====================
// 防止token过期时多个并发请求各自触发一次弹窗+跳转
let expiredLocked = false

/** 手动锁定（主动退出时调用，阻止后续401弹窗） */
export function lockExpired() {
  expiredLocked = true
}

/** 重置过期锁（登录成功后调用，恢复正常过期检测） */
export function resetExpiredLock() {
  expiredLocked = false
}

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
  return code === 200 || code === 0 || code === '200' || code === '0'
}

const isAuthError = (code) => {
  return code === 1001 || code === 1002 || code === 1003
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

    // ==================== 鉴权错误统一处理（防抖：只触发一次） ====================
    if (isAuthError(code) && !expiredLocked) {
      expiredLocked = true
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('perms')
      localStorage.removeItem('roles')
      // 仅当不在登录页时才跳转
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(new Error('TOKEN_EXPIRED'))
    }

    if (code === undefined || isSuccess(code)) {
      return data !== undefined ? data : body
    }

    // 业务错误
    ElMessage.error(message || `请求失败 (code: ${code})`)
    return Promise.reject(new Error(message || `code: ${code}`))
  },
  error => {
    if (error.response) {
      const { status } = error.response

      // ==================== HTTP 401 过期处理（防抖：只触发一次） ====================
      if (status === 401) {
        if (!expiredLocked) {
          expiredLocked = true
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          localStorage.removeItem('perms')
          localStorage.removeItem('roles')
          if (router.currentRoute.value.path !== '/login') {
            router.push('/login')
          }
          ElMessage.error('登录已过期，请重新登录')
        }
        return Promise.reject(new Error('TOKEN_EXPIRED'))
      }

      switch (status) {
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
