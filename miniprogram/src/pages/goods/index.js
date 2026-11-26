const ui = require('../../lib/ui');
const d = require('../../lib/display');
ui.definePage({
  data: { zone:'', query:'', all:[], items:[], unlock:false },
  onShow() { this.reload(); },
  reload() { return ui.load(this, () => getApp().api.goods(), rows => {
    const all=rows.map(row=>({...row,initial:(row.title || '物').slice(0, 1),zoneLabel:d.zone(row.campusZone),priceLabel:d.money(row.price)})); return {all,items:this.filtered(all)};
  }); },
  filtered(all) { const q=this.data.query.trim().toLowerCase();return all.filter(x=>(!this.data.zone||x.campusZone===this.data.zone) && (!q||[x.title,x.categoryName,x.description].some(v=>String(v||'').toLowerCase().includes(q)))); },
  search(e) { this.setData({query:e.detail.value}); this.setData({items:this.filtered(this.data.all)}); },
  changeZone(e) { this.setData({zone:e.detail.value});this.setData({items:this.filtered(this.data.all)}); },
  async publish() { const u=await ui.requireUser('/pages/goods/publish');if(!u)return;if(!d.hasRole(u,'GOODS_PUBLISHER'))this.setData({unlock:true});else ui.open('/pages/goods/publish'); },
  opened() {this.setData({unlock:false});if(d.hasRole(getApp().auth.user,'GOODS_PUBLISHER'))ui.open('/pages/goods/publish');},
  closeUnlock() {this.setData({unlock:false});}, clearPrivate() {this.closeUnlock();},
});
