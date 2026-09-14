<template>
  <el-tag :type="meta.type" size="small">{{ meta.text }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: {
    type: String,
    default: ''
  }
})

const dictionary = {
  ACTIVE: ['正常', 'success'],
  SUSPENDED: ['暂停', 'warning'],
  TERMINATED: ['终止', 'danger'],
  DEALER: ['经销商', 'primary'],
  DIRECT: ['直营店', 'warning'],
  NEW: ['新建', 'info'],
  ALLOCATED: ['已分配', 'primary'],
  INVOICED: ['已开票', 'warning'],
  DELIVERED: ['已交付', 'success'],
  CANCELLED: ['已取消', 'danger'],
  IN_STOCK: ['在库', 'success'],
  SOLD: ['已售', 'info'],
  IDLE: ['空闲', 'success'],
  BUSY: ['忙碌', 'warning'],
  OFF: ['停用', 'info'],
  BOOKED: ['已预约', 'primary'],
  ARRIVED: ['已到店', 'success'],
  NO_SHOW: ['爽约', 'danger'],
  DRAFT: ['草稿', 'info'],
  CHECKED_IN: ['已接车', 'primary'],
  DIAGNOSED: ['已诊断', 'primary'],
  QUOTED: ['已报价', 'warning'],
  APPROVED: ['已确认', 'warning'],
  DISPATCHED: ['已派工', 'warning'],
  IN_REPAIR: ['维修中', 'warning'],
  QC_PENDING: ['待质检', 'warning'],
  QC_PASSED: ['质检合格', 'success'],
  QC_FAILED: ['质检不合格', 'danger'],
  SETTLED: ['已结算', 'success'],
  CLOSED: ['已关闭', 'success'],
  SUBMITTED: ['已提交', 'primary'],
  APPROVED_CLAIM: ['已审核', 'success'],
  REJECTED: ['已拒绝', 'danger'],
  PAID: ['已支付', 'success'],
  PENDING: ['待填写', 'warning'],
  ANSWERED: ['已作答', 'success'],
  EXPIRED: ['已过期', 'info'],
  OPEN: ['待处理', 'danger'],
  PROCESSING: ['处理中', 'warning'],
  RESOLVED: ['已解决', 'success'],
  ISSUING: ['开具中', 'warning'],
  ISSUED: ['已开具', 'success'],
  RED_FLUSHED: ['已红冲', 'danger'],
  FAILED: ['失败', 'danger'],
  IN: ['入库', 'success'],
  OUT: ['出库', 'warning'],
  RESERVE: ['预留', 'primary'],
  RELEASE: ['释放', 'info'],
  ADJUST: ['调整', 'warning']
}

const meta = computed(() => {
  const item = dictionary[props.value]
  return item ? { text: item[0], type: item[1] } : { text: props.value, type: 'info' }
})
</script>
