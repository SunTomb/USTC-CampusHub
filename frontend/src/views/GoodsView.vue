<template>
  <section class="page-stack">
    <div class="page-heading marketplace-heading">
      <div>
        <p class="eyebrow">闲置好物 · 校友流转市集</p>
        <h2>校园二手市集</h2>
        <p>课本教材、数码外设、代步单车与宿舍好物——先看卖家校内信用，提交意向后再解锁微信号或 QQ。</p>
      </div>
      <div class="heading-actions">
        <el-button @click="loadGoods" :loading="loading">刷新市集</el-button>
        <el-button type="primary" @click="goPublish">{{ auth.canAccessIdentity('goodsPublisher') ? '上架闲置好物' : '阅读发布协议' }}</el-button>
      </div>
    </div>

    <EmptyState
      v-if="!loading && goods.length === 0"
      eyebrow="二手市集"
      title="市集里暂时还没有上架的商品"
      description="稍后再来逛逛，或者阅读闲置发布与交易规范后分享身边的闲置好物。"
      :action-text="auth.canAccessIdentity('goodsPublisher') ? '上架第一件闲置' : '阅读发布协议'"
      @action="goPublish"
    />

    <div v-else class="goods-grid" v-loading="loading">
      <article
        v-for="item in goods"
        :key="item.id"
        class="goods-card spotlight-surface"
        @click="router.push(`/goods/${item.id}`)"
      >
        <div class="goods-cover" :class="`cover-tone-${item.id % 4}`">
          <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" />
          <div v-else class="goods-cover-art">
            <span class="cover-art-zone">{{ formatCampusZone(item.campusZone) }}</span>
            <strong class="cover-art-glyph">{{ item.title.slice(0, 1) || '物' }}</strong>
            <small class="cover-art-sub">{{ item.conditionLevel || '校友闲置' }}</small>
          </div>
          <span class="goods-zone-floating">{{ formatCampusZone(item.campusZone) }}</span>
        </div>

        <div class="goods-card-body">
          <div class="goods-title-row">
            <h3>{{ item.title }}</h3>
          </div>
          <p class="goods-description">{{ item.description }}</p>

          <div class="goods-price-row">
            <strong class="mono-num">¥{{ item.price }}</strong>
            <span v-if="item.originalPrice" class="goods-origin-price mono-num">原价 ¥{{ item.originalPrice }}</span>
          </div>

          <div class="goods-meta">
            <span class="meta-pill">{{ item.conditionLevel }}</span>
            <span>📍 {{ item.tradeLocation }}</span>
          </div>

          <div class="goods-seller-line">
            <span class="seller-name">{{ item.sellerNickname }}</span>
            <span class="seller-credit mono-num">信用 {{ item.sellerCreditScore }}</span>
            <span class="mono-num">{{ item.viewCount }} 次浏览</span>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listGoods, type GoodsSummary } from '@/api/campushub'
import { formatCampusZone } from '@/utils/display'
import EmptyState from '@/components/common/EmptyState.vue'

const auth = useAuthStore()
const router = useRouter()
const goods = ref<GoodsSummary[]>([])
const loading = ref(false)

function goPublish() {
  router.push(!auth.isAuthenticated ? '/auth?redirect=/roles' : auth.canAccessIdentity('goodsPublisher') ? '/goods/publish' : '/roles')
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
