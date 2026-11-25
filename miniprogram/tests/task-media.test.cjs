const { test } = require('node:test');
const assert = require('node:assert/strict');
const rules = require('../src/lib/rules');
const display = require('../src/lib/display');

test('accepts estimated kilograms at gram precision without silently rounding', () => {
  assert.equal(rules.weightGrams('0.001'), 1);
  assert.equal(rules.weightGrams('1.25'), 1250);
  for (const value of ['', '0', '-1', '1.0001', '1e1', '100.001']) assert.throws(() => rules.weightGrams(value), /重量/);
  assert.equal(display.weightLabel(null), '未填写'); assert.equal(display.weightLabel(250), '250 克');
  assert.equal(display.weightLabel(1500), '1.5 公斤');
});
function client(platform, baseUrl = 'http://127.0.0.1:28180/api') {
  const { createTaskMedia } = require('../src/lib/task-media');
  const invalidations = [];
  const auth = { token:'synthetic', user:{id:1}, invalidate:t => invalidations.push(t) };
  return { media:createTaskMedia({baseUrl}, auth, platform), auth, invalidations };
}
const photo = { path:'wxfile://tmp/local.jpg', size:100, key:'test-request-key' };
test('uploads an item photo to the task-specific route with bearer and stable request key', async () => {
  let options;
  const { media } = client({ uploadFile: o => { options=o; o.success({statusCode:200, data:JSON.stringify({success:true,data:{id:8}})}); } });
  assert.equal((await media.upload(10, 'REQUEST_IMAGE', photo)).id, 8);
  assert.equal(options.url, 'http://127.0.0.1:28180/api/tasks/10/request-images');
  assert.deepEqual(options.header,{Authorization:'Bearer synthetic'}); assert.equal(options.name,'file');
  assert.equal(options.formData.clientRequestId, photo.key);
});
test('binary transfers protect target, old session, and offline fixture boundary', async () => {
  let opts;
  const { media, auth, invalidations }=client({downloadFile:o=>{opts=o;}});
  await assert.rejects(async()=>media.download('https://other',2));
  const d=media.download(10,2); opts.success({statusCode:403}); await assert.rejects(d, /权限/);
  assert.equal(invalidations.length,0);
  const later=media.download(10,2); auth.token='new'; opts.success({statusCode:401}); await assert.rejects(later);
  assert(!invalidations.includes('new'));
  const offline=client({ uploadFile:()=>{throw new Error('network called');} },'https://offline-preview.invalid/api').media;
  await assert.rejects(async()=>offline.upload(10,'DELIVERY_PHOTO',photo), /离线/);
});
test('photo-mode runner submits delivery proof, not a generic pending transition', () => {
  assert.equal(rules.nextTaskAction({workflowStatus:'DELIVERING',verificationMode:'PHOTO_AND_CONFIRMATION'},true), null);
});
