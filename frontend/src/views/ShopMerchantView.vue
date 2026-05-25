<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Merchant Workspace</p>
        <h2>店铺商家工作台</h2>
        <p>维护店铺资料、服务项目和预约状态；服务本金仍由双方线下沟通。</p>
      </div>
      <el-button @click="loadWorkspace" :loading="loading">刷新</el-button>
    </div>

    <LockedState
      v-if="auth.currentUser && !auth.canAccessIdentity('shopMerchant')"
      title="需要店铺商家身份"
      description="支付 20 元保证金并通过管理员审核后，可创建店铺、维护服务项目并处理预约。"
      primary-text="去申请身份"
      primary-to="/roles"
      secondary-text="返回学生店铺"
      secondary-to="/shops"
    />

    <template v-else>
      <el-alert v-if="!auth.currentUser" title="请先登录后再管理店铺" type="warning" show-icon :closable="false" />

      <el-alert
        v-else-if="!shop && !loading"
        title="开店需要先通过店铺商家身份审核。若尚未申请，请前往身份保证金页申请 20 元店铺商家身份。"
        type="info"
        show-icon
        :closable="false"
      >
        <template #default>
          <el-button type="primary" @click="router.push({ name: 'roles' })">前往身份申请</el-button>
        </template>
      </el-alert>

      <el-card v-if="auth.currentUser" shadow="never" class="workspace-card">
      <template #header>店铺资料</template>
      <el-form label-position="top" :model="shopForm" class="goods-publish-form form-section-stack">
        <FormSection title="店铺资料" description="展示服务范围、校区、营业时间和联系方式规则。">
          <div class="form-grid">
            <el-form-item label="店铺名"><el-input v-model="shopForm.name" /></el-form-item>
            <el-form-item label="服务校区">
              <el-select v-model="shopForm.campusZone"><el-option v-for="zone in zones" :key="zone.value" :label="zone.label" :value="zone.value" /></el-select>
            </el-form-item>
            <el-form-item label="服务范围"><el-input v-model="shopForm.serviceArea" /></el-form-item>
            <el-form-item label="服务时间"><el-input v-model="shopForm.openingHours" /></el-form-item>
          </div>
          <el-form-item label="店铺描述"><el-input v-model="shopForm.description" type="textarea" :rows="3" /></el-form-item>
        </FormSection>
        <el-button type="primary" :loading="savingShop" @click="saveShop">{{ shop ? '保存店铺资料' : '创建店铺' }}</el-button>
      </el-form>
    </el-card>

    <el-card v-if="shop" shadow="never" class="workspace-card">
      <template #header>
        <div class="panel-topline">
          <strong>服务项目</strong>
          <el-button type="primary" @click="resetItemForm">新增服务</el-button>
        </div>
      </template>
      <el-form label-position="top" :model="itemForm" class="goods-publish-form form-section-stack">
        <FormSection title="服务项目" description="用清晰的价格单位、服务说明和状态管理减少预约前沟通成本。">
          <div class="form-grid">
            <el-form-item label="类别">
              <el-select v-model="itemForm.category">
                <el-option label="摄影" value="PHOTO" />
                <el-option label="贴膜" value="FILM" />
                <el-option label="维修" value="REPAIR" />
                <el-option label="妆造" value="MAKEUP" />
                <el-option label="家教" value="TUTORING" />
                <el-option label="设计" value="DESIGN" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
            <el-form-item label="标题"><el-input v-model="itemForm.title" /></el-form-item>
            <el-form-item label="价格"><el-input-number v-model="itemForm.price" :min="0" :precision="2" class="wide" /></el-form-item>
            <el-form-item label="单位"><el-input v-model="itemForm.priceUnit" /></el-form-item>
            <el-form-item label="时长分钟"><el-input-number v-model="itemForm.durationMinutes" :min="1" class="wide" /></el-form-item>
          </div>
          <el-form-item label="描述"><el-input v-model="itemForm.description" type="textarea" :rows="3" /></el-form-item>
        </FormSection>
        <el-button type="primary" :loading="savingItem" @click="saveItem">保存服务项目</el-button>
      </el-form>

      <div class="shop-service-grid compact-list">
        <el-card v-for="item in shop.serviceItems" :key="item.id" shadow="never">
          <h3>{{ item.title }}</h3>
          <p>{{ item.description }}</p>
          <div class="panel-topline">
            <span>¥{{ item.price }}/{{ item.priceUnit }}</span>
            <el-tag>{{ item.status }}</el-tag>
          </div>
          <div class="card-actions">
            <el-button size="small" @click="editItem(item)">编辑</el-button>
            <el-button size="small" @click="toggleItem(item)">{{ item.status === 'PUBLISHED' ? '暂停' : '发布' }}</el-button>
          </div>
        </el-card>
      </div>
    </el-card>

      <el-card v-if="shop" shadow="never" class="workspace-card">
        <template #header>预约处理</template>
        <div class="mobile-table-wrapper">
          <el-table :data="orders" style="width: 100%">
          <el-table-column prop="serviceItemTitle" label="服务" min-width="140" />
          <el-table-column prop="customerNickname" label="顾客" width="120" />
          <el-table-column prop="appointmentTime" label="时间" min-width="170" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column label="操作" min-width="260">
            <template #default="{ row }">
              <div class="card-actions">
                <el-button size="small" @click="acceptOrder(row.id)">接受</el-button>
                <el-button size="small" @click="startOrder(row.id)">开始</el-button>
                <el-button size="small" type="success" @click="completeOrder(row.id)">完成</el-button>
                <el-button size="small" type="danger" @click="cancelOrder(row.id)">取消</el-button>
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
  await mutateOrder(() => cancelServiceOrder(id, { ...actionPayload(), cancelReason: '商家取消' }))
}

async function mutateOrder(action: () => Promise<unknown>) {
  try {
    await action()
    ElMessage.success('预约状态已更新')
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
