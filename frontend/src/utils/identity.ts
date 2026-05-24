import type { CurrentUser } from '@/api/campushub'

export type IdentityKey = 'guest' | 'student' | 'runner' | 'goodsPublisher' | 'shopMerchant' | 'admin'

export interface IdentityItem {
  key: IdentityKey
  label: string
  shortLabel: string
}

export interface IdentityProfile {
  primaryIdentity: IdentityKey
  displayName: string
  identities: IdentityItem[]
  capabilities: string[]
}

type RoleAwareUser = Pick<CurrentUser, 'id' | 'username' | 'nickname' | 'roles'>

const IDENTITY_ORDER: Exclude<IdentityKey, 'guest'>[] = ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin']

const IDENTITY_ITEMS: Record<IdentityKey, IdentityItem> = {
  guest: { key: 'guest', label: '未登录游客', shortLabel: '游客' },
  student: { key: 'student', label: '普通学生', shortLabel: '学生' },
  runner: { key: 'runner', label: '跑腿接单者', shortLabel: '跑腿' },
  goodsPublisher: { key: 'goodsPublisher', label: '二手发布者', shortLabel: '二手' },
  shopMerchant: { key: 'shopMerchant', label: '店铺商家', shortLabel: '商家' },
  admin: { key: 'admin', label: '管理员', shortLabel: '管理' },
}

const CAPABILITIES: Record<IdentityKey, string> = {
  guest: '公开浏览校园服务',
  student: '钱包、通知与信用中心',
  runner: '跑腿接单与任务工作台',
  goodsPublisher: '二手商品发布与管理',
  shopMerchant: '店铺服务与预约处理',
  admin: '治理、支付、钱包与运营后台',
}

const ROLE_ALIASES: Record<Exclude<IdentityKey, 'guest'>, string[]> = {
  student: ['ROLE_STUDENT', 'STUDENT'],
  runner: ['ROLE_RUNNER', 'RUNNER'],
  goodsPublisher: ['ROLE_GOODS_PUBLISHER', 'GOODS_PUBLISHER'],
  shopMerchant: ['ROLE_SHOP_MERCHANT', 'SHOP_MERCHANT'],
  admin: ['ROLE_ADMIN', 'ADMIN'],
}

function normalizeRole(role: string) {
  return role.trim().toUpperCase()
}

export function hasAnyRole(roles: readonly string[] | null | undefined, identities: readonly IdentityKey[]) {
  if (!roles?.length) {
    return false
  }

  const normalizedRoles = new Set(roles.map(normalizeRole))
  return identities.some((identity) => {
    if (identity === 'guest') {
      return false
    }
    return ROLE_ALIASES[identity].some((role) => normalizedRoles.has(role))
  })
}

export function roleDisplayName(identity: IdentityKey) {
  return IDENTITY_ITEMS[identity].label
}

export function buildIdentityProfile(user: RoleAwareUser | null): IdentityProfile {
  if (!user) {
    return {
      primaryIdentity: 'guest',
      displayName: '游客',
      identities: [IDENTITY_ITEMS.guest],
      capabilities: [CAPABILITIES.guest],
    }
  }

  const identities = [
    IDENTITY_ITEMS.student,
    ...IDENTITY_ORDER.filter((identity) => identity !== 'student' && hasAnyRole(user.roles, [identity])).map(
      (identity) => IDENTITY_ITEMS[identity],
    ),
  ]
  const primaryIdentity = identities.some((identity) => identity.key === 'admin') ? 'admin' : identities[identities.length - 1].key

  return {
    primaryIdentity,
    displayName: user.nickname || user.username,
    identities,
    capabilities: identities.map((identity) => CAPABILITIES[identity.key]),
  }
}
