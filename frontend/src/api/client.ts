import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data?: T
}

export class ApiError extends Error {
  constructor(message: string, public readonly status: number) {
    super(message)
    this.name = 'ApiError'
  }
}

export interface ApiRequestOptions {
  skipAuthExpireHandling?: boolean
}

declare module 'axios' {
  export interface AxiosRequestConfig {
    skipAuthExpireHandling?: boolean
  }
}

export function unwrapApiResponse<T>(response: ApiResponse<T>): T {
  if (!response.success) {
    throw new Error(response.message || '请求失败')
  }
  return response.data as T
}

export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10_000,
})

apiClient.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      if (error.config?.skipAuthExpireHandling) {
        return Promise.reject(new ApiError(error.response.data?.message || '请先登录', 401))
      }
      useAuthStore().clearSession()
      return Promise.reject(new ApiError('登录已过期，请重新登录', 401))
    }
    if (error.response?.status === 403) {
      return Promise.reject(new ApiError('当前账号无权限执行此操作', 403))
    }
    return Promise.reject(error)
  },
)

export async function getApi<T>(url: string, options?: ApiRequestOptions): Promise<T> {
  const response = await apiClient.get<ApiResponse<T>>(url, options)
  return unwrapApiResponse(response.data)
}

export async function postApi<T>(url: string, body?: unknown, options?: ApiRequestOptions): Promise<T> {
  const response = await apiClient.post<ApiResponse<T>>(url, body, options)
  return unwrapApiResponse(response.data)
}

export async function putApi<T>(url: string, body?: unknown, options?: ApiRequestOptions): Promise<T> {
  const response = await apiClient.put<ApiResponse<T>>(url, body, options)
  return unwrapApiResponse(response.data)
}
