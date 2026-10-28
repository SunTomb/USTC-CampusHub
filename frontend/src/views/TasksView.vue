<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">顺路互助 · 悬赏告示栏</p>
        <h2>校园跑腿大厅</h2>
        <p>跨校区代取快递、急送实验材料或食堂顺路带饭——支持直接抢单或报名筛选。</p>
      </div>
      <div class="heading-actions">
        <el-button @click="loadTasks" :loading="loading">刷新告示</el-button>
        <el-button type="primary" @click="openPublishDialog">张贴悬赏单</el-button>
      </div>
    </div>

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
          <p class="ticket-desc">{{ task.description }}</p>

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

        <div class="ticket- perforation" aria-hidden="true"></div>

        <div class="ticket-stub">
          <div class="ticket-reward">
            <span>悬赏酬金</span>
            <strong class="mono-num">¥{{ task.rewardAmount }}</strong>
            <small>委托人：{{ task.publisherNickname }}</small>
          </div>
          <div class="card-actions">
            <el-button v-if="task.acceptanceMode === 'GRAB'" type="primary" @click="grab(task.id)">立即抢单</el-button>
            <el-button v-else type="primary" @click="openApply(task)">报名接单</el-button>
            <el-button plain @click="$router.push(`/tasks/${task.id}/workspace`)">履约台</el-button>
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

    <el-drawer v-model="publisherOpen" title="张贴跑腿悬赏单" size="520px">
      <el-form ref="publishFormRef" label-position="top" :model="publishForm" :rules="publishRules" class="form-section-stack">
        <FormSection title="委托事项" description="简明扼要地说明需要代取或代办的物品，方便顺路同学快速判断。">
          <el-form-item label="任务标题" prop="title">
            <el-input v-model="publishForm.title" placeholder="例如：西区近邻宝代取中件快递送到中区宿舍" />
          </el-form-item>
          <el-form-item label="详细说明" prop="description">
            <el-input v-model="publishForm.description" type="textarea" :rows="3" placeholder="说明物品重量、注意事项（取件码可在接单后私聊发送）" />
          </el-form-item>
        </FormSection>

        <FormSection title="起止路线与时限" description="选择跨校区或同校区路线，注明大致楼宇或驿站位置。">
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
            <el-date-picker v-model="deadlineDate" type="datetime" class="wide" />
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
          <p class="hint">温馨提示：跑腿酬金由委托人与接单同学在送达后直接结算，请勿在公开描述中填写取件码或隐私密码。</p>
        </FormSection>
        <el-button type="primary" class="wide" :loading="publishing" @click="publish">确认张贴悬赏单</el-button>
      </el-form>
    </el-drawer>

    <el-dialog v-model="applyOpen" title="报名承接跑腿" width="420px">
      <el-input v-model="applyMessage" type="textarea" :rows="4" placeholder="向委托人说明你目前所在校区、预计送达时间或顺路情况" />
      <template #footer>
        <el-button @click="applyOpen = false">取消</el-button>
        <el-button type="primary" @click="apply">发送接单申请</el-button>
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
    if (!auth.currentUser) return
    await publishRunnerTask(publishForm, auth.currentUser.id)
    ElMessage.success('悬赏单已张贴')
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
  ElMessage.success('抢单成功，请前往履约台查看详情')
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
  ElMessage.success('接单申请已发送，请留意站内通知')
  applyOpen.value = false
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

onMounted(loadTasks)
</script>
