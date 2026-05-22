<template>
  <section class="state-card" :class="{ 'state-card-compact': compact }">
    <p v-if="eyebrow" class="state-eyebrow">{{ eyebrow }}</p>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <div v-if="actionText" class="state-actions">
      <el-button :type="actionType" @click="$emit('action')">{{ actionText }}</el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  eyebrow?: string
  title: string
  description: string
  actionText?: string
  actionType?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  compact?: boolean
}>(), {
  actionType: 'primary',
  compact: false
})

defineEmits<{
  action: []
}>()
</script>
