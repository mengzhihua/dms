<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="询价单号/标题" @keyup.enter="load" />
        <el-select v-model="query.status" clearable placeholder="状态" style="width:140px" @change="load">
          <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button v-if="isDealerSide" type="success" @click="openCreate">新建询价</el-button>
      </div>
      <el-table :data="rows" border stripe size="small" v-loading="loading">
        <el-table-column prop="inquiryNo" label="询价单号" min-width="140" />
        <el-table-column prop="title" label="标题" min-width="150" />
        <el-table-column prop="dealerCode" label="经销商" width="90" />
        <el-table-column prop="expectDate" label="期望到货" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column prop="oemRemark" label="厂家备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canDealerAct(row, ['DRAFT'])" link type="primary" size="small" @click="doSend(row)">发出</el-button>
            <el-button v-if="canDealerAct(row, ['DRAFT','SENT','QUOTED'])" link type="warning" size="small" @click="doClose(row)">关闭</el-button>
            <el-button v-if="canDealerAct(row, ['QUOTED'])" link type="success" size="small" @click="doToOrder(row)">转采购单</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="createVisible" title="新建询价单" width="640px">
      <el-form label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="期望到货"><el-date-picker v-model="form.expectDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
        <el-form-item label="明细">
          <el-table :data="form.lines" border size="small">
            <el-table-column label="备件号" width="140">
              <template #default="{ row }">
                <el-select v-model="row.partNo" filterable placeholder="选择" @change="(v) => pickPart(row, v)">
                  <el-option v-for="p in parts" :key="p.partNo" :label="`${p.partNo} ${p.name}`" :value="p.partNo" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="110">
              <template #default="{ row }"><el-input-number v-model="row.qty" :min="1" size="small" /></template>
            </el-table-column>
            <el-table-column label="目标价" width="130">
              <template #default="{ row }"><el-input-number v-model="row.targetPrice" :min="0" :precision="2" size="small" /></template>
            </el-table-column>
            <el-table-column width="60">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" @click="form.lines.splice($index, 1)">删</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button size="small" style="margin-top:6px" @click="form.lines.push({ partNo: '', qty: 1, targetPrice: null })">加行</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawer" :title="`询价单 ${detail.inquiryNo || ''}`" size="640px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="状态"><StatusTag :value="detail.status" /></el-descriptions-item>
        <el-descriptions-item label="经销商">{{ detail.dealerCode }}</el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.oemRemark" label="厂家备注" :span="2">{{ detail.oemRemark }}</el-descriptions-item>
      </el-descriptions>

      <h4 style="margin:16px 0 8px">询价明细</h4>
      <el-table :data="lines" border size="small">
        <el-table-column prop="partNo" label="备件号" width="90" />
        <el-table-column prop="name" label="名称" min-width="110" />
        <el-table-column prop="qty" label="数量" width="60" />
        <el-table-column prop="targetPrice" label="目标价" width="90" />
        <el-table-column label="报价" width="120">
          <template #default="{ row }">
            <el-input-number v-if="isOem && detail.status === 'SENT'" v-model="quoteMap[row.id].quotedPrice" :min="0" :precision="2" size="small" />
            <span v-else>{{ row.quotedPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column label="交期(天)" width="90">
          <template #default="{ row }">
            <el-input-number v-if="isOem && detail.status === 'SENT'" v-model="quoteMap[row.id].leadDays" :min="0" size="small" />
            <span v-else>{{ row.leadDays }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top:16px" v-if="isOem && detail.status === 'SENT'">
        <el-input v-model="oemRemark" placeholder="厂家备注" style="margin-bottom:8px" />
        <el-button type="success" @click="doQuote">提交报价</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import { parts as partsApi, procure } from '../../api'
import { store } from '../../store'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 20, keyword: '', status: '' })
const statuses = ['DRAFT', 'SENT', 'QUOTED', 'ORDERED', 'CLOSED']
const parts = ref([])

const role = computed(() => store.user?.role)
const isOem = computed(() => ['OEM', 'ADMIN'].includes(role.value))
const isDealerSide = computed(() => ['DEALER_MANAGER', 'ADVISOR'].includes(role.value))
const canDealerAct = (row, list) => isDealerSide.value && list.includes(row.status)

const createVisible = ref(false)
const form = reactive({ title: '', expectDate: '', remark: '', lines: [] })
const drawer = ref(false)
const detail = ref({})
const lines = ref([])
const quoteMap = reactive({})
const oemRemark = ref('')

async function load() {
  loading.value = true
  try {
    const p = await procure.inquiry.page({ ...query, dealerCode: store.dealerCode })
    rows.value = p.records || []
    total.value = p.total || 0
  } finally {
    loading.value = false
  }
}

function pickPart(row, partNo) {
  const p = parts.value.find((x) => x.partNo === partNo)
  if (p && !row.targetPrice) row.targetPrice = p.costPrice
}

function openCreate() {
  form.title = ''
  form.expectDate = ''
  form.remark = ''
  form.lines = [{ partNo: '', qty: 1, targetPrice: null }]
  createVisible.value = true
}

async function doCreate() {
  const data = { ...form, dealerCode: store.dealerCode }
  await procure.inquiry.create(data)
  ElMessage.success('已创建')
  createVisible.value = false
  await load()
}

async function openDetail(row) {
  detail.value = row
  lines.value = (await procure.inquiryLines(row.id)) || []
  Object.keys(quoteMap).forEach((k) => delete quoteMap[k])
  lines.value.forEach((l) => {
    quoteMap[l.id] = { quotedPrice: l.quotedPrice || null, leadDays: l.leadDays || null }
  })
  oemRemark.value = ''
  drawer.value = true
}

async function doSend(row) {
  await procure.inquirySend(row.id)
  ElMessage.success('已发出')
  await load()
}

async function doClose(row) {
  await procure.inquiryClose(row.id)
  ElMessage.success('已关闭')
  await load()
}

async function doToOrder(row) {
  await procure.inquiryToOrder(row.id)
  ElMessage.success('已生成采购订单')
  await load()
}

async function doQuote() {
  await procure.inquiryQuote(detail.value.id, {
    oemRemark: oemRemark.value,
    lines: lines.value.map((l) => ({
      lineId: l.id,
      quotedPrice: quoteMap[l.id]?.quotedPrice,
      leadDays: quoteMap[l.id]?.leadDays
    }))
  })
  ElMessage.success('报价已提交')
  drawer.value = false
  await load()
}

onMounted(async () => {
  load()
  parts.value = (await partsApi.part.list({ size: 500 })) || []
})
window.addEventListener('dealer-change', load)
</script>
