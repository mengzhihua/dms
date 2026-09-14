<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <el-icon><Van /></el-icon>
        <span>DMS 经销商管理</span>
      </div>
      <el-menu
        :default-active="route.path"
        :default-openeds="menus.filter((item) => item.children).map((item) => item.path)"
        background-color="#1f2d3d"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
      >
        <template v-for="menu in menus" :key="menu.path">
          <el-sub-menu v-if="menu.children" :index="menu.path">
            <template #title>
              <el-icon><component :is="menu.icon" /></el-icon>
              <span>{{ menu.name }}</span>
            </template>
            <el-menu-item
              v-for="child in menu.children"
              :key="child.path"
              :index="`${menu.path}/${child.path}`"
            >
              {{ child.name }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path">
            <el-icon><component :is="menu.icon" /></el-icon>
            <span>{{ menu.name }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="crumb in crumbs" :key="crumb">{{ crumb }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="header-right">
          <el-select
            :model-value="store.dealerCode"
            placeholder="选择经销商"
            clearable
            style="width: 220px"
            @change="onDealerChange"
          >
            <el-option
              v-for="d in dealers"
              :key="d.code"
              :label="`${d.code} ${d.name}`"
              :value="d.code"
            />
          </el-select>
          <span class="muted"><el-icon><User /></el-icon> admin</span>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { menus } from '../router'
import { network } from '../api'
import { store, setDealer } from '../store'

const route = useRoute()
const dealers = ref([])

function onDealerChange(code) {
  setDealer(code)
  window.dispatchEvent(new Event('dealer-change'))
}

onMounted(async () => {
  dealers.value = (await network.dealer.list({ size: 200 })) || []
})

const crumbs = computed(() => {
  const result = ['DMS']
  menus.forEach((menu) => {
    if (route.path === menu.path) {
      result.push(menu.name)
    }
    const child = menu.children?.find((item) => `${menu.path}/${item.path}` === route.path)
    if (child) {
      result.push(menu.name, child.name)
    }
  })
  if (route.name === '工单详情') {
    result.push('维修工单', '工单详情')
  }
  return result
})
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  overflow-y: auto;
  color: #fff;
  background: #1f2d3d;
}

.brand {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 16px;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.main {
  padding: 0;
  overflow: auto;
}
</style>
