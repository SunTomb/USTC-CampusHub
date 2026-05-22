<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Skill Shops</p>
        <h2>学生店铺</h2>
        <p>浏览校园技能服务，预约后可查看商家联系方式，平台不托管服务本金。</p>
      </div>
      <div class="heading-actions">
        <el-button @click="loadShops" :loading="loading">刷新</el-button>
        <el-button type="primary" @click="goMerchant">开店/商家工作台</el-button>
      </div>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters">
        <el-form-item label="校区">
          <el-select v-model="filters.campusZone" clearable placeholder="全部校区" style="width: 160px">
            <el-option v-for="zone in zones" :key="zone.value" :label="zone.label" :value="zone.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" clearable placeholder="店铺/服务范围" style="width: 220px" />
        </el-form-item>
      </el-form>
    </el-card>

    <div class="shop-grid" v-loading="loading">
      <article v-for="shop in filteredShops" :key="shop.id" class="shop-card" @click="router.push(`/shops/${shop.id}`)">
        <div class="panel-topline">
          <el-tag>{{ zoneLabel(shop.campusZone) }}</el-tag>
          <el-rate :model-value="Number(shop.rating)" disabled size="small" />
        </div>
        <h3>{{ shop.name }}</h3>
        <p>{{ shop.description }}</p>
        <div class="shop-meta">
          <span>{{ shop.serviceArea }}</span>
          <span>店主 {{ shop.ownerNickname }}</span>
          <span>{{ shop.openingHours || '服务时间面议' }}</span>
        </div>
        <el-button text type="primary">查看服务与预约</el-button>
      </article>
      <EmptyState
        v-if="!loading && filteredShops.length === 0"
        eyebrow="Student Shops"
        title="暂时没有符合条件的学生店铺"
        description="可以调整校区、关键词或服务范围筛选，也可以申请店铺商家身份后创建自己的服务店铺。"
        action-text="进入商家工作台"
        @action="goMerchant"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
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

const filteredShops = computed(() => shops.value.filter((shop) => {
  if (filters.campusZone && shop.campusZone !== filters.campusZone) return false
  const keyword = filters.keyword.trim().toLowerCase()
  if (keyword && !`${shop.name} ${shop.description} ${shop.serviceArea}`.toLowerCase().includes(keyword)) return false
  return true
}))

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
    ElMessage.warning('请先登录后再开店或管理店铺')
    router.push({ name: 'auth' })
    return
  }
  router.push({ name: 'shop-merchant' })
}

function zoneLabel(value: CampusZone) {
  return zones.find((zone) => zone.value === value)?.label ?? value
}

onMounted(loadShops)
</script>
