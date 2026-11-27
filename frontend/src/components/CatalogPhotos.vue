<template>
  <div class="catalog-pictures" :class="`catalog-layout-${mode}`">
    <div v-if="editable" class="catalog-caption">{{ photoLabel }}（选填）</div>
    <div v-if="mode !== 'gallery' || !allItems.length" class="catalog-thumb-frame">
      <button v-if="allItems[0]?.src" type="button" class="catalog-open" :aria-label="`预览${photoLabel}`" @click.stop.prevent="openPreview(0)">
        <img class="catalog-thumb-img" :src="allItems[0].src" :alt="photoLabel" loading="lazy" />
      </button>
      <div v-else class="catalog-fallback" :class="`cover-tone-${(tone || 0) % 4}`">
        <strong>{{ fallbackChar || '图' }}</strong><small v-if="fallbackSub">{{ fallbackSub }}</small>
      </div>
      <span v-if="mode === 'thumb' && allItems.length > 1" class="catalog-photo-count">{{ allItems.length }}图</span>
    </div>
    <div v-if="mode === 'gallery' || (mode === 'detail' && (editable || allItems.length > 1))" class="catalog-strip" :aria-label="photoLabel">
      <figure v-for="(item, index) in allItems" :key="item.key">
        <button type="button" class="catalog-open" :aria-label="`预览第 ${index + 1} 张图片`" @click.stop.prevent="openPreview(index)">
          <img v-if="item.src" :src="item.src" :alt="item.file ? '待上传图片' : photoLabel" loading="lazy" />
          <span v-else>{{ item.failed ? '暂不可用' : '加载中…' }}</span>
        </button>
        <button v-if="editable" type="button" class="catalog-remove" :disabled="busy" :aria-label="`移除第 ${index + 1} 张图片`" @click.stop="item.file ? drop(item.key) : remove(item.id)">移除</button>
      </figure>
    </div>
    <template v-if="editable">
      <input v-if="allItems.length < max" aria-label="选择图片" type="file" accept="image/jpeg,image/png" :multiple="max > 1" :disabled="busy || reading" @change="pick" />
      <button v-if="queue.length && target" type="button" :disabled="busy" @click="retry">{{ busy ? '上传中…' : '上传所选图片' }}</button>
    </template>
    <small v-if="editable && error" class="catalog-error" role="alert">{{ error }}</small>
    <el-dialog v-if="previewOpen" v-model="previewOpen" title="图片预览" width="min(92vw, 860px)" append-to-body align-center>
      <div class="catalog-lightbox">
        <img v-if="activeItem?.src && !previewFailed" :key="activeItem.key" :src="activeItem.src" :alt="`${photoLabel} ${previewIndex + 1}`" @error="previewFailed = true" />
        <span v-else role="status">{{ previewFailed || activeItem?.failed ? '图片暂时无法加载' : '正在加载图片…' }}</span>
      </div>
      <template #footer>
        <div class="catalog-lightbox-nav">
          <el-button v-if="allItems.length > 1" :disabled="previewIndex === 0" @click="openPreview(previewIndex - 1)">上一张</el-button>
          <span aria-live="polite">{{ previewIndex + 1 }} / {{ allItems.length }}</span>
          <el-button v-if="allItems.length > 1" :disabled="previewIndex === allItems.length - 1" @click="openPreview(previewIndex + 1)">下一张</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ApiError, apiClient, getApi, unwrapApiResponse } from '@/api/client'
import { uploadCatalogPhoto } from '@/api/catalogUpload'
import { useAuthStore } from '@/stores/auth'

type Layout = 'thumb' | 'detail' | 'gallery'
type Photo = { key: string; id?: number; file?: File; src: string; failed?: boolean }
const props = defineProps<{
  kind: string; objectId?: number | null; editable?: boolean; compact?: boolean
  fallbackChar?: string; fallbackSub?: string; tone?: number; layout?: Layout
}>()
const emit = defineEmits<{ changed: [] }>()
const auth = useAuthStore()
const mode = computed(() => props.layout ?? (props.compact ? 'thumb' : 'gallery'))
const max = computed(() => props.kind === 'SHOP' ? 1 : 3)
const photoLabel = computed(() => props.kind === 'SHOP' ? '店铺封面' : '展示图片')
const target = ref(0), images = ref<Photo[]>([]), queue = ref<Photo[]>([])
const allItems = computed(() => [...images.value, ...queue.value])
const busy = ref(false), reading = ref(false), error = ref('')
const previewOpen = ref(false), previewIndex = ref(0), previewFailed = ref(false)
const activeItem = computed(() => allItems.value[previewIndex.value])
const pending = new WeakMap<Photo, Promise<void>>()
let generation = 0, readSeq = 0
const path = (id: number) => `/catalog-media/${props.kind}/${id}`
function revoke(item: Photo) { if (item.src.startsWith('blob:')) URL.revokeObjectURL(item.src) }
function clear() {
  generation++; readSeq++; previewOpen.value = false
  allItems.value.forEach(revoke); images.value = []; queue.value = []
  error.value = ''; busy.value = false; reading.value = false
}
function loadBlob(item: Photo) {
  if (!item.id || item.src) return
  const existing = pending.get(item)
  if (existing) return existing
  const seq = readSeq
  item.failed = false
  const request = apiClient.get<Blob>(`${path(target.value)}/${item.id}`, { responseType: 'blob' })
    .then(({ data }) => { if (seq === readSeq) item.src = URL.createObjectURL(data) })
    .catch(() => { if (seq === readSeq) item.failed = true })
    .finally(() => { pending.delete(item) })
  pending.set(item, request)
  return request
}
function loadVisible() {
  const visible = mode.value === 'thumb' ? images.value.slice(0, 1) : images.value
  return Promise.all(visible.map(loadBlob))
}
async function load() {
  const id = target.value, seq = ++readSeq
  if (!id) return
  reading.value = true
  try {
    const base = path(id)
    const metadata = await getApi<{ id: number; published: boolean }[]>(base)
    if (seq !== readSeq) return
    previewOpen.value = false
    images.value.forEach(revoke)
    images.value = metadata.map(photo => ({ id: photo.id, key: `saved-${photo.id}`, src: photo.published ? `/api${base}/${photo.id}` : '' }))
    await loadVisible()
  } catch {
    if (seq === readSeq && !error.value) error.value = '图片暂时无法加载'
  } finally {
    if (seq === readSeq) reading.value = false
  }
}
watch([() => props.kind, () => props.objectId, () => auth.token], ([kind, id, token], old) => {
  const next = Number(id) || 0
  // A newly saved form gets its first ID before saveTo; retain its local selections only in this case.
  const firstSave = old && kind === old[0] && token === old[2] && !old[1] && next > 0 && (!target.value || target.value === next)
  if (!firstSave) clear()
  target.value = next
  if (next) void load()
}, { immediate: true, flush: 'sync' })
watch(mode, loadVisible)
onBeforeUnmount(clear)

function openPreview(index: number) {
  const item = allItems.value[index]
  if (!item) return
  previewIndex.value = index; previewFailed.value = false; previewOpen.value = true
  void loadBlob(item)
}
function pick(event: Event) {
  const input = event.target as HTMLInputElement, files = Array.from(input.files || [])
  input.value = ''
  if (files.length + allItems.value.length > max.value || files.some(file => !['image/png', 'image/jpeg'].includes(file.type) || !file.size || file.size > 5 * 1024 * 1024)) {
    error.value = `最多 ${max.value} 张 JPG 或 PNG，每张不超过 5 MiB`; return
  }
  queue.value.push(...files.map(file => ({ file, key: crypto.randomUUID(), src: URL.createObjectURL(file) })))
  error.value = ''
}
function drop(key: string) {
  const item = queue.value.find(photo => photo.key === key)
  if (item) { previewOpen.value = false; revoke(item); queue.value = queue.value.filter(photo => photo.key !== key) }
}
async function saveTo(id: number) {
  if (!id || !queue.value.length) return
  if (busy.value) throw Error('图片正在上传')
  const v = generation
  target.value = id; busy.value = true; error.value = ''
  try {
    for (const item of queue.value.slice()) {
      if (v !== generation) throw Error('页面已变化')
      const body = new FormData()
      body.append('clientRequestId', item.key); body.append('file', item.file!)
      await uploadCatalogPhoto(path(id), body)
      if (v !== generation) throw Error('页面已变化')
      drop(item.key)
    }
    await load()
    if (v === generation) emit('changed')
  } catch (e) {
    if (v !== generation) throw e
    const message = e instanceof ApiError ? e.message : '照片上传失败：未收到具体原因，请重试'
    error.value = message
    throw new Error(message)
  } finally { if (v === generation) busy.value = false }
}
async function retry() { try { await saveTo(target.value) } catch { /* error is shown inline */ } }
async function remove(id?: number) {
  if (!id || busy.value) return
  const v = generation
  busy.value = true; error.value = ''
  try {
    unwrapApiResponse((await apiClient.delete(`${path(target.value)}/${id}`)).data)
    if (v === generation) { await load(); if (v === generation) emit('changed') }
  } catch { if (v === generation) error.value = '移除失败，请重试' }
  finally { if (v === generation) busy.value = false }
}
defineExpose({ saveTo, reset: clear })
</script>

<style scoped>
.catalog-pictures { min-width: 0; max-width: 100%; }
.catalog-layout-thumb, .catalog-thumb-frame { width: 100%; height: 100%; }
.catalog-thumb-frame { position: relative; overflow: hidden; }
.catalog-layout-detail { --default-detail-size: 220px; width: var(--catalog-detail-size, var(--default-detail-size)); }
.catalog-layout-detail .catalog-thumb-frame { width: 100%; height: var(--catalog-detail-size, var(--default-detail-size)); border-radius: 14px; }
.catalog-layout-gallery .catalog-thumb-frame { width: 96px; height: 96px; border-radius: 10px; }
.catalog-fallback { display: flex; flex-direction: column; gap: 4px; align-items: center; justify-content: center; width: 100%; height: 100%; padding: 6px; box-sizing: border-box; }
.catalog-fallback strong { font: 600 clamp(1.6rem, 3vw, 2.8rem)/1.1 Georgia, 'Songti SC', serif; }
.catalog-fallback small { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: .75rem; }
.catalog-open { display: block; width: 100%; height: 100%; padding: 0; border: 0; border-radius: inherit; background: var(--bg-surface-warm); color: var(--text-secondary); cursor: zoom-in; }
.catalog-open:focus-visible { outline: 3px solid var(--accent-primary); outline-offset: -3px; }
.catalog-open img { display: block; width: 100%; height: 100%; object-fit: cover; cursor: zoom-in; border-radius: inherit; }
.catalog-photo-count, .catalog-remove { position: absolute; color: #fffdfa; background: rgba(44, 31, 23, .82); border: 0; border-radius: 5px; padding: 2px 6px; font: 500 12px/1.5 sans-serif; }
.catalog-photo-count { bottom: 6px; right: 6px; pointer-events: none; }
.catalog-strip { display: flex; gap: 6px; max-width: 100%; overflow-x: auto; }
.catalog-layout-gallery .catalog-strip { flex-wrap: wrap; }
.catalog-layout-detail .catalog-strip:not(:empty) { margin-top: 6px; }
.catalog-strip figure { position: relative; flex: none; width: 72px; height: 72px; margin: 0; border-radius: 8px; }
.catalog-layout-gallery figure { width: 96px; height: 96px; }
.catalog-remove { bottom: 4px; right: 4px; cursor: pointer; }
.catalog-pictures input { max-width: 100%; margin-block: 8px; }
.catalog-caption { margin-bottom: 8px; }
.catalog-error { display: block; color: var(--accent-primary); }
.catalog-lightbox { display: grid; place-items: center; min-height: 80px; border-radius: 8px; overflow: hidden; background: var(--bg-surface-warm); }
.catalog-lightbox img { display: block; max-height: 75vh; max-width: 100%; object-fit: contain; }
.catalog-lightbox-nav { display: flex; align-items: center; justify-content: center; gap: 12px; font-variant-numeric: tabular-nums; }
@media (max-width: 900px) { .catalog-layout-detail { --default-detail-size: 104px; } }
</style>
