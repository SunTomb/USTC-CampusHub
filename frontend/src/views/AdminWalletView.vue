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
import { formatChannel, formatStatus, statusTone } from '@/utils/display'
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
  await approveAdminWalletRecharge(id, 0, '微信扫码充值核对无误入账')
  ElMessage.success('充值已确认入账')
  await loadAll()
}

async function rejectRecharge(id: number) {
  await rejectAdminWalletRecharge(id, 0, '未查到对应收款记录，请核对付款备注')
  ElMessage.success('已退回该充值申请')
  await loadAll()
}

async function approveWithdrawal(id: number) {
  await approveAdminWalletWithdrawal(id, 0, '提现申请核对通过，准备打款')
  ElMessage.success('提现已审核通过')
  await loadAll()
}

async function completeWithdrawal(id: number) {
  await completeAdminWalletWithdrawal(id, 0, '已人工转账至指定收款账户')
  ElMessage.success('已确认打款完成')
  await loadAll()
}

async function rejectWithdrawal(id: number) {
  await rejectAdminWalletWithdrawal(id, 0, '提现信息有误，已退回并解冻余额')
  ElMessage.success('提现已退回并解冻余额')
  await loadAll()
}

onMounted(loadAll)
</script>

<template>
  <section class="page-stack">
    <div class="page-heading admin-page-heading">
      <div>
        <p class="eyebrow">站务中枢 · 资金入账与提现审核</p>
        <h2>钱包账务运营</h2>
        <p>人工核对同学提交的微信扫码免手续费充值单（核对备注中的科大邮箱或用户名），并处理余额提现打款。</p>
      </div>
      <div class="payment-actions">
        <el-button :loading="loading" @click="loadAll">刷新队列</el-button>
      </div>
    </div>

    <el-tabs class="tabs-surface premium-panel">
      <el-tab-pane label="充值入账核对">
        <div style="margin-bottom: 14px">
          <el-select v-model="rechargeStatus" clearable placeholder="全部状态" style="width: 180px" @change="loadAll">
            <el-option label="待人工核对" value="PENDING_REVIEW" />
            <el-option label="待完成支付" value="PENDING_PAYMENT" />
            <el-option label="已到账" value="PAID" />
            <el-option label="未通过" value="REJECTED" />
          </el-select>
        </div>
        <EmptyState v-if="!recharges.length && !loading" eyebrow="充值审核" title="暂无待核对充值单" description="同学发起充值后会显示在这里。" compact />
        <div class="responsive-card-grid">
          <article v-for="item in recharges" :key="item.id" class="info-card">
            <div class="panel-topline">
              <strong>{{ item.userNickname }} · {{ formatChannel(item.channel) }} <span class="mono-num">¥{{ item.amount }}</span></strong>
              <el-tag :type="statusTone(item.status)" size="small">{{ formatStatus(item.status) }}</el-tag>
            </div>
            <p>实付金额：<span class="mono-num">¥{{ item.payAmount }}</span></p>
            <p v-if="item.reviewNote">处理备注：{{ item.reviewNote }}</p>
            <div class="payment-actions" style="margin-top: 10px" v-if="item.status === 'PENDING_REVIEW'">
              <el-button type="primary" size="small" @click="approveRecharge(item.id)">确认收款入账</el-button>
              <el-button size="small" type="danger" plain @click="rejectRecharge(item.id)">退回申请</el-button>
            </div>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane label="提现打款审核">
        <div style="margin-bottom: 14px">
          <el-select v-model="withdrawalStatus" clearable placeholder="全部状态" style="width: 180px" @change="loadAll">
            <el-option label="待核对" value="PENDING_REVIEW" />
            <el-option label="已通过待打款" value="APPROVED" />
            <el-option label="已打款完成" value="COMPLETED" />
            <el-option label="已退回" value="REJECTED" />
          </el-select>
        </div>
        <EmptyState v-if="!withdrawals.length && !loading" eyebrow="提现审核" title="暂无提现申请" description="同学申请余额提现后会显示在这里。" compact />
        <div class="responsive-card-grid">
          <article v-for="item in withdrawals" :key="item.id" class="info-card">
            <div class="panel-topline">
              <strong>{{ item.userNickname }} · {{ formatChannel(item.channel) }}提现 <span class="mono-num">¥{{ item.amount }}</span></strong>
              <el-tag :type="statusTone(item.status)" size="small">{{ formatStatus(item.status) }}</el-tag>
            </div>
            <p>收款账号说明：{{ item.accountSnapshot || '未填写' }}</p>
            <p v-if="item.reviewNote">处理备注：{{ item.reviewNote }}</p>
            <div class="payment-actions" style="margin-top: 10px" v-if="item.status === 'PENDING_REVIEW'">
              <el-button type="primary" size="small" @click="approveWithdrawal(item.id)">审核通过</el-button>
              <el-button size="small" type="danger" plain @click="rejectWithdrawal(item.id)">退回并解冻</el-button>
            </div>
            <div class="payment-actions" style="margin-top: 10px" v-if="item.status === 'APPROVED'">
              <el-button type="success" size="small" @click="completeWithdrawal(item.id)">确认已完成打款</el-button>
            </div>
          </article>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>
