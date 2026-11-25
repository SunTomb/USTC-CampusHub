import { defineStore } from 'pinia'
import { getApi } from '@/api/client'
import { FINANCIAL_FEATURES_ENABLED } from '@/config/featureFlags'

export const usePlatformStore = defineStore('platform', {
  state: () => ({ financialEnabled: FINANCIAL_FEATURES_ENABLED, loaded: false }),
  actions: {
    async load() {
      if (this.loaded) return
      try {
        const features = await getApi<{ financialEnabled: boolean }>('/platform/features')
        this.financialEnabled = features.financialEnabled === true
      } catch {
        this.financialEnabled = false
      } finally {
        this.loaded = true
      }
    },
  },
})
