<template>
  <section class="page-stack">
    <div class="page-heading admin-page-heading">
      <div>
        <p class="eyebrow">站务中枢 · 项目与学生店铺巡查</p>
        <h2>展示管理工作台</h2>
        <p>核验项目广场招募信息、审批技能店主理人入驻申请，并维护学生店铺与服务秩序。</p>
      </div>
      <el-button :loading="loading" @click="loadAll">刷新数据</el-button>
    </div>

    <el-tabs class="tabs-surface premium-panel">
      <el-tab-pane label="项目展示审核">
        <div class="mobile-table-wrapper">
          <el-table v-loading="loading" :data="projectAds" stripe class="data-table">
            <el-table-column prop="title" label="项目标题" min-width="180" />
            <el-table-column prop="publisherNickname" label="发起人" width="120" />
            <el-table-column prop="status" label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="statusTone(row.status)" size="small">{{ formatStatus(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="approveProjectAd(row.id)">通过</el-button>
                <el-button size="small" @click="rejectProjectAd(row.id)">退回</el-button>
                <el-button size="small" type="danger" plain @click="blockProjectAd(row.id)">屏蔽</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="学生小店管理">
        <div class="responsive-card-grid merchant-applications">
          <article v-for="item in merchantApplications" :key="item.id" class="info-card">
            <div class="panel-topline">
              <strong>{{ item.userNickname }} · 技能店主理人申请</strong>
              <el-tag :type="statusTone(item.reviewStatus)" size="small">{{ formatStatus(item.reviewStatus) }}</el-tag>
            </div>
            <p v-if="item.agreementStatus === 'CONSENTED'">已同意协议 {{ item.agreementVersion }} · {{ item.agreementConsentedAt }}</p>
            <p v-else>历史申请：{{ formatStatus(item.reviewStatus) }}</p>
            <p v-if="item.applyNote">申请说明：{{ item.applyNote }}</p>
            <div class="payment-actions inline-card-actions">
              <el-button size="small" type="primary" @click="approveMerchant(item.id)">准予开店</el-button>
              <el-button size="small" type="danger" plain @click="rejectMerchant(item.id)">婉拒申请</el-button>
            </div>
          </article>
        </div>
        <div class="mobile-table-wrapper">
          <el-table v-loading="loading" :data="shops" stripe class="data-table">
            <el-table-column prop="name" label="店铺名称" min-width="180" />
            <el-table-column prop="ownerNickname" label="主理人" width="120" />
            <el-table-column prop="status" label="营业状态" width="130">
              <template #default="{ row }">
                <el-tag :type="statusTone(row.status)" size="small">{{ formatStatus(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240">
              <template #default="{ row }">
                <el-button size="small" @click="resumeShop(row.id)">恢复营业</el-button>
                <el-button size="small" @click="pauseShop(row.id)">暂停接单</el-button>
                <el-button size="small" type="danger" plain @click="blockShop(row.id)">关闭店铺</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="店铺服务单项">
        <div class="mobile-table-wrapper">
          <el-table v-loading="loading" :data="serviceItems" stripe class="data-table">
            <el-table-column prop="title" label="服务名称" min-width="180" />
            <el-table-column prop="shopName" label="所属小店" width="140" />
            <el-table-column prop="status" label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="statusTone(row.status)" size="small">{{ formatStatus(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="danger" plain @click="offShelfService(row.id)">下架服务</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="服务预约记录">
        <div class="responsive-card-grid">
          <article v-for="order in shopOrders" :key="order.id" class="info-card">
            <div class="panel-topline">
              <strong>{{ order.serviceItemTitle }}</strong>
              <el-tag :type="statusTone(order.status)" size="small">{{ formatStatus(order.status) }}</el-tag>
            </div>
            <p>预约同学：{{ order.customerNickname }} · 主理人：{{ order.providerNickname }}</p>
            <p>约定金额：<span class="mono-num">¥{{ order.amount }}</span></p>
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
  approveAdminShowcaseProjectAd,
  approveAdminShowcaseShopMerchantApplication,
  blockAdminShowcaseProjectAd,
  blockAdminShowcaseShop,
  listAdminShowcaseProjectAds,
  listAdminShowcaseServiceItems,
  listAdminShowcaseShopMerchantApplications,
  listAdminShowcaseShopOrders,
  listAdminShowcaseShops,
  offShelfAdminShowcaseServiceItem,
  pauseAdminShowcaseShop,
  rejectAdminShowcaseProjectAd,
  rejectAdminShowcaseShopMerchantApplication,
  resumeAdminShowcaseShop,
  type ProjectAdSummary,
  type RoleApplicationSummary,
  type ServiceItemSummary,
  type ServiceOrderSummary,
  type ShopSummary,
} from '@/api/campushub'
import { formatStatus, statusTone } from '@/utils/display'

const loading = ref(false)
const projectAds = ref<ProjectAdSummary[]>([])
const shops = ref<ShopSummary[]>([])
const serviceItems = ref<ServiceItemSummary[]>([])
const shopOrders = ref<ServiceOrderSummary[]>([])
const merchantApplications = ref<RoleApplicationSummary[]>([])

async function loadAll() {
  loading.value = true
  try {
    const [ads, shopData, itemData, orderData, applications] = await Promise.all([
      listAdminShowcaseProjectAds(),
      listAdminShowcaseShops(),
      listAdminShowcaseServiceItems(),
      listAdminShowcaseShopOrders(),
      listAdminShowcaseShopMerchantApplications(),
    ])
    projectAds.value = ads
    shops.value = shopData
    serviceItems.value = itemData
    shopOrders.value = orderData
    merchantApplications.value = applications
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '展示管理数据加载失败')
  } finally {
    loading.value = false
  }
}

async function approveProjectAd(id: number) {
  await runAction(() => approveAdminShowcaseProjectAd(id, { note: '展示管理员核验通过' }), '项目已准予展示')
}

async function rejectProjectAd(id: number) {
  await runAction(() => rejectAdminShowcaseProjectAd(id, { note: '展示管理员退回修改' }), '项目已退回')
}

async function blockProjectAd(id: number) {
  await runAction(() => blockAdminShowcaseProjectAd(id, { note: '展示管理员巡查屏蔽' }), '项目已屏蔽')
}

async function pauseShop(id: number) {
  await runAction(() => pauseAdminShowcaseShop(id, '展示管理员暂停'), '小店已暂停接单')
}

async function resumeShop(id: number) {
  await runAction(() => resumeAdminShowcaseShop(id, '展示管理员恢复'), '小店已恢复营业')
}

async function blockShop(id: number) {
  await runAction(() => blockAdminShowcaseShop(id, '展示管理员关闭'), '小店已关闭')
}

async function offShelfService(id: number) {
  await runAction(() => offShelfAdminShowcaseServiceItem(id, '展示管理员下架'), '服务项目已下架')
}

async function approveMerchant(id: number) {
  await runAction(() => approveAdminShowcaseShopMerchantApplication(id), '已准予开通主理人身份')
}

async function rejectMerchant(id: number) {
  await runAction(() => rejectAdminShowcaseShopMerchantApplication(id), '已婉拒主理人申请')
}

async function runAction(action: () => Promise<unknown>, successMessage: string) {
  try {
    await action()
    ElMessage.success(successMessage)
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

onMounted(loadAll)
</script>

<style scoped>
.merchant-applications {
  margin-bottom: 16px;
}

.inline-card-actions {
  margin-top: 10px;
  justify-content: flex-start;
}
</style>
