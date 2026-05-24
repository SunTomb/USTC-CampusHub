<template>
  <section class="locked-state">
    <div class="locked-state-icon">{{ icon }}</div>
    <p class="eyebrow">{{ eyebrow }}</p>
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
    <div class="state-actions">
      <el-button v-if="primaryText && primaryTo" type="primary" @click="router.push(primaryTo)">{{ primaryText }}</el-button>
      <el-button v-if="secondaryText && secondaryTo" plain @click="router.push(secondaryTo)">{{ secondaryText }}</el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

withDefaults(defineProps<{
  icon?: string
  eyebrow?: string
  title: string
  description: string
  primaryText?: string
  primaryTo?: string
  secondaryText?: string
  secondaryTo?: string
}>(), {
  icon: 'LOCK',
  eyebrow: 'Capability Locked',
  primaryText: '',
  primaryTo: '',
  secondaryText: '',
  secondaryTo: '',
})

const router = useRouter()
</script>
