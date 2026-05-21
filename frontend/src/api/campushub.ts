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

export interface ServiceFeeSummary {
  id: number
  feeNo: string
  payerId: number
  payerNickname: string
  targetType: string
  targetId: number
  amount: number
  status: string
  createdAt: string
  paidAt: string | null
}

export interface PaymentCreation {
  provider: string
  tradeNo: string
  paymentUrl: string
  status: string
  message: string
}

export interface PaymentStatus {
  provider: string
  tradeNo: string
  status: string
  message: string
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

export function listProjectAds() {
  return getApi<ProjectAdSummary[]>('/project-ads')
}

export function getWallet(userId = 1) {
  return getApi<WalletAccountSummary>(`/wallet/users/${userId}`)
}

export function listWalletFlows(userId = 1) {
  return getApi<WalletFlowSummary[]>(`/wallet/users/${userId}/flows`)
}

export function listServiceFees(userId = 1) {
  return getApi<ServiceFeeSummary[]>(`/payment/users/${userId}/service-fees`)
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
