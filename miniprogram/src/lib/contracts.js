/**
 * @template T
 * @typedef {{success: boolean, message: string, data: T}} ApiEnvelope
 */
/** @typedef {{id:number, username:string, nickname:string, email?:string, wechatContact?:string, qqContact?:string, roles:string[]}} CurrentUser */
/** @typedef {{id:number, title:string, description:string, publisherId:number, publisherNickname:string, rewardAmount:number, deadline:string, acceptanceMode:string, originZone:string, destinationZone:string, originDetail?:string, destinationDetail?:string, workflowStatus:string, acceptedApplicationId?:number}} RunnerTask */
/** @typedef {{id:number, taskId:number, taskTitle:string, applicantId:number, applicantNickname:string, status:string, message?:string, createdAt?:string}} TaskApplication */
/** @typedef {{agreementCode:string, version:string, title:string, content:string, effectiveAt:string, acceptedAt?:string}} RoleProtocol */
/** @typedef {{id:number, userId:number, roleType:string, reviewStatus:string, agreementStatus:string, agreementVersion?:string, agreementConsentedAt?:string}} RoleApplication */
/** @typedef {{id:number, name:string, description:string, ownerId:number, ownerNickname:string, campusZone:string, openingHours?:string, status:string}} Shop */
/** @typedef {{id:number, shopId:number, title:string, description:string, category:string, price:number, minPrice?:number, maxPrice?:number, priceUnit:string, durationMinutes:number, status:string}} ServiceItem */
/** @typedef {{id:number, shopId:number, shopName:string, serviceItemTitle:string, customerId:number, providerId:number, status:string, appointmentTime:string, amount:number, note?:string, contactSnapshot?:string, cancelReason?:string}} ServiceOrder */
/** @typedef {{id:number, title:string, adType:string, publisherId:number, status:string, description:string, summary?:string, tags?:string, campusZone?:string, contactVisible:boolean, contactInfo?:string, contactVisibility:string, favoriteCount:number, commentCount:number, favorited:boolean}} ProjectDetail */
/** @typedef {{id:number, recipientId:number, title:string, content:string, targetType:string, targetId:number, readAt?:string, createdAt:string}} StationNotification */
module.exports = {};
