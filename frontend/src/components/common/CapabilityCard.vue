<template>
  <article
    class="capability-card spotlight-surface"
    :class="[`capability-card-${tone}`, { 'capability-card-locked': locked }]"
    @mousemove="handleMouseMove"
  >
    <div class="capability-top">
      <div class="capability-icon">{{ icon }}</div>
      <span v-if="badge" class="capability-pill" :class="{ 'capability-pill-locked': locked }">{{ badge }}</span>
    </div>
    <div class="capability-content">
      <div class="panel-topline">
        <strong>{{ title }}</strong>
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

function handleMouseMove(event: MouseEvent) {
  const target = event.currentTarget as HTMLElement | null
  if (!target) return
  const rect = target.getBoundingClientRect()
  target.style.setProperty('--mouse-x', `${event.clientX - rect.left}px`)
  target.style.setProperty('--mouse-y', `${event.clientY - rect.top}px`)
}
</script>
