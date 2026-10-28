<template>
  <section class="auth-page">
    <div class="auth-copy">
      <p class="eyebrow">瀚海校集 · 实名学籍准入</p>
      <h2>凭科大邮箱，走进同窗互助的校园集市</h2>
      <p>支持使用 CampusHub 用户名或校园邮箱直接登录；首次加入需验证 @mail.ustc.edu.cn / @ustc.edu.cn 邮箱，并填写至少一种校内常用联络方式。</p>
      <ul class="auth-feature-list">
        <li>
          <strong>一处连通全校生活</strong>
          <span>跑腿代取、闲置流转、技能小店与大创组队一站汇聚，告别刷屏群消息。</span>
        </li>
        <li>
          <strong>隐私默认受保护</strong>
          <span>你的微信号或 QQ 不会公开在列表页，仅在接单、预约或提交购买意向后向交易对方开放。</span>
        </li>
        <li>
          <strong>实名校友信用背书</strong>
          <span>所有成员均经校园邮箱核验，每一次守信交接都会累积为你的校内信用誉分。</span>
        </li>
      </ul>
    </div>

    <el-card class="auth-panel premium-panel" shadow="never">
      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="账号登录" name="login">
          <el-form label-position="top" @submit.prevent>
            <el-form-item label="用户名 / 校园邮箱">
              <el-input v-model="loginForm.username" placeholder="输入用户名或 student@mail.ustc.edu.cn" />
            </el-form-item>
            <el-form-item label="登录密码">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-button type="primary" class="wide" size="large" :loading="loading" @click="handleLogin">进入校集</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="校园邮箱注册" name="register">
          <el-form label-position="top" @submit.prevent>
            <el-form-item label="科大校园邮箱">
              <div class="inline-action">
                <el-input v-model="registerForm.email" placeholder="student@mail.ustc.edu.cn" />
                <el-button :loading="codeLoading" @click="handleSendCode">获取验证码</el-button>
              </div>
            </el-form-item>
            <el-form-item label="CampusHub 用户名">
              <el-input v-model="registerForm.username" placeholder="3-64 位字母、数字或下划线（可用于登录）" />
            </el-form-item>
            <el-form-item label="邮箱验证码">
              <el-input v-model="registerForm.emailCode" maxlength="6" placeholder="查收邮件中的 6 位数字验证码" />
            </el-form-item>
            <el-form-item label="设置登录密码">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="至少 8 位字符" />
            </el-form-item>
            <p class="hint">微信号与 QQ 至少填写一项，仅在达成接单、预约或购买意向后向对方展示。</p>
            <el-form-item label="微信号（推荐）">
              <el-input v-model="registerForm.wechatContact" placeholder="方便校内当面交接联系" />
            </el-form-item>
            <el-form-item label="QQ 号（选填）">
              <el-input v-model="registerForm.qqContact" placeholder="微信号与 QQ 至少填写一项即可" />
            </el-form-item>
            <el-button type="primary" class="wide" size="large" :loading="loading" @click="handleRegister">完成验证并加入</el-button>
            <p class="hint" v-if="codeHint">{{ codeHint }}</p>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, sendRegisterCode } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
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
  username: '',
  password: '',
  emailCode: '',
  wechatContact: '',
  qqContact: '',
})

function normalizedRedirect() {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) return redirect
  return '/'
}

async function handleLogin() {
  loading.value = true
  try {
    await auth.login(loginForm.username, loginForm.password)
    ElMessage.success('欢迎回到校集')
    await router.push(normalizedRedirect())
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleSendCode() {
  if (!registerForm.email.trim()) {
    ElMessage.warning('请先填写科大校园邮箱')
    return
  }
  codeLoading.value = true
  try {
    const result = await sendRegisterCode(registerForm.email)
    codeHint.value = `验证码已发送至邮箱，有效期 ${result.ttlMinutes} 分钟，${result.resendSeconds} 秒后可重新获取。`
    ElMessage.success('验证码已发送，请查收邮件')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '验证码发送失败')
  } finally {
    codeLoading.value = false
  }
}

async function handleRegister() {
  if (!/^[a-zA-Z0-9_]{3,64}$/.test(registerForm.username.trim())) {
    ElMessage.error('用户名只能包含 3-64 位字母、数字或下划线')
    return
  }
  if (!registerForm.wechatContact.trim() && !registerForm.qqContact.trim()) {
    ElMessage.error('请至少填写微信号或 QQ 联系方式中的一项')
    return
  }
  loading.value = true
  try {
    const result = await register(registerForm)
    ElMessage.success(`注册成功，欢迎你：${result.username}`)
    activeTab.value = 'login'
    loginForm.username = result.username
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '注册失败')
  } finally {
    loading.value = false
  }
}
</script>
