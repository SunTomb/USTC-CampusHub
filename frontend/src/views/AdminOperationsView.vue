<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Operations</p>
        <h2>运营后台</h2>
        <p>聚合跑腿任务、异常、身份保证金、举报和违规治理入口。</p>
      </div>
      <div class="ops-heading-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          :clearable="true"
        />
        <el-button type="primary" :loading="analyticsLoading" @click="loadAnalytics">刷新分析</el-button>
        <el-button :loading="loading" @click="load">刷新监控</el-button>
      </div>
    </div>

    <div class="ops-metrics" v-if="dashboard">
      <div><span>已发布任务</span><strong>{{ dashboard.publishedTasks }}</strong></div>
      <div><span>履约中</span><strong>{{ dashboard.acceptedTasks }}</strong></div>
      <div><span>已完成</span><strong>{{ dashboard.completedTasks }}</strong></div>
      <div><span>开放异常</span><strong>{{ dashboard.openIssues }}</strong></div>
      <div><span>待审身份</span><strong>{{ dashboard.pendingRoleApplications }}</strong></div>
    </div>

    <section class="ops-analytics-panel" v-loading="analyticsLoading">
      <div class="panel-topline">
        <span>运营分析</span>
        <span v-if="overview">{{ overview.startDate }} 至 {{ overview.endDate }}</span>
      </div>
      <div class="analytics-grid" v-if="overview">
        <div v-for="card in overview.cards" :key="card.key">
          <span>{{ card.label }}</span>
          <strong>{{ formatMetricValue(card) }}</strong>
        </div>
      </div>
    </section>

    <el-tabs class="tabs-surface" v-loading="loading">
      <el-tab-pane label="业务漏斗">
        <div class="funnel-grid" v-if="funnels">
          <div class="funnel-card" v-for="funnel in funnels.funnels" :key="funnel.businessKey">
            <h3>{{ funnel.businessName }}</h3>
            <div class="funnel-step" v-for="step in funnel.steps" :key="step.key">
              <span>{{ step.label }}</span>
              <strong>{{ formatMetricValue(step) }}</strong>
            </div>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="校区分析">
        <div class="zone-grid" v-if="zones">
          <div class="zone-card" v-for="group in zoneGroups" :key="group.key">
            <h3>{{ group.title }}</h3>
            <div v-if="group.items.length">
              <div class="zone-row" v-for="item in group.items" :key="item.key">
                <div class="zone-row-main">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.count }}</strong>
                </div>
                <el-progress :percentage="zonePercentage(item.count, group.items)" :stroke-width="8" />
              </div>
            </div>
            <p class="hint" v-else>暂无数据</p>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="费用与导出">
        <div class="fee-grid" v-if="fees">
          <div>
            <span>服务费记录</span>
            <strong>{{ fees.serviceFeeCount }}</strong>
          </div>
          <div>
            <span>已付服务费</span>
            <strong>¥{{ fees.paidServiceFeeAmount }}</strong>
          </div>
          <div>
            <span>待付服务费</span>
            <strong>¥{{ fees.pendingServiceFeeAmount }}</strong>
          </div>
          <div>
            <span>身份申请</span>
            <strong>{{ fees.roleApplicationCount }}</strong>
          </div>
          <div>
            <span>身份保证金</span>
            <strong>¥{{ fees.roleDepositAmount }}</strong>
          </div>
        </div>
        <div class="fee-grid" v-if="fees">
          <div v-for="card in fees.serviceFeesByTargetType" :key="`fee-${card.key}`">
            <span>服务费：{{ card.label }}</span>
            <strong>{{ formatMetricValue(card) }}</strong>
          </div>
          <div v-for="card in fees.roleDepositsByType" :key="`deposit-${card.key}`">
            <span>保证金：{{ card.label }}</span>
            <strong>{{ formatMetricValue(card) }}</strong>
          </div>
        </div>
        <div class="export-grid">
          <el-button @click="exportCsv('tasks')">导出跑腿任务 CSV</el-button>
          <el-button @click="exportCsv('goods')">导出二手商品 CSV</el-button>
          <el-button @click="exportCsv('shop-orders')">导出店铺预约 CSV</el-button>
          <el-button @click="exportCsv('project-ads')">导出项目广告 CSV</el-button>
          <el-button @click="exportCsv('governance')">导出治理记录 CSV</el-button>
          <el-button @click="exportCsv('fees')">导出费用记录 CSV</el-button>
        </div>
        <p class="hint">CSV 导出不会主动包含密钥、令牌、密码、完整联系方式等敏感字段；治理说明、管理员备注等自由文本仍需谨慎填写，避免录入敏感信息。</p>
      </el-tab-pane>
      <el-tab-pane label="任务监控">
        <el-table :data="tasks" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="任务" />
          <el-table-column prop="acceptanceMode" label="模式" width="100" />
          <el-table-column prop="workflowStatus" label="状态" width="150" />
          <el-table-column prop="rewardAmount" label="报酬" width="100" />
          <el-table-column prop="publisherNickname" label="发布者" width="140" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="异常任务">
        <el-table :data="issues" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="taskTitle" label="任务" />
          <el-table-column prop="issueType" label="类型" width="160" />
          <el-table-column prop="description" label="说明" />
          <el-table-column prop="reporterNickname" label="上报人" width="140" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="身份保证金">
        <el-table :data="roles" stripe>
          <el-table-column prop="userNickname" label="用户" />
          <el-table-column prop="roleType" label="身份" width="160" />
          <el-table-column prop="depositAmount" label="保证金" width="120" />
          <el-table-column prop="depositStatus" label="支付" width="120" />
          <el-table-column prop="reviewStatus" label="审核" width="160" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="店铺预约">
        <el-table :data="shopOrders" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="shopName" label="店铺" min-width="140" />
          <el-table-column prop="serviceItemTitle" label="服务" min-width="160" />
          <el-table-column prop="customerNickname" label="顾客" width="120" />
          <el-table-column prop="providerNickname" label="商家" width="120" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column prop="appointmentTime" label="预约时间" min-width="170" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="项目广告">
        <div class="ops-toolbar">
          <el-select v-model="projectStatus" clearable placeholder="状态" @change="loadProjectAds">
            <el-option label="待审核" value="PENDING_REVIEW" />
            <el-option label="已公开" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已下架" value="CLOSED" />
            <el-option label="违规下架" value="BLOCKED" />
          </el-select>
          <el-button @click="loadProjectAds">刷新项目广告</el-button>
        </div>
        <el-table :data="projectAds" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="adType" label="类型" width="130" />
          <el-table-column prop="publisherNickname" label="发布者" width="130" />
          <el-table-column prop="status" label="状态" width="130" />
          <el-table-column prop="featured" label="精选" width="90">
            <template #default="{ row }">{{ row.featured ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="360">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="approve(row)">通过</el-button>
              <el-button size="small" type="warning" @click="feature(row)">精选</el-button>
              <el-button size="small" @click="unfeature(row)">取消精选</el-button>
              <el-button size="small" type="danger" @click="reject(row)">拒绝</el-button>
              <el-button size="small" type="danger" @click="block(row)">下架</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="举报与违规">
        <p class="hint">举报处理和违规记录继续复用审核治理页面，后续会统一到运营后台。</p>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  approveProjectAd,
  blockProjectAd,
  buildOpsExportUrl,
  featureProjectAd,
  getOpsAnalyticsFees,
  getOpsAnalyticsFunnels,
  getOpsAnalyticsOverview,
  getOpsAnalyticsZones,
  getOpsDashboard,
  listOpsProjectAds,
  listOpsRoleApplications,
  listOpsShopOrders,
  listOpsTaskIssues,
  listOpsTasks,
  rejectProjectAd,
  unfeatureProjectAd,
  type FeeAnalyticsSummary,
  type MetricCardSummary,
  type OperationsAnalyticsOverview,
  type OperationsDashboardSummary,
  type OperationsFunnelSummary,
  type OperationsZoneSummary,
  type OpsAnalyticsParams,
  type ProjectAdSummary,
  type RewardTaskSummary,
  type RoleApplicationSummary,
  type ServiceOrderSummary,
  type TaskIssueSummary,
  type ZoneMetricSummary,
} from '@/api/campushub'

type OpsExportKind = 'tasks' | 'goods' | 'shop-orders' | 'project-ads' | 'governance' | 'fees'

const loading = ref(false)
const dashboard = ref<OperationsDashboardSummary | null>(null)
const tasks = ref<RewardTaskSummary[]>([])
const issues = ref<TaskIssueSummary[]>([])
const roles = ref<RoleApplicationSummary[]>([])
const shopOrders = ref<ServiceOrderSummary[]>([])
const projectAds = ref<ProjectAdSummary[]>([])
const projectStatus = ref('PENDING_REVIEW')
const analyticsLoading = ref(false)
const dateRange = ref<[string, string] | null>(null)
const overview = ref<OperationsAnalyticsOverview | null>(null)
const funnels = ref<OperationsFunnelSummary | null>(null)
const zones = ref<OperationsZoneSummary | null>(null)
const fees = ref<FeeAnalyticsSummary | null>(null)

const zoneGroups = computed(() => {
  if (!zones.value) {
    return []
  }

  return [
    { key: 'taskOriginZones', title: '任务起点校区', items: zones.value.taskOriginZones },
    { key: 'taskDestinationZones', title: '任务终点校区', items: zones.value.taskDestinationZones },
    { key: 'taskRoutes', title: '任务路线', items: zones.value.taskRoutes },
    { key: 'goodsZones', title: '二手交易校区', items: zones.value.goodsZones },
    { key: 'shopZones', title: '学生店铺校区', items: zones.value.shopZones },
    { key: 'projectAdZones', title: '项目广告校区', items: zones.value.projectAdZones },
  ]
})

function analyticsParams(): OpsAnalyticsParams {
  if (!dateRange.value) {
    return {}
  }
  const [startDate, endDate] = dateRange.value
  return { startDate, endDate }
}

async function load() {
  loading.value = true
  try {
    const [dashboardData, taskData, issueData, roleData, shopOrderData, projectAdData] = await Promise.all([
      getOpsDashboard(),
      listOpsTasks(),
      listOpsTaskIssues(),
      listOpsRoleApplications(),
      listOpsShopOrders(),
      listOpsProjectAds(projectStatus.value),
    ])
    dashboard.value = dashboardData
    tasks.value = taskData
    issues.value = issueData
    roles.value = roleData
    shopOrders.value = shopOrderData
    projectAds.value = projectAdData
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '运营数据加载失败')
  } finally {
    loading.value = false
  }
}

async function loadAnalytics() {
  analyticsLoading.value = true
  try {
    const params = analyticsParams()
    const [overviewData, funnelData, zoneData, feeData] = await Promise.all([
      getOpsAnalyticsOverview(params),
      getOpsAnalyticsFunnels(params),
      getOpsAnalyticsZones(params),
      getOpsAnalyticsFees(params),
    ])
    overview.value = overviewData
    funnels.value = funnelData
    zones.value = zoneData
    fees.value = feeData
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '运营分析加载失败')
  } finally {
    analyticsLoading.value = false
  }
}

function exportCsv(kind: OpsExportKind) {
  window.open(buildOpsExportUrl(kind, analyticsParams()), '_blank')
}

function formatMetricValue(card: MetricCardSummary) {
  if (card.unit === 'CNY') {
    return `¥${card.value}`
  }
  return `${card.value}${card.unit && card.unit !== 'count' ? card.unit : ''}`
}

function topZoneCount(items: ZoneMetricSummary[]) {
  return Math.max(0, ...items.map((item) => item.count))
}

function zonePercentage(count: number, items: ZoneMetricSummary[]) {
  const topCount = topZoneCount(items)
  if (topCount === 0) {
    return 0
  }
  return Math.round((count / topCount) * 100)
}

async function loadProjectAds() {
  try {
    projectAds.value = await listOpsProjectAds(projectStatus.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '项目广告加载失败')
  }
}

async function approve(row: ProjectAdSummary) {
  await approveProjectAd(row.id, 4, { note: '内容完整，允许展示' })
  ElMessage.success('已通过')
  await loadProjectAds()
}

async function reject(row: ProjectAdSummary) {
  await rejectProjectAd(row.id, 4, { note: '请补充项目说明或联系方式' })
  ElMessage.success('已拒绝')
  await loadProjectAds()
}

async function feature(row: ProjectAdSummary) {
  await featureProjectAd(row.id, 4, { featuredPriority: 10 })
  ElMessage.success('已设为精选')
  await loadProjectAds()
}

async function unfeature(row: ProjectAdSummary) {
  await unfeatureProjectAd(row.id, 4)
  ElMessage.success('已取消精选')
  await loadProjectAds()
}

async function block(row: ProjectAdSummary) {
  await blockProjectAd(row.id, 4, { note: '运营后台下架' })
  ElMessage.success('已下架')
  await loadProjectAds()
}

onMounted(() => {
  void load()
  void loadAnalytics()
})
</script>
