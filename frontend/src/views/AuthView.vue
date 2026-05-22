<template>
  <section class="auth-page">
    <div class="auth-copy">
      <p class="eyebrow">CampusHub 账号</p>
      <h2>用校园邮箱进入校集工作台</h2>
      <p>注册强制校验 edu.cn 邮箱，验证码仅用于建立学生身份；演示账号仍可直接登录。</p>
      <ul>
        <li>统一浏览二手商品、悬赏任务和技能店铺</li>
        <li>钱包、审核、举报记录集中管理</li>
        <li>生产环境可接入 Brevo SMTP 发送验证码</li>
      </ul>
    </div>

    <el-card class="auth-panel" shadow="never">
      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form label-position="top" @submit.prevent>
            <el-form-item label="用户名">
              <el-input v-model="loginForm.username" placeholder="alice" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="本地演示密码" />
            </el-form-item>
            <el-button type="primary" class="wide" :loading="loading" @click="handleLogin">登录</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="邮箱注册" name="register">
          <el-form label-position="top" @submit.prevent>
            <el-form-item label="校园邮箱">
              <div class="inline-action">
                <el-input v-model="registerForm.email" placeholder="student@mail.ustc.edu.cn" />
                <el-button :loading="codeLoading" @click="handleSendCode">发送验证码</el-button>
              </div>
            </el-form-item>
            <el-form-item label="验证码">
              <el-input v-model="registerForm.emailCode" maxlength="6" placeholder="6 位数字" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="至少 8 位" />
            </el-form-item>
            <p class="hint">微信和 QQ 至少填写一种。联系方式只在交易、预约或任务达成后按规则展示，不会直接公开在列表页。</p>
            <el-form-item label="微信号（选填）">
              <el-input v-model="registerForm.wechatContact" placeholder="用于匹配后的校园联系" />
            </el-form-item>
            <el-form-item label="QQ（选填）">
              <el-input v-model="registerForm.qqContact" placeholder="微信或 QQ 至少填写一个" />
            </el-form-item>
            <el-button type="primary" class="wide" :loading="loading" @click="handleRegister">注册</el-button>
            <p class="hint" v-if="codeHint">{{ codeHint }}</p>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { register, sendRegisterCode } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const activeTab = ref('login')
const loading = ref(false)
const codeLoading = ref(false)
const codeHint = ref('')

const loginForm = reactive({
  username: 'alice',
  password: '',
})

const registerForm = reactive({
  email: '',
  password: '',
  emailCode: '',
  wechatContact: '',
  qqContact: '',
})

async function handleLogin() {
  loading.value = true
  try {
    await auth.login(loginForm.username, loginForm.password)
    ElMessage.success('登录成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleSendCode() {
  codeLoading.value = true
  try {
    const result = await sendRegisterCode(registerForm.email)
    codeHint.value = `验证码已发送至 ${result.email}，${result.ttlMinutes} 分钟内有效。`
    ElMessage.success('验证码已发送')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发送失败')
  } finally {
    codeLoading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.wechatContact.trim() && !registerForm.qqContact.trim()) {
    ElMessage.error('请至少填写微信或 QQ 联系方式')
    return
  }
  loading.value = true
  try {
    const result = await register(registerForm)
    ElMessage.success(`注册成功：${result.username}`)
    activeTab.value = 'login'
    loginForm.username = result.username
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '注册失败')
  } finally {
    loading.value = false
  }
}
</script>
