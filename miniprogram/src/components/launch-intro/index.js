const overlay = require('../../lib/overlays');
const filing = require('../../lib/filing');

Component({
  properties: {
    aboutVisible: {
      type: Boolean, value: false,
      observer(value) {
        if (!this._attached) return;
        if (value) this.showAbout();
        else if (this.data.mode === 'about') this.dismiss();
      },
    },
  },
  data: { showing: false, mode: 'intro', filing },
  lifetimes: {
    attached() {
      this._attached = true;
      if (this.properties.aboutVisible) this.showAbout();
      else if (getApp().claimLaunchIntro()) this.present('intro');
    },
    ready() {
      if (this.data.showing && this.data.mode === 'intro') {
        this._timer = setTimeout(() => this.dismiss(), 2200);
      }
    },
    detached() { this._attached = false; this.cleanup(); },
  },
  pageLifetimes: { hide() { this.dismiss(); } },
  methods: {
    noop() {},
    present(mode) {
      clearTimeout(this._timer);
      this._timer = null;
      this._holding = true;
      overlay.hold(this);
      this.setData({ showing: true, mode });
    },
    showAbout() { this.present('about'); },
    cleanup() {
      clearTimeout(this._timer);
      this._timer = null;
      if (this._holding) { this._holding = false; overlay.release(this); }
    },
    dismiss() {
      this.cleanup();
      if (this._attached && this.data.showing) {
        this.setData({ showing: false });
        this.triggerEvent('close');
      }
    },
    copyQuery() {
      wx.setClipboardData({
        data: filing.queryUrl,
        success: () => wx.showToast({ title: '已复制，请在浏览器查询', icon: 'none' }),
        fail: () => wx.showToast({ title: '复制失败，可长按网址复制', icon: 'none' }),
      });
    },
  },
});
