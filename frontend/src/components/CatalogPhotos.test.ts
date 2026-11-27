import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import * as Vue from 'vue'
import { compile } from '@vue/compiler-dom'
import { parse, compileScript } from 'vue/compiler-sfc'
import { AxiosError, AxiosHeaders, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import GoodsPublish from '@/views/GoodsPublishView.vue'
import { listGoodsCategories, getGoodsDetail, publishGoods, updateGoods } from '@/api/campushub'
import Photos from './CatalogPhotos.vue'
import source from './CatalogPhotos.vue?raw'
import { apiClient, getApi } from '@/api/client'

const auth = Vue.reactive({ token: 'viewer', currentUser: { id: 7 }, clearSession: vi.fn() })
const route = Vue.reactive({ query: {} as { edit?: string } }), push = vi.fn()
vi.mock('vue-router', () => ({ useRoute: () => route, useRouter: () => ({ push }) }))
vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn(), error: vi.fn(), warning: vi.fn() } }))
vi.mock('@/api/campushub', () => ({ listGoodsCategories: vi.fn(), getGoodsDetail: vi.fn(), publishGoods: vi.fn(), updateGoods: vi.fn() }))
vi.mock('@/stores/auth', () => ({ useAuthStore: () => auth }))
vi.mock('@/api/client', async importOriginal => ({ ...await importOriginal<typeof import('@/api/client')>(), getApi: vi.fn() }))

// Vitest runs in Node (the .vue module has an SSR template). Mount the actual client
// template on a small Vue host so tests click rendered controls without a DOM dependency.
const { descriptor } = parse(source)
const { code } = compile(descriptor.template!.content, {
  mode: 'function', prefixIdentifiers: true, bindingMetadata: compileScript(descriptor, { id: 'catalog' }).bindings,
})
const view = { ...Photos, render: new Function('Vue', code)(Vue) }
type Node = { tag: string; text: string; attrs: Record<string, any>; children: Node[]; parent?: Node }
const node = (tag: string, text = ''): Node => ({ tag, text, attrs: {}, children: [] })
const renderer = Vue.createRenderer<Node, Node>({
  createElement: node, createText: text => node('#text', text), createComment: () => node('#comment'),
  setText: (n, text) => { n.text = text }, setElementText: (n, text) => { n.text = text; n.children = [] },
  parentNode: n => n.parent ?? null, nextSibling: n => n.parent?.children[n.parent.children.indexOf(n) + 1] ?? null,
  insert(n, parent, anchor) { n.parent = parent; parent.children.splice(anchor ? parent.children.indexOf(anchor) : parent.children.length, 0, n) },
  remove(n) { const siblings = n.parent?.children; if (siblings) siblings.splice(siblings.indexOf(n), 1) },
  patchProp: (n, key, _, value) => { n.attrs[key] = value },
})
let cleanup: (() => void) | undefined
const settle = () => new Promise(resolve => setTimeout(resolve, 0))
function mount(options = {}, goodsPage = false) {
  const root = node('root')
  const props = Vue.reactive({ kind: 'GOODS', objectId: 3, compact: true, ...options })
  const app = renderer.createApp(goodsPage ? { ...GoodsPublish, render() {
    const state = (Vue.getCurrentInstance() as any).setupState
    return state.ready ? Vue.h(view as Vue.Component, { kind: 'GOODS', editable: true, ref: (el: any) => { state.photos = el }, objectId: state.savedId || state.editingId }) : null
  } } : { render: () => Vue.h(view as Vue.Component, props) })
  app.provide(Symbol.for('v-scx'), { modules: new Set() })
  app.component('ElDialog', Vue.defineComponent({
    props: ['modelValue'], emits: ['update:modelValue'],
    setup: (_, { slots, emit }) => () => Vue.h('dialog', [slots.default?.(), slots.footer?.(), Vue.h('button', { onClick: () => emit('update:modelValue', false) }, '关闭')]),
  }))
  app.component('ElButton', Vue.defineComponent({ setup: (_, { slots }) => () => Vue.h('button', slots.default?.()) }))
  const page = (app.mount(root) as any).$.setupState; cleanup = () => app.unmount()
  function nodes(n: Node = root): Node[] { return [n, ...n.children.flatMap(k => nodes(k))] }
  const byClass = (cls: string) => nodes().find(n => n.attrs.class?.split(' ').includes(cls))
  const byText = (text: string) => nodes().find(n => n.text === text)
  const click = (n?: Node) => { const e = { stopPropagation: vi.fn(), preventDefault: vi.fn() }; n!.attrs.onClick(e); return e }
  const select = (count: number) => nodes().find(n => n.tag === 'input')!.attrs.onChange({ target: { files: Array.from({length: count}, () => new Blob(['fixture'], {type: 'image/png'})), value: '' } })
  return { props, nodes, byClass, byText, click, select, page }
}
const originalAdapter = apiClient.defaults.adapter
beforeEach(() => { vi.resetAllMocks(); vi.spyOn(apiClient, 'get'); route.query = {}; auth.token = 'viewer';
  apiClient.defaults.adapter = async () => { throw Error('unconfigured fixture') };
  vi.mocked(listGoodsCategories).mockResolvedValue([{id: 1, name: '教材', enabled: true}] as any); vi.mocked(getApi).mockResolvedValue([]) })
afterEach(() => { cleanup?.(); apiClient.defaults.adapter = originalAdapter; vi.restoreAllMocks() })
it('keeps the fixed cover and a non-interactive fallback when the listing has no photos', async () => {
  const { byClass, byText } = mount({ fallbackChar: '书', fallbackSub: '教材', tone: 2 }); await settle()
  expect(byClass('catalog-thumb-frame')).toBeDefined()
  expect(byClass('cover-tone-2')).toBe(byClass('catalog-fallback'))
  expect(byClass('catalog-fallback')!.attrs.onClick).toBeUndefined()
  expect(byText('书')).toBeDefined(); expect(byText('教材')).toBeDefined()
  expect(byClass('catalog-open')).toBeUndefined()
})
it('opens all photos from the single thumb; fetches only the first private blob until navigation', async () => {
  vi.mocked(getApi).mockResolvedValue([{ id: 11, published: false }, { id: 12, published: true }, { id: 13, published: false }])
  vi.mocked(apiClient.get).mockResolvedValue({ data: new Blob(['test']) })
  const { nodes, byClass, byText, click } = mount(); await settle()
  const button = (text: string) => nodes().find(n => n.tag === 'button' && (n.text === text || n.children.some(child => child.text === text)))
  expect(byText('3图')).toBeDefined(); expect(nodes().filter(n => n.tag === 'img')).toHaveLength(1)
  expect(apiClient.get).toHaveBeenCalledTimes(1)
  expect(click(byClass('catalog-open')).stopPropagation).toHaveBeenCalled(); await settle()
  expect(byText('1 / 3')).toBeDefined()
  click(button('下一张')); await settle()
  expect(byText('2 / 3')).toBeDefined(); expect(byClass('catalog-lightbox')!.children[0].attrs.src).toBe('/api/catalog-media/GOODS/3/12')
  expect(apiClient.get).toHaveBeenCalledTimes(1)
  click(button('下一张')); await settle()
  expect(byText('3 / 3')).toBeDefined(); expect(apiClient.get).toHaveBeenNthCalledWith(2, '/catalog-media/GOODS/3/13', { responseType: 'blob' })
  click(button('上一张')); await settle(); expect(byText('2 / 3')).toBeDefined()
  click(button('关闭')); await settle(); expect(byClass('catalog-lightbox')).toBeUndefined()
})
it('previews queued files before upload and revokes their object URLs when the account changes', async () => {
  const revoke = vi.spyOn(URL, 'revokeObjectURL')
  const { nodes, byClass, click } = mount({ editable: true, compact: false }); await settle()
  nodes().find(n => n.tag === 'input')!.attrs.onChange({ target: { files: [new Blob(['photo'], { type: 'image/png' })], value: '' } }); await settle()
  expect(nodes().some(n => n.tag === 'figure')).toBe(true)
  click(byClass('catalog-open')); await settle()
  const url = byClass('catalog-lightbox')!.children[0].attrs.src
  expect(url).toMatch(/^blob:/); auth.token = 'replacement'; await settle()
  expect(revoke).toHaveBeenCalledWith(url); expect(byClass('catalog-lightbox')).toBeUndefined()
})
it('discards a private response that finishes after a listing switch', async () => {
  let finish!: (value: { data: Blob }) => void
  vi.mocked(getApi).mockResolvedValueOnce([{ id: 14, published: false }]).mockResolvedValueOnce([])
  vi.mocked(apiClient.get).mockImplementationOnce(() => new Promise(resolve => { finish = resolve }))
  const create = vi.spyOn(URL, 'createObjectURL')
  const { props, byClass } = mount(); await settle(); props.objectId++; await settle()
  finish({ data: new Blob(['old']) }); await settle()
  expect(create).not.toHaveBeenCalled(); expect(byClass('catalog-fallback')).toBeDefined()
})
it('explicit detail layout overrides compact and each thumbnail opens its corresponding image', async () => {
  vi.mocked(getApi).mockResolvedValue([{ id: 5, published: true }, { id: 6, published: true }])
  const { nodes, click, byClass, byText } = mount({ layout: 'detail' }); await settle()
  const thumbs = nodes().filter(n => n.tag === 'figure')
  expect(thumbs).toHaveLength(2); expect(byClass('catalog-thumb-frame')).toBeDefined()
  click(thumbs[1].children[0]); await settle()
  expect(byText('2 / 2')).toBeDefined(); expect(byClass('catalog-lightbox')!.children[0].attrs.src).toBe('/api/catalog-media/GOODS/3/6')
})

// Exercise the real Axios interceptors without a socket. Like the HTTP adapter,
// resolve/reject according to this particular request's validateStatus.
function reply(config: InternalAxiosRequestConfig, status: number, data: unknown) {
  const response: AxiosResponse = { config, status, data, statusText: '', headers: new AxiosHeaders() }
  if (!config.validateStatus?.(status)) throw new AxiosError('Request failed', 'ERR_BAD_RESPONSE', config, {}, response)
  return response
}
it.each([
  [403, { success: false, message: '不能修改此图片' }, '不能修改此图片'],
  [401, { success: false, message: 'fixture internal' }, '登录已过期'],
  [403, 'Forbidden', '无权限'],
  [200, { success: false, message: '照片上传失败' }, '未收到具体原因'],
  [200, { success: false, message: '图片已满，请先移除一张' }, '图片已满，请先移除一张'],
  [413, '<html>proxy /private/fixture</html>', '文件过大'],
  [502, { success: false, message: '读取 /private/fixture 失败' }, '服务暂时不可用'],
  [200, { success: false, message: 'java.lang.Exception: 内部错误' }, '未收到具体原因'],
  [200, '<html>private</html>', '未收到具体原因'],
  [200, {success: true}, '未收到具体原因'],
  [0, 'ECONNABORTED', '未收到具体原因'], [0, 'ETIMEDOUT', '超时'], [0, 'ERR_NETWORK', '网络'],
] as const)('direct catalog retry displays only the upload reason (%s, %j)', async (status, data, text) => {
  apiClient.defaults.adapter = async config => {
    if (!status) throw new AxiosError('private transport details', String(data), config)
    return reply(config, status, data)
  }
  const editor = mount({kind: 'SHOP', editable: true, compact: false}); await settle()
  editor.select(1); await settle(); editor.click(editor.byText('上传所选图片')); await settle()
  const alert = editor.byClass('catalog-error')?.text
  expect(alert).toContain('照片上传失败：'); expect(alert).toContain(text)
  expect(alert).not.toMatch(/private|java\.lang|<html>|闲置/)
  expect(editor.byText('上传所选图片')).toBeDefined()
  expect(auth.clearSession).toHaveBeenCalledTimes(status === 401 ? 1 : 0)
})
it.each([false, true])('shows a business rejection after saving goods; retries remaining photos without saving again (edit=%s)', async edit => {
  if (edit) route.query.edit = '23'
  vi.mocked(getGoodsDetail).mockResolvedValue({id: 23, sellerId: 7, canEdit: true, categoryId: 1, price: 2, conditionLevel: '新', tradeLocation: '校内'} as any)
  const save = edit ? updateGoods : publishGoods
  vi.mocked(save).mockResolvedValue({id: 23} as any)
  const sent: string[] = []
  apiClient.defaults.adapter = async config => {
    expect(config.url).toBe('/catalog-media/GOODS/23'); sent.push(config.data.get('clientRequestId'))
    return reply(config, 200, sent.length === 2 ? {success:false, message:'图片尺寸过大'} : {success:true, data:{id:sent.length}})
  }
  const editor = mount({}, true); await settle(); Object.assign(editor.page.form, {title:'课本', description:'说明'})
  editor.select(3); await Promise.all([editor.page.submit(), editor.page.submit()]); await settle()
  expect(ElMessage.error).toHaveBeenCalledWith('闲置已保存，照片上传失败：图片尺寸过大，请缩小图片宽高后重试')
  expect(editor.byClass('catalog-error')!.text).toBe('照片上传失败：图片尺寸过大，请缩小图片宽高后重试')
  expect(push).not.toHaveBeenCalled(); expect(editor.nodes().filter(n => n.tag === 'figure')).toHaveLength(2)
  await editor.page.submit(); expect(save).toHaveBeenCalledTimes(1); expect(sent).toHaveLength(4)
  expect(new Set(sent).size).toBe(3); expect(sent[2]).toBe(sent[1]); expect(push).toHaveBeenCalledWith('/goods/23')
})
it('does not label a rejected goods save or navigation as a photo failure', async () => {
  vi.mocked(publishGoods).mockRejectedValueOnce(Error('商品未保存')).mockResolvedValueOnce({id:23} as any)
  push.mockRejectedValueOnce(Error('导航失败'))
  const {page} = mount({}, true); await settle(); Object.assign(page.form, {title:'课本', description:'说明'})
  await page.submit(); expect(ElMessage.error).toHaveBeenLastCalledWith('商品未保存'); expect(page.savedId).toBe(0)
  await page.submit(); expect(page.savedId).toBe(23); expect(ElMessage.error).toHaveBeenLastCalledWith('未能打开详情，请点击「查看已保存的闲置」')
})
