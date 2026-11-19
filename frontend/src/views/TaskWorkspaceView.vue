<template>
  <section class="page-stack">
    <div class="page-heading">
      <div><p class="eyebrow">跑腿互助 · 履约跟进</p><h2>跑腿履约台 <span class="mono-num">#{{ taskId }}</span></h2><p>只向任务参与方展示私密交接信息和照片凭证。</p></div>
      <el-button @click="router.push('/tasks')">返回跑腿大厅</el-button>
    </div>
    <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" />
    <el-card v-if="workspace" v-loading="loading" shadow="never" class="workspace-card premium-panel">
      <div class="workspace-summary"><div><span>任务</span><strong>{{ workspace.task.title }}</strong></div><div><span>发布者</span><strong>{{ workspace.publisherContact.nickname }}</strong></div><div><span>接单同学</span><strong>{{ workspace.runnerContact?.nickname || '等待接单' }}</strong></div></div>
      <div class="task-private-details" v-if="workspace.privateDetails?.pickupCode"><strong>私密取件码</strong><code>{{ workspace.privateDetails.pickupCode }}</code></div>
      <el-steps :active="activeStep" finish-status="success" align-center><el-step v-for="label in stepLabels" :key="label" :title="label" /></el-steps>
      <div class="workspace-actions">
        <el-button v-if="canAdvance('HEADING_TO_PICKUP')" type="primary" plain @click="advance('HEADING_TO_PICKUP')">出发前往取件点</el-button>
        <el-button v-if="canAdvance('PICKED_UP')" type="primary" plain @click="advance('PICKED_UP')">已拿到物品</el-button>
        <el-button v-if="canAdvance('DELIVERING')" type="primary" @click="advance('DELIVERING')">开始派送</el-button>
      </div>
      <div v-if="isPhotoMode && isRunner && ['DELIVERING', 'DELIVERED'].includes(workspace.task.workflowStatus)" class="evidence-panel">
        <h3>上传送达照片</h3><input type="file" accept="image/jpeg,image/png" multiple @change="selectPhotos($event, 'DELIVERY_PHOTO')" />
        <div class="photo-previews"><span v-for="photo in workspace.evidence?.filter(photo => photo.evidenceType === 'DELIVERY_PHOTO')" :key="photo.id" class="evidence-photo"><img :src="evidenceUrls.get(photo.id)" :alt="evidenceTypeLabel(photo.evidenceType)" /><small>{{ evidenceTypeLabel(photo.evidenceType) }} · {{ formatBytes(photo.sizeBytes) }}</small></span></div>
        <el-button type="primary" :loading="uploading" @click="uploadPhotos('DELIVERY_PHOTO')">上传照片</el-button>
        <el-button v-if="['DELIVERING', 'DELIVERED'].includes(workspace.task.workflowStatus)" type="success" @click="submitDelivery">提交送达凭证</el-button>
      </div>
      <div v-if="isPublisher" class="evidence-panel"><h3>发布者请求照片</h3><input v-if="canUploadRequest" type="file" accept="image/jpeg,image/png" multiple @change="selectPhotos($event, 'REQUEST_IMAGE')" /><div class="photo-previews"><span v-for="photo in workspace.evidence?.filter(photo => photo.evidenceType === 'REQUEST_IMAGE')" :key="photo.id" class="evidence-photo"><img :src="evidenceUrls.get(photo.id)" :alt="evidenceTypeLabel(photo.evidenceType)" /><small>{{ evidenceTypeLabel(photo.evidenceType) }} · {{ formatBytes(photo.sizeBytes) }}</small></span></div><el-button v-if="canUploadRequest" type="primary" :loading="uploading" @click="uploadPhotos('REQUEST_IMAGE')">上传请求照片</el-button></div>
      <div v-if="isPublisher && workspace.task.workflowStatus === 'PENDING_CONFIRMATION'" class="workspace-actions"><el-button type="success" @click="confirm">确认送达</el-button></div>
      <div v-if="workspace.task.verificationMode === 'COMPLETION_CODE' && isRunner" class="completion-code-panel"><el-input v-model="completionCode" maxlength="32" placeholder="输入发布者提供的完成码" show-password /><el-button type="success" @click="completeWithCode">确认完成</el-button></div>
      <el-alert title="交接小贴士" type="info" show-icon :closable="false"><p>取件码和照片只在履约台展示，公开任务不会暴露这些信息。</p></el-alert>
      <el-timeline><el-timeline-item v-for="event in workspace.events" :key="event.id" :timestamp="event.createdAt">{{ event.note || event.eventType }}</el-timeline-item></el-timeline>
    </el-card>
    <el-card shadow="never" class="workspace-card premium-panel"><template #header>遇到突发状况？</template><el-form label-position="top" :model="issueForm"><el-form-item label="异常情况类别"><el-select v-model="issueForm.issueType" class="wide"><el-option label="驿站/柜机无法取件" value="PICKUP_FAILED" /><el-option label="所填地点有误" value="LOCATION_ERROR" /><el-option label="暂时联系不上委托同学" value="CONTACT_FAILED" /><el-option label="临时有事或时间来不及" value="TIMEOUT" /><el-option label="物品破损或超重异常" value="ITEM_ABNORMAL" /></el-select></el-form-item><el-form-item label="现场情况说明"><el-input v-model="issueForm.description" type="textarea" :rows="3" /></el-form-item><el-button type="danger" plain @click="reportIssue">提交异常报备</el-button></el-form></el-card>
  </section>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { advanceRunnerTask, confirmRunnerTask, completeRunnerTaskWithCode, deleteTaskEvidence, getTaskEvidenceBlob, getTaskWorkspace, reportRunnerTaskIssue, submitTaskDelivery, uploadTaskEvidence, type TaskEvidenceSummary, type TaskEvidenceType, type TaskWorkspaceSummary } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import { discardPhotos, draftPhoto, photoProblem, taskError, type DraftPhoto } from '@/utils/taskHandoff'
const route = useRoute(); const router = useRouter(); const auth = useAuthStore(); const taskId = computed(() => Number(route.params.id)); const loading = ref(false); const uploading = ref(false); const workspace = ref<TaskWorkspaceSummary | null>(null); const errorMessage = ref(''); const completionCode = ref(''); const issueForm = reactive({ issueType: 'LOCATION_ERROR', description: '' }); const requestPhotos = ref<DraftPhoto[]>([]); const deliveryPhotos = ref<DraftPhoto[]>([]); const evidenceUrls = ref(new Map<number, string>()); const stepLabels = ['已接单', '前往取件', '已取件', '派送途中', '送达确认']; const isPublisher = computed(() => workspace.value?.publisherContact.userId === auth.currentUser?.id); const isRunner = computed(() => workspace.value?.runnerContact?.userId === auth.currentUser?.id); const isPhotoMode = computed(() => workspace.value?.task.verificationMode === 'PHOTO_AND_CONFIRMATION'); const canUploadRequest = computed(() => isPublisher.value && ['PUBLISHED','ACCEPTED','HEADING_TO_PICKUP','PICKED_UP','DELIVERING','DELIVERED'].includes(workspace.value?.task.status || workspace.value?.task.workflowStatus || '')); const activeStep = computed(() => Math.max(0, stepLabels.findIndex((_, i) => ['ACCEPTED','HEADING_TO_PICKUP','PICKED_UP','DELIVERING','PENDING_CONFIRMATION','COMPLETED'].includes(workspace.value?.task.workflowStatus || '') && i <= 4)))
function evidenceTypeLabel(type: string) { return type === 'REQUEST_IMAGE' ? '请求照片' : type === 'DELIVERY_PHOTO' ? '送达照片' : '履约照片' }
function formatBytes(size: number) { return `${Math.max(1, Math.round(size / 1024))} KiB` }
async function load() { loading.value = true; errorMessage.value = ''; try { workspace.value = await getTaskWorkspace(taskId.value); if (workspace.value.evidence) { for (const evidence of workspace.value.evidence) { try { const blob = await getTaskEvidenceBlob(taskId.value, evidence.id); evidenceUrls.value.set(evidence.id, URL.createObjectURL(blob)) } catch { /* private evidence may become unavailable */ } } } } catch (error) { errorMessage.value = taskError(error, '无法加载履约台') } finally { loading.value = false } }
function canAdvance(next: string) { const current = workspace.value?.task.workflowStatus; return isRunner.value && ({ HEADING_TO_PICKUP: 'ACCEPTED', PICKED_UP: 'HEADING_TO_PICKUP', DELIVERING: 'PICKED_UP' } as Record<string,string>)[next] === current }
async function advance(next: string) { try { await advanceRunnerTask(taskId.value, auth.currentUser!.id, next, { note: '履约进度更新' }); await load() } catch (error) { ElMessage.error(taskError(error, '状态更新失败')) } }
async function completeWithCode() { if (!completionCode.value) return ElMessage.warning('请输入完成码'); try { await completeRunnerTaskWithCode(taskId.value, auth.currentUser!.id, { completionCode: completionCode.value }); completionCode.value = ''; await load() } catch (error) { ElMessage.error(taskError(error, '完成码核验失败')) } }
async function confirm() { try { await confirmRunnerTask(taskId.value, auth.currentUser!.id, { note: '已确认送达' }); await load() } catch (error) { ElMessage.error(taskError(error, '确认失败')) } }
function selectPhotos(event: Event, kind: TaskEvidenceType) { const files = Array.from((event.target as HTMLInputElement).files ?? []); const target = kind === 'REQUEST_IMAGE' ? requestPhotos : deliveryPhotos; const problem = photoProblem(files, target.value.length); if (problem) return ElMessage.error(problem); target.value.push(...files.map(draftPhoto)) }
async function uploadPhotos(kind: TaskEvidenceType) { const target = kind === 'REQUEST_IMAGE' ? requestPhotos : deliveryPhotos; uploading.value = true; try { for (const photo of target.value.filter(p => !p.uploaded)) { await uploadTaskEvidence(taskId.value, kind, photo.file, photo.clientRequestId); photo.uploaded = true } await load() } catch (error) { ElMessage.error(taskError(error, '上传失败，可重试未完成照片')) } finally { uploading.value = false } }
async function submitDelivery() { try { await submitTaskDelivery(taskId.value); await load() } catch (error) { ElMessage.error(taskError(error, '提交送达凭证失败')) } }
async function reportIssue() { try { await reportRunnerTaskIssue(taskId.value, auth.currentUser!.id, issueForm); await load() } catch (error) { ElMessage.error(taskError(error, '异常报备失败')) } }
onMounted(load); onBeforeUnmount(() => { discardPhotos(requestPhotos.value); discardPhotos(deliveryPhotos.value); evidenceUrls.value.forEach(url => URL.revokeObjectURL(url)) })
</script>
