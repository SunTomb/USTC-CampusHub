const zones = [
  ['CENTRAL', '中校区'], ['WEST', '西校区'], ['EAST', '东校区'], ['NORTH', '北校区'], ['SOUTH', '南校区'],
  ['HIGH_TECH', '高新校区'], ['ADVANCED_RESEARCH_INSTITUTE', '先研院'], ['SCIENCE_ISLAND', '科学岛'], ['OTHER', '其他校区'],
].map(([value, label]) => ({ value, label }));
const categories = [['', '全部服务'], ['PHOTO', '摄影约拍'], ['REPAIR', '电脑维修'], ['FILM', '数码贴膜'], ['MAKEUP', '妆造'], ['TUTORING', '辅导答疑'], ['DESIGN', '设计排版'], ['OTHER', '其他服务']].map(([value, label]) => ({ value, label }));
const adTypes = [['TEAM_UP', '组队招募'], ['PORTFOLIO', '作品展示'], ['CLUB_RECRUITMENT', '社团纳新'], ['CAMPUS_EVENT', '校园活动'], ['OTHER', '其他宣传']].map(([value, label]) => ({ value, label }));
const roles = [['RUNNER', '顺路跑腿员', '跑', false], ['GOODS_PUBLISHER', '闲置发布者', '闲', false], ['SHOP_MERCHANT', '技能店主理人', '店', true]].map(([value, label, seal, review]) => ({ value, label, seal, review }));
const eventTypes = {
  CREATED: '已发布', TASK_CREATED: '已发布', PUBLISHED: '已发布', GRABBED: '已抢单', TASK_GRABBED: '已抢单', ACCEPTED: '已接单', TASK_ACCEPTED: '已接单', APPLIED: '已报名', TASK_APPLIED: '已报名', APPLICATION_ACCEPTED: '已选定接单人', HEADING_TO_PICKUP: '前往取件', PICKED_UP: '已取件', DELIVERING: '送达途中',
  DELIVERED: '已送达', PENDING_CONFIRMATION: '等待确认', COMPLETED: '已完成', COMPLETED_WITH_CODE: '完成码已核销', COMPLETION_CONFIRMED: '已确认完成', PHOTO_SUBMITTED: '已提交送达确认', STATUS_CHANGED: '进度已更新', ISSUE_REPORTED: '已反馈问题', ISSUE_HANDLING: '问题处理中',
  COMPLETION_CODE_GENERATED: '已生成完成码', COMPLETE_CODE_GENERATED: '已生成完成码', COMPLETION_CODE_REDEEMED: '完成码已核销', COMPLETE_CODE_REDEEMED: '完成码已核销', WORKFLOW_UPDATED: '进度已更新',
};
const states = {
  PUBLISHED: '待接单', PENDING: '等待选择', ACCEPTED: '已接单', HEADING_TO_PICKUP: '前往取件', PICKED_UP: '已取件', DELIVERING: '送达途中',
  DELIVERED: '已送达', PENDING_CONFIRMATION: '等待确认', COMPLETED: '已完成', CANCELLED: '已取消', CANCELED: '已取消',
  ISSUE_HANDLING: '问题处理中', DISPUTE_HANDLING: '协调处理中', REQUESTED: '等待店主确认', IN_SERVICE: '服务进行中',
  PENDING_REVIEW: '等待站务核验', APPROVED: '已通过', REJECTED: '未通过', DRAFT: '草稿', CLOSED: '已关闭', BLOCKED: '已屏蔽', EXPIRED: '已过期', PAUSED: '已暂停', OFF_SHELF: '已下架', ACTIVE: '正常', CONSENTED: '已同意协议', LEGACY: '历史身份',
};
function label(options, value, fallback) { return (options.find(item => item.value === value) || {}).label || fallback; }
function zone(value) { return value ? label(zones, value, '其他校区') : '全校'; }
function status(value) { return states[value] || '状态待更新'; }
function money(value) { if (value == null || value === '') return '待协商'; const n = Number(value); return Number.isFinite(n) ? n.toFixed(2).replace(/\.00$/, '') : '待协商'; }
function dateText(value) { return typeof value === 'string' ? value.replace('T', ' ').slice(5, 16) : ''; }
function localMillis(value) {
  if (typeof value !== 'string') return NaN;
  const parts = value.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/);
  if (!parts) return NaN;
  const year = +parts[1], month = +parts[2], day = +parts[3], hour = +parts[4], minute = +parts[5];
  const date = new Date(year, month - 1, day, hour, minute);
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day && date.getHours() === hour && date.getMinutes() === minute ? date.getTime() : NaN;
}
function dateParts(offset = 3600000) {
  const d = new Date(Date.now() + offset); const pad = n => String(n).padStart(2, '0');
  return { date: d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()), time: pad(d.getHours()) + ':' + pad(d.getMinutes()) };
}
function hasRole(user, role) { return !!user && Array.isArray(user.roles) && user.roles.some(value => value === role || value === 'ROLE_' + role); }
function tags(value) { return String(value || '').split(/[,，]/).map(x => x.trim()).filter(Boolean).slice(0, 3); }
function eventType(value) { return eventTypes[value] || status(value); }
function dateTimeText(value) { return typeof value === 'string' ? value.replace('T', ' ').slice(0, 16) : ''; }
function contactView(value) {
  if (!value) return null;
  const items = [];
  if (value.wechatContact) items.push({ label: '微信', value: String(value.wechatContact) });
  if (value.qqContact) items.push({ label: 'QQ', value: String(value.qqContact) });
  return { ...value, items };
}
function eventView(value) { return { ...value, eventTypeLabel: eventType(value.eventType), createdAtLabel: dateTimeText(value.createdAt) }; }
function weightLabel(grams) { return !Number.isInteger(grams) || grams < 1 ? '未填写' : grams < 1000 ? grams + ' 克' : (grams / 1000) + ' 公斤'; }
function taskView(task) { return { ...task, weightLabel: weightLabel(task.weightGrams), originLabel: zone(task.originZone), destinationLabel: zone(task.destinationZone), rewardLabel: money(task.rewardAmount), deadlineLabel: dateText(task.deadline), statusLabel: status(task.workflowStatus || task.status), modeLabel: task.acceptanceMode === 'APPLICATION' ? '报名筛选' : '即时抢单', verificationModeLabel: task.verificationMode === 'COMPLETION_CODE' ? '完成码确认' : '发布者确认', expired: Number.isFinite(localMillis(task.deadline)) && localMillis(task.deadline) <= Date.now() }; }
function projectView(item) { return { ...item, typeLabel: label(adTypes, item.adType, '校园宣传'), statusLabel: status(item.status), tagList: tags(item.tags), zoneLabel: zone(item.campusZone), expiresLabel: dateText(item.expiresAt), initial: String(item.title || '同').slice(0, 1) }; }
function serviceView(item) { return { ...item, categoryLabel: label(categories, item.category, '其他服务'), priceLabel: item.minPrice != null && item.maxPrice != null ? money(item.minPrice) + '–' + money(item.maxPrice) : money(item.price) }; }
function shopView(item, services = []) { return { ...item, zoneLabel: zone(item.campusZone), initial: String(item.name || '店').slice(0, 1), previewServices: services.filter(s => s.shopId === item.id).slice(0, 2).map(serviceView) }; }
function contacts(value, visible) {
  if (!visible || !value) return [];
  const text = String(value); const list = []; const wx = text.match(/微信\s*[:：]\s*([^\s]+)/); const qq = text.match(/QQ\s*[:：]\s*([^\s]+)/i);
  if (wx) list.push({ label: '复制微信号', value: wx[1] });
  if (qq) list.push({ label: '复制 QQ 号', value: qq[1] });
  return list.length ? list : [{ label: '复制联系方式', value: text }];
}
module.exports = { weightLabel, zones, categories, adTypes, roles, zone, status, eventType, money, dateText, dateTimeText, localMillis, dateParts, hasRole, tags, taskView, contactView, eventView, projectView, serviceView, shopView, contacts };
