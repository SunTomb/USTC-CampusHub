<template>
  <section class="page-stack">
    <div class="page-heading admin-page-heading">
      <div>
        <p class="eyebrow">站务中枢 · 全站运营看板</p>
        <h2>运营数据与报表中心</h2>
        <p>实时总览跑腿、二手、小店与项目广场的活跃指标、校区热力分布及费用统计，支持一键导出脱敏 CSV 报表。</p>
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
        <el-button type="primary" :loading="analyticsLoading" @click="loadAnalytics">更新统计区间</el-button>
        <el-button :loading="loading" @click="load">刷新实时队列</el-button>
      </div>
    </div>

    <div class="ops-metrics" v-if="dashboard">
      <div><span>待接跑腿单</span><strong class="mono-num">{{ dashboard.publishedTasks }}</strong></div>
      <div><span>派送进行中</span><strong class="mono-num">{{ dashboard.acceptedTasks }}</strong></div>
      <div><span>已送达结单</span><strong class="mono-num">{{ dashboard.completedTasks }}</strong></div>
      <div><span>待处理异常</span><strong class="mono-num">{{ dashboard.openIssues }}</strong></div>
      <div><span>待核验身份</span><strong class="mono-num">{{ dashboard.pendingRoleApplications }}</strong></div>
    </div>

    <section class="ops-analytics-panel premium-panel" v-loading="analyticsLoading">
      <div class="panel-topline">
        <strong>核心运营指标概览</strong>
        <span v-if="overview" class="mono-num">{{ overview.startDate }} 至 {{ overview.endDate }}</span>
      </div>
      <div class="analytics-grid" v-if="overview">
        <div v-for="card in overview.cards" :key="card.key">
          <span>{{ card.label }}</span>
          <strong class="mono-num">{{ formatMetricValue(card) }}</strong>
        </div>
      </div>
    </section>

    <el-tabs class="tabs-surface premium-panel" v-loading="loading">
      <el-tab-pane label="业务转化漏斗">
        <div class="funnel-grid" v-if="funnels">
          <div class="funnel-card" v-for="funnel in funnels.funnels" :key="funnel.businessKey">
            <h3>{{ funnel.businessName }}</h3>
            <div class="funnel-step" v-for="step in funnel.steps" :key="step.key">
              <span>{{ step.label }}</span>
              <strong class="mono-num">{{ formatMetricValue(step) }}</strong>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="校区活跃分布">
        <div class="zone-grid" v-if="zones">
          <div class="zone-card" v-for="group in zoneGroups" :key="group.key">
            <h3>{{ group.title }}</h3>
            <div v-if="group.items.length">
              <div class="zone-row" v-for="item in group.items" :key="item.key">
                <div class="zone-row-main">
                  <span>{{ item.label }}</span>
                  <strong class="mono-num">{{ item.count }}</strong>
                </div>
                <el-progress :percentage="zonePercentage(item.count, group.items)" :stroke-width="8" />
              </div>
            </div>
            <p class="hint" v-else>该区间暂无记录</p>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="账务汇总与报表导出">
        <div class="fee-grid" v-if="fees">
          <div>
            <span>服务费笔数</span>
            <strong class="mono-num">{{ fees.serviceFeeCount }}</strong>
          </div>
          <div>
            <span>已结服务费</span>
            <strong class="mono-num">¥{{ fees.paidServiceFeeAmount }}</strong>
          </div>
          <div>
            <span>待结服务费</span>
            <strong class="mono-num">¥{{ fees.pendingServiceFeeAmount }}</strong>
          </div>
          <div>
            <span>身份开通申请</span>
            <strong class="mono-num">{{ fees.roleApplicationCount }}</strong>
          </div>
          <div>
            <span>历史保证金总额</span>
            <strong class="mono-num">¥{{ fees.roleDepositAmount }}</strong>
          </div>
        </div>
        <div class="fee-grid" v-if="fees" style="margin-top: 14px">
          <div v-for="card in fees.serviceFeesByTargetType" :key="`fee-${card.key}`">
            <span>服务费 · {{ card.label }}</span>
            <strong class="mono-num">{{ formatMetricValue(card) }}</strong>
          </div>
          <div v-for="card in fees.roleDepositsByType" :key="`deposit-${card.key}`">
            <span>保证金 · {{ card.label }}</span>
            <strong class="mono-num">{{ formatMetricValue(card) }}</strong>
          </div>
        </div>
        <div class="export-grid">
          <el-button @click="exportCsv('tasks')">导出跑腿任务报表 (CSV)</el-button>
          <el-button @click="exportCsv('goods')">导出二手商品报表 (CSV)</el-button>
          <el-button @click="exportCsv('shop-orders')">导出店铺预约报表 (CSV)</el-button>
          <el-button @click="exportCsv('project-ads')">导出项目展示报表 (CSV)</el-button>
          <el-button @click="exportCsv('governance')">导出社区治理报表 (CSV)</el-button>
          <el-button @click="exportCsv('fees')">导出账务费用报表 (CSV)</el-button>
        </div>
        <p class="hint">导出的 CSV 报表已自动隐去密码与完整私人联系方式，方便站务归档留存。</p>
      </el-tab-pane>

      <el-tab-pane label="跑腿任务巡查">
        <el-table :data="tasks" stripe>
          <el-table-column prop="id" label="编号" width="80">
            <template #default="{ row }"><span class="mono-num">#{{ row.id }}</span></template>
          </el-table-column>
          <el-table-column prop="title" label="任务标题" />
          <el-table-column prop="acceptanceMode" label="模式" width="110">
            <template #default="{ row }">{{ row.acceptanceMode === 'GRAB' ? '即时抢单' : '报名筛选' }}</template>
          </el-table-column>
          <el-table-column prop="workflowStatus" label="履约状态" width="140">
            <template #default="{ row }">
              <el-tag :type="statusTone(row.workflowStatus)" size="small">{{ formatStatus(row.workflowStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="rewardAmount" label="酬金" width="100">
            <template #default="{ row }"><span class="mono-num">¥{{ row.rewardAmount }}</span></template>
          </el-table-column>
          <el-table-column prop="publisherNickname" label="委托人" width="140" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="跑腿异常报备">
        <el-table :data="issues" stripe>
          <el-table-column prop="id" label="编号" width="80">
            <template #default="{ row }"><span class="mono-num">#{{ row.id }}</span></template>
          </el-table-column>
          <el-table-column prop="taskTitle" label="关联任务" />
          <el-table-column prop="issueType" label="异常类别" width="160">
            <template #default="{ row }">{{ formatIssueType(row.issueType) }}</template>
          </el-table-column>
          <el-table-column prop="description" label="现场说明" />
          <el-table-column prop="reporterNickname" label="报备同学" width="140" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="身份申请队列">
        <el-table :data="roles" stripe>
          <el-table-column prop="userNickname" label="申请同学" />
          <el-table-column prop="roleType" label="申请身份" width="160">
            <template #default="{ row }">{{ formatRoleType(row.roleType) }}</template>
          </el-table-column>
          <el-table-column prop="agreementVersion" label="协议版本" min-width="140" />
          <el-table-column prop="agreementStatus" label="协议确认" min-width="150">
            <template #default="{ row }">{{ row.agreementStatus === 'CONSENTED' ? '已同意' : '历史申请' }}</template>
          </el-table-column>
          <el-table-column prop="reviewStatus" label="核验状态" width="140">
            <template #default="{ row }">
              <el-tag :type="statusTone(row.reviewStatus)" size="small">{{ formatStatus(row.reviewStatus) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="小店预约巡查">
        <el-table :data="shopOrders" stripe>
          <el-table-column prop="id" label="编号" width="80">
            <template #default="{ row }"><span class="mono-num">#{{ row.id }}</span></template>
          </el-table-column>
          <el-table-column prop="shopName" label="店铺名" min-width="140" />
          <el-table-column prop="serviceItemTitle" label="预约项目" min-width="160" />
          <el-table-column prop="customerNickname" label="预约同学" width="120" />
          <el-table-column prop="providerNickname" label="主理人" width="120" />
          <el-table-column prop="status" label="状态" width="130">
            <template #default="{ row }">
              <el-tag :type="statusTone(row.status)" size="small">{{ formatStatus(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="appointmentTime" label="预约时间" min-width="170" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="项目广场审核">
        <div class="ops-toolbar">
          <el-select v-model="projectStatus" clearable placeholder="按状态筛选" @change="loadProjectAds">
            <el-option label="待审核" value="PENDING_REVIEW" />
            <el-option label="展示中" value="APPROVED" />
            <el-option label="未通过" value="REJECTED" />
            <el-option label="已结束" value="CLOSED" />
            <el-option label="已屏蔽" value="BLOCKED" />
          </el-select>
          <el-button @click="loadProjectAds">刷新列表</el-button>
        </div>
        <el-table :data="projectAds" stripe>
          <el-table-column prop="id" label="编号" width="80">
            <template #default="{ row }"><span class="mono-num">#{{ row.id }}</span></template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="adType" label="栏目" width="140">
            <template #default="{ row }">{{ formatAdType(row.adType) }}</template>
          </el-table-column>
          <el-table-column prop="publisherNickname" label="发起人" width="130" />
          <el-table-column prop="status" label="状态" width="130">
            <template #default="{ row }">
              <el-tag :type="statusTone(row.status)" size="small">{{ formatStatus(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="featured" label="精选" width="90">
            <template #default="{ row }">{{ row.featured ? '精选' : '常规' }}</template>
          </el-table-column>
          <el-table-column label="管理操作" width="360">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="approve(row)">通过</el-button>
              <el-button size="small" type="warning" @click="feature(row)">设为精选</el-button>
              <el-button size="small" @click="unfeature(row)">取消精选</el-button>
              <el-button size="small" type="danger" plain @click="reject(row)">退回</el-button>
              <el-button size="small" type="danger" @click="block(row)">下架</el-button>
            </template>
          </el-table-column>
        </el-table>
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
import { formatAdType, formatIssueType, formatRoleType, formatStatus, statusTone } from '@/utils/display'

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
    { key: 'taskOriginZones', title: '跑腿取件校区分布', items: zones.value.taskOriginZones },
    { key: 'taskDestinationZones', title: '跑腿送达校区分布', items: zones.value.taskDestinationZones },
    { key: 'taskRoutes', title: '高频跨校区跑腿路线', items: zones.value.taskRoutes },
    { key: 'goodsZones', title: '二手闲置所在校区', items: zones.value.goodsZones },
    { key: 'shopZones', title: '学生店铺常驻校区', items: zones.value.shopZones },
    { key: 'projectAdZones', title: '项目展示活跃校区', items: zones.value.projectAdZones },
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
  await approveProjectAd(row.id, 4, { note: '内容完整，准予展示' })
  ElMessage.success('已通过展示')
  await loadProjectAds()
}

async function reject(row: ProjectAdSummary) {
  await rejectProjectAd(row.id, 4, { note: '请补充项目说明或有效联络方式' })
  ElMessage.success('已退回修改')
  await loadProjectAds()
}

async function feature(row: ProjectAdSummary) {
  await featureProjectAd(row.id, 4, { featuredPriority: 10 })
  ElMessage.success('已设为广场精选')
  await loadProjectAds()
}

async function unfeature(row: ProjectAdSummary) {
  await unfeatureProjectAd(row.id, 4)
  ElMessage.success('已取消精选')
  await loadProjectAds()
}

async function block(row: ProjectAdSummary) {
  await blockProjectAd(row.id, 4, { note: '站务巡查下架' })
  ElMessage.success('已下架')
  await loadProjectAds()
}

onMounted(() => {
  void load()
  void loadAnalytics()
})
</script>
