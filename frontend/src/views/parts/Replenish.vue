<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="draft.partNo" placeholder="备件号 如P0001" style="width: 140px" />
        <el-input-number v-model="draft.qty" :min="1" style="width: 120px" />
        <el-button @click="addLine">加入明细</el-button>
        <el-button type="success" :disabled="!lines.length" @click="createDraft">生成补货单</el-button>
        <el-button type="warning" @click="fromShortage">按缺货预警生成</el-button>
        <el-button @click="syncAll">同步 OMS 状态</el-button>
        <el-button @click="queryInventory" :disabled="!lines.length">查 OMS 可售库存</el-button>
      </div>
      <el-table v-if="lines.length" :data="lines" size="small" border style="margin-top: 8px">
        <el-table-column prop="partNo" label="备件号" width="120" />
        <el-table-column prop="qty" label="数量" width="100" />
        <el-table-column prop="omsQty" label="OMS 可售" width="100" />
        <el-table-column label="" width="80">
          <template #default="{ $index }">
            <el-button link type="danger" @click="lines.splice($index, 1)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card">
      <el-table :data="rows" border stripe size="small" v-loading="loading">
        <el-table-column prop="replenishNo" label="补货单号" width="160" />
        <el-table-column prop="dealerCode" label="经销商" width="90" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" size="small">{{ statusText[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="90" />
        <el-table-column prop="omsOrderNo" label="OMS 单号" width="150" />
        <el-table-column prop="omsStatus" label="OMS 状态" width="100" />
        <el-table-column prop="trackingNo" label="运单号" width="140" />
        <el-table-column prop="lastError" label="最近错误" min-width="160" show-overflow-tooltip />
        <el-table-column prop="syncedAt" label="同步时间" width="160" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showLines(row)">明细</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="success" @click="push(row)">下单 OMS</el-button>
            <el-button v-if="['PUSHED', 'SHIPPED'].includes(row.status)" link @click="sync(row)">同步</el-button>
            <el-button v-if="['DRAFT', 'PUSHED'].includes(row.status)" link type="danger" @click="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 8px"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        :current-page="query.current"
        @current-change="(p) => ((query.current = p), load())"
      />
    </div>

    <el-dialog v-model="lineDialog" title="补货明细" width="520px">
      <el-table :data="detailLines" size="small" border>
        <el-table-column prop="partNo" label="备件号" width="110" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="qty" label="数量" width="80" />
        <el-table-column prop="price" label="单价" width="90" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { oms } from '../../api'
import { store } from '../../store'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 20 })
const draft = reactive({ partNo: '', qty: 10 })
const lines = ref([])
const lineDialog = ref(false)
const detailLines = ref([])

const statusText = { DRAFT: '草稿', PUSHING: '下单中', PUSHED: '已下单', SHIPPED: '已发货', RECEIVED: '已入库', CANCELLED: '已取消' }
const tagType = (s) => ({ DRAFT: 'info', PUSHING: 'info', PUSHED: 'primary', SHIPPED: 'warning', RECEIVED: 'success', CANCELLED: 'danger' })[s] || 'info'

async function load() {
  loading.value = true
  try {
    const page = await oms.replenish.page({ ...query, dealerCode: store.dealerCode })
    rows.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function addLine() {
  if (!draft.partNo) return
  lines.value.push({ partNo: draft.partNo.trim(), qty: draft.qty })
  draft.partNo = ''
}

async function createDraft() {
  await oms.draft({ dealerCode: store.dealerCode, items: lines.value })
  lines.value = []
  ElMessage.success('补货单已生成')
  await load()
}

async function fromShortage() {
  const r = await oms.fromShortage(store.dealerCode)
  ElMessage.success(r ? `已按缺货预警生成 ${r.replenishNo}` : '当前经销商无缺货备件')
  await load()
}

async function queryInventory() {
  const inv = (await oms.omsInventory(lines.value.map((l) => l.partNo).join(','))) || []
  const map = Object.fromEntries(inv.map((i) => [i.sku, i.qty]))
  lines.value = lines.value.map((l) => ({ ...l, omsQty: map[l.partNo] ?? 0 }))
}

async function push(row) {
  await oms.push(row.id)
  ElMessage.success('已下单 OMS')
  await load()
}

async function sync(row) {
  const r = await oms.sync(row.id)
  ElMessage.success(`OMS 状态 ${r.omsStatus || '-'} -> ${statusText[r.status] || r.status}`)
  await load()
}

async function syncAll() {
  const n = await oms.syncAll()
  ElMessage.success(`已同步 ${n} 张在途补货单`)
  await load()
}

async function cancel(row) {
  await ElMessageBox.confirm(`取消补货单 ${row.replenishNo}?`, '确认')
  await oms.cancel(row.id, '手工取消')
  ElMessage.success('已取消')
  await load()
}

async function showLines(row) {
  detailLines.value = (await oms.lines(row.id)) || []
  lineDialog.value = true
}

onMounted(load)
</script>
