<template>
  <CrudPage title="出入库流水" :api="parts.movement" :columns="columns" :extra-params="extra" readonly />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { parts } from '../../api'
import { store } from '../../store'

const extra = () => ({ dealerCode: store.dealerCode })

const columns = [
  { prop: 'dealerCode', label: '经销商' },
  { prop: 'partNo', label: '备件号' },
  { prop: 'type', label: '类型' },
  { prop: 'qty', label: '数量' },
  { prop: 'refType', label: '来源' },
  { prop: 'refNo', label: '关联单号', minWidth: 140 },
  { prop: 'location', label: '库位' },
  { prop: 'batchNo', label: '批次' },
  { prop: 'createdAt', label: '时间', minWidth: 150, hideInTable: false }
]
</script>
