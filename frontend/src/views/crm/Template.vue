<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="编码/名称" style="width: 200px" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openEdit(null)">新建模板</el-button>
      </div>
      <el-table :data="rows" border stripe size="small" v-loading="loading">
        <el-table-column prop="code" label="编码" width="160" />
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column prop="channel" label="渠道" width="90" />
        <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该模板？" @confirm="del(row)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑模板' : '新建模板'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="编码"><el-input v-model="form.code" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="form.channel" style="width: 100%">
            <el-option label="短信" value="SMS" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="通用" value="ANY" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="支持 {{name}} {{plate}} {{date}} {{dealer}} {{mileage}} 占位符" />
        </el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { crm } from '../../api'

const query = reactive({ current: 1, size: 20, keyword: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const editVisible = ref(false)
const form = reactive({})

async function load() {
  loading.value = true
  try {
    const d = await crm.template.page(query)
    rows.value = d.records || d.list || []
    total.value = d.total || 0
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { channel: 'SMS', enabled: true })
  editVisible.value = true
}

async function save() {
  if (form.id) {
    await crm.template.update(form.id, form)
  } else {
    await crm.template.create(form)
  }
  editVisible.value = false
  load()
}

async function del(row) {
  await crm.template.remove(row.id)
  load()
}

onMounted(load)
</script>
