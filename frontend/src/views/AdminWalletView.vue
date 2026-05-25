<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  approveAdminWalletRecharge,
  approveAdminWalletWithdrawal,
  completeAdminWalletWithdrawal,
  listAdminWalletRecharges,
  listAdminWalletWithdrawals,
  rejectAdminWalletRecharge,
  rejectAdminWalletWithdrawal,
  type WalletRechargeSummary,
  type WalletWithdrawalSummary,
} from '@/api/campushub'
import EmptyState from '@/components/common/EmptyState.vue'

const loading = ref(false)
const rechargeStatus = ref('')
const withdrawalStatus = ref('')
const recharges = ref<WalletRechargeSummary[]>([])
const withdrawals = ref<WalletWithdrawalSummary[]>([])

async function loadAll() {
  loading.value = true
  try {
    const [rechargeData, withdrawalData] = await Promise.all([
      listAdminWalletRecharges(rechargeStatus.value || undefined),
      listAdminWalletWithdrawals(withdrawalStatus.value || undefined),
    ])
    recharges.value = rechargeData
    withdrawals.value = withdrawalData
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '钱包运营数据加载失败')
  } finally {
    loading.value = false
  }
}

async function approveRecharge(id: number) {
  await approveAdminWalletRecharge(id, 0, '微信充值人工审核通过')
  ElMessage.success('充值已审核通过')
  await loadAll()
}

async function rejectRecharge(id: number) {
  await rejectAdminWalletRecharge(id, 0, '微信充值审核拒绝')
  ElMessage.success('充值已拒绝')
  await loadAll()
}

async function approveWithdrawal(id: number) {
  await approveAdminWalletWithdrawal(id, 0, '提现审核通过')
  ElMessage.success('提现已审核通过')
  await loadAll()
}

async function completeWithdrawal(id: number) {
  await completeAdminWalletWithdrawal(id, 0, '提现已人工打款')
  ElMessage.success('提现已完成')
  await loadAll()
}

async function rejectWithdrawal(id: number) {
  await rejectAdminWalletWithdrawal(id, 0, '提现审核拒绝')
  ElMessage.success('提现已拒绝并解冻')
  await loadAll()
}

onMounted(loadAll)
</script>

<template>
  <section class="page-stack">
    <div class="page-heading admin-page-heading">
      <div>
        <p class="eyebrow">Operations Console</p>
        <h2>钱包运营</h2>
        <p>处理微信充值与提现审核，保持低影响资金运营并追溯每笔状态变化。</p>
      </div>
      <div class="payment-actions">
        <el-button :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <el-tabs class="tabs-surface premium-panel">
      <el-tab-pane label="充值审核">
        <el-select v-model="rechargeStatus" clearable placeholder="全部状态" style="width: 180px" @change="loadAll">
          <el-option label="待审核" value="PENDING_REVIEW" />
          <el-option label="待支付" value="PENDING_PAYMENT" />
          <el-option label="已到账" value="PAID" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
        <EmptyState v-if="!recharges.length && !loading" eyebrow="Recharge" title="暂无充值订单" description="用户发起充值后会出现在这里。" compact />
        <div class="responsive-card-grid">
          <article v-for="item in recharges" :key="item.id" class="info-card premium-panel">
            <strong>{{ item.userNickname }} {{ item.channel }} 充值 ¥{{ item.amount }}</strong>
            <p>状态：{{ item.status }}；实际支付：¥{{ item.payAmount }}</p>
            <p v-if="item.reviewNote">审核备注：{{ item.reviewNote }}</p>
            <div class="payment-actions" v-if="item.status === 'PENDING_REVIEW'">
              <el-button type="primary" @click="approveRecharge(item.id)">通过</el-button>
              <el-button @click="rejectRecharge(item.id)">拒绝</el-button>
            </div>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane label="提现审核">
        <el-select v-model="withdrawalStatus" clearable placeholder="全部状态" style="width: 180px" @change="loadAll">
          <el-option label="待审核" value="PENDING_REVIEW" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
        <EmptyState v-if="!withdrawals.length && !loading" eyebrow="Withdraw" title="暂无提现申请" description="用户申请提现后会出现在这里。" compact />
        <div class="responsive-card-grid">
          <article v-for="item in withdrawals" :key="item.id" class="info-card premium-panel">
            <strong>{{ item.userNickname }} {{ item.channel }} 提现 ¥{{ item.amount }}</strong>
            <p>状态：{{ item.status }}；账号摘要：{{ item.accountSnapshot || '未填写' }}</p>
            <p v-if="item.reviewNote">审核备注：{{ item.reviewNote }}</p>
            <div class="payment-actions" v-if="item.status === 'PENDING_REVIEW'">
              <el-button type="primary" @click="approveWithdrawal(item.id)">通过</el-button>
              <el-button @click="rejectWithdrawal(item.id)">拒绝</el-button>
            </div>
            <div class="payment-actions" v-if="item.status === 'APPROVED'">
              <el-button type="success" @click="completeWithdrawal(item.id)">确认已打款</el-button>
            </div>
          </article>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
