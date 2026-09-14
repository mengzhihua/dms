<template>
  <CrudPage title="技术通报(TSB)" :api="guide.bulletin" :columns="columns" />
</template>

<script setup>
import CrudPage from '../../components/CrudPage.vue'
import { guide } from '../../api'

const columns = [
  { prop: 'code', label: '编码' },
  { prop: 'title', label: '标题', minWidth: 200 },
  { prop: 'modelCodes', label: '适用车型' },
  { prop: 'content', label: '内容', type: 'textarea', span: 24 },
  { prop: 'issueDate', label: '发布日期' }
]
</script>
