<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Showcase Admin</p>
        <h2>展示管理后台</h2>
        <p>管理项目广告、学生店铺、店铺服务和商家身份审核。</p>
      </div>
      <el-button :loading="loading" @click="loadAll">刷新</el-button>
    </div>

    <el-tabs>
      <el-tab-pane label="广告管理">
        <div class="mobile-table-wrapper">
          <el-table v-loading="loading" :data="projectAds" stripe class="data-table">
            <el-table-column prop="title" label="广告" min-width="180" />
            <el-table-column prop="publisherNickname" label="发布者" width="120" />
            <el-table-column prop="status" label="状态" width="130" />
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="approveProjectAd(row.id)">通过</el-button>
                <el-button size="small" @click="rejectProjectAd(row.id)">拒绝</el-button>
                <el-button size="small" type="danger" @click="blockProjectAd(row.id)">屏蔽</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="商店管理">
        <div class="responsive-card-grid merchant-applications">
          <article v-for="item in merchantApplications" :key="item.id" class="info-card">
            <strong>{{ item.userNickname }} · 店铺商家申请</strong>
            <p>保证金：{{ item.depositStatus }} · 审核：{{ item.reviewStatus }}</p>
            <p v-if="item.applyNote">{{ item.applyNote }}</p>
            <div class="payment-actions inline-card-actions">
              <el-button size="small" type="primary" @click="approveMerchant(item.id)">通过商家</el-button>
              <el-button size="small" @click="rejectMerchant(item.id)">拒绝</el-button>
            </div>
          </article>
        </div>
        <div class="mobile-table-wrapper">
          <el-table v-loading="loading" :data="shops" stripe class="data-table">
            <el-table-column prop="name" label="店铺" min-width="180" />
            <el-table-column prop="ownerNickname" label="店主" width="120" />
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button size="small" @click="resumeShop(row.id)">恢复</el-button>
                <el-button size="small" @click="pauseShop(row.id)">暂停</el-button>
                <el-button size="small" type="danger" @click="blockShop(row.id)">封禁</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="服务管理">
        <div class="mobile-table-wrapper">
          <el-table v-loading="loading" :data="serviceItems" stripe class="data-table">
            <el-table-column prop="title" label="服务" min-width="180" />
            <el-table-column prop="shopName" label="店铺" width="140" />
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="danger" @click="offShelfService(row.id)">下架</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="商店服务售后纠纷">
        <div class="responsive-card-grid">
          <article v-for="order in shopOrders" :key="order.id" class="info-card">
            <strong>{{ order.serviceItemTitle }}</strong>
            <p>客户：{{ order.customerNickname }} · 服务者：{{ order.providerNickname }}</p>
            <p>状态：{{ order.status }} · 金额：¥{{ order.amount }}</p>
            <p v-if="order.cancelReason">取消原因：{{ order.cancelReason }}</p>
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
  await runAction(() => approveAdminShowcaseProjectAd(id, { note: '展示管理员通过' }), '广告已通过')
}

async function rejectProjectAd(id: number) {
  await runAction(() => rejectAdminShowcaseProjectAd(id, { note: '展示管理员拒绝' }), '广告已拒绝')
}

async function blockProjectAd(id: number) {
  await runAction(() => blockAdminShowcaseProjectAd(id, { note: '展示管理员屏蔽' }), '广告已屏蔽')
}

async function pauseShop(id: number) {
  await runAction(() => pauseAdminShowcaseShop(id, '展示管理员暂停'), '店铺已暂停')
}

async function resumeShop(id: number) {
  await runAction(() => resumeAdminShowcaseShop(id, '展示管理员恢复'), '店铺已恢复')
}

async function blockShop(id: number) {
  await runAction(() => blockAdminShowcaseShop(id, '展示管理员封禁'), '店铺已封禁')
}

async function offShelfService(id: number) {
  await runAction(() => offShelfAdminShowcaseServiceItem(id, '展示管理员下架'), '服务已下架')
}

async function approveMerchant(id: number) {
  await runAction(() => approveAdminShowcaseShopMerchantApplication(id), '商家身份已通过')
}

async function rejectMerchant(id: number) {
  await runAction(() => rejectAdminShowcaseShopMerchantApplication(id), '商家身份已拒绝')
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
