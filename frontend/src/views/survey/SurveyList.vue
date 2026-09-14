<template>
  <CrudPage title="调研记录" :api="survey.survey" :columns="columns" :extra-params="extra" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { survey } from '../../api'
import { store } from '../../store'

const extra = () => ({ dealerCode: store.dealerCode })

const columns = [
  { prop: 'surveyNo', label: '调研单号', minWidth: 140 },
  { prop: 'templateCode', label: '模板' },
  { prop: 'dealerCode', label: '经销商' },
  { prop: 'customerId', label: '客户ID', type: 'number', precision: 0 },
  { prop: 'orderId', label: '工单ID', type: 'number', precision: 0 },
  { prop: 'channel', label: '渠道', type: 'select', options: [
    { value: 'SMS', label: '短信' }, { value: 'APP', label: 'APP' }, { value: 'PHONE', label: '电话' }] },
  { prop: 'status', label: '状态', type: 'select', options: [
    { value: 'PENDING', label: '待填写' }, { value: 'ANSWERED', label: '已作答' }, { value: 'EXPIRED', label: '已过期' }] },
  { prop: 'totalScore', label: '总分' },
  { prop: 'npsScore', label: 'NPS得分' },
  { prop: 'sentTime', label: '发送时间', minWidth: 150 },
  { prop: 'answeredTime', label: '作答时间', minWidth: 150 }
]
</script>
