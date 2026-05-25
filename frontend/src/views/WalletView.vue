<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Wallet Center</p>
        <h2>钱包与充值</h2>
        <p>支付宝充值通过 API-Transfer-Station 支付中心创建真实支付链接；微信充值进入人工审核，请备注校园邮箱或 CampusHub 用户名。</p>
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

    <el-tabs class="wallet-tabs">
      <el-tab-pane label="充值">
        <div class="payment-actions">
          <el-button type="primary" @click="rechargeDialogVisible = true">发起充值</el-button>
        </div>
        <EmptyState v-if="!loading && recharges.length === 0" eyebrow="Recharge" title="暂无充值记录" description="支付宝充值会创建支付中心订单，微信充值进入人工审核。" compact />
        <div v-else class="responsive-card-grid">
          <article v-for="item in recharges" :key="item.id" class="info-card">
            <strong>{{ item.channel }} 充值 ¥{{ item.amount }}</strong>
            <p>实际支付：¥{{ item.payAmount }}，手续费：¥{{ item.channelFee }}</p>
            <p>状态：{{ item.status }}</p>
            <p v-if="item.paymentOrderNo">支付单：{{ item.paymentOrderNo }}</p>
            <p v-if="item.paymentProvider">支付渠道：{{ item.paymentProvider }}</p>
            <div class="payment-actions inline-card-actions">
              <el-button v-if="item.paymentPayUrl && item.status !== 'PAID'" size="small" type="primary" @click="openRechargePayUrl(item)">继续支付</el-button>
              <el-button v-if="item.wechatQrUrl && item.status === 'PENDING_REVIEW'" size="small" @click="showWechatQr(item)">查看微信收款码</el-button>
            </div>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane label="提现">
        <div class="payment-actions">
          <el-button @click="withdrawalDialogVisible = true">申请提现</el-button>
        </div>
        <EmptyState v-if="!loading && withdrawals.length === 0" eyebrow="Withdraw" title="暂无提现申请" description="提现提交后会冻结余额，等待管理员人工审核和打款。" compact />
        <div v-else class="responsive-card-grid">
          <article v-for="item in withdrawals" :key="item.id" class="info-card">
            <strong>{{ item.channel }} 提现 ¥{{ item.amount }}</strong>
            <p>状态：{{ item.status }}</p>
            <p v-if="item.accountSnapshot">账号摘要：{{ item.accountSnapshot }}</p>
            <p v-if="item.reviewNote">审核备注：{{ item.reviewNote }}</p>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane label="冻结明细">
        <EmptyState v-if="!loading && frozenItems.length === 0" eyebrow="Frozen" title="暂无冻结明细" description="线上托管交易和提现冻结会显示在这里。" compact />
        <div v-else class="responsive-card-grid">
          <article v-for="item in frozenItems" :key="item.id" class="info-card">
            <strong>{{ item.businessType }} 冻结 ¥{{ item.amount }}</strong>
            <p>状态：{{ item.status }}</p>
            <p>{{ item.remark }}</p>
          </article>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-card shadow="never">
      <template #header>服务费与支付订单</template>
      <EmptyState
        v-if="!loading && serviceFees.length === 0"
        eyebrow="Service Fees"
        title="暂无服务费记录"
        description="平台服务费和支付中心订单会显示在这里。"
        compact
      />
      <div v-else class="mobile-table-wrapper">
        <el-table v-loading="loading" :data="serviceFees" stripe class="data-table">
          <el-table-column prop="feeNo" label="服务费单号" min-width="170" />
          <el-table-column prop="targetType" label="业务类型" width="150" />
          <el-table-column prop="amount" label="金额" width="100">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="paymentProvider" label="Provider" width="130" />
          <el-table-column prop="paymentOrderNo" label="支付订单" min-width="180" />
          <el-table-column prop="paidAt" label="支付时间" min-width="170" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button v-if="row.status !== 'PAID'" size="small" :loading="payingId === row.id" @click="payFee(row.id)">
                创建支付单
              </el-button>
              <el-tag v-else type="success">已支付</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <EmptyState
      v-if="!loading && flows.length === 0"
      eyebrow="Ledger"
      title="暂无钱包流水"
      description="充值、提现、冻结、划转和服务费变动会显示在这里。"
    />
    <div v-else class="mobile-table-wrapper">
      <el-table v-loading="loading" :data="flows" stripe class="data-table">
        <el-table-column prop="flowNo" label="流水号" min-width="180" />
        <el-table-column prop="flowType" label="类型" min-width="130" />
        <el-table-column prop="direction" label="方向" width="90" />
        <el-table-column prop="businessType" label="业务" width="140" />
        <el-table-column prop="amount" label="金额" width="110">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column prop="availableBalanceAfter" label="可用余额" width="120">
          <template #default="{ row }">¥{{ row.availableBalanceAfter ?? row.balanceAfter }}</template>
        </el-table-column>
        <el-table-column prop="frozenBalanceAfter" label="冻结余额" width="120">
          <template #default="{ row }">¥{{ row.frozenBalanceAfter ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" />
      </el-table>
    </div>

    <el-dialog v-model="rechargeDialogVisible" title="钱包充值" width="420px">
      <el-form label-width="90px" class="dialog-form">
        <el-form-item label="渠道">
          <el-select v-model="rechargeForm.channel">
            <el-option label="支付宝实时到账（0.6% 手续费）" value="ALIPAY" />
            <el-option label="微信人工审核（免手续费）" value="WECHAT" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="rechargeForm.amount" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="rechargeForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRecharge">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="wechatQrDialogVisible" title="微信扫码充值" width="420px">
      <div v-if="wechatRecharge" class="wechat-qr-box">
        <p>充值单号：{{ wechatRecharge.rechargeNo }}</p>
        <p>金额：¥{{ wechatRecharge.amount }}</p>
        <img v-if="wechatRecharge.wechatQrUrl" :src="wechatRecharge.wechatQrUrl" alt="微信收款二维码" class="wechat-qr-image" />
        <el-alert type="info" show-icon :title="wechatRecharge.wechatNote || '扫码支付后请备注你的校园邮箱或 CampusHub 用户名，管理员审核后入账。'" />
      </div>
      <template #footer>
        <el-button type="primary" @click="wechatQrDialogVisible = false">我知道了</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="withdrawalDialogVisible" title="申请提现" width="420px">
      <el-alert type="warning" show-icon title="Phase 9 提现为人工审核与人工打款，不会自动调用外部转账通道。" />
      <el-form label-width="90px" class="dialog-form">
        <el-form-item label="渠道">
          <el-select v-model="withdrawalForm.channel">
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="线下" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="withdrawalForm.amount" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="账号摘要">
          <el-input v-model="withdrawalForm.accountSnapshot" placeholder="只填写昵称或尾号，不填写敏感完整凭证" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="withdrawalDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitWithdrawal">提交</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createServiceFeePayment,
  createWalletRecharge,
  createWalletWithdrawal,
  getWallet,
  listServiceFees,
  listWalletFlows,
  listWalletFrozenItems,
  listWalletRecharges,
  listWalletWithdrawals,
  type ServiceFeeSummary,
  type WalletAccountSummary,
  type WalletFlowSummary,
  type WalletFrozenRecordSummary,
  type WalletRechargeSummary,
  type WalletWithdrawalSummary,
} from '@/api/campushub'
import EmptyState from '@/components/common/EmptyState.vue'
import { useAuthStore } from '@/stores/auth'
import { getRechargePaymentAction } from './walletPaymentActions'

const auth = useAuthStore()
const currentUserId = computed(() => auth.currentUser?.id)
const account = ref<WalletAccountSummary>()
const flows = ref<WalletFlowSummary[]>([])
const serviceFees = ref<ServiceFeeSummary[]>([])
const recharges = ref<WalletRechargeSummary[]>([])
const withdrawals = ref<WalletWithdrawalSummary[]>([])
const frozenItems = ref<WalletFrozenRecordSummary[]>([])
const loading = ref(false)
const payingId = ref<number>()
const rechargeDialogVisible = ref(false)
const withdrawalDialogVisible = ref(false)
const wechatQrDialogVisible = ref(false)
const wechatRecharge = ref<WalletRechargeSummary>()
const rechargeForm = reactive({ channel: 'ALIPAY', amount: 10, remark: '' })
const withdrawalForm = reactive({ channel: 'WECHAT', amount: 10, accountSnapshot: '' })

async function loadWallet() {
  loading.value = true
  try {
    const userId = currentUserId.value
    if (!userId) {
      ElMessage.warning('请先登录')
      return
    }
    const [walletAccount, walletFlows, fees, rechargeData, withdrawalData, frozenData] = await Promise.all([
      getWallet(userId),
      listWalletFlows(userId),
      listServiceFees(userId),
      listWalletRecharges(userId),
      listWalletWithdrawals(userId),
      listWalletFrozenItems(userId),
    ])
    account.value = walletAccount
    flows.value = walletFlows
    serviceFees.value = fees
    recharges.value = rechargeData
    withdrawals.value = withdrawalData
    frozenItems.value = frozenData
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '钱包加载失败')
  } finally {
    loading.value = false
  }
}

async function payFee(feeId: number) {
  payingId.value = feeId
  try {
    const payment = await createServiceFeePayment(feeId)
    ElMessage.success(payment.message || '支付单已创建')
    if (payment.payUrl && !payment.payUrl.startsWith('mock://')) {
      window.open(payment.payUrl, '_blank', 'noopener,noreferrer')
    }
    await loadWallet()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '本地支付失败')
  } finally {
    payingId.value = undefined
  }
}

async function submitRecharge() {
  try {
    const userId = currentUserId.value
    if (!userId) {
      ElMessage.warning('请先登录')
      return
    }
    const result = await createWalletRecharge(userId, rechargeForm)
    ElMessage.success(result.channel === 'WECHAT' ? '微信充值已提交人工审核' : '充值支付单已创建')
    rechargeDialogVisible.value = false
    handleRechargePaymentAction(result)
    await loadWallet()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '充值提交失败')
  }
}

function handleRechargePaymentAction(recharge: WalletRechargeSummary) {
  const action = getRechargePaymentAction(recharge)
  if (action.type === 'open-url') {
    window.open(action.url, '_blank', 'noopener,noreferrer')
    return
  }
  if (action.type === 'show-wechat-qr') {
    showWechatQr(recharge)
  }
}

function openRechargePayUrl(recharge: WalletRechargeSummary) {
  if (!recharge.paymentPayUrl) {
    return
  }
  window.open(recharge.paymentPayUrl, '_blank', 'noopener,noreferrer')
}

function showWechatQr(recharge: WalletRechargeSummary) {
  wechatRecharge.value = recharge
  wechatQrDialogVisible.value = true
}

async function submitWithdrawal() {
  try {
    const userId = currentUserId.value
    if (!userId) {
      ElMessage.warning('请先登录')
      return
    }
    await createWalletWithdrawal(userId, withdrawalForm)
    ElMessage.success('提现申请已提交，金额已冻结等待审核')
    withdrawalDialogVisible.value = false
    await loadWallet()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提现提交失败')
  }
}

onMounted(loadWallet)
</script>

<style scoped>
.inline-card-actions {
  margin-top: 10px;
  justify-content: flex-start;
}

.wechat-qr-box {
  display: grid;
  gap: 12px;
  justify-items: center;
  text-align: center;
}

.wechat-qr-image {
  width: min(260px, 80vw);
  max-width: 100%;
  border-radius: 16px;
  border: 1px solid var(--border-color);
}
</style>
