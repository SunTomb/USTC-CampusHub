import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import * as Vue from 'vue'
import { compile } from '@vue/compiler-dom'
import { parse, compileScript } from 'vue/compiler-sfc'
import Photos from './CatalogPhotos.vue'
import source from './CatalogPhotos.vue?raw'
import { apiClient, getApi } from '@/api/client'

const auth = Vue.reactive({ token: 'viewer' })
vi.mock('@/stores/auth', () => ({ useAuthStore: () => auth }))
vi.mock('@/api/client', () => ({ getApi: vi.fn(), apiClient: { get: vi.fn() }, unwrapApiResponse: vi.fn() }))

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
function mount(options = {}) {
  const root = node('root')
  const props = Vue.reactive({ kind: 'GOODS', objectId: 3, compact: true, ...options })
  const app = renderer.createApp({ render: () => Vue.h(view as Vue.Component, props) })
  app.provide(Symbol.for('v-scx'), { modules: new Set() })
  app.component('ElDialog', Vue.defineComponent({
    props: ['modelValue'], emits: ['update:modelValue'],
    setup: (_, { slots, emit }) => () => Vue.h('dialog', [slots.default?.(), slots.footer?.(), Vue.h('button', { onClick: () => emit('update:modelValue', false) }, '关闭')]),
  }))
  app.component('ElButton', Vue.defineComponent({ setup: (_, { slots }) => () => Vue.h('button', slots.default?.()) }))
  app.mount(root); cleanup = () => app.unmount()
  function nodes(n: Node = root): Node[] { return [n, ...n.children.flatMap(k => nodes(k))] }
  const byClass = (cls: string) => nodes().find(n => n.attrs.class?.split(' ').includes(cls))
  const byText = (text: string) => nodes().find(n => n.text === text)
  const click = (n?: Node) => { const e = { stopPropagation: vi.fn(), preventDefault: vi.fn() }; n!.attrs.onClick(e); return e }
  return { props, nodes, byClass, byText, click }
}
beforeEach(() => { vi.resetAllMocks(); auth.token = 'viewer'; vi.mocked(getApi).mockResolvedValue([]) })
afterEach(() => { cleanup?.(); vi.restoreAllMocks() })
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
