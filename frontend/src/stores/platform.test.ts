import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
const request = vi.hoisted(() => vi.fn())
vi.mock('@/api/client', () => ({ getApi: request }))
import { usePlatformStore } from './platform'

beforeEach(() => { setActivePinia(createPinia()); request.mockReset() })

describe('platform feature availability', () => {
  it('fails closed when the availability endpoint cannot load', async () => {
    request.mockRejectedValueOnce(new Error('offline'))
    const store = usePlatformStore()
    expect(store.financialEnabled).toBe(false)
    await store.load()
    expect(store.financialEnabled).toBe(false)
  })
  it('only accepts an explicit boolean true from the backend', async () => {
    request.mockResolvedValueOnce({ financialEnabled: 'true' })
    await usePlatformStore().load()
    expect(usePlatformStore().financialEnabled).toBe(false)
  })
  it('uses the backend flag and loads it only once', async () => {
    request.mockResolvedValueOnce({ financialEnabled: true })
    const store = usePlatformStore()
    await store.load()
    await store.load()
    expect(store.financialEnabled).toBe(true)
    expect(request).toHaveBeenCalledTimes(1)
  })
})
