<template>
  <div class="login-page">
    <div class="login-card">
      <h2>DMS 经销商管理系统</h2>
      <el-form @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" autofocus @blur="checkCaptcha">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
            @keyup.enter="submit"
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="captchaRequired">
          <el-input v-model="form.captchaCode" placeholder="验证码" size="large" @keyup.enter="submit">
            <template #append>
              <img
                v-if="captcha.image"
                :src="captcha.image"
                title="点击刷新"
                style="height: 32px; cursor: pointer"
                @click="refreshCaptcha"
              />
            </template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>
      <p class="hint">演示账号：admin / oem / d001mgr / d001sa / d001tech / d001fin / d002mgr，密码均为 123456</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/request'
import { setAuth } from '../store'

const route = useRoute()
const router = useRouter()
const form = reactive({ username: '', password: '', captchaCode: '' })
const loading = ref(false)
const captchaRequired = ref(false)
const captcha = reactive({ id: '', image: '' })

async function checkCaptcha() {
  try {
    const res = await http.get('/auth/captcha/required', { params: { username: form.username } })
    if (res.required) {
      captchaRequired.value = true
      if (!captcha.id) refreshCaptcha()
    }
  } catch (e) { /* 预检失败不阻塞登录 */ }
}

async function refreshCaptcha() {
  try {
    const res = await http.get('/auth/captcha')
    captcha.id = res.captchaId
    captcha.image = res.image
    form.captchaCode = ''
  } catch (e) { /* 忽略，提交时由后端兜底 */ }
}

checkCaptcha()

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await http.post('/auth/login', {
      username: form.username,
      password: form.password,
      captchaId: captcha.id,
      captchaCode: form.captchaCode
    })
    setAuth(res.token, res.user)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } catch (e) {
    if (!e.toasted) {
      ElMessage.error(e.message || '登录失败')
    }
    if (e.code === 4001) {
      captchaRequired.value = true
    }
    if (captchaRequired.value) {
      refreshCaptcha()
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #1f2d3d;
}
.login-card {
  width: 380px;
  padding: 40px 36px;
  background: #fff;
  border-radius: 8px;
}
h2 {
  text-align: center;
  margin: 0 0 28px;
}
.hint {
  margin: 16px 0 0;
  color: #909399;
  font-size: 12px;
  text-align: center;
}
</style>
