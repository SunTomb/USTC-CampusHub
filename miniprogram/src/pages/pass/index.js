const ui = require('../../lib/ui');
const d = require('../../lib/display');
const rules = require('../../lib/rules');
ui.definePage({
  data: { aboutOpen: false, section: 'pass', profile: null, creditLabel: '—', roleCards: [], role: 'RUNNER', unlock: false, notices: [], unread: 0, applications: [], tasks: [], publishedTasks: [], orders: [], providerMode: false, posts: [], profileOpen: false, passwordOpen: false, profileForm: { username: '', nickname: '', wechatContact: '', qqContact: '' }, passwordForm: { currentPassword: '', newPassword: '' }, protocolOpen: false, protocol: null, orderOpen: false, selectedOrder: null, actionReason: '' },
  onShow() { this.reload(); },
  openAbout() { this.setData({ aboutOpen: true }); },
  closeAbout() { this.setData({ aboutOpen: false }); },
  async reload() {
    const app = getApp(); await app.auth.restore();
    if (!app.auth.authenticated) { this.clearPrivate(); this.setData({ loading: false, error: app.auth.error }); return; }
    const user = app.auth.user;
    await ui.load(this, async () => {
      const [profile, applications, notices] = await Promise.all([app.api.profile(user.id), app.api.applications(user.id), app.api.notifications(user.id)]);
      return { profile, applications, notices };
    }, ({ profile, applications, notices }) => {
      const unread = notices.filter(n => n.readAt == null).length;
      if (unread) wx.setTabBarBadge({ index: 3, text: unread > 99 ? '99+' : String(unread), fail() {} }); else wx.removeTabBarBadge({ index: 3, fail() {} });
      return { profile, applications, notices: notices.map(n => ({ ...n, dateLabel: d.dateText(n.createdAt), unread: n.readAt == null })), unread, creditLabel: profile.creditScore == null ? '—' : String(profile.creditScore), roleCards: d.roles.map(role => { const application = applications.find(a => a.roleType === role.value); const active = d.hasRole(user, role.value); return { ...role, active, pending: !active && application && application.reviewStatus === 'PENDING_REVIEW', statusLabel: active ? '已开通' : application && application.reviewStatus === 'PENDING_REVIEW' ? '核验中' : role.review ? '协议后核验' : '同意协议开通' }; }) };
    });
    if (this.data.section !== 'pass' && this.data.section !== 'notices') await this.loadSection();
  },
  async switchSection(event) { const section = event.currentTarget.dataset.section; this.setData({ section, error: '' }); if (!await ui.requireUser('/pages/pass/index')) return; if (section === 'pass' || section === 'notices') this.reload(); else this.loadSection(); },
  async loadSection() {
    const app = getApp(), user = await ui.requireUser('/pages/pass/index');
    if (!user) return;
    const section = this.data.section;
    await ui.load(this, async () => {
      if (section === 'tasks') {
        const [own, posted] = await Promise.all([app.api.myTaskApplications(user.id), app.api.myPublishedTasks()]);
        return { tasks: own.map(a => ({ ...a, statusLabel: d.status(a.status), dateLabel: d.dateText(a.createdAt) })), publishedTasks: posted.map(d.taskView) };
      }
      if (section === 'orders') {
        const provider = this.data.providerMode && d.hasRole(user, 'SHOP_MERCHANT');
        const orders = await (provider ? app.api.providerOrders(user.id) : app.api.customerOrders(user.id));
        return { orders: orders.map(order => ({ ...order, amountLabel: d.money(order.amount), statusLabel: d.status(order.status), dateLabel: d.dateText(order.appointmentTime), actions: rules.orderActions(order, user.id), contacts: d.contacts(order.contactSnapshot, order.customerId === user.id) })) };
      }
      if (section === 'posts') return { posts: (await app.api.myProjects(user.id)).map(d.projectView) };
      return {};
    }, result => result);
  },
  async toggleOrders(event) {
    const providerMode = event.currentTarget.dataset.provider === 'yes';
    if (providerMode && !d.hasRole(getApp().auth.user, 'SHOP_MERCHANT')) {
      this.setData({ providerMode: false });
      await this.openRole({ currentTarget: { dataset: { role: 'SHOP_MERCHANT' } } });
      return;
    }
    this.setData({ providerMode }); await this.loadSection();
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
    if (notice.unread) await ui.run(this, () => getApp().api.readNotification(notice.id), () => { this.setData({ notices: this.data.notices.map(n => n.id === notice.id ? { ...n, unread: false } : n), unread: Math.max(0, this.data.unread - 1) }); if (!this.data.unread) wx.removeTabBarBadge({ index: 3, fail() {} }); });
  },
  showOrder(event) { const order = this.data.orders.find(o => o.id === Number(event.currentTarget.dataset.id)); if (order) this.setData({ selectedOrder: order, orderOpen: true, actionReason: '', error: '' }); },
  closeOrder() { if (!this.data.busy) this.setData({ orderOpen: false, selectedOrder: null }); },
  copyContact(event) { const order = this.data.selectedOrder, user = getApp().auth.user; if (!order || !user || order.customerId !== user.id) return; const contact = order.contacts[Number(event.currentTarget.dataset.index)]; if (contact) ui.copy(contact.value); },
  async orderAction(event) {
    const order = this.data.selectedOrder, user = await ui.requireUser('/pages/pass/index');
    if (!order || !user) return;
    const action = rules.orderActions(order, user.id).find(a => a.value === event.currentTarget.dataset.action);
    if (!action) return;
    if (!await ui.confirm(action.label, '确认更新本次预约状态？具体费用由双方直接结算。')) return;
    await ui.run(this, () => getApp().api.orderAction(order.id, action.value, ['cancel', 'reject'].includes(action.value) ? rules.required(this.data.actionReason, '取消或婉拒原因', 500) : undefined), async () => { this.setData({ orderOpen: false, selectedOrder: null }); ui.celebrate('预约已更新'); await this.loadSection(); });
  },
  clearPrivate() {
    this.setData({ profile: null, roleCards: [], notices: [], unread: 0, applications: [], tasks: [], publishedTasks: [], orders: [], posts: [], profileOpen: false, passwordOpen: false, orderOpen: false, selectedOrder: null, unlock: false, profileForm: { username: '', nickname: '', wechatContact: '', qqContact: '' }, passwordForm: { currentPassword: '', newPassword: '' } });
    wx.removeTabBarBadge({ index: 3, fail() {} });
  },
});
