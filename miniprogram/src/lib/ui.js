const TABS = ['/pages/tasks/index', '/pages/goods/index', '/pages/shops/index', '/pages/showcase/index', '/pages/pass/index'];
const ROUTES = new Set([...TABS, '/pages/tasks/detail', '/pages/tasks/publish', '/pages/goods/detail', '/pages/goods/publish', '/pages/shops/detail', '/pages/shops/item', '/pages/showcase/detail', '/pages/showcase/publish', '/pages/pass/auth', '/pages/orders/index']);
function validRoute(route) { return typeof route==='string' && /^\/pages\/[a-z]+\/[a-z]+(?:\?[A-Za-z0-9_%=&-]*)?$/.test(route) && ROUTES.has(route.split('?')[0]); }
function open(route) { if (!validRoute(route)) return; if (TABS.includes(route.split('?')[0])) wx.switchTab({ url: route.split('?')[0] }); else wx.navigateTo({ url: route }); }
function authRoute(next) {
  const url = '/pages/pass/auth' + (validRoute(next) ? '?next=' + encodeURIComponent(next) : '');
  const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : [];
  const current = pages[pages.length - 1];
  if (current && current.route === 'pages/pass/auth') return;
  if (current && /^pages\/(tasks|showcase|goods)\/publish$/.test(current.route)) wx.redirectTo({ url });
  else wx.navigateTo({ url });
}
async function requireUser(next) { const auth = getApp().auth; await auth.restore(); if (!auth.authenticated) { authRoute(next); return null; } return auth.user; }
function definePage(def) {
  const load = def.onLoad, unload = def.onUnload;
  def.data = { loading: false, error: '', busy: false, user: null, ...def.data };
  def.onLoad = function (query) {
    this._alive = true; this._loadSequence = 0; this._observedToken = getApp().auth.token; this._observedUserId = getApp().auth.user && getApp().auth.user.id;
    this.setData({ user: getApp().auth.user });
    this._unsubscribe = getApp().auth.subscribe(auth => {
      if (!this._alive) return;
      const userId = auth.user && auth.user.id;
      const identityChanged = !!this._observedUserId && this._observedUserId !== userId;
      const tokenChanged = this._observedToken !== auth.token;
      if (tokenChanged || identityChanged) { this._loadSequence++; this.setData({ loading: false }); }
      this._observedToken = auth.token; this._observedUserId = userId;
      this.setData({ user: auth.user });
      if ((!auth.user || identityChanged || tokenChanged) && this.clearPrivate) this.clearPrivate();
    });
    if (load) load.call(this, query || {});
  };
  def.onUnload = function () { this._alive = false; this._loadSequence++; if (this._unsubscribe) this._unsubscribe(); if (unload) unload.call(this); };
  def.onPullDownRefresh = async function () { try { if (this.reload) await this.reload(); } finally { wx.stopPullDownRefresh(); } };
  def.go = function (event) { open(event.currentTarget.dataset.url); };
  def.login = function () { authRoute('/pages/pass/index'); };
  def.field = function (event) {
    const field = event.currentTarget.dataset.field;
    if (typeof field === 'string' && /^[a-zA-Z0-9_.]+$/.test(field)) this.setData({ [field]: event.detail.value });
  };
  Page(def);
}
async function load(page, work, map) {
  const sequence = ++page._loadSequence;
  page.setData({ loading: true, error: '' });
  try {
    const result = await work();
    if (page._alive && sequence === page._loadSequence) page.setData(map(result));
  } catch (error) {
    if (page._alive && sequence === page._loadSequence) page.setData({ error: error.message || '暂时无法加载，请重试' });
  } finally { if (page._alive && sequence === page._loadSequence) page.setData({ loading: false }); }
}
async function run(page, work, success) {
  if (page.data.busy) return false;
  const token = getApp().auth.token;
  page.setData({ busy: true, error: '' });
  try {
    const result = await work();
    if (page._alive && token === getApp().auth.token && success) await success(result);
    return true;
  } catch (error) {
    if (page._alive) { page.setData({ error: error.message || '操作未完成，请重试' }); wx.showToast({ title: error.message || '操作未完成', icon: 'none' }); }
    return false;
  } finally { if (page._alive) page.setData({ busy: false }); }
}
function toast(title) { wx.showToast({ title, icon: 'none' }); }
function celebrate(title) { wx.showToast({ title, icon: 'success' }); wx.vibrateShort({ type: 'light', fail() {} }); }
function confirm(title, content) {
  const auth = getApp().auth, token = auth.token, userId = auth.user && auth.user.id;
  return new Promise(resolve => wx.showModal({ title, content, confirmColor: '#16325B', success: result => resolve(!!result.confirm && token === auth.token && userId === (auth.user && auth.user.id)), fail: () => resolve(false) }));
}
function copy(value) { if (typeof value === 'string' && value.trim()) wx.setClipboardData({ data: value.trim(), success: () => toast('已复制，可前往联系同学') }); }
function storageKey(name) { const app = getApp(); return 'woke:' + app.config.baseUrl + ':' + (app.auth.user ? app.auth.user.id : 'guest') + ':' + name; }
function readLocal(name, fallback) { try { return wx.getStorageSync(storageKey(name)) || fallback; } catch (_) { return fallback; } }
function writeLocal(name, data) { try { wx.setStorageSync(storageKey(name), data); return true; } catch (_) { toast('本机存储暂不可用'); return false; } }
function rememberTask(taskId) { const ids = readLocal('published-task-ids', []).filter(id => id !== taskId); writeLocal('published-task-ids', [taskId, ...ids].slice(0, 50)); }
module.exports = { TABS, validRoute, open, authRoute, requireUser, definePage, load, run, toast, celebrate, confirm, copy, readLocal, writeLocal, rememberTask };
