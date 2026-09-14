<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="投诉单号/内容" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <el-table :data="rows" border stripe size="small" v-loading="loading">
        <el-table-column prop="complaintNo" label="投诉单号" min-width="140" />
        <el-table-column prop="dealerCode" label="经销商" width="90" />
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="level" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="row.level === 'HIGH' ? 'danger' : 'warning'" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column prop="handler" label="处理人" width="90" />
        <el-table-column prop="resolution" label="处理结果" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'RESOLVED' && row.status !== 'CLOSED'" link type="primary" size="small" @click="openHandle(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @change="load" />
      </div>
    </div>
    <el-dialog v-model="visible" title="处理投诉" width="520px">
      <el-form label-width="80px">
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option value="PROCESSING" label="处理中" />
            <el-option value="RESOLVED" label="已解决" />
            <el-option value="CLOSED" label="已关闭" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理人"><el-input v-model="form.handler" /></el-form-item>
        <el-form-item label="处理结果"><el-input v-model="form.resolution" type="textarea" /></el-form-item>
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
import { survey } from '../../api'
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
    const p = await survey.complaint.page({ ...query, dealerCode: store.dealerCode })
    rows.value = p.records || []
    total.value = p.total || 0
  } finally {
    loading.value = false
  }
}

function openHandle(row) {
  form.value = { status: 'PROCESSING', handler: '', resolution: '', id: row.id }
  visible.value = true
}

async function save() {
  await survey.handleComplaint(form.value.id, form.value)
  ElMessage.success('已处理')
  visible.value = false
  await load()
}

onMounted(load)
window.addEventListener('dealer-change', load)
</script>
