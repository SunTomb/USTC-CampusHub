export const FINANCIAL_FEATURE_UNAVAILABLE_MESSAGE = '资金与线上担保功能暂未开放，历史记录仅供查看。'

export function isFinancialFeatureEnabled(value?: string | boolean | null) {
  if (typeof value === 'boolean') {
    return value
  }
  return String(value ?? '').trim().toLowerCase() === 'true'
}

export const FINANCIAL_FEATURES_ENABLED = false
