import http from './request'

export const crud = (base) => ({
  page: (params) => http.get(`${base}/page`, { params }),
  list: (params) => http.get(`${base}/list`, { params }),
  get: (id) => http.get(`${base}/${id}`),
  create: (data) => http.post(base, data),
  update: (id, data) => http.put(`${base}/${id}`, data),
  remove: (id) => http.delete(`${base}/${id}`)
})

export const network = {
  dealer: crud('/network/dealer'),
  target: crud('/network/target'),
  assessment: crud('/network/assessment'),
  technician: crud('/network/technician'),
  bay: crud('/network/bay'),
  vehicleStock: crud('/network/vehicle-stock'),
  salesOrder: crud('/network/sales-order'),
  achievement: (params) => http.get('/network/target/achievement', { params }),
  generateAssessment: (params) => http.post('/network/assessment/generate', null, { params }),
  allocate: (id) => http.post(`/network/sales-order/${id}/allocate`),
  invoiceOrder: (id, data) => http.post(`/network/sales-order/${id}/invoice`, data),
  deliverOrder: (id, data) => http.post(`/network/sales-order/${id}/deliver`, data),
  cancelOrder: (id) => http.post(`/network/sales-order/${id}/cancel`),
  financeApply: (id, data) => http.post(`/network/sales-order/${id}/finance`, data),
  financeDecision: (id, data) => http.post(`/network/sales-order/${id}/finance/decision`, data),
  insure: (id, data) => http.post(`/network/sales-order/${id}/insurance`, data),
  payOrder: (id, data) => http.post(`/network/sales-order/${id}/payment`, data),
  salesDetail: (id) => http.get(`/network/sales-order/${id}/detail`)
}

export const customer = {
  customer: crud('/customer/customer'),
  model: crud('/customer/model'),
  vehicle: crud('/customer/vehicle'),
  history: (vin) => http.get(`/customer/vehicle/vin/${vin}/history`),
  warrantyCheck: (id, mileage) =>
    http.get(`/customer/vehicle/${id}/warranty-check`, { params: { mileage } })
}

export const parts = {
  part: crud('/parts/part'),
  stock: crud('/parts/stock'),
  movement: crud('/parts/movement'),
  inbound: (data) => http.post('/parts/stock/inbound', data),
  shortage: () => http.get('/parts/stock/shortage'),
  available: (params) => http.get('/parts/stock/available', { params })
}

export const oms = {
  replenish: crud('/oms/replenish'),
  draft: (data) => http.post('/oms/replenish/draft', data),
  fromShortage: (dealerCode) =>
    http.post('/oms/replenish/from-shortage', null, { params: { dealerCode } }),
  push: (id) => http.post(`/oms/replenish/${id}/push`),
  sync: (id) => http.post(`/oms/replenish/${id}/sync`),
  syncAll: () => http.post('/oms/replenish/sync-all'),
  cancel: (id, reason) => http.post(`/oms/replenish/${id}/cancel`, { reason }),
  lines: (id) => http.get(`/oms/replenish/${id}/lines`),
  omsInventory: (partNos) => http.get('/oms/replenish/oms-inventory', { params: { partNos } })
}

export const guide = {
  labor: crud('/guide/labor'),
  guide: crud('/guide/guide'),
  bulletin: crud('/guide/bulletin'),
  recommend: (data) => http.post('/guide/recommend', data),
  estimate: (params) => http.get('/guide/estimate', { params })
}

export const workshop = {
  appointment: crud('/workshop/appointment'),
  claim: crud('/workshop/claim'),
  claimApprove: (id) => http.post(`/workshop/claim/${id}/approve`),
  claimReject: (id) => http.post(`/workshop/claim/${id}/reject`),
  claimPay: (id) => http.post(`/workshop/claim/${id}/pay`),
  orderPage: (params) => http.get('/workshop/order/page', { params }),
  orderDetail: (id) => http.get(`/workshop/order/${id}`),
  orderCreate: (data) => http.post('/workshop/order', data),
  addLabor: (id, data) => http.post(`/workshop/order/${id}/labor`, data),
  removeLabor: (id, lineId) => http.delete(`/workshop/order/${id}/labor/${lineId}`),
  addPart: (id, data) => http.post(`/workshop/order/${id}/part`, data),
  removePart: (id, lineId) => http.delete(`/workshop/order/${id}/part/${lineId}`),
  applyGuide: (id, code) => http.post(`/workshop/order/${id}/apply-guide/${code}`),
  action: (id, action, data) => http.post(`/workshop/order/${id}/${action}`, data || {})
}

export const survey = {
  template: crud('/survey/template'),
  question: crud('/survey/question'),
  survey: crud('/survey/record'),
  surveyAnswers: (id) => http.get(`/survey/record/${id}/answers`),
  answer: crud('/survey/answer'),
  complaint: crud('/survey/complaint'),
  submitAnswer: (id, answers) => http.post(`/survey/${id}/answer`, { answers }),
  stats: (params) => http.get('/survey/stats', { params }),
  handleComplaint: (id, data) => http.post(`/survey/complaint/${id}/handle`, data)
}

export const invoice = {
  invoice: crud('/invoice'),
  line: crud('/invoice/line'),
  taxConfig: crud('/invoice/tax-config'),
  issue: (id) => http.post(`/invoice/${id}/issue`),
  redFlush: (id) => http.post(`/invoice/${id}/red-flush`),
  preview: (id) => http.get(`/invoice/${id}/preview`)
}

export const auth = {
  login: (data) => http.post('/auth/login', data),
  me: () => http.get('/auth/me'),
  logout: () => http.post('/auth/logout'),
  changePassword: (old, newPwd) => http.put('/auth/password', { old, new: newPwd }),
  user: crud('/auth/user')
}

export const dashboard = (dealerCode) => http.get('/dashboard', { params: { dealerCode } })
