<template>
  <CrudPage title="维修指导库" :api="guide.guide" :columns="columns" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { guide } from '../../api'

const columns = [
  { prop: 'code', label: '编码' },
  { prop: 'title', label: '标题', minWidth: 200 },
  { prop: 'modelCodes', label: '适用车型(逗号/ALL)' },
  { prop: 'dtcCodes', label: '故障码(逗号)' },
  { prop: 'symptoms', label: '症状关键词(逗号)' },
  { prop: 'diagnosisSteps', label: '诊断步骤', type: 'textarea', span: 24, hideInTable: true },
  { prop: 'repairSteps', label: '维修步骤', type: 'textarea', span: 24, hideInTable: true },
  { prop: 'laborItemCodes', label: '工时项目(逗号)' },
  { prop: 'partNos', label: '备件号(逗号)' },
  { prop: 'difficulty', label: '难度', type: 'select', options: [
    { value: '低', label: '低' }, { value: '中', label: '中' }, { value: '高', label: '高' }] },
  { prop: 'safetyNotes', label: '安全提示', span: 24 }
]
</script>
