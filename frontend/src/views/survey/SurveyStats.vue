<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="q.dealerCode" placeholder="经销商编码" style="width: 140px" />
        <el-input v-model="q.from" placeholder="起 yyyy-MM-dd" style="width: 150px" />
        <el-input v-model="q.to" placeholder="止 yyyy-MM-dd" style="width: 150px" />
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <el-row :gutter="12" v-if="stats">
        <el-col :span="6"><div class="stat"><div class="label">答卷数</div><div class="value">{{ stats.count }}</div></div></el-col>
        <el-col :span="6"><div class="stat"><div class="label">平均分(0-100)</div><div class="value">{{ stats.avgScore ?? '-' }}</div></div></el-col>
        <el-col :span="6"><div class="stat"><div class="label">NPS</div><div class="value" :style="{color: (stats.nps ?? 0) >= 0 ? '#67c23a' : '#f56c6c'}">{{ stats.nps ?? '-' }}</div></div></el-col>
      </el-row>
      <div v-if="stats" style="margin-top: 16px; max-width: 640px">
        <p class="muted">分数段分布</p>
        <div v-for="(n, k) in stats.distribution" :key="k" style="display: flex; align-items: center; gap: 8px; margin-bottom: 6px">
          <span style="width: 60px">{{ k }}</span>
          <el-progress :percentage="stats.count ? Math.round((n / stats.count) * 100) : 0" style="flex: 1" />
          <span style="width: 40px">{{ n }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { survey } from '../../api'
import { store } from '../../store'

const q = reactive({ dealerCode: store.dealerCode, from: '', to: '' })
const stats = ref(null)

async function load() {
  stats.value = await survey.stats(q)
}

load()
</script>
