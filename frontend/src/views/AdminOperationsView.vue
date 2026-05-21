<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Operations</p>
        <h2>运营后台</h2>
        <p>聚合跑腿任务、异常、身份保证金、举报和违规治理入口。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <div class="ops-metrics" v-if="dashboard">
      <div><span>已发布任务</span><strong>{{ dashboard.publishedTasks }}</strong></div>
      <div><span>履约中</span><strong>{{ dashboard.acceptedTasks }}</strong></div>
      <div><span>已完成</span><strong>{{ dashboard.completedTasks }}</strong></div>
      <div><span>开放异常</span><strong>{{ dashboard.openIssues }}</strong></div>
      <div><span>待审身份</span><strong>{{ dashboard.pendingRoleApplications }}</strong></div>
    </div>

    <el-tabs class="tabs-surface" v-loading="loading">
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
      <el-tab-pane label="举报与违规">
        <p class="hint">举报处理和违规记录继续复用审核治理页面，后续会统一到运营后台。</p>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getOpsDashboard,
  listOpsRoleApplications,
  listOpsShopOrders,
  listOpsTaskIssues,
  listOpsTasks,
  type OperationsDashboardSummary,
  type RewardTaskSummary,
  type RoleApplicationSummary,
  type ServiceOrderSummary,
  type TaskIssueSummary,
} from '@/api/campushub'

const loading = ref(false)
const dashboard = ref<OperationsDashboardSummary | null>(null)
const tasks = ref<RewardTaskSummary[]>([])
const issues = ref<TaskIssueSummary[]>([])
const roles = ref<RoleApplicationSummary[]>([])
const shopOrders = ref<ServiceOrderSummary[]>([])

async function load() {
  loading.value = true
  try {
    const [dashboardData, taskData, issueData, roleData, shopOrderData] = await Promise.all([
      getOpsDashboard(),
      listOpsTasks(),
      listOpsTaskIssues(),
      listOpsRoleApplications(),
      listOpsShopOrders(),
    ])
    dashboard.value = dashboardData
    tasks.value = taskData
    issues.value = issueData
    roles.value = roleData
    shopOrders.value = shopOrderData
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '运营数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
