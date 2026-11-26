<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">同窗技艺 · 个人服务小店</p>
        <h2>学生技能店铺</h2>
        <p>校园人像约拍、笔记本清灰硅脂、手机贴膜、海报设计与学科答疑——在线发起预约后即可对接主理人微信或 QQ。</p>
      </div>
      <div class="heading-actions">
        <el-button @click="loadShops" :loading="loading">刷新列表</el-button>
        <el-button type="primary" @click="goMerchant">{{ auth.canAccessIdentity('shopMerchant') ? '主理人工作台' : '阅读协议并申请开店' }}</el-button>
      </div>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters" class="compact-filter-form">
        <el-form-item label="常驻校区">
          <el-select v-model="filters.campusZone" clearable placeholder="全部校区" style="width: 160px">
            <el-option v-for="zone in zones" :key="zone.value" :label="zone.label" :value="zone.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索服务">
          <el-input v-model="filters.keyword" clearable placeholder="搜店铺名、摄影、清灰、排版..." />
        </el-form-item>
      </el-form>
    </el-card>

    <div class="shop-grid" v-loading="loading">
      <article
        v-for="shop in filteredShops"
        :key="shop.id"
        class="shop-card spotlight-surface"
        @click="router.push(`/shops/${shop.id}`)"
      >
        <div class="catalog-slot">
          <CatalogPhotos kind="SHOP" :object-id="shop.id" compact :fallback-char="shop.name.slice(0, 1)" :tone="shop.id % 4" />
        </div>
        <div class="shop-card-copy">
          <div class="catalog-title-row">
            <h3 class="catalog-title">{{ shop.name }}</h3>
            <small class="shop-score">{{ shop.rating == null ? '暂无评分' : Number(shop.rating).toFixed(1) + ' 分' }}</small>
          </div>
          <p class="catalog-summary">{{ shop.description }}</p>
          <div class="catalog-meta">{{ zoneLabel(shop.campusZone) }} · {{ shop.ownerNickname }} · {{ shop.serviceArea }}</div>
          <router-link class="shop-enter-link" :to="`/shops/${shop.id}`" @click.stop>进店查看价目与预约 →</router-link>
        </div>
      </article>

      <EmptyState
        v-if="!loading && filteredShops.length === 0"
        eyebrow="同窗小店"
        title="暂时没有找到匹配的学生店铺"
        description="换个校区或关键词试试，或者阅读技能店主理人协议后提交站务核验。"
        action-text="申请主理人身份"
        @action="goMerchant"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import CatalogPhotos from '@/components/CatalogPhotos.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listShops, type CampusZone, type ShopSummary } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const auth = useAuthStore()
const shops = ref<ShopSummary[]>([])
const loading = ref(false)
const filters = reactive<{ campusZone?: CampusZone; keyword: string }>({ keyword: '' })

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

const filteredShops = computed(() =>
  shops.value.filter((shop) => {
    if (filters.campusZone && shop.campusZone !== filters.campusZone) return false
    const keyword = filters.keyword.trim().toLowerCase()
    if (keyword && !`${shop.name} ${shop.description} ${shop.serviceArea}`.toLowerCase().includes(keyword)) return false
    return true
  }),
)

async function loadShops() {
  loading.value = true
  try {
    shops.value = await listShops()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '店铺加载失败')
  } finally {
    loading.value = false
  }
}

function goMerchant() {
  if (!auth.currentUser) {
    ElMessage.warning('请先登录后再开店或打理店铺')
    router.push({ name: 'auth' })
    return
  }
  router.push({ name: auth.canAccessIdentity('shopMerchant') ? 'shop-merchant' : 'roles' })
}

function zoneLabel(value: CampusZone) {
  return zones.find((zone) => zone.value === value)?.label ?? value
}

onMounted(loadShops)
</script>
