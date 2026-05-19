<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Skill Shops</p>
        <h2>学生店铺</h2>
        <p>展示同学技能服务、服务范围和评分，用于后续预约下单。</p>
      </div>
      <el-button :loading="loading" @click="loadShops">刷新</el-button>
    </div>

    <div class="shop-grid" v-loading="loading">
      <article v-for="shop in shops" :key="shop.id" class="plain-panel">
        <div class="panel-topline">
          <span>{{ shop.serviceArea }}</span>
          <el-rate :model-value="Number(shop.rating)" disabled size="small" />
        </div>
        <h3>{{ shop.name }}</h3>
        <p>{{ shop.description }}</p>
        <small>店主：{{ shop.ownerNickname }}</small>
      </article>
      <el-empty v-if="!loading && shops.length === 0" description="暂无店铺" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listShops, type ShopSummary } from '@/api/campushub'

const shops = ref<ShopSummary[]>([])
const loading = ref(false)

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

onMounted(loadShops)
</script>
