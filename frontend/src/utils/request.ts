import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

let _401DialogShowing = false

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 如果是 FormData，不要设置 Content-Type，让浏览器自动处理
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 维护模式下静默处理，让 App.vue 的统一遮罩处理
    if (res.code === 9999) {
      return Promise.reject(new Error(res.msg || '系统维护中'))
    }
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    // 处理401未授权错误
    if (error.response && error.response.status === 401) {
      if (_401DialogShowing) return Promise.reject(error)
      _401DialogShowing = true
      const data = error.response.data
      const message = data?.msg || data?.message || '登录已过期'

      ElMessageBox.confirm(
        `<div class="prompt-body">
          <div class="prompt-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0110 0v4"/>
            </svg>
          </div>
          <div class="prompt-title">需要登录</div>
          <p class="prompt-desc">${message}，是否前往登录页面？</p>
        </div>`,
        '',
        {
          confirmButtonText: '去登录',
          cancelButtonText: '取消',
          customClass: 'login-prompt-box',
          distinguishCancelAndClose: true,
          icon: undefined,
          dangerouslyUseHTMLString: true
        }
      ).then(() => {
        _401DialogShowing = false
        localStorage.removeItem('token')
        router.push('/login')
      }).catch(() => {
        _401DialogShowing = false
      })

      return Promise.reject(new Error(message))
    }
    
    // 处理403权限不足错误
    if (error.response && error.response.status === 403) {
      const data = error.response.data
      const message = data?.msg || data?.message || '权限不足'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    
    // 处理500服务器错误 - 优先显示后端返回的错误信息
    if (error.response && error.response.status >= 500) {
      const data = error.response.data
      const message = data?.msg || data?.message || data?.error || '服务器错误，请稍后重试'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
