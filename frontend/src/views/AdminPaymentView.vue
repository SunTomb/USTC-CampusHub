<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listAdminPaymentCallbackEvents,
  listAdminPaymentOrders,
  type PaymentCallbackEventSummary,
  type PaymentOrderSummary,
} from '../api/campushub'
import { formatChannel, formatStatus, formatTargetType, statusTone } from '../utils/display'
import EmptyState from '../components/common/EmptyState.vue'

const loading = ref(false)
const status = ref('')
const orders = ref<PaymentOrderSummary[]>([])
const events = ref<PaymentCallbackEventSummary[]>([])

async function loadPayments() {
  loading.value = true
  try {
    const [orderData, eventData] = await Promise.all([
      listAdminPaymentOrders(status.value || undefined),
      listAdminPaymentCallbackEvents(),
    ])
    orders.value = orderData
    events.value = eventData
  } catch (error) {
    ElMessage.error('收银台监控数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadPayments)
</script>

<template>
  <section class="page-stack">
    <div class="page-heading admin-page-heading">
      <div>
        <p class="eyebrow">站务中枢 · 收银台账务对账</p>
        <h2>支付监控中心</h2>
        <p>只读查看历史支付订单与回调日志；新的充值、身份保证金和服务费支付目前暂未开放。</p>
      </div>
      <div class="payment-actions">
        <el-select v-model="status" clearable placeholder="全部订单状态" style="width: 180px" @change="loadPayments">
          <el-option label="待完成支付" value="PENDING" />
          <el-option label="已支付到账" value="PAID" />
          <el-option label="支付未完成" value="FAILED" />
          <el-option label="订单已过期" value="EXPIRED" />
        </el-select>
        <el-button @click="loadPayments">刷新对账</el-button>
      </div>
    </div>

    <el-alert title="资金相关新业务暂未开放，本页面仅用于查看历史订单和回调记录。" type="info" show-icon :closable="false" />

    <el-tabs class="tabs-surface premium-panel">
      <el-tab-pane label="收银台订单流水">
        <EmptyState v-if="!orders.length && !loading" title="暂无收银台订单" description="仅展示已有支付订单；当前不创建新的收银台订单。" />
        <el-table v-else v-loading="loading" :data="orders" class="mobile-safe-table" stripe>
          <el-table-column prop="orderNo" label="收银台订单号" min-width="180">
            <template #default="{ row }"><span class="mono-num">{{ row.orderNo }}</span></template>
          </el-table-column>
          <el-table-column prop="businessType" label="关联业务" min-width="130">
            <template #default="{ row }">{{ formatTargetType(row.businessType) }}</template>
          </el-table-column>
          <el-table-column prop="payerNickname" label="付款同学" min-width="120" />
          <el-table-column prop="amount" label="金额" min-width="100">
            <template #default="{ row }"><strong class="mono-num">¥{{ row.amount }}</strong></template>
          </el-table-column>
          <el-table-column prop="provider" label="收款通道" min-width="140">
            <template #default="{ row }">{{ formatChannel(row.provider) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" min-width="120">
            <template #default="{ row }">
              <el-tag :type="statusTone(row.status)" size="small">{{ formatStatus(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="expiresAt" label="有效截止" min-width="170" />
          <el-table-column prop="failureReason" label="异常备注" min-width="180" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="异步回调通知日志">
        <EmptyState v-if="!events.length && !loading" title="暂无回调通知日志" description="收银台完成支付后的签名核验与入账回调会记录于此。" />
        <el-table v-else v-loading="loading" :data="events" class="mobile-safe-table" stripe>
          <el-table-column prop="eventId" label="回调事件号" min-width="170">
            <template #default="{ row }"><span class="mono-num">{{ row.eventId }}</span></template>
          </el-table-column>
          <el-table-column prop="orderNo" label="关联订单号" min-width="180">
            <template #default="{ row }"><span class="mono-num">{{ row.orderNo }}</span></template>
          </el-table-column>
          <el-table-column prop="status" label="通知状态" min-width="120">
            <template #default="{ row }">
              <el-tag :type="statusTone(row.status)" size="small">{{ formatStatus(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" min-width="100">
            <template #default="{ row }"><span class="mono-num">¥{{ row.amount }}</span></template>
          </el-table-column>
          <el-table-column prop="verified" label="签名核验" min-width="100">
            <template #default="{ row }">{{ row.verified ? '已通过' : '未通过' }}</template>
          </el-table-column>
          <el-table-column prop="handled" label="入账处理" min-width="100">
            <template #default="{ row }">{{ row.handled ? '已入账' : '待处理' }}</template>
          </el-table-column>
          <el-table-column prop="failureReason" label="异常说明" min-width="180" />
          <el-table-column prop="createdAt" label="接收时间" min-width="170" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
