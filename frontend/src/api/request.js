import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

function reported(error) {
  error.reported = true
  return Promise.reject(error)
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
      ElMessage.error(body.msg || '请求失败')
      return reported(new Error(body.msg))
    }
    return body ? body.data : body
  },
  (error) => {
    ElMessage.error(error.response?.data?.msg || error.message || '网络错误')
    return reported(error)
  }
)

export default http
