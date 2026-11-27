const { ApiError } = require('./request');
const kinds=['GOODS','SHOP','SERVICE_ITEM','PROJECT_AD'];
function path(kind,id,photo){if(!kinds.includes(kind)||!Number.isSafeInteger(+id)||+id<1||photo!=null&&(!Number.isSafeInteger(+photo)||+photo<1))throw Error('图片编号无效');return '/catalog-media/'+kind+'/'+Number(id)+(photo==null?'':'/'+Number(photo));}

// Only a plain ApiResponse failure is user-facing. Never echo wx diagnostics,
// HTML gateway pages, stack traces or local file paths.
function uploadError(status = 0, body, diagnostic = '') {
  const message = body?.success === false && typeof body.message === 'string' ? body.message.trim().replace(/^(?:照片|图片)上传失败[：:]\s*/, '') : '';
  let reason = '未收到具体原因，请重试';
  if (/^[㐀-鿿A-Za-z0-9 ，。！？、：（）()％%;；,.!?-]{1,160}$/.test(message) &&
      !/^(?:(?:照片|图片)?上传失败|请求失败|操作失败)$/.test(message) &&
      !/(?:exception|stack trace|traceback|\b\w*Error\b|\bSQL\b|[A-Za-z]\.[A-Za-z])/i.test(message)) {
    reason = message === '图片尺寸过大' ? '图片尺寸过大，请缩小图片宽高后重试' : message;
  } else if (status === 413) reason = '照片文件过大，请压缩后重试';
  else if (status === 403) reason = '当前账号无权限上传照片';
  else if (status === 401) reason = '登录已过期，请重新登录';
  else if (status === 408 || /timeout|timed out/i.test(diagnostic)) reason = '上传超时，请重试';
  else if (/network|offline|disconnected|connection (?:lost|refused|reset)/i.test(diagnostic)) reason = '网络连接失败，请检查网络后重试';
  else if (status >= 500) reason = '服务暂时不可用，请稍后重试';
  return new ApiError('照片上传失败：' + reason, status);
}
function transfer(method, options) {
  const app = getApp(), token = app.auth.token;
  if (!token || !app.auth.user || app.config.baseUrl === 'https://offline-preview.invalid/api') throw new ApiError('需要登录后连接服务器');
  return new Promise((resolve, reject) => {
    wx[method]({ ...options, header: { Authorization: 'Bearer ' + token }, timeout: 60000,
      success(r) {
        try {
          if (token !== app.auth.token || !app.auth.user) throw Error('账号已切换');
          const status = r.statusCode, ok = status >= 200 && status < 300;
          if (status === 401) app.auth.invalidate(token);
          if (method === 'downloadFile') {
            if (!ok) throw Error('图片暂不可用');
            return resolve(r.tempFilePath);
          }
          let body = r.data;
          if (typeof body === 'string') { try { body = JSON.parse(body); } catch { body = null; } }
          if (!ok || body?.success !== true || !Number.isSafeInteger(body.data?.id) || body.data.id < 1) throw uploadError(status, body);
          resolve(body.data);
        } catch (e) { if (r.tempFilePath) app.taskMedia.discard(r.tempFilePath); reject(e); }
      },
      fail: e => reject(method === 'uploadFile' ? uploadError(0, null, e?.errMsg) : Error('图片传输失败，请重试')),
    });
  });
}
module.exports={path,transfer};
