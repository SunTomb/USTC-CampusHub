import { describe, expect, it } from 'vitest'
import { roleActivationMode, roleRequiresAgreement } from './roleAgreements'

describe('role agreements', () => {
  it('requires role agreements only for the three business identities', () => {
    for (const role of ['RUNNER', 'GOODS_PUBLISHER', 'SHOP_MERCHANT']) expect(roleRequiresAgreement(role)).toBe(true)
    for (const role of ['TRADE_ADMIN', 'SHOWCASE_ADMIN', 'constructor', 'toString']) expect(roleRequiresAgreement(role)).toBe(false)
  })
  it('approves runners and publishers automatically and reviews merchants', () => {
    expect(roleActivationMode('RUNNER')).toBe('AUTO_APPROVE')
    expect(roleActivationMode('GOODS_PUBLISHER')).toBe('AUTO_APPROVE')
    expect(roleActivationMode('SHOP_MERCHANT')).toBe('PENDING_REVIEW')
    expect(roleActivationMode('TRADE_ADMIN')).toBeNull()
  })
})
