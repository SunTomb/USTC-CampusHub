const { randomUUID } = require('node:crypto');

async function connect(port = 9420) {
  const socket = new WebSocket('ws://127.0.0.1:' + port);
  const pending = new Map();
  const events = [];
  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('Native automation connection timed out')), 10000);
    socket.addEventListener('open', () => { clearTimeout(timer); resolve(); }, { once: true });
    socket.addEventListener('error', () => { clearTimeout(timer); reject(new Error('Native automation connection failed')); }, { once: true });
  });
  socket.addEventListener('message', message => {
    const response = JSON.parse(String(message.data));
    if (!response.id) { events.push({ method: response.method, params: response.params }); return; }
    const task = pending.get(response.id);
    if (!task) return;
    pending.delete(response.id); clearTimeout(task.timer);
    if (response.error) task.reject(new Error(task.method + ': ' + (response.error.message || 'Native automation error')));
    else task.resolve(response.result);
  });
  function send(method, params = {}, timeout = 15000) {
    return new Promise((resolve, reject) => {
      const id = randomUUID();
      const timer = setTimeout(() => { pending.delete(id); reject(new Error(method + ' timed out')); }, timeout);
      pending.set(id, { resolve, reject, timer, method });
      socket.send(JSON.stringify({ id, method, params }));
    });
  }
  async function evaluate(fn, ...args) {
    const result = await send('App.callFunction', { functionDeclaration: fn.toString(), args });
    return result.result;
  }
  async function waitFor(probe, timeout = 15000) {
    const end = Date.now() + timeout;
    let last;
    while (Date.now() < end) {
      try { const value = await probe(); if (value) return value; } catch (error) { last = error; }
      await new Promise(resolve => setTimeout(resolve, 150));
    }
    throw last || new Error('Native condition did not become ready');
  }
  return {
    send, evaluate, waitFor, events,
    async assertOffline() {
      const base = await waitFor(() => evaluate(() => getApp().config && getApp().config.baseUrl));
      if (base !== 'https://offline-preview.invalid/api') throw new Error('Refusing to automate a project with real business APIs');
    },
    close() { pending.forEach(task => { clearTimeout(task.timer); task.reject(new Error('Automation closed')); }); pending.clear(); socket.close(); },
  };
}
module.exports = { connect };
