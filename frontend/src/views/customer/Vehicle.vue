<template>
  <div class="page">
    <CrudPage ref="page" title="车辆" :api="customer.vehicle" :columns="columns" :extra-params="extra" />
    <div class="card" style="margin-top: 12px">
      <div class="toolbar">
        <el-input v-model="wcId" placeholder="车辆ID" style="width: 120px" />
        <el-input v-model.number="wcMileage" placeholder="当前里程" style="width: 140px" />
        <el-button @click="warrantyCheck">保修校验</el-button>
        <el-input v-model="hisVin" placeholder="VIN查维修历史" style="width: 220px" />
        <el-button @click="loadHistory">查历史</el-button>
      </div>
      <el-alert v-if="wcResult" :title="wcResult.inWarranty ? '在保修期内' : '不在保修期'" :type="wcResult.inWarranty ? 'success' : 'warning'" :description="wcResult.reason" show-icon :closable="false" />
      <el-table v-if="history.length" :data="history" border stripe size="small" style="margin-top: 12px">
        <el-table-column prop="orderNo" label="工单号" min-width="140" />
        <el-table-column prop="serviceType" label="类型" width="90" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column prop="mileageIn" label="进店里程" width="100" />
        <el-table-column prop="totalAmount" label="金额" width="100" />
        <el-table-column prop="settleTime" label="结算时间" min-width="150" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import CrudPage from '../../components/CrudPage.vue'
import StatusTag from '../../components/StatusTag.vue'
import { customer } from '../../api'
import { store } from '../../store'

const extra = () => ({ dealerCode: store.dealerCode })
const wcId = ref('')
const wcMileage = ref()
const wcResult = ref(null)
const hisVin = ref('')
const history = ref([])

async function warrantyCheck() {
  wcResult.value = await customer.warrantyCheck(wcId.value, wcMileage.value)
}

async function loadHistory() {
  history.value = (await customer.history(hisVin.value)) || []
}

const columns = [
  { prop: 'vin', label: 'VIN', minWidth: 160 },
  { prop: 'plateNo', label: '车牌' },
  { prop: 'modelCode', label: '车型' },
  { prop: 'customerId', label: '客户ID', type: 'number', precision: 0 },
  { prop: 'dealerCode', label: '经销商' },
  { prop: 'mileage', label: '里程', type: 'number', precision: 0 },
  { prop: 'purchaseDate', label: '购车日期' },
  { prop: 'warrantyStart', label: '保修开始' },
  { prop: 'warrantyEnd', label: '保修结束' },
  { prop: 'lastServiceDate', label: '上次保养' },
  { prop: 'nextServiceMileage', label: '下次保养里程', type: 'number', precision: 0 }
]
</script>
