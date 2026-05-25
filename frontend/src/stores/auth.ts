import { defineStore } from 'pinia'
import { getCurrentUser, type CurrentUser } from '@/api/campushub'
import { postApi } from '@/api/client'
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
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('campushub_token'),
    currentUser: null,
    sessionLoaded: false,
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
      if (!this.token) {
        this.sessionLoaded = true
        return
      }
      try {
        this.currentUser = await getCurrentUser({ skipAuthExpireHandling: true })
      } catch {
        this.clearSession()
      } finally {
        this.sessionLoaded = true
      }
    },
    hasRole(role: string) {
      return Boolean(this.currentUser?.roles.includes(role))
    },
    setSession(token: string, currentUser: CurrentUser) {
      this.token = token
      this.currentUser = currentUser
      localStorage.setItem('campushub_token', token)
    },
    clearSession() {
      this.token = null
      this.currentUser = null
      this.sessionLoaded = true
      localStorage.removeItem('campushub_token')
    },
  },
})
