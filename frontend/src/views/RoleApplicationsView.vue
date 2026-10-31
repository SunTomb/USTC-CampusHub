<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">我的书院 · 身份协议与权限</p>
        <h2>身份解锁中心</h2>
        <p>顺路跑腿员、闲置发布者和技能店主理人先阅读对应协议；前两类同意后自动开通，店主理人由站务核验。</p>
      </div>
    </div>

    <div class="premium-panel role-status-panel">
      <p class="eyebrow">当前已点亮身份</p>
      <h3>蜗壳 CampusHub 身份</h3>
      <IdentityBadge :identities="identityProfile.identities" />
      <p>历史身份继续有效，不会因为资金功能暂未开放而被自动撤销。</p>
    </div>

    <div class="role-grid">
      <el-card v-for="role in roleCards" :key="role.roleType" class="role-card spotlight-surface" shadow="never">
        <template #header>
          <div class="panel-topline">
            <strong>{{ role.title }}</strong>
            <el-tag :type="role.review ? 'warning' : 'success'" effect="plain">
              {{ role.agreementRequired ? (role.review ? '协议后站务核验' : '同意协议后开通') : '总站务审核' }}
            </el-tag>
          </div>
        </template>
        <p>{{ role.description }}</p>
        <div class="deposit-amount mono-num">免保证金</div>
        <el-input v-model="notes[role.roleType]" type="textarea" :rows="3" placeholder="填写简短的申请说明或擅长领域（选填）" />
        <el-button
          type="primary"
          :loading="loadingRole === role.roleType || agreementLoading"
          :disabled="loadingRole !== null || agreementLoading || role.unlocked || role.application?.reviewStatus === 'PENDING_REVIEW'"
          @click="beginApplication(role.roleType)"
        >
          {{ role.unlocked ? '已开通' : role.application?.reviewStatus === 'PENDING_REVIEW' ? '等待站务核验' : role.agreementRequired ? '阅读协议并申请' : '提交管理员申请' }}
        </el-button>
        <div v-if="role.application" class="payment-meta">
          <span>申请状态：{{ formatStatus(role.application.reviewStatus) }}</span>
          <span v-if="role.agreementRequired">协议状态：{{ agreementStatusText(role.application.agreementStatus) }}</span>
          <span v-if="role.application.agreementVersion">协议版本：{{ role.application.agreementVersion }}</span>
          <span v-if="role.application.depositAmount > 0">
            历史保证金记录：¥{{ role.application.depositAmount }}
          </span>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="agreementDialogVisible" append-to-body :title="agreement?.title || '身份协议'" width="680px" :close-on-click-modal="loadingRole === null" :close-on-press-escape="loadingRole === null" :show-close="loadingRole === null">
      <div v-if="agreement" class="protocol-dialog-body">
        <div class="panel-topline">
          <strong>协议版本 {{ agreement.version }}</strong>
          <span>生效时间：{{ agreement.effectiveAt?.slice(0, 10) || '当前版本' }}</span>
        </div>
        <el-alert title="请注意受托物品、交付责任、个人信息核查与争议处理条款。协议同意不是任意公开联系方式的授权。" type="warning" show-icon :closable="false" />
        <div class="protocol-text" tabindex="0">{{ agreement.content }}</div>
        <el-alert
          title="蜗壳 CampusHub 是独立学生项目，不代表任何学校或机构官方立场。"
          type="info"
          show-icon
          :closable="false"
        />
        <el-checkbox v-model="agreementAccepted" class="protocol-checkbox">
          我已阅读并同意《{{ agreement.title }}》（{{ agreement.version }}），并已注意重要条款。
        </el-checkbox>
      </div>
      <template #footer>
        <el-button :disabled="loadingRole !== null" @click="agreementDialogVisible = false">暂不申请</el-button>
        <el-button type="primary" :loading="loadingRole !== null" :disabled="!agreementAccepted" @click="confirmAgreement">
          同意协议并提交申请
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  applyRole,
  getCurrentRoleAgreement,
  getCurrentUser,
  listRoleApplications,
  type PlatformProtocolSummary,
  type RoleApplicationSummary,
} from '@/api/campushub'
import IdentityBadge from '@/components/common/IdentityBadge.vue'
import { useAuthStore } from '@/stores/auth'
import { formatStatus } from '@/utils/display'
import { roleActivationMode, roleRequiresAgreement } from '@/config/roleAgreements'

const auth = useAuthStore()
const identityProfile = computed(() => auth.identityProfile)
const loadingRole = ref<string | null>(null)
const agreementLoading = ref(false)
const agreementDialogVisible = ref(false)
const agreementAccepted = ref(false)
const agreementRoleType = ref<string | null>(null)
const agreement = ref<PlatformProtocolSummary | null>(null)
const applications = reactive<Record<string, RoleApplicationSummary>>({})
const notes = reactive<Record<string, string>>({
  RUNNER: '',
  GOODS_PUBLISHER: '',
  SHOP_MERCHANT: '',
  TRADE_ADMIN: '',
  SHOWCASE_ADMIN: '',
})

const roles = [
  {
    roleType: 'RUNNER',
    title: '顺路跑腿员',
    description: '阅读协议后可抢单或报名承接跨校区代取快递、急送资料、食堂带饭等日常跑腿委托。',
    review: false,
  },
  {
    roleType: 'GOODS_PUBLISHER',
    title: '闲置发布者',
    description: '阅读协议后可在二手市集上架教材、数码、单车与宿舍好物，明确交接责任。',
    review: false,
  },
  {
    roleType: 'SHOP_MERCHANT',
    title: '技能店主理人',
    description: '阅读协议后提交站务核验，适合人像约拍、电脑清灰、科研绘图、贴膜维修等长期个人小店。',
    review: true,
  },
  {
    roleType: 'TRADE_ADMIN',
    title: '交易共治管理员',
    description: '协助巡查跑腿大厅与二手市集秩序、调解同窗交接纠纷，由总站务审批开通。',
    review: true,
  },
  {
    roleType: 'SHOWCASE_ADMIN',
    title: '展示共治管理员',
    description: '协助核验项目广场招募信息、学生技能店铺与服务反馈，由总站务审批开通。',
    review: true,
  },
]

const roleAliases: Record<string, string[]> = {
  RUNNER: ['ROLE_RUNNER', 'RUNNER'],
  GOODS_PUBLISHER: ['ROLE_GOODS_PUBLISHER', 'GOODS_PUBLISHER'],
  SHOP_MERCHANT: ['ROLE_SHOP_MERCHANT', 'SHOP_MERCHANT'],
  TRADE_ADMIN: ['ROLE_TRADE_ADMIN', 'TRADE_ADMIN'],
  SHOWCASE_ADMIN: ['ROLE_SHOWCASE_ADMIN', 'SHOWCASE_ADMIN'],
}

const roleCards = computed(() =>
  roles.map((role) => ({
    ...role,
    application: applicationFor(role.roleType),
    unlocked: hasIdentity(role.roleType),
    agreementRequired: roleRequiresAgreement(role.roleType),
    activationMode: roleActivationMode(role.roleType),
  })),
)

function applicationFor(roleType: string) {
  return applications[roleType]
}

function hasIdentity(roleType: string) {
  const aliases = roleAliases[roleType] ?? [roleType]
  const userRoles = auth.currentUser?.roles.map((role) => role.trim().toUpperCase()) ?? []
  return aliases.some((role) => userRoles.includes(role))
}

function agreementStatusText(status?: string | null) {
  if (!status || status === 'LEGACY') return '历史记录（未补写协议同意）'
  if (status === 'CONSENTED') return '已同意所列版本'
  return formatStatus(status)
}

async function loadApplications() {
  const requestedUserId = auth.currentUser?.id
  if (!requestedUserId) {
    Object.keys(applications).forEach((roleType) => delete applications[roleType])
    return
  }
  try {
    const existing = await listRoleApplications(requestedUserId)
    if (auth.currentUser?.id !== requestedUserId) return
    Object.keys(applications).forEach((roleType) => delete applications[roleType])
    existing.forEach((application) => {
      applications[application.roleType] = application
    })
  } catch (error) {
    if (auth.currentUser?.id === requestedUserId) {
      ElMessage.error(error instanceof Error ? error.message : '身份申请加载失败，请稍后重试')
    }
  }
}

async function beginApplication(roleType: string) {
  if (loadingRole.value !== null || agreementLoading.value || hasIdentity(roleType)) return
  if (!auth.currentUser?.id) {
    ElMessage.warning('请先登录')
    return
  }
  if (!roleRequiresAgreement(roleType)) {
    await submit(roleType)
    return
  }
  agreementLoading.value = true
  try {
    agreement.value = await getCurrentRoleAgreement(roleType)
    agreementRoleType.value = roleType
    agreementAccepted.value = false
    agreementDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '协议加载失败，请稍后重试')
  } finally {
    agreementLoading.value = false
  }
}

async function confirmAgreement() {
  if (!agreementRoleType.value || !agreement.value || !agreementAccepted.value) return
  const success = await submit(agreementRoleType.value, agreement.value.version)
  if (success) agreementDialogVisible.value = false
}

async function submit(roleType: string, agreementVersion?: string) {
  if (loadingRole.value !== null) return false
  const userId = auth.currentUser?.id
  if (!userId) {
    ElMessage.warning('请先登录')
    return
  }
  loadingRole.value = roleType
  try {
    const result = await applyRole(userId, {
      roleType,
      applyNote: notes[roleType],
      agreementVersion,
      agreementAccepted: Boolean(agreementVersion),
    })
    if (auth.currentUser?.id !== userId) return true
    applications[roleType] = result
    if (result.reviewStatus === 'APPROVED') {
      const role = roleAliases[roleType]?.[0]
      if (role) auth.currentUser = { ...auth.currentUser, roles: [...new Set([...auth.currentUser.roles, role])] }
      try {
        const current = await getCurrentUser({ skipAuthExpireHandling: true })
        if (auth.currentUser?.id === userId) auth.currentUser = current
      } catch {
        ElMessage.warning('身份已开通，个人资料暂时刷新失败，可稍后刷新')
      }
    }
    ElMessage.success(result.reviewStatus === 'APPROVED' ? '身份已开通' : '申请已提交站务核验')
    return true
  } catch (error) {
    agreementAccepted.value = false
    ElMessage.error(error instanceof Error ? error.message : '申请失败，请稍后重试')
    return false
  } finally {
    loadingRole.value = null
  }
}

watch(
  () => auth.currentUser?.id,
  () => {
    void loadApplications()
  },
  { immediate: true },
)
</script>

<style scoped>
.protocol-dialog-body { display: grid; gap: 16px; }
.protocol-text { max-height: 45vh; overflow-y: auto; white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.85; padding: 12px; border: 1px solid var(--border-subtle); }
.protocol-checkbox { height: auto; align-items: flex-start; }
.protocol-checkbox :deep(.el-checkbox__label) { white-space: normal; line-height: 1.6; }
</style>
