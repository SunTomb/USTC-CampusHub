import { createRenderer, defineComponent, getCurrentInstance, h, nextTick, reactive, type Component } from 'vue'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import Profile from './ProfileView.vue'
import Notifications from './NotificationsView.vue'
import { getApi, postApi, putApi } from '@/api/client'
import { ElMessage } from 'element-plus'

vi.mock('@/api/client', () => ({ getApi: vi.fn(), postApi: vi.fn(), putApi: vi.fn() }))
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn(), success: vi.fn() } }))
vi.mock('@/stores/auth', () => ({ useAuthStore: () => auth }))
const user = () => ({ id: 12, username: 'student', nickname: '同学', roles: ['ROLE_STUDENT'], wechatContact: 'previous', qqContact: null as string | null })
const auth = reactive({ token: 'test', currentUser: user(), identityProfile: { identities: [] } })

// Run the actual component lifecycle and its real API wrappers on an in-memory Vue host.
const host = createRenderer({
  createElement: () => ({}), createText: () => ({}), createComment: () => ({}),
  insert() {}, remove() {}, setText() {}, setElementText() {}, patchProp() {},
  parentNode: () => null, nextSibling: () => null,
})
let unmount: (() => void) | undefined
function mount(view: Component) {
  let state: any
  const app = host.createApp(defineComponent({ render: () => h({ ...view, render() {
    state = (getCurrentInstance() as any).setupState
    return null
  } }) }))
  app.provide(Symbol.for('v-scx'), { modules: new Set() })
  app.mount({})
  unmount = () => app.unmount()
  return state
}
const unread = { id: 1, recipientId: 12, readAt: null }
const read = { id: 2, recipientId: 12, readAt: '2026-01-01T00:00:00' }
beforeEach(() => { vi.resetAllMocks(); auth.currentUser = user(); auth.token = 'test' })
afterEach(() => { unmount?.() })

it('prefills both contacts and refuses a whitespace-only contact pair without a write', async () => {
  const page = mount(Profile)
  expect(page.contactForm).toMatchObject({ wechatContact: 'previous', qqContact: '' })
  Object.assign(page.contactForm, { wechatContact: '  ', qqContact: ' ' })
  await page.submitContacts()
  expect(putApi).not.toHaveBeenCalled()
  expect(ElMessage.error).toHaveBeenCalledOnce()
})

it.each([
  { wechatContact: '  newWX ', qqContact: '', savedWX: 'newWX', savedQQ: null },
  { wechatContact: '', qqContact: '  123456 ', savedWX: null, savedQQ: '123456' },
])('saves a single trimmed contact and keeps the rest of the current user', async ({ wechatContact, qqContact, savedWX, savedQQ }) => {
  const page = mount(Profile)
  expect(page.contactForm).toBeDefined()
  page.profileForm.nickname = '未保存的昵称'
  Object.assign(page.contactForm, { wechatContact, qqContact })
  vi.mocked(putApi).mockResolvedValue({ wechatContact: savedWX, qqContact: savedQQ })
  await page.submitContacts()
  expect(putApi).toHaveBeenCalledWith('/users/me/contacts', { wechatContact: wechatContact.trim(), qqContact: qqContact.trim() })
  expect(auth.currentUser).toEqual({ ...user(), wechatContact: savedWX, qqContact: savedQQ })
  expect(page.profileForm.nickname).toBe('未保存的昵称')
  expect(ElMessage.success).toHaveBeenCalledWith('联系方式已更新')
})

it('counts unread items, sends one bodyless bulk POST and reloads authoritative read times', async () => {
  vi.mocked(getApi).mockResolvedValueOnce([unread, read]).mockResolvedValueOnce([{ ...unread, readAt: read.readAt }, read])
  let finish!: (value: { updatedCount: number }) => void
  vi.mocked(postApi).mockImplementation(() => new Promise(resolve => { finish = resolve }))
  const page = mount(Notifications)
  await nextTick()
  expect(page.unreadCount).toBe(1)
  const pending = page.markAllRead()
  expect(page.markingAll).toBe(true)
  await page.markAllRead()
  expect(postApi).toHaveBeenCalledTimes(1); expect(postApi).toHaveBeenCalledWith('/notifications/read-all')
  finish({ updatedCount: 1 }); await pending
  expect(getApi).toHaveBeenNthCalledWith(2, '/users/12/notifications')
  expect(page.unreadCount).toBe(0)
  expect(page.markingAll).toBe(false)
  expect(ElMessage.success).toHaveBeenCalledWith('已全部标为已读')
  await page.markAllRead()
  expect(postApi).toHaveBeenCalledTimes(1)
})

it('keeps unread entries retryable when bulk marking fails', async () => {
  vi.mocked(getApi).mockResolvedValue([unread]); vi.mocked(postApi).mockRejectedValue(new Error('请求失败'))
  const page = mount(Notifications); await nextTick()
  expect(page.unreadCount).toBe(1)
  await page.markAllRead()
  expect(page.markingAll).toBe(false)
  expect(page.unreadCount).toBe(1)
  expect(ElMessage.success).not.toHaveBeenCalled()
  expect(ElMessage.error).toHaveBeenCalledOnce()
})
