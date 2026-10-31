<template>
  <WalletWorkspace v-if="platform.financialEnabled" />
  <section v-else class="page-stack">
    <LockedState
      eyebrow="服务状态"
      title="资金功能暂未开放"
      description="钱包、充值、提现、服务费、身份保证金及线上担保已暂停。历史记录继续保留，如涉及历史资金事项，请联系站务核对。"
      primary-text="查看服务协议"
      primary-to="/policy"
      secondary-text="返回市集"
      secondary-to="/goods"
    />
  </section>
</template>

<script setup lang="ts">
import { defineAsyncComponent } from 'vue'
import LockedState from '@/components/common/LockedState.vue'
import { usePlatformStore } from '@/stores/platform'

const platform = usePlatformStore()
const WalletWorkspace = defineAsyncComponent(() => import('./legacy/WalletWorkspace.vue'))
</script>
