<template>
  <CrudPage title="整车库存" :api="network.vehicleStock" :columns="columns" :extra-params="extra" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { network } from '../../api'
import { store } from '../../store'

const extra = () => ({ dealerCode: store.dealerCode })

const columns = [
  { prop: 'dealerCode', label: '经销商' },
  { prop: 'vin', label: '车架号VIN', minWidth: 160 },
  { prop: 'modelCode', label: '车型' },
  { prop: 'color', label: '颜色' },
  { prop: 'status', label: '状态', type: 'select', options: [
    { value: 'IN_STOCK', label: '在库' }, { value: 'ALLOCATED', label: '已分配' }, { value: 'SOLD', label: '已售' }] }
]
</script>
