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
  public: '校园市集',
  account: '我的书院',
  workspace: '创客工作台',
  admin: '站务后台',
}

const groupOrder: NavGroupKey[] = ['public', 'account', 'workspace', 'admin']

export const navItems: NavItem[] = [
  {
    path: '/',
    label: '首页',
    description: '校园各校区好物、急单与同窗服务总览',
    group: 'public',
    audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin', 'tradeAdmin', 'showcaseAdmin', 'masterAdmin'],
    icon: 'HomeFilled',
    mobileTab: 'always',
  },
  {
    path: '/tasks',
    label: '悬赏跑腿',
    mobileLabel: '跑腿',
    description: '跨校区代取快递、急送资料与顺路互助',
    group: 'public',
    audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin', 'tradeAdmin', 'showcaseAdmin', 'masterAdmin'],
    icon: 'Position',
    mobileTab: 'always',
  },
  {
    path: '/goods',
    label: '二手商品',
    mobileLabel: '二手',
    description: '教材、数码、单车与宿舍好物校友流转',
    group: 'public',
    audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin', 'tradeAdmin', 'showcaseAdmin', 'masterAdmin'],
    icon: 'Goods',
    mobileTab: 'always',
  },
  {
    path: '/shops',
    label: '学生店铺',
    mobileLabel: '店铺',
    description: '约拍、电脑清灰、学术排版与才艺小店',
    group: 'public',
    audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin', 'tradeAdmin', 'showcaseAdmin', 'masterAdmin'],
    icon: 'Shop',
    mobileTab: 'always',
  },
  {
    path: '/project-ads',
    label: '项目广告',
    description: '大创组队、比赛招募、个人作品集与社团活动',
    group: 'public',
    audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin', 'tradeAdmin', 'showcaseAdmin', 'masterAdmin'],
    icon: 'Promotion',
  },
  {
    path: '/policy',
    label: '协议与风险',
    description: '社区公约、联系方式保护与交易安全须知',
    group: 'public',
    audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin', 'tradeAdmin', 'showcaseAdmin', 'masterAdmin'],
    icon: 'DocumentChecked',
  },
  {
    path: '/auth',
    label: '登录注册',
    mobileLabel: '登录',
    description: '使用校园邮箱验证加入蜗壳社区',
    group: 'public',
    audiences: ['guest'],
    icon: 'UserFilled',
    mobileTab: 'guest',
  },
  {
    path: '/profile',
    label: '个人信息',
    description: '维护展示昵称、用户名与登录密码',
    group: 'account',
    audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin', 'tradeAdmin', 'showcaseAdmin', 'masterAdmin'],
    icon: 'User',
    requiresAuth: true,
  },
  {
    path: '/wallet',
    label: '钱包中心',
    description: '查看账户与历史记录',
    group: 'account',
    audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin', 'tradeAdmin', 'showcaseAdmin', 'masterAdmin'],
    icon: 'Wallet',
    requiresAuth: true,
  },
  {
    path: '/notifications',
    label: '站内通知',
    mobileLabel: '通知',
    description: '查收接单进展、预约回复与站务提醒',
    group: 'account',
    audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin', 'tradeAdmin', 'showcaseAdmin', 'masterAdmin'],
    icon: 'Bell',
    mobileTab: 'auth',
    requiresAuth: true,
  },
  {
    path: '/credit',
    label: '信用中心',
    description: '查看校园信用誉分、履约记录与反馈进度',
    group: 'account',
    audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin', 'tradeAdmin', 'showcaseAdmin', 'masterAdmin'],
    icon: 'Medal',
    requiresAuth: true,
  },
  {
    path: '/roles',
    label: '身份解锁',
    description: '开通跑腿员、闲置卖家、小店主理人等身份',
    group: 'account',
    audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin', 'tradeAdmin', 'showcaseAdmin', 'masterAdmin'],
    icon: 'Key',
    requiresAuth: true,
  },
  {
    path: '/goods/publish',
    label: '发布二手',
    description: '上架闲置好物并约定校内交接地点',
    group: 'workspace',
    audiences: ['goodsPublisher', 'admin', 'masterAdmin'],
    icon: 'CirclePlus',
    requiresAuth: true,
    requiredRole: 'goodsPublisher',
    lockedTitle: '需要开通二手发布者身份',
    lockedDescription: '阅读闲置发布与交易规范后自动开通发布权限。',
    unlockRoute: '/roles',
  },
  {
    path: '/shops/merchant',
    label: '店铺工作台',
    description: '打理小店资料、服务项目与同窗预约单',
    group: 'workspace',
    audiences: ['shopMerchant', 'admin', 'masterAdmin'],
    icon: 'Management',
    requiresAuth: true,
    requiredRole: 'shopMerchant',
    lockedTitle: '需要开通店铺商家身份',
    lockedDescription: '阅读技能店主理人协议并提交站务核验后即可申请开店。',
    unlockRoute: '/roles',
  },
  {
    path: '/project-ads/manage',
    label: '项目管理',
    description: '发布与管理你的组队招募、作品集与活动展示',
    group: 'workspace',
    audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'],
    icon: 'Briefcase',
    requiresAuth: true,
  },
  {
    path: '/admin/master/admin-applications',
    label: '审核管理员申请',
    description: '审批交易管理员与展示管理员申请',
    group: 'admin',
    audiences: ['masterAdmin'],
    icon: 'Avatar',
    requiresAuth: true,
    requiredRole: 'masterAdmin',
  },
  {
    path: '/admin/trade',
    label: '交易管理',
    description: '巡查跑腿悬赏、二手闲置与交易争议协调',
    group: 'admin',
    audiences: ['tradeAdmin', 'masterAdmin'],
    icon: 'Tickets',
    requiresAuth: true,
    requiredRole: 'tradeAdmin',
  },
  {
    path: '/admin/showcase',
    label: '展示管理',
    description: '审核项目展示、学生店铺与服务纠纷处理',
    group: 'admin',
    audiences: ['showcaseAdmin', 'masterAdmin'],
    icon: 'Shop',
    requiresAuth: true,
    requiredRole: 'showcaseAdmin',
  },
  {
    path: '/admin/ops',
    label: '运营数据',
    description: '全站各板块活跃度、校区分布与报表导出',
    group: 'admin',
    audiences: ['admin', 'masterAdmin'],
    icon: 'DataAnalysis',
    requiresAdmin: true,
  },
  {
    path: '/admin/governance',
    label: '治理台',
    description: '受理社区举报、信用分调整与违规处置',
    group: 'admin',
    audiences: ['admin', 'masterAdmin'],
    icon: 'ScaleToOriginal',
    requiresAdmin: true,
  },
  {
    path: '/admin/payment',
    label: '支付监控',
    description: '核查收银台订单状态与回调处理记录',
    group: 'admin',
    audiences: ['admin', 'masterAdmin'],
    icon: 'CreditCard',
    requiresAdmin: true,
  },
  {
    path: '/admin/wallet',
    label: '钱包运营',
    description: '只读查看历史充值、提现与钱包流水',
    group: 'admin',
    audiences: ['admin', 'masterAdmin'],
    icon: 'Money',
    requiresAdmin: true,
  },
]

export function canSeeNavItem(profile: IdentityProfile, item: NavItem) {
  const identities = new Set(profile.identities.map((identity) => identity.key))
  return item.audiences.some((audience) => identities.has(audience))
}

export function getVisibleNavGroups(profile: IdentityProfile, financialEnabled = false): NavGroup[] {
  return groupOrder
    .map((groupKey) => ({
      key: groupKey,
      label: navGroupLabels[groupKey],
      items: navItems.filter((item) => item.group === groupKey
        && (item.path !== '/wallet' || financialEnabled)
        && canSeeNavItem(profile, item)),
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
