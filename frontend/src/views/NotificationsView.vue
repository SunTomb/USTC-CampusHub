<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Station Messages</p>
        <h2>站内通知</h2>
        <p>接单申请、任务状态、异常处理和身份保证金结果都会沉淀在站内通知。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <div class="list-surface">
      <EmptyState
        v-if="!loading && notifications.length === 0"
        eyebrow="Notifications"
        title="暂时没有站内通知"
        description="任务状态、身份审核、预约处理和治理结果会在这里提醒你。"
        action-text="刷新通知"
        @action="load"
      />
      <div v-for="item in notifications" :key="item.id" class="list-row notification-row">
        <div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.content }}</p>
          <span>{{ item.targetType || '平台通知' }} · {{ formatTime(item.createdAt) }}</span>
        </div>
        <div class="metric-column">
          <el-tag :type="item.readAt ? 'info' : 'primary'">{{ item.readAt ? '已读' : '未读' }}</el-tag>
          <el-button v-if="!item.readAt" size="small" plain @click="markRead(item.id)">标记已读</el-button>
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
import EmptyState from '@/components/common/EmptyState.vue'

const auth = useAuthStore()
const loading = ref(false)
const notifications = ref<StationNotificationSummary[]>([])

async function load() {
  loading.value = true
  try {
    notifications.value = await listNotifications(auth.currentUser?.id ?? 1)
  } finally {
    loading.value = false
  }
}

async function markRead(id: number) {
  await markNotificationRead(id)
  ElMessage.success('已标记为已读')
  await load()
}

function formatTime(value: string) {
  return new Date(value).toLocaleString()
}

onMounted(load)
</script>
