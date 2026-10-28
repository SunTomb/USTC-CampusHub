<template>
  <section class="page-stack" v-loading="loading">
    <div class="page-heading">
      <div>
        <p class="eyebrow">项目广场 · 详情</p>
        <h2>{{ project?.title || '项目详情' }}</h2>
      </div>
      <el-button @click="router.push({ name: 'project-ads' })">返回项目广场</el-button>
    </div>

    <EmptyState
      v-if="!loading && !project"
      eyebrow="项目详情"
      title="该项目当前暂不可见"
      description="项目可能已截止、由发起人关闭或正在审核中，去广场看看其他招募吧。"
      action-text="返回项目广场"
      @action="router.push('/project-ads')"
    />

    <el-card v-if="project" class="detail-hero premium-panel" shadow="never">
      <div class="detail-header">
        <div>
          <p class="eyebrow">{{ formatAdType(project.adType) }}</p>
          <h2>{{ project.title }}</h2>
          <p>{{ project.summary }}</p>
          <div class="tag-list">
            <el-tag v-if="project.featured" type="warning">站务精选</el-tag>
            <el-tag v-for="tag in splitTags(project.tags)" :key="tag" effect="plain">{{ tag }}</el-tag>
          </div>
        </div>
        <div class="detail-stats mono-num">
          <span>浏览 {{ project.viewCount }}</span>
          <span>收藏 {{ project.favoriteCount }}</span>
          <span>留言 {{ project.commentCount }}</span>
        </div>
      </div>
    </el-card>

    <div v-if="project" class="detail-layout">
      <el-card shadow="never" class="premium-panel">
        <template #header>详细介绍与招募要求</template>
        <p class="preserve-lines">{{ project.description }}</p>
        <el-link v-if="project.linkUrl" :href="project.linkUrl" target="_blank" type="primary">访问项目主页 / 代码仓库 →</el-link>
      </el-card>

      <el-card shadow="never" class="premium-panel">
        <template #header>联络发起人</template>
        <el-alert v-if="project.contactVisible" type="success" :closable="false" title="发起人联系方式">
          <p>{{ project.contactInfo }}</p>
        </el-alert>
        <el-alert v-else type="info" :closable="false" title="联系方式解锁说明">
          <p>{{ contactHint }}</p>
        </el-alert>
        <div class="action-row" style="margin-top: 14px">
          <el-button type="primary" :disabled="!auth.currentUser" @click="favoriteProject">收藏关注</el-button>
          <el-button :disabled="!auth.currentUser" @click="reportVisible = true">反馈问题</el-button>
        </div>
      </el-card>
    </div>

    <el-card v-if="project" shadow="never" class="premium-panel">
      <template #header>相关文档与附件</template>
      <el-empty v-if="project.attachments.length === 0" description="未附带额外文件" />
      <div v-else class="attachment-grid">
        <div v-for="binding in project.attachments" :key="binding.id" class="attachment-item">
          <span>{{ binding.file.originalName }}</span>
          <small>{{ binding.file.contentType }}</small>
        </div>
      </div>
    </el-card>

    <el-card v-if="project" shadow="never" class="premium-panel">
      <template #header>同窗交流与自荐</template>
      <div class="comment-box">
        <el-input v-model="commentContent" type="textarea" :rows="3" placeholder="介绍一下你的专业背景、技术栈或感兴趣的方向..." />
        <el-button type="primary" :disabled="!auth.currentUser || !commentContent.trim()" @click="submitComment">发送留言</el-button>
      </div>
    </el-card>

    <el-dialog v-model="reportVisible" title="反馈项目问题" width="420px">
      <el-input v-model="reportForm.reason" placeholder="反馈原因" />
      <el-input v-model="reportForm.description" class="form-gap" type="textarea" :rows="3" placeholder="补充具体说明" />
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReport">提交反馈</el-button>
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
import { formatAdType } from '@/utils/display'
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
  if (!auth.currentUser) return '登录校园账号后即可按发起人设置查看联络方式。'
  if (project.value?.contactVisibility === 'INTERACTION_ONLY') return '点击「收藏关注」或下方发送留言后，即可查看发起人联系方式。'
  return '发起人暂未公开私人联系方式，欢迎直接在下方留言自荐或访问外部链接。'
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
  ElMessage.success('已收藏该项目')
  await loadProject()
}

async function submitComment() {
  if (!auth.currentUser || !project.value) return
  await commentTarget(auth.currentUser.id, { targetType: 'PROJECT_AD', targetId: project.value.id, content: commentContent.value })
  commentContent.value = ''
  ElMessage.success('留言已发送')
  await loadProject()
}

async function submitReport() {
  if (!auth.currentUser || !project.value) return
  await reportTarget(auth.currentUser.id, {
    targetType: 'PROJECT_AD',
    targetId: project.value.id,
    reason: reportForm.reason || '项目展示反馈',
    description: reportForm.description || reportForm.reason,
  })
  reportVisible.value = false
  ElMessage.success('反馈已提交')
}

function splitTags(tags: string | null) {
  return tags?.split(',').map((tag) => tag.trim()).filter(Boolean) ?? []
}

onMounted(loadProject)
</script>
