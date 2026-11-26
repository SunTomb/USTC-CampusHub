const { createGoodsApi } = require('./goods-api');
function createApi(client) {
  const id = value => { if (!/^\d+$/.test(String(value)) || Number(value) < 1) throw new Error('记录编号无效'); return String(value); };
  return {
    ...createGoodsApi(client),
    updateTask: (taskId,data)=>client.put('/tasks/'+id(taskId),data),
    withdrawTask: taskId=>client.delete('/tasks/'+id(taskId)),
    orderDesk: query=>client.get('/orders/mine',query),
    deskDetail: (kind,rowId)=>client.get('/orders/'+kind+'/'+id(rowId)),
    orderReview: (kind,rowId,data)=>client.post('/orders/'+kind+'/'+id(rowId)+'/review',data),
    orderComplaint: (kind,rowId,data)=>client.post('/orders/'+kind+'/'+id(rowId)+'/complaint',data),
    bookingRecord: rowId=>client.get('/service-orders/'+id(rowId)),
    serviceItem: itemId=>client.get('/service-items/'+id(itemId)),
    itemRatings: (itemId,page)=>client.get('/service-items/'+id(itemId)+'/ratings',{page}),
    reportContent: data=>client.post('/moderation/reports',data),
    features: () => client.get('/platform/features'),
    me: () => client.get('/auth/me', null, { quiet401: true }),
    login: data => client.post('/auth/login', data),
    registerCode: email => client.post('/auth/register/send-code', { email }),
    register: data => client.post('/auth/register', data),
    profile: userId => client.get('/users/' + id(userId)),
    saveProfile: data => client.put('/users/me/profile', data),
    saveContacts: data => client.put('/users/me/contacts', data),
    password: data => client.put('/users/me/password', data),
    tasks: () => client.get('/tasks'),
    myPublishedTasks: () => client.get('/tasks/mine'),
    task: taskId => client.get('/tasks/' + id(taskId)),
    taskWorkspace: taskId => client.get('/tasks/' + id(taskId) + '/workspace'),
    removeTaskPhoto: (taskId, photoId) => client.delete('/tasks/' + id(taskId) + '/evidence/' + id(photoId)),
    submitEvidence: taskId => client.post('/tasks/' + id(taskId) + '/evidence/submit', {}),
    generateCompletionCode: taskId => client.post('/tasks/' + id(taskId) + '/completion-code', {}),
    completeWithCode: (taskId, completionCode) => {
      if (!/^\d{6}$/.test(String(completionCode || ''))) throw new Error('请输入6位完成码');
      return client.post('/tasks/' + id(taskId) + '/complete-code', { completionCode: String(completionCode) });
    },
    publishTask: data => client.post('/tasks', data),
    grab: taskId => client.post('/tasks/' + id(taskId) + '/grab'),
    applyTask: (taskId, message) => client.post('/tasks/' + id(taskId) + '/applications', { message }),
    taskApplicants: taskId => client.get('/task-applications/task/' + id(taskId)),
    myTaskApplications: userId => client.get('/task-applications/applicant/' + id(userId)),
    acceptApplicant: (taskId, applicationId) => client.post('/tasks/' + id(taskId) + '/applications/' + id(applicationId) + '/accept'),
    advanceTask: (taskId, status) => {
      if (!['HEADING_TO_PICKUP', 'PICKED_UP', 'DELIVERING', 'PENDING_CONFIRMATION'].includes(status)) throw new Error('不支持的履约操作');
      return client.post('/tasks/' + id(taskId) + '/workflow/' + status, {});
    },
    confirmTask: taskId => client.post('/tasks/' + id(taskId) + '/confirm', {}),
    taskIssue: (taskId, data) => client.post('/tasks/' + id(taskId) + '/issues', data),
    shops: () => client.get('/shops'),
    shop: shopId => client.get('/shops/' + id(shopId)),
    services: () => client.get('/service-items'),
    book: (itemId, data) => client.post('/service-items/' + id(itemId) + '/orders', data),
    customerOrders: userId => client.get('/service-orders/customer/' + id(userId)),
    providerOrders: userId => client.get('/service-orders/provider/' + id(userId)),
    orderAction: (orderId, action, cancelReason) => {
      if (!['accept', 'reject', 'start', 'complete', 'cancel'].includes(action)) throw new Error('不支持的预约操作');
      return client.post('/service-orders/' + id(orderId) + '/' + action, { cancelReason });
    },
    showcase: query => client.get('/project-ads', query),
    featured: () => client.get('/project-ads/featured'),
    project: projectId => client.get('/project-ads/' + id(projectId)),
    myProjects: userId => client.get('/project-ads/users/' + id(userId)),
    createProject: data => client.post('/project-ads', data),
    updateProject: (projectId, data) => client.put('/project-ads/' + id(projectId), data),
    submitProject: projectId => client.post('/project-ads/' + id(projectId) + '/submit'),
    closeProject: projectId => client.post('/project-ads/' + id(projectId) + '/close'),
    comments: projectId => client.get('/interactions/PROJECT_AD/' + id(projectId) + '/comments'),
    comment: (projectId, content) => client.post('/interactions/comments', { targetType: 'PROJECT_AD', targetId: Number(id(projectId)), content }),
    favorite: (projectId, remove) => client[remove ? 'delete' : 'post']('/interactions/favorites', { targetType: 'PROJECT_AD', targetId: Number(id(projectId)) }),
    protocol: role => {
      if (!['USER_SERVICE', 'RUNNER', 'GOODS_PUBLISHER', 'SHOP_MERCHANT'].includes(role)) throw new Error('不支持的身份协议');
      return client.get('/platform/protocols/' + role);
    },
    applications: userId => client.get('/identity/users/' + id(userId) + '/roles'),
    openRole: (userId, role, version) => {
      if (!['RUNNER', 'GOODS_PUBLISHER', 'SHOP_MERCHANT'].includes(role)) throw new Error('不支持的身份');
      return client.post('/identity/users/' + id(userId) + '/roles', { roleType: role, agreementVersion: version, agreementAccepted: true });
    },
    notifications: userId => client.get('/users/' + id(userId) + '/notifications'),
    readAllNotifications: () => client.post('/notifications/read-all'),
    readNotification: notificationId => client.post('/notifications/' + id(notificationId) + '/read'),
  };
}
module.exports = { createApi };
