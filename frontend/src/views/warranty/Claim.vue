<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="索赔单号/VIN/车牌" @keyup.enter="load" />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 150px" @change="load">
          <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <el-table :data="rows" border stripe size="small" v-loading="loading">
        <el-table-column prop="claimNo" label="索赔单号" min-width="140" />
        <el-table-column prop="vin" label="VIN" min-width="140" />
        <el-table-column prop="faultDesc" label="故障描述" min-width="160" show-overflow-tooltip />
        <el-table-column prop="dealerCode" label="经销商" width="90" />
        <el-table-column prop="amount" label="索赔金额" width="100" />
        <el-table-column prop="approvedAmount" label="核准金额" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canDealerEdit(row)" link type="primary" size="small" @click="openDetail(row)">编辑/提交</el-button>
            <el-button v-if="isOem && row.status === 'SUBMITTED'" link type="success" size="small" @click="openDetail(row)">审核</el-button>
            <el-button v-if="!isOem && row.status === 'PARTS_RETURNING'" link type="warning" size="small" @click="openDetail(row)">发货</el-button>
            <el-button v-if="isOem && row.status === 'PARTS_SHIPPED'" link type="success" size="small" @click="openDetail(row)">签收</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-drawer v-model="drawer" :title="`索赔单 ${detail.claimNo || ''}`" size="640px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="状态"><StatusTag :value="detail.status" /></el-descriptions-item>
        <el-descriptions-item label="经销商">{{ detail.dealerCode }}</el-descriptions-item>
        <el-descriptions-item label="VIN">{{ detail.vin }}</el-descriptions-item>
        <el-descriptions-item label="车牌">{{ detail.plateNo }}</el-descriptions-item>
        <el-descriptions-item label="里程">{{ detail.mileage }}</el-descriptions-item>
        <el-descriptions-item label="维修日期">{{ detail.repairDate }}</el-descriptions-item>
        <el-descriptions-item label="索赔金额">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="核准金额">{{ detail.approvedAmount }}</el-descriptions-item>
        <el-descriptions-item label="故障码">{{ detail.faultCode }}</el-descriptions-item>
        <el-descriptions-item label="快递单号">{{ detail.returnShipNo }}</el-descriptions-item>
        <el-descriptions-item label="故障描述" :span="2">{{ detail.faultDesc }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.oemRemark" label="厂家意见" :span="2">
          <span style="color:#e6a23c">{{ detail.oemRemark }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-form v-if="canDealerEdit(detail)" label-width="80px" style="margin-top:12px">
        <el-form-item label="故障码"><el-input v-model="edit.faultCode" /></el-form-item>
        <el-form-item label="故障描述"><el-input v-model="edit.faultDesc" type="textarea" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="edit.remark" /></el-form-item>
        <el-button type="primary" size="small" @click="saveEdit">保存修改</el-button>
      </el-form>

      <h4 style="margin:16px 0 8px">索赔明细</h4>
      <el-table :data="lines" border size="small">
        <el-table-column prop="lineType" label="类型" width="70">
          <template #default="{ row }">{{ row.lineType === 'LABOR' ? '工时' : '配件' }}</template>
        </el-table-column>
        <el-table-column prop="code" label="编码" width="90" />
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="qty" label="数量" width="70" />
        <el-table-column prop="unitPrice" label="单价" width="90" />
        <el-table-column prop="amount" label="金额" width="90" />
        <el-table-column prop="approvedAmount" label="核准" width="90" />
        <el-table-column v-if="isOem && detail.status === 'SUBMITTED'" label="回收" width="60">
          <template #default="{ row }">
            <el-checkbox
              v-if="row.lineType === 'PART'"
              :model-value="returnIds.includes(row.id)"
              @change="(v) => toggleReturn(row.id, v)" />
          </template>
        </el-table-column>
        <el-table-column v-else label="回收" width="60">
          <template #default="{ row }">{{ row.returnRequired ? '是' : '' }}</template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px">
        <template v-if="canDealerEdit(detail)">
          <el-button type="primary" @click="doSubmit">提交厂家审核</el-button>
        </template>
        <template v-if="isOem && detail.status === 'SUBMITTED'">
          <el-form label-width="80px" style="margin-bottom:8px">
            <el-form-item label="核准金额"><el-input-number v-model="approveForm.approvedAmount" :min="0" :precision="2" /></el-form-item>
            <el-form-item label="厂家意见"><el-input v-model="approveForm.oemRemark" /></el-form-item>
          </el-form>
          <el-button type="success" @click="doApprove">审核通过</el-button>
          <el-button type="warning" @click="doReturn">退回</el-button>
          <el-button type="danger" @click="doReject">拒绝</el-button>
        </template>
        <template v-if="!isOem && detail.status === 'PARTS_RETURNING'">
          <el-input v-model="shipNo" placeholder="旧件快递单号" style="width:200px;margin-right:8px" />
          <el-button type="warning" @click="doShip">发货</el-button>
        </template>
        <template v-if="isOem && detail.status === 'PARTS_SHIPPED'">
          <el-button type="success" @click="doReceive">签收旧件并核准</el-button>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import { warranty } from '../../api'
import { store } from '../../store'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 20, keyword: '', status: '' })
const statuses = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'SUBMITTED', label: '已提交' },
  { value: 'RETURNED', label: '已退回' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'PARTS_RETURNING', label: '待回收旧件' },
  { value: 'PARTS_SHIPPED', label: '旧件在途' },
  { value: 'APPROVED', label: '已核准' },
  { value: 'SETTLED', label: '已结算' },
  { value: 'PAID', label: '已支付' }
]

const role = computed(() => store.user?.role)
const isOem = computed(() => ['OEM', 'ADMIN'].includes(role.value))
const isDealerSide = computed(() => role.value !== 'OEM')
const canDealerEdit = (row) =>
  isDealerSide.value && ['DRAFT', 'RETURNED'].includes(row.status)

const drawer = ref(false)
const detail = ref({})
const lines = ref([])
const returnIds = ref([])
const shipNo = ref('')
const edit = reactive({ faultCode: '', faultDesc: '', remark: '' })
const approveForm = reactive({ approvedAmount: 0, oemRemark: '' })

async function load() {
  loading.value = true
  try {
    const p = await warranty.claim.page({ ...query, dealerCode: store.dealerCode })
    rows.value = p.records || []
    total.value = p.total || 0
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  detail.value = (await warranty.claim.get(row.id)) || row
  lines.value = (await warranty.lines(row.id)) || []
  returnIds.value = []
  shipNo.value = ''
  edit.faultCode = detail.value.faultCode || ''
  edit.faultDesc = detail.value.faultDesc || ''
  edit.remark = detail.value.remark || ''
  approveForm.approvedAmount = detail.value.amount || 0
  approveForm.oemRemark = ''
  drawer.value = true
}

function toggleReturn(id, checked) {
  if (checked) {
    returnIds.value = [...returnIds.value, id]
  } else {
    returnIds.value = returnIds.value.filter((x) => x !== id)
  }
}

async function refresh() {
  await load()
  if (detail.value.id) {
    detail.value = (await warranty.claim.get(detail.value.id)) || detail.value
    lines.value = (await warranty.lines(detail.value.id)) || []
  }
}

async function saveEdit() {
  await warranty.claim.update(detail.value.id, { ...edit })
  ElMessage.success('已保存')
  await refresh()
}

async function doSubmit() {
  await warranty.submit(detail.value.id)
  ElMessage.success('已提交厂家审核')
  await refresh()
}

async function doApprove() {
  await warranty.approve(detail.value.id, {
    approvedAmount: approveForm.approvedAmount,
    oemRemark: approveForm.oemRemark,
    returnLineIds: returnIds.value
  })
  ElMessage.success('已审核')
  await refresh()
}

async function doReject() {
  const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝索赔', { inputValidator: (v) => !!v || '原因必填' })
  await warranty.reject(detail.value.id, { oemRemark: value })
  ElMessage.success('已拒绝')
  await refresh()
}

async function doReturn() {
  const { value } = await ElMessageBox.prompt('请输入退回原因', '退回索赔', { inputValidator: (v) => !!v || '原因必填' })
  await warranty.returnBack(detail.value.id, { oemRemark: value })
  ElMessage.success('已退回')
  await refresh()
}

async function doShip() {
  await warranty.ship(detail.value.id, { returnShipNo: shipNo.value })
  ElMessage.success('已发货')
  await refresh()
}

async function doReceive() {
  await warranty.receive(detail.value.id)
  ElMessage.success('已签收，索赔核准')
  await refresh()
}

onMounted(load)
window.addEventListener('dealer-change', load)
</script>
