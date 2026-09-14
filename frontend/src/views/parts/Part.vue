<template>
  <CrudPage title="备件主数据" :api="parts.part" :columns="columns" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { parts } from '../../api'

const columns = [
  { prop: 'partNo', label: '备件号' },
  { prop: 'name', label: '名称', minWidth: 140 },
  { prop: 'category', label: '分类' },
  { prop: 'unit', label: '单位' },
  { prop: 'costPrice', label: '成本价', type: 'number', precision: 2 },
  { prop: 'salePrice', label: '销售价', type: 'number', precision: 2 },
  { prop: 'taxRate', label: '税率', type: 'number', precision: 4 },
  { prop: 'minStock', label: '最低库存', type: 'number', precision: 0 }
]
</script>
