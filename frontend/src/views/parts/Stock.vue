<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="inForm.partNo" placeholder="备件号" style="width: 140px" />
        <el-input v-model="inForm.location" placeholder="库位 如A-01-01" style="width: 140px" />
        <el-input v-model="inForm.batchNo" placeholder="批次号" style="width: 140px" />
        <el-input-number v-model="inForm.qty" :min="1" placeholder="数量" style="width: 130px" />
        <el-button type="success" @click="inbound">入库</el-button>
      </div>
    </div>
    <CrudPage ref="page" title="备件库存" :api="parts.stock" :columns="columns" :extra-params="extra" readonly />
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import CrudPage from '../../components/CrudPage.vue'
import { parts } from '../../api'
import { store } from '../../store'

const page = ref()
const inForm = reactive({ partNo: '', location: 'A-01-01', batchNo: 'B' + new Date().toISOString().slice(0, 10).replace(/-/g, ''), qty: 10 })
const extra = () => ({ dealerCode: store.dealerCode })

async function inbound() {
  await parts.inbound({ ...inForm, dealerCode: store.dealerCode })
  ElMessage.success('入库成功')
  await page.value?.load()
}

const columns = [
  { prop: 'dealerCode', label: '经销商' },
  { prop: 'partNo', label: '备件号' },
  { prop: 'location', label: '库位' },
  { prop: 'batchNo', label: '批次' },
  { prop: 'qty', label: '数量', type: 'number', precision: 0 },
  { prop: 'reservedQty', label: '预留', type: 'number', precision: 0 }
]
</script>
