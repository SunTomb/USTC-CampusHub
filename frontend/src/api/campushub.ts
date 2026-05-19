import { getApi, postApi } from './client'

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  email: string
  password: string
  emailCode: string
}

export interface RegisterCodeResponse {
  email: string
  ttlMinutes: number
  resendSeconds: number
}

export interface RegisterResponse {
  userId: number
  username: string
  email: string
}

export interface GoodsSummary {
  id: number
  title: string
  description: string
  price: number
  sellerNickname: string
  tradeLocation: string
  conditionLevel: string
  viewCount: number
  createdAt: string
}

export interface RewardTaskSummary {
  id: number
  title: string
  description: string
  rewardAmount: number
  depositAmount: number
  taskLocation: string
  deadline: string
  publisherNickname: string
}

export interface ShopSummary {
  id: number
  name: string
  description: string
  ownerNickname: string
  serviceArea: string
  rating: number
}

export interface ProjectAdSummary {
  id: number
  title: string
  description: string
  publisherNickname: string
  linkUrl: string
  contactInfo: string
  viewCount: number
  createdAt: string
}

export interface WalletAccountSummary {
  id: number
  userId: number
  nickname: string
  balance: number
  frozenBalance: number
  status: string
}

export interface WalletFlowSummary {
  id: number
  flowNo: string
  direction: string
  amount: number
  balanceAfter: number
  businessType: string
  businessId: number
  remark: string
  createdAt: string
}

export interface ReviewRecordSummary {
  id: number
  reviewerId: number | null
  reviewerNickname: string | null
  targetType: string
  targetId: number
  result: string
  reason: string
  createdAt: string
}

export interface ReportRecordSummary {
  id: number
  reporterId: number
  reporterNickname: string
  targetType: string
  targetId: number
  reason: string
  description: string
  status: string
  handlerId: number | null
  handlerNickname: string | null
  handledAt: string | null
  createdAt: string
}

export function sendRegisterCode(email: string) {
  return postApi<RegisterCodeResponse>('/auth/register/send-code', { email })
}

export function register(payload: RegisterPayload) {
  return postApi<RegisterResponse>('/auth/register', payload)
}

export function listGoods() {
  return getApi<GoodsSummary[]>('/goods')
}

export function listTasks() {
  return getApi<RewardTaskSummary[]>('/tasks')
}

export function listShops() {
  return getApi<ShopSummary[]>('/shops')
}

export function listProjectAds() {
  return getApi<ProjectAdSummary[]>('/project-ads')
}

export function getWallet(userId = 1) {
  return getApi<WalletAccountSummary>(`/wallet/users/${userId}`)
}

export function listWalletFlows(userId = 1) {
  return getApi<WalletFlowSummary[]>(`/wallet/users/${userId}/flows`)
}

export function listReviewRecords() {
  return getApi<ReviewRecordSummary[]>('/moderation/reviews')
}

export function listReportRecords() {
  return getApi<ReportRecordSummary[]>('/moderation/reports')
}
