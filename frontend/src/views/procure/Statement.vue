<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="对账单号" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button v-if="isOem" type="success" @click="genVisible = true">生成对账单</el-button>
      </div>
      <el-table :data="rows" border stripe size="small" v-loading="loading">
        <el-table-column prop="statementNo" label="对账单号" min-width="150" />
        <el-table-column prop="dealerCode" label="经销商" width="100" />
        <el-table-column prop="period" label="账期" width="90" />
        <el-table-column prop="orderCount" label="订单数" width="80" />
        <el-table-column prop="totalAmount" label="总金额" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column prop="paidAt" label="付款时间" min-width="150" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">明细</el-button>
            <el-button v-if="isOem && row.status === 'DRAFT'" link type="warning" size="small" @click="act(row, 'statementConfirm')">确认</el-button>
            <el-button v-if="isOem && row.status === 'CONFIRMED'" link type="success" size="small" @click="act(row, 'statementPay')">付款</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="genVisible" title="生成采购对账单" width="420px">
      <el-form label-width="80px">
        <el-form-item label="经销商" required>
          <el-select v-model="gen.dealerCode" placeholder="选择经销商" style="width:100%">
            <el-option v-for="d in dealers" :key="d.code" :label="`${d.code} ${d.name}`" :value="d.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="账期">
          <el-date-picker v-model="gen.period" type="month" value-format="YYYY-MM" placeholder="不限（全部已对账范围）" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="genVisible = false">取消</el-button>
        <el-button type="primary" @click="doGenerate">生成</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawer" :title="`对账单 ${detail.statementNo || ''}`" size="560px">
      <el-descriptions :column="2" border size="small" style="margin-bottom:12px">
        <el-descriptions-item label="经销商">{{ detail.dealerCode }}</el-descriptions-item>
        <el-descriptions-item label="账期">{{ detail.period }}</el-descriptions-item>
        <el-descriptions-item label="订单数">{{ detail.orderCount }}</el-descriptions-item>
        <el-descriptions-item label="总金额">{{ detail.totalAmount }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="orders" border size="small">
        <el-table-column prop="poNo" label="采购单号" min-width="140" />
        <el-table-column prop="oemOrderNo" label="厂家单号" width="120" />
        <el-table-column prop="receivedAmount" label="到货金额" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import { network, procure } from '../../api'
import { store } from '../../store'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 20, keyword: '' })
const dealers = ref([])
const isOem = computed(() => ['OEM', 'ADMIN'].includes(store.user?.role))

const genVisible = ref(false)
const gen = reactive({ dealerCode: '', period: '' })
const drawer = ref(false)
const detail = ref({})
const orders = ref([])

async function load() {
  loading.value = true
  try {
    const p = await procure.statement.page({ ...query })
    rows.value = p.records || []
    total.value = p.total || 0
  } finally {
    loading.value = false
  }
}

async function doGenerate() {
  await procure.statementGenerate({ dealerCode: gen.dealerCode, period: gen.period || null })
  ElMessage.success('已生成')
  genVisible.value = false
  await load()
}

async function openDetail(row) {
  detail.value = row
  orders.value = (await procure.statementOrders(row.id)) || []
  drawer.value = true
}

async function act(row, action) {
  await procure[action](row.id)
  ElMessage.success('操作成功')
  await load()
}

onMounted(async () => {
  load()
  dealers.value = (await network.dealer.list({ size: 200 })) || []
})
</script>
