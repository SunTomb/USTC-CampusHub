const test = require('node:test');
const assert = require('node:assert/strict');
const { createClient, ApiError } = require('../src/lib/request');
const { createSession } = require('../src/lib/session');

function harness() {
  let token = 'session-a';
  const pending = [];
  const client = createClient({
    baseUrl: 'https://example.edu.cn/api',
    request: options => pending.push(options),
    getToken: () => token,
    onUnauthorized: sentToken => { if (sentToken === token) token = null; },
  });
  return { client, pending, token: () => token, setToken: value => { token = value; } };
}

test('unwraps API data, adds bearer header and encodes query', async () => {
  const h = harness();
  const p = h.client.get('/tasks', { campusZone: 'CENTRAL', keyword: '取件 & 返回' });
  assert.equal(h.pending[0].header.Authorization, 'Bearer session-a');
  assert(h.pending[0].url.includes('keyword=%E5%8F%96%E4%BB%B6%20%26%20%E8%BF%94%E5%9B%9E'));
  h.pending[0].success({ statusCode: 200, data: { success: true, data: [1] } });
  assert.deepEqual(await p, [1]);
});

test('network failures keep sessions; 403 is permission failure not signout', async () => {
  const h = harness();
  const p = h.client.get('/auth/me');
  h.pending.shift().fail({ errMsg: 'request:fail timeout' });
  await assert.rejects(p, e => e.network === true && !e.message.includes('request:fail'));
  assert.equal(h.token(), 'session-a');
  const forbidden = h.client.post('/tasks/1/grab', {});
  h.pending.shift().success({ statusCode: 403, data: { message: '禁止' } });
  await assert.rejects(forbidden, e => e.status === 403);
  assert.equal(h.token(), 'session-a');
});

test('clears only matching 401 session, and rejects absolute/administrative/financial routes', async () => {
  const h = harness();
  const p = h.client.get('/auth/me');
  h.setToken('session-b');
  h.pending.shift().success({ statusCode: 401, data: {} });
  await assert.rejects(p, e => e.status === 401);
  assert.equal(h.token(), 'session-b');
  for (const path of ['https://evil.example/', '//evil.example/', '/admin/ops', '/wallet/users/1', '/payment/orders/1', '/goods']) {
    await assert.rejects(h.client.get(path), /不支持|请求路径/);
  }
  assert.equal(h.pending.length, 0);
});

test('malformed and business-error responses never become successful data', async () => {
  const h = harness();
  const p = h.client.get('/tasks');
  h.pending.shift().success({ statusCode: 200, data: { success: false, message: '任务已被领取' } });
  await assert.rejects(p, /任务已被领取/);
  const q = h.client.get('/tasks');
  h.pending.shift().success({ statusCode: 200, data: '<html>gateway</html>' });
  await assert.rejects(q, /响应/);
});

test('session restore preserves token on outage, but does not expose old user', async () => {
  let stored = 'a';
  const session = createSession({ read: () => stored, write: v => { stored = v; }, me: () => Promise.reject(new ApiError('网络暂不可用', 0, true)) });
  await session.restore();
  assert.equal(stored, 'a');
  assert.equal(session.user, null);
  assert.equal(session.authenticated, false);
  assert(session.error);
});

test('late restore cannot overwrite new login or resurrect signout', async () => {
  let done;
  const session = createSession({ read: () => 'a', write: () => {}, me: () => new Promise(resolve => { done = resolve; }) });
  const load = session.restore();
  session.login('b', { id: 2, roles: ['ROLE_STUDENT'] });
  done({ id: 1, roles: ['ROLE_STUDENT'] });
  await load;
  assert.equal(session.user.id, 2);
  const second = session.restore(true);
  session.logout();
  done({ id: 2, roles: ['ROLE_STUDENT'] });
  await second;
  assert.equal(session.user, null);
  assert.equal(session.token, null);
});

test('overlapping restore deduplicates and 401 invalidates verified session', async () => {
  let reject, calls = 0;
  const session = createSession({ read: () => 'a', write: () => {}, me: () => { calls++; return new Promise((_, fail) => { reject = fail; }); } });
  const a = session.restore(); const b = session.restore();
  assert.equal(calls, 1);
  reject(new ApiError('请重新登录', 401));
  await Promise.all([a, b]);
  assert.equal(session.token, null);
});
