import { reactive } from 'vue'

// 当前选中经销商（头部选择器），持久化到 localStorage，作为各页默认过滤条件
export const store = reactive({
  dealerCode: localStorage.getItem('dealerCode') || 'D001',
  token: localStorage.getItem('token') || '',
  user: JSON.parse(localStorage.getItem('user') || 'null')
})

export function setDealer(code) {
  store.dealerCode = code
  if (code) {
    localStorage.setItem('dealerCode', code)
  } else {
    localStorage.removeItem('dealerCode')
  }
}

export function setAuth(token, user) {
  store.token = token
  store.user = user
  localStorage.setItem('token', token)
  localStorage.setItem('user', JSON.stringify(user))
  if (user && user.dealerCode) {
    setDealer(user.dealerCode)
  }
}

export function logout() {
  store.token = ''
  store.user = null
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}
