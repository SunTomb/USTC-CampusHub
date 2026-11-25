const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { connect } = require('./native-automation.cjs');

async function createDriver() {
  const app = await connect(Number(process.env.WECHAT_AUTO_PORT || 9420));
  await app.assertOffline();
  const output = path.resolve(__dirname, '../../.claude/verification/native-miniprogram/native-acceptance');
  fs.mkdirSync(output, { recursive: true });
  const page = () => app.send('App.getCurrentPage');
  async function get(selector, parent) {
    const p = await page();
    return { ...await app.send(parent ? 'Element.getElement' : 'Page.getElement', { ...(parent || { pageId: p.pageId }), selector }), pageId: p.pageId };
  }
  async function all(selector, parent) {
    const p = await page();
    const result = await app.send(parent ? 'Element.getElements' : 'Page.getElements', { ...(parent || { pageId: p.pageId }), selector });
    return result.elements.map(element => ({ ...element, pageId: p.pageId }));
  }
  async function data(key) { const p = await page(); return (await app.send('Page.getData', { pageId: p.pageId, path: key })).data; }
  async function tap(selector, parent) { const element = typeof selector === 'string' ? await get(selector, parent) : selector; return app.send('Element.tap', element); }
  async function text(element) { return (await app.send('Element.getDOMProperties', { ...element, names: ['innerText'] })).properties[0]; }
  async function button(label, parent) {
    for (const element of await all('button', parent)) if ((await text(element) || '').trim() === label) return element;
    throw new Error('No native button with text: ' + label);
  }
  async function input(selector, value, parent) { const element = await get(selector, parent); return app.send('Element.callFunction', { ...element, functionName: element.tagName === 'textarea' ? 'textarea.input' : 'input.input', args: [value] }); }
  async function rect(element) {
    const offset = await app.send('Element.getOffset', element);
    const size = await app.send('Element.getDOMProperties', { ...element, names: ['offsetWidth', 'offsetHeight'] });
    return { ...offset, width: size.properties[0], height: size.properties[1] };
  }
  async function ready(route) { return app.waitFor(async () => (await page()).path === route.replace(/^\//, '') && !(await data('loading'))); }
  async function go(url, tab = false) { await app.send('App.callWxMethod', { method: tab ? 'switchTab' : 'navigateTo', args: [{ url }] }); return ready(url.split('?')[0]); }
  async function screenshot(name) {
    if (process.env.WECHAT_CAPTURE === '0') return;
    let failure;
    for (let attempt = 0; attempt < 2; attempt++) {
      try {
        const image = await app.send('App.captureScreenshot');
        fs.writeFileSync(path.join(output, name + '.png'), Buffer.from(image.data, 'base64'));
        return;
      } catch (error) {
        failure = error;
        if (!/timeout/i.test(error.message)) throw error;
      }
    }
    throw failure;
  }
  async function reset() {
    await app.evaluate(() => {
      const app = getApp();
      if (app.config.baseUrl !== 'https://offline-preview.invalid/api' || typeof app.resetOfflineSample !== 'function') throw new Error('Not an offline sample');
      app.auth.logout(); app.resetOfflineSample();
      wx.removeStorageSync('woke:https://offline-preview.invalid/api:900001:showcase-editor');
      wx.removeStorageSync('woke:https://offline-preview.invalid/api:900001:published-task-ids');
    });
    await app.send('App.callWxMethod', { method: 'reLaunch', args: [{ url: '/pages/tasks/index' }] });
    await ready('/pages/tasks/index');
  }
  return { app, output, page, get, all, data, tap, text, button, input, rect, ready, go, screenshot, reset, assert };
}
module.exports = { createDriver };
