const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { createRequire } = require('node:module');

function component() {
  const filename = path.resolve(__dirname, '../src/components/role-unlock/index.js');
  let definition;
  const pending = [];
  const events = [];
  const auth = { token: 'a', user: { id: 1, roles: ['ROLE_STUDENT'] }, restore: async function () { return this.user; }, login(token, user) { this.token = token; this.user = user; } };
  const api = { protocol: () => new Promise(resolve => pending.push(resolve)), openRole: async () => ({ reviewStatus: 'APPROVED' }) };
  const runtime = { Component: value => { definition = value; }, require: createRequire(filename), getApp: () => ({ auth, api }), wx: { showToast() {}, vibrateShort() {} }, Set };
  vm.runInNewContext(fs.readFileSync(filename, 'utf8'), runtime);
  const instance = { ...definition.methods, properties: { visible: true, role: 'RUNNER' }, data: { ...definition.data }, _alive: true, setData(value) { Object.assign(this.data, value); }, triggerEvent(name, detail) { events.push({ name, detail }); } };
  return { instance, definition, pending, events, auth, api };
}

test('agreement content/version comes from the server and reload resets consent', async () => {
  const h = component();
  h.instance.data.accepted = true;
  const result = h.instance.reload();
  assert.equal(h.instance.data.accepted, false);
  h.pending[0]({ version: 'server-current', title: '当前协议', content: '完整正文' });
  await result;
  assert.equal(h.instance.data.protocol.version, 'server-current');
});

test('unchecked agreement never submits; deliberate acceptance submits the exact version', async () => {
  const h = component(); let calls = 0; let args;
  h.api.openRole = async (...values) => { calls++; args = values; return { reviewStatus: 'APPROVED' }; };
  h.instance.data.protocol = { version: 'latest-from-server' };
  await h.instance.submit(); assert.equal(calls, 0);
  h.instance.accept({ detail: { value: ['agree'] } });
  await h.instance.submit(); assert.equal(calls, 1);
  assert.deepEqual(args, [1, 'RUNNER', 'latest-from-server']);
  assert(h.auth.user.roles.includes('ROLE_RUNNER'));
});

test('stale-version errors uncheck consent and do not close the sheet', async () => {
  const h = component();
  h.api.openRole = async () => { throw new Error('协议版本已更新'); };
  h.instance.data.protocol = { version: 'old' }; h.instance.data.accepted = true;
  await h.instance.submit();
  assert.equal(h.instance.data.accepted, false);
  assert(h.instance.data.error.includes('版本'));
  assert.equal(h.events.some(e => e.name === 'close'), false);
});

test('rapid agreement taps make only one write while submission is pending', async () => {
  const h = component(); let calls = 0, resolve;
  h.api.openRole = () => { calls++; return new Promise(done => { resolve = done; }); };
  h.instance.data.protocol = { version: 'current' }; h.instance.data.accepted = true;
  const first = h.instance.submit(); const second = h.instance.submit();
  await new Promise(done => setImmediate(done));
  assert.equal(calls, 1);
  resolve({ reviewStatus: 'APPROVED' }); await Promise.all([first, second]);
});

test('merchant agreement waits for review without granting role locally', async () => {
  const h = component();
  h.instance.properties.role = 'SHOP_MERCHANT';
  h.api.openRole = async () => ({ reviewStatus: 'PENDING_REVIEW' });
  h.instance.data.protocol = { version: 'current' }; h.instance.data.accepted = true;
  await h.instance.submit();
  assert(!h.auth.user.roles.includes('ROLE_SHOP_MERCHANT'));
  assert(h.events.some(e => e.name === 'opened' && e.detail.application.reviewStatus === 'PENDING_REVIEW'));
});
