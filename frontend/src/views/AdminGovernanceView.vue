<template>
  <section class="page-stack governance-page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Governance</p>
        <h2>治理工作台</h2>
        <p>统一处理举报、违规、信用分、用户限制和管理员操作审计。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <div v-if="dashboard" class="governance-metrics">
      <div><span>开放举报</span><strong>{{ dashboard.openReports }}</strong></div>
      <div><span>处理中</span><strong>{{ dashboard.inReviewReports }}</strong></div>
      <div><span>已处理</span><strong>{{ dashboard.handledReports }}</strong></div>
      <div><span>高严重违规</span><strong>{{ dashboard.highSeverityViolations }}</strong></div>
      <div><span>有效限制</span><strong>{{ dashboard.activeRestrictions }}</strong></div>
    </div>

    <el-card shadow="never" class="surface-card">
      <div class="ops-toolbar governance-filters">
        <el-select v-model="statusFilter" clearable placeholder="举报状态" @change="loadReports">
          <el-option label="开放" value="OPEN" />
          <el-option label="处理中" value="IN_REVIEW" />
          <el-option label="已解决" value="RESOLVED" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="已升级" value="ESCALATED" />
        </el-select>
        <el-select v-model="targetFilter" clearable placeholder="目标类型" @change="loadReports">
          <el-option label="商品" value="GOODS" />
          <el-option label="跑腿任务" value="TASK" />
          <el-option label="店铺" value="SHOP" />
          <el-option label="服务预约" value="SERVICE_ORDER" />
          <el-option label="项目广告" value="PROJECT_AD" />
          <el-option label="评论" value="COMMENT" />
          <el-option label="用户" value="USER" />
        </el-select>
        <el-button @click="loadReports">筛选</el-button>
      </div>

      <div class="mobile-table-wrapper">
        <el-table :data="reports" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="targetType" label="对象" width="130" />
        <el-table-column prop="targetId" label="对象ID" width="100" />
        <el-table-column prop="reason" label="原因" min-width="140" />
        <el-table-column prop="reporterNickname" label="举报人" width="120" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="reviewNote" label="处理说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <div class="governance-actions">
              <el-button size="small" @click="handleStart(row)">受理</el-button>
              <el-button size="small" type="success" @click="handleResolve(row)">解决</el-button>
              <el-button size="small" type="warning" @click="handleEscalate(row)">升级</el-button>
              <el-button size="small" type="danger" @click="handleReject(row)">驳回</el-button>
              <el-button size="small" type="primary" @click="openViolation(row)">记违规</el-button>
            </div>
          </template>
        </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-card shadow="never" class="surface-card">
      <template #header>管理员操作审计</template>
      <div class="mobile-table-wrapper">
        <el-table :data="auditLogs" stripe>
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="adminNickname" label="管理员" width="120" />
        <el-table-column prop="actionType" label="动作" width="180" />
        <el-table-column prop="targetType" label="对象" width="130" />
        <el-table-column prop="targetId" label="对象ID" width="100" />
          <el-table-column prop="note" label="备注" show-overflow-tooltip />
        </el-table>
      </div>
    </el-card>

    <el-dialog v-model="violationDialog" title="创建违规记录" width="640px">
      <el-form label-width="120px">
        <el-form-item label="处理用户ID">
          <el-input-number v-model="violationForm.userId" :min="1" />
        </el-form-item>
        <el-form-item label="违规类型">
          <el-input v-model="violationForm.violationType" />
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="violationForm.severity">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
            <el-option label="严重" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="处罚动作">
          <el-select v-model="violationForm.penaltyType">
            <el-option label="仅信用" value="CREDIT_ONLY" />
            <el-option label="警告" value="WARNING" />
            <el-option label="发布冻结" value="POSTING_FREEZE" />
            <el-option label="服务冻结" value="SERVICE_FREEZE" />
            <el-option label="账号禁用" value="ACCOUNT_DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="信用变化">
          <el-input-number v-model="violationForm.creditDelta" :min="-100" :max="100" />
        </el-form-item>
        <el-form-item label="限制类型">
          <el-select v-model="violationForm.restrictionType" clearable>
            <el-option label="发布冻结" value="POSTING_FREEZE" />
            <el-option label="服务冻结" value="SERVICE_FREEZE" />
            <el-option label="账号禁用" value="ACCOUNT_DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="限制天数">
          <el-input-number v-model="violationForm.restrictionDays" :min="1" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="violationForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <p class="hint">创建违规会同步影响信用记录、限制状态和站内通知，请确认处理对象与说明准确。</p>
      <template #footer>
        <el-button @click="violationDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitViolation">提交</el-button>
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

const adminId = 1
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
  depositImpactNote: '暂不自动扣除保证金',
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
  const result = await ElMessageBox.prompt('请输入处理说明', title, { inputType: 'textarea' })
  return result.value
}

async function handleStart(report: ReportRecordSummary) {
  const note = await askNote('受理举报')
  await startReportReview(report.id, adminId, { note })
  ElMessage.success('已受理')
  await load()
}

async function handleResolve(report: ReportRecordSummary) {
  const note = await askNote('解决举报')
  await resolveReport(report.id, adminId, { resolutionType: 'CONTENT_REMOVED', note })
  ElMessage.success('已解决')
  await load()
}

async function handleReject(report: ReportRecordSummary) {
  const note = await askNote('驳回举报')
  await rejectReport(report.id, adminId, { note })
  ElMessage.success('已驳回')
  await load()
}

async function handleEscalate(report: ReportRecordSummary) {
  const note = await askNote('升级举报')
  await escalateReport(report.id, adminId, { note })
  ElMessage.success('已升级')
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
    await createViolation(adminId, { ...violationForm })
    ElMessage.success('违规记录已创建')
    violationDialog.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
