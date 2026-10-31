<script setup lang="ts">
import { defineAsyncComponent, onMounted, ref } from 'vue'
import { usePlatformStore } from '@/stores/platform'
import { ElMessage } from 'element-plus'
import {
  listAdminWalletRecharges,
  listAdminWalletWithdrawals,
  type WalletRechargeSummary,
  type WalletWithdrawalSummary,
} from '@/api/campushub'
import { formatChannel, formatStatus, statusTone } from '@/utils/display'
import EmptyState from '@/components/common/EmptyState.vue'

const platform = usePlatformStore()
const AdminWalletWorkspace = defineAsyncComponent(() => import('./legacy/AdminWalletWorkspace.vue'))
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
    ElMessage.error(error instanceof Error ? error.message : '钱包历史数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => { if (!platform.financialEnabled) void loadAll() })
</script>

<template>
  <AdminWalletWorkspace v-if="platform.financialEnabled" />
  <section v-else class="page-stack">
    <div class="page-heading admin-page-heading">
      <div>
        <p class="eyebrow">站务中枢 · 历史资金记录</p>
        <h2>钱包账务运营</h2>
        <p>当前仅保留充值、提现历史记录的只读查询；新的资金审核、打款和入账操作暂未开放。</p>
      </div>
      <div class="payment-actions">
        <el-button :loading="loading" @click="loadAll">刷新记录</el-button>
      </div>
    </div>

    <el-alert title="资金功能暂未开放，本页面不执行充值入账、提现审核、人工打款或解冻操作。" type="info" show-icon :closable="false" />

    <el-tabs class="tabs-surface premium-panel">
      <el-tab-pane label="充值历史">
        <div style="margin-bottom: 14px">
          <el-select v-model="rechargeStatus" clearable placeholder="全部状态" style="width: 180px" @change="loadAll">
            <el-option label="待人工核对" value="PENDING_REVIEW" />
            <el-option label="待完成支付" value="PENDING_PAYMENT" />
            <el-option label="已到账" value="PAID" />
            <el-option label="未通过" value="REJECTED" />
          </el-select>
        </div>
        <EmptyState v-if="!recharges.length && !loading" eyebrow="充值历史" title="暂无充值记录" description="历史充值记录会显示在这里。" compact />
        <div class="responsive-card-grid">
          <article v-for="item in recharges" :key="item.id" class="info-card">
            <div class="panel-topline">
              <strong>{{ item.userNickname }} · {{ formatChannel(item.channel) }} <span class="mono-num">¥{{ item.amount }}</span></strong>
              <el-tag :type="statusTone(item.status)" size="small">{{ formatStatus(item.status) }}</el-tag>
            </div>
            <p>实付金额：<span class="mono-num">¥{{ item.payAmount }}</span></p>
            <p v-if="item.reviewNote">处理备注：{{ item.reviewNote }}</p>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane label="提现历史">
        <div style="margin-bottom: 14px">
          <el-select v-model="withdrawalStatus" clearable placeholder="全部状态" style="width: 180px" @change="loadAll">
            <el-option label="待核对" value="PENDING_REVIEW" />
            <el-option label="已通过待打款" value="APPROVED" />
            <el-option label="已打款完成" value="COMPLETED" />
            <el-option label="已退回" value="REJECTED" />
          </el-select>
        </div>
        <EmptyState v-if="!withdrawals.length && !loading" eyebrow="提现历史" title="暂无提现记录" description="历史提现记录会显示在这里。" compact />
        <div class="responsive-card-grid">
          <article v-for="item in withdrawals" :key="item.id" class="info-card">
            <div class="panel-topline">
              <strong>{{ item.userNickname }} · {{ formatChannel(item.channel) }}提现 <span class="mono-num">¥{{ item.amount }}</span></strong>
              <el-tag :type="statusTone(item.status)" size="small">{{ formatStatus(item.status) }}</el-tag>
            </div>
            <p>收款账号说明：{{ item.accountSnapshot || '未填写' }}</p>
            <p v-if="item.reviewNote">处理备注：{{ item.reviewNote }}</p>
          </article>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
