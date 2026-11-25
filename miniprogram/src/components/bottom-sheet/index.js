const overlay = require('../../lib/overlays');
Component({
  options: { multipleSlots: true, styleIsolation: 'apply-shared' },
  properties: { visible: { type: Boolean, value: false, observer(value) { if (value) overlay.hold(this); else overlay.release(this); } }, title: String, busy: Boolean },
  lifetimes: { detached() { overlay.release(this); } },
  pageLifetimes: { hide() { overlay.release(this); if (this.properties.visible) this.triggerEvent('close'); }, show() { if (this.properties.visible) overlay.hold(this); } },
  methods: { noop() {}, close() { if (!this.properties.busy) this.triggerEvent('close'); } },
});
