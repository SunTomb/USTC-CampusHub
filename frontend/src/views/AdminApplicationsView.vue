<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Master Admin</p>
        <h2>审核管理员申请</h2>
        <p>仅最高级系统管理员可审批交易管理员和展示管理员身份。</p>
      </div>
      <el-button :loading="loading" @click="loadApplications">刷新</el-button>
    </div>

    <EmptyState
      v-if="!loading && applications.length === 0"
      eyebrow="Admin Roles"
      title="暂无待审核管理员申请"
      description="用户在身份解锁页申请交易管理员或展示管理员后，会进入这里等待 master 审批。"
    />

    <div v-else class="responsive-card-grid">
      <article v-for="item in applications" :key="item.id" class="info-card">
        <div class="panel-topline">
          <strong>{{ item.userNickname }} · {{ roleTypeText[item.roleType] ?? item.roleType }}</strong>
          <el-tag type="warning">{{ item.reviewStatus }}</el-tag>
        </div>
        <p>申请人 ID：{{ item.userId }}</p>
        <p>保证金：{{ item.depositStatus === 'NOT_REQUIRED' ? '无需保证金' : `¥${item.depositAmount}` }}</p>
        <p v-if="item.applyNote">申请说明：{{ item.applyNote }}</p>
        <p>申请时间：{{ item.createdAt }}</p>
        <div class="payment-actions inline-card-actions">
          <el-button size="small" type="primary" :loading="actingId === item.id" @click="approve(item.id)">通过</el-button>
          <el-button size="small" :loading="actingId === item.id" @click="reject(item.id)">拒绝</el-button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  approveMasterAdminApplication,
  listMasterAdminApplications,
  rejectMasterAdminApplication,
  type RoleApplicationSummary,
} from '@/api/campushub'
import EmptyState from '@/components/common/EmptyState.vue'

const applications = ref<RoleApplicationSummary[]>([])
const loading = ref(false)
const actingId = ref<number | null>(null)

const roleTypeText: Record<string, string> = {
  TRADE_ADMIN: '交易管理员',
  SHOWCASE_ADMIN: '展示管理员',
}

async function loadApplications() {
  loading.value = true
  try {
    applications.value = await listMasterAdminApplications()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '管理员申请加载失败')
  } finally {
    loading.value = false
  }
}

async function approve(applicationId: number) {
  actingId.value = applicationId
  try {
    await approveMasterAdminApplication(applicationId)
    ElMessage.success('管理员身份已通过')
    await loadApplications()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '审批失败')
  } finally {
    actingId.value = null
  }
}

async function reject(applicationId: number) {
  actingId.value = applicationId
  try {
    await rejectMasterAdminApplication(applicationId)
    ElMessage.success('管理员申请已拒绝')
    await loadApplications()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '拒绝失败')
  } finally {
    actingId.value = null
  }
}

onMounted(loadApplications)
</script>

<style scoped>
.inline-card-actions {
  margin-top: 10px;
  justify-content: flex-start;
}
</style>
