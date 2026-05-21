<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">My Showcase</p>
        <h2>我的项目广告</h2>
        <p>发布项目组队、作品展示、社团招募或校园活动，审核通过后公开展示。</p>
      </div>
      <el-button type="primary" @click="openCreate">新建项目广告</el-button>
    </div>

    <el-alert v-if="!auth.currentUser" type="warning" :closable="false" title="请先登录后管理项目广告" />

    <el-table v-else v-loading="loading" :data="projects" class="table-card">
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="adType" label="类型" width="130" />
      <el-table-column prop="status" label="状态" width="130" />
      <el-table-column prop="featured" label="精选" width="90">
        <template #default="{ row }">{{ row.featured ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="expiresAt" label="过期时间" width="150">
        <template #default="{ row }">{{ row.expiresAt ? formatDate(row.expiresAt) : '长期' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="primary" @click="submit(row)">提交审核</el-button>
          <el-button size="small" type="danger" @click="close(row)">下架</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑项目广告' : '新建项目广告'" width="720px">
      <el-form :model="form" label-width="120px" class="project-form">
        <el-form-item label="类型"><el-select v-model="form.adType"><el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="form.summary" /></el-form-item>
        <el-form-item label="详细说明"><el-input v-model="form.description" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="form.tags" placeholder="用英文逗号分隔，如 Vue,摄影,招募" /></el-form-item>
        <el-form-item label="校区"><el-input v-model="form.campusZone" placeholder="EAST / WEST / CENTRAL / OTHER" /></el-form-item>
        <el-form-item label="外部链接"><el-input v-model="form.linkUrl" /></el-form-item>
        <el-form-item label="联系方式"><el-input v-model="form.contactInfo" /></el-form-item>
        <el-form-item label="展示规则"><el-select v-model="form.contactVisibility"><el-option label="公开" value="PUBLIC" /><el-option label="登录后" value="LOGIN_ONLY" /><el-option label="互动后" value="INTERACTION_ONLY" /><el-option label="隐藏" value="HIDDEN" /></el-select></el-form-item>
        <el-form-item label="过期时间"><el-date-picker v-model="form.expiresAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
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

const auth = useAuthStore()
const projects = ref<ProjectAdSummary[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const typeOptions: Array<{ label: string; value: ProjectAdType }> = [
  { label: '项目组队', value: 'TEAM_UP' },
  { label: '作品展示', value: 'PORTFOLIO' },
  { label: '社团招募', value: 'CLUB_RECRUITMENT' },
  { label: '校园活动', value: 'CAMPUS_EVENT' },
  { label: '其他', value: 'OTHER' },
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
  Object.assign(form, { title: '', adType: 'TEAM_UP', summary: '', description: '', tags: '', campusZone: 'EAST', linkUrl: '', contactInfo: auth.currentUser?.wechatContact || auth.currentUser?.qqContact || '', contactVisibility: 'LOGIN_ONLY', expiresAt: '' })
  dialogVisible.value = true
}

function openEdit(project: ProjectAdSummary) {
  editingId.value = project.id
  Object.assign(form, { title: project.title, adType: project.adType, summary: project.summary || '', description: project.description, tags: project.tags || '', campusZone: project.campusZone || 'EAST', linkUrl: project.linkUrl || '', contactInfo: project.contactInfo || auth.currentUser?.wechatContact || auth.currentUser?.qqContact || '', contactVisibility: project.contactVisibility || 'LOGIN_ONLY', expiresAt: project.expiresAt || '' })
  dialogVisible.value = true
}

async function save() {
  if (!auth.currentUser) return
  if (!form.title.trim() || !form.description.trim() || !form.contactInfo.trim()) {
    ElMessage.error('请填写标题、说明和联系方式')
    return
  }
  const payload = { ...form, expiresAt: form.expiresAt || null }
  if (editingId.value) {
    await updateProjectAd(editingId.value, auth.currentUser.id, payload)
  } else {
    await createProjectAd(auth.currentUser.id, payload)
  }
  dialogVisible.value = false
  ElMessage.success('已保存，等待审核后公开展示')
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
  ElMessage.success('已下架')
  await loadProjects()
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString('zh-CN')
}

onMounted(loadProjects)
</script>
