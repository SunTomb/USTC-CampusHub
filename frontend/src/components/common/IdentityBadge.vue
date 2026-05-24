<template>
  <div class="identity-badges" :class="{ 'identity-badges-compact': compact }">
    <span v-for="identity in identities" :key="identity.key" class="identity-badge" :class="`identity-badge-${identity.key}`">
      {{ compact ? identity.shortLabel : identity.label }}
    </span>
  </div>
</template>

<script setup lang="ts">
import type { IdentityItem } from '@/utils/identity'

withDefaults(defineProps<{
  identities: IdentityItem[]
  compact?: boolean
}>(), {
  compact: false,
})
</script>
