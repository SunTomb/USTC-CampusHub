<template>
  <section class="page-stack">
    <div class="page-heading admin-page-heading">
      <div>
        <p class="eyebrow">站务中枢 · 共治成员审批</p>
        <h2>审核管理员申请</h2>
        <p>由总站务审批同学提交的「交易共治管理员」与「展示共治管理员」申请。</p>
      </div>
      <el-button :loading="loading" @click="loadApplications">刷新申请</el-button>
    </div>

    <EmptyState
      v-if="!loading && applications.length === 0"
      eyebrow="共治审批"
      title="当前没有待审批的管理员申请"
      description="当有同学在身份解锁中心申请成为交易管理员或展示管理员时，会在此处等待审批。"
    />

    <div v-else class="responsive-card-grid">
      <article v-for="item in applications" :key="item.id" class="info-card">
        <div class="panel-topline">
          <strong>{{ item.userNickname }} · {{ formatRoleType(item.roleType) }}</strong>
          <el-tag :type="statusTone(item.reviewStatus)" size="small">{{ formatStatus(item.reviewStatus) }}</el-tag>
        </div>
        <p>申请人编号：<span class="mono-num">#{{ item.userId }}</span></p>
        <p>保证金要求：{{ item.depositStatus === 'NOT_REQUIRED' ? '免保证金' : `¥${item.depositAmount}` }}</p>
        <p v-if="item.applyNote">自荐说明：{{ item.applyNote }}</p>
        <p>提交时间：{{ item.createdAt }}</p>
        <div class="payment-actions inline-card-actions">
          <el-button size="small" type="primary" :loading="actingId === item.id" @click="approve(item.id)">准予开通</el-button>
          <el-button size="small" type="danger" plain :loading="actingId === item.id" @click="reject(item.id)">婉拒申请</el-button>
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
import { formatRoleType, formatStatus, statusTone } from '@/utils/display'
import EmptyState from '@/components/common/EmptyState.vue'

const applications = ref<RoleApplicationSummary[]>([])
const loading = ref(false)
const actingId = ref<number | null>(null)

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
    ElMessage.success('已准予开通管理员身份')
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
    ElMessage.success('已婉拒该申请')
    await loadApplications()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
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
