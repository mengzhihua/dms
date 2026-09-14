import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'

export const menus = [
  {
    path: '/dashboard',
    name: '工作台',
    icon: 'Odometer',
    component: () => import('../views/Dashboard.vue')
  },
  {
    path: '/network',
    name: '网络管理',
    icon: 'OfficeBuilding',
    children: [
      { path: 'dealer', name: '经销商与直营店', component: () => import('../views/network/Dealer.vue') },
      { path: 'target', name: '目标与达成', component: () => import('../views/network/Target.vue') },
      { path: 'assessment', name: '经销商考核', component: () => import('../views/network/Assessment.vue') },
      { path: 'technician', name: '技师', component: () => import('../views/network/Technician.vue') },
      { path: 'bay', name: '工位', component: () => import('../views/network/Bay.vue') },
      { path: 'vehicle-stock', name: '整车库存', component: () => import('../views/network/VehicleStock.vue') },
      { path: 'sales-order', name: '整车销售订单', component: () => import('../views/network/SalesOrder.vue') }
    ]
  },
  {
    path: '/customer',
    name: '客户车辆',
    icon: 'User',
    children: [
      { path: 'customer', name: '客户', component: () => import('../views/customer/Customer.vue') },
      { path: 'model', name: '车型', component: () => import('../views/customer/VehicleModel.vue') },
      { path: 'vehicle', name: '车辆', component: () => import('../views/customer/Vehicle.vue') }
    ]
  },
  {
    path: '/parts',
    name: '备件管理',
    icon: 'Box',
    children: [
      { path: 'part', name: '备件主数据', component: () => import('../views/parts/Part.vue') },
      { path: 'stock', name: '库存', component: () => import('../views/parts/Stock.vue') },
      { path: 'movement', name: '出入库流水', component: () => import('../views/parts/Movement.vue') },
      { path: 'shortage', name: '缺货预警', component: () => import('../views/parts/Shortage.vue') },
      { path: 'replenish', name: 'OMS补货', component: () => import('../views/parts/Replenish.vue') }
    ]
  },
  {
    path: '/guide',
    name: '维修指导',
    icon: 'Reading',
    children: [
      { path: 'labor', name: '工时项目', component: () => import('../views/guide/LaborItem.vue') },
      { path: 'guide', name: '维修指导库', component: () => import('../views/guide/Guide.vue') },
      { path: 'bulletin', name: '技术通报', component: () => import('../views/guide/Bulletin.vue') },
      { path: 'recommend', name: '智能推荐', component: () => import('../views/guide/Recommend.vue') }
    ]
  },
  {
    path: '/workshop',
    name: '维修工单',
    icon: 'Tools',
    children: [
      { path: 'appointment', name: '预约', component: () => import('../views/workshop/Appointment.vue') },
      { path: 'order', name: '工单列表', component: () => import('../views/workshop/OrderList.vue') },
      { path: 'claim', name: '保修索赔', component: () => import('../views/workshop/Claim.vue') }
    ]
  },
  {
    path: '/survey',
    name: '满意度',
    icon: 'ChatDotSquare',
    children: [
      { path: 'survey', name: '调研记录', component: () => import('../views/survey/SurveyList.vue') },
      { path: 'answer', name: '答卷', component: () => import('../views/survey/SurveyAnswer.vue') },
      { path: 'stats', name: '统计', component: () => import('../views/survey/SurveyStats.vue') },
      { path: 'complaint', name: '投诉处理', component: () => import('../views/survey/Complaint.vue') }
    ]
  },
  {
    path: '/invoice',
    name: '发票管理',
    icon: 'Tickets',
    children: [
      { path: 'invoice', name: '发票列表', component: () => import('../views/invoice/InvoiceList.vue') },
      { path: 'list', name: '发票列表（快捷入口）', component: () => import('../views/invoice/InvoiceList.vue') }
    ]
  }
]

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: menus.flatMap((menu) =>
      menu.children
        ? menu.children.map((child) => ({
            path: `${menu.path}/${child.path}`,
            name: child.name,
            component: child.component
          }))
        : [{ path: menu.path, name: menu.name, component: menu.component }]
    )
  },
  {
    path: '/workshop/order/:id',
    component: Layout,
    children: [
      { path: '', name: '工单详情', component: () => import('../views/workshop/OrderDetail.vue') }
    ]
  }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
