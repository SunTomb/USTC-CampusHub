<template>
  <section class="page-stack">
    <div class="page-heading admin-page-heading">
      <div>
        <p class="eyebrow">站务中枢 · 审核与反馈归档</p>
        <h2>内容核验与反馈记录</h2>
        <p>并列查阅历史内容核验结果与同学提交的反馈档案。</p>
      </div>
      <el-button :loading="loading" @click="loadModeration">刷新记录</el-button>
    </div>

    <el-tabs class="tabs-surface premium-panel">
      <el-tab-pane label="内容核验档案">
        <EmptyState v-if="!loading && reviews.length === 0" title="暂无核验记录" description="站务核验结果会在此归档。" compact />
        <div v-else class="mobile-table-wrapper">
          <el-table v-loading="loading" :data="reviews" stripe>
            <el-table-column prop="targetType" label="所属板块" width="130">
              <template #default="{ row }">{{ formatTargetType(row.targetType) }}</template>
            </el-table-column>
            <el-table-column prop="targetId" label="编号" width="90">
              <template #default="{ row }"><span class="mono-num">#{{ row.targetId }}</span></template>
            </el-table-column>
            <el-table-column prop="result" label="结论" width="130">
              <template #default="{ row }">
                <el-tag :type="statusTone(row.result)" size="small">{{ formatStatus(row.result) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reviewerNickname" label="核验站务" width="140" />
            <el-table-column prop="reason" label="核验说明" min-width="220" />
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="同学反馈档案">
        <EmptyState v-if="!loading && reports.length === 0" title="暂无反馈记录" description="同学提交的反馈与处理记录会在此归档。" compact />
        <div v-else class="mobile-table-wrapper">
          <el-table v-loading="loading" :data="reports" stripe>
            <el-table-column prop="targetType" label="所属板块" width="130">
              <template #default="{ row }">{{ formatTargetType(row.targetType) }}</template>
            </el-table-column>
            <el-table-column prop="targetId" label="编号" width="90">
              <template #default="{ row }"><span class="mono-num">#{{ row.targetId }}</span></template>
            </el-table-column>
            <el-table-column prop="reason" label="反馈原因" width="180" />
            <el-table-column prop="status" label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="statusTone(row.status)" size="small">{{ formatStatus(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reporterNickname" label="反馈同学" width="140" />
            <el-table-column prop="description" label="详细说明" min-width="220" />
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listReportRecords,
  listReviewRecords,
  type ReportRecordSummary,
  type ReviewRecordSummary,
} from '@/api/campushub'
import { formatStatus, formatTargetType, statusTone } from '@/utils/display'
import EmptyState from '@/components/common/EmptyState.vue'

const reviews = ref<ReviewRecordSummary[]>([])
const reports = ref<ReportRecordSummary[]>([])
const loading = ref(false)

async function loadModeration() {
  loading.value = true
  try {
    const [reviewRecords, reportRecords] = await Promise.all([listReviewRecords(), listReportRecords()])
    reviews.value = reviewRecords
    reports.value = reportRecords
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '治理数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadModeration)
</script>
