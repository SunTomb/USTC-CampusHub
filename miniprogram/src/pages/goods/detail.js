const ui=require('../../lib/ui');
const d=require('../../lib/display');
const emptyDetail=()=>({item:null,intents:[],owner:false,contacts:[],want:false,message:'',deleted:false});
const STATUS={PUBLISHED:'在售',ON_SALE:'历史在售',SOLD:'已售出',OFF_SHELF:'已下架',OPEN:'已联系',COMPLETED:'已成交',CLOSED:'意向已结束'};
ui.definePage({
  data:emptyDetail(),
  onLoad(q){this.id=Number(q.id);this._epoch=0;},
  onShow(){this._visible=true;return this.reload();},
  onHide(){this._visible=false;this.clearPrivate();},
  clearPrivate(){this._epoch++;this._loadSequence++;this.setData({...emptyDetail(),error:''});},
  async reload(){
    const v=this._epoch;await getApp().auth.restore();if(v!==this._epoch||!this._visible)return;
    this.setData(emptyDetail());
    await ui.load(this,async()=>{
      const app=getApp(),user=app.auth.user;let item;
      try{item=await app.api.goodsDetail(this.id);}catch(e){if(e.status===410)return {deleted:true};throw e;}
      const owner=!!user&&user.id===item.sellerId;
      const intents=owner&&v===this._epoch?await app.api.goodsIntents(this.id):[];
      return {item,owner,intents};
    },result=>{
      if(result.deleted)return {...emptyDetail(),deleted:true};
      const {item,owner,intents}=result;
      return {item:{...item,zoneLabel:d.zone(item.campusZone),priceLabel:d.money(item.price),statusLabel:STATUS[item.status]||'暂不可交易',intentLabel:STATUS[item.myIntentStatus]||''},owner,
        contacts:d.contacts(item.contactSnapshot,item.contactVisible),intents:intents.map(i=>({...i,statusLabel:STATUS[i.status]||'已结束'}))};
    });
  },
  async openWant(){const u=await ui.requireUser('/pages/goods/detail?id='+this.id);const i=this.data.item;if(u&&i&&i.sellerId!==u.id&&i.status==='PUBLISHED'&&!i.myIntentStatus)this.setData({want:true,message:''});},
  closeWant(){if(!this.data.busy)this.setData({want:false});},
  async send(){if(!this.data.want||!this.data.item||this.data.owner||!getApp().auth.authenticated)return;const epoch=this._epoch;
    await ui.run(this,()=>getApp().api.createGoodsIntent(this.id,this.data.message.trim()),async()=>{if(epoch!==this._epoch)return;this.setData({want:false,message:''});ui.celebrate('意向已发送');await this.reload();});},
  copy(e){ui.copy(e.currentTarget.dataset.value);},
  async choose(e){const epoch=this._epoch,buyer=Number(e.currentTarget.dataset.buyer),intent=this.data.intents.find(i=>i.buyerId===buyer&&i.status==='OPEN');
    if(this.data.busy||!this.data.owner||!this.data.item.canMarkSold||!intent)return;
    if(!await ui.confirm('确认实际买家','已与「'+intent.buyerNickname+'」完成当面交接？确认后不能换人，其他意向将关闭。')||epoch!==this._epoch)return;
    await ui.run(this,()=>getApp().api.markGoodsSold(this.id,buyer),async()=>{if(epoch===this._epoch){ui.celebrate('已记录成交');await this.reload();}});},
  async deleteListing(){const epoch=this._epoch;if(this.data.busy||!this.data.owner||!this.data.item?.canDelete)return;
    if(!await ui.confirm('删除商品？','删除后，订单仍可查看，但不能再打开商品内容或照片。')||epoch!==this._epoch)return;
    await ui.run(this,()=>getApp().api.deleteGoods(this.id),async()=>{if(epoch===this._epoch)await this.reload();});},
  async offShelf(){const epoch=this._epoch;if(this.data.busy||!this.data.owner||!this.data.item.canOffShelf)return;
    if(!await ui.confirm('下架闲置','下架后不再接受意向，已有购买意向将关闭。')||epoch!==this._epoch)return;
    await ui.run(this,()=>getApp().api.offShelfGoods(this.id),async()=>{if(epoch===this._epoch){ui.celebrate('闲置已下架');await this.reload();}});},
});
