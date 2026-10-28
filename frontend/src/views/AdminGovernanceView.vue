<template>
  <section class="page-stack governance-page">
    <div class="page-heading admin-page-heading">
      <div>
        <p class="eyebrow">站务中枢 · 社区风纪与信用治理</p>
        <h2>社区治理工作台</h2>
        <p>集中受理同学提交的问题反馈、核实失信行为、调整信用誉分并记录每一次站务操作。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新队列</el-button>
    </div>

    <div v-if="dashboard" class="governance-metrics">
      <div><span>待受理反馈</span><strong class="mono-num">{{ dashboard.openReports }}</strong></div>
      <div><span>核实处理中</span><strong class="mono-num">{{ dashboard.inReviewReports }}</strong></div>
      <div><span>已办结反馈</span><strong class="mono-num">{{ dashboard.handledReports }}</strong></div>
      <div><span>严重失信记录</span><strong class="mono-num">{{ dashboard.highSeverityViolations }}</strong></div>
      <div><span>生效中限制</span><strong class="mono-num">{{ dashboard.activeRestrictions }}</strong></div>
    </div>

    <el-card shadow="never" class="surface-card premium-panel">
      <div class="ops-toolbar governance-filters">
        <el-select v-model="statusFilter" clearable placeholder="按处理进度筛选" @change="loadReports">
          <el-option label="待受理" value="OPEN" />
          <el-option label="核查处理中" value="IN_REVIEW" />
          <el-option label="已妥善解决" value="RESOLVED" />
          <el-option label="不予受理/驳回" value="REJECTED" />
          <el-option label="已升级处理" value="ESCALATED" />
        </el-select>
        <el-select v-model="targetFilter" clearable placeholder="按所属板块筛选" @change="loadReports">
          <el-option label="二手闲置" value="GOODS" />
          <el-option label="跑腿悬赏" value="TASK" />
          <el-option label="学生小店" value="SHOP" />
          <el-option label="服务预约" value="SERVICE_ORDER" />
          <el-option label="项目展示" value="PROJECT_AD" />
          <el-option label="互动留言" value="COMMENT" />
          <el-option label="校园用户" value="USER" />
        </el-select>
        <el-button @click="loadReports">执行筛选</el-button>
      </div>

      <div class="mobile-table-wrapper">
        <el-table :data="reports" stripe v-loading="loading">
          <el-table-column prop="id" label="编号" width="80">
            <template #default="{ row }"><span class="mono-num">#{{ row.id }}</span></template>
          </el-table-column>
          <el-table-column prop="targetType" label="所属板块" width="130">
            <template #default="{ row }">{{ formatTargetType(row.targetType) }}</template>
          </el-table-column>
          <el-table-column prop="targetId" label="对象编号" width="100">
            <template #default="{ row }"><span class="mono-num">#{{ row.targetId }}</span></template>
          </el-table-column>
          <el-table-column prop="reason" label="反馈原因" min-width="140" />
          <el-table-column prop="reporterNickname" label="反馈同学" width="120" />
          <el-table-column prop="status" label="状态" width="130">
            <template #default="{ row }">
              <el-tag :type="statusTone(row.status)" size="small">{{ formatStatus(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reviewNote" label="处理备注" min-width="180" show-overflow-tooltip />
          <el-table-column label="站务处置" width="360" fixed="right">
            <template #default="{ row }">
              <div class="governance-actions">
                <el-button size="small" @click="handleStart(row)">受理</el-button>
                <el-button size="small" type="success" @click="handleResolve(row)">办结</el-button>
                <el-button size="small" type="warning" @click="handleEscalate(row)">升级</el-button>
                <el-button size="small" type="danger" plain @click="handleReject(row)">驳回</el-button>
                <el-button size="small" type="primary" @click="openViolation(row)">登记违规</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-card shadow="never" class="surface-card premium-panel">
      <template #header>站务处置留痕记录</template>
      <div class="mobile-table-wrapper">
        <el-table :data="auditLogs" stripe>
          <el-table-column prop="createdAt" label="处理时间" width="180" />
          <el-table-column prop="adminNickname" label="值班站务" width="120" />
          <el-table-column prop="actionType" label="操作事项" width="180" />
          <el-table-column prop="targetType" label="所属板块" width="130">
            <template #default="{ row }">{{ formatTargetType(row.targetType) }}</template>
          </el-table-column>
          <el-table-column prop="targetId" label="编号" width="100">
            <template #default="{ row }"><span class="mono-num">#{{ row.targetId }}</span></template>
          </el-table-column>
          <el-table-column prop="note" label="处置说明" show-overflow-tooltip />
        </el-table>
      </div>
    </el-card>

    <el-dialog v-model="violationDialog" title="登记失信与违规处置" width="640px">
      <el-form label-width="120px">
        <el-form-item label="责任用户 ID">
          <el-input-number v-model="violationForm.userId" :min="1" />
        </el-form-item>
        <el-form-item label="违规类别">
          <el-input v-model="violationForm.violationType" placeholder="如：爽约未送达 / 虚假商品信息" />
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="violationForm.severity">
            <el-option label="轻微" value="LOW" />
            <el-option label="一般" value="MEDIUM" />
            <el-option label="较重" value="HIGH" />
            <el-option label="严重" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="处置方式">
          <el-select v-model="violationForm.penaltyType">
            <el-option label="仅调整信用分" value="CREDIT_ONLY" />
            <el-option label="站内提醒警告" value="WARNING" />
            <el-option label="暂停发布权限" value="POSTING_FREEZE" />
            <el-option label="暂停接单与服务" value="SERVICE_FREEZE" />
            <el-option label="限制账号登录" value="ACCOUNT_DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="信用分调整">
          <el-input-number v-model="violationForm.creditDelta" :min="-100" :max="100" />
        </el-form-item>
        <el-form-item label="附加权限限制">
          <el-select v-model="violationForm.restrictionType" clearable placeholder="无需附加限制可留空">
            <el-option label="暂停发布权限" value="POSTING_FREEZE" />
            <el-option label="暂停接单与服务" value="SERVICE_FREEZE" />
            <el-option label="限制账号登录" value="ACCOUNT_DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="限制持续天数">
          <el-input-number v-model="violationForm.restrictionDays" :min="1" />
        </el-form-item>
        <el-form-item label="处置原因说明">
          <el-input v-model="violationForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <p class="hint">确认提交后将同步更新该用户的校园信用分、权限状态并发送站内通知。</p>
      <template #footer>
        <el-button @click="violationDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitViolation">确认登记</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createViolation,
  escalateReport,
  getAdminActionLogs,
  getGovernanceDashboard,
  getGovernanceReports,
  rejectReport,
  resolveReport,
  startReportReview,
  type AdminActionLogSummary,
  type CreateViolationPayload,
  type GovernanceDashboardSummary,
  type ReportRecordSummary,
} from '@/api/campushub'
import { formatStatus, formatTargetType, statusTone } from '@/utils/display'

const loading = ref(false)
const submitting = ref(false)
const dashboard = ref<GovernanceDashboardSummary>()
const reports = ref<ReportRecordSummary[]>([])
const auditLogs = ref<AdminActionLogSummary[]>([])
const statusFilter = ref('OPEN')
const targetFilter = ref('')
const violationDialog = ref(false)
const selectedReport = ref<ReportRecordSummary>()
const violationForm = reactive<CreateViolationPayload>({
  userId: 1,
  reportId: null,
  targetType: null,
  targetId: null,
  violationType: 'GENERAL',
  severity: 'LOW',
  penaltyType: 'CREDIT_ONLY',
  description: '',
  creditDelta: -5,
  depositImpactNote: '暂不扣除保证金',
  restrictionType: null,
  restrictionDays: null,
})

async function load() {
  loading.value = true
  try {
    const [dashboardResult, reportsResult, logsResult] = await Promise.all([
      getGovernanceDashboard(),
      getGovernanceReports({ status: statusFilter.value || undefined, targetType: targetFilter.value || undefined }),
      getAdminActionLogs(),
    ])
    dashboard.value = dashboardResult
    reports.value = reportsResult
    auditLogs.value = logsResult
  } finally {
    loading.value = false
  }
}

async function loadReports() {
  reports.value = await getGovernanceReports({ status: statusFilter.value || undefined, targetType: targetFilter.value || undefined })
}

async function askNote(title: string) {
  const result = await ElMessageBox.prompt('请输入站务处理说明', title, { inputType: 'textarea' })
  return result.value
}

async function handleStart(report: ReportRecordSummary) {
  const note = await askNote('受理反馈')
  await startReportReview(report.id, 0, { note })
  ElMessage.success('已开始受理')
  await load()
}

async function handleResolve(report: ReportRecordSummary) {
  const note = await askNote('办结反馈')
  await resolveReport(report.id, 0, { resolutionType: 'CONTENT_REMOVED', note })
  ElMessage.success('已办结')
  await load()
}

async function handleReject(report: ReportRecordSummary) {
  const note = await askNote('驳回反馈')
  await rejectReport(report.id, 0, { note })
  ElMessage.success('已驳回')
  await load()
}

async function handleEscalate(report: ReportRecordSummary) {
  const note = await askNote('升级处理')
  await escalateReport(report.id, 0, { note })
  ElMessage.success('已标记升级处理')
  await load()
}

function openViolation(report: ReportRecordSummary) {
  selectedReport.value = report
  violationForm.reportId = report.id
  violationForm.targetType = report.targetType
  violationForm.targetId = report.targetId
  violationForm.description = report.description || report.reason
  violationDialog.value = true
}

async function submitViolation() {
  submitting.value = true
  try {
    await createViolation(0, { ...violationForm })
    ElMessage.success('违规处置已生效')
    violationDialog.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
