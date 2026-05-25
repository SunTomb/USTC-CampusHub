<template>
  <section class="page-stack credit-page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">Trust Center</p>
        <h2>信用中心</h2>
        <p>查看信用分、账号限制、违规记录、信用变动和我提交的举报。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-card v-if="center" shadow="never" class="credit-score-card premium-panel">
      <div>
        <span>当前信用分</span>
        <strong>{{ center.creditScore }}</strong>
        <p>{{ center.nickname }} · 信用分范围 0-100</p>
      </div>
      <el-tag :type="center.activeRestrictions.length ? 'danger' : 'success'">
        {{ center.activeRestrictions.length ? '存在限制' : '状态正常' }}
      </el-tag>
    </el-card>

    <el-alert
      title="CampusHub 只记录平台服务费、保证金身份和信用治理，不托管交易本金，也不按单冻结押金。请在线下或微信/QQ 沟通时保留必要证据。"
      type="info"
      show-icon
      :closable="false"
    />

    <el-tabs v-if="center" class="tabs-surface premium-panel">
      <el-tab-pane label="当前限制">
        <EmptyState
          v-if="center.activeRestrictions.length === 0"
          title="暂无有效限制"
          description="当前账号没有发布、服务或内容限制。"
          compact
        />
        <div v-else class="mobile-table-wrapper">
          <el-table :data="center.activeRestrictions" stripe>
            <el-table-column prop="restrictionType" label="类型" width="150" />
            <el-table-column prop="reason" label="原因" />
            <el-table-column prop="startsAt" label="开始" width="180" />
            <el-table-column prop="endsAt" label="结束" width="180" />
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="信用记录">
        <EmptyState
          v-if="center.creditAdjustments.length === 0"
          title="暂无信用变动"
          description="管理员处理举报、违规或申诉后，信用变化会显示在这里。"
          compact
        />
        <el-timeline v-else class="credit-timeline">
          <el-timeline-item v-for="item in center.creditAdjustments" :key="item.id" :timestamp="item.createdAt">
            <strong>{{ item.deltaScore > 0 ? '+' : '' }}{{ item.deltaScore }}</strong>
            <span>：{{ item.reason }}（{{ item.beforeScore }} → {{ item.afterScore }}）</span>
          </el-timeline-item>
        </el-timeline>
      </el-tab-pane>
      <el-tab-pane label="违规记录">
        <EmptyState
          v-if="center.violations.length === 0"
          title="暂无违规记录"
          description="保持良好履约和沟通记录，有助于提升校园交易可信度。"
          compact
        />
        <div v-else class="mobile-table-wrapper">
          <el-table :data="center.violations" stripe>
            <el-table-column prop="violationType" label="类型" width="150" />
            <el-table-column prop="severity" label="严重程度" width="120" />
            <el-table-column prop="penaltyType" label="处罚" width="150" />
            <el-table-column prop="creditDelta" label="信用变化" width="110" />
            <el-table-column prop="description" label="说明" />
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="我的举报">
        <EmptyState
          v-if="center.myReports.length === 0"
          title="暂无我的举报"
          description="你提交的举报处理进度会汇总在这里。"
          compact
        />
        <div v-else class="mobile-table-wrapper">
          <el-table :data="center.myReports" stripe>
            <el-table-column prop="targetType" label="对象" width="130" />
            <el-table-column prop="targetId" label="对象ID" width="100" />
            <el-table-column prop="reason" label="原因" />
            <el-table-column prop="status" label="状态" width="130" />
            <el-table-column prop="reviewNote" label="处理说明" />
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getCreditCenter, type CreditCenterSummary } from '@/api/campushub'
import { useAuthStore } from '@/stores/auth'
import EmptyState from '@/components/common/EmptyState.vue'

const auth = useAuthStore()
const loading = ref(false)
const center = ref<CreditCenterSummary>()
const userId = computed(() => auth.currentUser?.id)

async function load() {
  if (!userId.value) {
    return
  }
  loading.value = true
  try {
    center.value = await getCreditCenter(userId.value)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
