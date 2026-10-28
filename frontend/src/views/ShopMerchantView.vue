<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">主理人工作台 · 店铺经营</p>
        <h2>我的技能小店</h2>
        <p>在这里维护你的小店招牌、服务价目表并处理同窗发来的预约单。</p>
      </div>
      <el-button @click="loadWorkspace" :loading="loading">刷新数据</el-button>
    </div>

    <LockedState
      v-if="auth.currentUser && !auth.canAccessIdentity('shopMerchant') && !auth.isAdmin"
      title="需先开通「技能店主理人」身份"
      description="缴纳 20 元保证金并通过站务核验后，即可开启个人小店、上架服务项目并承接校园预约。"
      primary-text="去申请主理人身份"
      primary-to="/roles"
      secondary-text="返回学生店铺"
      secondary-to="/shops"
    />

    <template v-else>
      <el-alert v-if="!auth.currentUser" title="请先登录后再管理店铺" type="warning" show-icon :closable="false" />

      <el-alert
        v-else-if="!shop && !loading"
        title="欢迎开启你的第一家校园技能小店！填写下方店铺资料即可创建（若尚未开通主理人身份，请先前往身份解锁页申请）。"
        type="info"
        show-icon
        :closable="false"
      >
        <template #default>
          <el-button type="primary" size="small" @click="router.push({ name: 'roles' })">查看身份状态</el-button>
        </template>
      </el-alert>

      <el-card v-if="auth.currentUser" shadow="never" class="workspace-card premium-panel">
        <template #header>店铺招牌与基础信息</template>
        <el-form label-position="top" :model="shopForm" class="goods-publish-form form-section-stack">
          <FormSection title="小店名片" description="清晰的常驻校区与可约时段，能让有需要的同学更快找到你。">
            <div class="form-grid">
              <el-form-item label="小店名称"><el-input v-model="shopForm.name" placeholder="如：西区光影人像馆 / 极客笔记本清灰工坊" /></el-form-item>
              <el-form-item label="常驻校区">
                <el-select v-model="shopForm.campusZone"><el-option v-for="zone in zones" :key="zone.value" :label="zone.label" :value="zone.value" /></el-select>
              </el-form-item>
              <el-form-item label="擅长领域"><el-input v-model="shopForm.serviceArea" placeholder="如：胶片写真、毕业照、拯救者清灰换硅脂" /></el-form-item>
              <el-form-item label="可约时段"><el-input v-model="shopForm.openingHours" placeholder="如：周三下午及周末全天" /></el-form-item>
            </div>
            <el-form-item label="主理人介绍与小店公告"><el-input v-model="shopForm.description" type="textarea" :rows="3" /></el-form-item>
          </FormSection>
          <el-button type="primary" :loading="savingShop" @click="saveShop">{{ shop ? '保存店铺资料' : '立即创建小店' }}</el-button>
        </el-form>
      </el-card>

      <el-card v-if="shop" shadow="never" class="workspace-card premium-panel">
        <template #header>
          <div class="panel-topline">
            <strong>服务价目表管理</strong>
            <el-button type="primary" size="small" @click="resetItemForm">新增服务项目</el-button>
          </div>
        </template>
        <el-form label-position="top" :model="itemForm" class="goods-publish-form form-section-stack">
          <FormSection title="编辑 / 新增服务单项" description="标明计价单位与预计耗时，方便同学合理安排时间。">
            <div class="form-grid">
              <el-form-item label="服务分类">
                <el-select v-model="itemForm.category">
                  <el-option label="摄影约拍" value="PHOTO" />
                  <el-option label="数码贴膜" value="FILM" />
                  <el-option label="电脑维修与清灰" value="REPAIR" />
                  <el-option label="妆造造型" value="MAKEUP" />
                  <el-option label="学业答疑" value="TUTORING" />
                  <el-option label="设计排版" value="DESIGN" />
                  <el-option label="其他特长" value="OTHER" />
                </el-select>
              </el-form-item>
              <el-form-item label="项目名称"><el-input v-model="itemForm.title" placeholder="如：单人校园外景写真（含精修9张）" /></el-form-item>
              <el-form-item label="单价（元）"><el-input-number v-model="itemForm.price" :min="0" :precision="2" class="wide" /></el-form-item>
              <el-form-item label="计价单位"><el-input v-model="itemForm.priceUnit" placeholder="次 / 小时 / 台" /></el-form-item>
              <el-form-item label="预计时长（分钟）"><el-input-number v-model="itemForm.durationMinutes" :min="1" class="wide" /></el-form-item>
            </div>
            <el-form-item label="服务包含内容与说明"><el-input v-model="itemForm.description" type="textarea" :rows="3" /></el-form-item>
          </FormSection>
          <el-button type="primary" :loading="savingItem" @click="saveItem">保存服务项目</el-button>
        </el-form>

        <div class="shop-service-grid compact-list">
          <el-card v-for="item in shop.serviceItems" :key="item.id" shadow="never" class="service-card">
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
            <div class="panel-topline">
              <span class="mono-num">¥{{ item.price }} / {{ item.priceUnit }}</span>
              <el-tag :type="statusTone(item.status)">{{ formatStatus(item.status) }}</el-tag>
            </div>
            <div class="card-actions">
              <el-button size="small" @click="editItem(item)">修改</el-button>
              <el-button size="small" @click="toggleItem(item)">{{ item.status === 'PUBLISHED' ? '暂停接单' : '重新上架' }}</el-button>
            </div>
          </el-card>
        </div>
      </el-card>

      <el-card v-if="shop" shadow="never" class="workspace-card premium-panel">
        <template #header>同窗预约单管理</template>
        <div class="mobile-table-wrapper">
          <el-table :data="orders" style="width: 100%" stripe>
            <el-table-column prop="serviceItemTitle" label="预约项目" min-width="150" />
            <el-table-column prop="customerNickname" label="预约同学" width="120" />
            <el-table-column prop="appointmentTime" label="期望时间" min-width="170" />
            <el-table-column prop="status" label="当前状态" width="130">
              <template #default="{ row }">
                <el-tag :type="statusTone(row.status)" size="small">{{ formatStatus(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="接单处理" min-width="260">
              <template #default="{ row }">
                <div class="card-actions">
                  <el-button size="small" type="primary" plain @click="acceptOrder(row.id)">确认接单</el-button>
                  <el-button size="small" @click="startOrder(row.id)">开始服务</el-button>
                  <el-button size="small" type="success" @click="completeOrder(row.id)">标记完成</el-button>
                  <el-button size="small" type="danger" plain @click="cancelOrder(row.id)">婉拒/取消</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
    </template>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  acceptServiceOrder,
  cancelServiceOrder,
  completeServiceOrder,
  createServiceItem,
  createShop,
  getMyShop,
  listShopOrders,
  pauseServiceItem,
  publishServiceItem,
  startServiceOrder,
  updateServiceItem,
  updateShop,
  type CampusZone,
  type CreateServiceItemPayload,
  type CreateShopPayload,
  type ServiceItemSummary,
  type ServiceOrderSummary,
  type ShopDetailSummary,
} from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import { formatStatus, statusTone } from '@/utils/display'
import LockedState from '@/components/common/LockedState.vue'
import FormSection from '@/components/common/FormSection.vue'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const savingShop = ref(false)
const savingItem = ref(false)
const shop = ref<ShopDetailSummary | null>(null)
const orders = ref<ServiceOrderSummary[]>([])
const editingItemId = ref<number | null>(null)

const zones: Array<{ label: string; value: CampusZone }> = [
  { label: '中校区', value: 'CENTRAL' },
  { label: '西校区', value: 'WEST' },
  { label: '东校区', value: 'EAST' },
  { label: '北校区', value: 'NORTH' },
  { label: '南校区', value: 'SOUTH' },
  { label: '高新校区', value: 'HIGH_TECH' },
  { label: '先研院', value: 'ADVANCED_RESEARCH_INSTITUTE' },
  { label: '科学岛', value: 'SCIENCE_ISLAND' },
  { label: '其他', value: 'OTHER' },
]

const shopForm = reactive<CreateShopPayload>({
  name: '',
  description: '',
  serviceArea: '',
  campusZone: 'CENTRAL',
  contactVisibility: 'ORDER_ONLY',
  openingHours: '',
})

const itemForm = reactive<CreateServiceItemPayload>({
  category: 'OTHER',
  title: '',
  description: '',
  price: 0,
  priceUnit: '次',
  durationMinutes: 60,
})

async function loadWorkspace() {
  if (!auth.currentUser) return
  loading.value = true
  try {
    shop.value = await getMyShop(auth.currentUser.id)
    Object.assign(shopForm, {
      name: shop.value.name,
      description: shop.value.description,
      serviceArea: shop.value.serviceArea,
      campusZone: shop.value.campusZone,
      contactVisibility: shop.value.contactVisibility,
      openingHours: shop.value.openingHours ?? '',
    })
    orders.value = await listShopOrders(shop.value.id, auth.currentUser.id)
  } catch {
    shop.value = null
    orders.value = []
  } finally {
    loading.value = false
  }
}

async function saveShop() {
  if (!auth.currentUser) return
  savingShop.value = true
  try {
    shop.value = shop.value
      ? await updateShop(shop.value.id, auth.currentUser.id, shopForm)
      : await createShop(auth.currentUser.id, shopForm)
    ElMessage.success('店铺资料已保存')
    await loadWorkspace()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '店铺保存失败')
  } finally {
    savingShop.value = false
  }
}

async function saveItem() {
  if (!auth.currentUser || !shop.value) return
  savingItem.value = true
  try {
    if (editingItemId.value) {
      await updateServiceItem(editingItemId.value, auth.currentUser.id, itemForm)
    } else {
      await createServiceItem(shop.value.id, auth.currentUser.id, itemForm)
    }
    ElMessage.success('服务项目已保存')
    resetItemForm()
    await loadWorkspace()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '服务项目保存失败')
  } finally {
    savingItem.value = false
  }
}

function editItem(item: ServiceItemSummary) {
  editingItemId.value = item.id
  Object.assign(itemForm, {
    category: item.category,
    title: item.title,
    description: item.description,
    price: item.price,
    priceUnit: item.priceUnit,
    durationMinutes: item.durationMinutes,
  })
}

function resetItemForm() {
  editingItemId.value = null
  Object.assign(itemForm, { category: 'OTHER', title: '', description: '', price: 0, priceUnit: '次', durationMinutes: 60 })
}

async function toggleItem(item: ServiceItemSummary) {
  if (!auth.currentUser) return
  if (item.status === 'PUBLISHED') await pauseServiceItem(item.id, auth.currentUser.id)
  else await publishServiceItem(item.id, auth.currentUser.id)
  await loadWorkspace()
}

async function acceptOrder(id: number) {
  await mutateOrder(() => acceptServiceOrder(id, actionPayload()))
}

async function startOrder(id: number) {
  await mutateOrder(() => startServiceOrder(id, actionPayload()))
}

async function completeOrder(id: number) {
  await mutateOrder(() => completeServiceOrder(id, actionPayload()))
}

async function cancelOrder(id: number) {
  await mutateOrder(() => cancelServiceOrder(id, { ...actionPayload(), cancelReason: '主理人档期冲突取消' }))
}

async function mutateOrder(action: () => Promise<unknown>) {
  try {
    await action()
    ElMessage.success('预约单状态已更新')
    await loadWorkspace()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '预约操作失败')
  }
}

function actionPayload() {
  if (!auth.currentUser) {
    throw new Error('请先登录')
  }
  return { actorId: auth.currentUser.id }
}

onMounted(loadWorkspace)
</script>
