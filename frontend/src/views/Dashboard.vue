<template>
  <div class="page" v-loading="loading">
    <el-row :gutter="12">
      <el-col :span="4"><div class="stat"><div class="label">今日接车</div><div class="value">{{ d.todayCheckIns ?? 0 }}</div></div></el-col>
      <el-col :span="4"><div class="stat"><div class="label">维修中</div><div class="value">{{ d.inRepairCount ?? 0 }}</div></div></el-col>
      <el-col :span="4"><div class="stat"><div class="label">当月产值</div><div class="value">{{ d.monthRevenue ?? 0 }}</div></div></el-col>
      <el-col :span="4"><div class="stat"><div class="label">当月结算单</div><div class="value">{{ d.monthSettledOrders ?? 0 }}</div></div></el-col>
      <el-col :span="4"><div class="stat"><div class="label">平均维修时长(h)</div><div class="value">{{ d.avgRepairHours ?? 0 }}</div></div></el-col>
      <el-col :span="4"><div class="stat"><div class="label">技师利用率%</div><div class="value">{{ d.technicianUtilization ?? '-' }}</div></div></el-col>
    </el-row>
    <el-row :gutter="12" style="margin-top: 12px">
      <el-col :span="4"><div class="stat"><div class="label">备件缺货</div><div class="value" style="color:#f56c6c">{{ d.partsShortageCount ?? 0 }}</div></div></el-col>
      <el-col :span="4"><div class="stat"><div class="label">当月NPS</div><div class="value">{{ d.npsThisMonth ?? '-' }}</div></div></el-col>
      <el-col :span="4"><div class="stat"><div class="label">未结投诉</div><div class="value" style="color:#f56c6c">{{ d.openComplaints ?? 0 }}</div></div></el-col>
      <el-col :span="4"><div class="stat"><div class="label">当月已开发票</div><div class="value">{{ d.invoiceIssuedThisMonth ?? 0 }}</div></div></el-col>
      <el-col :span="4"><div class="stat"><div class="label">待填调研</div><div class="value">{{ d.pendingSurveys ?? 0 }}</div></div></el-col>
    </el-row>
    <el-row :gutter="12" style="margin-top: 12px">
      <el-col :span="12">
        <div class="card">
          <p><b>工单状态分布</b></p>
          <el-tag v-for="(n, s) in d.workOrderStatusCounts" :key="s" style="margin: 4px">
            {{ s }}: {{ n }}
          </el-tag>
          <p v-if="!d.workOrderStatusCounts || !Object.keys(d.workOrderStatusCounts).length" class="muted">暂无工单</p>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="card">
          <p><b>经销商当月产值排名</b></p>
          <el-table :data="d.dealerRanking || []" size="small" border>
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="dealerCode" label="编码" width="90" />
            <el-table-column prop="dealerName" label="经销商" min-width="160" />
            <el-table-column prop="monthRevenue" label="当月产值" width="120" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { dashboard } from '../api'
import { store } from '../store'

const d = ref({})
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    d.value = (await dashboard(store.dealerCode)) || {}
  } finally {
    loading.value = false
  }
}

onMounted(load)
window.addEventListener('dealer-change', load)
onUnmounted(() => window.removeEventListener('dealer-change', load))
</script>
