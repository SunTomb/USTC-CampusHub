<template>
  <section class="page-stack" v-loading="loading">
    <el-button text @click="router.push({ name: 'project-ads' })">返回项目广告</el-button>

    <EmptyState
      v-if="!loading && !project"
      eyebrow="Project Detail"
      title="项目详情暂不可用"
      description="内容可能已过期、下架或正在审核。你可以返回校园展示继续浏览。"
      action-text="返回校园展示"
      @action="router.push('/project-ads')"
    />

    <el-card v-if="project" class="detail-hero" shadow="never">
      <div class="detail-header">
        <div>
          <p class="eyebrow">{{ typeLabel(project.adType) }}</p>
          <h2>{{ project.title }}</h2>
          <p>{{ project.summary }}</p>
          <div class="tag-list">
            <el-tag v-if="project.featured" type="warning">精选</el-tag>
            <el-tag v-for="tag in splitTags(project.tags)" :key="tag" effect="plain">{{ tag }}</el-tag>
          </div>
        </div>
        <div class="detail-stats">
          <span>浏览 {{ project.viewCount }}</span>
          <span>收藏 {{ project.favoriteCount }}</span>
          <span>评论 {{ project.commentCount }}</span>
        </div>
      </div>
    </el-card>

    <div v-if="project" class="detail-layout">
      <el-card shadow="never">
        <template #header>项目说明</template>
        <p class="preserve-lines">{{ project.description }}</p>
        <el-link v-if="project.linkUrl" :href="project.linkUrl" target="_blank" type="primary">查看外部链接</el-link>
      </el-card>

      <el-card shadow="never">
        <template #header>联系与参与</template>
        <el-alert v-if="project.contactVisible" type="success" :closable="false" title="联系方式已开放">
          <p>{{ project.contactInfo }}</p>
        </el-alert>
        <el-alert v-else type="info" :closable="false" title="联系方式暂未开放">
          <p>{{ contactHint }}</p>
        </el-alert>
        <div class="action-row">
          <el-button type="primary" :disabled="!auth.currentUser" @click="favoriteProject">收藏</el-button>
          <el-button :disabled="!auth.currentUser" @click="reportVisible = true">举报</el-button>
        </div>
      </el-card>
    </div>

    <el-card v-if="project" shadow="never">
      <template #header>附件与图片</template>
      <el-empty v-if="project.attachments.length === 0" description="暂无附件" />
      <div v-else class="attachment-grid">
        <div v-for="binding in project.attachments" :key="binding.id" class="attachment-item">
          <span>{{ binding.file.originalName }}</span>
          <small>{{ binding.file.contentType }}</small>
        </div>
      </div>
    </el-card>

    <el-card v-if="project" shadow="never">
      <template #header>评论</template>
      <div class="comment-box">
        <el-input v-model="commentContent" type="textarea" :rows="3" placeholder="留下你的问题或合作意向" />
        <el-button type="primary" :disabled="!auth.currentUser || !commentContent.trim()" @click="submitComment">发表评论</el-button>
      </div>
      <el-alert type="info" :closable="false" title="评论提交后可刷新页面查看，复杂社区讨论留到后续阶段。" />
    </el-card>

    <el-dialog v-model="reportVisible" title="举报项目广告" width="420px">
      <el-input v-model="reportForm.reason" placeholder="举报原因" />
      <el-input v-model="reportForm.description" class="form-gap" type="textarea" :rows="3" placeholder="补充说明" />
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { commentTarget, favoriteTarget, getProjectAd, reportTarget, type ProjectAdDetailSummary } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const project = ref<ProjectAdDetailSummary | null>(null)
const loading = ref(false)
const commentContent = ref('')
const reportVisible = ref(false)
const reportForm = reactive({ reason: '', description: '' })

const contactHint = computed(() => {
  if (!auth.currentUser) return '登录后可按发布者设置查看联系方式。'
  if (project.value?.contactVisibility === 'INTERACTION_ONLY') return '收藏或评论后可查看联系方式。'
  return '发布者选择暂不公开联系方式，请通过评论或外部链接联系。'
})

async function loadProject() {
  loading.value = true
  try {
    project.value = await getProjectAd(Number(route.params.id), auth.currentUser?.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '项目详情加载失败')
  } finally {
    loading.value = false
  }
}

async function favoriteProject() {
  if (!auth.currentUser || !project.value) return
  await favoriteTarget(auth.currentUser.id, { targetType: 'PROJECT_AD', targetId: project.value.id })
  ElMessage.success('已收藏')
  await loadProject()
}

async function submitComment() {
  if (!auth.currentUser || !project.value) return
  await commentTarget(auth.currentUser.id, { targetType: 'PROJECT_AD', targetId: project.value.id, content: commentContent.value })
  commentContent.value = ''
  ElMessage.success('评论已提交')
  await loadProject()
}

async function submitReport() {
  if (!auth.currentUser || !project.value) return
  await reportTarget(auth.currentUser.id, {
    targetType: 'PROJECT_AD',
    targetId: project.value.id,
    reason: reportForm.reason || '项目广告举报',
    description: reportForm.description || reportForm.reason,
  })
  reportVisible.value = false
  ElMessage.success('举报已提交')
}

function splitTags(tags: string | null) {
  return tags?.split(',').map((tag) => tag.trim()).filter(Boolean) ?? []
}

function typeLabel(value: string) {
  const labels: Record<string, string> = {
    TEAM_UP: '项目组队',
    PORTFOLIO: '作品展示',
    CLUB_RECRUITMENT: '社团招募',
    CAMPUS_EVENT: '校园活动',
    OTHER: '其他',
  }
  return labels[value] ?? '项目广告'
}

onMounted(loadProject)
</script>
