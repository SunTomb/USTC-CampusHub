const fs=require('node:fs'),nodePath=require('node:path');
const componentFile=ext=>nodePath.join(__dirname,'../src/components/catalog-photos/index.'+ext);
const test=require('node:test'),assert=require('node:assert/strict');const {path}=require('../src/lib/catalog-transfer');
function harness(){let def;global.Component=v=>{def=v};const src=require.resolve('../src/components/catalog-photos/index');delete require.cache[src];require(src);const auth={token:'fixture',user:{id:5},subscribe(){return()=>{}}};global.getApp=()=>({auth,config:{baseUrl:'http://localhost:19100/api'},client:{get:async()=>[]},taskMedia:{discard(){}}});const el={properties:{kind:'GOODS',objectId:0,editable:true},data:structuredClone(def.data),setData(v){Object.assign(this.data,v)},triggerEvent(){},...def.methods};def.lifetimes.attached.call(el);return {el,def,auth};}
test('catalog transfers keep private task images and arbitrary targets out of their URL space',()=>{assert.equal(path('SHOP',2,3),'/catalog-media/SHOP/2/3');assert.throws(()=>path('TASK',1));assert.throws(()=>path('GOODS','../1'));});
test('retry uploads only missing image with the original key after target creation',async()=>{const {el,def}=harness();const keys=[];global.wx={uploadFile(o){keys.push(o.formData.clientRequestId);if(keys.length===2)o.fail({});else o.success({statusCode:200,data:{success:true,data:{id:7}}});}};el.setData({queue:[{path:'tmp-a',key:'img-a'},{path:'tmp-b',key:'img-b'}]});el.properties.objectId=12;def.observers['kind,objectId'].call(el,'GOODS',12);await assert.rejects(el.saveTo(12));assert.deepEqual(el.data.queue.map(x=>x.key),['img-b']);await el.saveTo(12);assert.deepEqual(keys,['img-a','img-b','img-b']);assert.equal(el.data.queue.length,0);});

test('an empty thumb keeps the same square and allows taps to reach the card', async () => {
  const {el,def}=harness();
  Object.assign(el.properties,{variant:'thumb',fallbackChar:'书',objectId:4,editable:false});
  await el.load();
  assert.equal(el.data.totalCount,0);
  assert.deepEqual(el.data.allUrls,[]);
  assert.equal(def.properties.variant.value,'');
  const template=fs.readFileSync(componentFile('wxml'),'utf8');
  const style=fs.readFileSync(componentFile('wxss'),'utf8');
  assert.match(template, /class="cover-square/);
  assert.match(template, /<view wx:else class="cover-fallback[^>]*>[\s\S]*?fallbackChar/);
  assert.doesNotMatch(template.match(/<view wx:else class="cover-fallback[^>]*>/)[0], /catchtap/);
  assert.match(style,/width:\s*var\(--catalog-thumb-size,\s*184rpx\)/);
  assert.match(style,/height:\s*var\(--catalog-thumb-size,\s*184rpx\)/);
});

test('compact cover previews the full published album; editor includes queued local files',async()=>{
  const {el}=harness(),app=getApp();
  global.getApp=()=>app;
  app.client.get=async()=>[{id:7,published:true},{id:8,published:true}];
  Object.assign(el.properties,{objectId:12,compact:true,editable:false});
  await el.load();
  const urls=[7,8].map(id=>app.config.baseUrl+path('GOODS',12,id));
  assert.equal(el.data.totalCount,2);
  assert.deepEqual(el.data.allUrls,urls);
  let shown;global.wx={previewImage:o=>{shown=o}};
  const tap=src=>({currentTarget:{dataset:{src}}});
  el.preview(tap(urls[0]));
  assert.equal(shown.current,urls[0]); assert.deepEqual(shown.urls,urls);
  el.properties.editable=true;
  el.setData({queue:[{path:'tmp-3',key:'local'}]});
  el.preview(tap('tmp-3'));
  assert.equal(shown.current,'tmp-3'); assert.deepEqual(shown.urls,[...urls,'tmp-3']);
  global.wx={};el.preview(tap(urls[1]));assert.equal(el.data.previewSrc,urls[1]);
  el.closePreview();assert.equal(el.data.previewSrc,'');
});
