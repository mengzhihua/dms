<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="索赔单号" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <el-table :data="rows" border stripe size="small" v-loading="loading">
        <el-table-column prop="claimNo" label="索赔单号" min-width="140" />
        <el-table-column prop="orderId" label="工单ID" width="90" />
        <el-table-column prop="dealerCode" label="经销商" width="100" />
        <el-table-column prop="amount" label="索赔金额" width="110" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button v-if="row.status === 'SUBMITTED'" link type="success" size="small" @click="act(row, 'claimApprove')">审核通过</el-button>
            <el-button v-if="row.status === 'SUBMITTED'" link type="danger" size="small" @click="act(row, 'claimReject')">拒绝</el-button>
            <el-button v-if="row.status === 'APPROVED'" link type="primary" size="small" @click="act(row, 'claimPay')">付款</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import { workshop } from '../../api'
import { store } from '../../store'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 20, keyword: '' })

async function load() {
  loading.value = true
  try {
    const p = await workshop.claim.page({ ...query, dealerCode: store.dealerCode })
    rows.value = p.records || []
    total.value = p.total || 0
  } finally {
    loading.value = false
  }
}

async function act(row, action) {
  await workshop[action](row.id)
  ElMessage.success('操作成功')
  await load()
}

onMounted(load)
window.addEventListener('dealer-change', load)
</script>
