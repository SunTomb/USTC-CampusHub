import { describe, expect, it } from 'vitest'

import { canBypassRequiredRole } from '@/utils/identity'

describe('role guard helpers', () => {
  it('allows only master admin to bypass domain-specific required roles', () => {
    expect(canBypassRequiredRole(['ROLE_MASTER_ADMIN'])).toBe(true)
    expect(canBypassRequiredRole(['ROLE_ADMIN'])).toBe(false)
    expect(canBypassRequiredRole(['ROLE_TRADE_ADMIN'])).toBe(false)
    expect(canBypassRequiredRole(['ROLE_SHOWCASE_ADMIN'])).toBe(false)
  })
})
