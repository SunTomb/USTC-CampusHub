<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">我的书院 · 身份印章与保证金</p>
        <h2>身份解锁中心</h2>
        <p>跑腿接单员与闲置发布者缴纳保证金后实时自动点亮；技能店主理人与领域管理员经站务审核后开通。</p>
      </div>
    </div>

    <div class="premium-panel role-status-panel">
      <p class="eyebrow">当前已点亮身份</p>
      <h3>我的瀚海通行印章</h3>
      <IdentityBadge :identities="identityProfile.identities" />
      <p>支持使用支付宝收银台或钱包可用余额缴纳身份保证金；无不良记录时保证金始终归属于你。</p>
    </div>

    <div class="role-grid">
      <el-card v-for="role in roleCards" :key="role.roleType" class="role-card spotlight-surface" shadow="never">
        <template #header>
          <div class="panel-topline">
            <strong>{{ role.title }}</strong>
            <el-tag :type="role.review ? 'warning' : 'success'" effect="plain">
              {{ role.review ? '需站务核验' : '缴后即开' }}
            </el-tag>
          </div>
        </template>
        <p>{{ role.description }}</p>
        <div class="deposit-amount mono-num">{{ role.depositAmount > 0 ? `¥${role.depositAmount}` : '免保证金' }}</div>
        <el-input v-model="notes[role.roleType]" type="textarea" :rows="3" placeholder="填写简短的申请说明或擅长领域（选填）" />
        <el-button type="primary" :loading="loadingRole === role.roleType" :disabled="role.unlocked" @click="submit(role.roleType)">
          {{ role.unlocked ? '已点亮该身份' : '申请点亮身份' }}
        </el-button>
        <div v-if="role.application" class="payment-meta">
          <span>保证金状态：{{ depositStatusText[role.application.depositStatus] ?? formatStatus(role.application.depositStatus) }}</span>
          <span>核验进度：{{ formatStatus(role.application.reviewStatus) }}</span>
          <span v-if="role.application.depositPaymentOrderNo">收银台单号：<span class="mono-num">{{ role.application.depositPaymentOrderNo }}</span></span>
          <div class="payment-actions" v-if="role.application.depositStatus !== 'PAID' && role.application.depositAmount > 0">
            <el-button
              size="small"
              type="primary"
              plain
              :loading="payingApplicationId === role.application.id"
              @click="payDeposit(role.application.id)"
            >
              支付宝支付
            </el-button>
            <el-button
              size="small"
              type="success"
              plain
              :loading="walletPayingApplicationId === role.application.id"
              @click="payDepositByWallet(role.application.id)"
            >
              钱包余额支付
            </el-button>
          </div>
        </div>
      </el-card>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { applyRole, createRoleDepositPayment, listRoleApplications, payRoleDepositWithWallet, type RoleApplicationSummary } from '@/api/campushub'
import IdentityBadge from '@/components/common/IdentityBadge.vue'
import { useAuthStore } from '@/stores/auth'
import { formatStatus } from '@/utils/display'

const auth = useAuthStore()
const identityProfile = computed(() => auth.identityProfile)
const loadingRole = ref<string | null>(null)
const payingApplicationId = ref<number | null>(null)
const walletPayingApplicationId = ref<number | null>(null)
const applications = reactive<Record<string, RoleApplicationSummary>>({})
const depositStatusText: Record<string, string> = {
  PENDING: '待缴纳',
  PAID: '已缴纳',
  FAILED: '支付未完成',
  EXPIRED: '支付单已过期',
  NOT_REQUIRED: '免保证金',
}
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
    depositAmount: 5,
    description: '可抢单或报名承接跨校区代取快递、急送资料、食堂带饭等日常跑腿委托。',
    review: false,
  },
  {
    roleType: 'GOODS_PUBLISHER',
    title: '闲置发布者',
    depositAmount: 10,
    description: '可在二手市集上架教材、数码、单车与宿舍好物，保障校内闲置交易真实可溯。',
    review: false,
  },
  {
    roleType: 'SHOP_MERCHANT',
    title: '技能店主理人',
    depositAmount: 20,
    description: '适合人像约拍、电脑清灰、科研绘图、贴膜维修等长期个人小店，经站务核验后开通。',
    review: true,
  },
  {
    roleType: 'TRADE_ADMIN',
    title: '交易共治管理员',
    depositAmount: 0,
    description: '协助巡查跑腿大厅与二手市集秩序、调解同窗交接纠纷，由总站务审批开通。',
    review: true,
  },
  {
    roleType: 'SHOWCASE_ADMIN',
    title: '展示共治管理员',
    depositAmount: 0,
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

async function loadApplications() {
  const requestedUserId = auth.currentUser?.id
  if (!requestedUserId) {
    Object.keys(applications).forEach((roleType) => {
      delete applications[roleType]
    })
    return
  }
  try {
    const existing = await listRoleApplications(requestedUserId)
    if (auth.currentUser?.id !== requestedUserId) {
      return
    }
    Object.keys(applications).forEach((roleType) => {
      delete applications[roleType]
    })
    existing.forEach((application) => {
      applications[application.roleType] = application
    })
  } catch (error) {
    if (auth.currentUser?.id === requestedUserId) {
      ElMessage.error(error instanceof Error ? error.message : '身份申请加载失败，请稍后重试')
    }
  }
}

async function submit(roleType: string) {
  const userId = auth.currentUser?.id
  if (!userId) {
    ElMessage.warning('请先登录')
    return
  }
  loadingRole.value = roleType
  try {
    const result = await applyRole(userId, { roleType, applyNote: notes[roleType] })
    applications[roleType] = result
    ElMessage.success(result.depositStatus === 'PENDING' ? '申请已记录，请继续完成保证金缴纳' : '申请已提交')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '申请失败，请稍后重试')
  } finally {
    loadingRole.value = null
  }
}

async function payDeposit(applicationId: number) {
  payingApplicationId.value = applicationId
  try {
    const result = await createRoleDepositPayment(applicationId)
    ElMessage.success(result.message || '保证金支付单已创建')
    await loadApplications()
    if (result.payUrl && !result.payUrl.startsWith('mock://')) {
      window.open(result.payUrl, '_blank', 'noopener,noreferrer')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保证金支付失败，请稍后重试')
  } finally {
    payingApplicationId.value = null
  }
}

async function payDepositByWallet(applicationId: number) {
  walletPayingApplicationId.value = applicationId
  try {
    const result = await payRoleDepositWithWallet(applicationId)
    ElMessage.success(result.message || '余额缴纳成功，身份已更新')
    await auth.loadCurrentUser()
    await loadApplications()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '余额不足或缴纳失败，请先充值或改用支付宝')
  } finally {
    walletPayingApplicationId.value = null
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
