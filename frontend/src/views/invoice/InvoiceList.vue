<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="申请号/抬头/发票号码" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openCreate">手工开票申请</el-button>
      </div>
      <el-table :data="rows" border stripe size="small" v-loading="loading">
        <el-table-column prop="invoiceNo" label="申请号" min-width="140" />
        <el-table-column prop="orderId" label="工单" width="80" />
        <el-table-column prop="invoiceType" label="类型" width="100" />
        <el-table-column prop="buyerName" label="抬头" min-width="140" show-overflow-tooltip />
        <el-table-column prop="amount" label="含税金额" width="100" />
        <el-table-column prop="taxAmount" label="税额" width="90" />
        <el-table-column prop="taxInvoiceNumber" label="发票号码" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DRAFT' || row.status === 'FAILED'" link type="primary" size="small" @click="issue(row)">开具</el-button>
            <el-button v-if="row.status === 'ISSUED'" link type="danger" size="small" @click="redFlush(row)">红冲</el-button>
            <el-button link size="small" @click="preview(row)">预览</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="createVisible" title="手工开票申请" width="560px">
      <el-form label-width="100px">
        <el-form-item label="经销商"><el-input v-model="createForm.dealerCode" /></el-form-item>
        <el-form-item label="发票类型">
          <el-select v-model="createForm.invoiceType" style="width: 100%">
            <el-option value="NORMAL" label="增值税普通发票" />
            <el-option value="SPECIAL" label="增值税专用发票" />
            <el-option value="ELECTRONIC" label="电子普通发票" />
          </el-select>
        </el-form-item>
        <el-form-item label="抬头"><el-input v-model="createForm.buyerName" /></el-form-item>
        <el-form-item label="税号"><el-input v-model="createForm.buyerTaxNo" /></el-form-item>
        <el-form-item label="含税金额"><el-input-number v-model="createForm.amount" :min="0" :precision="2" style="width: 200px" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="开票报文预览" width="720px">
      <pre class="preview">{{ previewText }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '../../components/StatusTag.vue'
import { invoice } from '../../api'
import { store } from '../../store'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const createVisible = ref(false)
const previewVisible = ref(false)
const previewText = ref('')
const createForm = ref({})
const query = reactive({ current: 1, size: 20, keyword: '' })
const route = useRoute()
const focusId = route.query.id

async function load() {
  loading.value = true
  try {
    if (focusId) {
      const inv = await invoice.invoice.get(focusId)
      rows.value = inv ? [inv] : []
      total.value = rows.value.length
    } else {
      const p = await invoice.invoice.page({ ...query, dealerCode: store.dealerCode })
      rows.value = p.records || []
      total.value = p.total || 0
    }
  } finally {
    loading.value = false
  }
}

function openCreate() {
  createForm.value = { dealerCode: store.dealerCode, invoiceType: 'ELECTRONIC' }
  createVisible.value = true
}

async function create() {
  await invoice.invoice.create(createForm.value)
  ElMessage.success('已创建')
  createVisible.value = false
  await load()
}

async function issue(row) {
  const r = await invoice.issue(row.id)
  if (r.status === 'ISSUED') {
    ElMessage.success(`已开具 发票号码 ${r.taxInvoiceNumber}`)
  } else {
    ElMessage.warning(`开票失败：${r.errorMsg || r.status}`)
  }
  await load()
}

async function redFlush(row) {
  await ElMessageBox.confirm('确认对该发票红冲？将生成负数红字发票。', '红冲确认')
  await invoice.redFlush(row.id)
  ElMessage.success('红冲完成')
  await load()
}

async function preview(row) {
  const p = await invoice.preview(row.id)
  previewText.value = JSON.stringify(p, null, 2)
  previewVisible.value = true
}

onMounted(load)
window.addEventListener('dealer-change', load)
</script>

<style scoped>
.preview {
  max-height: 480px;
  overflow: auto;
  background: #f7f8fa;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
}
</style>
