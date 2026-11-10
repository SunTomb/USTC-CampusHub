const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const baseURL = process.env.CAMPUSHUB_SMOKE_URL || 'http://127.0.0.1:5178';
if (!['127.0.0.1', 'localhost', '[::1]'].includes(new URL(baseURL).hostname)) {
  throw new Error('Mobile UI checks require a loopback frontend; never target production.');
}
const { chromium } = require(process.env.CAMPUSHUB_BROWSER_DRIVER || 'playwright');
const output = path.resolve(process.env.CAMPUSHUB_SMOKE_OUTPUT || '.claude/verification/mobile-fixes');
fs.mkdirSync(output, { recursive: true });
const selected = process.argv[2];
const token = 'synthetic-mobile-ui-fixture-not-a-real-token';
const user = { id: 900000001, username: 'mobile_fixture', nickname: '手机布局测试', email: 'fixture@example.edu.cn', roles: ['ROLE_STUDENT', 'ROLE_RUNNER', 'ROLE_GOODS_PUBLISHER', 'ROLE_SHOP_MERCHANT', 'ROLE_ADMIN'] };

function dataFor(url) {
  const p = url.pathname;
  if (p === '/api/platform/features') return { financialEnabled: false };
  if (p === '/api/auth/me') return user;
  if (p === '/api/shops/1') return {
    id: 1, name: '隔离预约测试店铺', description: '仅供浏览器布局回归，不是真实商家。',
    status: 'APPROVED', campusZone: 'CENTRAL', ownerId: 2, ownerNickname: '测试店主', ownerCreditScore: 100,
    rating: 5, serviceArea: '中校区', openingHours: '预约时间协商', contactVisible: false,
    serviceItems: [{ id: 1, title: '隔离服务项目', description: '不产生真实预约。', category: 'OTHER', price: 10, priceUnit: '次', durationMinutes: 30, status: 'PUBLISHED' }],
  };
  if (p === '/api/admin/ops/dashboard') return { publishedTasks: 0, acceptedTasks: 0, completedTasks: 0, openIssues: 0, pendingRoleApplications: 0 };
  if (p === '/api/admin/ops/analytics/overview') return { startDate: '2026-11-01', endDate: '2026-11-30', cards: [] };
  if (p === '/api/admin/ops/analytics/funnels') return { funnels: [] };
  if (p === '/api/admin/ops/analytics/zones') return { taskOriginZones: [], taskDestinationZones: [], taskRoutes: [], goodsZones: [], shopZones: [], projectAdZones: [] };
  if (p === '/api/admin/ops/analytics/fees') return { serviceFeeCount: 0, paidServiceFeeAmount: 0, pendingServiceFeeAmount: 0, roleApplicationCount: 0, roleDepositAmount: 0, serviceFeesByTargetType: [], roleDepositsByType: [] };
  if (p.startsWith('/api/platform/protocols/')) return { agreementCode: 'RUNNER_SERVICE', version: 'fixture-v1', title: '隔离协议显示测试', content: '这是本地前端回归使用的合成文本，不是平台正式协议。\n'.repeat(40), effectiveAt: '2026-11-01T00:00:00', acceptedAt: null };
  return [];
}

async function settle(page) {
  await page.evaluate(async () => {
    await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
    await Promise.all(document.getAnimations()
      .filter(animation => Number.isFinite(animation.effect?.getComputedTiming().endTime))
      .map(animation => animation.finished.catch(() => {})));
  });
}

async function capture(page, filename) {
  await page.evaluate(() => Promise.all([...document.images]
    .filter(image => new URL(image.src, location.href).origin === location.origin)
    .map(image => image.decode().catch(() => {}))));
  const cdp = await page.context().newCDPSession(page);
  try {
    const image = await cdp.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false, fromSurface: true });
    fs.writeFileSync(path.join(output, filename), Buffer.from(image.data, 'base64'));
  } finally {
    await cdp.detach();
  }
}

async function openTask(page) {
  await page.goto(baseURL + '/tasks');
  await page.getByRole('button', { name: '张贴悬赏单', exact: true }).first().tap();
  await page.waitForFunction(() => {
    const d = document.querySelector('.el-drawer');
    return d && Math.abs(d.getBoundingClientRect().right - document.documentElement.clientWidth) < 1;
  });
}

async function openBooking(page) {
  await page.goto(baseURL + '/shops/1');
  await page.getByRole('button', { name: '立即预约这项服务', exact: true }).tap();
  await page.getByRole('dialog').waitFor({ state: 'visible' });
  await settle(page);
}

const cases = {
  async 'mobile-directory'({ page }) {
    await page.goto(baseURL + '/tasks');
    await page.locator('.mobile-tabbar button').tap();
    const directory = page.locator('.mobile-menu-drawer');
    await directory.waitFor({ state: 'visible' });
    await settle(page);
    await directory.locator('a[href="/notifications"]').tap();
    await page.waitForURL('**/notifications');
    await directory.waitFor({ state: 'hidden' });
    assert.equal(await page.locator('.mobile-tabbar a[href="/wallet"]').count(), 0, 'closed wallet appeared in mobile navigation');
  },
  async 'drawer-width'({ page }) {
    await openTask(page);
    for (const width of [320, 360, 390, 430]) {
      await page.setViewportSize({ width, height: 844 });
      await settle(page);
      const geometry = await page.locator('.el-drawer:visible').evaluate(e => {
        const r = e.getBoundingClientRect();
        const outside = [...e.querySelectorAll('input,textarea')].filter(x => x.getBoundingClientRect().width > 0).filter(x => {
          const b = x.getBoundingClientRect(); return b.left < 0 || b.right > document.documentElement.clientWidth + 1;
        });
        return { left: r.left, right: r.right, width: r.width, outsideInputs: outside.length };
      });
      assert(geometry.left >= -1 && geometry.right <= width + 1, `drawer outside ${width}px: ${JSON.stringify(geometry)}`);
      assert.equal(geometry.outsideInputs, 0, `inputs outside ${width}px drawer`);
    }
    await page.setViewportSize({ width: 1280, height: 900 });
    await settle(page);
    assert.equal(Math.round((await page.locator('.el-drawer:visible').boundingBox()).width), 520);
  },
  async 'modal-layer'({ page }) {
    await openTask(page);
    await page.locator('.el-drawer__body').evaluate(e => { e.scrollTop = e.scrollHeight; });
    await settle(page);
    const hit = await page.locator('.el-drawer button').filter({ hasText: '确认张贴悬赏单' }).evaluate(b => {
      const r = b.getBoundingClientRect();
      const e = document.elementFromPoint(r.x + r.width / 2, r.y + r.height / 2);
      return { uncovered: !!e && b.contains(e), top: r.top, bottom: r.bottom, viewport: innerHeight };
    });
    assert(hit.uncovered && hit.top >= 0 && hit.bottom <= hit.viewport, `task submit inaccessible: ${JSON.stringify(hit)}`);
    await capture(page, 'task-submit-mobile.png');
    await page.keyboard.press('Escape');
    await openBooking(page);
    const nav = page.locator('.mobile-tabbar a[href="/goods"]');
    const r = await nav.boundingBox();
    const covered = await page.evaluate(({ x, y }) => !document.elementFromPoint(x, y)?.closest('.mobile-tabbar'), { x: r.x + r.width / 2, y: r.y + r.height / 2 });
    assert(covered, 'mobile navigation is above the booking modal');
    await page.touchscreen.tap(r.x + r.width / 2, r.y + r.height / 2);
    await settle(page);
    assert.equal(new URL(page.url()).pathname, '/shops/1', 'a touch through the modal changed routes');
  },
  async 'date-range'({ page, requests }) {
    await page.goto(baseURL + '/admin/ops');
    await page.locator('.ops-heading-actions input').first().waitFor();
    for (const width of [320, 390, 430]) {
      await page.setViewportSize({ width, height: 844 });
      await page.locator('.ops-heading-actions input').first().tap();
      const panel = page.locator('.el-date-range-picker:visible');
      await panel.waitFor();
      await settle(page);
      const box = await panel.boundingBox();
      assert(box.width <= width && box.x >= -1 && box.x + box.width <= width + 1, `range panel outside ${width}px: ${JSON.stringify(box)}`);
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth), false, 'range popup adds page overflow');
      if (width === 390) await capture(page, 'date-range-mobile.png');
      await page.keyboard.press('Escape');
    }
    await page.locator('.ops-heading-actions input').first().tap();
    const panel = page.locator('.el-date-range-picker:visible');
    await panel.locator('td.available:not(.prev-month):not(.next-month)').filter({ hasText: /^10$/ }).first().tap();
    await panel.locator('td.available:not(.prev-month):not(.next-month)').filter({ hasText: /^15$/ }).first().tap();
    await Promise.all([
      page.waitForResponse(r => new URL(r.url()).pathname === '/api/admin/ops/analytics/overview'),
      page.getByRole('button', { name: '更新统计区间', exact: true }).tap(),
    ]);
    assert(requests.some(u => u.pathname.endsWith('/analytics/overview') && u.searchParams.get('startDate')?.endsWith('-10') && u.searchParams.get('endDate')?.endsWith('-15')), 'selected date range was not sent to analytics');
    await page.setViewportSize({ width: 1280, height: 900 });
    await page.locator('.ops-heading-actions input').first().click();
    await settle(page);
    assert.equal(Math.round((await page.locator('.el-date-range-picker:visible').boundingBox()).width), 646, 'desktop range picker changed width');
  },
  async 'date-time'({ page }) {
    await page.setViewportSize({ width: 844, height: 390 });
    await openBooking(page);
    await page.locator('.el-dialog .el-date-editor input').tap();
    const panel = page.locator('.el-date-picker:visible');
    await panel.waitFor();
    await settle(page);
    const b = await panel.boundingBox();
    assert(b.height <= 390 && b.y >= -1 && b.y + b.height <= 391, `datetime panel outside landscape screen: ${JSON.stringify(b)}`);
    const confirm = panel.getByRole('button', { name: /^(OK|确定)$/ });
    await confirm.scrollIntoViewIfNeeded();
    const geometry = await confirm.evaluate(e => {
      const r = e.getBoundingClientRect(); const hit = document.elementFromPoint(r.x + r.width / 2, r.y + r.height / 2);
      return { top: r.top, bottom: r.bottom, uncovered: !!hit && e.contains(hit) };
    });
    assert(geometry.top >= 0 && geometry.bottom <= 390 && geometry.uncovered, `datetime confirm unreachable: ${JSON.stringify(geometry)}`);
    await capture(page, 'booking-date-landscape.png');
    await confirm.tap();
    await panel.waitFor({ state: 'hidden' });
    assert(await page.getByRole('dialog').isVisible(), 'confirming a date unexpectedly closed the booking');
  },
  async 'profile-width'({ page }) {
    await page.setViewportSize({ width: 320, height: 568 });
    await page.goto(baseURL + '/profile');
    await page.locator('.profile-grid').waitFor();
    const g = await page.evaluate(() => ({ client: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth }));
    assert(g.scroll <= g.client, `profile overflows: ${JSON.stringify(g)}`);
  },
  async 'session-race'({ page, authMode }) {
    authMode.value = 'offline';
    await page.goto(baseURL + '/profile');
    await page.waitForURL('**/auth?redirect=/profile');
    authMode.value = 'pending';
    await Promise.all([
      page.waitForRequest(r => new URL(r.url()).pathname === '/api/auth/me'),
      page.getByRole('button', { name: '重试验证登录', exact: true }).tap(),
    ]);
    authMode.value = 'ok';
    await page.locator('.mobile-tabbar a[href="/tasks"]').tap();
    await page.waitForURL('**/tasks');
    const failed = page.waitForEvent('requestfailed', { predicate: r => new URL(r.url()).pathname === '/api/auth/me' });
    authMode.release();
    await failed;
    await settle(page);
    assert.equal(new URL(page.url()).pathname, '/tasks', 'stale recovery changed the newer navigation');
    assert.equal(await page.getByRole('button', { name: '退出', exact: true }).count(), 1, 'stale recovery discarded the newer verified session');
  },
  async 'session-retry'({ page, authMode }) {
    authMode.value = 'offline';
    await page.goto(baseURL + '/profile');
    await page.waitForURL('**/auth?redirect=/profile');
    assert.equal(await page.evaluate(() => localStorage.getItem('campushub_token')), token, 'network failure removed the session');
    assert.equal(await page.locator('.profile-grid').count(), 0, 'unverified session exposed private content');
    const retry = page.getByRole('button', { name: '重试验证登录', exact: true });
    await retry.waitFor();
    const recovery = await retry.boundingBox();
    assert(recovery.y >= 0 && recovery.y + recovery.height <= 844, 'session recovery is below the initial mobile viewport');
    await capture(page, 'session-recovery-mobile.png');
    authMode.value = 'ok';
    await retry.tap();
    await page.waitForURL('**/profile');
    await page.locator('.profile-grid').waitFor();
    authMode.value = 'expired';
    await page.reload();
    await page.waitForURL('**/auth?redirect=/profile');
    assert.equal(await page.evaluate(() => localStorage.getItem('campushub_token')), null, '401 failed to clear the session');
  },
};

(async () => {
  if (selected && !cases[selected]) throw new Error(`Unknown case: ${selected}`);
  const browser = await chromium.launch({ headless: true, ...(process.env.CAMPUSHUB_CHROME_PATH ? { executablePath: process.env.CAMPUSHUB_CHROME_PATH } : {}) });
  const results = [];
  try {
    for (const [name, run] of Object.entries(cases)) {
      if (selected && name !== selected) continue;
      const context = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true, deviceScaleFactor: 1 });
      const page = await context.newPage();
      page.setDefaultTimeout(8000);
      page.setDefaultNavigationTimeout(25000);
      const errors = [], requests = [], writes = [];
      const authMode = { value: 'ok' };
      page.on('pageerror', e => errors.push(e.message));
      await page.addInitScript(value => { if (!sessionStorage.getItem('fixture-initialized')) { localStorage.setItem('campushub_token', value); sessionStorage.setItem('fixture-initialized', 'yes'); } }, token);
      await page.route(baseURL + '/api/**', async route => {
        const q = route.request(), url = new URL(q.url()); requests.push(url);
        if (!['GET', 'HEAD', 'OPTIONS'].includes(q.method())) { writes.push(url.pathname); return route.abort('blockedbyclient'); }
        if (url.pathname === '/api/auth/me' && authMode.value === 'pending') {
          await new Promise(resolve => { authMode.release = resolve; });
          return route.abort('connectionfailed');
        }
        if (url.pathname === '/api/auth/me' && authMode.value === 'offline') return route.abort('connectionfailed');
        if (url.pathname === '/api/auth/me' && authMode.value === 'expired') return route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ success: false, message: '登录失效' }) });
        return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, message: 'isolated fixture', data: dataFor(url) }) });
      });
      try {
        await run({ page, authMode, requests });
        assert.deepEqual(errors, [], 'unexpected JavaScript errors');
        assert.deepEqual(writes, [], 'unexpected business writes');
        results.push({ name, passed: true }); console.log('PASS ' + name);
      } catch (error) {
        results.push({ name, passed: false, error: error.message, pageErrors: errors }); console.log('FAIL ' + name + ': ' + error.message);
        await capture(page, name + '-failed.png').catch(() => {});
      } finally { await context.close(); }
    }
  } finally { await browser.close(); }
  fs.writeFileSync(path.join(output, selected ? selected + '-results.json' : 'results.json'), JSON.stringify({ checkedAt: new Date().toISOString(), baseURL, fixturesOnly: true, results }, null, 2) + '\n');
  if (results.some(r => !r.passed)) process.exitCode = 1;
})().catch(error => { console.error(error.stack); process.exitCode = 1; });
