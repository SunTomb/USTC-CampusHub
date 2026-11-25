<template>
  <section class="page-stack">
    <div class="page-heading"><div><p class="eyebrow">跑腿互助 · 履约跟进</p><h2>跑腿履约台 #{{ taskId }}</h2><p>查看交接信息、上传照片，跟进送达进度。</p></div><el-button @click="router.push('/tasks')">返回跑腿大厅</el-button></div>
    <el-alert v-if="expired && isPublisher && workspace?.canWithdraw" title="这条委托已截止，不再接受新报名。你仍可撤下；若没有报名，也可修改截止时间后重新展示。" type="info" :closable="false"/>
    <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false"><el-button @click="load">重新加载</el-button></el-alert>
    <el-card v-if="workspace" v-loading="loading || busy" shadow="never" class="workspace-card premium-panel">
      <h3>{{ workspace.task.title }}</h3>
      <p>{{ workflowLabels[status] || workspace.task.status }} · {{ weightLabel(workspace.task.weightGrams) }}</p>
      <div v-if="isPublisher" class="workspace-actions">
        <el-button v-if="workspace.canEdit" :disabled="busy" @click="router.push(`/tasks?edit=${taskId}`)">修改委托</el-button>
        <el-button v-if="workspace.canWithdraw" :disabled="busy" type="warning" plain @click="withdraw">撤下委托</el-button>
        <small v-if="workspace.canWithdraw && !workspace.canEdit">已有同学报名，不能再修改委托内容。</small>
      </div>
      <div v-if="isPublisher && applicants.length"><h4>收到的报名</h4>
        <div v-for="a in applicants" :key="a.id" class="applicant-row"><span>{{ a.applicantNickname }} · {{ a.message || '未附留言' }}</span>
          <el-button v-if="!expired && status === 'PUBLISHED' && ['PENDING','APPLIED'].includes(a.status)" :disabled="busy" @click="choose(a)">选择这位同学</el-button>
        </div>
      </div>
      <div class="workspace-summary"><div v-for="contact in contacts" :key="contact.userId"><strong>{{ contact.userId === workspace.publisherContact.userId ? '发布者' : '接单同学' }} · {{ contact.nickname }}</strong><span v-if="contact.wechatContact">微信：{{ contact.wechatContact }}</span><span v-if="contact.qqContact">QQ：{{ contact.qqContact }}</span></div></div>
      <div v-if="workspace.privateDetails?.pickupCode" class="task-private-details"><strong>私密取件码</strong><code>{{ workspace.privateDetails.pickupCode }}</code></div>
      <el-steps v-if="workspace.runnerContact" :active="activeStep" finish-status="success" align-center><el-step v-for="label in stepLabels" :key="label" :title="label" /></el-steps>
      <div class="workspace-actions">
        <el-button v-for="(label, next) in transitions" v-show="canAdvance(next)" :key="next" type="primary" @click="advance(next)">{{ label }}</el-button>
      </div>
      <div v-for="kind in photoKinds" :key="kind" class="evidence-panel">
        <h3>{{ evidenceTypeLabel(kind) }}</h3>
        <div class="photo-previews"><span v-for="photo in group(kind)" :key="photo.id" class="evidence-photo">
          <img v-if="evidenceUrls.has(photo.id)" :src="evidenceUrls.get(photo.id)" :alt="evidenceTypeLabel(photo.evidenceType)" @click="preview = evidenceUrls.get(photo.id) || ''" />
          <small>{{ evidenceTypeLabel(photo.evidenceType) }} · {{ Math.ceil(photo.sizeBytes / 1024) }} KiB</small>
        </span></div>
        <small v-if="!group(kind).length">暂无照片</small>
        <div v-if="canUpload(kind)"><input type="file" accept="image/jpeg,image/png" multiple :disabled="busy" @change="selectPhotos($event, kind)" />
          <el-button v-if="queue(kind).length" :loading="busy" @click="uploadPhotos(kind)">上传所选照片（{{ queue(kind).filter(p => !p.uploaded).length }}）</el-button>
          <el-button v-if="queue(kind).length" :disabled="busy" @click="discardPhotos(queue(kind))">清除选择</el-button>
        </div>
      </div>
      <el-dialog :model-value="!!preview" title="参与方照片" @close="preview = ''"><img v-if="preview" :src="preview" alt="私密交接照片" style="max-width:100%;max-height:70vh" /></el-dialog>
      <el-button v-if="canSubmitDelivery" type="success" :disabled="busy || deliveryPhotos.length > 0" @click="submitDelivery">提交送达凭证</el-button>
      <el-button v-if="canConfirm" type="success" @click="confirm">确认送达</el-button>
      <div v-if="canIssueCode" class="completion-code-panel"><el-button @click="issueCode">{{ issuedCode ? '重新生成（旧码失效）' : '生成完成码' }}</el-button><p v-if="issuedCode">完成码：<strong>{{ issuedCode.completionCode }}</strong> · 有效至 {{ issuedCode.expiresAt }}</p></div>
      <div v-if="canRedeemCode" class="completion-code-panel"><el-input v-model="completionCode" maxlength="6" placeholder="输入发布者提供的6位完成码" /><el-button type="success" @click="completeWithCode">核销完成码</el-button></div>
      <el-timeline><el-timeline-item v-for="event in workspace.events" :key="event.id" :timestamp="event.createdAt">{{ event.note || event.eventType }}</el-timeline-item></el-timeline>
    </el-card>
    <el-card v-if="activeAssignment" shadow="never"><template #header>遇到突发状况？</template><el-form label-position="top" :model="issueForm"><el-form-item label="异常情况类别"><el-select v-model="issueForm.issueType"><el-option v-for="(label, type) in issueTypes" :key="type" :label="label" :value="type" /></el-select></el-form-item><el-form-item label="现场情况说明"><el-input v-model="issueForm.description" type="textarea" :rows="3" /></el-form-item><el-button :disabled="busy" @click="reportIssue">提交异常报备</el-button></el-form></el-card>
  </section>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getApi } from '@/api/client'
import { acceptRunnerTaskApplication, withdrawRunnerTask, type TaskApplicationSummary, advanceRunnerTask, confirmRunnerTask, completeRunnerTaskWithCode, issueRunnerCompletionCode, getTaskEvidenceBlob, getTaskWorkspace, reportRunnerTaskIssue, submitTaskDelivery, uploadTaskEvidence, type TaskEvidenceType, type TaskParticipantContact, type TaskWorkspaceSummary } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import { weightLabel, workflowLabels, discardPhotos, draftPhoto, photoProblem, taskError, uploadMissing, type DraftPhoto } from '@/utils/taskHandoff'

const route = useRoute(), router = useRouter(), auth = useAuthStore()
const taskId = computed(() => Number(route.params.id))
const workspace = ref<TaskWorkspaceSummary | null>(null), evidenceUrls = ref(new Map<number, string>())
const applicants=ref<TaskApplicationSummary[]>([])
const loading = ref(false), busy = ref(false), errorMessage = ref(''), completionCode = ref(''), preview = ref('')
const issuedCode = ref<Awaited<ReturnType<typeof issueRunnerCompletionCode>> | null>(null)
const issueForm = reactive({ issueType: 'LOCATION_ERROR', description: '' })
const issueTypes = { PICKUP_FAILED:'无法取件', LOCATION_ERROR:'地点有误', CONTACT_FAILED:'暂时联系不上', TIMEOUT:'预计延误', ITEM_ABNORMAL:'物品异常' }
const requestPhotos = ref<DraftPhoto[]>([]), deliveryPhotos = ref<DraftPhoto[]>([])
const photoKinds: TaskEvidenceType[] = ['REQUEST_IMAGE', 'DELIVERY_PHOTO']
const group = (kind: TaskEvidenceType) => workspace.value?.evidence?.filter(p => p.evidenceType === kind) ?? []
const queue = (kind: TaskEvidenceType) => kind === 'REQUEST_IMAGE' ? requestPhotos.value : deliveryPhotos.value
const contacts = computed(() => [workspace.value?.publisherContact,workspace.value?.runnerContact].filter((p): p is TaskParticipantContact => !!p))
const isPublisher = computed(() => !!workspace.value && workspace.value.publisherContact.userId === auth.currentUser?.id)
const isRunner = computed(() => !!workspace.value?.runnerContact && workspace.value.runnerContact.userId === auth.currentUser?.id)
const status = computed(() => workspace.value?.task.workflowStatus ?? '')
const expired=computed(()=>!!workspace.value && new Date(workspace.value.task.deadline).getTime()<=Date.now())
const codeMode = computed(() => workspace.value?.task.verificationMode === 'COMPLETION_CODE')
const deliveryStage = computed(() => ['DELIVERING','DELIVERED'].includes(status.value))
const activeAssignment = computed(() => !!workspace.value?.runnerContact && ['ACCEPTED','HEADING_TO_PICKUP','PICKED_UP','DELIVERING','DELIVERED','PENDING_CONFIRMATION'].includes(status.value))
const deliveryEditable = computed(() => isRunner.value && workspace.value?.task.verificationMode === 'PHOTO_AND_CONFIRMATION' && deliveryStage.value)
const canSubmitDelivery = computed(() => deliveryEditable.value && group('DELIVERY_PHOTO').length > 0)
const canConfirm = computed(() => isPublisher.value && status.value === 'PENDING_CONFIRMATION' && (codeMode.value || group('DELIVERY_PHOTO').length > 0))
const canRedeemCode = computed(() => isRunner.value && codeMode.value && status.value === 'PENDING_CONFIRMATION')
const canIssueCode = computed(() => isPublisher.value && codeMode.value && activeAssignment.value)
const stepLabels = ['已接单', '前往取件', '已取件', '派送途中', '送达确认']
const activeStep = computed(() => Math.max(0, ['ACCEPTED','HEADING_TO_PICKUP','PICKED_UP','DELIVERING','PENDING_CONFIRMATION','COMPLETED'].indexOf(status.value === 'DELIVERED' ? 'PENDING_CONFIRMATION' : status.value)))
function canUpload(kind: TaskEvidenceType) { return kind === 'REQUEST_IMAGE' ? isPublisher.value && (status.value === 'PUBLISHED' || activeAssignment.value) : deliveryEditable.value }
function evidenceTypeLabel(type: TaskEvidenceType) { return type === 'REQUEST_IMAGE' ? '物品照片' : '送达照片' }
function revokeImages() { preview.value = ''; evidenceUrls.value.forEach(url => URL.revokeObjectURL(url)); evidenceUrls.value.clear() }
let context = new AbortController(), fetchController: AbortController | undefined
function reset() {
  context.abort(); fetchController?.abort(); context = new AbortController()
  workspace.value = null; applicants.value=[]; revokeImages(); discardPhotos(requestPhotos.value); discardPhotos(deliveryPhotos.value)
  completionCode.value = ''; issuedCode.value = null; issueForm.description = ''; errorMessage.value = ''; loading.value = busy.value = false
}
async function load() {
  fetchController?.abort()
  if (!auth.token || !auth.currentUser || document.hidden || !Number.isSafeInteger(taskId.value) || taskId.value < 1) return
  fetchController = new AbortController(); const signal = fetchController.signal, id = taskId.value
  loading.value = true; errorMessage.value = ''
  try {
    const data = await getTaskWorkspace(id, {signal}); if (signal.aborted) return
    revokeImages(); workspace.value = data
    if (!canIssueCode.value) issuedCode.value = null
    if(isPublisher.value && data.task.acceptanceMode==='APPLICATION' && data.task.workflowStatus==='PUBLISHED'){
      const rows=await getApi<TaskApplicationSummary[]>(`/task-applications/task/${id}`,{signal}); if(signal.aborted)return;applicants.value=rows
    }else applicants.value=[]
    await Promise.all((data.evidence ?? []).map(async photo => {
      try {
        const blob = await getTaskEvidenceBlob(id, photo.id, {signal}); if (!signal.aborted) evidenceUrls.value.set(photo.id, URL.createObjectURL(blob))
      } catch { if (!signal.aborted) errorMessage.value = '部分照片未加载，请重新加载。' }
    }))
  } catch (error) { if (!signal.aborted) { reset(); errorMessage.value = taskError(error, '无法加载履约台') } }
  finally { if (!signal.aborted) loading.value = false }
}
function visibility() { reset(); if (!document.hidden) void load() }
watch([taskId, () => auth.token, () => auth.currentUser?.id], visibility, {flush:'sync'})
onMounted(() => { document.addEventListener('visibilitychange', visibility); void load() })
onBeforeUnmount(() => { document.removeEventListener('visibilitychange', visibility); reset() })
async function action(work: (id:number, user:number, options:{signal:AbortSignal}) => Promise<unknown>, fallback:string) {
  if (busy.value || loading.value || !workspace.value || !auth.currentUser) return
  const signal = context.signal; busy.value = true
  try { await work(taskId.value, auth.currentUser.id, {signal}); if (!signal.aborted) await load() }
  catch (error) { if (!signal.aborted) ElMessage.error(taskError(error, fallback)) }
  finally { if (!signal.aborted) busy.value = false }
}
async function withdraw(){
  if(!isPublisher.value||!workspace.value?.canWithdraw||busy.value)return
  const signal=context.signal, id=taskId.value
  try{await ElMessageBox.confirm('撤下后不再接单；如有待选报名，对方会收到通知。记录保留在「我的订单」。','撤下委托',{type:'warning'})}catch{return}
  if(!signal.aborted && id===taskId.value)return action((task,_,o)=>withdrawRunnerTask(task,o),'未能撤下，请刷新任务状态')
}
async function choose(a:TaskApplicationSummary){
  if(!isPublisher.value||busy.value||expired.value||status.value!=='PUBLISHED'||!['PENDING','APPLIED'].includes(a.status))return
  const signal=context.signal
  try{await ElMessageBox.confirm(`确认将委托交给「${a.applicantNickname}」？`,'选择接单同学')}catch{return}
  if(!signal.aborted)return action((task,user,o)=>acceptRunnerTaskApplication(task,a.id,user,o),'未能选择报名，请刷新后重试')
}
const transitions = { HEADING_TO_PICKUP:'出发前往取件点', PICKED_UP:'已拿到物品', DELIVERING:'开始派送', PENDING_CONFIRMATION:'已送达，等待确认' }
function canAdvance(next:string) {
  if (next === 'PENDING_CONFIRMATION') return isRunner.value && codeMode.value && deliveryStage.value
  return isRunner.value && ({ HEADING_TO_PICKUP:'ACCEPTED', PICKED_UP:'HEADING_TO_PICKUP', DELIVERING:'PICKED_UP' } as Record<string,string>)[next] === status.value
}
function advance(next:string) { if (canAdvance(next)) return action((id,u,o) => advanceRunnerTask(id,u,next,{},o), '状态更新失败') }
function issueCode() { if (canIssueCode.value) return action(async (id,_,o) => { const result = await issueRunnerCompletionCode(id,o); if (!o.signal.aborted) issuedCode.value = result }, '完成码生成失败') }
function completeWithCode() {
  if (!canRedeemCode.value) return
  if (!/^\d{6}$/.test(completionCode.value)) return ElMessage.warning('请输入六位数字完成码')
  return action(async (id,u,o) => { await completeRunnerTaskWithCode(id,u,{completionCode:completionCode.value},o); if (!o.signal.aborted) completionCode.value = '' }, '完成码核验失败')
}
function confirm() { if (canConfirm.value) return action((id,u,o) => confirmRunnerTask(id,u,{},o), '确认失败') }
function selectPhotos(event:Event, kind:TaskEvidenceType) {
  const input = event.target as HTMLInputElement, files = Array.from(input.files ?? []); input.value = ''
  if (!canUpload(kind) || busy.value) return
  const selected = queue(kind), bytes = (workspace.value?.evidence ?? []).reduce((n,p) => n+p.sizeBytes,0) + [...requestPhotos.value,...deliveryPhotos.value].reduce((n,p) => n+p.file.size,0)
  const problem = photoProblem(files,group(kind).length + selected.length,bytes)
  if (problem) return ElMessage.error(problem)
  selected.push(...files.map(draftPhoto))
}
function uploadPhotos(kind:TaskEvidenceType) {
  if (!canUpload(kind)) return
  return action(async (id,_,o) => {
    const selected = queue(kind)
    await uploadMissing(selected,o.signal,p => uploadTaskEvidence(id,kind,p.file,p.clientRequestId,o)); if (!o.signal.aborted) discardPhotos(selected)
  },'照片上传失败，可重试未完成照片')
}
function submitDelivery() { if (canSubmitDelivery.value && !deliveryPhotos.value.length) return action((id,_,o) => submitTaskDelivery(id,o), '送达凭证提交失败') }
function reportIssue() { if (activeAssignment.value) return action((id,u,o) => reportRunnerTaskIssue(id,u,{...issueForm},o), '异常报备失败') }
</script>
<style scoped>
.applicant-row {display:flex;align-items:center;justify-content:space-between;gap:.75rem;flex-wrap:wrap;margin:.5rem 0}
.photo-previews { display:flex; gap:.75rem; flex-wrap:wrap }
.evidence-photo { display:grid; gap:.25rem }
.evidence-photo img { width:7rem; height:7rem; object-fit:cover; border-radius:.5rem; cursor:zoom-in }
</style>
