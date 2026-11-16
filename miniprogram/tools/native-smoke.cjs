const fs = require('node:fs');
const path = require('node:path');
const { createDriver } = require('./native-driver.cjs');

(async () => {
  const d = await createDriver();
  const results = [];
  async function run(name, test) {
    try { await test(); results.push({ name, passed: true }); console.log('PASS ' + name); }
    catch (error) { results.push({ name, passed: false, error: error.message }); console.log('FAIL ' + name + ': ' + error.message); await d.screenshot(name + '-failed').catch(() => {}); }
  }
  const componentData = async (component, field) => (await d.app.send('Element.getData', { ...component, path: field })).data;
  const disabled = async element => (await d.app.send('Element.getProperties', { ...element, names: ['disabled'] })).properties[0] === 'true';
  try {
    await d.app.send('App.enableLog');
    await d.reset();
    await d.app.waitFor(async () => (await d.all('.take-button')).length === 4);
    await run('ticket-buttons-fit', async () => {
      const ticket = await d.rect(await d.get('.ticket'));
      const button = await d.rect(await d.get('.take-button'));
      d.assert(button.left >= ticket.left && button.left + button.width <= ticket.left + ticket.width + 1, 'native action button extends beyond its ticket');
      await d.screenshot('01-tasks');
    });
    await run('guest-login-and-runner-agreement', async () => {
      await d.tap('.take-button'); await d.ready('/pages/pass/auth');
      await d.input('input[data-field="loginForm.username"]', 'offline_demo');
      await d.input('input[data-field="loginForm.password"]', 'offline_demo');
      await d.tap(await d.button('登录并继续')); await d.ready('/pages/tasks/index');
      await d.app.waitFor(async () => (await d.all('.take-button')).length === 4);
      await d.tap('.take-button'); await d.app.waitFor(() => d.data('unlock'));
      const role = await d.get('#runner-role');
      await d.app.waitFor(async () => !(await componentData(role, 'loading')) && !!(await componentData(role, 'protocol')));
      const submit = await d.button('同意协议并开通', role);
      d.assert.equal(await disabled(submit), true, 'agreement submission must start disabled');
      await d.screenshot('02-agreement-unchecked');
      await d.tap(await d.get('label', role));
      await d.app.waitFor(() => componentData(role, 'accepted'));
      d.assert.equal(await disabled(submit), false);
      const sheet = await d.get('#role-sheet', role);
      const footer = await d.rect(await d.get('.sheet-footer', sheet));
      const rect = await d.rect(submit);
      d.assert(rect.top >= footer.top && rect.top + rect.height <= footer.top + footer.height + 1, 'agreement action is outside footer');
      await d.screenshot('03-agreement-checked');
      await d.tap(submit);
      await d.app.waitFor(async () => !(await d.data('unlock')) && (await d.app.evaluate(() => getApp().auth.user.roles)).includes('ROLE_RUNNER'));
    });
    await run('native-task-grab', async () => {
      await d.go('/pages/tasks/index', true);
      await d.app.waitFor(async () => (await d.all('.take-button')).length === 4);
      await d.tap('.take-button'); await d.app.waitFor(async () => !!(await d.data('selected')));
      await d.screenshot('04-task-confirm');
      await d.tap(await d.button('确认接单'));
      await d.ready('/pages/tasks/detail');
      await d.app.waitFor(async () => !!(await d.data('task')) && !(await d.data('loading')));
      d.assert.equal(await d.data('isAssigned'), true);
      d.assert.equal((await d.data('action')).status, 'HEADING_TO_PICKUP');
      await d.screenshot('05-task-detail');
    });
    await run('shop-booking-and-private-contact', async () => {
      await d.go('/pages/shops/index', true);
      await d.app.waitFor(async () => (await d.all('.studio')).length === 2);
      await d.screenshot('06-shops');
      await d.tap('.studio-art'); await d.ready('/pages/shops/detail');
      await d.app.waitFor(async () => (await d.data('services')).length > 0);
      d.assert.equal((await d.data('contacts')).length, 0);
      await d.tap(await d.button('预约这项服务')); await d.app.waitFor(() => d.data('booking'));
      const sheet = await d.get('#booking-sheet');
      const footer = await d.rect(await d.get('.sheet-footer', sheet));
      const submit = await d.button('发送预约，等待主理人确认');
      const rect = await d.rect(submit);
      d.assert(rect.top >= footer.top && rect.top + rect.height <= footer.top + footer.height + 1, 'booking button outside footer');
      await d.screenshot('07-booking');
      await d.tap(submit); await d.app.waitFor(async () => !(await d.data('booking')) && (await d.data('contacts')).length > 0);
      await d.screenshot('08-shop-contact');
    });
    await run('showcase-favorite-and-comment', async () => {
      await d.go('/pages/showcase/index', true);
      await d.app.waitFor(async () => (await d.data('projects')).length >= 2);
      await d.screenshot('09-showcase');
      await d.tap('.poster'); await d.ready('/pages/showcase/detail');
      await d.app.waitFor(async () => !!(await d.data('project')) && !(await d.data('loading')));
      d.assert.equal((await d.data('contacts')).length, 0);
      await d.tap(await d.button('收藏'));
      await d.app.waitFor(async () => (await d.data('project')).favorited === true && !(await d.data('busy')));
      await d.tap(await d.button('留言')); await d.app.waitFor(() => d.data('commentSheet'));
      await d.input('textarea', '仅用于离线模拟器验收的留言');
      const submit = await d.button('发布公开留言');
      await d.app.waitFor(async () => !(await disabled(submit)));
      await d.tap(submit);
      await d.app.waitFor(async () => !(await d.data('commentSheet')) && (await d.data('comments')).length === 1);
      await d.screenshot('10-showcase-detail');
    });
    await run('pass-card-booking-and-notices', async () => {
      await d.go('/pages/pass/index', true);
      await d.app.waitFor(async () => !!(await d.data('profile')) && !(await d.data('loading')));
      d.assert.equal((await d.data('roleCards')).find(role => role.value === 'RUNNER').active, true);
      await d.screenshot('11-pass');
      await d.tap('.pass-tab[data-section="orders"]');
      await d.app.waitFor(async () => (await d.data('orders')).length === 1 && !(await d.data('loading')));
      await d.tap('.record'); await d.app.waitFor(() => d.data('orderOpen'));
      d.assert.equal((await d.data('selectedOrder')).status, 'REQUESTED');
      await d.screenshot('12-order');
      const sheet = await d.get('#order-sheet');
      await d.tap(await d.get('.sheet-close', sheet));
      await d.app.waitFor(async () => !(await d.data('orderOpen')));
      await d.tap('.pass-tab[data-section="notices"]');
      await d.app.waitFor(async () => !(await d.data('loading')));
      d.assert.deepEqual(await d.data('notices'), []);
      await d.screenshot('13-notices');
    });
    await run('task-publish-three-step-form', async () => {
      await d.go('/pages/tasks/publish');
      await d.input('input[data-field="form.originDetail"]', '图书馆门口');
      await d.input('input[data-field="form.destinationDetail"]', '食堂门口');
      await d.tap(await d.button('继续')); await d.app.waitFor(async () => (await d.data('step')) === 1);
      await d.input('input[data-field="form.title"]', '离线验收的跑腿样例');
      await d.input('textarea[data-field="form.description"]', '只用于本机模拟器，不会发布到生产。');
      await d.input('input[data-field="form.rewardAmount"]', '6');
      await d.tap(await d.button('继续')); await d.app.waitFor(async () => (await d.data('step')) === 2);
      await d.screenshot('14-task-publish');
      await d.tap(await d.button('确认发布')); await d.ready('/pages/tasks/detail');
      await d.app.waitFor(async () => !!(await d.data('task')) && !(await d.data('loading')));
      d.assert.equal(await d.data('isOwner'), true);
    });
    await run('publicity-local-draft-is-not-submitted', async () => {
      await d.go('/pages/showcase/publish');
      await d.app.waitFor(async () => await d.data('ready'));
      await d.input('input[data-field="title"]', '离线草稿，不提交审核');
      await d.tap(await d.button('存为本机草稿'));
      await d.app.waitFor(async () => String(await d.data('savedMessage')).includes('本机草稿'));
      d.assert.equal(await d.data('draftId'), '');
      await d.screenshot('15-showcase-draft');
    });
    await run('merchant-role-awaits-review', async () => {
      await d.go('/pages/pass/index', true);
      await d.app.waitFor(async () => !!(await d.data('profile')) && !(await d.data('loading')));
      await d.tap('.pass-tab[data-section="pass"]');
      await d.app.waitFor(async () => !(await d.data('loading')));
      await d.tap('.role-tile[data-role="SHOP_MERCHANT"]');
      await d.app.waitFor(() => d.data('unlock'));
      const role = await d.get('#pass-role');
      await d.app.waitFor(async () => !!(await componentData(role, 'protocol')) && !(await componentData(role, 'loading')));
      await d.tap(await d.get('label', role));
      await d.app.waitFor(() => componentData(role, 'accepted'));
      await d.tap(await d.button('同意协议并申请核验', role));
      await d.app.waitFor(async () => !(await d.data('unlock')) && (await d.data('roleCards')).some(item => item.value === 'SHOP_MERCHANT' && item.pending));
      d.assert.equal((await d.app.evaluate(() => getApp().auth.user.roles)).includes('ROLE_SHOP_MERCHANT'), false);
    });
    await d.go('/pages/tasks/index', true);
    const errors = d.app.events.filter(event => event.method === 'App.exceptionThrown').map(event => event.params);
    d.assert.deepEqual(errors, [], 'native runtime raised an exception');
    const system = await d.app.evaluate(() => { const info = wx.getWindowInfo(); return { windowWidth: info.windowWidth, windowHeight: info.windowHeight, safeArea: info.safeArea }; });
    fs.writeFileSync(path.join(d.output, 'runtime.json'), JSON.stringify({ environment: 'WeChat native simulator; isolated offline fixture APIs', system, exceptions: errors }, null, 2));
  } finally {
    fs.writeFileSync(path.join(d.output, 'results.json'), JSON.stringify({ checkedAt: new Date().toISOString(), environment: 'real WeChat developer-tool simulator with isolated offline API fixtures', results }, null, 2) + '\n');
    d.app.close();
  }
  if (results.some(result => !result.passed)) process.exitCode = 1;
})().catch(error => { console.error(error.stack); process.exitCode = 1; });
