<template>
  <section class="page-stack credit-page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">我的书院 · 守信记录</p>
        <h2>校园信用中心</h2>
        <p>查看你的校内信用誉分、守信记录、权限状态与我提交的反馈进度。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新记录</el-button>
    </div>

    <el-card v-if="center" shadow="never" class="credit-score-card premium-panel">
      <div>
        <span>当前校园信用分</span>
        <strong class="mono-num">{{ center.creditScore }}</strong>
        <p>{{ center.nickname }} · 满分 100 分，良好的信用有助于获得更多校友信任</p>
      </div>
      <el-tag :type="center.activeRestrictions.length ? 'danger' : 'success'" size="large">
        {{ center.activeRestrictions.length ? '部分权限受限' : '信用状态良好' }}
      </el-tag>
    </el-card>

    <el-alert
      title="校集通过实名邮箱、诚信保证金与信用评价共同维护校园互信。线下当面交接或微信/QQ 沟通时，请妥善保留沟通与交接凭证。"
      type="info"
      show-icon
      :closable="false"
    />

    <el-tabs v-if="center" class="tabs-surface premium-panel">
      <el-tab-pane label="当前权限限制">
        <EmptyState
          v-if="center.activeRestrictions.length === 0"
          title="没有任何权限限制"
          description="你的账号状态一切正常，可自由使用发布、接单与预约功能。"
          compact
        />
        <div v-else class="mobile-table-wrapper">
          <el-table :data="center.activeRestrictions" stripe>
            <el-table-column prop="restrictionType" label="限制类型" width="160">
              <template #default="{ row }">{{ formatPenalty(row.restrictionType) }}</template>
            </el-table-column>
            <el-table-column prop="reason" label="原因说明" />
            <el-table-column prop="startsAt" label="生效时间" width="180" />
            <el-table-column prop="endsAt" label="解除时间" width="180" />
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="信用分变动明细">
        <EmptyState
          v-if="center.creditAdjustments.length === 0"
          title="暂无信用分变动记录"
          description="保持准时履约与诚实守信，你的初始信用分将一直保持良好。"
          compact
        />
        <el-timeline v-else class="credit-timeline">
          <el-timeline-item v-for="item in center.creditAdjustments" :key="item.id" :timestamp="item.createdAt">
            <strong class="mono-num">{{ item.deltaScore > 0 ? '+' : '' }}{{ item.deltaScore }}</strong>
            <span>：{{ item.reason }}（<span class="mono-num">{{ item.beforeScore }} → {{ item.afterScore }}</span>）</span>
          </el-timeline-item>
        </el-timeline>
      </el-tab-pane>

      <el-tab-pane label="违规处置记录">
        <EmptyState
          v-if="center.violations.length === 0"
          title="无违规记录"
          description="感谢你共同维护温暖守信的科大校园社区。"
          compact
        />
        <div v-else class="mobile-table-wrapper">
          <el-table :data="center.violations" stripe>
            <el-table-column prop="violationType" label="违规类别" width="150" />
            <el-table-column prop="severity" label="程度" width="120">
              <template #default="{ row }">
                <el-tag :type="statusTone(row.severity)" size="small">{{ formatStatus(row.severity) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="penaltyType" label="处置方式" width="160">
              <template #default="{ row }">{{ formatPenalty(row.penaltyType) }}</template>
            </el-table-column>
            <el-table-column prop="creditDelta" label="信用分调整" width="120">
              <template #default="{ row }"><span class="mono-num">{{ row.creditDelta }}</span></template>
            </el-table-column>
            <el-table-column prop="description" label="详细说明" />
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我提交的反馈">
        <EmptyState
          v-if="center.myReports.length === 0"
          title="暂无反馈记录"
          description="如果你在市集或跑腿中遇到异常并提交反馈，处理进展会显示在这里。"
          compact
        />
        <div v-else class="mobile-table-wrapper">
          <el-table :data="center.myReports" stripe>
            <el-table-column prop="targetType" label="所属板块" width="140">
              <template #default="{ row }">{{ formatTargetType(row.targetType) }}</template>
            </el-table-column>
            <el-table-column prop="targetId" label="编号" width="100">
              <template #default="{ row }"><span class="mono-num">#{{ row.targetId }}</span></template>
            </el-table-column>
            <el-table-column prop="reason" label="反馈原因" />
            <el-table-column prop="status" label="处理进度" width="130">
              <template #default="{ row }">
                <el-tag :type="statusTone(row.status)" size="small">{{ formatStatus(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reviewNote" label="站务回复" />
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
import { formatPenalty, formatStatus, formatTargetType, statusTone } from '@/utils/display'
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
