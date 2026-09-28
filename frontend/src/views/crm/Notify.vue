<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 120px" @change="load">
          <el-option label="待发送" value="PENDING" />
          <el-option label="已发送" value="SENT" />
          <el-option label="失败" value="FAILED" />
        </el-select>
        <el-select v-model="query.channel" clearable placeholder="渠道" style="width: 110px" @change="load">
          <el-option label="短信" value="SMS" />
          <el-option label="微信" value="WECHAT" />
        </el-select>
        <el-input v-model="query.keyword" clearable placeholder="接收人/模板/内容" style="width: 200px" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <el-table :data="rows" border stripe size="small" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="dealerCode" label="经销商" width="80" />
        <el-table-column prop="channel" label="渠道" width="80" />
        <el-table-column prop="receiver" label="接收人" width="120" />
        <el-table-column prop="templateCode" label="模板" width="140" />
        <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误" width="140" show-overflow-tooltip />
        <el-table-column prop="sentAt" label="发送时间" width="150" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button v-if="row.status === 'FAILED'" link type="warning" size="small" @click="retry(row)">重发</el-button>
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
import { crm } from '../../api'
import StatusTag from '../../components/StatusTag.vue'

const query = reactive({ current: 1, size: 20, keyword: '', status: '', channel: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const d = await crm.message.page({ ...query })
    rows.value = d.records || d.list || []
    total.value = d.total || 0
  } finally {
    loading.value = false
  }
}

async function retry(row) {
  const m = await crm.messageRetry(row.id)
  ElMessage.success(m.status === 'SENT' ? '重发成功' : `重发失败：${m.errorMsg || ''}`)
  load()
}

onMounted(load)
</script>
