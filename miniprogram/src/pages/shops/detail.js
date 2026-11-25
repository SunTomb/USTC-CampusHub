const ui = require('../../lib/ui');
const d = require('../../lib/display');
const rules = require('../../lib/rules');
ui.definePage({
  data: { shop: null, services: [], contacts: [], booking: false, serviceIndex: 0, date: '', time: '', note: '', bookingDone: false },
  onLoad(query) { this.shopId = Number(query.id); this._bookItem=Number(query.item)||null;this._bookOnLoad = query.book === '1'; this.setData(d.dateParts(86400000)); },
  async onShow() { await this.reload(); if (this._bookOnLoad && this.data.services.length) { this._bookOnLoad = false; await this.openBooking({ currentTarget: { dataset: { id: this._bookItem || this.data.services[0].id } } }); } },
  async reload() { await getApp().auth.restore(); return ui.load(this, () => getApp().api.shop(this.shopId), shop => ({ shop: { ...shop, zoneLabel: d.zone(shop.campusZone) }, services: (shop.serviceItems || []).map(d.serviceView), contacts: d.contacts(shop.contactSnapshot, shop.contactVisible === true) })); },
  async openBooking(event) {
    const user = await ui.requireUser('/pages/shops/detail?id=' + this.shopId + '&book=1&item='+Number(event.currentTarget.dataset.id));
    if (!user || !this.data.shop) return;
    if (this.data.shop.ownerId === user.id) { ui.toast('这是你自己的小店，可在个人 → 我的订单处理预约'); return; }
    const index = this.data.services.findIndex(item => item.id === Number(event.currentTarget.dataset.id));
    if (index < 0) return;
    this.setData({ booking: true, serviceIndex: index, note: '', error: '', ...d.dateParts(86400000) });
  },
  closeBooking() { if (!this.data.busy) this.setData({ booking: false }); },
  async submitBooking() {
    if (!await ui.requireUser('/pages/shops/detail?id=' + this.shopId)) return;
    const service = this.data.services[Number(this.data.serviceIndex)];
    if (!service) return;
    await ui.run(this, () => getApp().api.book(service.id, { appointmentTime: rules.future(this.data.date, this.data.time), note: this.data.note.trim() }), async () => { this.setData({ booking: false, bookingDone: true }); ui.celebrate('预约已发送'); await this.reload(); });
  },
  copyContact(event) { const contact = this.data.contacts[Number(event.currentTarget.dataset.index)]; if (contact && getApp().auth.authenticated) ui.copy(contact.value); },
  clearPrivate() { this.setData({ contacts: [], booking: false, note: '', bookingDone: false }); if (this.data.shop) this.setData({ 'shop.contactVisible': false, 'shop.contactSnapshot': null }); },
});
