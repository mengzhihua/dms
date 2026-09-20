<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="单号/VIN/车型" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openForm()">新增订单</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" border stripe size="small">
        <el-table-column prop="orderNo" label="单号" min-width="140" />
        <el-table-column prop="dealerCode" label="经销商" width="90" />
        <el-table-column prop="modelCode" label="车型" width="90" />
        <el-table-column prop="vin" label="VIN" min-width="150" show-overflow-tooltip />
        <el-table-column prop="price" label="售价" width="100" />
        <el-table-column label="付款方式" width="110">
          <template #default="{ row }">
            <span>{{ row.paymentType === 'LOAN' ? '贷款' : '全款' }}</span>
            <el-tag v-if="row.paymentType === 'LOAN'" size="small" :type="loanTagType(row.loanStatus)" style="margin-left:4px">
              {{ loanStatusText(row.loanStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已收/总价" width="110">
          <template #default="{ row }">{{ row.paidAmount || 0 }}/{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="保险" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.insuranceStatus === 'ISSUED'" size="small" type="success">已投保</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="420" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'NEW'" link type="primary" size="small" @click="act(row, 'allocate')">分配</el-button>
            <el-button v-if="['NEW','ALLOCATED'].includes(row.status)" link type="warning" size="small" @click="openFinance(row)">金融</el-button>
            <el-button v-if="!['DELIVERED','CANCELLED'].includes(row.status)" link type="warning" size="small" @click="openInsure(row)">保险</el-button>
            <el-button v-if="!['DELIVERED','CANCELLED'].includes(row.status)" link type="success" size="small" @click="openPay(row)">收款</el-button>
            <el-button v-if="row.status === 'ALLOCATED'" link type="warning" size="small" @click="openInvoice(row)">开票</el-button>
            <el-button v-if="row.status === 'INVOICED'" link type="success" size="small" @click="openDeliver(row)">交车</el-button>
            <el-button v-if="!['DELIVERED','CANCELLED'].includes(row.status)" link type="danger" size="small" @click="act(row, 'cancelOrder')">取消</el-button>
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="visible" title="整车销售订单" width="640px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="经销商"><el-input v-model="form.dealerCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="客户ID"><el-input-number v-model="form.customerId" :min="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="车型"><el-input v-model="form.modelCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="颜色"><el-input v-model="form.color" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="售价"><el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="定金"><el-input-number v-model="form.deposit" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="finVisible" :title="finRow.loanStatus === 'APPLIED' ? '贷款审批' : '贷款申请'" width="480px">
      <template v-if="finRow.loanStatus === 'APPLIED'">
        <el-form label-width="100px">
          <el-form-item label="金融机构">{{ finRow.loanProvider }}</el-form-item>
          <el-form-item label="金额">{{ finRow.loanAmount }} / {{ finRow.loanTermMonths }}期</el-form-item>
          <el-form-item label="备注"><el-input v-model="decision.remark" /></el-form-item>
        </el-form>
      </template>
      <el-form v-else :model="fin" label-width="100px">
        <el-form-item label="金融机构"><el-input v-model="fin.loanProvider" /></el-form-item>
        <el-form-item label="贷款金额"><el-input-number v-model="fin.loanAmount" :min="0" :max="finRow.price" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="期数(月)"><el-input-number v-model="fin.loanTermMonths" :min="1" :max="60" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="finVisible = false">取消</el-button>
        <template v-if="finRow.loanStatus === 'APPLIED'">
          <el-button type="danger" @click="decide(false)">拒绝</el-button>
          <el-button type="success" @click="decide(true)">批准</el-button>
        </template>
        <el-button v-else type="primary" @click="submitFinance">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="insVisible" title="保险登记" width="480px">
      <el-form :model="ins" label-width="100px">
        <el-form-item label="保险公司"><el-input v-model="ins.company" /></el-form-item>
        <el-form-item label="保单号"><el-input v-model="ins.policyNo" /></el-form-item>
        <el-form-item label="保费"><el-input-number v-model="ins.amount" :min="0" :precision="2" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="insVisible = false">取消</el-button>
        <el-button type="primary" @click="submitInsure">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="payVisible" title="收款" width="480px">
      <el-form :model="pay" label-width="100px">
        <el-form-item label="类型">
          <el-select v-model="pay.payType" style="width:100%">
            <el-option label="定金" value="DEPOSIT" /><el-option label="尾款" value="BALANCE" />
            <el-option label="贷款到账" value="LOAN" /><el-option label="保费" value="INSURANCE" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额"><el-input-number v-model="pay.amount" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="方式">
          <el-select v-model="pay.method" style="width:100%">
            <el-option label="现金" value="CASH" /><el-option label="转账" value="TRANSFER" />
            <el-option label="POS" value="POS" /><el-option label="贷款" value="LOAN" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="pay.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPay">确认收款</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="invVisible" title="开具发票" width="480px">
      <el-form :model="inv" label-width="100px">
        <el-form-item label="发票类型">
          <el-select v-model="inv.invoiceType" style="width:100%">
            <el-option label="机动车销售统一发票(电子)" value="ELECTRONIC" />
            <el-option label="机动车销售统一发票" value="NORMAL" />
            <el-option label="增值税专用发票" value="SPECIAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="购方名称"><el-input v-model="inv.buyerName" /></el-form-item>
        <el-form-item label="购方税号"><el-input v-model="inv.buyerTaxNo" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="invVisible = false">取消</el-button>
        <el-button type="primary" @click="submitInvoice">开票</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="delVisible" title="交车（PDI 检查）" width="480px">
      <el-form :model="del" label-width="100px">
        <el-form-item label="PDI 通过"><el-checkbox v-model="del.pdiPassed">检查通过</el-checkbox></el-form-item>
        <el-form-item label="交付备注"><el-input v-model="del.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="delVisible = false">取消</el-button>
        <el-button type="success" @click="submitDeliver">确认交车</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" :title="`订单详情 ${detail.order?.orderNo || ''}`" size="520px">
      <el-descriptions :column="2" size="small" border>
        <el-descriptions-item label="状态"><StatusTag :value="detail.order?.status" /></el-descriptions-item>
        <el-descriptions-item label="付款">{{ detail.order?.paymentType === 'LOAN' ? '贷款' : '全款' }}</el-descriptions-item>
        <el-descriptions-item label="已收">{{ detail.order?.paidAmount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="售价">{{ detail.order?.price }}</el-descriptions-item>
        <el-descriptions-item label="贷款" :span="2">
          <span v-if="detail.order?.paymentType === 'LOAN'">
            {{ detail.order.loanProvider }} {{ detail.order.loanAmount }} / {{ detail.order.loanTermMonths }}期（{{ loanStatusText(detail.order.loanStatus) }}）
          </span><span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="保险" :span="2">
          <span v-if="detail.order?.insuranceStatus === 'ISSUED'">{{ detail.order.insuranceCompany }} {{ detail.order.insurancePolicyNo }} ￥{{ detail.order.insuranceAmount }}</span><span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="发票" :span="2">
          <template v-if="detail.invoice">
            <router-link :to="`/invoice/list?id=${detail.invoice.id}`">{{ detail.invoice.invoiceNo }}</router-link>
            （{{ detail.invoice.status }}）
          </template><span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="调研" :span="2">
          <router-link v-if="detail.survey" :to="`/survey/answer?id=${detail.survey.id}`">{{ detail.survey.surveyNo }}</router-link>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
      <h4>收款流水</h4>
      <el-table :data="detail.payments || []" size="small" border>
        <el-table-column prop="payType" label="类型" width="90" />
        <el-table-column prop="amount" label="金额" width="100" />
        <el-table-column prop="method" label="方式" width="80" />
        <el-table-column prop="paidAt" label="时间" min-width="140" />
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import { network } from '../../api'
import { store } from '../../store'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const visible = ref(false)
const form = ref({})
const query = reactive({ current: 1, size: 20, keyword: '' })

const finVisible = ref(false)
const finRow = ref({})
const fin = ref({})
const decision = ref({})
const insVisible = ref(false)
const insRow = ref({})
const ins = ref({})
const payVisible = ref(false)
const payRow = ref({})
const pay = ref({})
const invVisible = ref(false)
const invRow = ref({})
const inv = ref({})
const delVisible = ref(false)
const delRow = ref({})
const del = ref({})
const detailVisible = ref(false)
const detail = ref({})

function loanStatusText(s) {
  return { NONE: '无', APPLIED: '已申请', APPROVED: '已批准', REJECTED: '已拒绝' }[s] || s || '-'
}
function loanTagType(s) {
  return { APPLIED: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const page = await network.salesOrder.page({ ...query, dealerCode: store.dealerCode })
    rows.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function openForm(row) {
  form.value = row ? { ...row } : { dealerCode: store.dealerCode }
  visible.value = true
}

async function save() {
  if (form.value.id) {
    await network.salesOrder.update(form.value.id, form.value)
  } else {
    await network.salesOrder.create(form.value)
  }
  ElMessage.success('保存成功')
  visible.value = false
  await load()
}

async function act(row, action) {
  await network[action](row.id)
  ElMessage.success('操作成功')
  await load()
}

function openFinance(row) {
  finRow.value = row
  fin.value = { loanProvider: row.loanProvider || '', loanAmount: row.loanAmount || null, loanTermMonths: row.loanTermMonths || 24 }
  decision.value = { remark: '' }
  finVisible.value = true
}
async function submitFinance() {
  await network.financeApply(finRow.value.id, fin.value)
  ElMessage.success('贷款申请已提交')
  finVisible.value = false
  await load()
}
async function decide(approved) {
  await network.financeDecision(finRow.value.id, { approved, remark: decision.value.remark })
  ElMessage.success(approved ? '已批准' : '已拒绝')
  finVisible.value = false
  await load()
}

function openInsure(row) {
  insRow.value = row
  ins.value = { company: row.insuranceCompany || '', policyNo: row.insurancePolicyNo || '', amount: row.insuranceAmount || null }
  insVisible.value = true
}
async function submitInsure() {
  await network.insure(insRow.value.id, ins.value)
  ElMessage.success('保险已登记')
  insVisible.value = false
  await load()
}

function openPay(row) {
  payRow.value = row
  pay.value = { payType: 'BALANCE', amount: null, method: 'TRANSFER', remark: '' }
  payVisible.value = true
}
async function submitPay() {
  await network.payOrder(payRow.value.id, pay.value)
  ElMessage.success('已收款')
  payVisible.value = false
  await load()
}

function openInvoice(row) {
  invRow.value = row
  inv.value = { invoiceType: 'ELECTRONIC', buyerName: '', buyerTaxNo: '' }
  invVisible.value = true
}
async function submitInvoice() {
  await network.invoiceOrder(invRow.value.id, inv.value)
  ElMessage.success('发票已创建')
  invVisible.value = false
  await load()
}

function openDeliver(row) {
  delRow.value = row
  del.value = { pdiPassed: false, remark: '' }
  delVisible.value = true
}
async function submitDeliver() {
  await network.deliverOrder(delRow.value.id, del.value)
  ElMessage.success('已交车')
  delVisible.value = false
  await load()
}

async function openDetail(row) {
  detail.value = await network.salesDetail(row.id)
  detailVisible.value = true
}

onMounted(load)
window.addEventListener('dealer-change', load)
</script>
