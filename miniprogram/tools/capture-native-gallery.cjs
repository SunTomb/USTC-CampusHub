const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { createDriver } = require('./native-driver.cjs');
(async () => {
  const d = await createDriver();
  const captures = [];
  try {
    for (const [route, file] of [
      ['/pages/tasks/index', 'gallery-tasks'],
      ['/pages/shops/index', 'gallery-shops'],
      ['/pages/showcase/index', 'gallery-showcase'],
      ['/pages/pass/index', 'gallery-pass'],
    ]) {
      if (process.argv[2] && file !== process.argv[2]) continue;
      await d.go(route, true);
      if (route === '/pages/pass/index') await d.tap('.pass-tab[data-section="pass"]');
      await d.app.waitFor(async () => !(await d.data('loading')));
      await d.screenshot(file);
      const data = fs.readFileSync(path.join(d.output, file + '.png'));
      captures.push({ route, file: file + '.png', capturedAt: new Date().toISOString(), width: data.readUInt32BE(16), height: data.readUInt32BE(20), sha256: crypto.createHash('sha256').update(data).digest('hex') });
      console.log('CAPTURED ' + route);
    }
    await d.go('/pages/tasks/index', true);
  } finally {
    const manifest = path.join(d.output, 'gallery.json');
    const previous = process.argv[2] && fs.existsSync(manifest) ? JSON.parse(fs.readFileSync(manifest, 'utf8')).captures || [] : [];
    fs.writeFileSync(manifest, JSON.stringify({ environment: 'actual native WeChat simulator, offline sample data only', captures: [...previous.filter(item => !captures.some(updated => item.file === updated.file)), ...captures] }, null, 2));
    d.app.close();
  }
})().catch(error => { console.error(error.stack); process.exitCode = 1; });
