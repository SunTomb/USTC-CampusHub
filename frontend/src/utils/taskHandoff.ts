export function weightGrams(input: string): number | null {
  const match = /^(\d+)(?:\.(\d{1,3}))?$/.exec(input.trim())
  if (!match) return null
  const grams = Number(match[1]) * 1000 + Number((match[2] ?? '').padEnd(3, '0'))
  return grams >= 1 && grams <= 100000 ? grams : null
}

export function weightLabel(grams?: number | null) {
  return grams != null && Number.isInteger(grams) && grams > 0 ? `${grams / 1000} kg` : '未填写'
}

export const PHOTO_LIMIT = 5 * 1024 * 1024
export interface DraftPhoto { file: File; clientRequestId: string; url: string; uploaded: boolean }
export function photoProblem(files: readonly Pick<File, 'type' | 'size'>[], existing: number, existingBytes = 0): string {
  if (existing + files.length > 3) return '每类照片最多 3 张，请减少选择数量。'
  if (files.some(f => !['image/jpeg', 'image/png'].includes(f.type))) return '只支持 JPEG 或 PNG 图片。'
  if (files.some(f => f.size <= 0 || f.size > PHOTO_LIMIT)) return '请选择非空且不超过 5 MiB 的图片。'
  if (existingBytes + files.reduce((total, f) => total + f.size, 0) > 3 * PHOTO_LIMIT) return '两类照片合计不能超过 15 MiB。'
  return ''
}
export function draftPhoto(file: File): DraftPhoto {
  const id = Array.from(crypto.getRandomValues(new Uint8Array(16)), byte => byte.toString(16).padStart(2, '0')).join('')
  return { file, clientRequestId: id, url: URL.createObjectURL(file), uploaded: false }
}
export function discardPhotos(photos: DraftPhoto[]) {
  photos.splice(0).forEach(photo => URL.revokeObjectURL(photo.url))
}
export async function uploadMissing(photos: DraftPhoto[], signal: AbortSignal, upload: (photo: DraftPhoto) => Promise<unknown>) {
  for (const photo of photos) {
    signal.throwIfAborted()
    if (photo.uploaded) continue
    await upload(photo)
    signal.throwIfAborted()
    photo.uploaded = true
  }
}
export function taskPath(id: number) {
  if (!Number.isSafeInteger(id) || id <= 0) throw new Error('无效的任务编号')
  return `/tasks/${id}`
}
export function evidencePath(taskId: number, photoId: number) {
  if (!Number.isSafeInteger(photoId) || photoId <= 0) throw new Error('无效的照片编号')
  return `${taskPath(taskId)}/evidence/${photoId}`
}
export const workflowLabels: Record<string, string> = { PUBLISHED: '待接单', ACCEPTED: '已接单', HEADING_TO_PICKUP: '前往取件', PICKED_UP: '已取件', DELIVERING: '派送中', DELIVERED: '已送达', PENDING_CONFIRMATION: '待确认', COMPLETED: '已完成', CANCELLED: '已取消', ISSUE_HANDLING: '异常处理中', DISPUTE_HANDLING: '争议处理中' }
export function taskError(error: unknown, fallback: string) {
  const message = (error as { response?: { data?: { message?: unknown } } })?.response?.data?.message
  return typeof message === 'string' && message ? message : error instanceof Error ? error.message : fallback
}
