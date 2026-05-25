<template>
  <section class="profile-page">
    <div class="admin-page-heading">
      <div>
        <p class="eyebrow">Account Center</p>
        <h2>个人信息</h2>
        <p>维护 CampusHub 用户名、展示昵称和登录密码。用户名可用于登录，也会作为微信充值备注识别信息之一。</p>
      </div>
      <IdentityBadge v-if="auth.currentUser" :identities="auth.identityProfile.identities" compact />
    </div>

    <div class="profile-grid">
      <el-card class="premium-panel" shadow="never">
        <template #header>基础资料</template>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="校园邮箱">
            <el-input :model-value="auth.currentUser?.email || ''" disabled />
          </el-form-item>
          <el-form-item label="CampusHub 用户名">
            <el-input v-model="profileForm.username" placeholder="3-64 位字母、数字或下划线" />
          </el-form-item>
          <el-form-item label="展示昵称">
            <el-input v-model="profileForm.nickname" placeholder="在列表、评论和通知中展示" />
          </el-form-item>
          <el-button type="primary" :loading="profileLoading" @click="submitProfile">保存资料</el-button>
        </el-form>
      </el-card>

      <el-card class="premium-panel" shadow="never">
        <template #header>修改密码</template>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="当前密码">
            <el-input v-model="passwordForm.currentPassword" type="password" show-password autocomplete="current-password" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" placeholder="至少 8 位" />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
          </el-form-item>
          <el-button type="primary" :loading="passwordLoading" @click="submitPassword">更新密码</el-button>
        </el-form>
      </el-card>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref, watchEffect } from 'vue'
import { ElMessage } from 'element-plus'
import IdentityBadge from '@/components/common/IdentityBadge.vue'
import { changeCurrentUserPassword, updateCurrentUserProfile } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const profileLoading = ref(false)
const passwordLoading = ref(false)

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

async function submitProfile() {
  const username = profileForm.username.trim()
  const nickname = profileForm.nickname.trim()
  if (!/^[a-zA-Z0-9_]{3,64}$/.test(username)) {
    ElMessage.error('用户名只能包含 3-64 位字母、数字或下划线')
    return
  }
  if (!nickname) {
    ElMessage.error('请填写展示昵称')
    return
  }
  profileLoading.value = true
  try {
    const updated = await updateCurrentUserProfile({ username, nickname })
    auth.currentUser = { ...auth.currentUser!, username: updated.username, nickname: updated.nickname }
    ElMessage.success('个人信息已更新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    profileLoading.value = false
  }
}

async function submitPassword() {
  if (passwordForm.newPassword.length < 8) {
    ElMessage.error('新密码至少 8 位')
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
