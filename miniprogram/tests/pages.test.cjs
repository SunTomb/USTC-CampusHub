const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const ui = require('../src/lib/ui');

function pageHarness(file, api, user = { id: 1, username: 'fixture', roles: ['ROLE_STUDENT', 'ROLE_RUNNER'] }) {
  let definition;
  const listeners = new Set();
  const auth = { user, token: user ? 'fixture-session' : null, error: '', get authenticated() { return !!(this.user && this.token); }, restore: async () => auth.user, subscribe: callback => { listeners.add(callback); return () => listeners.delete(callback); }, login: (token, value) => { auth.token = token; auth.user = value; listeners.forEach(callback => callback(auth)); }, logout: () => { auth.user = null; auth.token = null; listeners.forEach(callback => callback(auth)); } };
  const calls = [];
  const storage = new Map();
  global.getApp = () => ({ auth, api, config: { baseUrl: 'http://127.0.0.1:1234/api' } });
  global.wx = {
    showToast: value => calls.push(['toast', value.title]),
    showModal: value => value.success({ confirm: true }),
    navigateTo: value => calls.push(['navigate', value.url]),
    redirectTo: value => calls.push(['redirect', value.url]),
    switchTab: value => calls.push(['tab', value.url]),
    getStorageSync: key => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: key => storage.delete(key),
    vibrateShort() {}, stopPullDownRefresh() {}, setTabBarBadge() {}, removeTabBarBadge() {},
  };
  global.Page = value => { definition = value; };
  const full = path.join(__dirname, '../src/pages', file + '.js');
  delete require.cache[require.resolve(full)]; require(full);
  const page = { ...definition, data: structuredClone(definition.data), setData(value) { for (const [key, item] of Object.entries(value)) { const parts = key.split('.'); let target = this.data; while (parts.length > 1) target = target[parts.shift()]; target[parts[0]] = item; } } };
  page.onLoad({ id: '10' });
  return { page, auth, calls, storage };
}

test('task workspace shows private contacts/events and clears generated code on hide', async () => {
  const task = { id: 10, publisherId: 1, publisherNickname: '发布者', title: '送书', description: '说明', rewardAmount: 8, deadline: '2099-01-01T12:00:00', acceptanceMode: 'GRAB', verificationMode: 'COMPLETION_CODE', originZone: 'CENTRAL', destinationZone: 'WEST', workflowStatus: 'ACCEPTED', acceptedApplicationId: 22 };
  const h = pageHarness('tasks/detail', {
    task: async () => task,
    taskApplicants: async () => [],
    taskWorkspace: async () => ({ task, publisherContact: { userId: 1, nickname: '发布者', wechatContact: 'pub_wx', qqContact: '10001' }, runnerContact: { userId: 2, nickname: '接单者', wechatContact: 'runner_wx', qqContact: null }, events: [{ id: 1, actorId: 1, actorNickname: '发布者', eventType: 'ACCEPTED', note: '已接单', createdAt: '2099-01-01T10:00:00' }] }),
    generateCompletionCode: async () => ({ completionCode: '123456', expiresAt: '2099-01-01T10:30:00' }),
  });
  await h.page.reload();
  assert.equal(h.page.data.workspace.publisherContact.wechatContact, 'pub_wx');
  assert.equal(h.page.data.workspace.events[0].eventTypeLabel, '已接单');
  assert.equal(h.page.data.canGenerateCode, true);
  await h.page.generateCode();
  assert.equal(h.page.data.completionCode, '123456');
  h.page.onHide();
  assert.equal(h.page.data.completionCode, null);
  assert.equal(h.page.data.workspace, null);
});

test('accepted runner can redeem a completion code only in the pending-confirmation workspace', async () => {
  const task = { id: 10, publisherId: 3, publisherNickname: '发布者', title: '送书', description: '说明', rewardAmount: 8, deadline: '2099-01-01T12:00:00', acceptanceMode: 'GRAB', verificationMode: 'COMPLETION_CODE', originZone: 'CENTRAL', destinationZone: 'WEST', workflowStatus: 'PENDING_CONFIRMATION', acceptedApplicationId: 22 };
  let redeemed;
  const h = pageHarness('tasks/detail', {
    task: async () => task,
    myTaskApplications: async () => [{ id: 22, applicantId: 1, status: 'ACCEPTED' }],
    taskWorkspace: async () => ({ task, publisherContact: { userId: 3, nickname: '发布者', wechatContact: 'pub_wx', qqContact: null }, runnerContact: { userId: 1, nickname: '接单者', wechatContact: 'runner_wx', qqContact: null }, events: [] }),
    completeWithCode: async (_, code) => { redeemed = code; return { ...task, workflowStatus: 'COMPLETED' }; },
  });
  await h.page.reload();
  assert.equal(h.page.data.canRedeemCode, true);
  h.page.setData({ enteredCompletionCode: '654321' });
  await h.page.redeemCode();
  assert.equal(redeemed, '654321');
});

test('public task response remains visible when the same-token session restore completes', async () => {
  let resolve;
  const h = pageHarness('tasks/index', { tasks: () => new Promise(done => { resolve = done; }) }, null);
  h.auth.token = 'restoring-token'; h.page._observedToken = 'restoring-token';
  const pending = h.page.reload();
  h.auth.login('restoring-token', { id: 1, roles: ['ROLE_STUDENT'] });
  resolve([{ id: 10, deadline: '2099-01-01T12:00:00', acceptanceMode: 'GRAB', originZone: 'CENTRAL', destinationZone: 'WEST' }]);
  await pending;
  assert.equal(h.page.data.tasks.length, 1);
  assert.equal(h.page.data.loading, false);
});

test('duplicate task submission is blocked and no deposit field is sent', async () => {
  let resolve, count = 0, payload;
  const h = pageHarness('tasks/publish', { publishTask: data => { count++; payload = data; return new Promise(done => { resolve = done; }); } });
  h.page.setData({ form: { title: '取教材', originDetail: '图书馆', destinationDetail: '食堂门口', weightKg: '1', notes: '公开地点交接', pickupCode: '', rewardAmount: '6.50', acceptanceMode: 'GRAB', verificationMode: 'PHOTO_AND_CONFIRMATION' }, date: '2099-01-01', time: '12:30' });
  h.page.next();
  const a = h.page.submit(); await new Promise(resolve => setImmediate(resolve));
  const b = h.page.submit(); await new Promise(resolve => setImmediate(resolve));
  assert.equal(count, 1);
  assert.equal(payload.depositAmount, undefined);
  assert.equal(payload.weightGrams, 1000);
  assert.equal(payload.deadline, '2099-01-01T12:30:00');
  resolve({ id: 10 }); await Promise.all([a,b]);
  assert.equal(h.calls.filter(x => x[0] === 'redirect').length, 1);
});

test('task publish sends the selected completion-code verification mode', async () => {
  let payload;
  const h = pageHarness('tasks/publish', { publishTask: async data => { payload = data; return { id: 10 }; } });
  h.page.setData({ form: { title: '取教材', originDetail: '图书馆', destinationDetail: '食堂门口', weightKg: '1', notes: '公开地点交接', pickupCode: '', rewardAmount: '6.50', acceptanceMode: 'GRAB', verificationMode: 'COMPLETION_CODE' }, date: '2099-01-01', time: '12:30' });
  h.page.next();
  await h.page.submit();
  assert.equal(payload.verificationMode, 'COMPLETION_CODE');
});

test('self task cannot be grabbed and new runner role opens an explicit agreement first', async () => {
  const h = pageHarness('tasks/index', {});
  h.page.setData({ tasks: [{ id: 10, publisherId: 1, expired: false }] });
  await h.page.choose({ currentTarget: { dataset: { id: 10 } } });
  assert.equal(h.page.data.selected, null);
  assert.equal(h.calls[0][0], 'navigate');
  h.auth.user = { id: 2, roles: ['ROLE_STUDENT'] };
  await h.page.choose({ currentTarget: { dataset: { id: 10 } } });
  assert.equal(h.page.data.unlock, true);
  assert.equal(h.page.data.selected, null);
});

test('switching accounts during application lookup cannot submit as the new user', async () => {
  let resolve, applied = 0;
  const h = pageHarness('tasks/index', { myTaskApplications: () => new Promise(done => { resolve = done; }), applyTask: async () => { applied++; } });
  h.page.setData({ selected: { id: 10, publisherId: 3, acceptanceMode: 'APPLICATION' }, message: '我可以顺路带回' });
  const pending = h.page.take();
  await new Promise(done => setImmediate(done));
  h.auth.logout();
  h.auth.login('other-session', { id: 2, roles: ['ROLE_STUDENT', 'ROLE_RUNNER'] });
  resolve([]); await pending;
  assert.equal(applied, 0);
});

test('profile contact save uses the dedicated contacts endpoint and preserves roles', async () => {
  let payload;
  const h = pageHarness('pass/index', { saveContacts: async data => { payload = data; return { id: 1, username: 'fixture', nickname: '测试用户', roles: ['ROLE_STUDENT'], wechatContact: data.wechatContact, qqContact: data.qqContact }; } });
  h.page.setData({ profileForm: { username: 'fixture', nickname: '测试用户', wechatContact: ' wx_fixture ', qqContact: ' 123 ' } });
  await h.page.saveContacts();
  assert.deepEqual(payload, { wechatContact: 'wx_fixture', qqContact: '123' });
});

test('a confirmation accepted after account switch cannot authorize a write', async () => {
  const h = pageHarness('tasks/index', {}); let accept;
  global.wx.showModal = options => { accept = options.success; };
  const pending = ui.confirm('确认', '执行本人操作');
  h.auth.login('different', { id: 2, roles: ['ROLE_STUDENT'] });
  accept({ confirm: true });
  assert.equal(await pending, false);
});

test('only notification ids from the current recipient list can be marked read', async () => {
  const ids = [];
  const h = pageHarness('pass/index', { readNotification: async id => ids.push(id) });
  h.page.setData({ unread: 2, notices: [{ id: 8, recipientId: 1, unread: true }, { id: 9, recipientId: 2, unread: true }] });
  await h.page.readNotice({ currentTarget: { dataset: { id: 9 } } });
  await h.page.readNotice({ currentTarget: { dataset: { id: 100 } } });
  await h.page.readNotice({ currentTarget: { dataset: { id: 8 } } });
  assert.deepEqual(ids, [8]);
});

test('mark all read posts once, clears local notices and the personal tab badge only after success', async () => {
  let finish, requests = 0;
  const h = pageHarness('pass/index', { readAllNotifications() { requests++; return new Promise(done => { finish = done; }); } });
  const badges = [];
  global.wx.removeTabBarBadge = ({ index }) => badges.push(index);
  h.page.setData({ unread: 2, notices: [true, true, false].map((unread, id) => ({ id, unread })) });
  const pending = h.page.readAllNotices();
  await h.page.readAllNotices();
  assert.equal(requests, 1);
  assert.equal(h.page.data.unread, 2);
  assert.deepEqual(badges, []);
  finish({ updatedCount: 2 });
  await pending;
  assert.equal(h.page.data.unread, 0);
  assert.deepEqual(h.page.data.notices.map(n => n.unread), [false, false, false]);
  assert.deepEqual(badges, [4]);
  assert.deepEqual(h.calls, [['toast', '已全部标为已读']]);
  await h.page.readAllNotices();
  assert.equal(requests, 1);
});

test('refresh started before mark all read does not restore a stale unread badge', async () => {
  let finishRefresh;
  const h = pageHarness('pass/index', {
    profile: async () => ({ id: 1 }), applications: async () => [], orderDesk: async () => ({}),
    notifications: () => new Promise(done => { finishRefresh = done; }), readAllNotifications: async () => ({ updatedCount: 1 }),
  });
  h.page.setData({ notices: [{ id: 10, unread: true }], unread: 1 });
  const refresh = h.page.reload();
  await new Promise(done => setImmediate(done));
  await h.page.readAllNotices();
  finishRefresh([{ id: 10, readAt: null }]);
  await refresh;
  assert.equal(h.page.data.notices[0].unread, false);
  assert.equal(h.page.data.unread, 0);
});

test('logout scrubs personal data and invalidates late requests', async () => {
  let done;
  const h = pageHarness('pass/index', {});
  const pending = ui.load(h.page, () => new Promise(resolve => { done = resolve; }), profile => ({ profile }));
  h.auth.logout();
  done({ id: 1, email: 'private@example.edu.cn' });
  await pending;
  assert.equal(h.page.data.profile, null);
  assert.equal(h.page.data.loading, false);
});

test('loss of verification with the same token invalidates private pending data', async () => {
  let done;
  const h = pageHarness('pass/index', {});
  const pending = ui.load(h.page, () => new Promise(resolve => { done = resolve; }), profile => ({ profile }));
  h.auth.login('fixture-session', null);
  done({ id: 1, email: 'private@example.edu.cn' });
  await pending;
  assert.equal(h.page.data.profile, null);
});

test('saving a local publicity draft never triggers review or a server write', async () => {
  let creates = 0;
  const h = pageHarness('showcase/publish', { createProject: async () => { creates++; return { id: 20, status: 'PENDING_REVIEW' }; } });
  h.page._editorToken = h.auth.token; h.page._editorUser = 1; h.page._initialized = true; h.page._visible = true;
  h.page.setData({ authorized: true, ready: true, editable: true, canSubmit: true, dirty: true, form: { title: '竞赛招募', adType: 'TEAM_UP', description: '寻找队友', contactInfo: '微信:fixture', contactVisibility: 'LOGIN_ONLY', campusZone: 'CENTRAL', summary: '', tags: '', linkUrl: '', expiryEnabled: false } });
  await h.page.save({ currentTarget: { dataset: { mode: 'save' } } });
  assert.equal(creates, 0);
  assert(h.storage.size > 0);
});

test('new publicity creation already in review does not send a redundant submit', async () => {
  let creates = 0, submits = 0;
  const h = pageHarness('showcase/publish', { createProject: async () => { creates++; return { id: 20, status: 'PENDING_REVIEW' }; }, submitProject: async () => { submits++; return { id: 20, status: 'PENDING_REVIEW' }; } });
  h.page._editorToken = h.auth.token; h.page._editorUser = 1; h.page._initialized = true; h.page._visible = true;
  h.page.setData({ authorized: true, ready: true, editable: true, canSubmit: true, dirty: true, form: { title: '竞赛招募', adType: 'TEAM_UP', description: '寻找队友', contactInfo: '微信:fixture', contactVisibility: 'LOGIN_ONLY', campusZone: 'CENTRAL', summary: '', tags: '', linkUrl: '', expiryEnabled: false } });
  await h.page.save({ currentTarget: { dataset: { mode: 'submit' } } });
  assert.equal(creates, 1);
  assert.equal(submits, 0);
  assert.equal(h.page.data.draftId, '20');
  assert.equal(h.page.data.canSubmit, false);
});

test('publicity create uncertainty is remembered instead of automatically duplicating', async () => {
  let creates = 0;
  const h = pageHarness('showcase/publish', { createProject: async () => { creates++; const e = new Error('网络中断'); e.network = true; throw e; } });
  h.page._editorToken = h.auth.token; h.page._editorUser = 1; h.page._initialized = true; h.page._visible = true;
  h.page.setData({ authorized: true, ready: true, editable: true, canSubmit: true, dirty: true, form: { title: '竞赛招募', adType: 'TEAM_UP', description: '寻找队友', contactInfo: '微信:fixture', contactVisibility: 'LOGIN_ONLY', campusZone: 'CENTRAL', summary: '', tags: '', linkUrl: '', expiryEnabled: false } });
  await h.page.save({ currentTarget: { dataset: { mode: 'submit' } } });
  assert.equal(h.page.data.creationUncertain, true);
  await h.page.save({ currentTarget: { dataset: { mode: 'submit' } } });
  assert.equal(creates, 1);
});

test('publicity private contact and comments are scrubbed when a viewer signs out', async () => {
  const h = pageHarness('showcase/detail', {
    project: async () => ({ id: 10, publisherId: 2, status: 'APPROVED', title: '招募', adType: 'TEAM_UP', contactVisible: true, contactVisibility: 'LOGIN_ONLY', contactInfo: '微信:private_fixture', attachments: [] }),
    comments: async () => [],
  });
  h.page._visible = true;
  await h.page.reload();
  assert.equal(h.page.data.contacts.length, 1);
  h.page._visible = false;
  h.auth.logout();
  assert.deepEqual(h.page.data.contacts, []);
  assert.equal(h.page.data.project, null);
});

test('订单中的预约按钮由实际下单/接单方决定，不依赖个人页切换身份', () => {
  const {orderActions}=require('../src/lib/rules');
  const order={id:4,customerId:1,providerId:2,status:'REQUESTED'};
  assert.deepEqual(orderActions(order,1).map(x=>x.value),['cancel']);
  assert.deepEqual(orderActions(order,2).map(x=>x.value),['accept','reject','cancel']);
  assert.deepEqual(orderActions(order,3),[]);
});

test('booking has no payment call and returned contact data is hidden after logout', async () => {
  let books = 0;
  const h = pageHarness('shops/detail', {
    book: async () => { books++; return { id: 1 }; },
    shop: async () => ({ id: 10, contactVisible: true, contactSnapshot: '微信:fixture_merchant', serviceItems: [] }),
  });
  h.page.setData({ services: [{ id: 1, title: '电脑清灰' }], serviceIndex: 0, date: '2099-01-01', time: '10:00', note: '演练', booking: true });
  await h.page.submitBooking();
  assert.equal(books, 1);
  assert.equal(h.page.data.booking, false);
  assert.equal(h.page.data.contacts.length, 1);
  h.auth.logout();
  assert.deepEqual(h.page.data.contacts, []);
});

test('completed runner can still read participant workspace and history', async () => {
  const task = { id: 10, publisherId: 2, workflowStatus: 'COMPLETED', acceptedApplicationId: 22 };
  const h = pageHarness('tasks/detail', { task: async () => task,
    myTaskApplications: async () => [{ id: 22, applicantId: 1, status: 'COMPLETED' }],
    taskWorkspace: async () => ({ task, events: [] }) });
  await h.page.reload();
  assert.ok(h.page.data.workspace);
  assert.equal(h.page.data.canIssue, false);
});

test('hidden task page ignores late private workspace responses', async () => {
  let resolve;
  const task = { id: 10, publisherId: 1, workflowStatus: 'ACCEPTED' };
  const h = pageHarness('tasks/detail', { task: async () => task, taskApplicants: async () => [],
    taskWorkspace: () => new Promise(done => { resolve = done; }) });
  const loading = h.page.reload();
  while (!resolve) await new Promise(done => setImmediate(done));
  h.page.onHide(); resolve({ task, events: [] }); await loading;
  assert.equal(h.page.data.workspace, null);
  assert.equal(h.page.data.canIssue, false);
});

test('hidden task page ignores late completion-code responses', async () => {
  let resolve;
  const h = pageHarness('tasks/detail', { generateCompletionCode: () => new Promise(done => { resolve = done; }) });
  h.page.setData({ canGenerateCode: true });
  const generating = h.page.generateCode();
  while (!resolve) await new Promise(done => setImmediate(done));
  h.page.onHide(); resolve({ completionCode: '123456', expiresAt: '2099-01-01T12:00:00' }); await generating;
  assert.equal(h.page.data.completionCode, null);
});

test('publisher confirmation and issue actions match server states', async () => {
  const task = { id: 10, publisherId: 1, workflowStatus: 'DELIVERED' };
  const h = pageHarness('tasks/detail', { task: async () => task, taskApplicants: async () => [], taskWorkspace: async () => ({ task, events: [] }) });
  await h.page.reload(); assert.equal(h.page.data.canConfirm, false);
  task.workflowStatus = 'ISSUE_HANDLING'; await h.page.reload(); assert.equal(h.page.data.canIssue, false);
});

test('new errand needs weight; pickup code remains a separate string, not public description', async () => {
  const sent=[]; const h=pageHarness('tasks/publish',{publishTask: async data=>{sent.push(data);return{id:101};}});
  h.page.setData({form:{...h.page.data.form,title:'代取书籍',originDetail:'驿站',destinationDetail:'食堂',rewardAmount:'6'},date:'2099-01-01',time:'12:30'});
  await h.page.submit(); assert.equal(sent.length,0); assert.match(h.page.data.error,/重量/);
  h.page.setData({'form.weightKg':'0.75','form.pickupCode':' 0012-A ','form.notes':'请勿折叠'});
  await h.page.submit(); assert.equal(sent.length,1); assert.equal(sent[0].weightGrams,750);
  assert.equal(sent[0].pickupCode,'0012-A'); assert.equal(sent[0].notes,'请勿折叠'); assert(!sent[0].description.includes('0012-A'));
});
test('after task creation retry only uploads the failed photo, never another task',async()=>{
  let created=0;const uploaded=[];const h=pageHarness('tasks/publish',{publishTask:async()=>{created++;return{id:119};}});
  const app=getApp();app.taskMedia={upload: async(id,kind,photo)=>{uploaded.push([id,kind,photo.key]);if(uploaded.length===2)throw new Error('网络中断');return{id:90+uploaded.length};}};global.getApp=()=>app;
  h.page.setData({form:{...h.page.data.form,title:'代取书籍',weightKg:'1',originDetail:'A',destinationDetail:'B',rewardAmount:'2'},date:'2099-01-01',time:'12:30',photos:[{key:'a',path:'local-a'},{key:'b',path:'local-b'}]});
  await h.page.submit(); assert.equal(h.page.data.publishedId,119); assert.equal(h.calls.filter(v=>v[0]==='redirect').length,0);
  await h.page.submit();assert.equal(created,1);assert.deepEqual(uploaded,[[119,'REQUEST_IMAGE','a'],[119,'REQUEST_IMAGE','b'],[119,'REQUEST_IMAGE','b']]);assert.equal(h.calls.filter(v=>v[0]==='redirect').length,1);
});

function photoWorkspace(owner = false) {
  const task = { id: 10, publisherId: owner ? 1 : 3, acceptedApplicationId: 22, verificationMode: 'PHOTO_AND_CONFIRMATION', workflowStatus: 'DELIVERING' };
  const evidence = [{ id: 41, evidenceType: 'REQUEST_IMAGE', downloadPath: '/private/request' }];
  const api = { task: async () => task, taskApplicants: async () => [], myTaskApplications: async () => [{id:22, applicantId:1, status:'ACCEPTED'}],
    taskWorkspace: async () => ({ task, evidence, events: [] }), submitEvidence: async () => { task.workflowStatus = 'PENDING_CONFIRMATION'; } };
  const h = pageHarness('tasks/detail', api), discarded = [];
  const media = { download: async (_, id) => 'wxfile://tmp/' + id, discard: p => discarded.push(p) };
  const app = { ...getApp(), taskMedia:media }; global.getApp = () => app;
  return { ...h, evidence, discarded, media, task };
}
test('item photos allow delivery upload but only delivery evidence unlocks runner submission', async () => {
  const h = photoWorkspace(); await h.page.reload();
  assert.equal(h.page.data.canUploadDelivery, true); assert.equal(h.page.data.canSubmitEvidence, false);
  await h.page.submitEvidence(); assert.equal(h.task.workflowStatus, 'DELIVERING');
  h.evidence.push({id:42,evidenceType:'DELIVERY_PHOTO'}); await h.page.reload();
  assert.equal(h.page.data.canSubmitEvidence, true); await h.page.submitEvidence();
  assert.equal(h.task.workflowStatus,'PENDING_CONFIRMATION'); assert.equal(h.page.data.canSubmitEvidence,false);
});
test('publisher sees both protected photos via local files, cannot submit, clears files on hide',async()=>{
  const h=photoWorkspace(true); h.evidence.push({id:42,evidenceType:'DELIVERY_PHOTO'}); await h.page.reload();
  assert.deepEqual(h.page.data.photos.map(p=>p.previewPath),['wxfile://tmp/41','wxfile://tmp/42']);
  assert.equal(h.page.data.canSubmitEvidence,false); h.page.onHide(); assert.deepEqual(h.page.data.photos,[]);
  assert.deepEqual(h.discarded.sort(),['wxfile://tmp/41','wxfile://tmp/42']);
});
test('late authenticated thumbnail after unload is discarded rather than displayed',async()=>{
  const h=photoWorkspace(); let finish; h.media.download=()=>new Promise(done=>{finish=done;});
  const loading=h.page.reload(); await new Promise(done=>setImmediate(done)); h.page.onUnload();
  assert.equal(typeof finish,'function'); finish('wxfile://tmp/late'); await loading;
  assert.deepEqual(h.discarded,['wxfile://tmp/late']); assert.deepEqual(h.page.data.photos,[]);
});
test('picker may hide and reopen detail: only the refreshed owner can upload a request photo',async()=>{
  const h=photoWorkspace(true);await h.page.reload();let picked;const uploads=[];
  h.media.pick=()=>new Promise(done=>{picked=done});h.media.upload=async(id,kind,p)=>uploads.push([id,kind,p.key]);
  const choosing=h.page.chooseEvidence({currentTarget:{dataset:{kind:'REQUEST_IMAGE'}}});h.page.onHide();h.page.onShow();picked([{key:'same-selection'}]);await choosing;
  assert.deepEqual(uploads,[[10,'REQUEST_IMAGE','same-selection']]);
  const stale=h.page.chooseEvidence({currentTarget:{dataset:{kind:'REQUEST_IMAGE'}}});h.auth.login('replacement',{id:4});picked([{key:'old-selection'}]);await stale;
  assert.equal(uploads.length,1);assert.deepEqual(h.page.data.photos,[]);
});

test('二手订单打开同一商品；返回时取最新内容，只有详情 410 才显示已删除', async () => {
  let title='书本',failure=0;
  const h=pageHarness('goods/detail',{goodsDetail:async id=>{assert.equal(id,10);if(failure){const e=Error('未能读取');e.status=failure;throw e;}return {id,sellerId:2,status:'SOLD',title,price:16};}});
  await h.page.onShow();assert.equal(h.page.data.item.title,'书本');
  title='新版说明';h.page.onHide();await h.page.onShow();assert.equal(h.page.data.item.title,'新版说明');
  failure=404;await h.page.reload();assert.equal(h.page.data.deleted,false);assert.equal(h.page.data.item,null);assert.ok(h.page.data.error);
  failure=503;await h.page.reload();assert.equal(h.page.data.deleted,false);assert.ok(h.page.data.error);
  failure=410;await h.page.reload();assert.equal(h.page.data.deleted,true);assert.equal(h.page.data.item,null);assert.deepEqual(h.page.data.contacts,[]);
});

test('runner draft previews selected local photos without uploading or losing the selection on return',async()=>{
  const {page}=pageHarness('tasks/publish',{});
  const photos=[{key:'a',path:'tmp-a'},{key:'b',path:'tmp-b'}];
  page.setData({photos});let shown;
  global.wx.previewImage=o=>{shown=o;page.onHide();};
  page.previewDraft({currentTarget:{dataset:{src:'tmp-b'}}});
  assert.equal(shown.current,'tmp-b');assert.deepEqual(shown.urls,['tmp-a','tmp-b']);
  await page.onShow();assert.deepEqual(page.data.photos,photos);
  page.onHide();assert.deepEqual(page.data.photos,[]);
});

test('owner album stays mounted on picker return but is cleared on ordinary hide and logout',async()=>{
  const h=pageHarness('goods/detail',{goodsDetail:async()=>({id:10,sellerId:1,title:'书'}),goodsIntents:async()=>[]});
  await h.page.onShow();let picking=true;
  h.page.selectAllComponents=()=>[{isSelectingMedia:()=>picking}];
  h.page.onHide();assert.ok(h.page.data.item);picking=false;await h.page.onShow();
  h.page.onHide();assert.equal(h.page.data.item,null);
  await h.page.onShow();picking=true;h.page.onHide();h.auth.logout();assert.equal(h.page.data.item,null);
});

for(const edit of [false,true]) test(`goods save and gallery share the specific failure; retry keeps the ID and keys (edit=${edit})`,async()=>{
  let saves=0, rejectSave=true; const uploads=[];
  const save=async()=>{saves++;if(rejectSave)throw Error('商品未保存');return {id:23};};
  const {page,calls}=pageHarness('goods/publish',edit?{updateGoods:save}:{publishGoods:save});
  const app=getApp(); app.client={get:async()=>[]};global.getApp=()=>app;
  let gallery;global.Component=v=>{gallery=v;};const file=require.resolve('../src/components/catalog-photos/index');delete require.cache[file];require(file);
  const el={...gallery.methods,properties:{kind:'GOODS',objectId:edit?23:0,editable:true},data:structuredClone(gallery.data),setData(v){Object.assign(this.data,v);},triggerEvent(name, detail){ if(name==='retryresult') page.photoRetry({detail}); }};
  gallery.lifetimes.attached.call(el);page.selectComponent=()=>el;
  page._visible=true;page.setData({ready:true,editId:edit?23:0,categories:[{id:1}],form:{title:'课本',description:'说明',price:'2',conditionLevel:'新',tradeLocation:'校内'}});
  el.setData({queue:['a','b','c'].map(key=>({key,path:'tmp-'+key}))});
  wx.uploadFile=o=>{assert.ok(o.url.endsWith('/GOODS/23'));uploads.push(o.formData.clientRequestId);o.success({statusCode:200,data:JSON.stringify(uploads.length===2?{success:false,message:'图片尺寸过大'}:{success:true,data:{id:1}})});};
  await page.save();assert.equal(page.data.error,'商品未保存');assert.equal(page.data.savedId,0);assert.deepEqual(uploads,[]);
  rejectSave=false;await Promise.all([page.save(),page.save()]);
  const reason='照片上传失败：图片尺寸过大，请缩小图片宽高后重试';
  assert.equal(page.data.error,'闲置已保存，'+reason);assert.equal(el.data.error,reason);assert.deepEqual(el.data.queue.map(f=>f.key),['b','c']);
  if(edit){
    wx.uploadFile=o=>o.success({statusCode:403,data:{success:false,message:'不能修改此图片'}});
    await el.retry();assert.equal(page.data.error,'闲置已保存，照片上传失败：不能修改此图片');
    wx.uploadFile=o=>{uploads.push(o.formData.clientRequestId);o.success({statusCode:200,data:{success:true,data:{id:2}}});};
    await el.retry();assert.equal(page.data.error,'');
  }
  await page.save();assert.equal(saves,2);assert.deepEqual(uploads,['a','b','b','c']);assert.deepEqual(calls.at(-1),['redirect','/pages/goods/detail?id=23']);
});
