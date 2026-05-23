import { getApi, postApi, putApi } from './client'

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  email: string
  password: string
  emailCode: string
  wechatContact?: string
  qqContact?: string
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
  originalPrice: number | null
  sellerId: number
  sellerNickname: string
  sellerCreditScore: number
  tradeLocation: string
  campusZone: string
  conditionLevel: string
  status: string
  viewCount: number
  createdAt: string
  coverUrl: string | null
}

export interface FileResourceSummary {
  id: number
  uploaderId: number
  uploaderNickname: string
  originalName: string
  storagePath: string
  contentType: string
  sizeBytes: number
  status: string
  createdAt: string
}

export interface FileBindingSummary {
  id: number
  targetType: string
  targetId: number
  usageType: string
  sortOrder: number
  file: FileResourceSummary
  createdAt: string
}

export interface CommentSummary {
  id: number
  userId: number
  userNickname: string
  targetType: string
  targetId: number
  parentId: number | null
  content: string
  status: string
  createdAt: string
}

export interface ReviewSummary {
  id: number
  reviewerId: number
  reviewerNickname: string
  targetUserId: number
  targetUserNickname: string
  targetType: string
  targetId: number
  rating: number
  content: string | null
  createdAt: string
}

export interface GoodsDetailSummary extends GoodsSummary {
  deliveryMethod: string
  contactVisibility: string
  publishedAt: string | null
  updatedAt: string | null
  contactVisible: boolean
  contactSnapshot: string | null
  images: FileBindingSummary[]
  comments: CommentSummary[]
  sellerReviews: ReviewSummary[]
  favoriteCount: number
  favoritedByViewer: boolean
}

export interface CreateGoodsPayload {
  categoryId: number
  title: string
  description: string
  price: number
  originalPrice?: number | null
  conditionLevel: string
  campusZone: string
  tradeLocation: string
  deliveryMethod: string
  contactVisibility: string
}

export interface GoodsIntentSummary {
  id: number
  goodsId: number
  buyerId: number
  buyerNickname: string
  sellerId: number
  sellerNickname: string
  message: string | null
  contactSnapshot: string
  status: string
  serviceFeeId: number | null
  createdAt: string
}

export interface GoodsOrderSummary {
  id: number
  orderNo: string
  goodsId: number
  goodsTitle: string
  buyerId: number
  buyerNickname: string
  sellerId: number
  sellerNickname: string
  amount: number
  serviceFee: number
  status: string
  tradeMode: string
  escrowStatus: string
  escrowAmount: number
  platformServiceFee: number
  escrowFrozenAt: string | null
  escrowReleasedAt: string | null
  escrowUnfrozenAt: string | null
  cancelReason: string | null
  disputeReason: string | null
  contactSnapshot: string
  createdAt: string | null
  paidAt: string | null
  completedAt: string | null
  canceledAt: string | null
}

export type CampusZone =
  | 'CENTRAL'
  | 'WEST'
  | 'EAST'
  | 'NORTH'
  | 'SOUTH'
  | 'HIGH_TECH'
  | 'ADVANCED_RESEARCH_INSTITUTE'
  | 'SCIENCE_ISLAND'
  | 'OTHER'

export type TaskAcceptanceMode = 'GRAB' | 'APPLICATION'
export type TaskWorkflowStatus =
  | 'DRAFT'
  | 'PUBLISHED'
  | 'ACCEPTED'
  | 'HEADING_TO_PICKUP'
  | 'PICKED_UP'
  | 'DELIVERING'
  | 'DELIVERED'
  | 'PENDING_CONFIRMATION'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'ISSUE_HANDLING'
  | 'DISPUTE_HANDLING'

export type TaskVerificationMode = 'COMPLETION_CODE' | 'PHOTO_AND_CONFIRMATION'

export interface RewardTaskSummary {
  id: number
  title: string
  description: string
  rewardAmount: number
  depositAmount: number
  taskLocation: string
  deadline: string
  status: string
  acceptanceMode: TaskAcceptanceMode
  originZone: CampusZone
  destinationZone: CampusZone
  originDetail: string | null
  destinationDetail: string | null
  workflowStatus: TaskWorkflowStatus
  verificationMode: TaskVerificationMode
  acceptedApplicationId: number | null
  publisherId: number
  publisherNickname: string
}

export interface CreateRunnerTaskPayload {
  title: string
  description: string
  rewardAmount: number
  depositAmount?: number
  acceptanceMode: TaskAcceptanceMode
  originZone: CampusZone
  destinationZone: CampusZone
  originDetail?: string
  destinationDetail?: string
  deadline: string
  verificationMode: TaskVerificationMode
}

export interface ApplyTaskPayload {
  message?: string
}

export interface TaskActionPayload {
  note?: string
  completionCode?: string
}

export interface TaskIssuePayload {
  issueType: string
  description: string
}

export interface TaskApplicationSummary {
  id: number
  taskId: number
  taskTitle: string
  applicantId: number
  applicantNickname: string
  message: string | null
  status: string
  createdAt: string
  acceptedAt: string | null
  completedAt: string | null
}

export interface RoleApplicationSummary {
  id: number
  userId: number
  userNickname: string
  roleType: string
  depositAmount: number
  depositStatus: string
  depositPaymentOrderNo: string | null
  reviewStatus: string
  applyNote: string | null
  reviewerNickname: string | null
  createdAt: string
  reviewedAt: string | null
}

export interface ApplyRolePayload {
  roleType: string
  applyNote?: string
}

export interface StationNotificationSummary {
  id: number
  recipientId: number
  title: string
  content: string
  targetType: string | null
  targetId: number | null
  readAt: string | null
  createdAt: string
}

export interface OperationsDashboardSummary {
  publishedTasks: number
  acceptedTasks: number
  completedTasks: number
  openIssues: number
  pendingRoleApplications: number
}

export interface MetricCardSummary {
  key: string
  label: string
  value: number
  unit: string
}

export interface OperationsAnalyticsOverview {
  startDate: string
  endDate: string
  newUsers: number
  activeUsers: number
  newTasks: number
  completedTasks: number
  taskIssues: number
  newGoods: number
  activeGoods: number
  goodsIntents: number
  newShopOrders: number
  completedShopOrders: number
  canceledShopOrders: number
  newProjectAds: number
  approvedProjectAds: number
  projectAdViews: number
  openReports: number
  pendingRoleApplications: number
  pendingProjectAds: number
  paidServiceFeeAmount: number
  roleDepositAmount: number
  cards: MetricCardSummary[]
}

export interface BusinessFunnelSummary {
  businessKey: string
  businessName: string
  steps: MetricCardSummary[]
}

export interface OperationsFunnelSummary {
  startDate: string
  endDate: string
  funnels: BusinessFunnelSummary[]
}

export interface ZoneMetricSummary {
  key: string
  label: string
  count: number
}

export interface OperationsZoneSummary {
  startDate: string
  endDate: string
  taskOriginZones: ZoneMetricSummary[]
  taskDestinationZones: ZoneMetricSummary[]
  taskRoutes: ZoneMetricSummary[]
  goodsZones: ZoneMetricSummary[]
  shopZones: ZoneMetricSummary[]
  projectAdZones: ZoneMetricSummary[]
}

export interface FeeAnalyticsSummary {
  startDate: string
  endDate: string
  serviceFeeCount: number
  paidServiceFeeAmount: number
  pendingServiceFeeAmount: number
  serviceFeesByTargetType: MetricCardSummary[]
  roleApplicationCount: number
  roleDepositAmount: number
  roleDepositsByType: MetricCardSummary[]
}

export interface OpsAnalyticsParams {
  startDate?: string
  endDate?: string
}

export interface TaskIssueSummary {
  id: number
  taskId: number
  taskTitle: string
  reporterId: number
  reporterNickname: string
  issueType: string
  description: string
  status: string
  createdAt: string
}

export interface ShopSummary {
  id: number
  name: string
  description: string
  ownerId: number
  ownerNickname: string
  serviceArea: string
  campusZone: CampusZone
  contactVisibility: string
  openingHours: string | null
  coverFileId: number | null
  status: string
  rating: number
}

export interface ServiceItemSummary {
  id: number
  shopId: number
  shopName: string
  category: string
  title: string
  description: string
  price: number
  minPrice: number | null
  maxPrice: number | null
  priceUnit: string
  coverFileId: number | null
  durationMinutes: number
  status: string
  createdAt: string
}

export interface ShopDetailSummary extends ShopSummary {
  ownerCreditScore: number
  contactVisible: boolean
  contactSnapshot: string | null
  serviceItems: ServiceItemSummary[]
  createdAt: string
  updatedAt: string
}

export interface CreateShopPayload {
  name: string
  description: string
  serviceArea: string
  campusZone: CampusZone
  contactVisibility: string
  openingHours?: string
  coverFileId?: number | null
}

export interface CreateServiceItemPayload {
  category: string
  title: string
  description: string
  price: number
  minPrice?: number | null
  maxPrice?: number | null
  priceUnit: string
  durationMinutes: number
  coverFileId?: number | null
}

export interface CreateServiceOrderPayload {
  appointmentTime: string
  amount?: number | null
  note?: string
}

export interface ServiceOrderActionPayload {
  actorId: number
  note?: string
  cancelReason?: string
}

export interface ServiceOrderSummary {
  id: number
  orderNo: string
  serviceItemId: number
  serviceItemTitle: string
  shopId: number
  shopName: string
  customerId: number
  customerNickname: string
  providerId: number
  providerNickname: string
  appointmentTime: string
  amount: number
  serviceFee: number
  status: string
  note: string | null
  contactSnapshot: string | null
  cancelReason: string | null
  serviceFeeId: number | null
  createdAt: string
  paidAt: string | null
  completedAt: string | null
  canceledAt: string | null
}

export type ProjectAdType = 'TEAM_UP' | 'PORTFOLIO' | 'CLUB_RECRUITMENT' | 'CAMPUS_EVENT' | 'OTHER'
export type ProjectAdStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'EXPIRED' | 'CLOSED' | 'BLOCKED'
export type ProjectAdContactVisibility = 'PUBLIC' | 'LOGIN_ONLY' | 'INTERACTION_ONLY' | 'HIDDEN'

export interface ProjectAdSummary {
  id: number
  title: string
  adType: ProjectAdType
  summary: string | null
  description: string
  tags: string | null
  campusZone: string | null
  coverFileId: number | null
  publisherId: number
  publisherNickname: string
  linkUrl: string | null
  contactInfo: string | null
  contactVisibility: ProjectAdContactVisibility
  status: ProjectAdStatus
  viewCount: number
  featured: boolean
  featuredPriority: number
  expiresAt: string | null
  publishedAt: string | null
  createdAt: string
}

export interface ProjectAdDetailSummary extends ProjectAdSummary {
  contactVisibility: ProjectAdContactVisibility
  contactVisible: boolean
  contactInfo: string | null
  reviewNote: string | null
  favoriteCount: number
  commentCount: number
  favorited: boolean
  attachments: FileBindingSummary[]
}

export interface ProjectAdPayload {
  title: string
  adType: ProjectAdType
  summary?: string | null
  description: string
  tags?: string | null
  campusZone?: string | null
  coverFileId?: number | null
  linkUrl?: string | null
  contactInfo: string
  contactVisibility: ProjectAdContactVisibility
  expiresAt?: string | null
}

export interface ProjectAdReviewPayload {
  note?: string | null
  featuredPriority?: number | null
}

export interface ProjectAdListParams {
  adType?: ProjectAdType | ''
  campusZone?: string
  keyword?: string
  featured?: boolean
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
  flowType: string
  amount: number
  balanceAfter: number
  availableBalanceAfter: number
  frozenBalanceAfter: number
  businessType: string
  businessId: number
  idempotencyKey: string | null
  counterpartyUserId: number | null
  counterpartyNickname: string | null
  createdBy: string
  operatorId: number | null
  operatorNickname: string | null
  remark: string
  createdAt: string
}

export interface WalletRechargeSummary {
  id: number
  rechargeNo: string
  userId: number
  userNickname: string
  channel: string
  amount: number
  channelFee: number
  payAmount: number
  status: string
  paymentOrderNo: string | null
  reviewNote: string | null
  reviewedAt: string | null
  createdAt: string | null
}

export interface WalletWithdrawalSummary {
  id: number
  withdrawalNo: string
  userId: number
  userNickname: string
  amount: number
  channel: string
  accountSnapshot: string | null
  status: string
  reviewNote: string | null
  reviewedAt: string | null
  completedAt: string | null
  createdAt: string | null
}

export interface WalletFrozenRecordSummary {
  id: number
  freezeNo: string
  userId: number
  userNickname: string
  businessType: string
  businessId: number
  amount: number
  status: string
  frozenAt: string | null
  releasedAt: string | null
  remark: string | null
}

export interface ServiceFeeSummary {
  id: number
  feeNo: string
  payerId: number
  payerNickname: string
  targetType: string
  targetId: number
  amount: number
  status: string
  paymentOrderNo: string | null
  paymentProvider: string | null
  paymentCenterOrderNo: string | null
  payUrl: string | null
  createdAt: string
  paidAt: string | null
  expiresAt: string | null
  failedAt: string | null
  failureReason: string | null
}

export interface PaymentCreation {
  provider: string
  orderNo: string
  providerOrderNo: string
  payUrl: string
  status: string
  expiresAt: string | null
  message: string
}

export interface PaymentStatus {
  provider: string
  orderNo: string
  providerOrderNo: string | null
  status: string
  paidAt: string | null
  failureReason: string | null
  message: string
}

export interface PaymentOrderSummary {
  id: number
  orderNo: string
  businessType: string
  businessId: number
  payerId: number
  payerNickname: string
  amount: number
  provider: string
  providerOrderNo: string | null
  payUrl: string | null
  status: string
  expiresAt: string | null
  paidAt: string | null
  failedAt: string | null
  failureReason: string | null
  createdAt: string | null
}

export interface PaymentCallbackEventSummary {
  id: number
  eventId: string
  orderNo: string
  providerOrderNo: string | null
  status: string
  amount: number
  verified: boolean
  handled: boolean
  failureReason: string | null
  createdAt: string | null
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
  reviewNote: string | null
  resolutionType: string | null
  handlerId: number | null
  handlerNickname: string | null
  handledAt: string | null
  createdAt: string
  updatedAt: string | null
}

export interface ViolationRecordSummary {
  id: number
  userId: number
  userNickname: string
  reportId: number | null
  targetType: string | null
  targetId: number | null
  violationType: string
  severity: string
  penaltyType: string
  description: string
  creditDelta: number
  adminId: number | null
  adminNickname: string | null
  depositImpactNote: string | null
  createdAt: string
}

export interface GovernanceDashboardSummary {
  openReports: number
  inReviewReports: number
  handledReports: number
  highSeverityViolations: number
  activeRestrictions: number
}

export interface GovernanceActionPayload {
  resolutionType?: string
  note?: string
}

export interface CreateViolationPayload {
  userId: number
  reportId?: number | null
  targetType?: string | null
  targetId?: number | null
  violationType: string
  severity: string
  penaltyType: string
  description: string
  creditDelta: number
  depositImpactNote?: string | null
  restrictionType?: string | null
  restrictionDays?: number | null
}

export interface CreditAdjustmentSummary {
  id: number
  userId: number
  userNickname: string
  violationId: number | null
  beforeScore: number
  deltaScore: number
  afterScore: number
  reason: string
  adminId: number | null
  adminNickname: string | null
  createdAt: string
}

export interface UserRestrictionSummary {
  id: number
  userId: number
  userNickname: string
  violationId: number | null
  restrictionType: string
  reason: string
  startsAt: string
  endsAt: string | null
  active: boolean
  adminId: number | null
  adminNickname: string | null
  createdAt: string
}

export interface AdminActionLogSummary {
  id: number
  adminId: number | null
  adminNickname: string | null
  actionType: string
  targetType: string
  targetId: number
  note: string | null
  createdAt: string
}

export interface CreditCenterSummary {
  userId: number
  nickname: string
  creditScore: number
  activeRestrictions: UserRestrictionSummary[]
  violations: ViolationRecordSummary[]
  creditAdjustments: CreditAdjustmentSummary[]
  myReports: ReportRecordSummary[]
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

export function getGoodsDetail(id: number, viewerId?: number) {
  const query = viewerId ? `?viewerId=${viewerId}` : ''
  return getApi<GoodsDetailSummary>(`/goods/${id}${query}`)
}

export function publishGoods(sellerId: number, payload: CreateGoodsPayload) {
  return postApi<GoodsDetailSummary>(`/goods?sellerId=${sellerId}`, payload)
}

export function createGoodsIntent(goodsId: number, buyerId: number, message: string) {
  return postApi<GoodsIntentSummary>(`/goods/${goodsId}/intents?buyerId=${buyerId}`, { message })
}

export function markGoodsSold(goodsId: number, userId: number, buyerId?: number) {
  return postApi<GoodsDetailSummary>(`/goods/${goodsId}/mark-sold`, { userId, buyerId })
}

export function createGoodsEscrowOrder(goodsId: number, buyerId: number) {
  return postApi<GoodsOrderSummary>(`/goods/${goodsId}/orders/escrow?buyerId=${buyerId}`, {})
}

export function freezeGoodsEscrow(orderId: number, buyerId: number) {
  return postApi<GoodsOrderSummary>(`/goods/orders/${orderId}/escrow/freeze?buyerId=${buyerId}`, {})
}

export function confirmGoodsEscrow(orderId: number, buyerId: number) {
  return postApi<GoodsOrderSummary>(`/goods/orders/${orderId}/escrow/confirm?buyerId=${buyerId}`, {})
}

export function bindFileToTarget(payload: { fileId: number; targetType: string; targetId: number; usageType: string; sortOrder: number }) {
  return postApi<FileBindingSummary>('/files/bindings', payload)
}

export function commentTarget(userId: number, payload: { targetType: string; targetId: number; parentId?: number | null; content: string }) {
  return postApi<CommentSummary>(`/interactions/comments?userId=${userId}`, payload)
}

export function favoriteTarget(userId: number, payload: { targetType: string; targetId: number }) {
  return postApi<void>(`/interactions/favorites?userId=${userId}`, payload)
}

export function reportTarget(reporterId: number, payload: { targetType: string; targetId: number; reason: string; description: string }) {
  return postApi<ReportRecordSummary>(`/moderation/reports?reporterId=${reporterId}`, payload)
}

export function createReview(reviewerId: number, payload: { targetUserId: number; targetType: string; targetId: number; rating: number; content: string }) {
  return postApi<ReviewSummary>(`/reviews?reviewerId=${reviewerId}`, payload)
}

export function listTasks() {
  return getApi<RewardTaskSummary[]>('/tasks')
}

export function publishRunnerTask(payload: CreateRunnerTaskPayload, publisherId = 1) {
  return postApi<RewardTaskSummary>(`/tasks?publisherId=${publisherId}`, payload)
}

export function grabRunnerTask(taskId: number, userId = 1) {
  return postApi<RewardTaskSummary>(`/tasks/${taskId}/grab?runnerId=${userId}`, {})
}

export function applyRunnerTask(taskId: number, userId: number, payload: ApplyTaskPayload) {
  return postApi<TaskApplicationSummary>(`/tasks/${taskId}/applications?applicantId=${userId}`, payload)
}

export function acceptRunnerTaskApplication(taskId: number, applicationId: number, publisherId = 1) {
  return postApi<TaskApplicationSummary>(`/tasks/${taskId}/applications/${applicationId}/accept?publisherId=${publisherId}`, {})
}

export function advanceRunnerTask(taskId: number, userId: number, nextStatus: string, payload: TaskActionPayload) {
  return postApi<RewardTaskSummary>(`/tasks/${taskId}/workflow/${nextStatus}?actorId=${userId}`, payload)
}

export function completeRunnerTaskWithCode(taskId: number, userId: number, payload: TaskActionPayload) {
  return postApi<RewardTaskSummary>(`/tasks/${taskId}/complete-code?runnerId=${userId}`, payload)
}

export function confirmRunnerTask(taskId: number, publisherId: number, payload: TaskActionPayload) {
  return postApi<RewardTaskSummary>(`/tasks/${taskId}/confirm?publisherId=${publisherId}`, payload)
}

export function reportRunnerTaskIssue(taskId: number, userId: number, payload: TaskIssuePayload) {
  return postApi<RewardTaskSummary>(`/tasks/${taskId}/issues?reporterId=${userId}`, payload)
}

export function applyRole(userId: number, payload: ApplyRolePayload) {
  return postApi<RoleApplicationSummary>(`/identity/users/${userId}/roles`, payload)
}

export function listNotifications(userId: number) {
  return getApi<StationNotificationSummary[]>(`/users/${userId}/notifications`)
}

export function markNotificationRead(notificationId: number) {
  return postApi<StationNotificationSummary>(`/notifications/${notificationId}/read`, {})
}

export function getOpsDashboard() {
  return getApi<OperationsDashboardSummary>('/admin/ops/dashboard')
}

export function getOpsAnalyticsOverview(params?: OpsAnalyticsParams) {
  return getApi<OperationsAnalyticsOverview>(`/admin/ops/analytics/overview${buildQuery(params)}`)
}

export function getOpsAnalyticsFunnels(params?: OpsAnalyticsParams) {
  return getApi<OperationsFunnelSummary>(`/admin/ops/analytics/funnels${buildQuery(params)}`)
}

export function getOpsAnalyticsZones(params?: OpsAnalyticsParams) {
  return getApi<OperationsZoneSummary>(`/admin/ops/analytics/zones${buildQuery(params)}`)
}

export function getOpsAnalyticsFees(params?: OpsAnalyticsParams) {
  return getApi<FeeAnalyticsSummary>(`/admin/ops/analytics/fees${buildQuery(params)}`)
}

export function buildOpsExportUrl(
  kind: 'tasks' | 'goods' | 'shop-orders' | 'project-ads' | 'governance' | 'fees',
  params?: OpsAnalyticsParams,
) {
  return `/api/admin/ops/exports/${kind}.csv${buildQuery(params)}`
}

export function listOpsTasks() {
  return getApi<RewardTaskSummary[]>('/admin/ops/tasks')
}

export function listOpsTaskIssues() {
  return getApi<TaskIssueSummary[]>('/admin/ops/task-issues')
}

export function listOpsRoleApplications() {
  return getApi<RoleApplicationSummary[]>('/admin/ops/role-applications')
}

export function listOpsShopOrders() {
  return getApi<ServiceOrderSummary[]>('/admin/ops/shop-orders')
}

export function listShops() {
  return getApi<ShopSummary[]>('/shops')
}

export function getShopDetail(id: number, viewerId?: number) {
  const query = viewerId ? `?viewerId=${viewerId}` : ''
  return getApi<ShopDetailSummary>(`/shops/${id}${query}`)
}

export function getMyShop(ownerId: number) {
  return getApi<ShopDetailSummary>(`/shops/mine?ownerId=${ownerId}`)
}

export function createShop(ownerId: number, payload: CreateShopPayload) {
  return postApi<ShopDetailSummary>(`/shops?ownerId=${ownerId}`, payload)
}

export function updateShop(shopId: number, ownerId: number, payload: CreateShopPayload) {
  return putApi<ShopDetailSummary>(`/shops/${shopId}?ownerId=${ownerId}`, payload)
}

export function createServiceItem(shopId: number, ownerId: number, payload: CreateServiceItemPayload) {
  return postApi<ServiceItemSummary>(`/service-items/shop/${shopId}?ownerId=${ownerId}`, payload)
}

export function updateServiceItem(itemId: number, ownerId: number, payload: CreateServiceItemPayload) {
  return putApi<ServiceItemSummary>(`/service-items/${itemId}?ownerId=${ownerId}`, payload)
}

export function pauseServiceItem(itemId: number, userId: number) {
  return postApi<ServiceItemSummary>(`/service-items/${itemId}/pause`, { userId })
}

export function publishServiceItem(itemId: number, userId: number) {
  return postApi<ServiceItemSummary>(`/service-items/${itemId}/publish`, { userId })
}

export function createServiceOrder(itemId: number, customerId: number, payload: CreateServiceOrderPayload) {
  return postApi<ServiceOrderSummary>(`/service-items/${itemId}/orders?customerId=${customerId}`, payload)
}

export function listShopOrders(shopId: number, ownerId: number) {
  return getApi<ServiceOrderSummary[]>(`/shops/${shopId}/orders?ownerId=${ownerId}`)
}

export function listCustomerServiceOrders(customerId: number) {
  return getApi<ServiceOrderSummary[]>(`/service-orders/customer/${customerId}`)
}

export function acceptServiceOrder(orderId: number, payload: ServiceOrderActionPayload) {
  return postApi<ServiceOrderSummary>(`/service-orders/${orderId}/accept`, payload)
}

export function rejectServiceOrder(orderId: number, payload: ServiceOrderActionPayload) {
  return postApi<ServiceOrderSummary>(`/service-orders/${orderId}/reject`, payload)
}

export function startServiceOrder(orderId: number, payload: ServiceOrderActionPayload) {
  return postApi<ServiceOrderSummary>(`/service-orders/${orderId}/start`, payload)
}

export function completeServiceOrder(orderId: number, payload: ServiceOrderActionPayload) {
  return postApi<ServiceOrderSummary>(`/service-orders/${orderId}/complete`, payload)
}

export function cancelServiceOrder(orderId: number, payload: ServiceOrderActionPayload) {
  return postApi<ServiceOrderSummary>(`/service-orders/${orderId}/cancel`, payload)
}

export function listProjectAds(params?: ProjectAdListParams) {
  const query = buildQuery(params)
  return getApi<ProjectAdSummary[]>(`/project-ads${query}`)
}

export function listFeaturedProjectAds() {
  return getApi<ProjectAdSummary[]>('/project-ads/featured')
}

export function getProjectAd(id: number, viewerId?: number) {
  const query = viewerId ? `?viewerId=${viewerId}` : ''
  return getApi<ProjectAdDetailSummary>(`/project-ads/${id}${query}`)
}

export function listUserProjectAds(userId: number) {
  return getApi<ProjectAdSummary[]>(`/project-ads/users/${userId}`)
}

export function createProjectAd(publisherId: number, payload: ProjectAdPayload) {
  return postApi<ProjectAdSummary>(`/project-ads?publisherId=${publisherId}`, payload)
}

export function updateProjectAd(id: number, publisherId: number, payload: ProjectAdPayload) {
  return putApi<ProjectAdSummary>(`/project-ads/${id}?publisherId=${publisherId}`, payload)
}

export function submitProjectAd(id: number, publisherId: number) {
  return postApi<ProjectAdSummary>(`/project-ads/${id}/submit?publisherId=${publisherId}`, {})
}

export function closeProjectAd(id: number, publisherId: number) {
  return postApi<ProjectAdSummary>(`/project-ads/${id}/close?publisherId=${publisherId}`, {})
}

export function listOpsProjectAds(status?: string) {
  const query = status ? `?status=${encodeURIComponent(status)}` : ''
  return getApi<ProjectAdSummary[]>(`/admin/ops/project-ads${query}`)
}

export function approveProjectAd(id: number, adminId: number, payload: ProjectAdReviewPayload) {
  return postApi<ProjectAdSummary>(`/admin/ops/project-ads/${id}/approve?adminId=${adminId}`, payload)
}

export function rejectProjectAd(id: number, adminId: number, payload: ProjectAdReviewPayload) {
  return postApi<ProjectAdSummary>(`/admin/ops/project-ads/${id}/reject?adminId=${adminId}`, payload)
}

export function featureProjectAd(id: number, adminId: number, payload: ProjectAdReviewPayload) {
  return postApi<ProjectAdSummary>(`/admin/ops/project-ads/${id}/feature?adminId=${adminId}`, payload)
}

export function unfeatureProjectAd(id: number, adminId: number) {
  return postApi<ProjectAdSummary>(`/admin/ops/project-ads/${id}/unfeature?adminId=${adminId}`, {})
}

export function blockProjectAd(id: number, adminId: number, payload: ProjectAdReviewPayload) {
  return postApi<ProjectAdSummary>(`/admin/ops/project-ads/${id}/block?adminId=${adminId}`, payload)
}

export function getGovernanceDashboard() {
  return getApi<GovernanceDashboardSummary>('/admin/governance/dashboard')
}

export function getGovernanceReports(params?: { status?: string; targetType?: string }) {
  return getApi<ReportRecordSummary[]>(`/admin/governance/reports${buildQuery(params)}`)
}

export function startReportReview(reportId: number, adminId: number, payload: GovernanceActionPayload) {
  return postApi<ReportRecordSummary>(`/admin/governance/reports/${reportId}/start-review?adminId=${adminId}`, payload)
}

export function rejectReport(reportId: number, adminId: number, payload: GovernanceActionPayload) {
  return postApi<ReportRecordSummary>(`/admin/governance/reports/${reportId}/reject?adminId=${adminId}`, payload)
}

export function resolveReport(reportId: number, adminId: number, payload: GovernanceActionPayload) {
  return postApi<ReportRecordSummary>(`/admin/governance/reports/${reportId}/resolve?adminId=${adminId}`, payload)
}

export function escalateReport(reportId: number, adminId: number, payload: GovernanceActionPayload) {
  return postApi<ReportRecordSummary>(`/admin/governance/reports/${reportId}/escalate?adminId=${adminId}`, payload)
}

export function createViolation(adminId: number, payload: CreateViolationPayload) {
  return postApi<ViolationRecordSummary>(`/admin/governance/violations?adminId=${adminId}`, payload)
}

export function getAdminActionLogs() {
  return getApi<AdminActionLogSummary[]>('/admin/governance/audit-logs')
}

export function getCreditCenter(userId: number) {
  return getApi<CreditCenterSummary>(`/credit/users/${userId}`)
}

function buildQuery(params?: object) {
  if (!params) {
    return ''
  }
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value))
    }
  })
  const value = search.toString()
  return value ? `?${value}` : ''
}

export function getWallet(userId = 1) {
  return getApi<WalletAccountSummary>(`/wallet/users/${userId}`)
}

export function listWalletFlows(userId = 1) {
  return getApi<WalletFlowSummary[]>(`/wallet/users/${userId}/flows`)
}

export function createWalletRecharge(userId: number, payload: { channel: string; amount: number; remark?: string }) {
  return postApi<WalletRechargeSummary>(`/wallet/users/${userId}/recharges`, payload)
}

export function listWalletRecharges(userId = 1) {
  return getApi<WalletRechargeSummary[]>(`/wallet/users/${userId}/recharges`)
}

export function createWalletWithdrawal(userId: number, payload: { amount: number; channel: string; accountSnapshot?: string }) {
  return postApi<WalletWithdrawalSummary>(`/wallet/users/${userId}/withdrawals`, payload)
}

export function listWalletWithdrawals(userId = 1) {
  return getApi<WalletWithdrawalSummary[]>(`/wallet/users/${userId}/withdrawals`)
}

export function listWalletFrozenItems(userId = 1) {
  return getApi<WalletFrozenRecordSummary[]>(`/wallet/users/${userId}/frozen-items`)
}

export function listServiceFees(userId = 1) {
  return getApi<ServiceFeeSummary[]>(`/payment/users/${userId}/service-fees`)
}

export function createServiceFeePayment(feeId: number) {
  return postApi<PaymentCreation>(`/payment/service-fees/${feeId}/pay`, {})
}

export function getPaymentOrder(orderNo: string) {
  return getApi<PaymentOrderSummary>(`/payment/orders/${orderNo}`)
}

export function createRoleDepositPayment(applicationId: number) {
  return postApi<PaymentCreation>(`/identity/roles/${applicationId}/deposit-pay`, {})
}

export function listAdminWalletRecharges(status?: string) {
  const query = status ? `?status=${encodeURIComponent(status)}` : ''
  return getApi<WalletRechargeSummary[]>(`/admin/wallet/recharges${query}`)
}

export function approveAdminWalletRecharge(id: number, adminId: number, note: string) {
  return postApi<WalletRechargeSummary>(`/admin/wallet/recharges/${id}/approve?adminId=${adminId}&note=${encodeURIComponent(note)}`, {})
}

export function rejectAdminWalletRecharge(id: number, adminId: number, note: string) {
  return postApi<WalletRechargeSummary>(`/admin/wallet/recharges/${id}/reject?adminId=${adminId}&note=${encodeURIComponent(note)}`, {})
}

export function listAdminWalletWithdrawals(status?: string) {
  const query = status ? `?status=${encodeURIComponent(status)}` : ''
  return getApi<WalletWithdrawalSummary[]>(`/admin/wallet/withdrawals${query}`)
}

export function approveAdminWalletWithdrawal(id: number, adminId: number, note: string) {
  return postApi<WalletWithdrawalSummary>(`/admin/wallet/withdrawals/${id}/approve?adminId=${adminId}&note=${encodeURIComponent(note)}`, {})
}

export function completeAdminWalletWithdrawal(id: number, adminId: number, note: string) {
  return postApi<WalletWithdrawalSummary>(`/admin/wallet/withdrawals/${id}/complete?adminId=${adminId}&note=${encodeURIComponent(note)}`, {})
}

export function rejectAdminWalletWithdrawal(id: number, adminId: number, note: string) {
  return postApi<WalletWithdrawalSummary>(`/admin/wallet/withdrawals/${id}/reject?adminId=${adminId}&note=${encodeURIComponent(note)}`, {})
}

export function listAdminPaymentOrders(status?: string) {
  const query = status ? `?status=${encodeURIComponent(status)}` : ''
  return getApi<PaymentOrderSummary[]>(`/admin/payment/orders${query}`)
}

export function listAdminPaymentCallbackEvents() {
  return getApi<PaymentCallbackEventSummary[]>('/admin/payment/callback-events')
}

export function createMockServiceFeePayment(feeId: number) {
  return postApi<PaymentCreation>(`/payment/service-fees/${feeId}/mock-pay`, {})
}

export function markMockServiceFeeSuccess(feeId: number) {
  return postApi<PaymentStatus>(`/payment/service-fees/${feeId}/mock-success`, {})
}

export function listReviewRecords() {
  return getApi<ReviewRecordSummary[]>('/moderation/reviews')
}

export function listReportRecords() {
  return getApi<ReportRecordSummary[]>('/moderation/reports')
}
