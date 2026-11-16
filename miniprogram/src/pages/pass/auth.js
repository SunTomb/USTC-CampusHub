const ui = require('../../lib/ui');
const rules = require('../../lib/rules');
ui.definePage({
  data: { tab: 'login', sessionError: '', recovery: false, loginForm: { username: '', password: '' }, registerForm: { email: '', username: '', emailCode: '', password: '', wechatContact: '', qqContact: '' }, sending: false, seconds: 0, protocolOpen: false, protocol: null },
  onLoad(query) {
    let next = query.next || '/pages/pass/index';
    try { next = decodeURIComponent(next); } catch (_) { next = '/pages/pass/index'; }
    this.next = ui.validRoute(next) ? next : '/pages/pass/index';
  },
  onShow() { const auth = getApp().auth; this.setData({ recovery: !!(auth.token && auth.error), sessionError: auth.error }); if (auth.authenticated) this.finish(); },
  onUnload() { if (this._countdown) clearInterval(this._countdown); },
  changeTab(event) { if (!this.data.busy) this.setData({ tab: event.currentTarget.dataset.tab, error: '' }); },
  finish() { const route = this.next || '/pages/pass/index'; if (ui.TABS.includes(route.split('?')[0])) wx.switchTab({ url: route.split('?')[0] }); else wx.redirectTo({ url: route }); },
  async submitLogin() {
    await ui.run(this, () => getApp().api.login({ username: rules.required(this.data.loginForm.username, '用户名或校园邮箱', 128), password: rules.required(this.data.loginForm.password, '密码', 72) }), result => { getApp().auth.login(result.accessToken, result.user); this.setData({ 'loginForm.password': '' }); this.finish(); });
  },
  async retrySession() {
    if (this.data.busy) return;
    this.setData({ busy: true });
    await getApp().auth.restore(true);
    if (!this._alive) return;
    const auth = getApp().auth;
    this.setData({ busy: false, sessionError: auth.error, recovery: !!(auth.token && auth.error) });
    if (auth.authenticated) this.finish();
  },
  otherAccount() { getApp().auth.logout(); this.setData({ recovery: false, sessionError: '', error: '' }); },
  async sendCode() {
    if (this.data.sending || this.data.seconds) return;
    this.setData({ sending: true, error: '' });
    try {
      const email = rules.email(this.data.registerForm.email);
      const result = await getApp().api.registerCode(email);
      if (!this._alive) return;
      this.setData({ seconds: result.resendSeconds || 60 });
      ui.toast('验证码已发送，请查看校园邮箱');
      this._countdown = setInterval(() => {
        if (!this._alive) return;
        const seconds = Math.max(0, this.data.seconds - 1); this.setData({ seconds });
        if (!seconds) { clearInterval(this._countdown); this._countdown = null; }
      }, 1000);
    } catch (error) { if (this._alive) this.setData({ error: error.message }); }
    finally { if (this._alive) this.setData({ sending: false }); }
  },
  async submitRegistration() {
    await ui.run(this, () => {
      const form = this.data.registerForm;
      if (!form.wechatContact.trim() && !form.qqContact.trim()) throw new Error('微信号与 QQ 至少填写一项');
      if (!/^\d{6}$/.test(form.emailCode)) throw new Error('请填写 6 位邮箱验证码');
      return getApp().api.register({ email: rules.email(form.email), username: rules.username(form.username), password: rules.password(form.password), emailCode: form.emailCode, wechatContact: form.wechatContact.trim(), qqContact: form.qqContact.trim() });
    }, result => { ui.celebrate('注册成功，请登录'); this.setData({ tab: 'login', 'loginForm.username': result.username || this.data.registerForm.username, 'loginForm.password': '', 'registerForm.password': '', 'registerForm.emailCode': '' }); });
  },
  async openProtocol() { this.setData({ protocolOpen: true, protocol: null }); await ui.load(this, () => getApp().api.protocol('USER_SERVICE'), protocol => ({ protocol })); },
  closeProtocol() { this.setData({ protocolOpen: false }); },
  clearPrivate() { this.setData({ 'loginForm.password': '', 'registerForm.password': '', 'registerForm.emailCode': '' }); },
});
