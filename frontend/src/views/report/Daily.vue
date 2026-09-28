<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" placeholder="日期" />
        <el-select v-if="networkWide" v-model="dealerCode" clearable placeholder="全部经销商" style="width: 160px" @change="load">
          <el-option v-for="d in dealers" :key="d.code" :label="d.name" :value="d.code" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button v-if="canGenerate" type="success" @click="generate">生成日报</el-button>
        <el-button @click="doExport('xlsx')">导出 Excel</el-button>
        <el-button @click="doExport('pdf')">导出 PDF</el-button>
      </div>

      <div v-if="today" style="margin-bottom: 16px">
        <el-row :gutter="12">
          <el-col :span="3" v-for="(v, k) in kpis" :key="k">
            <el-card shadow="hover"><div style="font-size:12px;color:#999">{{ k }}</div><div style="font-size:20px">{{ v }}</div></el-card>
          </el-col>
        </el-row>
      </div>

      <h4 style="margin: 8px 0">历史日报</h4>
      <el-table :data="rows" border stripe size="small" v-loading="loading">
        <el-table-column prop="reportDate" label="日期" width="110" />
        <el-table-column prop="dealerName" label="经销商" width="140" />
        <el-table-column prop="checkIns" label="进厂" width="70" />
        <el-table-column prop="delivered" label="交车" width="70" />
        <el-table-column prop="settledOrders" label="结算单" width="70" />
        <el-table-column prop="revenue" label="产值" width="100" />
        <el-table-column prop="salesDelivered" label="销售交车" width="80" />
        <el-table-column prop="salesAmount" label="销售额" width="100" />
        <el-table-column prop="partsOut" label="出库" width="70" />
        <el-table-column prop="surveys" label="答卷" width="70" />
        <el-table-column prop="nps" label="NPS" width="70" />
        <el-table-column prop="claims" label="索赔" width="70" />
        <el-table-column prop="pendingTasks" label="待办" width="70" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { network, report } from '../../api'
import { store } from '../../store'

const date = ref(new Date().toLocaleDateString('sv'))
const dealerCode = ref('')
const dealers = ref([])
const rows = ref([])
const today = ref(null)
const loading = ref(false)
const networkWide = computed(() => ['ADMIN', 'OEM'].includes(store.user?.role))
const canGenerate = computed(() => store.user?.role !== 'TECHNICIAN' && store.user?.role !== 'FINANCE')

const kpis = computed(() => {
  const r = today.value
  if (!r) return {}
  return {
    进厂: r.checkIns, 交车: r.delivered, 结算单: r.settledOrders,
    产值: r.revenue, 销售交车: r.salesDelivered, 销售额: r.salesAmount,
    出库: r.partsOut, 待办: r.pendingTasks
  }
})

async function load() {
  loading.value = true
  try {
    const d = await report.dailyPage({
      from: date.value,
      to: date.value,
      dealerCode: dealerCode.value || undefined
    })
    rows.value = d || []
    today.value = rows.value.find((x) => x.reportDate === date.value) || rows.value[0] || null
  } finally {
    loading.value = false
  }
}

async function generate() {
  const list = await report.dailyGenerate({
    date: date.value,
    dealerCode: dealerCode.value || undefined
  })
  ElMessage.success(`已生成 ${list.length} 条日报`)
  load()
}

function doExport(format) {
  report.exportFile('daily', { date: date.value, dealerCode: dealerCode.value || undefined }, format)
}

onMounted(async () => {
  if (networkWide.value) {
    dealers.value = (await network.dealer.list()) || []
  }
  load()
})
</script>
