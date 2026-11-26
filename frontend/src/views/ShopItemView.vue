<template>
  <section class="page-stack shop-item-page" v-loading="loading">
    <el-button class="item-back" text @click="router.push('/shops/' + shopId)">← {{ item?.shopName || '返回店铺' }}</el-button>
    <p role="alert" v-if="error">{{ error }} <el-button text @click="load">重试</el-button></p>
    <template v-if="item">
      <el-card shadow="never">
        <header class="catalog-detail-header item-detail-header">
          <div class="catalog-detail-media">
            <CatalogPhotos kind="SERVICE_ITEM" :object-id="item.id" layout="detail" :fallback-char="item.title.slice(0, 1)" :tone="item.id % 4" />
          </div>
          <div class="catalog-detail-info">
            <p class="eyebrow">{{ item.shopName }}</p>
            <h2 :title="item.title">{{ item.title }}</h2>
            <div class="goods-price-row"><strong>¥{{ item.price }}</strong> / {{ item.priceUnit }}</div>
            <p>约 {{ item.durationMinutes }} 分钟</p>
          </div>
        </header>
        <p class="preserve-lines">{{ item.description }}</p>
        <el-button type="primary" v-if="item.status === 'PUBLISHED' && !owner" @click="book">预约这项服务</el-button>
      </el-card>
      <section><h3>顾客评价</h3><ItemRatings :id="item.id" :score="item.rating" :count="item.reviewCount" /></section>
    </template>
  </section>
</template>
<script setup lang="ts">
import {ref,computed,watch,onBeforeUnmount} from 'vue';import {useRoute,useRouter} from 'vue-router';import {getApi} from '@/api/client';import {getShopDetail,type ServiceItemSummary} from '@/api/campushub';import {useAuthStore} from '@/stores/auth';import CatalogPhotos from '@/components/CatalogPhotos.vue';import ItemRatings from '@/components/ItemRatings.vue';const router=useRouter(),route=useRoute(),auth=useAuthStore(),item=ref<ServiceItemSummary|null>(null),owner=ref(false),loading=ref(false),error=ref(''),shopId=computed(()=>Number(route.params.shopId));let seq=0;async function load(){const n=++seq;item.value=null;error.value='';loading.value=true;try{const s=await getApi<ServiceItemSummary>(`/service-items/${Number(route.params.itemId)}`);if(s.shopId!==shopId.value)throw Error('服务不属于该店铺');const sh=await getShopDetail(s.shopId);if(n===seq){item.value=s;owner.value=sh.ownerId===auth.currentUser?.id}}catch(e){if(n===seq)error.value=e instanceof Error?e.message:'商品加载失败'}finally{if(n===seq)loading.value=false}}function book(){const b=`/shops/${shopId.value}?book=${item.value!.id}`;void router.push(auth.currentUser?b:{name:'auth',query:{redirect:b}})}watch([()=>route.params.itemId,shopId,()=>auth.token],load,{immediate:true});onBeforeUnmount(()=>{seq++})
</script>
