const fs = require('node:fs');
const path = require('node:path');
const { createDriver } = require('./native-driver.cjs');

(async () => {
  const d = await createDriver();
  const checks = [];
  const out = path.resolve(__dirname, '../../.claude/verification/native-miniprogram/filing-acceptance');
  fs.mkdirSync(out, { recursive: true });
  const state = () => d.app.evaluate(() => {
    const component = getCurrentPages()[getCurrentPages().length - 1].selectComponent('#launch-intro');
    return { showing: component.data.showing, mode: component.data.mode, number: component.data.filing.number };
  });
  const passed = name => { checks.push(name); console.log('PASS ' + name); };
  try {
    await d.app.send('App.enableLog');
    await d.reset();
    await d.app.evaluate(() => { getApp()._launchIntroClaimed = false; });
    const start = Date.now();
    await d.app.send('App.callWxMethod', { method: 'reLaunch', args: [{ url: '/pages/tasks/index' }] });
    await d.app.waitFor(async () => (await state()).showing);
    d.assert.equal((await state()).number, '浙ICP备2026099873号');
    d.assert.equal((await state()).mode, 'intro');
    passed('launch filing visible with exact supplied number');
    const component = await d.get('#launch-intro');
    const footer = await d.rect(await d.get('.intro-footer', component));
    const layer = await d.rect(await d.get('.intro-layer', component));
    d.assert(footer.top >= layer.top && footer.top + footer.height <= layer.top + layer.height + 1);
    d.assert(footer.left >= layer.left && footer.left + footer.width <= layer.left + layer.width + 1);
    passed('filing footer fits native viewport');
    await d.app.waitFor(async () => !(await state()).showing, 6000);
    d.assert(Date.now() - start < 6000);
    passed('intro dismisses automatically without waiting for business APIs');
    await d.go('/pages/pass/index', true);
    d.assert.equal((await state()).showing, false);
    passed('tab switch does not replay launch notice');
    await d.tap('#about-entry');
    await d.app.waitFor(async () => (await state()).mode === 'about' && (await state()).showing);
    passed('guest can open about and filing from passport');
    const about = await d.get('#launch-intro');
    const query = await d.rect(await d.get('.intro-query-button', about));
    const aboutLayer = await d.rect(await d.get('.intro-layer', about));
    d.assert(query.top >= aboutLayer.top && query.top + query.height <= aboutLayer.top + aboutLayer.height);
    passed('query action remains visible above safe area');
    if (process.env.WECHAT_CAPTURE !== '0') {
      const image = await d.app.send('App.captureScreenshot', {}, 25000);
      fs.writeFileSync(path.join(out, 'about-filing.png'), Buffer.from(image.data, 'base64'));
    }
    await d.tap(await d.get('.intro-dismiss', about));
    await d.app.waitFor(async () => !(await state()).showing);
    d.assert.equal(await d.data('aboutOpen'), false);
    await d.tap('#about-entry');
    await d.app.waitFor(async () => (await state()).showing);
    await d.go('/pages/tasks/index', true);
    d.assert.equal((await state()).showing, false);
    passed('closing or navigating away releases filing overlay');
    await d.app.evaluate(() => { getApp()._launchIntroClaimed = false; });
    await d.app.send('App.callWxMethod', { method: 'reLaunch', args: [{ url: '/pages/showcase/detail?id=1' }] });
    await d.app.waitFor(async () => (await state()).showing);
    const deep = await d.get('#launch-intro');
    await d.tap(await d.get('.intro-dismiss', deep));
    await d.app.waitFor(async () => !(await state()).showing);
    d.assert.equal((await d.page()).query.id, '1');
    passed('direct detail entry shows once and skip preserves route');
    d.assert.deepEqual(d.app.events.filter(event => event.method === 'App.exceptionThrown'), []);
    await d.go('/pages/tasks/index', true);
    fs.writeFileSync(path.join(out, 'results.json'), JSON.stringify({ checkedAt: new Date().toISOString(),
      environment: 'real native simulator with offline fixture API; launch claim reset only by test driver',
      checks, footer, layer, exceptions: [] }, null, 2));
  } finally { d.app.close(); }
})().catch(error => { console.error(error.stack); process.exit(1); });
