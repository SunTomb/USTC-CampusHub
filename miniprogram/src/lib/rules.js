const { localMillis } = require('./display');
function required(value, label, max = 2000) {
  const text = String(value || '').trim();
  if (!text) throw new Error('请填写' + label);
  if (text.length > max) throw new Error(label + '不能超过' + max + '字');
  return text;
}
function amount(value) {
  if (!/^\d+(\.\d{1,2})?$/.test(String(value))) throw new Error('请填写有效金额，最多两位小数');
  const n = Number(value);
  if (!Number.isFinite(n) || n < 0 || n > 99999) throw new Error('金额应在 0 至 99999 元之间');
  return n;
}
function future(date, time) {
  const value = date + 'T' + time + ':00';
  const timestamp = localMillis(value);
  if (!Number.isFinite(timestamp) || timestamp <= Date.now()) throw new Error('请选择未来的时间');
  return value;
}
function username(value) { const text = String(value || '').trim(); if (!/^[a-zA-Z0-9_]{3,64}$/.test(text)) throw new Error('用户名需为 3–64 位字母、数字或下划线'); return text; }
function password(value) { if (typeof value !== 'string' || value.length < 8 || value.length > 72) throw new Error('密码需为 8–72 位字符'); return value; }
function email(value) { const text = String(value || '').trim().toLowerCase(); if (!/^[^@\s]+@[^@\s]+\.edu\.cn$/.test(text) && !/^[^@\s]+@edu\.cn$/.test(text)) throw new Error('请填写 edu.cn 校园邮箱'); return text; }
function completionCode(value) {
  const text = String(value || '').trim();
  if (!/^\d{6}$/.test(text)) throw new Error('请输入6位完成码');
  return text;
}
function nextTaskAction(task, isAssigned) {
  if (!isAssigned) return null;
  const next = { ACCEPTED: ['HEADING_TO_PICKUP', '出发取件'], HEADING_TO_PICKUP: ['PICKED_UP', '确认已取件'], PICKED_UP: ['DELIVERING', '开始配送'], DELIVERING: ['PENDING_CONFIRMATION', '已送达，请对方确认'], DELIVERED: ['PENDING_CONFIRMATION', '请发布者确认'] }[task.workflowStatus];
  return next ? { status: next[0], label: next[1] } : null;
}
function orderActions(order, userId) {
  const provider = order.providerId === userId, customer = order.customerId === userId;
  if (!provider && !customer) return [];
  const actions = [];
  if (provider && order.status === 'REQUESTED') actions.push({ value: 'accept', label: '接受预约' }, { value: 'reject', label: '婉拒预约' });
  if (provider && order.status === 'ACCEPTED') actions.push({ value: 'start', label: '开始服务' });
  if (order.status === 'IN_SERVICE') actions.push({ value: 'complete', label: '确认完成' });
  if (['REQUESTED', 'ACCEPTED'].includes(order.status)) actions.push({ value: 'cancel', label: '取消预约' });
  return actions;
}
module.exports = { required, amount, future, username, password, email, completionCode, nextTaskAction, orderActions };
