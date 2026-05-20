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

    <el-empty v-if="!loading && goods.length === 0" description="暂无在售商品" />

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
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listGoods, type GoodsSummary } from '@/api/campushub'

const router = useRouter()
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
