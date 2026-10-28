<template>
  <section class="page-stack">
    <div class="page-heading admin-page-heading">
      <div>
        <p class="eyebrow">站务中枢 · 跑腿与二手交易巡查</p>
        <h2>交易管理工作台</h2>
        <p>巡查跑腿大厅悬赏单、二手市集商品状态，并协调处理同窗交接过程中的异常与争议。</p>
      </div>
      <el-button :loading="loading" @click="loadAll">刷新数据</el-button>
    </div>

    <el-tabs class="tabs-surface premium-panel">
      <el-tab-pane label="跑腿悬赏管理">
        <div class="mobile-table-wrapper">
          <el-table v-loading="loading" :data="tasks" stripe class="data-table">
            <el-table-column prop="title" label="任务标题" min-width="180" />
            <el-table-column prop="publisherNickname" label="委托同学" width="120" />
            <el-table-column prop="rewardAmount" label="酬金" width="100">
              <template #default="{ row }"><span class="mono-num">¥{{ row.rewardAmount }}</span></template>
            </el-table-column>
            <el-table-column prop="workflowStatus" label="当前状态" width="140">
              <template #default="{ row }">
                <el-tag :type="statusTone(row.workflowStatus)" size="small">{{ formatStatus(row.workflowStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="danger" plain @click="closeTask(row.id)">关闭任务</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="二手闲置管理">
        <div class="mobile-table-wrapper">
          <el-table v-loading="loading" :data="goods" stripe class="data-table">
            <el-table-column prop="title" label="商品名称" min-width="180" />
            <el-table-column prop="sellerNickname" label="卖家" width="120" />
            <el-table-column prop="price" label="转让价" width="100">
              <template #default="{ row }"><span class="mono-num">¥{{ row.price }}</span></template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="statusTone(row.status)" size="small">{{ formatStatus(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="danger" plain @click="offShelfGoods(row.id)">下架商品</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="异常与交接争议">
        <div class="responsive-card-grid">
          <article v-for="issue in taskIssues" :key="issue.id" class="info-card">
            <div class="panel-topline">
              <strong>跑腿异常 · {{ issue.taskTitle }}</strong>
              <el-tag :type="statusTone(issue.status)" size="small">{{ formatStatus(issue.status) }}</el-tag>
            </div>
            <p>异常原因：{{ formatIssueType(issue.issueType) }}</p>
            <p>{{ issue.description }}</p>
          </article>
          <article v-for="order in goodsOrders" :key="order.id" class="info-card">
            <div class="panel-topline">
              <strong>二手订单 · {{ order.goodsTitle }}</strong>
              <el-tag :type="statusTone(order.status)" size="small">{{ formatStatus(order.status) }}</el-tag>
            </div>
            <p>订单号：<span class="mono-num">{{ order.orderNo }}</span></p>
            <p>担保状态：{{ formatStatus(order.escrowStatus) }} · 金额 <span class="mono-num">¥{{ order.amount }}</span></p>
            <p v-if="order.disputeReason">争议说明：{{ order.disputeReason }}</p>
            <p v-if="order.cancelReason">取消说明：{{ order.cancelReason }}</p>
          </article>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  closeAdminTradeTask,
  listAdminTradeGoods,
  listAdminTradeGoodsOrders,
  listAdminTradeTaskIssues,
  listAdminTradeTasks,
  offShelfAdminTradeGoods,
  type GoodsOrderSummary,
  type GoodsSummary,
  type RewardTaskSummary,
  type TaskIssueSummary,
} from '@/api/campushub'
import { formatIssueType, formatStatus, statusTone } from '@/utils/display'

const loading = ref(false)
const tasks = ref<RewardTaskSummary[]>([])
const goods = ref<GoodsSummary[]>([])
const goodsOrders = ref<GoodsOrderSummary[]>([])
const taskIssues = ref<TaskIssueSummary[]>([])

async function loadAll() {
  loading.value = true
  try {
    const [taskData, goodsData, orderData, issueData] = await Promise.all([
      listAdminTradeTasks(),
      listAdminTradeGoods(),
      listAdminTradeGoodsOrders(),
      listAdminTradeTaskIssues(),
    ])
    tasks.value = taskData
    goods.value = goodsData
    goodsOrders.value = orderData
    taskIssues.value = issueData
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '交易管理数据加载失败')
  } finally {
    loading.value = false
  }
}

async function closeTask(taskId: number) {
  try {
    await closeAdminTradeTask(taskId, '交易管理员巡查关闭')
    ElMessage.success('任务已关闭')
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '任务关闭失败')
  }
}

async function offShelfGoods(goodsId: number) {
  try {
    await offShelfAdminTradeGoods(goodsId, '交易管理员巡查下架')
    ElMessage.success('商品已下架')
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '商品下架失败')
  }
}

onMounted(loadAll)
</script>
