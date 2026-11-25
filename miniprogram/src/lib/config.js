const PRODUCTION = 'https://ustc.suntomb.cn/api';
function configFor(platform) {
  let mode = 'release';
  try { mode = platform.getAccountInfoSync().miniProgram.envVersion; } catch (_) { /* Unknown environments must use the production boundary. */ }
  let baseUrl = PRODUCTION;
  if (mode === 'develop') {
    try {
      const value = platform.getStorageSync('woke_development_api');
      if (typeof value === 'string' && /^https?:\/\/(127\.0\.0\.1|localhost)(:\d+)?\/api$/.test(value)) baseUrl = value;
    } catch (_) { /* Local override is optional. */ }
  }
  return { baseUrl, mode, tokenKey: 'woke_session:' + baseUrl };
}
module.exports = { configFor, PRODUCTION };
