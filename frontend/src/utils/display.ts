export type TagTone = 'success' | 'warning' | 'danger' | 'info' | 'primary'

export const CAMPUS_ZONE_LABELS: Record<string, string> = {
  CENTRAL: '中校区',
  WEST: '西校区',
  EAST: '东校区',
  NORTH: '北校区',
  SOUTH: '南校区',
  HIGH_TECH: '高新校区',
  ADVANCED_RESEARCH_INSTITUTE: '先研院',
  SCIENCE_ISLAND: '科学岛',
  OTHER: '其他校区',
}

export function formatCampusZone(zone?: string | null): string {
  if (!zone) return '全校通用'
  return CAMPUS_ZONE_LABELS[zone.trim().toUpperCase()] ?? zone
}

const CHANNEL_LABELS: Record<string, string> = {
  ALIPAY: '支付宝',
  WECHAT: '微信支付',
  OFFLINE: '线下当面',
  WALLET: '钱包余额',
  PAYMENT_CENTER: '支付宝收银台',
  MOCK: '演示通道',
}

export function formatChannel(channel?: string | null): string {
  if (!channel) return '站内通道'
  const key = channel.trim().toUpperCase()
  return CHANNEL_LABELS[key] ?? channel
}

const TARGET_TYPE_LABELS: Record<string, string> = {
  GOODS: '二手闲置',
  GOODS_ORDER: '二手订单',
  GOODS_ESCROW: '二手线上担保',
  TASK: '跑腿悬赏',
  REWARD_TASK: '跑腿悬赏',
  RUNNER_TASK: '跑腿悬赏',
  SHOP: '学生小店',
  SERVICE_ITEM: '店铺服务',
  SERVICE_ORDER: '服务预约',
  PROJECT_AD: '项目展示',
  ROLE_APPLICATION: '身份保证金',
  ROLE_DEPOSIT: '身份保证金',
  SERVICE_FEE: '平台服务费',
  WALLET_RECHARGE: '钱包充值',
  WALLET_WITHDRAWAL: '余额提现',
  WITHDRAWAL: '余额提现',
  RECHARGE: '钱包充值',
  COMMENT: '互动留言',
  USER: '校园用户',
  SYSTEM: '站务通知',
}

export function formatTargetType(type?: string | null): string {
  if (!type) return '校园服务'
  const key = type.trim().toUpperCase()
  return TARGET_TYPE_LABELS[key] ?? type
}

const FLOW_TYPE_LABELS: Record<string, string> = {
  RECHARGE: '余额充值',
  WITHDRAWAL: '余额提现',
  WITHDRAW_FREEZE: '提现冻结',
  WITHDRAW_UNFREEZE: '提现退回解冻',
  ESCROW_FREEZE: '担保交易冻结',
  ESCROW_RELEASE: '担保交易放款',
  ESCROW_REFUND: '担保交易退回',
  SERVICE_FEE: '平台服务费',
  ROLE_DEPOSIT: '身份保证金',
  PAYMENT: '账单支付',
  REFUND: '资金退回',
  ADJUSTMENT: '账务调整',
}

export function formatFlowType(flowType?: string | null): string {
  if (!flowType) return '账务变动'
  const key = flowType.trim().toUpperCase()
  return FLOW_TYPE_LABELS[key] ?? flowType
}

const DIRECTION_LABELS: Record<string, string> = {
  IN: '入账',
  CREDIT: '入账',
  OUT: '支出',
  DEBIT: '支出',
  FREEZE: '冻结',
  UNFREEZE: '解冻',
}

export function formatDirection(direction?: string | null): string {
  if (!direction) return '变动'
  const key = direction.trim().toUpperCase()
  return DIRECTION_LABELS[key] ?? direction
}

const ROLE_TYPE_LABELS: Record<string, string> = {
  RUNNER: '跑腿接单员',
  ROLE_RUNNER: '跑腿接单员',
  GOODS_PUBLISHER: '闲置发布者',
  ROLE_GOODS_PUBLISHER: '闲置发布者',
  SHOP_MERCHANT: '技能店主理人',
  ROLE_SHOP_MERCHANT: '技能店主理人',
  TRADE_ADMIN: '交易管理员',
  ROLE_TRADE_ADMIN: '交易管理员',
  SHOWCASE_ADMIN: '展示管理员',
  ROLE_SHOWCASE_ADMIN: '展示管理员',
  MASTER_ADMIN: '总站务管理员',
  ROLE_MASTER_ADMIN: '总站务管理员',
  ADMIN: '平台管理员',
  ROLE_ADMIN: '平台管理员',
  STUDENT: '校园同学',
  ROLE_STUDENT: '校园同学',
}

export function formatRoleType(roleType?: string | null): string {
  if (!roleType) return '校园成员'
  const key = roleType.trim().toUpperCase()
  return ROLE_TYPE_LABELS[key] ?? roleType
}

const DELIVERY_METHOD_LABELS: Record<string, string> = {
  OFFLINE_MEETUP: '校内当面交接',
  SELF_PICKUP: '买家上门自提',
  NEGOTIATED: '双方协商交接',
}

export function formatDeliveryMethod(method?: string | null): string {
  if (!method) return '校内面交'
  const key = method.trim().toUpperCase()
  return DELIVERY_METHOD_LABELS[key] ?? method
}

const ISSUE_TYPE_LABELS: Record<string, string> = {
  PICKUP_FAILED: '无法取件',
  LOCATION_ERROR: '地点有误',
  CONTACT_FAILED: '暂时联系不上',
  TIMEOUT: '时间来不及',
  ITEM_ABNORMAL: '物品状态异常',
  OTHER: '其他问题',
}

export function formatIssueType(issueType?: string | null): string {
  if (!issueType) return '履约异常'
  const key = issueType.trim().toUpperCase()
  return ISSUE_TYPE_LABELS[key] ?? issueType
}

const AD_TYPE_LABELS: Record<string, string> = {
  TEAM_UP: '科研/竞赛组队',
  PORTFOLIO: '个人作品集',
  CLUB_RECRUITMENT: '社团与实验室纳新',
  CAMPUS_EVENT: '校园活动',
  OTHER: '综合展示',
}

export function formatAdType(adType?: string | null): string {
  if (!adType) return '校园展示'
  const key = adType.trim().toUpperCase()
  return AD_TYPE_LABELS[key] ?? adType
}

const STATUS_LABELS: Record<string, { label: string; tone: TagTone }> = {
  ACTIVE: { label: '正常可用', tone: 'success' },
  FROZEN: { label: '已冻结', tone: 'warning' },
  DISABLED: { label: '已停用', tone: 'danger' },
  PAID: { label: '已支付', tone: 'success' },
  UNPAID: { label: '待支付', tone: 'warning' },
  PENDING: { label: '待处理', tone: 'warning' },
  PENDING_PAYMENT: { label: '待完成支付', tone: 'warning' },
  PENDING_REVIEW: { label: '待人工核对', tone: 'warning' },
  PENDING_FREEZE: { label: '待冻结资金', tone: 'warning' },
  IN_REVIEW: { label: '核对处理中', tone: 'primary' },
  APPROVED: { label: '已通过', tone: 'success' },
  PUBLISHED: { label: '展示中', tone: 'success' },
  OPEN: { label: '待接单', tone: 'primary' },
  ACCEPTED: { label: '已接单', tone: 'primary' },
  HEADING_TO_PICKUP: { label: '前往取件中', tone: 'primary' },
  PICKED_UP: { label: '已取件', tone: 'primary' },
  DELIVERING: { label: '正在派送', tone: 'primary' },
  IN_PROGRESS: { label: '服务进行中', tone: 'primary' },
  REQUESTED: { label: '待商家确认', tone: 'warning' },
  COMPLETED: { label: '已完成', tone: 'success' },
  CONFIRMED: { label: '已确认完成', tone: 'success' },
  RELEASED: { label: '已解冻放款', tone: 'success' },
  RESOLVED: { label: '已妥善解决', tone: 'success' },
  SOLD: { label: '已售出', tone: 'info' },
  PAUSED: { label: '已暂停', tone: 'info' },
  CLOSED: { label: '已关闭', tone: 'info' },
  OFF_SHELF: { label: '已下架', tone: 'info' },
  CANCELLED: { label: '已取消', tone: 'info' },
  EXPIRED: { label: '已过期', tone: 'info' },
  NOT_REQUIRED: { label: '免保证金', tone: 'info' },
  REJECTED: { label: '未通过', tone: 'danger' },
  FAILED: { label: '支付未完成', tone: 'danger' },
  BLOCKED: { label: '已屏蔽', tone: 'danger' },
  DISPUTED: { label: '纠纷协调中', tone: 'danger' },
  ESCALATED: { label: '已升级处理', tone: 'danger' },
  DRAFT: { label: '草稿', tone: 'info' },
  LOW: { label: '轻微', tone: 'info' },
  MEDIUM: { label: '一般', tone: 'warning' },
  HIGH: { label: '较重', tone: 'danger' },
  CRITICAL: { label: '严重', tone: 'danger' },
}

export function formatStatus(status?: string | null): string {
  if (!status) return '处理中'
  const key = status.trim().toUpperCase()
  return STATUS_LABELS[key]?.label ?? status
}

export function statusTone(status?: string | null): TagTone {
  if (!status) return 'info'
  const key = status.trim().toUpperCase()
  return STATUS_LABELS[key]?.tone ?? 'info'
}

const PENALTY_LABELS: Record<string, string> = {
  CREDIT_ONLY: '仅调整信用分',
  WARNING: '站内提醒警告',
  POSTING_FREEZE: '暂停发布权限',
  SERVICE_FREEZE: '暂停接单与服务',
  ACCOUNT_DISABLED: '限制账号登录',
}

export function formatPenalty(penalty?: string | null): string {
  if (!penalty) return '常规处理'
  const key = penalty.trim().toUpperCase()
  return PENALTY_LABELS[key] ?? penalty
}
