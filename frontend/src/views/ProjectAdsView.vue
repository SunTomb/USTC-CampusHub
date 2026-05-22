<template>
  <section class="page-stack project-showcase">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Campus Showcase</p>
        <h2>项目广告与校园展示</h2>
        <p>发现项目组队、作品展示、社团招募和校园活动，审核后公开展示。</p>
      </div>
      <div class="heading-actions">
        <el-button @click="loadProjects" :loading="loading">刷新</el-button>
        <el-button type="primary" @click="goManage">发布 / 管理</el-button>
      </div>
    </div>

    <el-card class="filter-card" shadow="never">
      <div class="filter-grid">
        <el-input v-model="filters.keyword" clearable placeholder="搜索标题、简介或标签" @keyup.enter="loadProjects" />
        <el-select v-model="filters.adType" clearable placeholder="类型">
          <el-option v-for="option in typeOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-select v-model="filters.campusZone" clearable placeholder="校区">
          <el-option v-for="option in campusOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-switch v-model="featuredOnly" active-text="只看精选" />
        <el-button type="primary" @click="loadProjects">筛选</el-button>
      </div>
    </el-card>

    <div v-loading="loading" class="showcase-grid">
      <EmptyState
        v-if="!loading && projects.length === 0"
        eyebrow="Campus Showcase"
        title="暂时没有符合条件的项目广告"
        description="可以调整类型、校区、精选或关键词筛选，也可以提交项目、社团或活动展示。"
        action-text="管理我的项目广告"
        @action="goManage"
      />
      <el-card v-for="project in projects" :key="project.id" class="showcase-card" shadow="hover" @click="goDetail(project.id)">
        <div class="showcase-cover">
          <span>{{ typeLabel(project.adType) }}</span>
          <el-tag v-if="project.featured" type="warning" effect="dark">精选</el-tag>
        </div>
        <div class="showcase-content">
          <div class="card-title-row">
            <h3>{{ project.title }}</h3>
            <el-tag size="small" type="info">{{ zoneLabel(project.campusZone) }}</el-tag>
          </div>
          <p>{{ project.summary || project.description }}</p>
          <div class="tag-list">
            <el-tag v-for="tag in splitTags(project.tags)" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
          </div>
          <div class="meta-line">
            <span>{{ project.publisherNickname }}</span>
            <span>浏览 {{ project.viewCount }}</span>
            <span v-if="project.expiresAt">{{ formatDate(project.expiresAt) }} 过期</span>
          </div>
        </div>
      </el-card>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listProjectAds, type ProjectAdListParams, type ProjectAdSummary, type ProjectAdType } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const auth = useAuthStore()
const projects = ref<ProjectAdSummary[]>([])
const loading = ref(false)
const featuredOnly = ref(false)
const filters = reactive<ProjectAdListParams>({})

const typeOptions: Array<{ label: string; value: ProjectAdType }> = [
  { label: '项目组队', value: 'TEAM_UP' },
  { label: '作品展示', value: 'PORTFOLIO' },
  { label: '社团招募', value: 'CLUB_RECRUITMENT' },
  { label: '校园活动', value: 'CAMPUS_EVENT' },
  { label: '其他', value: 'OTHER' },
]

const campusOptions = [
  { label: '中校区', value: 'CENTRAL' },
  { label: '西校区', value: 'WEST' },
  { label: '东校区', value: 'EAST' },
  { label: '北校区', value: 'NORTH' },
  { label: '南校区', value: 'SOUTH' },
  { label: '高新校区', value: 'HIGH_TECH' },
  { label: '其他', value: 'OTHER' },
]

async function loadProjects() {
  loading.value = true
  try {
    projects.value = await listProjectAds({ ...filters, featured: featuredOnly.value ? true : undefined })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '项目加载失败')
  } finally {
    loading.value = false
  }
}

function goDetail(id: number) {
  router.push({ name: 'project-ad-detail', params: { id } })
}

function goManage() {
  if (!auth.currentUser) {
    ElMessage.warning('请先登录后发布项目广告')
    router.push({ name: 'auth' })
    return
  }
  router.push({ name: 'project-ad-manage' })
}

function splitTags(tags: string | null) {
  return tags?.split(',').map((tag) => tag.trim()).filter(Boolean).slice(0, 4) ?? []
}

function typeLabel(value: string) {
  return typeOptions.find((option) => option.value === value)?.label ?? '项目广告'
}

function zoneLabel(value: string | null) {
  return campusOptions.find((option) => option.value === value)?.label ?? '全校'
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString('zh-CN')
}

onMounted(loadProjects)
</script>
