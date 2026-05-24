<template>
  <article class="capability-card" :class="[`capability-card-${tone}`, { 'capability-card-locked': locked }]">
    <div class="capability-icon">{{ icon }}</div>
    <div class="capability-content">
      <div class="panel-topline">
        <strong>{{ title }}</strong>
        <el-tag v-if="badge" :type="locked ? 'warning' : 'success'">{{ badge }}</el-tag>
      </div>
      <p>{{ description }}</p>
      <div v-if="$slots.meta" class="capability-meta">
        <slot name="meta" />
      </div>
      <div v-if="$slots.actions" class="card-actions">
        <slot name="actions" />
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  icon: string
  title: string
  description: string
  badge?: string
  tone?: 'blue' | 'green' | 'orange' | 'dark'
  locked?: boolean
}>(), {
  tone: 'blue',
  badge: '',
  locked: false,
})
</script>
