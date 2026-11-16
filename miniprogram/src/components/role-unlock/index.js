const { hasRole } = require('../../lib/display');
Component({
  options: { styleIsolation: 'apply-shared' },
  properties: { visible: Boolean, role: { type: String, value: 'RUNNER' } },
  data: { protocol: null, loading: false, busy: false, accepted: false, error: '', merchant: false },
  observers: { 'visible,role': function (visible, role) { if (visible) { this.setData({ merchant: role === 'SHOP_MERCHANT' }); this.reload(); } else { this._sequence = (this._sequence || 0) + 1; this.setData({ accepted: false, protocol: null }); } } },
  lifetimes: { attached() { this._alive = true; }, detached() { this._alive = false; this._sequence = (this._sequence || 0) + 1; } },
  methods: {
    close() { if (!this.data.busy) this.triggerEvent('close'); },
    accept(event) { this.setData({ accepted: event.detail.value.includes('agree') }); },
    async reload() {
      const sequence = this._sequence = (this._sequence || 0) + 1;
      this.setData({ protocol: null, accepted: false, loading: true, error: '' });
      try {
        const protocol = await getApp().api.protocol(this.properties.role);
        if (sequence === this._sequence) this.setData({ protocol });
      } catch (error) { if (sequence === this._sequence) this.setData({ error: error.message }); }
      finally { if (sequence === this._sequence) this.setData({ loading: false }); }
    },
    async submit() {
      const app = getApp();
      if (this.data.busy || !this.data.accepted || !this.data.protocol) return;
      const token = app.auth.token, role = this.properties.role, version = this.data.protocol.version;
      this.setData({ busy: true, error: '' });
      try {
        const user = await app.auth.restore();
        if (!this._alive || !this.properties.visible || token !== app.auth.token || role !== this.properties.role) return;
        if (!user) { this.setData({ error: app.auth.error || '请先登录后申请' }); return; }
        if (hasRole(user, role)) { this.triggerEvent('opened'); this.triggerEvent('close'); return; }
        const result = await app.api.openRole(user.id, role, version);
        if (token !== app.auth.token) return;
        if (result.reviewStatus === 'APPROVED') {
          app.auth.login(token, { ...user, roles: [...new Set([...user.roles, 'ROLE_' + role])] });
          wx.vibrateShort({ type: 'light', fail() {} });
        }
        wx.showToast({ title: result.reviewStatus === 'APPROVED' ? '身份已开通' : '已提交站务核验', icon: 'success' });
        this.triggerEvent('opened', { role, application: result });
        this.triggerEvent('close');
      } catch (error) { if (this._alive && token === app.auth.token) this.setData({ error: error.message, accepted: false }); }
      finally { if (this._alive) this.setData({ busy: false }); }
    },
  },
});
