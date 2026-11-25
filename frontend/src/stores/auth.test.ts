import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { apiClient } from '@/api/client'
import { useAuthStore } from './auth'

const getCurrentUser = vi.hoisted(() => vi.fn())
vi.mock('@/api/campushub', () => ({ getCurrentUser }))

const user = { id: 1, username: 'fixture', nickname: '测试用户', roles: ['ROLE_STUDENT'] }
let storage: Map<string, string>

beforeEach(() => {
  storage = new Map([['campushub_token', 'saved-session']])
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => storage.get(key) ?? null,
    setItem: (key: string, value: string) => storage.set(key, value),
    removeItem: (key: string) => storage.delete(key),
  })
  setActivePinia(createPinia())
  getCurrentUser.mockReset()
})

afterEach(() => vi.unstubAllGlobals())

async function httpError(status: number) {
  const handler = apiClient.interceptors.response.handlers?.[0].rejected
  try {
    await handler?.({ config: { skipAuthExpireHandling: true }, response: { status, data: { message: 'request failed' } } })
  } catch (error) {
    return error
  }
  throw new Error('Expected a rejected HTTP response')
}

describe('session restoration', () => {
  it.each(['network', 'timeout', 'server', 'forbidden'])('retains the token without granting access on a %s failure', async (failure) => {
    const error = failure === 'server' ? await httpError(503) : failure === 'forbidden' ? await httpError(403) : new Error(failure)
    getCurrentUser.mockRejectedValueOnce(error)
    const auth = useAuthStore()

    await auth.loadCurrentUser()

    expect(auth.token).toBe('saved-session')
    expect(storage.get('campushub_token')).toBe('saved-session')
    expect(auth.currentUser).toBeNull()
    expect(auth.isAuthenticated).toBe(false)
    expect(auth.sessionLoaded).toBe(false)
    expect(auth.sessionError).toBeTruthy()
  })

  it('can retry after a temporary failure and verifies the current user', async () => {
    getCurrentUser.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce(user)
    const auth = useAuthStore()

    await auth.loadCurrentUser()
    await auth.loadCurrentUser()

    expect(getCurrentUser).toHaveBeenCalledTimes(2)
    expect(auth.currentUser).toEqual(user)
    expect(auth.isAuthenticated).toBe(true)
    expect(auth.sessionLoaded).toBe(true)
    expect(auth.sessionError).toBeNull()
  })

  it('clears a session only when restoration explicitly returns 401', async () => {
    getCurrentUser.mockRejectedValueOnce(await httpError(401))
    const auth = useAuthStore()

    await auth.loadCurrentUser()

    expect(auth.token).toBeNull()
    expect(storage.has('campushub_token')).toBe(false)
    expect(auth.currentUser).toBeNull()
    expect(auth.sessionLoaded).toBe(true)
    expect(auth.sessionError).toBeNull()
  })

  it('does not let an older failure undo a newer restore for the same token', async () => {
    let rejectOlder!: (error: unknown) => void
    let resolveNewer!: (value: typeof user) => void
    getCurrentUser
      .mockReturnValueOnce(new Promise((_, fail) => { rejectOlder = fail }))
      .mockReturnValueOnce(new Promise<typeof user>((done) => { resolveNewer = done }))
    const auth = useAuthStore()
    const older = auth.loadCurrentUser()
    const newer = auth.loadCurrentUser()
    resolveNewer(user)
    await newer
    rejectOlder(new Error('older connection failed'))
    await older

    expect(auth.currentUser).toEqual(user)
    expect(auth.isAuthenticated).toBe(true)
    expect(auth.sessionLoaded).toBe(true)
    expect(auth.sessionError).toBeNull()
  })

  it('invalidates in-flight restoration across sign-out even if a token is reused', async () => {
    let resolve!: (value: typeof user) => void
    getCurrentUser.mockReturnValueOnce(new Promise<typeof user>((done) => { resolve = done }))
    const auth = useAuthStore()
    const pending = auth.loadCurrentUser()
    auth.clearSession()
    const replacement = { ...user, id: 2, username: 'replacement' }
    auth.setSession('saved-session', replacement)
    resolve(user)
    await pending

    expect(auth.currentUser).toEqual(replacement)
    expect(auth.sessionLoaded).toBe(true)
    expect(auth.sessionError).toBeNull()
  })

  it('does not load a user without a token', async () => {
    storage.clear()
    const auth = useAuthStore()

    await auth.loadCurrentUser()

    expect(getCurrentUser).not.toHaveBeenCalled()
    expect(auth.isAuthenticated).toBe(false)
  })

  it('does not restore a user after they have signed out during the request', async () => {
    let resolve!: (value: typeof user) => void
    getCurrentUser.mockReturnValueOnce(new Promise<typeof user>((done) => { resolve = done }))
    const auth = useAuthStore()
    const pending = auth.loadCurrentUser()
    auth.clearSession()
    resolve(user)
    await pending

    expect(auth.token).toBeNull()
    expect(auth.currentUser).toBeNull()
    expect(auth.sessionError).toBeNull()
  })

  it('does not replace a newer session with an older successful response', async () => {
    let resolve!: (value: typeof user) => void
    getCurrentUser.mockReturnValueOnce(new Promise<typeof user>((done) => { resolve = done }))
    const auth = useAuthStore()
    const pending = auth.loadCurrentUser()
    const replacement = { ...user, id: 2, username: 'replacement' }
    auth.setSession('new-session', replacement)
    resolve(user)
    await pending

    expect(auth.currentUser).toEqual(replacement)
    expect(auth.sessionLoaded).toBe(true)
    expect(auth.sessionError).toBeNull()
  })

  it('does not clear a newer session when an older request returns 401', async () => {
    let reject!: (error: unknown) => void
    getCurrentUser.mockReturnValueOnce(new Promise((_, fail) => { reject = fail }))
    const auth = useAuthStore()
    const pending = auth.loadCurrentUser()
    const replacement = { ...user, id: 2, username: 'replacement' }
    auth.setSession('new-session', replacement)
    reject(await httpError(401))
    await pending

    expect(auth.token).toBe('new-session')
    expect(auth.currentUser).toEqual(replacement)
    expect(auth.sessionLoaded).toBe(true)
    expect(auth.sessionError).toBeNull()
  })
})
