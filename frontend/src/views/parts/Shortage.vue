<template>
  <div class="page">
    <div class="card">
      <el-table :data="rows" border stripe size="small">
        <el-table-column prop="dealerCode" label="经销商" width="120" />
        <el-table-column prop="partNo" label="备件号" width="120" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="available" label="可用量" width="100" />
        <el-table-column prop="minStock" label="最低库存" width="100" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { parts } from '../../api'

const rows = ref([])

onMounted(async () => {
  rows.value = (await parts.shortage()) || []
})
</script>
