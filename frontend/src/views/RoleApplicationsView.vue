<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Identity & Deposit</p>
        <h2>身份与保证金</h2>
        <p>跑腿接单者和二手发布者支付保证金后自动开通，店铺商家进入人工审核。</p>
      </div>
    </div>

    <div class="premium-panel role-status-panel">
      <p class="eyebrow">Unlocked Capabilities</p>
      <h3>当前已解锁身份</h3>
      <IdentityBadge :identities="identityProfile.identities" />
      <p>跑腿和二手发布者支付保证金后自动开通；店铺商家支付保证金后进入人工审核。</p>
    </div>

    <div class="role-grid">
      <el-card v-for="role in roleCards" :key="role.roleType" class="role-card" shadow="never">
        <template #header>
          <div class="panel-topline">
            <strong>{{ role.title }}</strong>
            <el-tag :type="role.review ? 'warning' : 'success'">{{ role.review ? '人工审核' : '自动开通' }}</el-tag>
          </div>
        </template>
        <p>{{ role.description }}</p>
        <div class="deposit-amount">¥{{ role.depositAmount }}</div>
        <el-input v-model="notes[role.roleType]" type="textarea" :rows="3" placeholder="申请说明，可选" />
        <el-button type="primary" :loading="loadingRole === role.roleType" :disabled="role.unlocked" @click="submit(role.roleType)">
          {{ role.unlocked ? '已解锁' : '申请开通' }}
        </el-button>
        <div v-if="role.application" class="payment-meta">
          <span>保证金状态：{{ depositStatusText[role.application.depositStatus] ?? role.application.depositStatus }}</span>
          <span>审核状态：{{ role.application.reviewStatus }}</span>
          <span v-if="role.application.depositPaymentOrderNo">支付单：{{ role.application.depositPaymentOrderNo }}</span>
          <el-button
            v-if="role.application.depositStatus !== 'PAID'"
            size="small"
            :loading="payingApplicationId === role.application.id"
            @click="payDeposit(role.application.id)"
          >
            支付保证金
          </el-button>
        </div>
      </el-card>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { applyRole, createRoleDepositPayment, type RoleApplicationSummary } from '@/api/campushub'
import IdentityBadge from '@/components/common/IdentityBadge.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const identityProfile = computed(() => auth.identityProfile)
const loadingRole = ref<string | null>(null)
const payingApplicationId = ref<number | null>(null)
const applications = reactive<Record<string, RoleApplicationSummary>>({})
const depositStatusText: Record<string, string> = {
  PENDING: '待支付',
  PAID: '已支付',
  FAILED: '支付失败',
  EXPIRED: '已过期'
}
const notes = reactive<Record<string, string>>({
  RUNNER: '',
  GOODS_PUBLISHER: '',
  SHOP_MERCHANT: '',
})

const roles = [
  {
    roleType: 'RUNNER',
    title: '跑腿接单者',
    depositAmount: '5.00',
    description: '可抢单或申请校园跑腿任务，适合快递、外卖、打印代取等履约场景。',
    review: false,
  },
  {
    roleType: 'GOODS_PUBLISHER',
    title: '二手发布者',
    depositAmount: '10.00',
    description: '用于后续真实二手交易发布身份，保证商品信息和信用治理可追溯。',
    review: false,
  },
  {
    roleType: 'SHOP_MERCHANT',
    title: '学生店铺商家',
    depositAmount: '20.00',
    description: '适合摄影、贴膜、电脑维护等持续服务，需要管理员人工审核。',
    review: true,
  },
]

const roleAliases: Record<string, string[]> = {
  RUNNER: ['ROLE_RUNNER', 'RUNNER'],
  GOODS_PUBLISHER: ['ROLE_GOODS_PUBLISHER', 'GOODS_PUBLISHER'],
  SHOP_MERCHANT: ['ROLE_SHOP_MERCHANT', 'SHOP_MERCHANT'],
}

const roleCards = computed(() => roles.map((role) => ({
  ...role,
  application: applicationFor(role.roleType),
  unlocked: hasIdentity(role.roleType),
})))

function applicationFor(roleType: string) {
  return applications[roleType]
}

function hasIdentity(roleType: string) {
  const aliases = roleAliases[roleType] ?? [roleType]
  const userRoles = auth.currentUser?.roles.map((role) => role.trim().toUpperCase()) ?? []
  return aliases.some((role) => userRoles.includes(role))
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
    ElMessage.success(result.depositStatus === 'PENDING' ? '申请已提交，请继续支付保证金' : '申请已提交')
  } finally {
    loadingRole.value = null
  }
}

async function payDeposit(applicationId: number) {
  payingApplicationId.value = applicationId
  try {
    const result = await createRoleDepositPayment(applicationId)
    ElMessage.success(result.message || '保证金支付单已创建')
    if (result.payUrl && !result.payUrl.startsWith('mock://')) {
      window.open(result.payUrl, '_blank', 'noopener,noreferrer')
    }
  } finally {
    payingApplicationId.value = null
  }
}
</script>
