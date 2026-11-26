const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const root = path.resolve(__dirname, '..');
const src = path.join(root, 'src');
const walk = directory => fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => entry.isDirectory() ? walk(path.join(directory, entry.name)) : [path.join(directory, entry.name)]);
const all = walk(src);
const app = JSON.parse(fs.readFileSync(path.join(src, 'app.json'), 'utf8'));
assert.equal(app.pages.length, 15, 'Native package should have exactly 15 focused pages');
assert.equal(app.tabBar.list.length, 5, 'Native navigation must have exactly five tabs');
assert.deepEqual(app.tabBar.list.map(t => t.text), ['跑腿', '二手', '小店', '宣传', '个人']);
const components = Object.values(app.usingComponents).map(value => value.replace(/^\//, ''));
for (const base of [...app.pages, ...components]) {
  for (const extension of ['js', 'wxml', 'wxss', 'json']) assert(fs.existsSync(path.join(src, base + '.' + extension)), 'Missing ' + base + '.' + extension);
}
for (const file of all) {
  const text = fs.readFileSync(file, 'utf8');
  if (file.endsWith('.json')) JSON.parse(text);
  if (file.endsWith('.js')) {
    new vm.Script(text, { filename: file });
    assert(!/wx\.(requestPayment|uploadFile|chooseImage|chooseMedia|getLocation|login)\s*\(/.test(text), 'Unapproved platform capability in ' + file);
    assert(!/\/api\/admin\/|client\.(get|post|put)\(['"]\/(wallet|payment|admin)/.test(text), 'Excluded capability in ' + file);
    for (const match of text.matchAll(/require\(['"]([^'"]+)['"]\)/g)) {
      assert(match[1].startsWith('.'), 'Runtime package must not depend on external npm modules');
      assert(fs.existsSync(path.resolve(path.dirname(file), match[1] + '.js')), 'Missing local module in ' + file);
    }
  }
}
for (const base of [...app.pages, ...components]) {
  let definition;
  const originalPage = global.Page, originalComponent = global.Component;
  global.Page = value => { definition = value; };
  global.Component = value => { definition = value; };
  try {
    const script = path.join(src, base + '.js');
    delete require.cache[require.resolve(script)]; require(script);
    assert(definition, 'Page/component did not register: ' + base);
    const handlers = base.startsWith('components/') ? definition.methods || {} : definition;
    const template = fs.readFileSync(path.join(src, base + '.wxml'), 'utf8');
    for (const match of template.matchAll(/<button\b((?:[^>"']|"[^"]*"|'[^']*')*)>/g)) {
      assert(/\bclass\s*=/.test(match[1]), 'Every native button needs an explicit style class to override platform width rules: ' + base);
    }
    for (const match of template.matchAll(/(?:bind|catch)(?::[a-zA-Z0-9_-]+|[a-zA-Z]+)="([a-zA-Z_$][\w$]*)"/g)) {
      assert.equal(typeof handlers[match[1]], 'function', 'Missing handler ' + match[1] + ' in ' + base);
    }
  } finally { global.Page = originalPage; global.Component = originalComponent; }
}
for (const tab of app.tabBar.list) {
  for (const key of ['iconPath','selectedIconPath']) {
    const image = fs.readFileSync(path.join(src, tab[key]));
    assert(image.subarray(0, 8).equals(Buffer.from('89504e470d0a1a0a', 'hex')), 'Expected a PNG tab icon');
    assert.equal(image.readUInt32BE(16), 81);
    assert.equal(image.readUInt32BE(20), 81);
    assert(image.length < 40 * 1024, 'Native tab icon exceeds 40KB');
  }
}
const bytes = all.reduce((sum, file) => sum + fs.statSync(file).size, 0);
assert(bytes < 2 * 1024 * 1024, 'Native main package exceeds 2MiB');
console.log('PASS static package: ' + app.pages.length + ' pages, 5 tabs, ' + all.length + ' files, ' + bytes + ' bytes; no external runtime dependencies.');

const compilerRoot = process.env.WECHAT_COMPILER_DIR || 'C:/Program Files (x86)/Tencent/微信web开发者工具/resources/app.asar.unpacked/node_modules/wcc-exec';
const wcc = path.join(compilerRoot, 'wcc.exe');
const wcsc = path.join(compilerRoot, 'wcsc.exe');
if (fs.existsSync(wcc) && fs.existsSync(wcsc)) {
  const relative = extension => all.filter(f => f.endsWith(extension)).map(f => './' + path.relative(src, f).split(path.sep).join('/'));
  const xml = spawnSync(wcc, relative('.wxml'), { cwd: src, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 });
  if (xml.status !== 0) { console.error(xml.stderr || xml.stdout); process.exit(1); }
  console.log('PASS official WXML compiler');
  const css = spawnSync(wcsc, relative('.wxss'), { cwd: src, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 });
  if (css.status !== 0) { console.error(css.stderr || css.stdout); process.exit(1); }
  if (css.stderr) console.log(css.stderr.slice(0, 2500));
  console.log('PASS official WXSS compiler');
} else {
  console.log('SKIPPED official WXML/WXSS compiler: set WECHAT_COMPILER_DIR to the installed developer-tool compiler directory.');
}
