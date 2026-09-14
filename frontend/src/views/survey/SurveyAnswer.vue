<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="surveyId" placeholder="调研ID" style="width: 140px" />
        <el-button type="primary" @click="load">加载答卷</el-button>
      </div>
      <template v-if="survey">
        <p>调研 {{ survey.surveyNo }} <el-tag size="small">{{ survey.status }}</el-tag></p>
        <div v-for="q in questions" :key="q.id" class="question">
          <p><b>{{ q.seq }}. {{ q.text }}</b></p>
          <el-slider v-if="q.type === 'SCORE'" v-model="answers[q.id]" :min="1" :max="10" show-input :disabled="answered" style="max-width: 480px" />
          <el-slider v-else-if="q.type === 'NPS'" v-model="answers[q.id]" :min="0" :max="10" show-input :disabled="answered" style="max-width: 480px" />
          <el-input v-else v-model="answers[q.id]" type="textarea" :disabled="answered" style="max-width: 480px" />
        </div>
        <el-button type="primary" :disabled="answered" @click="submit">提交答卷</el-button>
        <p v-if="answered" class="muted">已作答：总分 {{ survey.totalScore }}，NPS {{ survey.npsScore }}</p>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { survey as api } from '../../api'

const route = useRoute()
const surveyId = ref(route.query.id ? String(route.query.id) : '')
const survey = ref(null)
const questions = ref([])
const answers = reactive({})
const answered = computed(() => survey.value && survey.value.status === 'ANSWERED')

async function load() {
  survey.value = await api.survey.get(surveyId.value)
  const tpl = (await api.template.list({ size: 50 })) || []
  const t = tpl.find((x) => x.code === survey.value.templateCode)
  if (t) {
    questions.value = (await api.question.list({ size: 100, templateId: t.id })) || []
    questions.value.sort((a, b) => (a.seq || 0) - (b.seq || 0))
  }
  if (survey.value.status === 'ANSWERED') {
    const saved = (await api.surveyAnswers(surveyId.value)) || []
    saved.forEach((a) => {
      answers[a.questionId] = a.score != null ? Number(a.score) : a.text
    })
  }
}

async function submit() {
  const list = questions.value.map((q) => ({
    questionId: q.id,
    score: q.type === 'TEXT' ? undefined : answers[q.id],
    text: q.type === 'TEXT' ? answers[q.id] : undefined
  }))
  await api.submitAnswer(surveyId.value, list)
  ElMessage.success('提交成功')
  await load()
}

onMounted(() => {
  if (surveyId.value) {
    load()
  }
})
</script>

<style scoped>
.question {
  margin-bottom: 16px;
}
</style>
