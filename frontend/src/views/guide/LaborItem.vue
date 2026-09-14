<template>
  <CrudPage title="工时项目" :api="guide.labor" :columns="columns" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { guide } from '../../api'

const columns = [
  { prop: 'code', label: '编码' },
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'standardHours', label: '标准工时(h)', type: 'number', precision: 2 },
  { prop: 'category', label: '分类' }
]
</script>
