<template>
  <section class="page-stack">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Moderation</p>
        <h2>审核治理</h2>
        <p>审核记录与举报记录并列展示，支持课程设计中的内容治理闭环说明。</p>
      </div>
      <el-button :loading="loading" @click="loadModeration">刷新</el-button>
    </div>

    <el-tabs class="tabs-surface">
      <el-tab-pane label="审核记录">
        <el-table v-loading="loading" :data="reviews" stripe>
          <el-table-column prop="targetType" label="对象" width="120" />
          <el-table-column prop="targetId" label="ID" width="90" />
          <el-table-column prop="result" label="结果" width="120" />
          <el-table-column prop="reviewerNickname" label="审核人" width="140" />
          <el-table-column prop="reason" label="原因" min-width="220" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="举报记录">
        <el-table v-loading="loading" :data="reports" stripe>
          <el-table-column prop="targetType" label="对象" width="120" />
          <el-table-column prop="targetId" label="ID" width="90" />
          <el-table-column prop="reason" label="原因" width="180" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column prop="reporterNickname" label="举报人" width="140" />
          <el-table-column prop="description" label="描述" min-width="220" />
        </el-table>
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
