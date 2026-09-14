<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="achForm.dealerCode" placeholder="经销商编码" style="width: 160px" />
        <el-input v-model="achForm.yearMonth" placeholder="月份 yyyy-MM" style="width: 140px" />
        <el-button type="primary" @click="loadAchievement">查询达成</el-button>
      </div>
      <el-descriptions v-if="achievement" :column="4" border title="目标达成">
        <el-descriptions-item label="整车目标/实际">
          {{ achievement.target?.salesTarget }} / {{ achievement.salesActual }}
          ({{ achievement.salesRate ?? '-' }}%)
        </el-descriptions-item>
        <el-descriptions-item label="工单目标/实际">
          {{ achievement.target?.serviceTarget }} / {{ achievement.serviceActual }}
          ({{ achievement.serviceRate ?? '-' }}%)
        </el-descriptions-item>
        <el-descriptions-item label="产值目标/实际">
          {{ achievement.target?.revenueTarget }} / {{ achievement.revenueActual }}
          ({{ achievement.revenueRate ?? '-' }}%)
        </el-descriptions-item>
      </el-descriptions>
    </div>
    <CrudPage title="经销商目标" :api="network.target" :columns="columns" :extra-params="extra" />
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import CrudPage from '../../components/CrudPage.vue'
import { network } from '../../api'
import { store } from '../../store'

const achForm = reactive({ dealerCode: store.dealerCode, yearMonth: new Date().toISOString().slice(0, 7) })
const achievement = ref(null)
const extra = () => ({ dealerCode: store.dealerCode })

async function loadAchievement() {
  achievement.value = await network.achievement(achForm)
}

const columns = [
  { prop: 'dealerCode', label: '经销商' },
  { prop: 'yearMonth', label: '月份' },
  { prop: 'salesTarget', label: '整车目标(台)', type: 'number', precision: 0 },
  { prop: 'serviceTarget', label: '工单目标', type: 'number', precision: 0 },
  { prop: 'revenueTarget', label: '产值目标', type: 'number', precision: 2 }
]
</script>
