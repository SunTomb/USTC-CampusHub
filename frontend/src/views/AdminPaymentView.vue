<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listAdminPaymentCallbackEvents,
  listAdminPaymentOrders,
  type PaymentCallbackEventSummary,
  type PaymentOrderSummary
} from '../api/campushub'
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
      listAdminPaymentCallbackEvents()
    ])
    orders.value = orderData
    events.value = eventData
  } catch (error) {
    ElMessage.error('支付监控数据加载失败')
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
        <p class="eyebrow">Operations Console</p>
        <h2>支付监控</h2>
        <p>查看服务费与身份保证金支付订单，保持低影响排查并追溯回调处理状态。</p>
      </div>
      <div class="payment-actions">
        <el-select v-model="status" clearable placeholder="全部状态" style="width: 180px" @change="loadPayments">
          <el-option label="待支付" value="PENDING" />
          <el-option label="已支付" value="PAID" />
          <el-option label="失败" value="FAILED" />
          <el-option label="已过期" value="EXPIRED" />
        </el-select>
        <el-button @click="loadPayments">刷新</el-button>
      </div>
    </div>

    <el-tabs class="tabs-surface premium-panel">
      <el-tab-pane label="支付订单">
        <EmptyState v-if="!orders.length && !loading" title="暂无支付订单" description="创建服务费或身份保证金支付单后会出现在这里。" />
        <el-table v-else v-loading="loading" :data="orders" class="mobile-safe-table">
          <el-table-column prop="orderNo" label="本地订单号" min-width="180" />
          <el-table-column prop="businessType" label="业务类型" min-width="120" />
          <el-table-column prop="payerNickname" label="付款用户" min-width="120" />
          <el-table-column prop="amount" label="金额" min-width="90" />
          <el-table-column prop="provider" label="Provider" min-width="130" />
          <el-table-column prop="status" label="状态" min-width="100" />
          <el-table-column prop="expiresAt" label="过期时间" min-width="170" />
          <el-table-column prop="failureReason" label="失败原因" min-width="180" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="回调事件">
        <EmptyState v-if="!events.length && !loading" title="暂无回调事件" description="支付中心回调会记录幂等事件和处理结果。" />
        <el-table v-else v-loading="loading" :data="events" class="mobile-safe-table">
          <el-table-column prop="eventId" label="事件 ID" min-width="170" />
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column prop="status" label="状态" min-width="100" />
          <el-table-column prop="amount" label="金额" min-width="90" />
          <el-table-column prop="verified" label="已校验" min-width="90" />
          <el-table-column prop="handled" label="已处理" min-width="90" />
          <el-table-column prop="failureReason" label="失败原因" min-width="180" />
          <el-table-column prop="createdAt" label="时间" min-width="170" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
