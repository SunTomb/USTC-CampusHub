import { describe, expect, it } from 'vitest'

import { getVisibleNavGroups, getMobileTabItems, navItems } from './navigation'
import { buildIdentityProfile } from '@/utils/identity'

const guestProfile = buildIdentityProfile(null)
const studentProfile = buildIdentityProfile({ id: 1, username: 'student', nickname: '学生', roles: ['ROLE_STUDENT'] })
const adminProfile = buildIdentityProfile({ id: 2, username: 'admin', nickname: '管理员', roles: ['ROLE_STUDENT', 'ROLE_ADMIN'] })

describe('navigation config', () => {
  it('does not expose task workspaces as static navigation links', () => {
    expect(navItems.map((item) => item.path)).not.toContain('/tasks/1/workspace')
  })

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
