const test = require('node:test');
const assert = require('node:assert/strict');
const { configFor, PRODUCTION } = require('../src/lib/config');
const d = require('../src/lib/display');
const rules = require('../src/lib/rules');
const { createApi } = require('../src/lib/api');

test('release ignores local API override and development refuses arbitrary hosts', () => {
  assert.equal(configFor({ getAccountInfoSync: () => ({ miniProgram: { envVersion: 'release' } }), getStorageSync: () => 'http://127.0.0.1:28180/api' }).baseUrl, PRODUCTION);
  assert.equal(configFor({ getAccountInfoSync: () => ({ miniProgram: { envVersion: 'develop' } }), getStorageSync: () => 'https://evil.example/api' }).baseUrl, PRODUCTION);
  assert.equal(configFor({ getAccountInfoSync: () => ({ miniProgram: { envVersion: 'develop' } }), getStorageSync: () => 'http://127.0.0.1:28180/api' }).baseUrl, 'http://127.0.0.1:28180/api');
});

test('state/zone fallback never exposes an unknown machine enum', () => {
  assert.equal(d.status('IN_SERVICE'), '服务进行中');
  assert.equal(d.eventType('GRABBED'), '已抢单');
  assert.equal(d.eventType('COMPLETED_WITH_CODE'), '完成码已核销');
  assert.equal(d.zone('SCIENCE_ISLAND'), '科学岛');
  assert.equal(d.status('NEW_INTERNAL_STATE'), '状态待更新');
  assert.equal(d.zone('NEW_INTERNAL_ZONE'), '其他校区');
});

test('private contacts require the explicit visibility flag', () => {
  assert.deepEqual(d.contacts('微信:private QQ:123', false), []);
  assert.deepEqual(d.contacts('微信:student_1 QQ:123', true).map(c => c.value), ['student_1', '123']);
});

test('task and booking action controls restrict participants and transitions', () => {
  assert.equal(rules.nextTaskAction({ workflowStatus: 'COMPLETED' }, true), null);
  assert.equal(rules.nextTaskAction({ workflowStatus: 'DELIVERING' }, false), null);
  assert.equal(rules.nextTaskAction({ workflowStatus: 'DELIVERING' }, true).status, 'PENDING_CONFIRMATION');
  const order = { status: 'REQUESTED', providerId: 1, customerId: 2 };
  assert.deepEqual(rules.orderActions(order, 3), []);
  assert.deepEqual(rules.orderActions(order, 2).map(a => a.value), ['cancel']);
  assert(rules.orderActions(order, 1).some(a => a.value === 'accept'));
  assert.deepEqual(rules.orderActions({ ...order, status: 'COMPLETED' }, 1), []);
});

test('completion-code APIs use private task routes and reject malformed codes', () => {
  const calls = [];
  const client = Object.fromEntries(['get','post','put','delete'].map(method => [method, (...args) => calls.push({ method, args })]));
  const api = createApi(client);
  api.openRole(12, 'RUNNER', 'current-server-version');
  api.taskWorkspace(12);
  api.generateCompletionCode(12);
  api.completeWithCode(12, '123456');
  assert.deepEqual(calls[0], { method: 'post', args: ['/identity/users/12/roles', { roleType: 'RUNNER', agreementVersion: 'current-server-version', agreementAccepted: true }] });
  assert.deepEqual(calls[1], { method: 'get', args: ['/tasks/12/workspace'] });
  assert.deepEqual(calls[2], { method: 'post', args: ['/tasks/12/completion-code', {}] });
  assert.deepEqual(calls[3], { method: 'post', args: ['/tasks/12/complete-code', { completionCode: '123456' }] });
  assert.throws(() => api.openRole(12, 'MASTER_ADMIN', '1'), /不支持/);
  assert.throws(() => api.advanceTask(1, 'COMPLETED'), /不支持/);
  assert.throws(() => api.completeWithCode(1, '12345'), /完成码/);
  assert.throws(() => api.task('1/../../admin'), /编号/);
});

test('invalid date parts and absent prices are not silently converted to valid values', () => {
  assert(Number.isNaN(d.localMillis('2099-13-01T12:00:00')));
  assert(Number.isNaN(d.localMillis('2099-02-30T12:00:00')));
  assert(Number.isNaN(d.localMillis('2099-02-20T25:00:00')));
  assert.equal(d.money(null), '待协商');
  assert.equal(d.money(undefined), '待协商');
});

test('validates campus emails, names, amounts and future local date-times', () => {
  assert.equal(rules.email(' Student@MAIL.USTC.EDU.CN '), 'student@mail.ustc.edu.cn');
  assert.throws(() => rules.email('user@edu.cn.evil.example'));
  assert.throws(() => rules.username('a b'));
  assert.throws(() => rules.amount('-2'));
  assert.throws(() => rules.amount('1.001'));
  assert.equal(rules.amount('12.30'), 12.3);
  assert.throws(() => rules.future('2000-01-01', '12:00'));
  const parts = d.dateParts(86400000);
  assert.equal(rules.future(parts.date, parts.time), parts.date + 'T' + parts.time + ':00');
});
