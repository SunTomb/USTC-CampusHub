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

export async function getApi<T>(url: string): Promise<T> {
  const response = await apiClient.get<ApiResponse<T>>(url)
  return unwrapApiResponse(response.data)
}

export async function postApi<T>(url: string, body?: unknown): Promise<T> {
  const response = await apiClient.post<ApiResponse<T>>(url, body)
  return unwrapApiResponse(response.data)
}
