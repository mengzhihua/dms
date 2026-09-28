<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 120px" @change="load">
          <el-option label="待处理" value="PENDING" />
          <el-option label="已完成" value="DONE" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-select v-model="query.type" clearable placeholder="类型" style="width: 140px" @change="load">
          <el-option v-for="(v, k) in typeMap" :key="k" :label="v" :value="k" />
        </el-select>
        <el-input v-model="query.keyword" clearable placeholder="单号/客户/电话/VIN" style="width: 220px" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <template v-if="dealerSide">
          <el-button type="success" @click="createVisible = true">新建任务</el-button>
          <el-button type="warning" :loading="genLoading" @click="generate">一键生成</el-button>
        </template>
      </div>
      <el-table :data="rows" border stripe size="small" v-loading="loading">
        <el-table-column prop="taskNo" label="任务号" min-width="140" />
        <el-table-column prop="dealerCode" label="经销商" width="80" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ typeMap[row.type] || row.type }}</template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" width="90" />
        <el-table-column prop="phone" label="电话" width="110" />
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="dueDate" label="截止日" width="100" />
        <el-table-column prop="assignee" label="负责人" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><StatusTag :value="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <template v-if="dealerSide && row.status === 'PENDING'">
              <el-button link type="success" size="small" @click="openComplete(row)">完成</el-button>
              <el-dropdown trigger="click" @command="(c) => notify(row, c)" style="margin: 0 8px">
                <el-button link type="warning" size="small">通知</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="SMS">短信</el-dropdown-item>
                    <el-dropdown-item command="WECHAT">微信</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button link type="info" size="small" @click="openAssign(row)">改派</el-button>
              <el-button link type="danger" size="small" @click="cancel(row)">取消</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="createVisible" title="新建跟进任务" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="(v, k) in typeMap" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户ID"><el-input-number v-model="form.customerId" :min="0" /></el-form-item>
        <el-form-item label="客户姓名"><el-input v-model="form.customerName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="车牌"><el-input v-model="form.plateNo" /></el-form-item>
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="截止日"><el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.assignee" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="completeVisible" title="完成任务" width="440px">
      <el-input v-model="result" type="textarea" :rows="3" placeholder="处理结果" />
      <template #footer>
        <el-button @click="completeVisible = false">取消</el-button>
        <el-button type="primary" @click="doComplete">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="改派任务" width="380px">
      <el-input v-model="assignee" placeholder="负责人姓名" />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="doAssign">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="任务详情" size="480px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="任务号">{{ detail.taskNo }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ typeMap[detail.type] || detail.type }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ detail.customerName }} {{ detail.phone }}</el-descriptions-item>
          <el-descriptions-item label="车辆">{{ detail.plateNo }} {{ detail.vin }}</el-descriptions-item>
          <el-descriptions-item label="内容">{{ detail.content }}</el-descriptions-item>
          <el-descriptions-item label="截止">{{ detail.dueDate }}</el-descriptions-item>
          <el-descriptions-item label="状态"><StatusTag :value="detail.status" /></el-descriptions-item>
          <el-descriptions-item label="结果">{{ detail.result }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin: 16px 0 8px">通知记录</h4>
        <el-table :data="messages" size="small" border>
          <el-table-column prop="channel" label="渠道" width="80" />
          <el-table-column prop="receiver" label="接收人" width="110" />
          <el-table-column prop="content" label="内容" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="80">
            <template #default="{ row }"><StatusTag :value="row.status" /></template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { crm } from '../../api'
import StatusTag from '../../components/StatusTag.vue'
import { store } from '../../store'

const dealerSide = computed(() => store.user?.role !== 'OEM')
const typeMap = {
  MAINTENANCE_REMIND: '保养提醒',
  SERVICE_FOLLOWUP: '售后回访',
  COMPLAINT_FOLLOWUP: '投诉跟进',
  SALES_FOLLOWUP: '销售回访',
  BIRTHDAY: '生日关怀',
  RENEWAL: '续保提醒',
  MANUAL: '手工任务'
}
const query = reactive({ current: 1, size: 20, keyword: '', status: '', type: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const genLoading = ref(false)
const createVisible = ref(false)
const saving = ref(false)
const form = reactive({ type: 'MANUAL' })
const detail = ref(null)
const detailVisible = ref(false)
const messages = ref([])
const completeVisible = ref(false)
const result = ref('')
const assignVisible = ref(false)
const assignee = ref('')
const current = ref(null)

async function load() {
  loading.value = true
  try {
    const d = await crm.task.page({ ...query, dealerCode: store.dealerCode })
    rows.value = d.records || d.list || []
    total.value = d.total || 0
  } finally {
    loading.value = false
  }
}

async function generate() {
  genLoading.value = true
  try {
    const n = await crm.taskGenerate(store.user?.role === 'ADMIN' || store.user?.role === 'OEM' ? store.dealerCode : null)
    ElMessage.success(`已生成 ${n} 条任务`)
    load()
  } finally {
    genLoading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await crm.task.create({ ...form, dealerCode: store.dealerCode })
    createVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
  crm.taskMessages(row.id).then((m) => (messages.value = m || []))
}

function openComplete(row) {
  current.value = row
  result.value = ''
  completeVisible.value = true
}

async function doComplete() {
  await crm.taskComplete(current.value.id, result.value)
  completeVisible.value = false
  load()
}

function openAssign(row) {
  current.value = row
  assignee.value = row.assignee || ''
  assignVisible.value = true
}

async function doAssign() {
  await crm.taskAssign(current.value.id, assignee.value)
  assignVisible.value = false
  load()
}

async function cancel(row) {
  await crm.taskCancel(row.id)
  load()
}

async function notify(row, channel) {
  const m = await crm.taskNotify(row.id, channel)
  ElMessage.success(`通知${m.status === 'SENT' ? '已发送' : '发送失败'}`)
}

onMounted(load)
</script>
