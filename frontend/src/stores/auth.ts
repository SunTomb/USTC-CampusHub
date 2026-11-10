import { defineStore } from 'pinia'
import { getCurrentUser, type CurrentUser } from '@/api/campushub'
import { ApiError, postApi } from '@/api/client'
import { buildIdentityProfile, hasAnyRole, type IdentityKey } from '@/utils/identity'

interface LoginResponse {
  tokenType: string
  accessToken: string
  expiresInMinutes: number
  user: CurrentUser
}

interface AuthState {
  token: string | null
  currentUser: CurrentUser | null
  sessionLoaded: boolean
  sessionError: string | null
  restoreSequence: number
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('campushub_token'),
    currentUser: null,
    sessionLoaded: false,
    sessionError: null,
    restoreSequence: 0,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token && state.currentUser),
    identityProfile: (state) => buildIdentityProfile(state.currentUser),
    isAdmin: (state) => hasAnyRole(state.currentUser?.roles, ['admin']),
    canAccessIdentity: (state) => (identity: IdentityKey) =>
      identity === 'guest' || hasAnyRole(state.currentUser?.roles, [identity]),
  },
  actions: {
    async login(username: string, password: string) {
      const session = await postApi<LoginResponse>('/auth/login', { username, password })
      this.setSession(session.accessToken, session.user)
    },
    async loadCurrentUser() {
      const requestSequence = ++this.restoreSequence
      const requestedToken = this.token
      if (!requestedToken) {
        this.sessionLoaded = true
        this.sessionError = null
        return
      }
      try {
        const user = await getCurrentUser({ skipAuthExpireHandling: true })
        if (this.restoreSequence !== requestSequence || this.token !== requestedToken) return
        this.currentUser = user
        this.sessionLoaded = true
        this.sessionError = null
      } catch (error) {
        if (this.restoreSequence !== requestSequence || this.token !== requestedToken) return
        if (error instanceof ApiError && error.status === 401) {
          this.clearSession()
        } else {
          this.currentUser = null
          this.sessionLoaded = false
          this.sessionError = '暂时无法验证登录状态，已保留登录凭证。请检查网络后重试。'
        }
      }
    },
    hasRole(role: string) {
      return Boolean(this.currentUser?.roles.includes(role))
    },
    setSession(token: string, currentUser: CurrentUser) {
      this.restoreSequence++
      this.token = token
      this.currentUser = currentUser
      this.sessionLoaded = true
      this.sessionError = null
      localStorage.setItem('campushub_token', token)
    },
    clearSession() {
      this.restoreSequence++
      this.token = null
      this.currentUser = null
      this.sessionLoaded = true
      this.sessionError = null
      localStorage.removeItem('campushub_token')
    },
  },
})
