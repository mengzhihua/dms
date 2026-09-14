<template>
  <CrudPage title="技师" :api="network.technician" :columns="columns" :extra-params="extra" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { network } from '../../api'
import { store } from '../../store'

const extra = () => ({ dealerCode: store.dealerCode })

const columns = [
  { prop: 'dealerCode', label: '经销商' },
  { prop: 'code', label: '工号' },
  { prop: 'name', label: '姓名' },
  { prop: 'level', label: '等级', type: 'select', options: [
    { value: '初级', label: '初级' }, { value: '中级', label: '中级' },
    { value: '高级', label: '高级' }, { value: '技师长', label: '技师长' }] },
  { prop: 'skills', label: '技能(逗号分隔)' },
  { prop: 'status', label: '状态', type: 'select', options: [
    { value: 'IDLE', label: '空闲' }, { value: 'BUSY', label: '忙碌' }, { value: 'OFF', label: '停用' }] }
]
</script>
