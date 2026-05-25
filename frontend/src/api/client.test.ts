import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AxiosError } from 'axios'

import { apiClient, unwrapApiResponse, type ApiResponse } from './client'

const clearSession = vi.fn()

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    token: 'existing-token',
    clearSession,
  }),
}))

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

describe('apiClient auth expiration handling', () => {
  beforeEach(() => {
    clearSession.mockClear()
  })

  it('clears the session and shows expiration message on normal 401 responses', async () => {
    const error = {
      config: {},
      response: { status: 401, data: { message: '请先登录' } },
    } as AxiosError<ApiResponse<never>>

    const handler = apiClient.interceptors.response.handlers?.[0].rejected

    await expect(handler?.(error)).rejects.toThrow('登录已过期，请重新登录')
    expect(clearSession).toHaveBeenCalledOnce()
  })

  it('keeps the session and uses backend message when auth expiration handling is skipped', async () => {
    const error = {
      config: { skipAuthExpireHandling: true },
      response: { status: 401, data: { message: '请先登录' } },
    } as AxiosError<ApiResponse<never>>

    const handler = apiClient.interceptors.response.handlers?.[0].rejected

    await expect(handler?.(error)).rejects.toThrow('请先登录')
    expect(clearSession).not.toHaveBeenCalled()
  })
})
