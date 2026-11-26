<template>
  <el-dialog :model-value="!!src" title="照片预览" append-to-body align-center
    width="min(760px, calc(100vw - 32px))" @update:model-value="(open: boolean) => !open && emit('close')">
    <img v-if="src" class="task-full-photo" :src="src" :alt="alt" />
  </el-dialog>
</template>

<script setup lang="ts">
defineProps<{ src: string; alt: string }>()
const emit = defineEmits<{ close: [] }>()
</script>

<style scoped>
.task-full-photo { display: block; margin: auto; max-width: 100%; max-height: 75vh; object-fit: contain; }
</style>
