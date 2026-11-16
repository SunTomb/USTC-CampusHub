const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { createRequire } = require('node:module');
const src = path.resolve(__dirname, '../src');

function appHarness() {
  const filename = path.join(src, 'app.js');
  let app;
  vm.runInNewContext(fs.readFileSync(filename, 'utf8'), {
    require: createRequire(filename), App: value => { app = value; },
    wx: { getAccountInfoSync: () => ({ miniProgram: { envVersion: 'develop' } }), getStorageSync: () => null },
  });
  app.onLaunch();
  return app;
}

function componentHarness(app, aboutVisible = false) {
  const filename = path.join(src, 'components/launch-intro/index.js');
  let definition;
  const timers = new Map(), events = [], calls = [], owners = new Set();
  let sequence = 0;
  vm.runInNewContext(fs.readFileSync(filename, 'utf8'), {
    require: name => name === '../../lib/overlays'
      ? { hold: owner => { owners.add(owner); calls.push('hideTabBar'); }, release: owner => { owners.delete(owner); calls.push('releaseTabBar'); } }
      : createRequire(filename)(name),
    Component: value => { definition = value; }, getApp: () => app,
    setTimeout: (callback, delay) => { timers.set(++sequence, { callback, delay }); return sequence; },
    clearTimeout: id => timers.delete(id),
    wx: { setClipboardData: value => { calls.push(['clipboard', value.data]); value.success(); }, showToast: value => calls.push(['toast', value.title]) },
  });
  const instance = { ...definition.methods, data: structuredClone(definition.data), properties: { aboutVisible },
    setData: function (value) { Object.assign(this.data, value); }, triggerEvent: name => events.push(name) };
  definition.lifetimes.attached.call(instance);
  definition.lifetimes.ready.call(instance);
  return { instance, definition, timers, events, calls, owners };
}

test('launch intro is claimed only once per cold launch, not persisted across launches', () => {
  const app = appHarness();
  assert.equal(typeof app.claimLaunchIntro, 'function');
  assert.equal(app.claimLaunchIntro(), true);
  assert.equal(app.claimLaunchIntro(), false);
  app.onLaunch();
  assert.equal(app.claimLaunchIntro(), true);
});

test('first entry shows supplied filing and auto dismisses without waiting for API or login', () => {
  const h = componentHarness(appHarness());
  assert.equal(h.instance.data.showing, true);
  assert.equal(h.instance.data.filing.number, '浙ICP备2026099873号');
  assert.equal(h.instance.data.filing.queryUrl, 'https://beian.miit.gov.cn/');
  assert.equal(h.owners.size, 1);
  const timer = [...h.timers.values()][0];
  assert.equal(timer.delay, 2200);
  timer.callback();
  assert.equal(h.instance.data.showing, false);
  assert.equal(h.owners.size, 0);
  assert.equal(h.timers.size, 0);
});

test('tab switches, deep links and warm return do not replay the intro', () => {
  const app = appHarness(), first = componentHarness(app);
  first.definition.pageLifetimes.hide.call(first.instance);
  assert.equal(first.instance.data.showing, false);
  assert.equal(first.timers.size, 0);
  const next = componentHarness(app);
  assert.equal(next.instance.data.showing, false);
  assert.equal(next.timers.size, 0);
});

test('skip and detach clear launch timers and release their own overlay', () => {
  const h = componentHarness(appHarness());
  h.instance.dismiss();
  assert.equal(h.instance.data.showing, false);
  assert.equal(h.timers.size, 0);
  const next = componentHarness(appHarness());
  next.definition.lifetimes.detached.call(next.instance);
  assert.equal(next.timers.size, 0);
  assert.equal(next.owners.size, 0);
});

test('filing details stay open for reading and only copy official query address on request', () => {
  const h = componentHarness(appHarness());
  h.instance.showAbout();
  assert.equal(h.instance.data.mode, 'about');
  assert.equal(h.timers.size, 0);
  assert.equal(h.calls.some(value => Array.isArray(value) && value[0] === 'clipboard'), false);
  h.instance.copyQuery();
  assert.deepEqual(h.calls.find(value => Array.isArray(value) && value[0] === 'clipboard'), ['clipboard', 'https://beian.miit.gov.cn/']);
  h.instance.dismiss();
  assert(h.events.includes('close'));
});

test('passport can open filing later even when launch intro was already claimed', () => {
  const app = appHarness(); app.claimLaunchIntro();
  const h = componentHarness(app, true);
  assert.equal(h.instance.data.showing, true);
  assert.equal(h.instance.data.mode, 'about');
  assert.equal(h.timers.size, 0);
  h.definition.pageLifetimes.hide.call(h.instance);
  assert.equal(h.instance.data.showing, false);
  assert(h.events.includes('close'));
});

test('every entry page mounts the launch component and keeps ten pages/four tabs', () => {
  const config = JSON.parse(fs.readFileSync(path.join(src, 'app.json'), 'utf8'));
  assert.equal(config.pages.length, 10); assert.equal(config.tabBar.list.length, 4);
  assert.equal(config.usingComponents['launch-intro'], '/components/launch-intro/index');
  for (const route of config.pages) {
    const wxml = fs.readFileSync(path.join(src, route + '.wxml'), 'utf8');
    assert.equal((wxml.match(/<launch-intro\b/g) || []).length, 1, route);
  }
  const pass = fs.readFileSync(path.join(src, 'pages/pass/index.wxml'), 'utf8');
  assert.match(pass, /关于与备案/);
});
