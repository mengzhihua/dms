<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="用户名/姓名" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openForm()">新增用户</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" border stripe size="small">
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="姓名" width="140" />
        <el-table-column label="角色" width="140">
          <template #default="{ row }"><el-tag size="small">{{ roleNames[row.role] || row.role }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="dealerCode" label="经销商" width="110" />
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="150" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="remove(row)">
              <template #reference><el-button link type="danger" size="small">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="visible" :title="(form.id ? '编辑' : '新增') + '用户'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option v-for="r in roleOptions" :key="r" :label="roleNames[r]" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="经销商">
          <el-select v-model="form.dealerCode" clearable filterable placeholder="全网角色留空" style="width: 100%">
            <el-option v-for="d in dealers" :key="d.code" :label="`${d.code} ${d.name}`" :value="d.code" />
          </el-select>
        </el-form-item>
        <el-form-item :label="form.id ? '重置密码' : '密码'">
          <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? '留空不修改' : '初始密码'" />
        </el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
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
import { auth, network } from '../../api'

const roleNames = {
  ADMIN: '系统管理员',
  OEM: '厂家',
  DEALER_MANAGER: '经销商经理',
  ADVISOR: '服务顾问',
  TECHNICIAN: '技师',
  FINANCE: '财务'
}
const roleOptions = Object.keys(roleNames)

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const visible = ref(false)
const form = ref({})
const dealers = ref([])
const query = reactive({ current: 1, size: 20, keyword: '' })

async function load() {
  loading.value = true
  try {
    const p = await auth.user.page({ ...query })
    rows.value = p.records || []
    total.value = p.total || 0
  } finally {
    loading.value = false
  }
}

function openForm(row) {
  form.value = row ? { ...row, password: '' } : { enabled: true, role: 'ADVISOR' }
  visible.value = true
}

async function save() {
  if (form.value.id) {
    await auth.user.update(form.value.id, form.value)
  } else {
    await auth.user.create(form.value)
  }
  ElMessage.success('保存成功')
  visible.value = false
  await load()
}

async function remove(row) {
  await auth.user.remove(row.id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(async () => {
  dealers.value = (await network.dealer.list({ size: 200 })) || []
  await load()
})
</script>
