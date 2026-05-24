<template>
  <section class="home-page page-stack">
    <div class="home-hero">
      <div>
        <p class="eyebrow">CampusHub Service OS</p>
        <h2>{{ heroTitle }}</h2>
        <p>{{ heroDescription }}</p>
        <IdentityBadge :identities="identityProfile.identities" />
        <div class="home-hero-actions">
          <el-button v-if="!auth.isAuthenticated" type="primary" size="large" @click="router.push('/auth')">使用校园邮箱加入</el-button>
          <el-button v-else type="primary" size="large" @click="router.push('/notifications')">查看我的通知</el-button>
          <el-button size="large" plain @click="router.push('/policy')">查看协议与风险</el-button>
        </div>
      </div>
      <div class="home-hero-panel">
        <span>已上线能力</span>
        <strong>12 个阶段</strong>
        <p>跑腿、二手、店铺、项目、治理、运营、钱包与真实支付中心闭环。</p>
      </div>
    </div>

    <div class="capability-grid">
      <CapabilityCard
        v-for="card in capabilityCards"
        :key="card.title"
        :icon="card.icon"
        :title="card.title"
        :description="card.description"
        :badge="card.badge"
        :tone="card.tone"
        :locked="card.locked"
      >
        <template #actions>
          <el-button type="primary" plain @click="router.push(card.to)">{{ card.action }}</el-button>
        </template>
      </CapabilityCard>
    </div>

    <div class="home-section-grid">
      <section class="premium-panel">
        <p class="eyebrow">Next Best Actions</p>
        <h3>建议下一步</h3>
        <div class="action-list">
          <button v-for="action in nextActions" :key="action.to" type="button" @click="router.push(action.to)">
            <strong>{{ action.title }}</strong>
            <span>{{ action.description }}</span>
          </button>
        </div>
      </section>

      <section class="premium-panel">
        <p class="eyebrow">Trust Boundary</p>
        <h3>平台边界</h3>
        <p>CampusHub 只保存本地钱包账本、服务费、保证金和业务状态；支付宝真实支付仍由 API-Transfer-Station 支付中心处理。</p>
        <p>微信充值采用人工审核，请按页面提示备注校园邮箱或 CampusHub 用户名。</p>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import CapabilityCard from '@/components/common/CapabilityCard.vue'
import IdentityBadge from '@/components/common/IdentityBadge.vue'

const router = useRouter()
const auth = useAuthStore()
const identityProfile = computed(() => auth.identityProfile)

const heroTitle = computed(() => auth.isAuthenticated ? `${identityProfile.value.displayName}，欢迎回到校集` : '把校园二手、跑腿和学生服务放进一个可信平台')
const heroDescription = computed(() => auth.isAuthenticated
  ? '根据你已解锁的身份，CampusHub 会优先展示最相关的钱包、通知、工作台和管理入口。'
  : '使用校园邮箱注册后，可充值钱包、申请身份、发布二手、承接跑腿、经营店铺并接收站内通知。')

const hasIdentity = (key: string) => identityProfile.value.identities.some((identity) => identity.key === key)

const capabilityCards = computed(() => [
  {
    icon: '跑',
    title: '跑腿接单者',
    description: hasIdentity('runner') ? '你已解锁抢单、申请和任务工作台能力。' : '支付 5 元保证金后自动开通，适合快递、外卖、打印代取。',
    badge: hasIdentity('runner') ? '已解锁' : '可申请',
    tone: 'green' as const,
    locked: !hasIdentity('runner'),
    to: !auth.isAuthenticated ? '/auth' : hasIdentity('runner') ? '/tasks' : '/roles',
    action: !auth.isAuthenticated ? '登录后申请' : hasIdentity('runner') ? '进入跑腿大厅' : '申请身份',
  },
  {
    icon: '二',
    title: '二手发布者',
    description: hasIdentity('goodsPublisher') ? '你可以发布和管理二手商品。' : '支付 10 元保证金后自动开通，用于可信二手发布。',
    badge: hasIdentity('goodsPublisher') ? '已解锁' : '可申请',
    tone: 'orange' as const,
    locked: !hasIdentity('goodsPublisher'),
    to: !auth.isAuthenticated ? '/auth' : hasIdentity('goodsPublisher') ? '/goods/publish' : '/roles',
    action: !auth.isAuthenticated ? '登录后申请' : hasIdentity('goodsPublisher') ? '发布二手' : '申请身份',
  },
  {
    icon: '店',
    title: '店铺商家',
    description: hasIdentity('shopMerchant') ? '你可以维护店铺、服务项目和预约。' : '支付 20 元保证金并通过人工审核后开通。',
    badge: hasIdentity('shopMerchant') ? '已解锁' : '需审核',
    tone: 'blue' as const,
    locked: !hasIdentity('shopMerchant'),
    to: !auth.isAuthenticated ? '/auth' : hasIdentity('shopMerchant') ? '/shops/merchant' : '/roles',
    action: !auth.isAuthenticated ? '登录后申请' : hasIdentity('shopMerchant') ? '店铺工作台' : '申请身份',
  },
  {
    icon: '管',
    title: '运营管理员',
    description: hasIdentity('admin') ? '你可以进入审核、治理、运营、支付和钱包后台。' : '管理员能力仅对授权账号展示。',
    badge: hasIdentity('admin') ? '管理员' : '受限',
    tone: 'dark' as const,
    locked: !hasIdentity('admin'),
    to: hasIdentity('admin') ? '/admin/ops' : '/policy',
    action: hasIdentity('admin') ? '进入运营后台' : '了解规则',
  },
])

const nextActions = computed(() => {
  if (!auth.isAuthenticated) return [
    { title: '注册或登录', description: '使用校园邮箱加入，继续后续充值和身份申请。', to: '/auth' },
    { title: '先浏览服务', description: '看看当前校园二手、跑腿、店铺和项目广告。', to: '/tasks' },
    { title: '了解风险边界', description: '阅读平台资金、隐私和交易风险说明。', to: '/policy' },
  ]
  if (hasIdentity('admin')) return [
    { title: '查看运营数据', description: '进入跨业务运营看板和导出入口。', to: '/admin/ops' },
    { title: '处理治理队列', description: '查看举报、违规、信用和限制。', to: '/admin/governance' },
    { title: '查看支付监控', description: '确认支付订单和回调事件状态。', to: '/admin/payment' },
  ]
  return [
    { title: '充值钱包', description: '使用支付宝支付中心或微信人工充值。', to: '/wallet' },
    { title: '申请身份', description: '解锁跑腿、二手发布或店铺商家能力。', to: '/roles' },
    { title: '查看通知', description: '关注身份、支付、治理和交易提醒。', to: '/notifications' },
  ]
})
</script>
