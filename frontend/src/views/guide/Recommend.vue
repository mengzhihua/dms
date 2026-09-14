<template>
  <div class="page">
    <div class="card">
      <el-form :inline="true">
        <el-form-item label="车型">
          <el-input v-model="form.modelCode" placeholder="如 M001" style="width: 140px" />
        </el-form-item>
        <el-form-item label="故障码">
          <el-input v-model="dtcText" placeholder="逗号分隔 如 P0300,P0301" style="width: 220px" />
        </el-form-item>
        <el-form-item label="症状">
          <el-input v-model="form.symptom" placeholder="如 发动机抖动" style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="run">智能推荐</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div v-for="item in results" :key="item.guide.code" class="card" style="margin-top: 12px">
      <div style="display: flex; justify-content: space-between; align-items: center">
        <b>{{ item.guide.code }} {{ item.guide.title }}</b>
        <div>
          <el-tag type="warning">匹配度 {{ item.score }}</el-tag>
          <el-button link type="primary" @click="estimate(item.guide.code)">估价</el-button>
        </div>
      </div>
      <div class="muted" style="margin-top: 8px">
        工时：<span v-for="l in item.labors" :key="l.code">{{ l.name }}({{ l.standardHours }}h) </span>
      </div>
      <div class="muted" style="margin-top: 4px">
        备件：<span v-for="p in item.parts" :key="p.partNo">{{ p.name }}￥{{ p.salePrice }}(可用{{ p.available ?? '-' }}) </span>
      </div>
      <div class="muted" style="margin-top: 4px">诊断：{{ item.guide.diagnosisSteps }}</div>
    </div>
    <el-dialog v-model="estVisible" title="维修估价" width="600px">
      <template v-if="est">
        <p>工时费率：{{ est.laborRate }} 元/h × {{ est.totalHours }}h = <b>{{ est.laborAmount }}</b> 元</p>
        <p>备件金额：<b>{{ est.partsAmount }}</b> 元</p>
        <p>预估合计：<b style="color: #f56c6c">{{ est.totalAmount }}</b> 元</p>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { guide } from '../../api'
import { store } from '../../store'

const form = reactive({ modelCode: 'M001', symptom: '' })
const dtcText = ref('')
const results = ref([])
const loading = ref(false)
const est = ref(null)
const estVisible = ref(false)

async function run() {
  loading.value = true
  try {
    results.value = await guide.recommend({
      modelCode: form.modelCode,
      dtcCodes: dtcText.value ? dtcText.value.split(/[,，]/).map((s) => s.trim()) : [],
      symptom: form.symptom,
      dealerCode: store.dealerCode
    })
  } finally {
    loading.value = false
  }
}

async function estimate(code) {
  est.value = await guide.estimate({ guideCode: code, dealerCode: store.dealerCode })
  estVisible.value = true
}
</script>
