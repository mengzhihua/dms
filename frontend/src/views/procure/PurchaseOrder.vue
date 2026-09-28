<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="采购单号/厂家单号" @keyup.enter="load" />
        <el-select v-model="query.status" clearable placeholder="状态" style="width:160px" @change="load">
          <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button v-if="isDealerSide" type="success" @click="openCreate">新建采购单</el-button>
        <el-button v-if="isDealerSide" type="warning" @click="doFromShortage">缺货一键采购</el-button>
      </div>
      <el-table :data="rows" border stripe size="small" v-loading="loading">
        <el-table-column prop="poNo" label="采购单号" min-width="140" />
        <el-table-column prop="dealerCode" label="经销商" width="90" />
        <el-table-column prop="source" label="来源" width="90" />
        <el-table-column prop="totalAmount" label="订单金额" width="100" />
        <el-table-column prop="receivedAmount" label="已到货金额" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column prop="oemOrderNo" label="厂家单号" width="120" />
        <el-table-column label="操作" width="300">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="isDealerSide && row.status === 'DRAFT'" link type="primary" size="small" @click="doSubmit(row)">提交</el-button>
            <el-button v-if="isOem && row.status === 'SUBMITTED'" link type="success" size="small" @click="confirmRow = row; confirmVisible = true">确认</el-button>
            <el-button v-if="isOem && row.status === 'SUBMITTED'" link type="warning" size="small" @click="doReject(row)">退回</el-button>
            <el-button v-if="isDealerSide && ['DRAFT','SUBMITTED'].includes(row.status)" link type="danger" size="small" @click="doCancel(row)">取消</el-button>
            <el-button v-if="isDealerSide && ['CONFIRMED','PARTIAL_RECEIVED'].includes(row.status)" link type="warning" size="small" @click="openDetail(row, true)">到货</el-button>
            <el-button v-if="isDealerSide && row.status === 'RECEIVED'" link type="info" size="small" @click="doClose(row)">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="createVisible" title="新建采购订单" width="640px">
      <el-form label-width="80px">
        <el-form-item label="期望到货"><el-date-picker v-model="form.expectDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
        <el-form-item label="明细">
          <el-table :data="form.lines" border size="small">
            <el-table-column label="备件号" width="160">
              <template #default="{ row }">
                <el-select v-model="row.partNo" filterable placeholder="选择" @change="(v) => pickPart(row, v)">
                  <el-option v-for="p in parts" :key="p.partNo" :label="`${p.partNo} ${p.name}`" :value="p.partNo" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="110">
              <template #default="{ row }"><el-input-number v-model="row.qty" :min="1" size="small" /></template>
            </el-table-column>
            <el-table-column label="单价" width="130">
              <template #default="{ row }"><el-input-number v-model="row.unitPrice" :min="0" :precision="2" size="small" /></template>
            </el-table-column>
            <el-table-column width="60">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" @click="form.lines.splice($index, 1)">删</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button size="small" style="margin-top:6px" @click="form.lines.push({ partNo: '', qty: 1, unitPrice: null })">加行</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="confirmVisible" title="厂家确认订单" width="420px">
      <el-form label-width="80px">
        <el-form-item label="厂家单号"><el-input v-model="confirmForm.oemOrderNo" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="confirmForm.oemRemark" /></el-form-item>
        <el-form-item label="期望到货"><el-date-picker v-model="confirmForm.expectDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmVisible = false">取消</el-button>
        <el-button type="primary" @click="doConfirm">确认</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawer" :title="`采购单 ${detail.poNo || ''}`" size="680px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="状态"><StatusTag :value="detail.status" /></el-descriptions-item>
        <el-descriptions-item label="经销商">{{ detail.dealerCode }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">{{ detail.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="已到货金额">{{ detail.receivedAmount }}</el-descriptions-item>
        <el-descriptions-item label="厂家单号">{{ detail.oemOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="期望到货">{{ detail.expectDate }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.oemRemark" label="厂家备注" :span="2">{{ detail.oemRemark }}</el-descriptions-item>
      </el-descriptions>

      <h4 style="margin:16px 0 8px">订单明细</h4>
      <el-table :data="lines" border size="small">
        <el-table-column prop="partNo" label="备件号" width="90" />
        <el-table-column prop="name" label="名称" min-width="110" />
        <el-table-column prop="qty" label="订购" width="60" />
        <el-table-column prop="receivedQty" label="已到" width="60" />
        <el-table-column prop="unitPrice" label="单价" width="90" />
        <el-table-column prop="amount" label="金额" width="90" />
        <el-table-column v-if="recvVisible" label="本次到货" width="110">
          <template #default="{ row }">
            <el-input-number v-model="recvMap[row.id]" :min="0" :max="row.qty - (row.receivedQty || 0)" size="small" />
          </template>
        </el-table-column>
      </el-table>

      <div v-if="recvVisible" style="margin-top:12px">
        <el-input v-model="recvLocation" placeholder="库位" style="width:130px;margin-right:8px" />
        <el-input v-model="recvBatch" placeholder="批次号(可选)" style="width:150px;margin-right:8px" />
        <el-button type="warning" @click="doReceive">确认到货入库</el-button>
      </div>

      <h4 v-if="receipts.length" style="margin:16px 0 8px">到货记录</h4>
      <div v-for="r in receipts" :key="r.id" style="margin-bottom:8px;font-size:13px">
        {{ r.receiptNo }} · {{ r.location }}<span v-if="r.batchNo"> · {{ r.batchNo }}</span> · {{ r.createdAt }}
        <div v-for="l in r.lines" :key="l.id" style="padding-left:16px;color:#888">
          {{ l.partNo }} × {{ l.qty }}（{{ l.amount }}）
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import { parts as partsApi, procure } from '../../api'
import { store } from '../../store'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 20, keyword: '', status: '' })
const statuses = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'SUBMITTED', label: '已提交' },
  { value: 'CONFIRMED', label: '已确认' },
  { value: 'PARTIAL_RECEIVED', label: '部分到货' },
  { value: 'RECEIVED', label: '已到货' },
  { value: 'CLOSED', label: '已关闭' },
  { value: 'CANCELLED', label: '已取消' }
]
const parts = ref([])

const role = computed(() => store.user?.role)
const isOem = computed(() => ['OEM', 'ADMIN'].includes(role.value))
const isDealerSide = computed(() => ['DEALER_MANAGER', 'ADVISOR'].includes(role.value))

const createVisible = ref(false)
const form = reactive({ expectDate: '', remark: '', lines: [] })
const drawer = ref(false)
const recvVisible = ref(false)
const detail = ref({})
const lines = ref([])
const receipts = ref([])
const recvMap = reactive({})
const recvLocation = ref('RCV-01')
const recvBatch = ref('')
const confirmVisible = ref(false)
const confirmRow = ref({})
const confirmForm = reactive({ oemOrderNo: '', oemRemark: '', expectDate: '' })

async function load() {
  loading.value = true
  try {
    const p = await procure.order.page({ ...query, dealerCode: store.dealerCode })
    rows.value = p.records || []
    total.value = p.total || 0
  } finally {
    loading.value = false
  }
}

function pickPart(row, partNo) {
  const p = parts.value.find((x) => x.partNo === partNo)
  if (p && !row.unitPrice) row.unitPrice = p.costPrice
}

function openCreate() {
  form.expectDate = ''
  form.remark = ''
  form.lines = [{ partNo: '', qty: 1, unitPrice: null }]
  createVisible.value = true
}

async function doCreate() {
  await procure.order.create({ ...form, dealerCode: store.dealerCode })
  ElMessage.success('已创建')
  createVisible.value = false
  await load()
}

async function doFromShortage() {
  try {
    await procure.orderFromShortage(store.dealerCode)
    ElMessage.success('已按缺货生成采购单')
  } catch (e) {
    // 消息已由拦截器提示
  }
  await load()
}

async function openDetail(row, receive = false) {
  detail.value = row
  lines.value = (await procure.orderLines(row.id)) || []
  receipts.value = (await procure.orderReceipts(row.id)) || []
  Object.keys(recvMap).forEach((k) => delete recvMap[k])
  lines.value.forEach((l) => {
    recvMap[l.id] = l.qty - (l.receivedQty || 0)
  })
  recvVisible.value = receive
  drawer.value = true
}

async function refreshDetail() {
  await load()
  detail.value = (await procure.order.get(detail.value.id)) || detail.value
  lines.value = (await procure.orderLines(detail.value.id)) || []
  receipts.value = (await procure.orderReceipts(detail.value.id)) || []
  lines.value.forEach((l) => { recvMap[l.id] = l.qty - (l.receivedQty || 0) })
}

async function doSubmit(row) {
  await procure.orderSubmit(row.id)
  ElMessage.success('已提交厂家')
  await load()
}

async function doConfirm() {
  await procure.orderConfirm(confirmRow.value.id, { ...confirmForm })
  ElMessage.success('已确认')
  confirmVisible.value = false
  await load()
}

async function doReject(row) {
  const { value } = await ElMessageBox.prompt('请输入退回原因', '退回订单', { inputValidator: (v) => !!v || '原因必填' })
  await procure.orderReject(row.id, { oemRemark: value })
  ElMessage.success('已退回')
  await load()
}

async function doCancel(row) {
  const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单')
  await procure.orderCancel(row.id, { reason: value })
  ElMessage.success('已取消')
  await load()
}

async function doReceive() {
  const rcvLines = lines.value
    .filter((l) => (recvMap[l.id] || 0) > 0)
    .map((l) => ({ orderLineId: l.id, qty: recvMap[l.id] }))
  if (!rcvLines.length) {
    ElMessage.warning('请填写到货数量')
    return
  }
  await procure.orderReceive(detail.value.id, {
    location: recvLocation.value,
    batchNo: recvBatch.value,
    lines: rcvLines
  })
  ElMessage.success('到货已入库')
  recvVisible.value = false
  await refreshDetail()
}

async function doClose(row) {
  await procure.orderClose(row.id)
  ElMessage.success('已关闭')
  await load()
}

onMounted(async () => {
  load()
  parts.value = (await partsApi.part.list({ size: 500 })) || []
})
window.addEventListener('dealer-change', load)
</script>
