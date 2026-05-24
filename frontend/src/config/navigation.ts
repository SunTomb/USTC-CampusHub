import type { IdentityKey, IdentityProfile } from '@/utils/identity'

export type NavGroupKey = 'public' | 'account' | 'workspace' | 'admin'
export type NavMobileTab = 'guest' | 'auth' | 'always'

export interface NavItem {
  path: string
  label: string
  mobileLabel?: string
  description: string
  group: NavGroupKey
  audiences: IdentityKey[]
  icon: string
  mobileTab?: NavMobileTab
  requiresAuth?: boolean
  requiredRole?: IdentityKey
  requiresAdmin?: boolean
  lockedTitle?: string
  lockedDescription?: string
  unlockRoute?: string
}

export interface NavGroup {
  key: NavGroupKey
  label: string
  items: NavItem[]
}

const navGroupLabels: Record<NavGroupKey, string> = {
  public: '校园服务',
  account: '个人中心',
  workspace: '工作台',
  admin: '管理后台',
}

const groupOrder: NavGroupKey[] = ['public', 'account', 'workspace', 'admin']

export const navItems: NavItem[] = [
  {
    path: '/',
    label: '首页',
    description: '浏览 CampusHub 校园服务入口与平台概览',
    group: 'public',
    audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'],
    icon: 'HomeFilled',
    mobileTab: 'always',
  },
  {
    path: '/tasks',
    label: '悬赏跑腿',
    mobileLabel: '跑腿',
    description: '查看校园取件、代买、跑腿等悬赏任务',
    group: 'public',
    audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'],
    icon: 'Position',
    mobileTab: 'always',
  },
  {
    path: '/goods',
    label: '二手商品',
    mobileLabel: '二手',
    description: '浏览校园二手商品与闲置交易信息',
    group: 'public',
    audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'],
    icon: 'Goods',
    mobileTab: 'always',
  },
  {
    path: '/shops',
    label: '学生店铺',
    mobileLabel: '店铺',
    description: '发现同学校园技能服务与预约项目',
    group: 'public',
    audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'],
    icon: 'Shop',
    mobileTab: 'always',
  },
  {
    path: '/project-ads',
    label: '项目广告',
    description: '查看校园项目展示、招募与作品集广告',
    group: 'public',
    audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'],
    icon: 'Promotion',
  },
  {
    path: '/policy',
    label: '协议与风险',
    description: '查看平台使用规则、交易风险和安全提示',
    group: 'public',
    audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'],
    icon: 'DocumentChecked',
  },
  {
    path: '/auth',
    label: '登录注册',
    mobileLabel: '登录',
    description: '登录账号或注册校园邮箱账号',
    group: 'public',
    audiences: ['guest'],
    icon: 'UserFilled',
    mobileTab: 'guest',
  },
  {
    path: '/wallet',
    label: '钱包中心',
    description: '管理余额、充值、提现、冻结与钱包流水',
    group: 'account',
    audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'],
    icon: 'Wallet',
    requiresAuth: true,
  },
  {
    path: '/notifications',
    label: '站内通知',
    mobileLabel: '通知',
    description: '查看任务、店铺、支付与治理相关站内消息',
    group: 'account',
    audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'],
    icon: 'Bell',
    mobileTab: 'auth',
    requiresAuth: true,
  },
  {
    path: '/credit',
    label: '信用中心',
    description: '查看信用分、信用调整和限制状态',
    group: 'account',
    audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'],
    icon: 'Medal',
    requiresAuth: true,
  },
  {
    path: '/roles',
    label: '身份解锁',
    description: '申请跑腿、二手发布者和店铺商家身份',
    group: 'account',
    audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'],
    icon: 'Key',
    requiresAuth: true,
  },
  {
    path: '/goods/publish',
    label: '发布二手',
    description: '发布二手商品并管理交易方式',
    group: 'workspace',
    audiences: ['goodsPublisher', 'admin'],
    icon: 'CirclePlus',
    requiresAuth: true,
    requiredRole: 'goodsPublisher',
    lockedTitle: '需要二手发布者身份',
    lockedDescription: '请先完成二手发布者身份解锁，再发布商品。',
    unlockRoute: '/roles',
  },
  {
    path: '/shops/merchant',
    label: '店铺工作台',
    description: '管理学生店铺、服务项目与预约订单',
    group: 'workspace',
    audiences: ['shopMerchant', 'admin'],
    icon: 'Management',
    requiresAuth: true,
    requiredRole: 'shopMerchant',
    lockedTitle: '需要店铺商家身份',
    lockedDescription: '请先完成店铺商家身份解锁，再管理店铺。',
    unlockRoute: '/roles',
  },
  {
    path: '/project-ads/manage',
    label: '项目管理',
    description: '创建、提交与管理项目广告和校园展示',
    group: 'workspace',
    audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'],
    icon: 'Briefcase',
    requiresAuth: true,
  },
  {
    path: '/admin/review',
    label: '内容审核',
    description: '处理内容审核、举报与平台安全事项',
    group: 'admin',
    audiences: ['admin'],
    icon: 'Checked',
    requiresAdmin: true,
  },
  {
    path: '/admin/ops',
    label: '运营数据',
    description: '查看跨业务运营指标、漏斗分析与导出',
    group: 'admin',
    audiences: ['admin'],
    icon: 'DataAnalysis',
    requiresAdmin: true,
  },
  {
    path: '/admin/governance',
    label: '治理台',
    description: '处理举报、违规、信用调整与用户限制',
    group: 'admin',
    audiences: ['admin'],
    icon: 'ScaleToOriginal',
    requiresAdmin: true,
  },
  {
    path: '/admin/payment',
    label: '支付监控',
    description: '查看支付订单、回调事件和支付中心状态',
    group: 'admin',
    audiences: ['admin'],
    icon: 'CreditCard',
    requiresAdmin: true,
  },
  {
    path: '/admin/wallet',
    label: '钱包运营',
    description: '审核充值、提现和冻结记录等钱包运营事项',
    group: 'admin',
    audiences: ['admin'],
    icon: 'Money',
    requiresAdmin: true,
  },
]

export function canSeeNavItem(profile: IdentityProfile, item: NavItem) {
  const identities = new Set(profile.identities.map((identity) => identity.key))
  return item.audiences.some((audience) => identities.has(audience))
}

export function getVisibleNavGroups(profile: IdentityProfile): NavGroup[] {
  return groupOrder
    .map((groupKey) => ({
      key: groupKey,
      label: navGroupLabels[groupKey],
      items: navItems.filter((item) => item.group === groupKey && canSeeNavItem(profile, item)),
    }))
    .filter((group) => group.items.length > 0)
}

function withMobileLabel(item: NavItem): NavItem {
  return item.mobileLabel ? { ...item, label: item.mobileLabel } : item
}

export function getMobileTabItems(profile: IdentityProfile): NavItem[] {
  const isGuest = profile.primaryIdentity === 'guest'
  return navItems
    .filter((item) => item.mobileTab === 'always' || item.mobileTab === (isGuest ? 'guest' : 'auth'))
    .filter((item) => canSeeNavItem(profile, item))
    .map(withMobileLabel)
}

export function findNavItemByPath(path: string) {
  return navItems.find((item) => item.path === path)
}
