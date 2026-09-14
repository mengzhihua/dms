<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="form.dealerCode" placeholder="经销商编码" style="width: 160px" />
        <el-input v-model="form.yearMonth" placeholder="月份 yyyy-MM" style="width: 140px" />
        <el-button type="primary" @click="generate">生成考核</el-button>
        <span class="muted">公式：销售达成30% + 服务达成30% + 满意度30% + 合规10%</span>
      </div>
    </div>
    <CrudPage ref="page" title="经销商考核" :api="network.assessment" :columns="columns" :extra-params="extra" />
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import CrudPage from '../../components/CrudPage.vue'
import { network } from '../../api'
import { store } from '../../store'

const page = ref()
const form = reactive({ dealerCode: store.dealerCode, yearMonth: new Date().toISOString().slice(0, 7) })
const extra = () => ({ dealerCode: store.dealerCode })

async function generate() {
  await network.generateAssessment(form)
  ElMessage.success('考核已生成')
}

const columns = [
  { prop: 'dealerCode', label: '经销商' },
  { prop: 'yearMonth', label: '月份' },
  { prop: 'salesScore', label: '销售得分' },
  { prop: 'serviceScore', label: '服务得分' },
  { prop: 'csiScore', label: '满意度得分' },
  { prop: 'complianceScore', label: '合规得分' },
  { prop: 'total', label: '总分' },
  { prop: 'grade', label: '等级' }
]
</script>
