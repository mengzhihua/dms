<template>
  <CrudPage title="经销商/直营店" :api="network.dealer" :columns="columns" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { network } from '../../api'

const columns = [
  { prop: 'code', label: '编码' },
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'type', label: '类型', type: 'select', options: [
    { value: 'DEALER', label: '经销商' }, { value: 'DIRECT', label: '直营店' }] },
  { prop: 'level', label: '等级', type: 'select', options: [
    { value: 'A', label: 'A' }, { value: 'B', label: 'B' }, { value: 'C', label: 'C' }] },
  { prop: 'region', label: '大区' },
  { prop: 'province', label: '省份' },
  { prop: 'city', label: '城市' },
  { prop: 'address', label: '地址', minWidth: 180 },
  { prop: 'contact', label: '联系人' },
  { prop: 'phone', label: '电话' },
  { prop: 'status', label: '状态', type: 'select', options: [
    { value: 'ACTIVE', label: '正常' }, { value: 'SUSPENDED', label: '暂停' }, { value: 'TERMINATED', label: '终止' }] },
  { prop: 'contractStart', label: '合同开始' },
  { prop: 'contractEnd', label: '合同结束' },
  { prop: 'creditLimit', label: '信用额度', type: 'number', precision: 2 },
  { prop: 'laborRate', label: '工时单价', type: 'number', precision: 2 },
  { prop: 'taxNo', label: '税号' },
  { prop: 'bankAccount', label: '银行账户' },
  { prop: 'parentDealerCode', label: '上级经销商' }
]
</script>
