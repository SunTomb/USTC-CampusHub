const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const root = path.resolve(__dirname, '..');
const out = path.resolve(root, '../.claude/verification/native-miniprogram/api-integration');
const marker = path.join(out, '.native-api-integration-owned');
if (fs.existsSync(out) && !fs.existsSync(marker)) throw new Error('Refusing to overwrite an unowned directory');
fs.mkdirSync(out, { recursive: true });
fs.writeFileSync(marker, 'Test-only project; real isolated backend, never upload.\n');
function copy(source, destination) {
  fs.mkdirSync(destination, { recursive: true });
  for (const item of fs.readdirSync(source, { withFileTypes: true })) {
    const a = path.join(source, item.name), b = path.join(destination, item.name);
    if (item.isDirectory()) copy(a, b);
    else { const content = fs.readFileSync(a); fs.writeFileSync(b, content); }
  }
}
copy(path.join(root, 'src'), path.join(out, 'src'));
const config = JSON.parse(fs.readFileSync(path.join(root, 'project.config.json'), 'utf8'));
config.projectname = 'isolated-native-api-integration';
config.description = '本机可写H2联调，禁止上传发布';
config.setting.urlCheck = false;
fs.writeFileSync(path.join(out, 'project.config.json'), JSON.stringify(config, null, 2));
function hashes(dir, rel = '') {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap(item => item.isDirectory()
    ? hashes(path.join(dir, item.name), rel + item.name + '/')
    : [{ path: rel + item.name, sha256: crypto.createHash('sha256').update(fs.readFileSync(path.join(dir, item.name))).digest('hex') }]);
}
const files = hashes(path.join(root, 'src'));
if (JSON.stringify(files) !== JSON.stringify(hashes(path.join(out, 'src')))) throw new Error('Source copy differs');
fs.writeFileSync(path.join(out, 'source-manifest.json'), JSON.stringify(files, null, 2));
console.log('Created test-only API project with byte-identical formal src; ' + files.length + ' files.');
