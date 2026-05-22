<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Runner Hall</p>
        <h2>校园跑腿任务大厅</h2>
        <p>按校区、接单模式和履约方式快速筛选，发布者可选择抢单或申请模式。</p>
      </div>
      <div class="heading-actions">
        <el-button @click="loadTasks" :loading="loading">刷新</el-button>
        <el-button type="primary" @click="openPublishDialog">发布任务</el-button>
      </div>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters">
        <el-form-item label="起点">
          <el-select v-model="filters.originZone" clearable placeholder="全部校区" style="width: 150px">
            <el-option v-for="zone in zones" :key="zone.value" :label="zone.label" :value="zone.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="终点">
          <el-select v-model="filters.destinationZone" clearable placeholder="全部校区" style="width: 150px">
            <el-option v-for="zone in zones" :key="zone.value" :label="zone.label" :value="zone.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="模式">
          <el-select v-model="filters.acceptanceMode" clearable placeholder="全部" style="width: 130px">
            <el-option label="抢单" value="GRAB" />
            <el-option label="申请" value="APPLICATION" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="task-card-grid" v-loading="loading">
      <el-card v-for="task in filteredTasks" :key="task.id" shadow="never" class="runner-card">
        <div class="panel-topline">
          <el-tag>{{ modeLabel(task.acceptanceMode) }}</el-tag>
          <span>{{ formatDate(task.deadline) }} 前</span>
        </div>
        <h3>{{ task.title }}</h3>
        <p>{{ task.description }}</p>
        <div class="route-line">
          <strong>{{ zoneLabel(task.originZone) }}</strong>
          <span>→</span>
          <strong>{{ zoneLabel(task.destinationZone) }}</strong>
        </div>
        <p class="row-detail">{{ task.originDetail || '起点待沟通' }} → {{ task.destinationDetail || '终点待沟通' }}</p>
        <div class="task-meta">
          <span>报酬 <strong>¥{{ task.rewardAmount }}</strong></span>
          <span>{{ task.verificationMode === 'PHOTO_AND_CONFIRMATION' ? '图片凭证' : '完成码' }}</span>
          <span>发布者 {{ task.publisherNickname }}</span>
        </div>
        <div class="card-actions">
          <el-button v-if="task.acceptanceMode === 'GRAB'" type="primary" @click="grab(task.id)">立即抢单</el-button>
          <el-button v-else type="primary" @click="openApply(task)">申请接单</el-button>
          <el-button plain @click="$router.push(`/tasks/${task.id}/workspace`) ">工作台</el-button>
        </div>
      </el-card>
      <EmptyState
        v-if="!loading && filteredTasks.length === 0"
        eyebrow="Task Hall"
        title="当前没有符合条件的跑腿任务"
        description="可以调整校区、类型或接单模式筛选，也可以发布一个新的校园跑腿需求。"
        action-text="发布跑腿任务"
        @action="openPublishDialog"
      />
    </div>

    <el-drawer v-model="publisherOpen" title="发布跑腿任务" size="520px">
      <el-form ref="publishFormRef" label-position="top" :model="publishForm" :rules="publishRules" class="form-section-stack">
        <FormSection title="任务信息" description="用一句话说明要帮忙做什么，类型会影响接单者判断。">
          <el-form-item label="标题" prop="title"><el-input v-model="publishForm.title" /></el-form-item>
          <el-form-item label="描述" prop="description"><el-input v-model="publishForm.description" type="textarea" :rows="3" /></el-form-item>
        </FormSection>
        <FormSection title="路线与时间" description="先使用校区和文字地点，后续根据真实数据再沉淀常用点位。">
          <el-form-item label="起点/终点校区" required>
            <div class="two-column">
              <el-select v-model="publishForm.originZone"><el-option v-for="zone in zones" :key="zone.value" :label="zone.label" :value="zone.value" /></el-select>
              <el-select v-model="publishForm.destinationZone"><el-option v-for="zone in zones" :key="zone.value" :label="zone.label" :value="zone.value" /></el-select>
            </div>
          </el-form-item>
          <el-form-item label="地点详情" required>
            <div class="two-column">
              <el-input v-model="publishForm.originDetail" placeholder="取件点" />
              <el-input v-model="publishForm.destinationDetail" placeholder="送达点" />
            </div>
          </el-form-item>
          <el-form-item label="截止时间" prop="deadline"><el-date-picker v-model="deadlineDate" type="datetime" class="wide" /></el-form-item>
        </FormSection>
        <FormSection title="接单与确认" description="抢单适合标准任务；申请模式适合需要筛选人的任务。">
          <el-form-item label="报酬" prop="rewardAmount"><el-input-number v-model="publishForm.rewardAmount" :min="0" :precision="2" class="wide" /></el-form-item>
          <el-form-item label="接单与确认方式" required>
            <div class="two-column">
              <el-select v-model="publishForm.acceptanceMode">
                <el-option label="抢单" value="GRAB" />
                <el-option label="申请" value="APPLICATION" />
              </el-select>
              <el-select v-model="publishForm.verificationMode">
                <el-option label="完成码" value="COMPLETION_CODE" />
                <el-option label="图片凭证+确认" value="PHOTO_AND_CONFIRMATION" />
              </el-select>
            </div>
          </el-form-item>
          <p class="hint">平台只收服务费和身份保证金，不托管跑腿报酬本金，也不做逐单保证金冻结。</p>
        </FormSection>
        <el-button type="primary" class="wide" :loading="publishing" @click="publish">提交发布</el-button>
      </el-form>
    </el-drawer>

    <el-dialog v-model="applyOpen" title="申请接单" width="420px">
      <el-input v-model="applyMessage" type="textarea" :rows="4" placeholder="说明你的当前位置、预计到达时间或经验" />
      <template #footer>
        <el-button @click="applyOpen = false">取消</el-button>
        <el-button type="primary" @click="apply">提交申请</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  applyRunnerTask,
  grabRunnerTask,
  listTasks,
  publishRunnerTask,
  type CampusZone,
  type CreateRunnerTaskPayload,
  type RewardTaskSummary,
  type TaskAcceptanceMode,
} from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import EmptyState from '@/components/common/EmptyState.vue'
import FormSection from '@/components/common/FormSection.vue'

const router = useRouter()
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
const publishForm = reactive<CreateRunnerTaskPayload>({
  title: '',
  description: '',
  rewardAmount: 5,
  acceptanceMode: 'GRAB',
  originZone: 'CENTRAL',
  destinationZone: 'CENTRAL',
  originDetail: '',
  destinationDetail: '',
  deadline: new Date().toISOString(),
  verificationMode: 'COMPLETION_CODE',
})

const publishRules: FormRules<CreateRunnerTaskPayload> = {
  title: [
    { required: true, message: '请填写任务标题', trigger: 'blur' },
    { min: 2, max: 80, message: '标题长度应为 2-80 个字符', trigger: 'blur' },
  ],
  description: [
    { required: true, message: '请填写任务描述', trigger: 'blur' },
    { min: 5, max: 1000, message: '描述长度应为 5-1000 个字符', trigger: 'blur' },
  ],
  rewardAmount: [
    { required: true, message: '请填写任务报酬', trigger: 'change' },
    { type: 'number', min: 0.01, message: '报酬需大于 0 元', trigger: 'change' },
  ],
  deadline: [{ required: true, message: '请选择截止时间', trigger: 'change' }],
}

const filteredTasks = computed(() => tasks.value.filter((task) => {
  if (filters.originZone && task.originZone !== filters.originZone) return false
  if (filters.destinationZone && task.destinationZone !== filters.destinationZone) return false
  if (filters.acceptanceMode && task.acceptanceMode !== filters.acceptanceMode) return false
  return true
}))

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
  publisherOpen.value = true
}

async function publish() {
  publishForm.deadline = deadlineDate.value?.toISOString() ?? ''
  await publishFormRef.value?.validate()
  if (!publishForm.originDetail?.trim() || !publishForm.destinationDetail?.trim()) {
    ElMessage.error('请填写起点和终点地点详情')
    return
  }
  publishing.value = true
  try {
    publishForm.originDetail = publishForm.originDetail.trim()
    publishForm.destinationDetail = publishForm.destinationDetail.trim()
    await publishRunnerTask(publishForm, auth.currentUser?.id ?? 1)
    ElMessage.success('任务已发布')
    publisherOpen.value = false
    await loadTasks()
  } finally {
    publishing.value = false
  }
}

async function grab(taskId: number) {
  if (!requireLogin()) return
  const user = auth.currentUser
  if (!user) return
  await grabRunnerTask(taskId, user.id)
  ElMessage.success('抢单成功')
  await loadTasks()
}

function openApply(task: RewardTaskSummary) {
  if (!requireLogin()) return
  applyTaskId.value = task.id
  applyMessage.value = ''
  applyOpen.value = true
}

async function apply() {
  if (!applyTaskId.value || !auth.currentUser) return
  await applyRunnerTask(applyTaskId.value, auth.currentUser.id, { message: applyMessage.value })
  ElMessage.success('申请已提交')
  applyOpen.value = false
}

function requireLogin() {
  if (auth.currentUser) return true
  ElMessage.warning('请先登录后再发布、抢单或申请跑腿任务')
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
  return value === 'GRAB' ? '抢单' : '申请'
}

onMounted(loadTasks)
</script>
