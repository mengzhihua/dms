<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="关键词" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button v-if="!readonly" type="success" @click="openForm()">新增{{ title }}</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" border stripe size="small">
        <el-table-column
          v-for="column in tableColumns"
          :key="column.prop"
          :prop="column.prop"
          :label="column.label"
          :width="column.width"
          :min-width="column.minWidth || 100"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <StatusTag v-if="column.type === 'status'" :value="row[column.prop]" />
            <el-tag v-else-if="column.type === 'bool'" :type="row[column.prop] ? 'success' : 'info'">
              {{ row[column.prop] ? '是' : '否' }}
            </el-tag>
            <span v-else>{{ row[column.prop] }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!readonly" label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="remove(row)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          @change="load"
        />
      </div>
    </div>

    <el-dialog v-model="visible" :title="(form.id ? '编辑' : '新增') + title" width="720px">
      <el-form ref="formRef" :model="form" label-width="110px">
        <el-row :gutter="12">
          <el-col v-for="column in columns" :key="column.prop" :span="column.span || 12">
            <el-form-item :label="column.label">
              <el-switch
                v-if="column.type === 'bool'"
                v-model="form[column.prop]"
              />
              <el-input-number
                v-else-if="column.type === 'number'"
                v-model="form[column.prop]"
                :min="column.min ?? 0"
                :precision="column.precision ?? 3"
                style="width: 100%"
              />
              <el-select
                v-else-if="column.type === 'select'"
                v-model="form[column.prop]"
                clearable
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="option in column.options"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
              <el-input
                v-else
                v-model="form[column.prop]"
                :type="column.type === 'textarea' ? 'textarea' : 'text'"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from './StatusTag.vue'

const props = defineProps({
  title: { type: String, required: true },
  api: { type: Object, required: true },
  columns: { type: Array, required: true },
  extraParams: { type: Function, default: () => ({}) },
  readonly: { type: Boolean, default: false }
})

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const form = ref({})
const formRef = ref()
const query = reactive({ current: 1, size: 20, keyword: '' })
const tableColumns = props.columns.filter((column) => !column.hideInTable)

async function load() {
  loading.value = true
  try {
    const page = await props.api.page({ ...query, ...props.extraParams() })
    rows.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}

function openForm(row) {
  form.value = row ? { ...row } : {}
  visible.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.value.id) {
      await props.api.update(form.value.id, form.value)
    } else {
      await props.api.create(form.value)
    }
    ElMessage.success('保存成功')
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await props.api.remove(row.id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(load)

defineExpose({ load })
</script>
