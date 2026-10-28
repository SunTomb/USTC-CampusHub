<template>
  <section class="page-stack" v-loading="loading">
    <div class="page-heading" v-if="shop">
      <div>
        <p class="eyebrow">同窗小店 · 价目与预约</p>
        <h2>{{ shop.name }}</h2>
        <p>{{ shop.serviceArea }} · 常驻{{ zoneLabel(shop.campusZone) }} · {{ shop.openingHours || '服务时间可私聊商议' }}</p>
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
      <el-card shadow="never" class="shop-hero premium-panel">
        <div>
          <el-tag :type="statusTone(shop.status)">{{ shop.status === 'APPROVED' ? '营业接单中' : formatStatus(shop.status) }}</el-tag>
          <h3>{{ shop.description }}</h3>
          <p>主理人：<strong>{{ shop.ownerNickname }}</strong> · 校内信用分 <span class="mono-num">{{ shop.ownerCreditScore }}</span></p>
        </div>
        <div class="metric-column">
          <span>同窗口碑评分</span>
          <strong class="mono-num">{{ shop.rating }}</strong>
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
        <el-card v-for="item in shop.serviceItems" :key="item.id" shadow="never" class="service-card spotlight-surface">
          <div class="panel-topline">
            <el-tag effect="plain">{{ categoryLabel(item.category) }}</el-tag>
            <span class="mono-num">约 {{ item.durationMinutes }} 分钟</span>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.description }}</p>
          <div class="goods-price-row">
            <strong class="mono-num">{{ priceLabel(item) }}</strong>
            <span>/ {{ item.priceUnit }}</span>
          </div>
          <el-button type="primary" @click="openBooking(item)">立即预约这项服务</el-button>
        </el-card>
        <el-empty v-if="shop.serviceItems.length === 0" description="主理人正在整理服务价目表，请稍后再来" />
      </div>
    </template>

    <el-dialog v-model="bookingOpen" title="发起服务预约" width="460px">
      <el-form label-position="top" :model="bookingForm">
        <el-form-item label="期望预约时间">
          <el-date-picker v-model="appointmentDate" type="datetime" class="wide" />
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
