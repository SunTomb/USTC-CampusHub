import { describe, expect, it } from 'vitest'

import { getRechargePaymentAction } from './walletPaymentActions'

describe('getRechargePaymentAction', () => {
  it('opens non-mock Alipay payment URLs', () => {
    expect(getRechargePaymentAction({ channel: 'ALIPAY', paymentPayUrl: 'https://pay.example.com/1' })).toEqual({
      type: 'open-url',
      url: 'https://pay.example.com/1',
    })
  })

  it('does not open mock payment URLs', () => {
    expect(getRechargePaymentAction({ channel: 'ALIPAY', paymentPayUrl: 'mock://pay/1' })).toEqual({ type: 'none' })
  })

  it('shows WeChat manual QR when configured', () => {
    expect(getRechargePaymentAction({ channel: 'WECHAT', wechatQrUrl: 'https://assets.example.com/wx.png' })).toEqual({
      type: 'show-wechat-qr',
    })
  })
})
