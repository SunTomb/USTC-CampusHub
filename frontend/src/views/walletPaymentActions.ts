export type RechargePaymentInput = {
  channel: string
  paymentPayUrl?: string | null
  wechatQrUrl?: string | null
}

export type RechargePaymentAction =
  | { type: 'open-url'; url: string }
  | { type: 'show-wechat-qr' }
  | { type: 'none' }

export function getRechargePaymentAction(recharge: RechargePaymentInput): RechargePaymentAction {
  if (recharge.channel === 'ALIPAY' && recharge.paymentPayUrl && !recharge.paymentPayUrl.startsWith('mock://')) {
    return { type: 'open-url', url: recharge.paymentPayUrl }
  }
  if (recharge.channel === 'WECHAT' && recharge.wechatQrUrl) {
    return { type: 'show-wechat-qr' }
  }
  return { type: 'none' }
}
