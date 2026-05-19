import { describe, expect, it } from 'vitest'

import { unwrapApiResponse, type ApiResponse } from './client'

describe('unwrapApiResponse', () => {
  it('returns data when backend response succeeds', () => {
    const response: ApiResponse<{ id: number }> = { success: true, data: { id: 1 }, message: 'ok' }

    expect(unwrapApiResponse(response)).toEqual({ id: 1 })
  })

  it('throws backend message when response fails', () => {
    const response: ApiResponse<never> = { success: false, message: '邮箱验证码错误' }

    expect(() => unwrapApiResponse(response)).toThrow('邮箱验证码错误')
  })
})
