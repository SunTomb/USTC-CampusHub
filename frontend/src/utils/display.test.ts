import { describe, expect, it } from 'vitest'
import {
  formatAdType,
  formatCampusZone,
  formatChannel,
  formatDeliveryMethod,
  formatDirection,
  formatFlowType,
  formatIssueType,
  formatPenalty,
  formatRoleType,
  formatStatus,
  formatTargetType,
  statusTone,
} from './display'

describe('display semantic formatters', () => {
  it('formats campus zones into natural Chinese labels', () => {
    expect(formatCampusZone('WEST')).toBe('西校区')
    expect(formatCampusZone('high_tech')).toBe('高新校区')
    expect(formatCampusZone(null)).toBe('全校通用')
  })

  it('formats payment channels and target types into readable Chinese', () => {
    expect(formatChannel('ALIPAY')).toBe('支付宝')
    expect(formatChannel('WECHAT')).toBe('微信支付')
    expect(formatTargetType('GOODS_ESCROW')).toBe('二手线上担保')
    expect(formatTargetType('ROLE_APPLICATION')).toBe('身份保证金')
  })

  it('formats wallet flow types, directions, and roles', () => {
    expect(formatFlowType('WITHDRAW_FREEZE')).toBe('提现冻结')
    expect(formatDirection('IN')).toBe('入账')
    expect(formatRoleType('ROLE_SHOP_MERCHANT')).toBe('技能店主理人')
  })

  it('formats delivery methods, issue types, ad types, and penalties', () => {
    expect(formatDeliveryMethod('OFFLINE_MEETUP')).toBe('校内当面交接')
    expect(formatIssueType('PICKUP_FAILED')).toBe('无法取件')
    expect(formatAdType('TEAM_UP')).toBe('科研/竞赛组队')
    expect(formatPenalty('POSTING_FREEZE')).toBe('暂停发布权限')
  })

  it('maps status codes to localized labels and tag tones', () => {
    expect(formatStatus('PENDING_REVIEW')).toBe('待人工核对')
    expect(statusTone('PENDING_REVIEW')).toBe('warning')
    expect(formatStatus('PAID')).toBe('已支付')
    expect(statusTone('PAID')).toBe('success')
    expect(formatStatus('REJECTED')).toBe('未通过')
    expect(statusTone('REJECTED')).toBe('danger')
  })
})
