<template>
  <section class="home-page page-stack">
    <div class="bento-hero-grid">
      <div class="home-hero spotlight-surface" @mousemove="handleSpotlight">
        <div class="hero-content">
          <div class="hero-eyebrow-bar">
            <span class="seal-pill">蜗壳 CampusHub</span>
            <span class="hero-zone-ticker">中校区 · 西校区 · 东校区 · 高新园区 · 先研院</span>
          </div>
          <h2>{{ heroTitle }}</h2>
          <p>{{ heroDescription }}</p>
          <IdentityBadge :identities="identityProfile.identities" />
          <div class="home-hero-actions">
            <el-button v-if="!auth.isAuthenticated" type="primary" size="large" @click="router.push('/auth')">
              凭校园邮箱加入
            </el-button>
            <el-button v-else type="primary" size="large" @click="router.push('/tasks')">
              去逛悬赏与市集
            </el-button>
            <el-button size="large" class="hero-ghost-btn" @click="router.push('/goods')">浏览校友闲置</el-button>
            <el-button size="large" text class="hero-link-btn" @click="router.push('/policy')">社区公约与保障 →</el-button>
          </div>
        </div>
      </div>

      <aside class="home-passport-card spotlight-surface" @mousemove="handleSpotlight">
        <div class="passport-top">
          <div>
            <span class="passport-label">CAMPUS PASS · 校园通行证</span>
            <h3>{{ auth.isAuthenticated ? identityProfile.displayName : '访客通行卡' }}</h3>
          </div>
          <span class="passport-emblem">蜗</span>
        </div>

        <p class="passport-desc">
          {{
            auth.isAuthenticated
              ? '已绑定校园邮箱，可随时发起同城跑腿、流转闲置或预约同窗技能。'
              : '使用 edu.cn 校园邮箱验证后，可申请身份并参与互助与交易。邮箱验证不等于实名认证。'
          }}
        </p>

        <div class="passport-metrics">
          <div>
            <span>已点亮身份</span>
            <strong class="mono-num">{{ identityProfile.identities.length }} 项</strong>
          </div>
          <div>
            <span>隐私保护</span>
            <strong>意向达成后可见</strong>
          </div>
        </div>

        <div class="passport-footer">
          <el-button v-if="!auth.isAuthenticated" type="primary" class="wide" @click="router.push('/auth')">
            立即验证校园邮箱
          </el-button>
          <div v-else class="passport-quick-links">
            <el-button size="small" plain @click="router.push('/roles')">身份印章</el-button>
            <el-button size="small" plain @click="router.push('/notifications')">站内信箱</el-button>
          </div>
        </div>
      </aside>
    </div>

    <div class="campus-marquee-bar">
      <span class="marquee-tag">校区直通</span>
      <div class="marquee-items">
        <span @click="router.push('/tasks')">🏃 东区西区代取急件 · 顺路悬赏立达</span>
        <span class="dot">·</span>
        <span @click="router.push('/goods')">📦 毕业季教材、数码、单车校友直出</span>
        <span class="dot">·</span>
        <span @click="router.push('/shops')">🎨 校园约拍、装机清灰、LaTeX 排版小店</span>
        <span class="dot">·</span>
        <span @click="router.push('/project-ads')">🚀 大创国创、数学建模与实验室纳新广场</span>
      </div>
    </div>

    <div class="section-header-line">
      <div>
        <span class="section-kicker">身份与板块入口</span>
        <h3>四大校园生活场景</h3>
      </div>
      <span class="section-sub">支持随时切换买家与创作者视角</span>
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
        <span class="eyebrow">常用直达</span>
        <h3>现在可以从这里开始</h3>
        <div class="action-list">
          <button v-for="action in nextActions" :key="action.to" type="button" @click="router.push(action.to)">
            <div class="action-item-main">
              <strong>{{ action.title }}</strong>
              <span>{{ action.description }}</span>
            </div>
            <span class="action-arrow">→</span>
          </button>
        </div>
      </section>

      <section class="premium-panel editorial-note-panel">
        <span class="eyebrow">交易与隐私守则</span>
        <h3>同窗互信，安心交接</h3>
        <ul class="editorial-pledge-list">
          <li>
            <strong>微信号与 QQ 隐私保护</strong>
            <span>你的联系方式不会在大厅公开展示，仅在接单、预约或提交购买意向后向交易对方开放。</span>
          </li>
          <li>
            <strong>资金功能暂未开放</strong>
            <span>当前不创建充值、提现、身份保证金或线上担保业务；线下交接请双方自行核验并保留沟通凭证。</span>
          </li>
          <li>
            <strong>邮箱验证与社区信用</strong>
            <span>每一次准时送达、如实描述与友善沟通，都有助于维护你的校园信用誉分。</span>
          </li>
        </ul>
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

const heroTitle = computed(() =>
  auth.isAuthenticated
    ? `${identityProfile.value.displayName}，今日校园里有什么新鲜事？`
    : '中区取件、西区出闲置、高新找队友——同学们自己的校园生活集市',
)

const heroDescription = computed(() =>
  auth.isAuthenticated
    ? '无论是顺路帮带一份快递、出掉书架上的旧教材，还是在周末接一场校园约拍，这里都为你准备好了趁手的工作台。'
    : '凭校园邮箱验证加入，把散落在各个微信群与朋友圈里的跑腿互助、闲置流转、技能小店和比赛招募汇聚一处。',
)

const hasIdentity = (key: string) => identityProfile.value.identities.some((identity) => identity.key === key)

const capabilityCards = computed(() => [
  {
    icon: '跑',
    title: '顺路跑腿员',
    description: hasIdentity('runner')
      ? '你已开通跑腿接单权限，可随时抢单或申请跨校区代取、代送任务。'
      : '阅读跑腿服务协议后自动开通，课间顺路帮同学代取快递、急送材料。',
    badge: hasIdentity('runner') ? '已开通' : '阅读协议后开通',
    tone: 'green' as const,
    locked: !hasIdentity('runner'),
    to: !auth.isAuthenticated ? '/auth' : hasIdentity('runner') ? '/tasks' : '/roles',
    action: !auth.isAuthenticated ? '去登录开通' : hasIdentity('runner') ? '进入跑腿大厅' : '去开通身份',
  },
  {
    icon: '闲',
    title: '闲置发布者',
    description: hasIdentity('goodsPublisher')
      ? '你已开通闲置发布权限，可随时上架教材、数码、单车与宿舍好物。'
      : '阅读闲置发布与交易规范后自动开通，让闲置好物在同校同学之间安心流转。',
    badge: hasIdentity('goodsPublisher') ? '已开通' : '阅读协议后开通',
    tone: 'orange' as const,
    locked: !hasIdentity('goodsPublisher'),
    to: !auth.isAuthenticated ? '/auth' : hasIdentity('goodsPublisher') ? '/goods/publish' : '/roles',
    action: !auth.isAuthenticated ? '去登录开通' : hasIdentity('goodsPublisher') ? '上架闲置好物' : '去开通身份',
  },
  {
    icon: '店',
    title: '技能店主理人',
    description: hasIdentity('shopMerchant')
      ? '你已拥有主理人身份，可管理个人小店、上架服务项目并处理同窗预约。'
      : '适合摄影约拍、电脑清灰、科研绘图、贴膜维修等长期技能服务，经站务核验后开店。',
    badge: hasIdentity('shopMerchant') ? '营业中' : '协议后站务核验',
    tone: 'blue' as const,
    locked: !hasIdentity('shopMerchant'),
    to: !auth.isAuthenticated ? '/auth' : hasIdentity('shopMerchant') ? '/shops/merchant' : '/roles',
    action: !auth.isAuthenticated ? '去登录开通' : hasIdentity('shopMerchant') ? '进入店铺工作台' : '申请开店',
  },
  {
    icon: '展',
    title: '项目与站务共治',
    description: hasIdentity('admin')
      ? '你拥有站务管理权限，可巡查全站运营指标、受理社区反馈与核对资金账务。'
      : '免费发布大创组队、竞赛招募与作品集展示；亦可申请成为交易或展示管理员参与共治。',
    badge: hasIdentity('admin') ? '站务管理' : '全员开放',
    tone: 'dark' as const,
    locked: false,
    to: hasIdentity('admin') ? '/admin/ops' : '/project-ads',
    action: hasIdentity('admin') ? '进入运营看板' : '去逛项目广场',
  },
])

const nextActions = computed(() => {
  if (!auth.isAuthenticated) {
    return [
      { title: '验证校园邮箱加入', description: '支持 @mail.ustc.edu.cn 与 @ustc.edu.cn 邮箱一键收码注册。', to: '/auth' },
      { title: '先去市集与跑腿大厅逛逛', description: '无需登录即可浏览各校区最新上架的闲置好物与悬赏急单。', to: '/goods' },
      { title: '了解社区交易与隐私约定', description: '查看联系方式解锁机制、当面交接建议与站内信用规则。', to: '/policy' },
    ]
  }
  if (hasIdentity('admin')) {
    return [
      { title: '查看全站运营数据与报表', description: '按日期查看各板块转化漏斗、校区活跃度并导出 CSV。', to: '/admin/ops' },
      { title: '处理社区举报与信用记录', description: '及时响应同学反馈，维护清朗守信的校园交易环境。', to: '/admin/governance' },
      { title: '查看历史资金记录', description: '查看充值、提现和服务费历史记录；当前不开放新的资金操作。', to: '/admin/wallet' },
    ]
  }
  return [
    { title: '了解资金与交接边界', description: '查看当前暂未开放的资金功能说明，以及校内当面交接建议。', to: '/policy' },
    { title: '点亮更多创作者身份', description: '一键开通跑腿接单员、闲置卖家或申请技能店主理人。', to: '/roles' },
    { title: '查收最新站内通知', description: '及时跟进你的接单进度、买家意向与预约提醒。', to: '/notifications' },
  ]
})

function handleSpotlight(event: MouseEvent) {
  const target = event.currentTarget as HTMLElement | null
  if (!target) return
  const rect = target.getBoundingClientRect()
  target.style.setProperty('--mouse-x', `${event.clientX - rect.left}px`)
  target.style.setProperty('--mouse-y', `${event.clientY - rect.top}px`)
}
</script>
