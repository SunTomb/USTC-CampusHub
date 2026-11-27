import { isAxiosError } from 'axios'
import { apiClient, ApiError, type ApiResponse } from './client'

// Catalog errors are shown verbatim in multiple editors. Only expose a short,
// plain business message, never an arbitrary HTTP body or transport exception.
function uploadError(status = 0, body?: ApiResponse<unknown>, code?: string) {
  const message = body?.success === false && typeof body.message === 'string' ? body.message.trim().replace(/^(?:照片|图片)上传失败[：:]\s*/, '') : ''
  let reason = '未收到具体原因，请重试'
  if (/^[㐀-鿿A-Za-z0-9 ，。！？、：（）()％%;；,.!?-]{1,160}$/.test(message) &&
      !/^(?:(?:照片|图片)?上传失败|请求失败|操作失败)$/.test(message) &&
      !/(?:exception|stack trace|traceback|\b\w*Error\b|\bSQL\b|[A-Za-z]\.[A-Za-z])/i.test(message)) {
    reason = message === '图片尺寸过大' ? '图片尺寸过大，请缩小图片宽高后重试' : message
  } else if (status === 413) reason = '照片文件过大，请压缩后重试'
  else if (status === 403) reason = '当前账号无权限上传照片'
  else if (status === 401) reason = '登录已过期，请重新登录'
  else if (status === 408 || code === 'ETIMEDOUT') reason = '上传超时，请重试'
  else if (code === 'ERR_NETWORK') reason = '网络连接失败，请检查网络后重试'
  else if (status >= 500) reason = '服务暂时不可用，请稍后重试'
  return new ApiError(`照片上传失败：${reason}`, status)
}

export async function uploadCatalogPhoto(path: string, form: FormData) {
  // Inspect the upload response locally (including 403); keep the normal 401 interceptor.
  const { status, data } = await apiClient.post<ApiResponse<{id: number}>>(path, form, {
    timeout: 60000, validateStatus: status => status !== 401,
    transitional: { clarifyTimeoutError: true }, // ETIMEDOUT distinguishes a timeout from an abort.
  }).catch((error: unknown) => {
    if (isAxiosError(error)) throw uploadError(error.response?.status, error.response?.data, error.code)
    throw uploadError(error instanceof ApiError ? error.status : 0)
  })
  if (status < 200 || status >= 300 || data?.success !== true || !data.data || !Number.isSafeInteger(data.data.id) || data.data.id < 1) {
    throw uploadError(status, data)
  }
}
