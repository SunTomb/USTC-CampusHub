<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Tasks</p>
        <h2>悬赏任务</h2>
        <p>快递代取、食堂代取等校园跑腿任务的奖励、押金和截止时间。</p>
      </div>
      <el-button :loading="loading" @click="loadTasks">刷新</el-button>
    </div>

    <div class="list-surface" v-loading="loading">
      <article v-for="task in tasks" :key="task.id" class="list-row">
        <div>
          <h3>{{ task.title }}</h3>
          <p>{{ task.description }}</p>
          <span>{{ task.taskLocation }} · 发布者 {{ task.publisherNickname }}</span>
        </div>
        <div class="metric-column">
          <strong>¥{{ task.rewardAmount }}</strong>
          <span>押金 ¥{{ task.depositAmount }}</span>
          <small>{{ formatDate(task.deadline) }} 前</small>
        </div>
      </article>
      <el-empty v-if="!loading && tasks.length === 0" description="暂无任务" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listTasks, type RewardTaskSummary } from '@/api/campushub'

const tasks = ref<RewardTaskSummary[]>([])
const loading = ref(false)

async function loadTasks() {
  loading.value = true
  try {
    tasks.value = await listTasks()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '任务加载失败')
  } finally {
    loading.value = false
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(loadTasks)
</script>
