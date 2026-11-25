const rules = require('../../lib/rules');
const ui = require('../../lib/ui');
const d = require('../../lib/display');
ui.definePage({
  data: { orderCounts:{}, shortcuts:[{key:'PENDING',label:'待处理'},{key:'ACTIVE',label:'进行中'},{key:'REVIEW',label:'待评价'},{key:'ENDED',label:'已结束'}], aboutOpen: false, section: 'pass', profile: null, creditLabel: '—', roleCards: [], role: 'RUNNER', unlock: false, notices: [], unread: 0, applications: [], profileOpen: false, passwordOpen: false, profileForm: { username: '', nickname: '', wechatContact: '', qqContact: '' }, passwordForm: { currentPassword: '', newPassword: '' }, protocolOpen: false, protocol: null,  },
  onShow() { this.reload(); },
  openAbout() { this.setData({ aboutOpen: true }); },
  closeAbout() { this.setData({ aboutOpen: false }); },
  async reload() {
    const app = getApp(); await app.auth.restore();
    if (!app.auth.authenticated) { this.clearPrivate(); this.setData({ loading: false, error: app.auth.error }); return; }
    const user = app.auth.user;
    await ui.load(this, async () => {
      const [profile, applications, notices, desk] = await Promise.all([app.api.profile(user.id), app.api.applications(user.id), app.api.notifications(user.id),app.api.orderDesk({size:1}).catch(()=>null)]);
      return { profile, applications, notices, desk };
    }, ({ profile, applications, notices, desk }) => {
      const unread = notices.filter(n => n.readAt == null).length;
      if (unread) wx.setTabBarBadge({ index: 4, text: unread > 99 ? '99+' : String(unread), fail() {} }); else wx.removeTabBarBadge({ index: 4, fail() {} });
      return { orderCounts:desk?.counts||{}, profile, applications, notices: notices.map(n => ({ ...n, dateLabel: d.dateText(n.createdAt), unread: n.readAt == null })), unread, creditLabel: profile.creditScore == null ? '—' : String(profile.creditScore), roleCards: d.roles.map(role => { const application = applications.find(a => a.roleType === role.value); const active = d.hasRole(user, role.value); return { ...role, active, pending: !active && application && application.reviewStatus === 'PENDING_REVIEW', statusLabel: active ? '已开通' : application && application.reviewStatus === 'PENDING_REVIEW' ? '核验中' : role.review ? '协议后核验' : '同意协议开通' }; }) };
    });
  },
  async switchSection(event) {
    const section=event.currentTarget.dataset.section;
    if (!['pass','notices'].includes(section) || !await ui.requireUser('/pages/pass/index')) return;
    this.setData({section,error:''}); await this.reload();
  },
  async openRole(event) {
    if (!await ui.requireUser('/pages/pass/index')) return;
    const role = this.data.roleCards.find(r => r.value === event.currentTarget.dataset.role);
    if (!role || role.active || role.pending) return;
    this.setData({ role: role.value, unlock: true });
  },
  closeRole() { this.setData({ unlock: false }); },
  roleOpened() { this.reload(); },
  openProfile() {
    if (!getApp().auth.authenticated || !this.data.profile) return;
    this.setData({ profileOpen: true, error: '', profileForm: { username: this.data.profile.username, nickname: this.data.profile.nickname, wechatContact: this.data.profile.wechatContact || '', qqContact: this.data.profile.qqContact || '' } });
  },
  closeProfile() { if (!this.data.busy) this.setData({ profileOpen: false }); },
  async saveProfile() {
    await ui.run(this, () => getApp().api.saveProfile({ username: rules.username(this.data.profileForm.username), nickname: rules.required(this.data.profileForm.nickname, '昵称', 64) }), async result => { const auth = getApp().auth; auth.login(auth.token, { ...auth.user, ...result, roles: auth.user.roles }); this.setData({ profileOpen: false }); ui.celebrate('个人资料已保存'); await this.reload(); });
  },
  async saveContacts() {
    const wechatContact = String(this.data.profileForm.wechatContact || '').trim();
    const qqContact = String(this.data.profileForm.qqContact || '').trim();
    if (!wechatContact && !qqContact) { this.setData({ error: '微信号与 QQ 至少填写一项' }); return; }
    await ui.run(this, () => getApp().api.saveContacts({ wechatContact, qqContact }), async result => {
      const auth = getApp().auth;
      auth.login(auth.token, { ...auth.user, ...result, roles: auth.user.roles });
      this.setData({ profile: result, error: '' });
      ui.celebrate('联系方式已更新');
    });
  },
  openPassword() { this.setData({ profileOpen: false, passwordOpen: true, error: '', passwordForm: { currentPassword: '', newPassword: '' } }); },
  closePassword() { if (!this.data.busy) this.setData({ passwordOpen: false, passwordForm: { currentPassword: '', newPassword: '' } }); },
  async savePassword() {
    if (!await ui.requireUser('/pages/pass/index')) return;
    await ui.run(this, () => getApp().api.password({ currentPassword: rules.required(this.data.passwordForm.currentPassword, '当前密码', 72), newPassword: rules.password(this.data.passwordForm.newPassword) }), () => { this.closePassword(); getApp().auth.logout(); ui.toast('密码已更新，请重新登录'); });
  },
  async logout() { if (await ui.confirm('退出当前账号', '未提交的表单不会自动保存。')) getApp().auth.logout(); },
  async openProtocol() {
    this.setData({ protocolOpen: true, protocol: null, error: '' });
    await ui.load(this, () => getApp().api.protocol('USER_SERVICE'), protocol => ({ protocol }));
  },
  closeProtocol() { this.setData({ protocolOpen: false, protocol: null }); },
  async readNotice(event) {
    const notice = this.data.notices.find(n => n.id === Number(event.currentTarget.dataset.id));
    const user = getApp().auth.user;
    if (!notice || !user || notice.recipientId !== user.id) return;
    if (notice.unread) await ui.run(this, () => getApp().api.readNotification(notice.id), () => { this.setData({ notices: this.data.notices.map(n => n.id === notice.id ? { ...n, unread: false } : n), unread: Math.max(0, this.data.unread - 1) }); if (!this.data.unread) wx.removeTabBarBadge({ index: 4, fail() {} }); });
  },
  clearPrivate() {
    this.setData({ orderCounts:{}, profile: null, roleCards: [], notices: [], unread: 0, applications: [], profileOpen: false, passwordOpen: false, unlock: false, profileForm: { username: '', nickname: '', wechatContact: '', qqContact: '' }, passwordForm: { currentPassword: '', newPassword: '' } });
    wx.removeTabBarBadge({ index: 4, fail() {} });
  },
});
