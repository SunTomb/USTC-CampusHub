<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">创客工作台 · 招募与作品管理</p>
        <h2>我发布的项目展示</h2>
        <p>发起科研比赛组队、个人作品集、实验室纳新或校园活动，经站务核验后在广场展示。</p>
      </div>
      <el-button v-if="auth.currentUser" type="primary" @click="openCreate">发起新项目 / 招募</el-button>
    </div>

    <LockedState
      v-if="!auth.currentUser"
      title="登录后即可发布项目与招募"
      description="使用科大邮箱登录后，可免费发布大创组队、竞赛招募、作品集与社团活动。"
      primary-text="去登录 / 注册"
      primary-to="/auth"
      secondary-text="返回项目广场"
      secondary-to="/project-ads"
    />

    <div v-else class="mobile-table-wrapper premium-panel">
      <el-table v-loading="loading" :data="projects" stripe>
        <el-table-column prop="title" label="项目标题" min-width="180" />
        <el-table-column prop="adType" label="所属栏目" width="150">
          <template #default="{ row }">{{ formatAdType(row.adType) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="展示状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTone(row.status)" size="small">{{ formatStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="featured" label="精选推荐" width="100">
          <template #default="{ row }">{{ row.featured ? '精选' : '常规' }}</template>
        </el-table-column>
        <el-table-column prop="expiresAt" label="有效截止" width="150">
          <template #default="{ row }">{{ row.expiresAt ? formatDate(row.expiresAt) : '长期有效' }}</template>
        </el-table-column>
        <el-table-column label="管理操作" width="280">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">修改</el-button>
            <el-button size="small" type="primary" plain @click="submit(row)">提交审核</el-button>
            <el-button size="small" type="danger" plain @click="close(row)">结束下架</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑项目展示' : '发起新项目 / 招募'" width="720px">
      <el-form :model="form" label-position="top" class="project-form">
        <FormSection title="核心信息" description="清晰的标题、一句话摘要与技术/方向标签，能吸引志同道合的同学。">
          <el-form-item label="所属栏目">
            <el-select v-model="form.adType" class="wide">
              <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="项目标题"><el-input v-model="form.title" placeholder="如：2026 国创 AI 医疗影像组招募前端与算法队友" /></el-form-item>
          <el-form-item label="一句话简介"><el-input v-model="form.summary" placeholder="显示在列表卡片上的核心亮点" /></el-form-item>
          <el-form-item label="关键词标签"><el-input v-model="form.tags" placeholder="用英文逗号分隔，如 PyTorch,Vue3,大创,西区" /></el-form-item>
        </FormSection>

        <FormSection title="校区、联络规则与有效期" description="你可以自由控制联络方式的可见范围。">
          <el-form-item label="主要活动校区">
            <el-select v-model="form.campusZone" class="wide">
              <el-option label="东校区" value="EAST" />
              <el-option label="西校区" value="WEST" />
              <el-option label="中校区" value="CENTRAL" />
              <el-option label="高新校区" value="HIGH_TECH" />
              <el-option label="先研院 / 科学岛 / 全校" value="OTHER" />
            </el-select>
          </el-form-item>
          <el-form-item label="联络方式（微信 / QQ / 邮箱）"><el-input v-model="form.contactInfo" /></el-form-item>
          <el-form-item label="联络方式展示范围">
            <el-select v-model="form.contactVisibility" class="wide">
              <el-option label="所有人直接可见" value="PUBLIC" />
              <el-option label="仅登录校友可见" value="LOGIN_ONLY" />
              <el-option label="收藏或留言互动后可见" value="INTERACTION_ONLY" />
              <el-option label="暂不公开（仅通过留言沟通）" value="HIDDEN" />
            </el-select>
          </el-form-item>
          <el-form-item label="招募截止时间（选填）"><el-date-picker v-model="form.expiresAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" class="wide" /></el-form-item>
        </FormSection>

        <FormSection title="详细介绍与外部链接" description="详细说明背景目标、分工需求或附上 GitHub / 飞书文档链接。">
          <el-form-item label="详细说明"><el-input v-model="form.description" type="textarea" :rows="5" /></el-form-item>
          <el-form-item label="外部链接（选填）"><el-input v-model="form.linkUrl" placeholder="https://..." /></el-form-item>
        </FormSection>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存项目</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  closeProjectAd,
  createProjectAd,
  listUserProjectAds,
  submitProjectAd,
  updateProjectAd,
  type ProjectAdPayload,
  type ProjectAdSummary,
  type ProjectAdType,
} from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import { formatAdType, formatStatus, statusTone } from '@/utils/display'
import LockedState from '@/components/common/LockedState.vue'
import FormSection from '@/components/common/FormSection.vue'

const auth = useAuthStore()
const projects = ref<ProjectAdSummary[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const typeOptions: Array<{ label: string; value: ProjectAdType }> = [
  { label: '科研/竞赛组队', value: 'TEAM_UP' },
  { label: '个人作品集', value: 'PORTFOLIO' },
  { label: '社团与实验室纳新', value: 'CLUB_RECRUITMENT' },
  { label: '校园活动', value: 'CAMPUS_EVENT' },
  { label: '其他展示', value: 'OTHER' },
]

const form = reactive<ProjectAdPayload>({
  title: '',
  adType: 'TEAM_UP',
  summary: '',
  description: '',
  tags: '',
  campusZone: 'EAST',
  linkUrl: '',
  contactInfo: '',
  contactVisibility: 'LOGIN_ONLY',
  expiresAt: '',
})

async function loadProjects() {
  if (!auth.currentUser) return
  loading.value = true
  try {
    projects.value = await listUserProjectAds(auth.currentUser.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '项目广告加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    title: '',
    adType: 'TEAM_UP',
    summary: '',
    description: '',
    tags: '',
    campusZone: 'EAST',
    linkUrl: '',
    contactInfo: auth.currentUser?.wechatContact || auth.currentUser?.qqContact || '',
    contactVisibility: 'LOGIN_ONLY',
    expiresAt: '',
  })
  dialogVisible.value = true
}

function openEdit(project: ProjectAdSummary) {
  editingId.value = project.id
  Object.assign(form, {
    title: project.title,
    adType: project.adType,
    summary: project.summary || '',
    description: project.description,
    tags: project.tags || '',
    campusZone: project.campusZone || 'EAST',
    linkUrl: project.linkUrl || '',
    contactInfo: project.contactInfo || auth.currentUser?.wechatContact || auth.currentUser?.qqContact || '',
    contactVisibility: project.contactVisibility || 'LOGIN_ONLY',
    expiresAt: project.expiresAt || '',
  })
  dialogVisible.value = true
}

async function save() {
  if (!auth.currentUser) return
  if (!form.title.trim() || !form.description.trim() || !form.contactInfo.trim()) {
    ElMessage.error('请填写标题、详细说明与联络方式')
    return
  }
  const payload = { ...form, expiresAt: form.expiresAt || null }
  if (editingId.value) {
    await updateProjectAd(editingId.value, auth.currentUser.id, payload)
  } else {
    await createProjectAd(auth.currentUser.id, payload)
  }
  dialogVisible.value = false
  ElMessage.success('已保存，提交审核通过后即可在广场展示')
  await loadProjects()
}

async function submit(project: ProjectAdSummary) {
  if (!auth.currentUser) return
  await submitProjectAd(project.id, auth.currentUser.id)
  ElMessage.success('已提交审核')
  await loadProjects()
}

async function close(project: ProjectAdSummary) {
  if (!auth.currentUser) return
  await closeProjectAd(project.id, auth.currentUser.id)
  ElMessage.success('已结束展示并下架')
  await loadProjects()
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString('zh-CN')
}

onMounted(loadProjects)
</script>
