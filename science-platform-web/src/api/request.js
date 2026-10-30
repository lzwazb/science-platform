import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 全局 Axios 实例（开发档案 6.4：统一设置 baseURL、超时、令牌携带、业务错误提示）。
 *
 * baseURL 使用相对路径 /api/v1，开发期由 Vite 代理转发到 http://localhost:8080，
 * 生产部署时前后端同源也可直接工作。
 */
const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截：携带认证令牌（阶段 3 登录功能接入后生效，未登录时无令牌则跳过）
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截：统一解包后端 { code, message, data, timestamp } 结构
request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 非标准结构（例如文件流）原样返回
    if (body === null || typeof body !== 'object' || !('code' in body)) {
      return body
    }
    if (body.code === 200) {
      return body.data
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    const serverMessage = error.response?.data?.message
    let message = serverMessage || '网络异常，请稍后重试'

    if (status === 401) {
      message = '登录状态已失效，请重新登录'
      localStorage.removeItem('token')
    } else if (status === 403) {
      message = '没有权限执行该操作'
    } else if (status === 404) {
      message = '请求的资源不存在'
    } else if (status >= 500) {
      message = '服务端异常，请稍后重试'
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请检查后端服务是否已启动'
    }

    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default request
