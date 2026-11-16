const { configFor } = require('./lib/config');
const { createClient } = require('./lib/request');
const { createSession } = require('./lib/session');
const { createApi } = require('./lib/api');

App({
  claimLaunchIntro() {
    if (this._launchIntroClaimed) return false;
    this._launchIntroClaimed = true;
    return true;
  },
  onLaunch() {
    this._launchIntroClaimed = false;
    this.config = configFor(wx);
    this.auth = createSession({
      read: () => wx.getStorageSync(this.config.tokenKey),
      write: token => token ? wx.setStorageSync(this.config.tokenKey, token) : wx.removeStorageSync(this.config.tokenKey),
      me: () => this.api.me(),
    });
    this.client = createClient({ baseUrl: this.config.baseUrl, request: options => wx.request(options), getToken: () => this.auth.token, onUnauthorized: token => this.auth.invalidate(token) });
    this.api = createApi(this.client);
    this.auth.restore();
  },
});
