<template>
  <section class="page-stack" v-loading="loading">
    <div class="page-heading" v-if="shop">
      <div>
        <p class="eyebrow">Shop Detail</p>
        <h2>{{ shop.name }}</h2>
        <p>{{ shop.serviceArea }} · {{ zoneLabel(shop.campusZone) }} · {{ shop.openingHours || '服务时间面议' }}</p>
      </div>
      <el-button @click="router.push({ name: 'shops' })">返回店铺</el-button>
    </div>

    <EmptyState
      v-if="!loading && !shop"
      eyebrow="Shop Detail"
      title="店铺详情暂不可用"
      description="店铺可能已暂停、关闭或被平台处理。你可以返回学生店铺继续浏览。"
      action-text="返回学生店铺"
      @action="router.push('/shops')"
    />

    <template v-if="shop">
      <el-card shadow="never" class="shop-hero">
        <div>
          <el-tag type="success">{{ shop.status === 'APPROVED' ? '营业中' : shop.status }}</el-tag>
          <h3>{{ shop.description }}</h3>
          <p>店主 {{ shop.ownerNickname }} · 信用分 {{ shop.ownerCreditScore }}</p>
        </div>
        <div class="metric-column">
          <span>评分</span>
          <strong>{{ shop.rating }}</strong>
        </div>
      </el-card>

      <el-alert
        :title="shop.contactVisible ? `联系方式：${shop.contactSnapshot}` : '提交预约后可查看商家微信/QQ 联系方式；服务本金由双方线下沟通，平台不托管。'"
        :type="shop.contactVisible ? 'success' : 'info'"
        show-icon
        :closable="false"
      />

      <div class="shop-service-grid">
        <el-card v-for="item in shop.serviceItems" :key="item.id" shadow="never" class="service-card">
          <div class="panel-topline">
            <el-tag>{{ categoryLabel(item.category) }}</el-tag>
            <span>{{ item.durationMinutes }} 分钟</span>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.description }}</p>
          <div class="goods-price-row">
            <strong>{{ priceLabel(item) }}</strong>
            <span>/{{ item.priceUnit }}</span>
          </div>
          <el-button type="primary" @click="openBooking(item)">预约服务</el-button>
        </el-card>
        <el-empty v-if="shop.serviceItems.length === 0" description="该店铺暂未发布服务项目" />
      </div>
    </template>

    <el-dialog v-model="bookingOpen" title="预约服务" width="460px">
      <el-form label-position="top" :model="bookingForm">
        <el-form-item label="期望时间">
          <el-date-picker v-model="appointmentDate" type="datetime" class="wide" />
        </el-form-item>
        <el-form-item label="预算/金额">
          <el-input-number v-model="bookingForm.amount" :min="0" :precision="2" class="wide" />
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="bookingForm.note" type="textarea" :rows="3" placeholder="说明地点、需求细节或可联系时间" />
        </el-form-item>
      </el-form>
      <p class="hint">预约后系统会按规则保存商家联系方式快照；服务本金仍由双方线下自行协商，平台不托管。</p>
      <template #footer>
        <el-button @click="bookingOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBooking">提交预约</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createServiceOrder,
  getShopDetail,
  type CampusZone,
  type ServiceItemSummary,
  type ShopDetailSummary,
} from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const submitting = ref(false)
const shop = ref<ShopDetailSummary | null>(null)
const bookingOpen = ref(false)
const selectedItem = ref<ServiceItemSummary | null>(null)
const appointmentDate = ref<Date>(new Date(Date.now() + 24 * 60 * 60 * 1000))
const bookingForm = reactive({ amount: 0, note: '' })

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

async function loadShop() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    shop.value = await getShopDetail(id, auth.currentUser?.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '店铺详情加载失败')
  } finally {
    loading.value = false
  }
}

function openBooking(item: ServiceItemSummary) {
  if (!auth.currentUser) {
    ElMessage.warning('请先登录后再预约服务')
    router.push({ name: 'auth' })
    return
  }
  selectedItem.value = item
  bookingForm.amount = Number(item.price)
  bookingForm.note = ''
  appointmentDate.value = new Date(Date.now() + 24 * 60 * 60 * 1000)
  bookingOpen.value = true
}

async function submitBooking() {
  if (!selectedItem.value || !auth.currentUser) return
  submitting.value = true
  try {
    await createServiceOrder(selectedItem.value.id, auth.currentUser.id, {
      appointmentTime: appointmentDate.value.toISOString(),
      amount: bookingForm.amount,
      note: bookingForm.note,
    })
    ElMessage.success('预约已提交，商家会收到站内通知')
    bookingOpen.value = false
    await loadShop()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '预约提交失败')
  } finally {
    submitting.value = false
  }
}

function priceLabel(item: ServiceItemSummary) {
  if (item.minPrice != null && item.maxPrice != null) return `¥${item.minPrice} - ¥${item.maxPrice}`
  return `¥${item.price}`
}

function categoryLabel(value: string) {
  const labels: Record<string, string> = {
    PHOTO: '摄影',
    FILM: '贴膜',
    REPAIR: '维修',
    MAKEUP: '妆造',
    TUTORING: '家教',
    DESIGN: '设计',
    OTHER: '其他',
  }
  return labels[value] ?? value
}

function zoneLabel(value: CampusZone) {
  return zones.find((zone) => zone.value === value)?.label ?? value
}

onMounted(loadShop)
</script>
