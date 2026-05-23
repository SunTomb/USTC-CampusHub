import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data?: T
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
      useAuthStore().clearSession()
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    if (error.response?.status === 403) {
      return Promise.reject(new Error('当前账号无权限执行此操作'))
    }
    return Promise.reject(error)
  },
)

export async function getApi<T>(url: string): Promise<T> {
  const response = await apiClient.get<ApiResponse<T>>(url)
  return unwrapApiResponse(response.data)
}

export async function postApi<T>(url: string, body?: unknown): Promise<T> {
  const response = await apiClient.post<ApiResponse<T>>(url, body)
  return unwrapApiResponse(response.data)
}

export async function putApi<T>(url: string, body?: unknown): Promise<T> {
  const response = await apiClient.put<ApiResponse<T>>(url, body)
  return unwrapApiResponse(response.data)
}
