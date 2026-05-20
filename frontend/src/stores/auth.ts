import { defineStore } from 'pinia'
import { postApi } from '@/api/client'

export interface CurrentUser {
  id: number
  username: string
  nickname: string
  email?: string
  wechatContact?: string | null
  qqContact?: string | null
  roles: string[]
}

interface LoginResponse {
  tokenType: string
  accessToken: string
  expiresInMinutes: number
  user: CurrentUser
}

interface AuthState {
  token: string | null
  currentUser: CurrentUser | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('campushub_token'),
    currentUser: null,
  }),
  actions: {
    async login(username: string, password: string) {
      const session = await postApi<LoginResponse>('/auth/login', { username, password })
      this.setSession(session.accessToken, session.user)
    },
    setSession(token: string, currentUser: CurrentUser) {
      this.token = token
      this.currentUser = currentUser
      localStorage.setItem('campushub_token', token)
    },
    clearSession() {
      this.token = null
      this.currentUser = null
      localStorage.removeItem('campushub_token')
    },
  },
})
