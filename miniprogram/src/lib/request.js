class ApiError extends Error {
  constructor(message, status = 0, network = false) {
    super(message); this.name = 'ApiError'; this.status = status; this.network = network;
  }
}

const ALLOWED = /^\/(auth|platform|identity|tasks|task-applications|shops|service-items|service-orders|project-ads|interactions|users|notifications)(\/|$|\?)/;
function createClient({ baseUrl, request, getToken, onUnauthorized }) {
  function send(method, path, data, options = {}) {
    return new Promise((resolve, reject) => {
      if (!ALLOWED.test(path) || path.includes('..') || path.includes('://') || path.startsWith('//')) {
        reject(new ApiError('不支持的请求路径')); return;
      }
      const sentToken = getToken();
      let url = baseUrl.replace(/\/$/, '') + path;
      if (method === 'GET' && data) {
        const query = Object.keys(data).filter(k => data[k] !== undefined && data[k] !== null && data[k] !== '').map(k => encodeURIComponent(k) + '=' + encodeURIComponent(String(data[k]))).join('&');
        if (query) url += (url.includes('?') ? '&' : '?') + query;
      }
      request({
        url, method, timeout: 10000,
        data: method === 'GET' ? undefined : data,
        header: { 'Content-Type': 'application/json', ...(sentToken ? { Authorization: 'Bearer ' + sentToken } : {}) },
        success(response) {
          const status = response.statusCode;
          if (status === 401) {
            if (!options.quiet401) onUnauthorized(sentToken);
            reject(new ApiError('请重新登录后继续', 401)); return;
          }
          if (status === 403) { reject(new ApiError('当前账号暂不能进行此操作', 403)); return; }
          if (status < 200 || status >= 300) { reject(new ApiError('服务暂时不可用，请稍后重试', status)); return; }
          const body = response.data;
          if (!body || typeof body !== 'object' || typeof body.success !== 'boolean') { reject(new ApiError('服务响应异常，请稍后重试', status)); return; }
          if (!body.success) { reject(new ApiError(body.message || '操作未完成，请重试', status)); return; }
          resolve(body.data);
        },
        fail() { reject(new ApiError('网络暂不可用，请检查连接后重试', 0, true)); },
      });
    });
  }
  return { get: (path, query, options) => send('GET', path, query, options), post: (path, data) => send('POST', path, data), put: (path, data) => send('PUT', path, data), delete: (path, data) => send('DELETE', path, data) };
}
module.exports = { createClient, ApiError };
