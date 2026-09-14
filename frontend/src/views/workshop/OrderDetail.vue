<template>
  <div class="page" v-loading="loading">
    <div class="card" v-if="order">
      <div style="display: flex; justify-content: space-between; align-items: center">
        <h3 style="margin: 0">工单 {{ order.orderNo }} <StatusTag :value="order.status" /></h3>
        <el-button @click="$router.back()">返回</el-button>
      </div>
      <el-steps :active="stepIndex" align-center style="margin: 16px 0" finish-status="success">
        <el-step v-for="s in steps" :key="s" :title="stepNames[s]" />
      </el-steps>
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="车牌">{{ order.plateNo }}</el-descriptions-item>
        <el-descriptions-item label="VIN">{{ order.vin }}</el-descriptions-item>
        <el-descriptions-item label="进店里程">{{ order.mileageIn }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ order.orderType }}/{{ order.serviceType }}</el-descriptions-item>
        <el-descriptions-item label="客户描述" :span="2">{{ order.complaint }}</el-descriptions-item>
        <el-descriptions-item label="诊断" :span="2">{{ order.diagnosis }}</el-descriptions-item>
        <el-descriptions-item label="工时费">{{ order.laborAmount }}</el-descriptions-item>
        <el-descriptions-item label="备件费">{{ order.partsAmount }}</el-descriptions-item>
        <el-descriptions-item label="厂家承担">{{ order.warrantyAmount }}</el-descriptions-item>
        <el-descriptions-item label="客户应付">{{ order.customerPayable }}(税{{ order.taxAmount }})</el-descriptions-item>
      </el-descriptions>

      <div class="detail-actions">
        <el-button v-if="order.status === 'CHECKED_IN' || order.status === 'DIAGNOSED'" @click="guideVisible = true">带入维修指导</el-button>
        <el-button v-if="order.status === 'CHECKED_IN'" type="primary" @click="doDiagnose">诊断完成</el-button>
        <el-button v-if="order.status === 'DIAGNOSED'" type="primary" @click="doAction('quote')">报价</el-button>
        <el-button v-if="order.status === 'QUOTED'" type="primary" @click="doAction('approve')">客户确认</el-button>
        <el-button v-if="order.status === 'APPROVED'" type="primary" @click="dispatchVisible = true">派工</el-button>
        <el-button v-if="order.status === 'DISPATCHED'" type="primary" @click="doAction('start')">开工</el-button>
        <el-button v-if="order.status === 'IN_REPAIR'" type="primary" @click="doAction('finish')">完工提交质检</el-button>
        <el-button v-if="order.status === 'QC_PENDING'" type="primary" @click="qcVisible = true">质检</el-button>
        <el-button v-if="order.status === 'QC_PASSED'" type="primary" @click="settleVisible = true">结算</el-button>
        <el-button v-if="order.status === 'SETTLED'" type="success" @click="doAction('deliver')">交车</el-button>
        <el-button v-if="order.status === 'DELIVERED'" @click="doAction('close')">关单</el-button>
        <el-popconfirm v-if="canCancel" title="确认取消工单？" @confirm="doAction('cancel')">
          <template #reference><el-button type="danger">取消工单</el-button></template>
        </el-popconfirm>
      </div>

      <el-tabs>
        <el-tab-pane label="工时">
          <el-table :data="labors" border size="small">
            <el-table-column prop="laborCode" label="编码" width="90" />
            <el-table-column prop="name" label="名称" min-width="160" />
            <el-table-column prop="hours" label="工时" width="80" />
            <el-table-column prop="rate" label="单价" width="90" />
            <el-table-column prop="amount" label="金额" width="90" />
            <el-table-column label="保修" width="70">
              <template #default="{ row }"><el-tag v-if="row.isWarranty" size="small" type="warning">保修</el-tag></template>
            </el-table-column>
            <el-table-column v-if="editable" label="操作" width="70">
              <template #default="{ row }">
                <el-button link type="danger" size="small" @click="removeLabor(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="editable" style="margin-top: 8px; display: flex; gap: 8px">
            <el-select v-model="newLabor.laborCode" placeholder="工时项目" filterable style="width: 240px">
              <el-option v-for="l in laborOptions" :key="l.value" :label="l.label" :value="l.value" />
            </el-select>
            <el-checkbox v-model="newLabor.isWarranty">保修</el-checkbox>
            <el-button type="primary" size="small" @click="addLabor">添加工时</el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane label="备件">
          <el-table :data="partsList" border size="small">
            <el-table-column prop="partNo" label="备件号" width="90" />
            <el-table-column prop="name" label="名称" min-width="140" />
            <el-table-column prop="qty" label="数量" width="70" />
            <el-table-column prop="unitPrice" label="单价" width="90" />
            <el-table-column prop="amount" label="金额" width="90" />
            <el-table-column label="标记" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.isWarranty" size="small" type="warning">保修</el-tag>
                <el-tag v-if="row.reservedFlag" size="small" type="success">已预留</el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="editable" label="操作" width="70">
              <template #default="{ row }">
                <el-button link type="danger" size="small" @click="removePart(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="editable" style="margin-top: 8px; display: flex; gap: 8px">
            <el-select v-model="newPart.partNo" placeholder="备件" filterable style="width: 240px">
              <el-option v-for="p in partOptions" :key="p.value" :label="p.label" :value="p.value" />
            </el-select>
            <el-input-number v-model="newPart.qty" :min="1" style="width: 110px" />
            <el-checkbox v-model="newPart.isWarranty">保修</el-checkbox>
            <el-button type="primary" size="small" @click="addPart">添加备件</el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane label="日志">
          <el-timeline>
            <el-timeline-item v-for="l in logs" :key="l.id" :timestamp="l.logTime">
              {{ l.fromStatus || '-' }} → {{ l.toStatus }} {{ l.operator }} {{ l.remark }}
            </el-timeline-item>
          </el-timeline>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="dispatchVisible" title="派工" width="480px">
      <el-form label-width="80px">
        <el-form-item label="技师">
          <el-select v-model="dispatchForm.technicianCode" filterable style="width: 100%">
            <el-option v-for="t in idleTechs" :key="t.code" :label="`${t.code} ${t.name}(${t.level})`" :value="t.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="工位">
          <el-select v-model="dispatchForm.bayCode" style="width: 100%">
            <el-option v-for="b in idleBays" :key="b.code" :label="`${b.code}(${b.type})`" :value="b.code" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" @click="doDispatch">派工</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="qcVisible" title="质检" width="480px">
      <el-form label-width="80px">
        <el-form-item label="结果">
          <el-radio-group v-model="qcForm.pass">
            <el-radio :value="true">合格</el-radio>
            <el-radio :value="false">不合格(返工)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="qcForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="qcVisible = false">取消</el-button>
        <el-button type="primary" @click="doQc">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="settleVisible" title="结算" width="560px">
      <el-form label-width="100px">
        <el-form-item label="优惠金额"><el-input-number v-model="settleForm.discountAmount" :min="0" :precision="2" style="width: 200px" /></el-form-item>
        <el-form-item label="需要发票"><el-switch v-model="settleForm.needInvoice" /></el-form-item>
        <template v-if="settleForm.needInvoice">
          <el-form-item label="发票类型">
            <el-select v-model="settleForm.invoiceType" style="width: 200px">
              <el-option value="NORMAL" label="增值税普通发票" />
              <el-option value="SPECIAL" label="增值税专用发票" />
              <el-option value="ELECTRONIC" label="电子普通发票" />
            </el-select>
          </el-form-item>
          <el-form-item label="抬头"><el-input v-model="settleForm.buyerName" /></el-form-item>
          <el-form-item label="税号"><el-input v-model="settleForm.buyerTaxNo" /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="settleVisible = false">取消</el-button>
        <el-button type="primary" @click="doSettle">结算</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="guideVisible" title="带入维修指导" width="640px">
      <el-table :data="guides" size="small" border>
        <el-table-column prop="code" label="编码" width="90" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="difficulty" label="难度" width="70" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="applyGuide(row.code)">一键带入</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import { guide, network, parts, workshop } from '../../api'
import { store } from '../../store'

const route = useRoute()
const id = route.params.id
const loading = ref(false)
const order = ref(null)
const labors = ref([])
const partsList = ref([])
const logs = ref([])
const laborOptions = ref([])
const partOptions = ref([])
const idleTechs = ref([])
const idleBays = ref([])
const guides = ref([])
const dispatchVisible = ref(false)
const qcVisible = ref(false)
const settleVisible = ref(false)
const guideVisible = ref(false)
const dispatchForm = reactive({ technicianCode: '', bayCode: '' })
const qcForm = reactive({ pass: true, remark: '' })
const settleForm = reactive({ discountAmount: 0, needInvoice: false, invoiceType: 'ELECTRONIC', buyerName: '', buyerTaxNo: '' })
const newLabor = reactive({ laborCode: '', isWarranty: false })
const newPart = reactive({ partNo: '', qty: 1, isWarranty: false })

const steps = ['CHECKED_IN', 'DIAGNOSED', 'QUOTED', 'APPROVED', 'DISPATCHED', 'IN_REPAIR', 'QC_PENDING', 'QC_PASSED', 'SETTLED', 'DELIVERED']
const stepNames = {
  CHECKED_IN: '接车', DIAGNOSED: '诊断', QUOTED: '报价', APPROVED: '客户确认',
  DISPATCHED: '派工', IN_REPAIR: '维修', QC_PENDING: '待质检', QC_PASSED: '质检合格',
  SETTLED: '结算', DELIVERED: '交车'
}
const stepIndex = computed(() => {
  if (!order.value) return 0
  if (order.value.status === 'QC_FAILED') return steps.indexOf('IN_REPAIR')
  const i = steps.indexOf(order.value.status)
  return i < 0 ? 0 : i + 1
})
const editable = computed(() =>
  ['DRAFT', 'CHECKED_IN', 'DIAGNOSED', 'QUOTED'].includes(order.value?.status))
const canCancel = computed(() =>
  ['DRAFT', 'CHECKED_IN', 'DIAGNOSED', 'QUOTED', 'APPROVED'].includes(order.value?.status))

async function load() {
  loading.value = true
  try {
    const d = await workshop.orderDetail(id)
    order.value = d.order
    labors.value = d.labors || []
    partsList.value = d.parts || []
    logs.value = d.logs || []
  } finally {
    loading.value = false
  }
}

async function doAction(action, body) {
  await workshop.action(id, action, body)
  ElMessage.success('操作成功')
  await load()
}

async function doDiagnose() {
  const { value } = await ElMessageBox.prompt('请输入诊断结论', '诊断', { inputValue: order.value.diagnosis || '' })
  await doAction('diagnose', { diagnosis: value })
}

async function doDispatch() {
  await workshop.action(id, 'dispatch', dispatchForm)
  dispatchVisible.value = false
  ElMessage.success('已派工')
  await load()
}

async function doQc() {
  await workshop.action(id, 'qc', { pass: qcForm.pass, remark: qcForm.remark })
  qcVisible.value = false
  ElMessage.success('质检完成')
  await load()
}

async function doSettle() {
  await workshop.action(id, 'settle', settleForm)
  settleVisible.value = false
  ElMessage.success('已结算')
  await load()
}

async function applyGuide(code) {
  await workshop.applyGuide(id, code)
  guideVisible.value = false
  ElMessage.success('已带入工时与备件')
  await load()
}

async function addLabor() {
  await workshop.addLabor(id, newLabor)
  newLabor.laborCode = ''
  newLabor.isWarranty = false
  await load()
}

async function removeLabor(lineId) {
  await workshop.removeLabor(id, lineId)
  await load()
}

async function addPart() {
  await workshop.addPart(id, newPart)
  newPart.partNo = ''
  newPart.qty = 1
  newPart.isWarranty = false
  await load()
}

async function removePart(lineId) {
  await workshop.removePart(id, lineId)
  await load()
}

onMounted(async () => {
  await load()
  const [ls, ps, ts, bs, gs] = await Promise.all([
    guide.labor.list({ size: 200 }),
    parts.part.list({ size: 500 }),
    network.technician.list({ size: 200, dealerCode: store.dealerCode, status: 'IDLE' }),
    network.bay.list({ size: 200, dealerCode: store.dealerCode, status: 'IDLE' }),
    guide.guide.list({ size: 200 })
  ])
  laborOptions.value = (ls || []).map((l) => ({ label: `${l.code} ${l.name}(${l.standardHours}h)`, value: l.code }))
  partOptions.value = (ps || []).map((p) => ({ label: `${p.partNo} ${p.name} ￥${p.salePrice}`, value: p.partNo }))
  idleTechs.value = ts || []
  idleBays.value = bs || []
  guides.value = gs || []
})
</script>
