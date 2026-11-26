const ui=require('../../lib/ui'),d=require('../../lib/display');
ui.definePage({data:{item:null,owner:false},onLoad(q){this.id=Number(q.id);},onShow(){this._visible=true;
    if (this._returningFromMedia) { this._returningFromMedia=false; return; }
    return this.reload();},onHide(){this._visible=false;
    if (ui.catalogMediaOpen(this)) { this._returningFromMedia=true; return; }
    this._loadSequence++;this.clearPrivate();},clearPrivate(){this._returningFromMedia=false;this.setData({item:null,owner:false});},async reload(){await getApp().auth.restore();if(!this._visible||!this._alive)return;await ui.load(this,async()=>{const api=getApp().api,item=await api.serviceItem(this.id),shop=await api.shop(item.shopId);return{item:{...d.serviceView(item),initial:(item.title || '服').slice(0, 1)},owner:getApp().auth.user?.id===shop.ownerId};},v=>v);},photosChanged(){this.selectComponent('#service-thumb')?.load();},async book(){const i=this.data.item;if(!i||this.data.owner)return;const url='/pages/shops/detail?id='+i.shopId+'&book=1&item='+i.id;if(await ui.requireUser(url))ui.open(url);}});
