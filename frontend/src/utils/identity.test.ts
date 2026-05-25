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

  it('recognizes domain admin roles without generic admin visibility', () => {
    const masterProfile = buildIdentityProfile({ id: 3, username: 'master', nickname: 'Master', roles: ['ROLE_MASTER_ADMIN'] })
    const tradeProfile = buildIdentityProfile({ id: 4, username: 'trade', nickname: 'Trade', roles: ['ROLE_TRADE_ADMIN'] })
    const showcaseProfile = buildIdentityProfile({
      id: 5,
      username: 'showcase',
      nickname: 'Showcase',
      roles: ['ROLE_SHOWCASE_ADMIN'],
    })

    expect(masterProfile.primaryIdentity).toBe('masterAdmin')
    expect(masterProfile.identities.map((item) => item.key)).toEqual(['student', 'admin', 'masterAdmin'])
    expect(tradeProfile.primaryIdentity).toBe('tradeAdmin')
    expect(tradeProfile.identities.map((item) => item.key)).toEqual(['student', 'tradeAdmin'])
    expect(showcaseProfile.primaryIdentity).toBe('showcaseAdmin')
    expect(showcaseProfile.identities.map((item) => item.key)).toEqual(['student', 'showcaseAdmin'])
    expect(hasAnyRole(['ROLE_MASTER_ADMIN'], ['admin'])).toBe(true)
    expect(hasAnyRole(['ROLE_TRADE_ADMIN'], ['admin'])).toBe(false)
    expect(hasAnyRole(['ROLE_SHOWCASE_ADMIN'], ['admin'])).toBe(false)
    expect(hasAnyRole(['ROLE_TRADE_ADMIN'], ['tradeAdmin'])).toBe(true)
    expect(hasAnyRole(['ROLE_SHOWCASE_ADMIN'], ['showcaseAdmin'])).toBe(true)
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
    expect(roleDisplayName('tradeAdmin')).toBe('交易管理员')
    expect(roleDisplayName('showcaseAdmin')).toBe('展示管理员')
    expect(roleDisplayName('masterAdmin')).toBe('最高级系统管理员')
  })
})
