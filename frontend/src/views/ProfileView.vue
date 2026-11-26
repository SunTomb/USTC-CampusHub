<template>
  <section class="profile-page page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">我的书院 · 个人名片</p>
        <h2>个人信息与安全设置</h2>
        <p>修改你的蜗壳同路 CampusHub 登录用户名、校园展示昵称、联系方式与登录密码。</p>
      </div>
      <IdentityBadge v-if="auth.currentUser" :identities="auth.identityProfile.identities" compact />
    </div>

    <div class="profile-grid">
      <el-card class="premium-panel" shadow="never">
        <template #header>基础名片资料</template>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="已验证校园邮箱">
            <el-input :model-value="auth.currentUser?.email || ''" disabled />
          </el-form-item>
          <el-form-item label="蜗壳同路用户名">
            <el-input v-model="profileForm.username" placeholder="3-64 位字母、数字或下划线" />
          </el-form-item>
          <el-form-item label="校园展示昵称">
            <el-input v-model="profileForm.nickname" placeholder="在市集、跑腿、小店与评论中展示的称呼" />
          </el-form-item>
          <el-button type="primary" :loading="profileLoading" @click="submitProfile">保存个人资料</el-button>
        </el-form>
      </el-card>

      <el-card class="premium-panel" shadow="never">
        <template #header>联系方式</template>
        <el-form label-position="top" @submit.prevent="submitContacts">
          <el-form-item label="微信号（与 QQ 至少填一项）">
            <el-input v-model="contactForm.wechatContact" maxlength="120" placeholder="达成预约、购买意向或接单后用于联系" />
          </el-form-item>
          <el-form-item label="QQ 号（选填）">
            <el-input v-model="contactForm.qqContact" maxlength="60" />
          </el-form-item>
          <p>仅在接单、购买意向或小店预约后向对方展示</p>
          <el-button type="primary" native-type="submit" :loading="contactLoading">保存联系方式</el-button>
        </el-form>
      </el-card>

      <el-card class="premium-panel" shadow="never">
        <template #header>修改登录密码</template>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="当前密码">
            <el-input v-model="passwordForm.currentPassword" type="password" show-password autocomplete="current-password" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" placeholder="至少 8 位字符" />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
          </el-form-item>
          <el-button type="primary" :loading="passwordLoading" @click="submitPassword">更新登录密码</el-button>
        </el-form>
      </el-card>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref, watchEffect } from 'vue'
import { ElMessage } from 'element-plus'
import IdentityBadge from '@/components/common/IdentityBadge.vue'
import { changeCurrentUserPassword, updateCurrentUserContacts, updateCurrentUserProfile } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const profileLoading = ref(false)
const passwordLoading = ref(false)
const contactLoading = ref(false)
const contactForm = reactive({ wechatContact: '', qqContact: '' })

const profileForm = reactive({
  username: '',
  nickname: '',
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

watchEffect(() => {
  if (auth.currentUser) {
    profileForm.username = auth.currentUser.username
    profileForm.nickname = auth.currentUser.nickname
  }
})

watchEffect(() => {
  contactForm.wechatContact = auth.currentUser?.wechatContact ?? ''
  contactForm.qqContact = auth.currentUser?.qqContact ?? ''
})

async function submitContacts() {
  const userId = auth.currentUser?.id
  const token = auth.token
  if (!userId || contactLoading.value) return
  const wechatContact = contactForm.wechatContact.trim()
  const qqContact = contactForm.qqContact.trim()
  if (!wechatContact && !qqContact) {
    ElMessage.error('请至少填写微信或 QQ 联系方式')
    return
  }
  contactLoading.value = true
  try {
    const updated = await updateCurrentUserContacts({ wechatContact, qqContact })
    if (auth.token !== token || auth.currentUser?.id !== userId) return
    Object.assign(auth.currentUser, { wechatContact: updated.wechatContact, qqContact: updated.qqContact })
    ElMessage.success('联系方式已更新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '联系方式保存失败')
  } finally {
    contactLoading.value = false
  }
}

async function submitProfile() {
  const username = profileForm.username.trim()
  const nickname = profileForm.nickname.trim()
  if (!/^[a-zA-Z0-9_]{3,64}$/.test(username)) {
    ElMessage.error('用户名只能包含 3-64 位字母、数字或下划线')
    return
  }
  if (!nickname) {
    ElMessage.error('请填写校园展示昵称')
    return
  }
  profileLoading.value = true
  try {
    const updated = await updateCurrentUserProfile({ username, nickname })
    if (auth.currentUser) Object.assign(auth.currentUser, { username: updated.username, nickname: updated.nickname })
    ElMessage.success('个人名片已更新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    profileLoading.value = false
  }
}

async function submitPassword() {
  if (passwordForm.newPassword.length < 8) {
    ElMessage.error('新密码至少需要 8 位字符')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  passwordLoading.value = true
  try {
    await changeCurrentUserPassword({ currentPassword: passwordForm.currentPassword, newPassword: passwordForm.newPassword })
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    ElMessage.success('密码已更新，请牢记新密码')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '修改密码失败')
  } finally {
    passwordLoading.value = false
  }
}
</script>
