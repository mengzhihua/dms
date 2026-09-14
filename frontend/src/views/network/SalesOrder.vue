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
        <el-table-column prop="color" label="颜色" width="90" />
        <el-table-column prop="price" label="售价" width="100" />
        <el-table-column prop="deposit" label="定金" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'NEW'" link type="primary" size="small" @click="act(row, 'allocate')">分配</el-button>
            <el-button v-if="row.status === 'ALLOCATED'" link type="warning" size="small" @click="act(row, 'invoiceOrder')">开票</el-button>
            <el-button v-if="row.status === 'ALLOCATED' || row.status === 'INVOICED'" link type="success" size="small" @click="act(row, 'deliverOrder')">交付</el-button>
            <el-button v-if="row.status !== 'DELIVERED' && row.status !== 'CANCELLED'" link type="danger" size="small" @click="act(row, 'cancelOrder')">取消</el-button>
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

onMounted(load)
window.addEventListener('dealer-change', load)
</script>
