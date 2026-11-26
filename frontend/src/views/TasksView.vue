<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">顺路互助 · 悬赏告示栏</p>
        <h2>校园跑腿大厅</h2>
        <p>跨校区代取快递、急送实验材料或食堂顺路带饭——支持直接抢单或报名筛选。</p>
      </div>
      <div class="heading-actions">
        <el-button v-if="auth.currentUser" @click="router.push('/orders?type=TASK')">我的订单</el-button>
        <el-button v-if="auth.currentUser" :loading="mineLoading" @click="loadMine">我的履约台</el-button>
        <el-button @click="loadTasks" :loading="loading">刷新告示</el-button>
        <el-button type="primary" @click="openPublishDialog">张贴悬赏单</el-button>
      </div>
    </div>

    <el-card v-if="mine !== null" shadow="never"><h3>我的履约台</h3>
      <p v-if="!mine.length">暂无已发布或已承接的跑腿。</p>
      <el-button v-for="item in mine" :key="item.id" @click="openWorkspace(item.id)">{{ item.role }} · {{ item.title }} #{{ item.id }}</el-button>
    </el-card>
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters" class="compact-filter-form">
        <el-form-item label="取件校区">
          <el-select v-model="filters.originZone" clearable placeholder="全部校区" style="width: 150px">
            <el-option v-for="zone in zones" :key="zone.value" :label="zone.label" :value="zone.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="送达校区">
          <el-select v-model="filters.destinationZone" clearable placeholder="全部校区" style="width: 150px">
            <el-option v-for="zone in zones" :key="zone.value" :label="zone.label" :value="zone.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="接单方式">
          <el-select v-model="filters.acceptanceMode" clearable placeholder="全部方式" style="width: 130px">
            <el-option label="即时抢单" value="GRAB" />
            <el-option label="报名筛选" value="APPLICATION" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="task-card-grid" v-loading="loading">
      <article v-for="task in filteredTasks" :key="task.id" class="runner-ticket-card">
        <div class="ticket-main">
          <div class="panel-topline">
            <div class="ticket-badges">
              <span class="ticket-mode-badge" :class="task.acceptanceMode === 'GRAB' ? 'mode-grab' : 'mode-apply'">
                {{ modeLabel(task.acceptanceMode) }}
              </span>
              <span class="ticket-verify-badge">
                {{ task.verificationMode === 'PHOTO_AND_CONFIRMATION' ? '拍照凭证交接' : '凭完成码交接' }}
              </span>
            </div>
            <span class="ticket-deadline">截止 {{ formatDate(task.deadline) }}</span>
          </div>

          <h3>{{ task.title }}</h3>
          <p class="ticket-desc">{{ task.description || '发布者未填写额外说明' }}</p>
          <div class="ticket-public-details">
            <span>估重：{{ weightLabel(task.weightGrams) }}</span>
            <span v-if="task.notes">备注：{{ task.notes }}</span>
          </div>

          <div class="ticket-route-box">
            <div class="route-stop">
              <span class="stop-dot stop-origin">起</span>
              <div>
                <strong>{{ zoneLabel(task.originZone) }}</strong>
                <small>{{ task.originDetail || '接单后沟通具体位置' }}</small>
              </div>
            </div>
            <div class="route-arrow">⎯⎯ 顺路派送 ⎯→</div>
            <div class="route-stop">
              <span class="stop-dot stop-dest">终</span>
              <div>
                <strong>{{ zoneLabel(task.destinationZone) }}</strong>
                <small>{{ task.destinationDetail || '接单后沟通具体位置' }}</small>
              </div>
            </div>
          </div>
        </div>


        <div class="ticket-stub">
          <div class="ticket-reward">
            <span>悬赏酬金</span>
            <strong class="mono-num">¥{{ task.rewardAmount }}</strong>
            <small>委托人：{{ task.publisherNickname }}</small>
          </div>
          <div class="card-actions">
            <el-button v-if="task.publisherId !== auth.currentUser?.id && task.acceptanceMode === 'GRAB'" type="primary" @click="grab(task.id)">立即抢单</el-button>
            <el-button v-else-if="task.publisherId !== auth.currentUser?.id" type="primary" @click="openApply(task)">报名接单</el-button>
            <el-button v-if="task.publisherId === auth.currentUser?.id" plain @click="openWorkspace(task.id)">履约台</el-button>
          </div>
        </div>
      </article>

      <EmptyState
        v-if="!loading && filteredTasks.length === 0"
        eyebrow="悬赏告示栏"
        title="当前筛选下暂无待接跑腿单"
        description="可以切换其他校区看看，或者直接张贴一张新的跑腿求助单。"
        action-text="张贴悬赏单"
        @action="openPublishDialog"
      />
    </div>

    <el-drawer v-model="publisherOpen" append-to-body :title="editId ? '修改跑腿委托' : '张贴跑腿悬赏单'" size="min(520px, 100vw)" destroy-on-close :before-close="beforeClosePublish">
      <p v-if="editLoading" role="status">正在读取你的委托…</p>
      <el-alert v-else-if="editError" :title="editError" type="warning" :closable="false"><el-button v-if="editId" text @click="openEditor(editId)">刷新编辑权限</el-button></el-alert>
      <el-form v-else-if="!pendingTaskId && !uncertain" :disabled="publishing" ref="publishFormRef" label-position="top" :model="publishForm" :rules="publishRules" class="form-section-stack">
        <FormSection title="任务信息、路线与时限" description="填写取件点、送达地点和截止时间。">
          <div class="two-column">
            <el-form-item label="任务标题" prop="title">
              <el-input v-model="publishForm.title" placeholder="例如：西区近邻宝代取中件快递送到中区宿舍" />
            </el-form-item>
            <el-form-item label="预估重量（kg）" prop="weightKg">
              <el-input v-model="publishForm.weightKg" inputmode="decimal" placeholder="0.001–100 kg" />
            </el-form-item>
          </div>
          <el-form-item label="公开说明（可选）" prop="description">
            <el-input v-model="publishForm.description" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="可补充物品类型、包装情况或注意事项" />
          </el-form-item>
          <el-form-item label="公开备注（可选）" prop="notes">
            <el-input v-model="publishForm.notes" type="textarea" :rows="2" maxlength="1000" show-word-limit placeholder="如：易碎、需要保温袋、放宿舍楼下等" />
          </el-form-item>
          <el-form-item label="起点 / 终点校区" required>
            <div class="two-column">
              <el-select v-model="publishForm.originZone">
                <el-option v-for="zone in zones" :key="zone.value" :label="zone.label" :value="zone.value" />
              </el-select>
              <el-select v-model="publishForm.destinationZone">
                <el-option v-for="zone in zones" :key="zone.value" :label="zone.label" :value="zone.value" />
              </el-select>
            </div>
          </el-form-item>
          <el-form-item label="具体地点描述" required>
            <div class="two-column">
              <el-input v-model="publishForm.originDetail" placeholder="取件点（如：西区菜鸟驿站）" />
              <el-input v-model="publishForm.destinationDetail" placeholder="送达点（如：中区 2 号楼下）" />
            </div>
          </el-form-item>
          <el-form-item label="期望送达截止时间" prop="deadline">
            <el-date-picker v-model="deadlineDate" type="datetime" class="wide" popper-class="responsive-date-popper" />
          </el-form-item>
          <el-form-item label="取件码" prop="pickupCode">
            <el-input v-model="publishForm.pickupCode" maxlength="120" show-password placeholder="选填，仅任务参与方可见" />
          </el-form-item>
          <p v-if="editId" class="hint">原有照片会保留；可在履约台管理参与方照片。</p>
          <el-form-item v-else label="物品照片（可选，最多 3 张）">
            <input type="file" accept="image/jpeg,image/png" multiple :disabled="publishing" @change="onPublishPhotos" />
            <div class="photo-previews">
              <span v-if="!publishPhotos.length" class="draft-photo-fallback cover-tone-0" aria-label="暂无物品照片">物</span>
              <figure v-for="photo in publishPhotos" :key="photo.clientRequestId">
                <button type="button" class="task-photo-button" @click="previewDraftPhoto = photo.url"><img :src="photo.url" alt="预览待上传物品照片" /></button>
                <button type="button" :disabled="publishing" @click="removePublishPhoto(photo.clientRequestId)">移除</button>
              </figure>
            </div>
            <small class="hint">照片仅供参与者查看；每张不超过 5 MiB。</small>
          </el-form-item>
        </FormSection>

        <FormSection title="酬金与交接方式" description="常规跑腿推荐即时抢单；贵重实验器材或复杂代办可选报名筛选。">
          <el-form-item label="悬赏报酬（元）" prop="rewardAmount">
            <el-input-number v-model="publishForm.rewardAmount" :min="0" :precision="2" class="wide" />
          </el-form-item>
          <el-form-item label="接单与验收模式" required>
            <div class="two-column">
              <el-select v-model="publishForm.acceptanceMode">
                <el-option label="即时抢单（先到先得）" value="GRAB" />
                <el-option label="报名筛选（由你挑人）" value="APPLICATION" />
              </el-select>
              <el-select v-model="publishForm.verificationMode">
                <el-option label="凭完成码交接" value="COMPLETION_CODE" />
                <el-option label="拍照凭证 + 确认" value="PHOTO_AND_CONFIRMATION" />
              </el-select>
            </div>
          </el-form-item>
          <p class="hint">跑腿酬金由双方在送达后直接结算。</p>
        </FormSection>
        <el-button type="primary" class="wide" :loading="publishing" @click="publish">{{ editId ? '保存修改' : '确认张贴悬赏单' }}</el-button>
      </el-form>
      <div v-else-if="pendingTaskId"><p>悬赏单 #{{ pendingTaskId }} 已发布，不要重复张贴。</p>
        <el-button v-if="publishPhotos.some(p => !p.uploaded)" :loading="publishing" @click="publish">重试未完成照片</el-button>
        <el-button :disabled="publishing" @click="openWorkspace(pendingTaskId)">打开已有履约台</el-button></div>
      <div v-else><p>未收到发布结果。请先在“我的订单”核对任务，再决定是否重新张贴。</p><el-button @click="acknowledgeUncertain">查看我的订单</el-button></div>
    </el-drawer>

    <TaskPhotoPreview :src="previewDraftPhoto" alt="待上传物品照片" @close="previewDraftPhoto = ''" />
    <el-dialog v-model="applyOpen" append-to-body title="报名承接跑腿" width="420px">
      <el-input v-model="applyMessage" type="textarea" :rows="4" placeholder="向委托人说明你目前所在校区、预计送达时间或顺路情况" />
      <template #footer>
        <el-button @click="applyOpen = false">取消</el-button>
        <el-button type="primary" @click="apply">发送接单申请</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  applyRunnerTask,
  grabRunnerTask,
  listTasks, listMyPublishedTasks, listMyTaskApplications,
  publishRunnerTask, updateRunnerTask, getTaskWorkspace,
  type CampusZone,
  type CreateRunnerTaskPayload,
  type RewardTaskSummary,
  type TaskAcceptanceMode,
  uploadTaskEvidence,
} from '@/api/campushub'
import { weightGrams, weightLabel, photoProblem, taskError, draftPhoto, discardPhotos, uploadMissing, type DraftPhoto } from '@/utils/taskHandoff'
import { useAuthStore } from '@/stores/auth'
import EmptyState from '@/components/common/EmptyState.vue'
import FormSection from '@/components/common/FormSection.vue'
import TaskPhotoPreview from '@/components/TaskPhotoPreview.vue'

const router = useRouter(), route = useRoute()
const auth = useAuthStore()
const tasks = ref<RewardTaskSummary[]>([])
const loading = ref(false)
const publisherOpen = ref(false)
const publishing = ref(false)
const applyOpen = ref(false)
const applyTaskId = ref<number | null>(null)
const applyMessage = ref('')
const publishFormRef = ref<FormInstance>()
const deadlineDate = ref<Date>(new Date(Date.now() + 2 * 60 * 60 * 1000))
const previewDraftPhoto = ref('')
const publishPhotos = ref<DraftPhoto[]>([])
const editId=ref<number|null>(null), editLoading=ref(false), editError=ref('')
const pendingTaskId = ref<number | null>(null), uncertain = ref(false)
const mine = ref<{id:number; title:string; role:string}[] | null>(null), mineLoading = ref(false)
let session = new AbortController()
function clearHistory() { session.abort(); session = new AbortController(); mine.value = null; mineLoading.value = false }
async function loadMine() {
  if (!auth.currentUser || mineLoading.value || document.hidden) return
  const signal = session.signal; mineLoading.value = true
  try {
    const [published, applications] = await Promise.all([listMyPublishedTasks({signal}), listMyTaskApplications(auth.currentUser.id,{signal})])
    if (!signal.aborted) mine.value = [...published.map(t => ({id:t.id,title:t.title,role:'发布'})),
      ...applications.filter(a => ['ACCEPTED','COMPLETED'].includes(a.status)).map(a => ({id:a.taskId,title:a.taskTitle,role:'承接'}))]
  } catch(error) { if (!signal.aborted) ElMessage.error(taskError(error,'履约记录加载失败')) }
  finally { if (!signal.aborted) mineLoading.value = false }
}
let controller = new AbortController(), creating = false
function clearDraft() {
  controller.abort(); controller = new AbortController(); publishing.value = creating = false
  Object.assign(publishForm, initialForm()); deadlineDate.value = new Date(Date.now() + 2 * 60 * 60 * 1000)
  previewDraftPhoto.value = ''
  discardPhotos(publishPhotos.value)
  editId.value=null; editLoading.value=false; editError.value=''
}
function beforeClosePublish(done: () => void) {
  if (publishing.value) return
  clearDraft(); pendingTaskId.value = null; done(); if(route.query.edit != null) void router.replace('/tasks')
}
function acknowledgeUncertain() { uncertain.value = false; clearDraft(); publisherOpen.value = false; void router.push('/orders?type=TASK') }
function clearPrivate() {
  clearHistory()
  publisherOpen.value = false; applyOpen.value = false; applyMessage.value = ''; applyTaskId.value = null
  clearDraft(); pendingTaskId.value = null; uncertain.value = false
}
function hideDraft() {
  if (!document.hidden) {
    if (route.query.edit != null && !publisherOpen.value && !pendingTaskId.value) void openEditor(Number(route.query.edit))
    return
  }
  clearHistory()
  // A canceled create response cannot tell us whether the server committed the task.
  if (creating) uncertain.value = true
  clearDraft(); publisherOpen.value = false; applyOpen.value = false; applyMessage.value = ''
}
function onPublishPhotos(event: Event) {
  if (!publisherOpen.value || publishing.value || pendingTaskId.value || !auth.currentUser) return
  const files = Array.from((event.target as HTMLInputElement).files ?? [])
  ;(event.target as HTMLInputElement).value = ''
  const problem = photoProblem(files, publishPhotos.value.length)
  if (problem) {
    ElMessage.error(problem)
    ;(event.target as HTMLInputElement).value = ''
    return
  }
  publishPhotos.value.push(...files.map(draftPhoto))
}
function removePublishPhoto(clientRequestId: string) {
  const index = publishPhotos.value.findIndex(photo => photo.clientRequestId === clientRequestId)
  if (index >= 0) {
    if (previewDraftPhoto.value === publishPhotos.value[index].url) previewDraftPhoto.value = ''
    URL.revokeObjectURL(publishPhotos.value[index].url)
    publishPhotos.value.splice(index, 1)
  }
}

const zones: Array<{ label: string; value: CampusZone }> = [
  { label: '中校区', value: 'CENTRAL' },
  { label: '西校区', value: 'WEST' },
  { label: '东校区', value: 'EAST' },
  { label: '北校区', value: 'NORTH' },
  { label: '南校区', value: 'SOUTH' },
  { label: '高新校区', value: 'HIGH_TECH' },
  { label: '先研院', value: 'ADVANCED_RESEARCH_INSTITUTE' },
  { label: '科学岛', value: 'SCIENCE_ISLAND' },
  { label: '其他', value: 'OTHER' },
]

const filters = reactive<{ originZone?: CampusZone; destinationZone?: CampusZone; acceptanceMode?: TaskAcceptanceMode }>({})
const initialForm = (): CreateRunnerTaskPayload & { weightKg: string } => ({
  title: '',
  description: '',
  weightGrams: 1,
  weightKg: '',
  notes: '',
  pickupCode: '',
  rewardAmount: 5,
  acceptanceMode: 'GRAB',
  originZone: 'CENTRAL',
  destinationZone: 'CENTRAL',
  originDetail: '',
  destinationDetail: '',
  deadline: new Date().toISOString(),
  verificationMode: 'COMPLETION_CODE',
})

const publishForm = reactive(initialForm())

const publishRules: FormRules<CreateRunnerTaskPayload & { weightKg: string }> = {
  title: [
    { required: true, message: '请填写任务标题', trigger: 'blur' },
    { min: 1, max: 120, message: '标题长度应为 1-120 个字符', trigger: 'blur' },
  ],
  description: [{ max: 2000, message: '公开说明不能超过 2000 个字符', trigger: 'blur' }],
  notes: [{ max: 1000, message: '公开备注不能超过 1000 个字符', trigger: 'blur' }],
  pickupCode: [{ max: 120, message: '取件码不能超过 120 个字符', trigger: 'blur' }],
  weightKg: [{ required: true, message: '请填写预估重量', trigger: 'blur' }],
  rewardAmount: [
    { required: true, message: '请填写任务报酬', trigger: 'change' },
    { type: 'number', min: 0, message: '报酬不能为负数', trigger: 'change' },
  ],
  deadline: [{ required: true, message: '请选择截止时间', trigger: 'change' }],
}

const filteredTasks = computed(() =>
  tasks.value.filter((task) => {
    if (filters.originZone && task.originZone !== filters.originZone) return false
    if (filters.destinationZone && task.destinationZone !== filters.destinationZone) return false
    if (filters.acceptanceMode && task.acceptanceMode !== filters.acceptanceMode) return false
    return true
  }),
)

async function loadTasks() {
  loading.value = true
  try {
    tasks.value = await listTasks()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '任务加载失败')
  } finally {
    loading.value = false
  }
}

function openPublishDialog() {
  if (!requireLogin()) return
  if(route.query.edit != null){void openEditor(Number(route.query.edit));return}
  publisherOpen.value = true
}

async function openEditor(id:number) {
  if(!auth.currentUser || !auth.token || document.hidden)return
  clearDraft(); pendingTaskId.value=null; uncertain.value=false; publisherOpen.value=true; editId.value=id; editLoading.value=true
  const signal=controller.signal, user=auth.currentUser.id
  try {
    if(!Number.isSafeInteger(id)||id<1)throw Error('无效的委托编号')
    const w=await getTaskWorkspace(id,{signal}); if(signal.aborted)return
    if(!w.canEdit || w.task.publisherId!==user)throw Error('该委托已有报名、已被接单或已结束，不能修改')
    const t=w.task
    Object.assign(publishForm,{title:t.title,description:t.description||'',rewardAmount:t.rewardAmount,
      originZone:t.originZone,destinationZone:t.destinationZone,originDetail:t.originDetail||'',destinationDetail:t.destinationDetail||'',
      acceptanceMode:t.acceptanceMode,verificationMode:t.verificationMode,weightKg:t.weightGrams==null?'':String(t.weightGrams/1000),
      notes:t.notes||'',pickupCode:w.privateDetails?.pickupCode||'',deadline:t.deadline})
    deadlineDate.value=new Date(t.deadline)
  }catch(e){if(!signal.aborted)editError.value=taskError(e,'无法读取这条委托')}finally{if(!signal.aborted)editLoading.value=false}
}
function localDeadline(date:Date){const pad=(n:number)=>String(n).padStart(2,'0');return `${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`}

async function publish() {
  if (!publisherOpen.value || !auth.currentUser || !auth.token || publishing.value || uncertain.value || editLoading.value || editError.value) return
  publishing.value = true
  const signal = controller.signal, userId = auth.currentUser.id
  try {
    let id = pendingTaskId.value
    if (!id) {
      if (!deadlineDate.value || !(deadlineDate.value.getTime() > Date.now())) return ElMessage.error('请选择未来的截止时间')
      publishForm.deadline = localDeadline(deadlineDate.value)
      if (publishFormRef.value && !await publishFormRef.value.validate().catch(() => false)) return
      if (signal.aborted) return
      const grams = weightGrams(publishForm.weightKg)
      if (grams == null) return ElMessage.error('重量需为 0.001–100 kg，精确到克')
      if (!publishForm.originDetail?.trim() || !publishForm.destinationDetail?.trim()) return ElMessage.error('请填写起点和终点地点详情')
      const { weightKg: _, ...fields } = publishForm
      const payload={...fields, weightGrams:grams, description:fields.description.trim(), notes:fields.notes?.trim() || undefined,
        pickupCode:fields.pickupCode?.trim() || undefined, originDetail:fields.originDetail?.trim(), destinationDetail:fields.destinationDetail?.trim()}
      if (editId.value) {
        const id=editId.value
        await updateRunnerTask(id,payload,{signal}); if(signal.aborted)return
        publisherOpen.value=false; clearDraft(); ElMessage.success('委托已修改'); openWorkspace(id); return
      }
      creating = true
      try {
        const created = await publishRunnerTask(payload,userId,{signal})
        if (signal.aborted) return
        id = pendingTaskId.value = created.id; publishForm.pickupCode = ''
      } catch (error) {
        const e = error as {response?:{status?:number};status?:number}
        const status = e.response?.status ?? e.status
        if (!signal.aborted && (!status || status >= 500 || status === 408)) uncertain.value = true
        throw error
      } finally { if (!signal.aborted) creating = false }
    }
    const task = id
    await uploadMissing(publishPhotos.value, signal, photo => uploadTaskEvidence(task, 'REQUEST_IMAGE', photo.file, photo.clientRequestId, {signal}))
    if (signal.aborted) return
    ElMessage.success('悬赏单已张贴'); publisherOpen.value = false; pendingTaskId.value = null; clearDraft(); await loadTasks()
  } catch (error) {
    if (!signal.aborted) ElMessage.error(pendingTaskId.value ? `任务已发布，照片未全部上传：${taskError(error,'请重试')}` : taskError(error,editId.value?'修改未完成':'发布未完成'))
  } finally { if (!signal.aborted) publishing.value = false }
}

async function grab(taskId: number) {
  if (!requireRunner()) return
  const user = auth.currentUser
  if (!user) return
  const signal = session.signal
  await grabRunnerTask(taskId, user.id,{signal})
  if (!signal.aborted) openWorkspace(taskId)
}

function openApply(task: RewardTaskSummary) {
  if (!requireRunner()) return
  applyTaskId.value = task.id
  applyMessage.value = ''
  applyOpen.value = true
}

async function apply() {
  if (!requireRunner() || !applyTaskId.value || !auth.currentUser) return
  await applyRunnerTask(applyTaskId.value, auth.currentUser.id, { message: applyMessage.value })
  ElMessage.success('接单申请已发送，请留意站内通知')
  applyOpen.value = false
}

function openWorkspace(taskId: number) {
  if (!auth.currentUser) {
    ElMessage.warning('请先登录后查看履约台')
    router.push('/auth')
    return
  }
  router.push(`/tasks/${taskId}/workspace`)
}

function requireRunner() {
  if (!auth.currentUser) {
    ElMessage.warning('请先登录后再接单或查看履约台')
    router.push('/auth')
    return false
  }
  if (!auth.canAccessIdentity('runner')) {
    ElMessage.warning('请先阅读并同意顺路跑腿服务协议')
    router.push('/roles')
    return false
  }
  return true
}

function requireLogin() {
  if (auth.currentUser) return true
  ElMessage.warning('请先登录后再发布、抢单或报名跑腿任务')
  router.push('/auth')
  return false
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function zoneLabel(value: CampusZone) {
  return zones.find((zone) => zone.value === value)?.label ?? value
}

function modeLabel(value: TaskAcceptanceMode) {
  return value === 'GRAB' ? '即时抢单' : '报名筛选'
}

watch([() => auth.token, () => auth.currentUser?.id], clearPrivate, {flush:'sync'})
watch([()=>route.query.edit,()=>auth.currentUser?.id,()=>auth.token],([raw])=>{
  if(raw!=null)void openEditor(Number(raw))
  else if(editId.value!==null){clearDraft();publisherOpen.value=false}
}, {immediate:true})
watch(() => route.query.publish === '1' || route.query.open === 'publish', open => {
  if (open && !publisherOpen.value && auth.currentUser) openPublishDialog()
}, {immediate:true})
onMounted(() => { document.addEventListener('visibilitychange', hideDraft); void loadTasks() })
onBeforeUnmount(() => { document.removeEventListener('visibilitychange', hideDraft); clearPrivate() })
</script>
<style scoped>
.photo-previews { display:flex; flex-wrap:wrap; gap:.75rem }
.photo-previews figure { margin:0; display:grid; gap:.25rem }
.photo-previews img, .draft-photo-fallback { width:96px; height:96px; object-fit:cover; border-radius:.5rem }
.draft-photo-fallback { display:grid; place-items:center; font:600 2rem var(--font-display); }
</style>
