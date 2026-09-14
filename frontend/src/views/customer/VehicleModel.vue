<template>
  <CrudPage title="车型" :api="customer.model" :columns="columns" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { customer } from '../../api'

const columns = [
  { prop: 'code', label: '编码' },
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'brand', label: '品牌' },
  { prop: 'series', label: '车系' },
  { prop: 'warrantyMonths', label: '保修(月)', type: 'number', precision: 0 },
  { prop: 'warrantyKm', label: '保修(公里)', type: 'number', precision: 0 },
  { prop: 'maintenanceIntervalKm', label: '保养间隔(km)', type: 'number', precision: 0 }
]
</script>
