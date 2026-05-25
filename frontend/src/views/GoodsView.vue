<template>
  <section class="page-stack">
    <div class="page-heading marketplace-heading">
      <div>
        <p class="eyebrow">Second-hand Marketplace</p>
        <h2>校园二手市场</h2>
        <p>发布闲置、查看卖家信用，提交购买意向后再展示联系方式。</p>
      </div>
      <div class="heading-actions">
        <el-button @click="loadGoods" :loading="loading">刷新</el-button>
        <el-button type="primary" @click="router.push('/goods/publish')">发布商品</el-button>
      </div>
    </div>

    <div class="business-cta-card">
      <div>
        <p class="eyebrow">Recommended Next Step</p>
        <h3>{{ primaryCta.label }}</h3>
        <p>CampusHub 会根据登录状态和已解锁身份展示最适合的下一步入口。</p>
      </div>
      <el-button type="primary" @click="router.push(primaryCta.to)">{{ primaryCta.label }}</el-button>
    </div>

    <EmptyState
      v-if="!loading && goods.length === 0"
      eyebrow="Second-hand"
      title="暂时没有符合条件的二手商品"
      description="可以调整分类、校区或关键词筛选，也可以在完成二手发布者身份后发布商品。"
      action-text="发布二手商品"
      @action="router.push('/goods/publish')"
    />

    <div v-else class="goods-grid" v-loading="loading">
      <article v-for="item in goods" :key="item.id" class="goods-card" @click="router.push(`/goods/${item.id}`)">
        <div class="goods-cover">
          <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" />
          <span v-else>CampusHub</span>
        </div>
        <div class="goods-card-body">
          <div class="goods-title-row">
            <h3>{{ item.title }}</h3>
            <el-tag size="small" effect="plain">{{ zoneLabel(item.campusZone) }}</el-tag>
          </div>
          <p class="goods-description">{{ item.description }}</p>
          <div class="goods-price-row">
            <strong>¥{{ item.price }}</strong>
            <span v-if="item.originalPrice">原价 ¥{{ item.originalPrice }}</span>
          </div>
          <div class="goods-meta">
            <span>{{ item.conditionLevel }}</span>
            <span>{{ item.tradeLocation }}</span>
          </div>
          <div class="goods-seller-line">
            <span>{{ item.sellerNickname }}</span>
            <span>信用 {{ item.sellerCreditScore }}</span>
            <span>浏览 {{ item.viewCount }}</span>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listGoods, type GoodsSummary } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const auth = useAuthStore()
const goods = ref<GoodsSummary[]>([])
const loading = ref(false)

const zoneMap: Record<string, string> = {
  CENTRAL: '中校区',
  WEST: '西校区',
  EAST: '东校区',
  NORTH: '北校区',
  SOUTH: '南校区',
  HIGH_TECH: '高新校区',
  ADVANCED_RESEARCH_INSTITUTE: '先研院',
  SCIENCE_ISLAND: '科学岛',
  OTHER: '其他',
}

const primaryCta = computed(() => {
  if (!auth.currentUser) {
    return { label: '登录后继续', to: '/auth' }
  }
  if (auth.isAdmin) {
    return { label: '进入运营后台', to: '/admin/ops' }
  }
  if (auth.canAccessIdentity('goodsPublisher')) {
    return { label: '发布二手', to: '/goods/publish' }
  }
  return { label: '申请二手发布者', to: '/roles' }
})

function zoneLabel(zone: string) {
  return zoneMap[zone] ?? zone
}

async function loadGoods() {
  loading.value = true
  try {
    goods.value = await listGoods()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '商品加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadGoods)
</script>
