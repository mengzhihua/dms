import { reactive } from 'vue'

// 当前选中经销商（头部选择器），持久化到 localStorage，作为各页默认过滤条件
export const store = reactive({
  dealerCode: localStorage.getItem('dealerCode') || 'D001'
})

export function setDealer(code) {
  store.dealerCode = code
  if (code) {
    localStorage.setItem('dealerCode', code)
  } else {
    localStorage.removeItem('dealerCode')
  }
}
