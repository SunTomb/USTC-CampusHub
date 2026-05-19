<template>
  <section class="dashboard-grid">
    <el-card v-for="item in modules" :key="item.title" shadow="hover">
      <h2>{{ item.title }}</h2>
      <p>{{ item.description }}</p>
    </el-card>
  </section>
</template>

<script setup lang="ts">
const modules = [
  { title: '二手商品交易', description: '商品发布、审核、收藏、下单和评价闭环。' },
  { title: '悬赏任务接单', description: '快递代取、食堂代取等校园跑腿任务流程。' },
  { title: '学生技能店铺', description: '摄影、贴膜、电脑维护等技能服务预约。' },
  { title: '内容治理与信用', description: '审核、举报、违规记录和信用分联动。' },
]
</script>
