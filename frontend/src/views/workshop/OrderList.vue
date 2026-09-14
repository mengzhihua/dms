<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="工单号/VIN" @keyup.enter="load" />
        <el-input v-model="query.plateNo" clearable placeholder="车牌" @keyup.enter="load" />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
          <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openCreate">新建工单</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" border stripe size="small">
        <el-table-column prop="orderNo" label="工单号" min-width="140" />
        <el-table-column prop="plateNo" label="车牌" width="100" />
        <el-table-column prop="serviceType" label="服务" width="80" />
        <el-table-column prop="orderType" label="类型" width="90" />
        <el-table-column prop="complaint" label="客户描述" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="90" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/workshop/order/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @change="load" />
      </div>
    </div>
    <el-dialog v-model="visible" title="新建工单" width="640px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="预约ID(可空)"><el-input-number v-model="form.appointmentId" :min="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="车辆ID"><el-input-number v-model="form.vehicleId" :min="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="进店里程"><el-input-number v-model="form.mileageIn" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="油量"><el-input v-model="form.fuelLevel" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="服务类型">
            <el-select v-model="form.serviceType" style="width:100%">
              <el-option v-for="t in ['保养','维修','事故','召回']" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="工单类型">
            <el-select v-model="form.orderType" style="width:100%">
              <el-option v-for="t in orderTypes" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="24"><el-form-item label="客户描述"><el-input v-model="form.complaint" type="textarea" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="服务顾问"><el-input v-model="form.advisorName" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">创建并接车</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import StatusTag from '../../components/StatusTag.vue'
import { workshop } from '../../api'
import { store } from '../../store'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const visible = ref(false)
const form = ref({})
const statuses = ['CHECKED_IN','DIAGNOSED','QUOTED','APPROVED','DISPATCHED','IN_REPAIR','QC_PENDING','QC_PASSED','QC_FAILED','SETTLED','DELIVERED','CLOSED','CANCELLED']
const orderTypes = [
  { value: 'REGULAR', label: '普通' }, { value: 'WARRANTY', label: '保修' },
  { value: 'INSURANCE', label: '保险' }, { value: 'INTERNAL', label: '内部' }]
const query = reactive({ current: 1, size: 20, keyword: '', status: '', plateNo: '' })

async function load() {
  loading.value = true
  try {
    const page = await workshop.orderPage({ ...query, dealerCode: store.dealerCode })
    rows.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { orderType: 'REGULAR', serviceType: '维修' }
  visible.value = true
}

async function save() {
  const o = await workshop.orderCreate({ ...form.value, dealerCode: store.dealerCode })
  ElMessage.success('工单已创建')
  visible.value = false
  router.push(`/workshop/order/${o.id}`)
}

onMounted(load)
window.addEventListener('dealer-change', load)
</script>
