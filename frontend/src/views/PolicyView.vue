<template>
  <section class="page-stack policy-page">
    <div class="page-heading">
      <div>
        <p class="eyebrow">蜗壳 CampusHub · 服务协议与隐私说明</p>
        <h2>协议与交易规则</h2>
        <p>独立学生项目，不代表任何学校或机构。当前不提供充值、提现、收款或线上担保。</p>
      </div>
      <el-button :loading="loading" @click="load">重新加载</el-button>
    </div>
    <el-alert title="请重点阅读交易责任、必要信息核查与个人信息权利条款。阅读页面本身不会替你提交协议同意。" type="info" show-icon :closable="false" />
    <el-tabs v-model="selected" class="premium-panel" @tab-change="load">
      <el-tab-pane v-for="item in agreements" :key="item.key" :label="item.label" :name="item.key" />
    </el-tabs>
    <el-card v-loading="loading" shadow="never" class="premium-panel">
      <template v-if="protocol">
        <h3>{{ protocol.title }}</h3>
        <p class="hint">版本 {{ protocol.version }} · 生效时间 {{ protocol.effectiveAt.slice(0, 10) }}</p>
        <div class="protocol-text">{{ protocol.content }}</div>
      </template>
      <EmptyState v-else-if="!loading" title="协议暂时无法加载" description="请稍后重试；协议未加载时不能提交身份申请。" />
    </el-card>
    <el-button @click="router.push('/roles')">前往身份解锁</el-button>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCurrentRoleAgreement, type PlatformProtocolSummary } from '@/api/campushub'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const selected = ref('USER_SERVICE')
const protocol = ref<PlatformProtocolSummary | null>(null)
const loading = ref(false)
const agreements = [
  { key: 'USER_SERVICE', label: '用户服务协议' },
  { key: 'RUNNER', label: '顺路跑腿' },
  { key: 'GOODS_PUBLISHER', label: '闲置发布' },
  { key: 'SHOP_MERCHANT', label: '技能店主理人' },
]
let requestVersion = 0
async function load() {
  const currentRequest = ++requestVersion
  protocol.value = null
  loading.value = true
  try {
    const response = await getCurrentRoleAgreement(selected.value)
    if (currentRequest === requestVersion) protocol.value = response
  } catch (error) {
    if (currentRequest === requestVersion) ElMessage.error(error instanceof Error ? error.message : '协议加载失败')
  } finally {
    if (currentRequest === requestVersion) loading.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.protocol-text { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.9; }
</style>
