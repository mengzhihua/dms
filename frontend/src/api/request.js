import axios from 'axios'
import { ElMessage } from 'element-plus'
import { store, logout } from '../store'
import router from '../router'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  if (store.token) {
    config.headers.Authorization = `Bearer ${store.token}`
  }
  return config
})

function reported(error) {
  error.reported = true
  return Promise.reject(error)
}

let relogging = false

function toLogin() {
  if (relogging) return
  relogging = true
  logout()
  const redirect = router.currentRoute.value.fullPath
  router.push(`/login?redirect=${encodeURIComponent(redirect)}`).finally(() => {
    relogging = false
  })
}

window.addEventListener('unhandledrejection', (event) => {
  if (event.reason && event.reason.reported) {
    event.preventDefault()
  }
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code !== undefined && body.code !== 0) {
      const err = new Error(body.msg)
      if (body.code === 401) {
        // 已在登录页时（登录失败）不跳转，交由页面自行提示
        if (router.currentRoute.value.path !== '/login') {
          toLogin()
        }
      } else {
        ElMessage.error(body.msg || '请求失败')
        err.toasted = true
      }
      return reported(err)
    }
    return body ? body.data : body
  },
  (error) => {
    if (error.response?.status === 401 || error.response?.data?.code === 401) {
      if (router.currentRoute.value.path !== '/login') {
        toLogin()
      }
    } else {
      ElMessage.error(error.response?.data?.msg || error.message || '网络错误')
      error.toasted = true
    }
    return reported(error)
  }
)

export default http
