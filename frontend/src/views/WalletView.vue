<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Wallet</p>
        <h2>钱包流水</h2>
        <p>演示平台保证金、服务费和本地 mock 支付流水，不直接处理真实支付密钥。</p>
      </div>
      <el-button :loading="loading" @click="loadWallet">刷新</el-button>
    </div>

    <div v-if="account" class="wallet-strip">
      <div>
        <span>账户</span>
        <strong>{{ account.nickname }}</strong>
      </div>
      <div>
        <span>可用余额</span>
        <strong>¥{{ account.balance }}</strong>
      </div>
      <div>
        <span>冻结金额</span>
        <strong>¥{{ account.frozenBalance }}</strong>
      </div>
      <div>
        <span>状态</span>
        <el-tag type="success">{{ account.status }}</el-tag>
      </div>
    </div>

    <el-card shadow="never">
      <template #header>服务费与本地 mock 支付</template>
      <el-table v-loading="loading" :data="serviceFees" stripe class="data-table">
        <el-table-column prop="feeNo" label="服务费单号" min-width="170" />
        <el-table-column prop="targetType" label="业务类型" width="150" />
        <el-table-column prop="amount" label="金额" width="100">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="paidAt" label="支付时间" min-width="170" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'PAID'" size="small" :loading="payingId === row.id" @click="payFee(row.id)">
              本地模拟支付
            </el-button>
            <el-tag v-else type="success">已支付</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-table v-loading="loading" :data="flows" stripe class="data-table">
      <el-table-column prop="flowNo" label="流水号" min-width="180" />
      <el-table-column prop="direction" label="方向" width="90" />
      <el-table-column prop="businessType" label="业务" width="120" />
      <el-table-column prop="amount" label="金额" width="110">
        <template #default="{ row }">¥{{ row.amount }}</template>
      </el-table-column>
      <el-table-column prop="balanceAfter" label="余额" width="110">
        <template #default="{ row }">¥{{ row.balanceAfter }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="180" />
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createMockServiceFeePayment,
  getWallet,
  listServiceFees,
  listWalletFlows,
  markMockServiceFeeSuccess,
  type ServiceFeeSummary,
  type WalletAccountSummary,
  type WalletFlowSummary,
} from '@/api/campushub'

const account = ref<WalletAccountSummary>()
const flows = ref<WalletFlowSummary[]>([])
const serviceFees = ref<ServiceFeeSummary[]>([])
const loading = ref(false)
const payingId = ref<number>()

async function loadWallet() {
  loading.value = true
  try {
    const [walletAccount, walletFlows, fees] = await Promise.all([getWallet(1), listWalletFlows(1), listServiceFees(1)])
    account.value = walletAccount
    flows.value = walletFlows
    serviceFees.value = fees
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '钱包加载失败')
  } finally {
    loading.value = false
  }
}

async function payFee(feeId: number) {
  payingId.value = feeId
  try {
    const payment = await createMockServiceFeePayment(feeId)
    const status = await markMockServiceFeeSuccess(feeId)
    ElMessage.success(`${payment.provider}: ${status.message}`)
    await loadWallet()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '本地支付失败')
  } finally {
    payingId.value = undefined
  }
}

onMounted(loadWallet)
</script>
