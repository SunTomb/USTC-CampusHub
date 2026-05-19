<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Marketplace</p>
        <h2>二手商品</h2>
        <p>查看在售商品、价格、交易地点和浏览热度。</p>
      </div>
      <el-button :loading="loading" @click="loadGoods">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="goods" stripe class="data-table">
      <el-table-column prop="title" label="商品" min-width="180" />
      <el-table-column prop="sellerNickname" label="卖家" width="120" />
      <el-table-column prop="price" label="价格" width="120">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="conditionLevel" label="成色" width="100" />
      <el-table-column prop="tradeLocation" label="交易地点" min-width="150" />
      <el-table-column prop="viewCount" label="浏览" width="90" />
      <el-table-column type="expand">
        <template #default="{ row }">
          <p class="row-detail">{{ row.description }}</p>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listGoods, type GoodsSummary } from '@/api/campushub'

const goods = ref<GoodsSummary[]>([])
const loading = ref(false)

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
