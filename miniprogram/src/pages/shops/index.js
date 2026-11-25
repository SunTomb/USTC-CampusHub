const ui = require('../../lib/ui');
const d = require('../../lib/display');
ui.definePage({
  data: { zone: '', category: '', categories: d.categories, shops: [], allShops: [], services: [], keyword: '' },
  onShow() { this.reload(); },
  reload() { return ui.load(this, () => Promise.all([getApp().api.shops(), getApp().api.services()]), ([shops, services]) => { this._shops = shops || []; this._services = services || []; return { shops: this.filtered(), allShops: this._shops }; }); },
  filtered() {
    const keyword = this.data.keyword.trim().toLowerCase();
    return (this._shops || []).filter(shop => (!this.data.zone || shop.campusZone === this.data.zone) && (!this.data.category || (this._services || []).some(service => service.shopId === shop.id && service.category === this.data.category)) && (!keyword || (shop.name + ' ' + shop.description).toLowerCase().includes(keyword))).map(shop => d.shopView(shop, this._services));
  },
  changeZone(event) { this.setData({ zone: event.detail.value }); this.setData({ shops: this.filtered() }); },
  changeCategory(event) { this.setData({ category: event.currentTarget.dataset.value }); this.setData({ shops: this.filtered() }); },
  search() { this.setData({ shops: this.filtered() }); },
  detail(event) { ui.open('/pages/shops/detail?id=' + event.currentTarget.dataset.id); },
  book(event) { ui.open('/pages/shops/detail?id=' + event.currentTarget.dataset.id + '&book=1'); },
});
