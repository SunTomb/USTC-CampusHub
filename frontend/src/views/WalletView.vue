<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">我的书院 · 资金与账本</p>
        <h2>钱包与充值中心</h2>
        <p>支付宝充值通过收银台实时到账；微信扫码充值免手续费，支付时备注你的科大邮箱或用户名即可由站务核对入账。</p>
      </div>
      <el-button :loading="loading" @click="loadWallet">刷新账本</el-button>
    </div>

    <div v-if="account" class="wallet-strip spotlight-surface">
      <div>
        <span>持卡校友</span>
        <strong>{{ account.nickname }}</strong>
      </div>
      <div>
        <span>可用余额</span>
        <strong class="mono-num">¥{{ account.balance }}</strong>
      </div>
      <div>
        <span>担保 / 提现冻结中</span>
        <strong class="mono-num">¥{{ account.frozenBalance }}</strong>
      </div>
      <div>
        <span>账户状态</span>
        <div>
          <el-tag :type="statusTone(account.status)" effect="dark">{{ formatStatus(account.status) }}</el-tag>
        </div>
      </div>
    </div>

    <el-tabs class="wallet-tabs tabs-surface premium-panel">
      <el-tab-pane label="余额充值">
        <div class="payment-actions" style="margin-bottom: 14px">
          <el-button type="primary" @click="rechargeDialogVisible = true">发起余额充值</el-button>
        </div>
        <EmptyState
          v-if="!loading && recharges.length === 0"
          eyebrow="余额充值"
          title="还没有充值记录"
          description="支持支付宝实时到账或微信扫码免手续费充值。"
          compact
        />
        <div v-else class="responsive-card-grid">
          <article v-for="item in recharges" :key="item.id" class="info-card">
            <div class="panel-topline">
              <strong>{{ formatChannel(item.channel) }} · <span class="mono-num">¥{{ item.amount }}</span></strong>
              <el-tag :type="statusTone(item.status)" size="small">{{ formatStatus(item.status) }}</el-tag>
            </div>
            <p>实付金额：<span class="mono-num">¥{{ item.payAmount }}</span>（通道手续费：<span class="mono-num">¥{{ item.channelFee }}</span>）</p>
            <p v-if="item.paymentOrderNo">收银台单号：<span class="mono-num">{{ item.paymentOrderNo }}</span></p>
            <p v-if="item.paymentProvider">收款通道：{{ formatChannel(item.paymentProvider) }}</p>
            <div class="payment-actions inline-card-actions">
              <el-button v-if="item.paymentPayUrl && item.status !== 'PAID'" size="small" type="primary" @click="openRechargePayUrl(item)">
                继续支付
              </el-button>
              <el-button v-if="item.wechatQrUrl && item.status === 'PENDING_REVIEW'" size="small" @click="showWechatQr(item)">
                查看微信收款码
              </el-button>
            </div>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane label="余额提现">
        <div class="payment-actions" style="margin-bottom: 14px">
          <el-button @click="withdrawalDialogVisible = true">申请余额提现</el-button>
        </div>
        <EmptyState
          v-if="!loading && withdrawals.length === 0"
          eyebrow="余额提现"
          title="暂无提现申请"
          description="提交提现申请后将冻结对应余额，由站务核对无误后人工转账至你的收款账户。"
          compact
        />
        <div v-else class="responsive-card-grid">
          <article v-for="item in withdrawals" :key="item.id" class="info-card">
            <div class="panel-topline">
              <strong>{{ formatChannel(item.channel) }}提现 · <span class="mono-num">¥{{ item.amount }}</span></strong>
              <el-tag :type="statusTone(item.status)" size="small">{{ formatStatus(item.status) }}</el-tag>
            </div>
            <p v-if="item.accountSnapshot">收款账号备注：{{ item.accountSnapshot }}</p>
            <p v-if="item.reviewNote">站务处理说明：{{ item.reviewNote }}</p>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane label="冻结资金明细">
        <EmptyState
          v-if="!loading && frozenItems.length === 0"
          eyebrow="冻结明细"
          title="当前没有冻结中的资金"
          description="当你发起二手商品线上担保交易或申请余额提现时，暂冻金额会列在这里。"
          compact
        />
        <div v-else class="responsive-card-grid">
          <article v-for="item in frozenItems" :key="item.id" class="info-card">
            <div class="panel-topline">
              <strong>{{ formatTargetType(item.businessType) }} · <span class="mono-num">¥{{ item.amount }}</span></strong>
              <el-tag :type="statusTone(item.status)" size="small">{{ formatStatus(item.status) }}</el-tag>
            </div>
            <p>{{ item.remark }}</p>
          </article>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-card shadow="never" class="premium-panel">
      <template #header>平台服务费账单</template>
      <EmptyState
        v-if="!loading && serviceFees.length === 0"
        eyebrow="服务费账单"
        title="暂无待结或历史服务费"
        description="交易产生的轻量平台服务费账单会记录于此。"
        compact
      />
      <div v-else class="mobile-table-wrapper">
        <el-table v-loading="loading" :data="serviceFees" stripe class="data-table">
          <el-table-column prop="feeNo" label="账单编号" min-width="170">
            <template #default="{ row }"><span class="mono-num">{{ row.feeNo }}</span></template>
          </el-table-column>
          <el-table-column prop="targetType" label="关联业务" width="150">
            <template #default="{ row }">{{ formatTargetType(row.targetType) }}</template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="110">
            <template #default="{ row }"><span class="mono-num">¥{{ row.amount }}</span></template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusTone(row.status)" size="small">{{ formatStatus(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="paymentProvider" label="支付通道" width="140">
            <template #default="{ row }">{{ formatChannel(row.paymentProvider) }}</template>
          </el-table-column>
          <el-table-column prop="paymentOrderNo" label="收银台单号" min-width="180">
            <template #default="{ row }"><span class="mono-num">{{ row.paymentOrderNo || '—' }}</span></template>
          </el-table-column>
          <el-table-column prop="paidAt" label="完成时间" min-width="170" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button v-if="row.status !== 'PAID'" size="small" type="primary" plain :loading="payingId === row.id" @click="payFee(row.id)">
                前往支付
              </el-button>
              <el-tag v-else type="success" size="small">已结清</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-card shadow="never" class="premium-panel">
      <template #header>钱包收支流水</template>
      <EmptyState
        v-if="!loading && flows.length === 0"
        eyebrow="收支账本"
        title="暂无钱包流水记录"
        description="每一笔充值、提现、担保冻结与放款都会清晰记录在账本中。"
        compact
      />
      <div v-else class="mobile-table-wrapper">
        <el-table v-loading="loading" :data="flows" stripe class="data-table">
          <el-table-column prop="flowNo" label="流水单号" min-width="180">
            <template #default="{ row }"><span class="mono-num">{{ row.flowNo }}</span></template>
          </el-table-column>
          <el-table-column prop="flowType" label="账务类型" min-width="140">
            <template #default="{ row }">{{ formatFlowType(row.flowType) }}</template>
          </el-table-column>
          <el-table-column prop="direction" label="收支方向" width="100">
            <template #default="{ row }">{{ formatDirection(row.direction) }}</template>
          </el-table-column>
          <el-table-column prop="businessType" label="所属板块" width="140">
            <template #default="{ row }">{{ formatTargetType(row.businessType) }}</template>
          </el-table-column>
          <el-table-column prop="amount" label="变动金额" width="120">
            <template #default="{ row }"><strong class="mono-num">¥{{ row.amount }}</strong></template>
          </el-table-column>
          <el-table-column prop="availableBalanceAfter" label="变动后可用" width="130">
            <template #default="{ row }"><span class="mono-num">¥{{ row.availableBalanceAfter ?? row.balanceAfter }}</span></template>
          </el-table-column>
          <el-table-column prop="frozenBalanceAfter" label="变动后冻结" width="130">
            <template #default="{ row }"><span class="mono-num">¥{{ row.frozenBalanceAfter ?? 0 }}</span></template>
          </el-table-column>
          <el-table-column prop="remark" label="账务备注" min-width="180" />
        </el-table>
      </div>
    </el-card>

    <el-dialog v-model="rechargeDialogVisible" title="发起钱包充值" width="440px">
      <el-form label-width="90px" class="dialog-form">
        <el-form-item label="充值方式">
          <el-select v-model="rechargeForm.channel" class="wide">
            <el-option label="支付宝收银台（实时到账，0.6% 通道费）" value="ALIPAY" />
            <el-option label="微信扫码转账（免手续费，站务人工核对）" value="WECHAT" />
          </el-select>
        </el-form-item>
        <el-form-item label="充值金额">
          <el-input-number v-model="rechargeForm.amount" :min="0.01" :precision="2" class="wide" />
        </el-form-item>
        <el-form-item label="附言备注">
          <el-input v-model="rechargeForm.remark" placeholder="选填，可填写用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRecharge">确认发起充值</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="wechatQrDialogVisible" title="微信扫码免手续费充值" width="420px">
      <div v-if="wechatRecharge" class="wechat-qr-box">
        <p>充值单号：<span class="mono-num">{{ wechatRecharge.rechargeNo }}</span></p>
        <p>应付金额：<strong class="mono-num">¥{{ wechatRecharge.amount }}</strong></p>
        <img v-if="wechatRecharge.wechatQrUrl" :src="wechatRecharge.wechatQrUrl" alt="微信收款二维码" class="wechat-qr-image" />
        <el-alert
          type="info"
          show-icon
          :closable="false"
          :title="wechatRecharge.wechatNote || '扫码支付时请务必在付款备注中填写你的科大邮箱或 CampusHub 用户名，站务同学核对后将尽快为你入账。'"
        />
      </div>
      <template #footer>
        <el-button type="primary" @click="wechatQrDialogVisible = false">我已完成扫码备注</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="withdrawalDialogVisible" title="申请余额提现" width="440px">
      <el-alert
        type="warning"
        show-icon
        :closable="false"
        title="提现提交后将暂冻对应余额，由站务同学核对无误后人工转账至你指定的收款方式。"
      />
      <el-form label-width="90px" class="dialog-form">
        <el-form-item label="收款渠道">
          <el-select v-model="withdrawalForm.channel" class="wide">
            <el-option label="微信收款" value="WECHAT" />
            <el-option label="支付宝收款" value="ALIPAY" />
            <el-option label="校内当面结算" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item label="提现金额">
          <el-input-number v-model="withdrawalForm.amount" :min="0.01" :precision="2" class="wide" />
        </el-form-item>
        <el-form-item label="收款说明">
          <el-input v-model="withdrawalForm.accountSnapshot" placeholder="填写收款账号昵称或尾号提示，请勿填写任何密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="withdrawalDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitWithdrawal">提交提现申请</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
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
import { formatChannel, formatDirection, formatFlowType, formatStatus, formatTargetType, statusTone } from '@/utils/display'
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
    if (auth.token && !auth.currentUser && !auth.sessionLoaded) {
      await auth.loadCurrentUser()
    }
    const userId = currentUserId.value
    if (!userId) {
      if (!auth.token) {
        ElMessage.warning('请先登录')
      }
      return
    }
    const authLoadOptions = { skipAuthExpireHandling: true }
    const [walletAccount, walletFlows, fees, rechargeData, withdrawalData, frozenData] = await Promise.all([
      getWallet(userId, authLoadOptions),
      listWalletFlows(userId, authLoadOptions),
      listServiceFees(userId, authLoadOptions),
      listWalletRecharges(userId, authLoadOptions),
      listWalletWithdrawals(userId, authLoadOptions),
      listWalletFrozenItems(userId, authLoadOptions),
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
    ElMessage.error(error instanceof Error ? error.message : '支付单创建失败')
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
    ElMessage.success(result.channel === 'WECHAT' ? '微信充值单已生成，请扫码并备注邮箱或用户名' : '支付宝收银台订单已创建')
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
    ElMessage.success('提现申请已提交，对应余额已暂冻等待站务核对打款')
    withdrawalDialogVisible.value = false
    await loadWallet()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提现提交失败')
  }
}

watch(
  () => auth.currentUser?.id,
  (userId) => {
    if (userId) {
      void loadWallet()
    }
  },
)

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
  border: 1px solid var(--border-subtle);
}
</style>
