<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Projects</p>
        <h2>项目广告</h2>
        <p>学生项目、社团招募和作品展示入口，方便联系与曝光。</p>
      </div>
      <el-button :loading="loading" @click="loadProjects">刷新</el-button>
    </div>

    <el-timeline v-loading="loading" class="timeline-surface">
      <el-timeline-item v-for="project in projects" :key="project.id" :timestamp="formatDate(project.createdAt)" placement="top">
        <div class="timeline-item">
          <div>
            <h3>{{ project.title }}</h3>
            <p>{{ project.description }}</p>
            <span>发布者 {{ project.publisherNickname }} · 浏览 {{ project.viewCount }}</span>
          </div>
          <el-link v-if="project.linkUrl" :href="project.linkUrl" target="_blank" type="primary">查看链接</el-link>
        </div>
      </el-timeline-item>
    </el-timeline>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listProjectAds, type ProjectAdSummary } from '@/api/campushub'

const projects = ref<ProjectAdSummary[]>([])
const loading = ref(false)

async function loadProjects() {
  loading.value = true
  try {
    projects.value = await listProjectAds()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '项目加载失败')
  } finally {
    loading.value = false
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString('zh-CN')
}

onMounted(loadProjects)
</script>
