<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Trade Admin</p>
        <h2>交易管理后台</h2>
        <p>管理悬赏跑腿、二手商品和跑腿商品售后纠纷。</p>
      </div>
      <el-button :loading="loading" @click="loadAll">刷新</el-button>
    </div>

    <el-tabs>
      <el-tab-pane label="跑腿管理">
        <div class="mobile-table-wrapper">
          <el-table v-loading="loading" :data="tasks" stripe class="data-table">
            <el-table-column prop="title" label="任务" min-width="180" />
            <el-table-column prop="publisherNickname" label="发布者" width="120" />
            <el-table-column prop="rewardAmount" label="赏金" width="100" />
            <el-table-column prop="workflowStatus" label="状态" width="150" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="danger" @click="closeTask(row.id)">关闭</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="商品管理">
        <div class="mobile-table-wrapper">
          <el-table v-loading="loading" :data="goods" stripe class="data-table">
            <el-table-column prop="title" label="商品" min-width="180" />
            <el-table-column prop="sellerNickname" label="卖家" width="120" />
            <el-table-column prop="price" label="价格" width="100" />
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="danger" @click="offShelfGoods(row.id)">下架</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="跑腿商品售后纠纷">
        <div class="responsive-card-grid">
          <article v-for="issue in taskIssues" :key="issue.id" class="info-card">
            <strong>{{ issue.taskTitle }}</strong>
            <p>问题类型：{{ issue.issueType }}</p>
            <p>状态：{{ issue.status }}</p>
            <p>{{ issue.description }}</p>
          </article>
          <article v-for="order in goodsOrders" :key="order.id" class="info-card">
            <strong>{{ order.goodsTitle }}</strong>
            <p>订单：{{ order.orderNo }} · {{ order.status }}</p>
            <p>托管：{{ order.escrowStatus }} · ¥{{ order.amount }}</p>
            <p v-if="order.disputeReason">纠纷：{{ order.disputeReason }}</p>
            <p v-if="order.cancelReason">取消：{{ order.cancelReason }}</p>
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
    await closeAdminTradeTask(taskId, '管理员关闭')
    ElMessage.success('任务已关闭')
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '任务关闭失败')
  }
}

async function offShelfGoods(goodsId: number) {
  try {
    await offShelfAdminTradeGoods(goodsId, '管理员下架')
    ElMessage.success('商品已下架')
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '商品下架失败')
  }
}

onMounted(loadAll)
</script>
