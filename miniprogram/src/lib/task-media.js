const { ApiError } = require('./request');
const MAX_PHOTOS = 3;
const MAX_BYTES = 5 * 1024 * 1024;
const photoKey = () => 'photo-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 12);
function id(value) {
  if (!Number.isSafeInteger(Number(value)) || Number(value) < 1) throw new Error('照片或任务编号无效');
  return Number(value);
}
function mediaType(kind) {
  if (kind === 'REQUEST_IMAGE') return 'request-images';
  if (kind === 'DELIVERY_PHOTO') return 'evidence';
  throw new Error('照片类型无效');
}
function createTaskMedia(config, auth, platform) {
  const base = config.baseUrl;
  const offline = base === 'https://offline-preview.invalid/api';
  function actor() {
    if (offline) throw new Error('离线样例不上传或下载照片，请连接隔离后端');
    if (!auth.user || !auth.token) throw new ApiError('请登录后操作', 401);
    return { token:auth.token, userId:auth.user.id };
  }
  function current(sent) { return sent.token === auth.token && auth.user && sent.userId === auth.user.id; }
  function check(status, sent) {
    if (!current(sent)) throw new ApiError('账号已切换，操作结果已丢弃');
    if (status === 401) { auth.invalidate(sent.token); throw new ApiError('请重新登录后继续',401); }
    if (status === 403) throw new ApiError('当前账号没有照片权限',403);
    if (status === 413) throw new ApiError('单张照片不能超过 5 MiB',413);
    if (status < 200 || status >= 300) throw new ApiError('照片传输失败，请重试',status);
  }
  function discard(path) {
    if (path && typeof platform.getFileSystemManager === 'function') platform.getFileSystemManager().unlink({filePath:path,fail() {}});
  }
  async function pick(count) {
    const sent=actor();
    if (!Number.isInteger(count) || count < 1 || count > MAX_PHOTOS) throw new Error('每组最多 3 张照片');
    const files = await new Promise((resolve,reject) => platform.chooseMedia({ count, mediaType:['image'],sourceType:['album','camera'],sizeType:['compressed'],
      success:r=>resolve(r.tempFiles || []),fail:e=>/cancel/i.test(e.errMsg||'') ? resolve([]) : reject(new Error('无法选择照片，请检查相册权限')) }));
    if (files.length > count) throw new Error('照片数量超限');
    const photos=[];
    for (const f of files) {
      if (!current(sent)) throw new Error('账号已切换');
      if (!f.tempFilePath || !f.size || f.size > MAX_BYTES) throw new Error('单张照片需大于 0 且不超过 5 MiB');
      const type = await new Promise((resolve,reject)=>platform.getImageInfo({src:f.tempFilePath,success:r=>resolve(r.type),fail:()=>reject(new Error('无法读取图片'))}));
      if (!/^(jpe?g|png)$/i.test(type)) throw new Error('请选择 JPG 或 PNG 照片');
      photos.push({key:photoKey(),path:f.tempFilePath,size:f.size});
    }
    if (!current(sent)) throw new Error('账号已切换');
    return photos;
  }
  async function upload(taskId,kind,photo,onProgress) {
    const sent=actor(), url=base+'/tasks/'+id(taskId)+'/'+mediaType(kind);
    return new Promise((resolve,reject)=>{
      const transfer=platform.uploadFile({url,filePath:photo.path,name:'file',header:{Authorization:'Bearer '+sent.token},formData:{clientRequestId:photo.key},timeout:60000,
        success:r=>{try { check(r.statusCode,sent); const body=typeof r.data==='string'?JSON.parse(r.data):r.data;
          if (!body || body.success!==true || !body.data || !body.data.id) throw new Error(body && body.message || '上传响应无效'); resolve(body.data);
        } catch(e){reject(e);} },fail:()=>reject(new ApiError('照片未确认上传，请重试',0,true))});
      if (transfer && transfer.onProgressUpdate && onProgress) transfer.onProgressUpdate(r=>{if(current(sent))onProgress(r.progress);});
    });
  }
  async function download(taskId,photoId) {
    const sent=actor(),url=base+'/tasks/'+id(taskId)+'/evidence/'+id(photoId);
    return new Promise((resolve,reject)=>platform.downloadFile({url,header:{Authorization:'Bearer '+sent.token},timeout:60000,
      success:r=>{try{check(r.statusCode,sent);if(!r.tempFilePath)throw new Error('照片暂不可用');resolve(r.tempFilePath);}catch(e){discard(r.tempFilePath);reject(e);}},
      fail:()=>reject(new ApiError('照片未下载，请重试',0,true))}));
  }
  return {pick,upload,download,discard,offline};
}
module.exports={createTaskMedia,MAX_PHOTOS,MAX_BYTES};
