<template>
  <div class="page">
    <div class="card">
      <el-tabs v-model="type" @tab-change="load">
        <el-tab-pane v-for="(v, k) in types" :key="k" :label="v" :name="k" />
      </el-tabs>
      <div class="toolbar">
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" />
        <el-radio-group v-model="groupBy" @change="load">
          <el-radio-button value="day">按日</el-radio-button>
          <el-radio-button value="month">按月</el-radio-button>
          <el-radio-button value="dealer">按经销商</el-radio-button>
        </el-radio-group>
        <el-select v-if="networkWide" v-model="dealerCode" clearable placeholder="全部经销商" style="width: 160px" @change="load">
          <el-option v-for="d in dealers" :key="d.code" :label="d.name" :value="d.code" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="doExport('xlsx')">导出 Excel</el-button>
        <el-button @click="doExport('pdf')">导出 PDF</el-button>
      </div>

      <div v-for="sec in sections" :key="sec.title" style="margin-bottom: 24px">
        <h4 style="margin: 8px 0">{{ sec.title }}</h4>
        <el-table :data="sec.rows" border stripe size="small" v-loading="loading">
          <el-table-column v-for="(h, i) in sec.headers" :key="i" :label="h">
            <template #default="{ row }">{{ row[i] }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="summary && Object.keys(summary).length" class="card" style="background:#fafafa">
        <el-tag v-for="(v, k) in summary" :key="k" style="margin-right: 8px" type="info">{{ k }}：{{ v }}</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { network, report } from '../../api'
import { store } from '../../store'

const types = {
  workshop: '售后',
  sales: '销售',
  parts: '备件',
  satisfaction: '满意度',
  warranty: '保修',
  procure: '采购'
}
const type = ref('workshop')
const now = new Date()
const first = new Date(now.getFullYear(), now.getMonth(), 1).toLocaleDateString('sv')
const range = ref([first, now.toLocaleDateString('sv')])
const groupBy = ref('month')
const dealerCode = ref('')
const dealers = ref([])
const sections = ref([])
const summary = ref(null)
const loading = ref(false)
const networkWide = computed(() => ['ADMIN', 'OEM'].includes(store.user?.role))

async function load() {
  loading.value = true
  try {
    const d = await report.get(type.value, {
      from: range.value[0],
      to: range.value[1],
      dealerCode: dealerCode.value || undefined,
      groupBy: groupBy.value
    })
    sections.value = d.sections || []
    summary.value = d.summary || {}
  } finally {
    loading.value = false
  }
}

function doExport(format) {
  report.exportFile(type.value, {
    from: range.value[0],
    to: range.value[1],
    dealerCode: dealerCode.value || undefined,
    groupBy: groupBy.value
  }, format)
}

onMounted(async () => {
  if (networkWide.value) {
    dealers.value = (await network.dealer.list()) || []
  }
  load()
})
</script>
