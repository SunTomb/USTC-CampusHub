<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">我的书院 · 站内信箱</p>
        <h2>站内通知</h2>
        <p>跑腿接单进展、买家购买意向、小店预约提醒与身份核验通知都会汇集在这里。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新信箱</el-button>
    </div>

    <div class="list-surface premium-panel">
      <EmptyState
        v-if="!loading && notifications.length === 0"
        eyebrow="站内信箱"
        title="信箱里暂时没有新消息"
        description="当有同学接下你的跑腿单、预约你的小店或发送购买意向时，系统会第一时间在这里提醒你。"
        action-text="刷新看看"
        @action="load"
      />
      <div v-for="item in notifications" :key="item.id" class="list-row notification-row">
        <div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.content }}</p>
          <span>{{ formatTargetType(item.targetType) }} · {{ formatTime(item.createdAt) }}</span>
        </div>
        <div class="metric-column">
          <el-tag :type="item.readAt ? 'info' : 'primary'">{{ item.readAt ? '已读' : '新提醒' }}</el-tag>
          <el-button v-if="!item.readAt" size="small" plain @click="markRead(item.id)">标为已读</el-button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listNotifications, markNotificationRead, type StationNotificationSummary } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import { formatTargetType } from '@/utils/display'
import EmptyState from '@/components/common/EmptyState.vue'

const auth = useAuthStore()
const loading = ref(false)
const notifications = ref<StationNotificationSummary[]>([])

async function load() {
  loading.value = true
  try {
    if (!auth.currentUser?.id) {
      notifications.value = []
      return
    }
    notifications.value = await listNotifications(auth.currentUser.id)
  } finally {
    loading.value = false
  }
}

async function markRead(id: number) {
  await markNotificationRead(id)
  ElMessage.success('已标为已读')
  await load()
}

function formatTime(value: string) {
  return new Date(value).toLocaleString('zh-CN')
}

onMounted(load)
</script>
