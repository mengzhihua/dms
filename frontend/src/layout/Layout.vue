<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <el-icon><Van /></el-icon>
        <span>DMS 经销商管理</span>
      </div>
      <el-menu
        :default-active="route.path"
        :default-openeds="visibleMenus.filter((item) => item.children).map((item) => item.path)"
        background-color="#1f2d3d"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
      >
        <template v-for="menu in visibleMenus" :key="menu.path">
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
            :disabled="!networkWide"
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
          <el-tag size="small" :type="roleTagType">{{ roleName }}</el-tag>
          <el-dropdown @command="onUserCmd">
            <span class="user-name">
              <el-icon><User /></el-icon> {{ store.user?.realName || store.user?.username }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <el-dialog v-model="pwdVisible" title="修改密码" width="400px">
      <el-form label-width="80px">
        <el-form-item label="原密码"><el-input v-model="pwd.old" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwd.new" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="changePwd">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { menus } from '../router'
import { network, auth } from '../api'
import { store, setDealer, logout } from '../store'

const route = useRoute()
const router = useRouter()
const dealers = ref([])
const pwdVisible = ref(false)
const pwd = reactive({ old: '', new: '' })

const roleName = computed(() => {
  const names = {
    ADMIN: '系统管理员',
    OEM: '厂家',
    DEALER_MANAGER: '经销商经理',
    ADVISOR: '服务顾问',
    TECHNICIAN: '技师',
    FINANCE: '财务'
  }
  return names[store.user?.role] || store.user?.role || ''
})
const roleTagType = computed(() => (networkWide.value ? 'warning' : 'info'))
const networkWide = computed(() => ['ADMIN', 'OEM'].includes(store.user?.role))

const visibleMenus = computed(() => {
  const role = store.user?.role
  if (!role) return []
  return menus.filter((m) => !m.roles || m.roles.includes(role))
})

function onDealerChange(code) {
  setDealer(code)
  window.dispatchEvent(new Event('dealer-change'))
}

async function onUserCmd(cmd) {
  if (cmd === 'logout') {
    try {
      await auth.logout()
    } catch (e) {
      // 忽略登出失败
    }
    logout()
    router.push('/login')
  } else if (cmd === 'password') {
    pwd.old = ''
    pwd.new = ''
    pwdVisible.value = true
  }
}

async function changePwd() {
  if (!pwd.old || !pwd.new) {
    ElMessage.warning('请输入原密码和新密码')
    return
  }
  await auth.changePassword(pwd.old, pwd.new)
  ElMessage.success('密码已修改')
  pwdVisible.value = false
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

.user-name {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.main {
  padding: 0;
  overflow: auto;
}
</style>
