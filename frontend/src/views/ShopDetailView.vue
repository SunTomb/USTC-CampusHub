<template>
  <section class="page-stack" v-loading="loading">
    <div class="page-heading" v-if="shop">
      <div>
        <p class="eyebrow">同窗小店</p>
        <h2>价目与预约</h2>
      </div>
      <el-button @click="router.push({ name: 'shops' })">返回店铺列表</el-button>
    </div>

    <EmptyState
      v-if="!loading && !shop"
      eyebrow="同窗小店"
      title="该店铺当前暂未开放"
      description="主理人可能正在休整或调整服务项目，去看看其他同学的技能小店吧。"
      action-text="返回学生店铺"
      @action="router.push('/shops')"
    />

    <template v-if="shop">
      <el-card shadow="never" class="shop-hero">
        <div class="catalog-detail-header shop-detail-header">
          <div class="catalog-detail-media">
            <CatalogPhotos kind="SHOP" :object-id="shop.id" layout="detail" :fallback-char="shop.name.slice(0, 1)" :tone="shop.id % 4" />
          </div>
          <div class="catalog-detail-info">
            <el-tag size="small" :type="statusTone(shop.status)">{{ shop.status === 'APPROVED' ? '营业接单中' : formatStatus(shop.status) }}</el-tag>
            <h3 :title="shop.name">{{ shop.name }}</h3>
            <p>{{ shop.description }}</p>
            <p>主理人 {{ shop.ownerNickname }} · 信用 {{ shop.ownerCreditScore }} · {{ shop.rating == null ? '暂无评分' : Number(shop.rating).toFixed(1) + ' 分' }}</p>
            <p>{{ shop.serviceArea }} · {{ zoneLabel(shop.campusZone) }} · {{ shop.openingHours || '服务时间可私聊商议' }}</p>
          </div>
        </div>
      </el-card>

      <el-alert
        :title="
          shop.contactVisible
            ? `主理人联系方式：${shop.contactSnapshot}`
            : '选择下方服务项目并提交预约后，即可直接查看主理人的微信或 QQ，方便双方敲定细节。'
        "
        :type="shop.contactVisible ? 'success' : 'info'"
        show-icon
        :closable="false"
      />

      <div class="shop-service-grid">
        <article v-for="item in shop.serviceItems" :key="item.id" class="service-card spotlight-surface"
          @click="router.push(`/shops/${shop.id}/items/${item.id}`)">
          <div class="catalog-slot">
            <CatalogPhotos kind="SERVICE_ITEM" :object-id="item.id" compact :fallback-char="item.title.slice(0, 1)"
              :fallback-sub="categoryLabel(item.category)" :tone="item.id % 4" />
          </div>
          <div class="service-card-copy">
            <h3 class="catalog-title"><router-link :to="`/shops/${shop.id}/items/${item.id}`" @click.stop>{{ item.title }}</router-link></h3>
            <p class="catalog-summary">{{ item.description }}</p>
            <div class="catalog-meta">{{ categoryLabel(item.category) }} · 约 {{ item.durationMinutes }} 分钟 · {{ item.reviewCount ? `${Number(item.rating).toFixed(1)} 分（${item.reviewCount}）` : '暂无评价' }}</div>
            <div class="service-card-actions">
              <span class="catalog-meta"><strong>{{ priceLabel(item) }}</strong> / {{ item.priceUnit }}</span>
              <el-button type="primary" size="small" :disabled="shop.ownerId === auth.currentUser?.id" @click.stop="openBooking(item)">预约</el-button>
            </div>
          </div>
        </article>
        <el-empty v-if="shop.serviceItems.length === 0" description="主理人正在整理服务价目表，请稍后再来" />
      </div>
      <ContentReport v-if="shop.status==='APPROVED' && auth.currentUser?.id!==shop.ownerId" kind="SHOP" :id="shop.id" />
    </template>

    <el-dialog v-model="bookingOpen" append-to-body title="发起服务预约" width="460px">
      <el-form label-position="top" :model="bookingForm">
        <el-form-item label="期望预约时间">
          <el-date-picker v-model="appointmentDate" type="datetime" class="wide" popper-class="responsive-date-popper" />
        </el-form-item>
        <el-form-item label="心理预算 / 约定金额（元）">
          <el-input-number v-model="bookingForm.amount" :min="0" :precision="2" class="wide" />
        </el-form-item>
        <el-form-item label="需求备注">
          <el-input v-model="bookingForm.note" type="textarea" :rows="3" placeholder="说明具体需求（如电脑型号、期望拍摄风格）及方便联系的时段" />
        </el-form-item>
      </el-form>
      <p class="hint">提交预约后系统将向你展示主理人联系方式，并向对方发送站内通知；具体费用在服务完成后由双方直接结算。</p>
      <template #footer>
        <el-button @click="bookingOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBooking">确认发送预约</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import CatalogPhotos from '@/components/CatalogPhotos.vue'
import ContentReport from '@/components/ContentReport.vue'
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
import { formatStatus, statusTone } from '@/utils/display'
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
    const book=Number(route.query.book);const item=shop.value.serviceItems.find(s=>s.id===book && s.status==='PUBLISHED');
    if(item && auth.currentUser && auth.currentUser.id!==shop.value.ownerId){openBooking(item);void router.replace({path:route.path})}
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
      appointmentTime: new Date(appointmentDate.value.getTime()-appointmentDate.value.getTimezoneOffset()*60000).toISOString().slice(0,19),
      amount: bookingForm.amount,
      note: bookingForm.note,
    })
    ElMessage.success('预约单已提交，主理人联系方式已为你解锁')
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
    PHOTO: '摄影约拍',
    FILM: '数码贴膜',
    REPAIR: '电脑清灰与维修',
    MAKEUP: '妆造造型',
    TUTORING: '学业答疑',
    DESIGN: '视觉与排版',
    OTHER: '特色服务',
  }
  return labels[value] ?? value
}

function zoneLabel(value: CampusZone) {
  return zones.find((zone) => zone.value === value)?.label ?? value
}

onMounted(loadShop)
</script>
