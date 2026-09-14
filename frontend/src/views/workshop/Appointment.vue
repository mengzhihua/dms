<template>
  <CrudPage title="预约" :api="workshop.appointment" :columns="columns" :extra-params="extra" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { workshop } from '../../api'
import { store } from '../../store'

const extra = () => ({ dealerCode: store.dealerCode })

const columns = [
  { prop: 'dealerCode', label: '经销商' },
  { prop: 'customerId', label: '客户ID', type: 'number', precision: 0 },
  { prop: 'vehicleId', label: '车辆ID', type: 'number', precision: 0 },
  { prop: 'appointmentTime', label: '预约时间', minWidth: 150 },
  { prop: 'serviceType', label: '服务类型', type: 'select', options: [
    { value: '保养', label: '保养' }, { value: '维修', label: '维修' },
    { value: '事故', label: '事故' }, { value: '召回', label: '召回' }] },
  { prop: 'status', label: '状态', type: 'select', options: [
    { value: 'BOOKED', label: '已预约' }, { value: 'ARRIVED', label: '已到店' },
    { value: 'CANCELLED', label: '已取消' }, { value: 'NO_SHOW', label: '爽约' }] },
  { prop: 'remark', label: '备注' }
]
</script>
