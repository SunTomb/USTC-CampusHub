import { describe, expect, it } from 'vitest'

import { FINANCIAL_FEATURE_UNAVAILABLE_MESSAGE, isFinancialFeatureEnabled } from './featureFlags'

describe('financial feature flags', () => {
  it('keeps financial writes disabled by default', () => {
    expect(isFinancialFeatureEnabled(undefined)).toBe(false)
    expect(isFinancialFeatureEnabled('false')).toBe(false)
    expect(isFinancialFeatureEnabled('true')).toBe(true)
  })

  it('provides a stable unavailable message for the closed state', () => {
    expect(FINANCIAL_FEATURE_UNAVAILABLE_MESSAGE).toContain('暂未开放')
  })
})
