# CampusHub Phase 13 Identity-Aware UI Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade CampusHub into a visually polished, identity-aware campus service platform with role-filtered navigation, premium branding, smoother onboarding, and clearer permission guidance.

**Architecture:** Keep Phase 13 frontend-first: derive identity and capabilities from the existing authenticated user's `roles`, drive desktop/mobile navigation from one shared config, and layer a stronger visual system over the existing Vue 3 + Element Plus app. Backend APIs, payment-center boundaries, and Flyway migrations remain unchanged unless implementation proves existing `/auth/me` data insufficient.

**Tech Stack:** Vue 3, Vite, TypeScript, Pinia, Vue Router, Element Plus, Vitest, centralized CSS in `frontend/src/styles.css`, existing Spring Boot APIs.

---

## File structure and responsibilities

- Create `frontend/src/utils/identity.ts`: maps backend role strings to UI identities, capability labels, and role checks. Keeps identity logic out of views.
- Create `frontend/src/utils/identity.test.ts`: unit tests for guest/student/admin/multi-role identity behavior.
- Create `frontend/src/config/navigation.ts`: single source of truth for route groups, visibility, mobile tab eligibility, required roles, and locked-state copy.
- Create `frontend/src/config/navigation.test.ts`: unit tests for guest/student/admin navigation filtering and mobile tabs.
- Create `frontend/src/components/common/IdentityBadge.vue`: compact badge list for current user identities.
- Create `frontend/src/components/common/CapabilityCard.vue`: reusable capability/unlock cards for home and role pages.
- Create `frontend/src/components/common/LockedState.vue`: reusable login/role/admin denied state with clear CTA.
- Modify `frontend/src/stores/auth.ts`: add getters for identity profile and common role checks.
- Modify `frontend/src/router/index.ts`: add role-aware route meta and route guard behavior that redirects with actionable context.
- Modify `frontend/src/layouts/MainLayout.vue`: rebuild desktop sidebar, header identity summary, mobile tab bar, and mobile directory drawer from `navigation.ts`.
- Modify `frontend/src/views/HomeView.vue`: replace simple module grid with identity-aware landing/workbench.
- Modify `frontend/src/views/AuthView.vue`: improve onboarding copy and login/register guidance without changing API behavior.
- Modify `frontend/src/views/RoleApplicationsView.vue`: turn role deposits into an ability unlock center.
- Modify `frontend/src/views/WalletView.vue`: polish wallet/recharge presentation while preserving Alipay payment-center and WeChat QR flows.
- Modify core business views only where needed for CTA/locked-state polish: `TasksView.vue`, `GoodsView.vue`, `GoodsPublishView.vue`, `ShopsView.vue`, `ShopMerchantView.vue`, `ProjectAdsView.vue`, `ProjectAdManageView.vue`.
- Modify admin/account support views for visual consistency: `NotificationsView.vue`, `CreditCenterView.vue`, `AdminOperationsView.vue`, `AdminGovernanceView.vue`, `AdminReviewView.vue`, `AdminPaymentView.vue`, `AdminWalletView.vue`.
- Modify `frontend/src/styles.css`: brand tokens, shell layout, navigation groups, premium cards, capability cards, locked states, mobile directory, and admin surfaces.
- Modify `README.md` and `CLAUDE.md`: Phase 13 handoff and verification status after implementation/deployment.

## Role names to support

Support both current likely role names and legacy/plain variants so the frontend is resilient to backend naming differences:

- Student: `ROLE_STUDENT`, `STUDENT`
- Admin: `ROLE_ADMIN`, `ADMIN`
- Runner: `ROLE_RUNNER`, `RUNNER`
- Goods publisher: `ROLE_GOODS_PUBLISHER`, `GOODS_PUBLISHER`
- Shop merchant: `ROLE_SHOP_MERCHANT`, `SHOP_MERCHANT`

---

### Task 1: Identity utility and tests

**Files:**
- Create: `frontend/src/utils/identity.ts`
- Create: `frontend/src/utils/identity.test.ts`
- Modify: `frontend/src/stores/auth.ts`

- [ ] **Step 1: Write identity utility tests**

Create `frontend/src/utils/identity.test.ts`:

```ts
import { describe, expect, it } from 'vitest'

import { buildIdentityProfile, hasAnyRole, roleDisplayName } from './identity'

describe('identity utilities', () => {
  it('treats missing users as guests', () => {
    const profile = buildIdentityProfile(null)

    expect(profile.primaryIdentity).toBe('guest')
    expect(profile.displayName).toBe('游客')
    expect(profile.identities.map((item) => item.key)).toEqual(['guest'])
    expect(profile.capabilities).toContain('公开浏览校园服务')
  })

  it('builds a student profile from ROLE_STUDENT', () => {
    const profile = buildIdentityProfile({ id: 1, username: 'alice', nickname: 'Alice', roles: ['ROLE_STUDENT'] })

    expect(profile.primaryIdentity).toBe('student')
    expect(profile.displayName).toBe('Alice')
    expect(profile.identities.map((item) => item.key)).toEqual(['student'])
    expect(profile.capabilities).toContain('钱包、通知与信用中心')
  })

  it('builds multi-role profiles in stable display order', () => {
    const profile = buildIdentityProfile({
      id: 2,
      username: 'ops',
      nickname: 'Ops',
      roles: ['ROLE_ADMIN', 'ROLE_SHOP_MERCHANT', 'ROLE_RUNNER', 'ROLE_GOODS_PUBLISHER'],
    })

    expect(profile.primaryIdentity).toBe('admin')
    expect(profile.identities.map((item) => item.key)).toEqual(['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'])
    expect(profile.capabilities).toContain('治理、支付、钱包与运营后台')
  })

  it('accepts plain role aliases', () => {
    expect(hasAnyRole(['RUNNER'], ['runner'])).toBe(true)
    expect(hasAnyRole(['GOODS_PUBLISHER'], ['goodsPublisher'])).toBe(true)
    expect(hasAnyRole(['SHOP_MERCHANT'], ['shopMerchant'])).toBe(true)
    expect(hasAnyRole(['ADMIN'], ['admin'])).toBe(true)
  })

  it('returns readable role display names', () => {
    expect(roleDisplayName('runner')).toBe('跑腿接单者')
    expect(roleDisplayName('goodsPublisher')).toBe('二手发布者')
    expect(roleDisplayName('shopMerchant')).toBe('店铺商家')
    expect(roleDisplayName('admin')).toBe('管理员')
  })
})
```

- [ ] **Step 2: Run identity tests to verify they fail**

Run: `npm --prefix frontend run test -- src/utils/identity.test.ts`

Expected: FAIL because `frontend/src/utils/identity.ts` does not exist.

- [ ] **Step 3: Implement identity utility**

Create `frontend/src/utils/identity.ts`:

```ts
import type { CurrentUser } from '@/api/campushub'

export type IdentityKey = 'guest' | 'student' | 'runner' | 'goodsPublisher' | 'shopMerchant' | 'admin'

export interface IdentityItem {
  key: IdentityKey
  label: string
  shortLabel: string
  description: string
}

export interface IdentityProfile {
  displayName: string
  primaryIdentity: IdentityKey
  identities: IdentityItem[]
  capabilities: string[]
}

const roleAliases: Record<Exclude<IdentityKey, 'guest'>, string[]> = {
  student: ['ROLE_STUDENT', 'STUDENT'],
  runner: ['ROLE_RUNNER', 'RUNNER'],
  goodsPublisher: ['ROLE_GOODS_PUBLISHER', 'GOODS_PUBLISHER'],
  shopMerchant: ['ROLE_SHOP_MERCHANT', 'SHOP_MERCHANT'],
  admin: ['ROLE_ADMIN', 'ADMIN'],
}

const identityItems: Record<IdentityKey, IdentityItem> = {
  guest: {
    key: 'guest',
    label: '未登录游客',
    shortLabel: '游客',
    description: '可浏览公开校园服务，登录后可互动、充值和申请身份。',
  },
  student: {
    key: 'student',
    label: '普通学生',
    shortLabel: '学生',
    description: '可使用钱包、通知、信用中心、收藏评论和角色申请。',
  },
  runner: {
    key: 'runner',
    label: '跑腿接单者',
    shortLabel: '跑腿',
    description: '可抢单或申请校园跑腿任务，并进入任务工作台。',
  },
  goodsPublisher: {
    key: 'goodsPublisher',
    label: '二手发布者',
    shortLabel: '二手',
    description: '可发布和管理二手商品，承接可信交易线索。',
  },
  shopMerchant: {
    key: 'shopMerchant',
    label: '店铺商家',
    shortLabel: '商家',
    description: '可创建学生店铺、维护服务项目并处理预约。',
  },
  admin: {
    key: 'admin',
    label: '管理员',
    shortLabel: '管理',
    description: '可进入审核、治理、运营、支付和钱包后台。',
  },
}

const identityOrder: Exclude<IdentityKey, 'guest'>[] = ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin']

const capabilityText: Record<IdentityKey, string> = {
  guest: '公开浏览校园服务',
  student: '钱包、通知与信用中心',
  runner: '跑腿接单与任务工作台',
  goodsPublisher: '二手商品发布与管理',
  shopMerchant: '店铺服务与预约处理',
  admin: '治理、支付、钱包与运营后台',
}

export function hasAnyRole(userRoles: string[] | undefined, identities: IdentityKey[]) {
  if (!userRoles?.length) return false
  return identities.some((identity) => {
    if (identity === 'guest') return false
    return roleAliases[identity].some((role) => userRoles.includes(role))
  })
}

export function roleDisplayName(identity: IdentityKey) {
  return identityItems[identity].label
}

export function buildIdentityProfile(user: Pick<CurrentUser, 'username' | 'nickname' | 'roles'> | null): IdentityProfile {
  if (!user) {
    return {
      displayName: '游客',
      primaryIdentity: 'guest',
      identities: [identityItems.guest],
      capabilities: [capabilityText.guest],
    }
  }

  const identities = identityOrder.filter((identity) => hasAnyRole(user.roles, [identity])).map((identity) => identityItems[identity])
  if (!identities.some((identity) => identity.key === 'student')) {
    identities.unshift(identityItems.student)
  }

  const primaryIdentity = identities.some((identity) => identity.key === 'admin')
    ? 'admin'
    : identities[identities.length - 1]?.key ?? 'student'

  return {
    displayName: user.nickname || user.username,
    primaryIdentity,
    identities,
    capabilities: identities.map((identity) => capabilityText[identity.key]),
  }
}
```

- [ ] **Step 4: Add auth store getters**

Modify `frontend/src/stores/auth.ts` imports and getters:

```ts
import { defineStore } from 'pinia'
import { getCurrentUser, type CurrentUser } from '@/api/campushub'
import { postApi } from '@/api/client'
import { buildIdentityProfile, hasAnyRole, type IdentityKey } from '@/utils/identity'
```

Replace the `getters` block with:

```ts
  getters: {
    isAuthenticated: (state) => Boolean(state.token && state.currentUser),
    identityProfile: (state) => buildIdentityProfile(state.currentUser),
    isAdmin: (state) => hasAnyRole(state.currentUser?.roles, ['admin']),
    canAccessIdentity: (state) => (identity: IdentityKey) => identity === 'guest' || hasAnyRole(state.currentUser?.roles, [identity]),
  },
```

Keep the existing `hasRole(role: string)` action for compatibility with any current callers.

- [ ] **Step 5: Run identity tests to verify they pass**

Run: `npm --prefix frontend run test -- src/utils/identity.test.ts`

Expected: PASS for all identity utility tests.

- [ ] **Step 6: Commit Task 1**

```bash
git add frontend/src/utils/identity.ts frontend/src/utils/identity.test.ts frontend/src/stores/auth.ts
git commit -m "add identity profile utilities"
```

---

### Task 2: Navigation config and route guard

**Files:**
- Create: `frontend/src/config/navigation.ts`
- Create: `frontend/src/config/navigation.test.ts`
- Modify: `frontend/src/router/index.ts`

- [ ] **Step 1: Write navigation tests**

Create `frontend/src/config/navigation.test.ts`:

```ts
import { describe, expect, it } from 'vitest'

import { getVisibleNavGroups, getMobileTabItems } from './navigation'
import { buildIdentityProfile } from '@/utils/identity'

const guestProfile = buildIdentityProfile(null)
const studentProfile = buildIdentityProfile({ id: 1, username: 'student', nickname: '学生', roles: ['ROLE_STUDENT'] })
const adminProfile = buildIdentityProfile({ id: 2, username: 'admin', nickname: '管理员', roles: ['ROLE_STUDENT', 'ROLE_ADMIN'] })

describe('navigation config', () => {
  it('keeps private and admin entries out of guest navigation', () => {
    const labels = getVisibleNavGroups(guestProfile).flatMap((group) => group.items.map((item) => item.label))

    expect(labels).toContain('首页')
    expect(labels).toContain('二手商品')
    expect(labels).toContain('登录注册')
    expect(labels).not.toContain('钱包中心')
    expect(labels).not.toContain('运营数据')
  })

  it('shows student account entries after login', () => {
    const labels = getVisibleNavGroups(studentProfile).flatMap((group) => group.items.map((item) => item.label))

    expect(labels).toContain('钱包中心')
    expect(labels).toContain('站内通知')
    expect(labels).toContain('身份解锁')
    expect(labels).not.toContain('运营数据')
  })

  it('shows admin backend entries for admins', () => {
    const labels = getVisibleNavGroups(adminProfile).flatMap((group) => group.items.map((item) => item.label))

    expect(labels).toContain('运营数据')
    expect(labels).toContain('治理台')
    expect(labels).toContain('支付监控')
    expect(labels).toContain('钱包运营')
  })

  it('changes mobile tabs by authentication state', () => {
    expect(getMobileTabItems(guestProfile).map((item) => item.label)).toEqual(['首页', '跑腿', '二手', '店铺', '登录'])
    expect(getMobileTabItems(studentProfile).map((item) => item.label)).toEqual(['首页', '跑腿', '二手', '店铺', '通知'])
  })
})
```

- [ ] **Step 2: Run navigation tests to verify they fail**

Run: `npm --prefix frontend run test -- src/config/navigation.test.ts`

Expected: FAIL because `frontend/src/config/navigation.ts` does not exist.

- [ ] **Step 3: Implement navigation config**

Create `frontend/src/config/navigation.ts`:

```ts
import type { IdentityKey, IdentityProfile } from '@/utils/identity'

export interface NavItem {
  path: string
  label: string
  description: string
  group: 'public' | 'account' | 'workspace' | 'admin'
  audiences: IdentityKey[]
  icon: string
  mobileTab?: 'guest' | 'auth' | 'always'
  requiresAuth?: boolean
  requiredRole?: Exclude<IdentityKey, 'guest' | 'student'>
  requiresAdmin?: boolean
  lockedTitle?: string
  lockedDescription?: string
  unlockRoute?: string
}

export interface NavGroup {
  key: NavItem['group']
  label: string
  items: NavItem[]
}

export const navItems: NavItem[] = [
  { path: '/', label: '首页', description: '身份工作台与平台总览', group: 'public', audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'], icon: '⌂', mobileTab: 'always' },
  { path: '/tasks', label: '悬赏跑腿', description: '浏览和承接校园代取任务', group: 'public', audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'], icon: '↗', mobileTab: 'always' },
  { path: '/goods', label: '二手商品', description: '浏览校园二手交易线索', group: 'public', audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'], icon: '◈', mobileTab: 'always' },
  { path: '/shops', label: '学生店铺', description: '预约学生技能服务', group: 'public', audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'], icon: '✦', mobileTab: 'always' },
  { path: '/project-ads', label: '项目广告', description: '发现项目组队、作品和社团招募', group: 'public', audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'], icon: '◎' },
  { path: '/policy', label: '协议与风险', description: '服务协议、隐私与交易风险说明', group: 'public', audiences: ['guest', 'student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'], icon: '§' },
  { path: '/auth', label: '登录注册', description: '使用校园邮箱加入 CampusHub', group: 'public', audiences: ['guest'], icon: '→', mobileTab: 'guest' },

  { path: '/wallet', label: '钱包中心', description: '余额、充值、提现和冻结记录', group: 'account', audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'], icon: '¥', requiresAuth: true },
  { path: '/notifications', label: '站内通知', description: '查看身份、交易、治理和支付提醒', group: 'account', audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'], icon: '●', mobileTab: 'auth', requiresAuth: true },
  { path: '/credit', label: '信用中心', description: '信用分、限制、违规和举报状态', group: 'account', audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'], icon: '☆', requiresAuth: true },
  { path: '/roles', label: '身份解锁', description: '申请跑腿、二手发布者和店铺商家身份', group: 'account', audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'], icon: '◆', requiresAuth: true },

  { path: '/tasks/1/workspace', label: '跑腿工作台', description: '处理已接跑腿任务', group: 'workspace', audiences: ['runner', 'admin'], icon: '⇄', requiresAuth: true, requiredRole: 'runner', lockedTitle: '需要跑腿接单者身份', lockedDescription: '支付 5 元保证金后可解锁跑腿接单能力。', unlockRoute: '/roles' },
  { path: '/goods/publish', label: '发布二手', description: '发布和管理二手商品', group: 'workspace', audiences: ['goodsPublisher', 'admin'], icon: '+', requiresAuth: true, requiredRole: 'goodsPublisher', lockedTitle: '需要二手发布者身份', lockedDescription: '支付 10 元保证金后可发布二手商品。', unlockRoute: '/roles' },
  { path: '/shops/merchant', label: '店铺工作台', description: '维护店铺、服务项目和预约', group: 'workspace', audiences: ['shopMerchant', 'admin'], icon: '店', requiresAuth: true, requiredRole: 'shopMerchant', lockedTitle: '需要店铺商家身份', lockedDescription: '支付 20 元保证金并通过管理员审核后可经营店铺。', unlockRoute: '/roles' },
  { path: '/project-ads/manage', label: '项目管理', description: '发布和管理项目广告', group: 'workspace', audiences: ['student', 'runner', 'goodsPublisher', 'shopMerchant', 'admin'], icon: '稿', requiresAuth: true },

  { path: '/admin/review', label: '内容审核', description: '处理平台内容审核事项', group: 'admin', audiences: ['admin'], icon: '审', requiresAuth: true, requiresAdmin: true },
  { path: '/admin/ops', label: '运营数据', description: '查看平台运营指标与导出', group: 'admin', audiences: ['admin'], icon: '数', requiresAuth: true, requiresAdmin: true },
  { path: '/admin/governance', label: '治理台', description: '处理举报、违规、信用和限制', group: 'admin', audiences: ['admin'], icon: '治', requiresAuth: true, requiresAdmin: true },
  { path: '/admin/payment', label: '支付监控', description: '查看支付订单和回调事件', group: 'admin', audiences: ['admin'], icon: '付', requiresAuth: true, requiresAdmin: true },
  { path: '/admin/wallet', label: '钱包运营', description: '审核充值、提现和冻结记录', group: 'admin', audiences: ['admin'], icon: '账', requiresAuth: true, requiresAdmin: true },
]

const groupLabels: Record<NavGroup['key'], string> = {
  public: '公开服务',
  account: '我的校园',
  workspace: '身份工作台',
  admin: '管理后台',
}

const groupOrder: NavGroup['key'][] = ['public', 'account', 'workspace', 'admin']

export function canSeeNavItem(profile: IdentityProfile, item: NavItem) {
  return item.audiences.some((audience) => profile.identities.some((identity) => identity.key === audience))
}

export function getVisibleNavGroups(profile: IdentityProfile): NavGroup[] {
  return groupOrder
    .map((group) => ({
      key: group,
      label: groupLabels[group],
      items: navItems.filter((item) => item.group === group && canSeeNavItem(profile, item)),
    }))
    .filter((group) => group.items.length > 0)
}

export function getMobileTabItems(profile: IdentityProfile) {
  return navItems.filter((item) => {
    if (!canSeeNavItem(profile, item)) return false
    if (item.mobileTab === 'always') return ['/', '/tasks', '/goods', '/shops'].includes(item.path)
    if (item.mobileTab === 'guest') return profile.primaryIdentity === 'guest'
    if (item.mobileTab === 'auth') return profile.primaryIdentity !== 'guest'
    return false
  }).slice(0, 5)
}

export function findNavItemByPath(path: string) {
  return navItems.find((item) => item.path === path)
}
```

- [ ] **Step 4: Run navigation tests to verify they pass**

Run: `npm --prefix frontend run test -- src/config/navigation.test.ts`

Expected: PASS for all navigation config tests.

- [ ] **Step 5: Extend router meta and guard**

Modify `frontend/src/router/index.ts` route records for role-protected paths:

```ts
        { path: 'goods/publish', name: 'goods-publish', component: GoodsPublishView, meta: { requiresAuth: true, requiredRole: 'goodsPublisher', unlockRoute: '/roles', lockedTitle: '需要二手发布者身份' } },
        { path: 'tasks/:id/workspace', name: 'task-workspace', component: TaskWorkspaceView, meta: { requiresAuth: true, requiredRole: 'runner', unlockRoute: '/roles', lockedTitle: '需要跑腿接单者身份' } },
        { path: 'shops/merchant', name: 'shop-merchant', component: ShopMerchantView, meta: { requiresAuth: true, requiredRole: 'shopMerchant', unlockRoute: '/roles', lockedTitle: '需要店铺商家身份' } },
```

Add imports near the top:

```ts
import { hasAnyRole, type IdentityKey } from '@/utils/identity'
```

Replace the `router.beforeEach` body with:

```ts
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (auth.token && !auth.currentUser && !auth.sessionLoaded) {
    await auth.loadCurrentUser()
  }
  if (to.meta.requiresAdmin) {
    if (!auth.isAuthenticated) {
      ElMessage.warning('请先登录，登录后可继续访问管理页面')
      return { name: 'auth', query: { redirect: to.fullPath } }
    }
    if (!auth.isAdmin) {
      ElMessage.error('当前账号不是管理员，请使用管理员账号或返回首页')
      return { name: 'home', query: { locked: 'admin' } }
    }
  }
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    ElMessage.warning('请先登录，登录后可继续操作')
    return { name: 'auth', query: { redirect: to.fullPath } }
  }
  const requiredRole = to.meta.requiredRole as IdentityKey | undefined
  if (requiredRole && !hasAnyRole(auth.currentUser?.roles, [requiredRole])) {
    ElMessage.warning(String(to.meta.lockedTitle || '当前身份尚未解锁该能力'))
    return { path: String(to.meta.unlockRoute || '/roles'), query: { locked: requiredRole, redirect: to.fullPath } }
  }
  return true
})
```

Add this declaration in `frontend/src/router/index.ts` after imports to type route meta:

```ts
declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    requiresAdmin?: boolean
    requiredRole?: IdentityKey
    unlockRoute?: string
    lockedTitle?: string
  }
}
```

- [ ] **Step 6: Run navigation and client tests**

Run: `npm --prefix frontend run test -- src/config/navigation.test.ts src/utils/identity.test.ts src/api/client.test.ts`

Expected: PASS.

- [ ] **Step 7: Commit Task 2**

```bash
git add frontend/src/config/navigation.ts frontend/src/config/navigation.test.ts frontend/src/router/index.ts
git commit -m "add identity aware navigation config"
```

---

### Task 3: Shared identity UI components

**Files:**
- Create: `frontend/src/components/common/IdentityBadge.vue`
- Create: `frontend/src/components/common/CapabilityCard.vue`
- Create: `frontend/src/components/common/LockedState.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Create IdentityBadge component**

Create `frontend/src/components/common/IdentityBadge.vue`:

```vue
<template>
  <div class="identity-badges" :class="{ 'identity-badges-compact': compact }">
    <span v-for="identity in identities" :key="identity.key" class="identity-badge" :class="`identity-badge-${identity.key}`">
      {{ compact ? identity.shortLabel : identity.label }}
    </span>
  </div>
</template>

<script setup lang="ts">
import type { IdentityItem } from '@/utils/identity'

withDefaults(defineProps<{
  identities: IdentityItem[]
  compact?: boolean
}>(), {
  compact: false,
})
</script>
```

- [ ] **Step 2: Create CapabilityCard component**

Create `frontend/src/components/common/CapabilityCard.vue`:

```vue
<template>
  <article class="capability-card" :class="[`capability-card-${tone}`, { 'capability-card-locked': locked }]">
    <div class="capability-icon">{{ icon }}</div>
    <div class="capability-content">
      <div class="panel-topline">
        <strong>{{ title }}</strong>
        <el-tag v-if="badge" :type="locked ? 'warning' : 'success'">{{ badge }}</el-tag>
      </div>
      <p>{{ description }}</p>
      <div v-if="$slots.meta" class="capability-meta">
        <slot name="meta" />
      </div>
      <div v-if="$slots.actions" class="card-actions">
        <slot name="actions" />
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  icon: string
  title: string
  description: string
  badge?: string
  tone?: 'blue' | 'green' | 'orange' | 'dark'
  locked?: boolean
}>(), {
  tone: 'blue',
  badge: '',
  locked: false,
})
</script>
```

- [ ] **Step 3: Create LockedState component**

Create `frontend/src/components/common/LockedState.vue`:

```vue
<template>
  <section class="locked-state">
    <div class="locked-state-icon">{{ icon }}</div>
    <p class="eyebrow">{{ eyebrow }}</p>
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
    <div class="state-actions">
      <el-button v-if="primaryText && primaryTo" type="primary" @click="router.push(primaryTo)">{{ primaryText }}</el-button>
      <el-button v-if="secondaryText && secondaryTo" plain @click="router.push(secondaryTo)">{{ secondaryText }}</el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

withDefaults(defineProps<{
  icon?: string
  eyebrow?: string
  title: string
  description: string
  primaryText?: string
  primaryTo?: string
  secondaryText?: string
  secondaryTo?: string
}>(), {
  icon: '🔒',
  eyebrow: 'Capability Locked',
  primaryText: '',
  primaryTo: '',
  secondaryText: '',
  secondaryTo: '',
})

const router = useRouter()
</script>
```

- [ ] **Step 4: Add component styles**

Append to `frontend/src/styles.css` before the first `@media` block:

```css
.identity-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.identity-badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 3px 9px;
  border-radius: 999px;
  color: #1e3a8a;
  background: #dbeafe;
  font-size: 12px;
  font-weight: 700;
}

.identity-badges-compact .identity-badge {
  min-height: 22px;
  padding: 2px 8px;
}

.identity-badge-admin {
  color: #f8fafc;
  background: #0f172a;
}

.identity-badge-runner,
.identity-badge-shopMerchant {
  color: #065f46;
  background: #d1fae5;
}

.identity-badge-goodsPublisher {
  color: #9a3412;
  background: #ffedd5;
}

.capability-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 14px;
  min-width: 0;
  padding: 18px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.08);
}

.capability-card-locked {
  background: #fff7ed;
}

.capability-icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 16px;
  color: #ffffff;
  background: linear-gradient(135deg, #2563eb, #14b8a6);
  font-weight: 800;
}

.capability-card-orange .capability-icon {
  background: linear-gradient(135deg, #f97316, #facc15);
}

.capability-card-green .capability-icon {
  background: linear-gradient(135deg, #059669, #2dd4bf);
}

.capability-card-dark .capability-icon {
  background: linear-gradient(135deg, #0f172a, #475569);
}

.capability-content {
  min-width: 0;
}

.capability-content p,
.capability-meta {
  color: #64748b;
  line-height: 1.7;
}

.locked-state {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 28px;
  border: 1px solid rgba(251, 146, 60, 0.35);
  border-radius: 26px;
  background: linear-gradient(135deg, #fff7ed, #ffffff);
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.08);
}

.locked-state-icon {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 18px;
  background: #fed7aa;
  font-size: 24px;
}

.locked-state h2,
.locked-state p {
  margin: 0;
}
```

- [ ] **Step 5: Run frontend build check**

Run: `npm --prefix frontend run build`

Expected: PASS. Known Vite large chunk and dependency pure-comment warnings are acceptable.

- [ ] **Step 6: Commit Task 3**

```bash
git add frontend/src/components/common/IdentityBadge.vue frontend/src/components/common/CapabilityCard.vue frontend/src/components/common/LockedState.vue frontend/src/styles.css
git commit -m "add shared identity ui components"
```

---

### Task 4: Identity-aware app shell

**Files:**
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Replace MainLayout template**

Replace `frontend/src/layouts/MainLayout.vue` template with:

```vue
<template>
  <el-container class="app-shell">
    <el-aside width="272px" class="sidebar desktop-sidebar">
      <div class="brand brand-premium">
        <span class="brand-mark">CH</span>
        <div>
          <strong>校集 CampusHub</strong>
          <span>真实校园服务运营平台</span>
        </div>
      </div>

      <div class="sidebar-identity">
        <p class="eyebrow">Current Identity</p>
        <strong>{{ identityProfile.displayName }}</strong>
        <IdentityBadge :identities="identityProfile.identities" compact />
      </div>

      <nav class="nav-groups" aria-label="桌面端主导航">
        <section v-for="group in navGroups" :key="group.key" class="nav-group">
          <p>{{ group.label }}</p>
          <RouterLink v-for="item in group.items" :key="item.path" :to="item.path" class="nav-link">
            <span class="nav-link-icon">{{ item.icon }}</span>
            <span>
              <strong>{{ item.label }}</strong>
              <small>{{ item.description }}</small>
            </span>
          </RouterLink>
        </section>
      </nav>
    </el-aside>

    <el-container>
      <el-header class="header premium-header">
        <div class="mobile-nav-trigger">
          <el-button plain @click="mobileMenuOpen = true">完整目录</el-button>
        </div>
        <div class="header-copy">
          <p class="eyebrow">Campus Service OS</p>
          <h1>校集 CampusHub</h1>
          <p>二手交易 × 跑腿悬赏 × 学生店铺 × 信用治理 × 钱包运营</p>
        </div>
        <div class="session-box premium-session-box">
          <IdentityBadge :identities="identityProfile.identities" compact />
          <span v-if="auth.currentUser">{{ auth.currentUser.nickname }}</span>
          <span v-else>未登录游客</span>
          <el-button v-if="auth.token" size="small" plain @click="auth.clearSession()">退出</el-button>
          <el-button v-else size="small" type="primary" @click="$router.push('/auth')">登录注册</el-button>
        </div>
      </el-header>
      <el-main class="main-content">
        <RouterView />
      </el-main>
    </el-container>

    <nav class="mobile-tabbar" aria-label="移动端主导航">
      <RouterLink v-for="item in mobileTabItems" :key="item.path" :to="item.path" class="mobile-tabbar-item">
        <span class="mobile-tabbar-icon">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </RouterLink>
      <button type="button" class="mobile-tabbar-item mobile-tabbar-button" @click="mobileMenuOpen = true">
        <span class="mobile-tabbar-icon">☰</span>
        <span>更多</span>
      </button>
    </nav>

    <el-drawer v-model="mobileMenuOpen" title="校集 CampusHub" direction="ltr" size="86%" class="mobile-menu-drawer">
      <div class="mobile-directory">
        <div class="sidebar-identity mobile-directory-identity">
          <p class="eyebrow">当前身份</p>
          <strong>{{ identityProfile.displayName }}</strong>
          <IdentityBadge :identities="identityProfile.identities" compact />
        </div>
        <section v-for="group in navGroups" :key="group.key" class="mobile-directory-group">
          <p>{{ group.label }}</p>
          <RouterLink v-for="item in group.items" :key="item.path" :to="item.path" class="mobile-directory-link" @click="mobileMenuOpen = false">
            <span>{{ item.icon }}</span>
            <strong>{{ item.label }}</strong>
            <small>{{ item.description }}</small>
          </RouterLink>
        </section>
      </div>
    </el-drawer>
  </el-container>
</template>
```

- [ ] **Step 2: Replace MainLayout script**

Replace the script block with:

```vue
<script setup lang="ts">
import { computed, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getMobileTabItems, getVisibleNavGroups } from '@/config/navigation'
import IdentityBadge from '@/components/common/IdentityBadge.vue'

const auth = useAuthStore()
const mobileMenuOpen = ref(false)
const identityProfile = computed(() => auth.identityProfile)
const navGroups = computed(() => getVisibleNavGroups(identityProfile.value))
const mobileTabItems = computed(() => getMobileTabItems(identityProfile.value))
</script>
```

- [ ] **Step 3: Add shell styles**

Add to `frontend/src/styles.css` near existing sidebar/header styles, replacing conflicting `.brand`, `.header`, `.session-box`, `.menu` expectations where needed:

```css
.brand-premium {
  flex-direction: row;
  align-items: center;
  gap: 12px;
  color: #f8fafc;
  background: radial-gradient(circle at top left, rgba(20, 184, 166, 0.35), transparent 42%), #0f172a;
  border-bottom: 1px solid rgba(148, 163, 184, 0.22);
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 16px;
  color: #0f172a;
  background: linear-gradient(135deg, #ffffff, #99f6e4);
  font-weight: 900;
}

.sidebar {
  background: #0f172a;
  border-right: 0;
}

.sidebar-identity {
  display: grid;
  gap: 10px;
  margin: 16px;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 20px;
  color: #f8fafc;
  background: rgba(15, 23, 42, 0.68);
}

.sidebar-identity strong {
  font-size: 18px;
}

.nav-groups {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 0 12px 24px;
}

.nav-group p,
.mobile-directory-group p {
  margin: 0 0 8px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.nav-link {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  padding: 11px 12px;
  border-radius: 16px;
  color: #cbd5e1;
  text-decoration: none;
}

.nav-link:hover,
.nav-link.router-link-active,
.nav-link.router-link-exact-active {
  color: #ffffff;
  background: rgba(37, 99, 235, 0.32);
}

.nav-link-icon {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.1);
}

.nav-link strong,
.nav-link small {
  display: block;
}

.nav-link small {
  margin-top: 3px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.35;
}

.premium-header {
  height: 112px;
  background: radial-gradient(circle at top left, rgba(45, 212, 191, 0.45), transparent 32%), linear-gradient(135deg, #1e3a8a, #0f172a);
}

.header-copy h1 {
  margin: 2px 0 8px;
}

.premium-session-box {
  justify-content: flex-end;
  max-width: 360px;
}

.mobile-directory {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.mobile-directory-identity {
  margin: 0;
  color: #0f172a;
  background: linear-gradient(135deg, #eff6ff, #ecfeff);
}

.mobile-directory-link {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 4px 10px;
  padding: 13px 0;
  border-bottom: 1px solid #eef2f7;
  color: #0f172a;
  text-decoration: none;
}

.mobile-directory-link span {
  grid-row: span 2;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 12px;
  color: #2563eb;
  background: #eff6ff;
}

.mobile-directory-link small {
  color: #64748b;
  line-height: 1.4;
}
```

- [ ] **Step 4: Run layout build check**

Run: `npm --prefix frontend run build`

Expected: PASS with only known Vite warnings.

- [ ] **Step 5: Manual browser check if dev server is available**

Run: `npm --prefix frontend run dev -- --host 127.0.0.1`

Open `http://127.0.0.1:5173/` and check:

- Guest sidebar does not show wallet/admin entries.
- Mobile width shows bottom tabs and “更多”.
- Opening “更多” shows grouped directory.

Stop the dev server after the check.

- [ ] **Step 6: Commit Task 4**

```bash
git add frontend/src/layouts/MainLayout.vue frontend/src/styles.css
git commit -m "upgrade app shell navigation"
```

---

### Task 5: Identity-aware home page

**Files:**
- Modify: `frontend/src/views/HomeView.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Replace HomeView with identity workbench**

Replace `frontend/src/views/HomeView.vue` with:

```vue
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
    to: hasIdentity('runner') ? '/tasks' : '/roles',
    action: hasIdentity('runner') ? '进入跑腿大厅' : '申请身份',
  },
  {
    icon: '二',
    title: '二手发布者',
    description: hasIdentity('goodsPublisher') ? '你可以发布和管理二手商品。' : '支付 10 元保证金后自动开通，用于可信二手发布。',
    badge: hasIdentity('goodsPublisher') ? '已解锁' : '可申请',
    tone: 'orange' as const,
    locked: !hasIdentity('goodsPublisher'),
    to: hasIdentity('goodsPublisher') ? '/goods/publish' : '/roles',
    action: hasIdentity('goodsPublisher') ? '发布二手' : '申请身份',
  },
  {
    icon: '店',
    title: '店铺商家',
    description: hasIdentity('shopMerchant') ? '你可以维护店铺、服务项目和预约。' : '支付 20 元保证金并通过人工审核后开通。',
    badge: hasIdentity('shopMerchant') ? '已解锁' : '需审核',
    tone: 'blue' as const,
    locked: !hasIdentity('shopMerchant'),
    to: hasIdentity('shopMerchant') ? '/shops/merchant' : '/roles',
    action: hasIdentity('shopMerchant') ? '店铺工作台' : '申请身份',
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
  if (!auth.isAuthenticated) {
    return [
      { title: '注册或登录', description: '使用校园邮箱加入，继续后续充值和身份申请。', to: '/auth' },
      { title: '先浏览服务', description: '看看当前校园二手、跑腿、店铺和项目广告。', to: '/tasks' },
      { title: '了解风险边界', description: '阅读平台资金、隐私和交易风险说明。', to: '/policy' },
    ]
  }
  if (hasIdentity('admin')) {
    return [
      { title: '查看运营数据', description: '进入跨业务运营看板和导出入口。', to: '/admin/ops' },
      { title: '处理治理队列', description: '查看举报、违规、信用和限制。', to: '/admin/governance' },
      { title: '查看支付监控', description: '确认支付订单和回调事件状态。', to: '/admin/payment' },
    ]
  }
  return [
    { title: '充值钱包', description: '使用支付宝支付中心或微信人工充值。', to: '/wallet' },
    { title: '申请身份', description: '解锁跑腿、二手发布或店铺商家能力。', to: '/roles' },
    { title: '查看通知', description: '关注身份、支付、治理和交易提醒。', to: '/notifications' },
  ]
})
</script>
```

- [ ] **Step 2: Add home styles**

Append to `frontend/src/styles.css` before media rules:

```css
.home-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 24px;
  padding: 30px;
  border-radius: 30px;
  color: #ffffff;
  background: radial-gradient(circle at top right, rgba(45, 212, 191, 0.45), transparent 34%), linear-gradient(135deg, #1e3a8a, #0f172a);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.18);
}

.home-hero h2 {
  max-width: 760px;
  margin: 8px 0 12px;
  font-size: clamp(30px, 5vw, 54px);
  line-height: 1.08;
}

.home-hero p {
  max-width: 720px;
  color: #dbeafe;
  line-height: 1.8;
}

.home-hero .eyebrow {
  color: #99f6e4;
}

.home-hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.home-hero-panel {
  align-self: stretch;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 10px;
  padding: 22px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.12);
}

.home-hero-panel strong {
  font-size: 40px;
}

.capability-grid,
.home-section-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.premium-panel {
  padding: 22px;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.06);
}

.premium-panel h3 {
  margin: 4px 0 12px;
}

.premium-panel p {
  color: #64748b;
  line-height: 1.8;
}

.action-list {
  display: grid;
  gap: 10px;
}

.action-list button {
  display: grid;
  gap: 4px;
  width: 100%;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  text-align: left;
  background: #f8fafc;
  cursor: pointer;
}

.action-list button:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}

.action-list span {
  color: #64748b;
}
```

Inside the existing `@media (max-width: 900px)` block, add:

```css
  .home-hero,
  .capability-grid,
  .home-section-grid {
    grid-template-columns: 1fr;
  }

  .home-hero {
    padding: 22px;
    border-radius: 24px;
  }
```

- [ ] **Step 3: Run build**

Run: `npm --prefix frontend run build`

Expected: PASS with only known Vite warnings.

- [ ] **Step 4: Commit Task 5**

```bash
git add frontend/src/views/HomeView.vue frontend/src/styles.css
git commit -m "redesign identity aware home page"
```

---

### Task 6: Auth, role unlock, and wallet polish

**Files:**
- Modify: `frontend/src/views/AuthView.vue`
- Modify: `frontend/src/views/RoleApplicationsView.vue`
- Modify: `frontend/src/views/WalletView.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Update AuthView copy and redirect behavior**

In `frontend/src/views/AuthView.vue`, preserve existing submit functions and forms. Update visible copy so the page explains:

```vue
<p class="eyebrow">Campus Identity</p>
<h2>用校园邮箱进入真实校园服务平台</h2>
<p>登录支持用户名或校园邮箱。注册需要邮箱验证码，并至少填写微信或 QQ，便于交易达成后双方按规则联系。</p>
```

After successful login, route to `route.query.redirect` if present. Add imports if missing:

```ts
import { useRoute, useRouter } from 'vue-router'
```

Use this after `await auth.login(...)` succeeds:

```ts
const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
router.push(redirect)
```

Expected behavior: unauthenticated users sent to `/auth?redirect=/wallet` return to `/wallet` after login.

- [ ] **Step 2: Add current identity summary to RoleApplicationsView**

Import components:

```ts
import { computed, reactive, ref } from 'vue'
import CapabilityCard from '@/components/common/CapabilityCard.vue'
import IdentityBadge from '@/components/common/IdentityBadge.vue'
```

Add in script:

```ts
const identityProfile = computed(() => auth.identityProfile)
const hasIdentity = (roleType: string) => {
  const roleMap: Record<string, string[]> = {
    RUNNER: ['ROLE_RUNNER', 'RUNNER'],
    GOODS_PUBLISHER: ['ROLE_GOODS_PUBLISHER', 'GOODS_PUBLISHER'],
    SHOP_MERCHANT: ['ROLE_SHOP_MERCHANT', 'SHOP_MERCHANT'],
  }
  return roleMap[roleType]?.some((role) => auth.currentUser?.roles.includes(role)) ?? false
}
```

At the top of the template after `.page-heading`, add:

```vue
<div class="premium-panel role-status-panel">
  <p class="eyebrow">Unlocked Capabilities</p>
  <h3>当前已解锁身份</h3>
  <IdentityBadge :identities="identityProfile.identities" />
  <p>跑腿和二手发布者支付保证金后自动开通；店铺商家支付保证金后进入人工审核。</p>
</div>
```

In each role card button, set disabled when already unlocked:

```vue
<el-button type="primary" :disabled="hasIdentity(role.roleType)" :loading="loadingRole === role.roleType" @click="submit(role.roleType)">
  {{ hasIdentity(role.roleType) ? '已解锁' : '申请开通' }}
</el-button>
```

- [ ] **Step 3: Improve wallet page copy without changing payment actions**

In `frontend/src/views/WalletView.vue`, preserve existing recharge and payment functions. Update top heading copy to include:

```vue
<p class="eyebrow">Wallet Center</p>
<h2>钱包与充值</h2>
<p>支付宝充值通过 API-Transfer-Station 支付中心创建真实支付链接；微信充值进入人工审核，请备注校园邮箱或 CampusHub 用户名。</p>
```

Ensure existing UI still displays payment provider/order state and that `getRechargePaymentAction` behavior remains unchanged.

- [ ] **Step 4: Add account path styles**

Append to `frontend/src/styles.css`:

```css
.role-status-panel {
  display: grid;
  gap: 10px;
}

.role-status-panel h3,
.role-status-panel p {
  margin: 0;
}

.role-card {
  border-radius: 22px;
  overflow: hidden;
}

.role-card :deep(.el-card__header) {
  background: linear-gradient(135deg, #eff6ff, #f8fafc);
}
```

- [ ] **Step 5: Run focused tests and build**

Run: `npm --prefix frontend run test -- src/views/walletPaymentActions.test.ts src/utils/identity.test.ts src/config/navigation.test.ts`

Expected: PASS.

Run: `npm --prefix frontend run build`

Expected: PASS with only known Vite warnings.

- [ ] **Step 6: Commit Task 6**

```bash
git add frontend/src/views/AuthView.vue frontend/src/views/RoleApplicationsView.vue frontend/src/views/WalletView.vue frontend/src/styles.css
git commit -m "polish onboarding wallet and identity unlock"
```

---

### Task 7: Business route CTA and locked-state polish

**Files:**
- Modify: `frontend/src/views/TasksView.vue`
- Modify: `frontend/src/views/GoodsView.vue`
- Modify: `frontend/src/views/GoodsPublishView.vue`
- Modify: `frontend/src/views/ShopsView.vue`
- Modify: `frontend/src/views/ShopMerchantView.vue`
- Modify: `frontend/src/views/ProjectAdsView.vue`
- Modify: `frontend/src/views/ProjectAdManageView.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Add identity-aware CTA helpers to business list pages**

For each list page (`TasksView.vue`, `GoodsView.vue`, `ShopsView.vue`, `ProjectAdsView.vue`), add a computed CTA object near existing script setup state:

```ts
const primaryCta = computed(() => {
  if (!auth.currentUser) return { label: '登录后继续', to: '/auth' }
  if (auth.isAdmin) return { label: '进入运营后台', to: '/admin/ops' }
  return { label: '查看我的能力', to: '/roles' }
})
```

If the page already has a more specific action, specialize it:

- Tasks: runner users get `进入跑腿大厅` / `/tasks`; non-runner gets `申请跑腿身份` / `/roles`.
- Goods: goods publishers get `发布二手` / `/goods/publish`; non-publisher gets `申请二手发布者` / `/roles`.
- Shops: shop merchants get `店铺工作台` / `/shops/merchant`; non-merchant gets `申请店铺商家` / `/roles`.
- Project ads: logged-in users get `管理项目广告` / `/project-ads/manage`.

Use role checks with `auth.canAccessIdentity('runner')`, `auth.canAccessIdentity('goodsPublisher')`, and `auth.canAccessIdentity('shopMerchant')`.

- [ ] **Step 2: Add CTA cards to list page templates**

Below each page heading, add:

```vue
<div class="business-cta-card">
  <div>
    <p class="eyebrow">Recommended Next Step</p>
    <h3>{{ primaryCta.label }}</h3>
    <p>CampusHub 会根据登录状态和已解锁身份展示最适合的下一步入口。</p>
  </div>
  <el-button type="primary" @click="router.push(primaryCta.to)">{{ primaryCta.label }}</el-button>
</div>
```

Ensure `const router = useRouter()` exists in each script that uses the CTA.

- [ ] **Step 3: Use LockedState on role-gated workspaces**

In `GoodsPublishView.vue`, `ShopMerchantView.vue`, and `ProjectAdManageView.vue`, import `LockedState`:

```ts
import LockedState from '@/components/common/LockedState.vue'
```

For `GoodsPublishView.vue`, if the user lacks goods publisher identity, render:

```vue
<LockedState
  v-if="auth.currentUser && !auth.canAccessIdentity('goodsPublisher')"
  icon="二"
  title="需要二手发布者身份"
  description="支付 10 元保证金后可解锁二手商品发布能力。"
  primary-text="去申请身份"
  primary-to="/roles"
  secondary-text="返回二手市场"
  secondary-to="/goods"
/>
```

For `ShopMerchantView.vue`, if the user lacks shop merchant identity, render:

```vue
<LockedState
  v-if="auth.currentUser && !auth.canAccessIdentity('shopMerchant')"
  icon="店"
  title="需要店铺商家身份"
  description="支付 20 元保证金并通过管理员审核后，可创建店铺、维护服务项目并处理预约。"
  primary-text="去申请身份"
  primary-to="/roles"
  secondary-text="返回店铺市场"
  secondary-to="/shops"
/>
```

For `ProjectAdManageView.vue`, keep logged-in access but improve unauthenticated state:

```vue
<LockedState
  v-if="!auth.currentUser"
  icon="稿"
  title="登录后管理项目广告"
  description="登录后可以发布项目组队、作品展示、社团招募和活动宣传。"
  primary-text="登录注册"
  primary-to="/auth"
  secondary-text="返回项目广场"
  secondary-to="/project-ads"
/>
```

Wrap existing page content in `v-else` where needed so the locked state does not show alongside the form.

- [ ] **Step 4: Add business CTA styles**

Append to `frontend/src/styles.css`:

```css
.business-cta-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px;
  border: 1px solid #bfdbfe;
  border-radius: 22px;
  background: linear-gradient(135deg, #eff6ff, #ffffff);
}

.business-cta-card h3,
.business-cta-card p {
  margin: 0;
}

.business-cta-card p:last-child {
  color: #64748b;
  line-height: 1.7;
}
```

Inside the mobile media block add:

```css
  .business-cta-card {
    align-items: flex-start;
    flex-direction: column;
  }
```

- [ ] **Step 5: Run build**

Run: `npm --prefix frontend run build`

Expected: PASS with only known Vite warnings.

- [ ] **Step 6: Commit Task 7**

```bash
git add frontend/src/views/TasksView.vue frontend/src/views/GoodsView.vue frontend/src/views/GoodsPublishView.vue frontend/src/views/ShopsView.vue frontend/src/views/ShopMerchantView.vue frontend/src/views/ProjectAdsView.vue frontend/src/views/ProjectAdManageView.vue frontend/src/styles.css
git commit -m "add identity aware business ctas"
```

---

### Task 8: Admin and account visual consistency pass

**Files:**
- Modify: `frontend/src/views/NotificationsView.vue`
- Modify: `frontend/src/views/CreditCenterView.vue`
- Modify: `frontend/src/views/AdminOperationsView.vue`
- Modify: `frontend/src/views/AdminGovernanceView.vue`
- Modify: `frontend/src/views/AdminReviewView.vue`
- Modify: `frontend/src/views/AdminPaymentView.vue`
- Modify: `frontend/src/views/AdminWalletView.vue`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Add admin/account surface wrappers**

For each admin view, ensure the top page heading uses this pattern:

```vue
<div class="page-heading admin-page-heading">
  <div>
    <p class="eyebrow">Operations Console</p>
    <h2>页面中文标题</h2>
    <p>一句话说明本页运营用途，强调低影响处理和状态可追溯。</p>
  </div>
</div>
```

Use page-specific titles:

- `AdminOperationsView.vue`: `运营数据`
- `AdminGovernanceView.vue`: `治理台`
- `AdminReviewView.vue`: `内容审核`
- `AdminPaymentView.vue`: `支付监控`
- `AdminWalletView.vue`: `钱包运营`

For `NotificationsView.vue` use eyebrow `Message Center` and title `站内通知`.

For `CreditCenterView.vue` use eyebrow `Trust Center` and title `信用中心`.

- [ ] **Step 2: Add premium panel class to major cards**

Where admin/account views currently use `data-table`, `tabs-surface`, `timeline-surface`, `list-surface`, or `workspace-card`, keep existing class and add `premium-panel` to major top-level containers:

```vue
<el-card class="premium-panel" shadow="never">
```

or:

```vue
<div class="tabs-surface premium-panel">
```

Do not change table columns, API calls, or admin action logic.

- [ ] **Step 3: Add admin styles**

Append to `frontend/src/styles.css`:

```css
.admin-page-heading {
  padding: 22px;
  border: 1px solid #dbeafe;
  border-radius: 24px;
  background: linear-gradient(135deg, #eff6ff, #ffffff);
}

.admin-page-heading h2 {
  color: #0f172a;
}

.admin-page-heading p:last-child {
  max-width: 760px;
  line-height: 1.7;
}

.premium-panel .el-table,
.premium-panel .el-tabs {
  --el-border-color-lighter: #e2e8f0;
}
```

- [ ] **Step 4: Run build**

Run: `npm --prefix frontend run build`

Expected: PASS with only known Vite warnings.

- [ ] **Step 5: Commit Task 8**

```bash
git add frontend/src/views/NotificationsView.vue frontend/src/views/CreditCenterView.vue frontend/src/views/AdminOperationsView.vue frontend/src/views/AdminGovernanceView.vue frontend/src/views/AdminReviewView.vue frontend/src/views/AdminPaymentView.vue frontend/src/views/AdminWalletView.vue frontend/src/styles.css
git commit -m "polish account and admin surfaces"
```

---

### Task 9: Full frontend verification and documentation handoff

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`
- Verify: frontend tests/build, deployment, API smoke, Playwriter matrix

- [ ] **Step 1: Run frontend unit tests**

Run: `npm --prefix frontend run test -- src/api/client.test.ts src/views/walletPaymentActions.test.ts src/utils/identity.test.ts src/config/navigation.test.ts`

Expected: PASS.

- [ ] **Step 2: Run frontend production build**

Run: `npm --prefix frontend run build`

Expected: PASS. Known Vite large chunk and dependency pure-comment warnings are acceptable.

- [ ] **Step 3: Update README Phase 13 section**

Append after the Phase 12 section in `README.md`:

```md
## Phase 13 身份感知 UI / UX 终极优化

Phase 13 不新增业务闭环，而是把已上线平台升级为更像真实运营产品的身份感知 Web 体验。

已落地能力：

- 全局视觉升级为更统一的校园服务平台品牌：高级侧栏、身份区、卡片体系、按钮层级、空状态和后台 surface；
- 前端复用 `/api/auth/me` 返回的角色，区分游客、普通学生、跑腿接单者、二手发布者、店铺商家和管理员；
- 桌面侧栏、移动底部 tab 和“更多”目录按身份过滤，不再让游客和普通学生看到所有后台入口；
- 首页升级为身份感知 landing / workbench，按登录状态和角色展示下一步建议；
- 登录注册、钱包充值、身份申请、业务列表/工作台和后台页面完成视觉与引导文案打磨；
- 支付宝充值仍通过 API-Transfer-Station payment-center 返回真实支付链接，微信充值仍展示人工收款二维码和邮箱/用户名备注说明。

Phase 13 不做后端权限模型重写、不新增数据库迁移、不替换 Element Plus、不做小程序或原生 App。生产支付边界不变：CampusHub 不读取、不复制、不保存支付宝密钥、SMTP 密码、JWT secret、payment-center token/signing secret 或服务器 `.env` 正文。
```

- [ ] **Step 4: Update CLAUDE.md handoff**

Append a new section to `CLAUDE.md`:

```md
## Latest Phase 13 identity-aware UI handoff, 2026-05-24

Latest Phase 13 work upgrades CampusHub's frontend into a more polished identity-aware campus service platform without changing backend schema or payment boundaries.

Implemented Phase 13:

- New docs: `docs/superpowers/specs/2026-05-24-campushub-phase13-identity-aware-ui-design.md` and `docs/superpowers/plans/2026-05-24-campushub-phase13-identity-aware-ui-upgrade.md`.
- Frontend identity utilities derive guest/student/runner/goods-publisher/shop-merchant/admin presentation from `/api/auth/me` roles.
- Desktop sidebar, mobile bottom tabs, mobile directory drawer, homepage shortcuts, and locked-state guidance are generated from shared identity-aware navigation config.
- Homepage now acts as a role-aware landing/workbench; auth, wallet, role unlock, business surfaces, notifications, credit, and admin pages received visual/UX polish.
- Phase 13 does not add a Flyway migration and does not change CampusHub's payment-center boundary.

Verification expected before completion:

- `npm --prefix frontend run test -- src/api/client.test.ts src/views/walletPaymentActions.test.ts src/utils/identity.test.ts src/config/navigation.test.ts`
- `npm --prefix frontend run build`
- Server-side frontend Docker build/restart after push.
- Server-local API smoke for public routes, payment service-fee route, ops/governance/credit routes.
- Playwriter checks for guest, normal student, and `yeshenghao@mail.ustc.edu.cn` admin views; 390x844 mobile without document-level horizontal overflow; Alipay/WeChat recharge entries remain visible and non-mock.
```

- [ ] **Step 5: Commit documentation**

```bash
git add README.md CLAUDE.md docs/superpowers/specs/2026-05-24-campushub-phase13-identity-aware-ui-design.md docs/superpowers/plans/2026-05-24-campushub-phase13-identity-aware-ui-upgrade.md
git commit -m "document phase 13 identity aware ui"
```

- [ ] **Step 6: Push to GitHub only after local verification passes**

Run: `git status --short`

Expected: only intended tracked changes are committed; untracked `.claude/`, `.superpowers/`, LaTeX/backup/handoff files remain uncommitted.

Run: `git push origin master`

Expected: push succeeds.

- [ ] **Step 7: Low-impact production deploy**

On server `/opt/campushub`, do not print `.env` or secrets. Pull and rebuild only what changed:

```bash
ssh root@38.76.179.17 'cd /opt/campushub && git pull --ff-only && docker compose -f docker-compose.prod.yml build web && docker compose -f docker-compose.prod.yml up -d web'
```

Expected: web image builds successfully and web container restarts. Backend and MySQL should remain running.

- [ ] **Step 8: Server-local API smoke**

Run a low-frequency server-local smoke without printing secrets:

```bash
ssh root@38.76.179.17 'cd /opt/campushub && for path in /api/goods /api/tasks /api/shops /api/project-ads /api/payment/service-fees /api/admin/ops/analytics/overview /api/admin/governance/dashboard /api/credit/users/1; do code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080$path); printf "%s %s\n" "$code" "$path"; done'
```

Expected: public routes return 200; protected admin/credit routes may return 401/403 without auth if Phase 10 security applies. Treat 401/403 for protected anonymous requests as acceptable if public routes still return 200 and authenticated smoke covers protected routes.

- [ ] **Step 9: Playwriter verification matrix**

Use Playwriter browser verification on production:

Guest desktop:

- `/`
- `/tasks`
- `/goods`
- `/shops`
- `/project-ads`
- `/auth`
- `/policy`

Expected: no white screen; guest navigation does not show wallet/admin entries; login/register CTA visible.

Normal student desktop:

- Login with configured beta student account.
- `/`
- `/wallet`
- `/roles`
- `/notifications`
- `/credit`

Expected: student navigation shows account entries; admin entries hidden; wallet shows Alipay payment-center/continue-pay behavior and WeChat recharge entry.

Admin desktop:

- Login as `yeshenghao@mail.ustc.edu.cn` with user-known password.
- `/`
- `/admin/ops`
- `/admin/governance`
- `/admin/payment`
- `/admin/wallet`

Expected: admin navigation shows management entries; pages render without white screens.

Mobile 390x844:

- `/`
- `/tasks`
- `/goods`
- `/shops`
- `/project-ads`
- `/wallet`
- `/roles`
- `/admin/ops`

Expected: bottom tab and “更多” directory render; document-level `scrollWidth === clientWidth`; no horizontal overflow; identity-filtered navigation differs between guest/student/admin.

- [ ] **Step 10: Final commit or handoff update if verification changes docs**

If verification results require adjusting `CLAUDE.md`, update only the handoff section and commit:

```bash
git add CLAUDE.md
git commit -m "update phase 13 verification handoff"
git push origin master
```

Expected: documentation accurately reflects actual verification evidence.

---

## Self-review checklist

- [ ] Spec coverage: visual branding, identity segmentation, filtered desktop/mobile navigation, permission guidance, new-user paths, admin polish, payment regression, and mobile verification are covered by Tasks 1-9.
- [ ] Placeholder scan: the plan contains no `TBD`, `TODO`, or vague “handle later” steps.
- [ ] Type consistency: `IdentityKey`, `IdentityProfile`, `NavItem`, `requiredRole`, `identityProfile`, `canAccessIdentity`, and component prop names are defined before use and stay consistent across tasks.
- [ ] Scope control: no backend migration, no Element Plus replacement, no payment-center boundary change, no production secret reads.
- [ ] Verification: unit tests, frontend build, server deploy, API smoke, and Playwriter guest/student/admin/mobile matrix are explicit.
