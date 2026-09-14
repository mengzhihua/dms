<template>
  <CrudPage title="客户" :api="customer.customer" :columns="columns" :extra-params="extra" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { customer } from '../../api'
import { store } from '../../store'

const extra = () => ({ dealerCode: store.dealerCode })

const columns = [
  { prop: 'name', label: '姓名' },
  { prop: 'phone', label: '电话' },
  { prop: 'idNo', label: '证件号', minWidth: 150 },
  { prop: 'gender', label: '性别', type: 'select', options: [
    { value: '男', label: '男' }, { value: '女', label: '女' }] },
  { prop: 'level', label: '等级', type: 'select', options: [
    { value: '普通', label: '普通' }, { value: 'VIP', label: 'VIP' }] },
  { prop: 'dealerCode', label: '所属经销商' }
]
</script>
