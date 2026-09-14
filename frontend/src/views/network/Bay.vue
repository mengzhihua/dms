<template>
  <CrudPage title="工位" :api="network.bay" :columns="columns" :extra-params="extra" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { network } from '../../api'
import { store } from '../../store'

const extra = () => ({ dealerCode: store.dealerCode })

const columns = [
  { prop: 'dealerCode', label: '经销商' },
  { prop: 'code', label: '工位号' },
  { prop: 'type', label: '类型', type: 'select', options: [
    { value: '机修', label: '机修' }, { value: '钣金', label: '钣金' },
    { value: '喷漆', label: '喷漆' }, { value: '快保', label: '快保' }] },
  { prop: 'status', label: '状态', type: 'select', options: [
    { value: 'IDLE', label: '空闲' }, { value: 'BUSY', label: '占用' }, { value: 'OFF', label: '停用' }] }
]
</script>
