const ui=require('../../lib/ui');
const d=require('../../lib/display');
const rules=require('../../lib/rules');
const deliveries=[{value:'OFFLINE_MEETUP',label:'校内当面交接'},{value:'SELF_PICKUP',label:'买家自提'},{value:'NEGOTIATED',label:'双方约定'}];
const blank=()=>({title:'',description:'',price:'',originalPrice:'',conditionLevel:'',tradeLocation:''});
function payload(page){const {form:f,categories,categoryIndex,zoneIndex,methodIndex}=page.data;
  if(!categories[categoryIndex])throw Error('请选择有效分类');const price=rules.amount(f.price);if(price<0.01)throw Error('转让价至少为 0.01 元');
  return {categoryId:categories[categoryIndex].id,title:rules.required(f.title,'物品名称',120),description:rules.required(f.description,'物品说明',4000),price,
    originalPrice:f.originalPrice==null||f.originalPrice===''?null:rules.amount(f.originalPrice),conditionLevel:rules.required(f.conditionLevel,'物品成色',40),
    campusZone:d.zones[zoneIndex].value,tradeLocation:rules.required(f.tradeLocation,'交接点',120),deliveryMethod:deliveries[methodIndex].value,contactVisibility:'INTENT_ONLY'};
}
ui.definePage({
  data:{ready:false,unlock:false,editId:0,savedId:0,uncertain:false,form:blank(),categories:[],categoryIndex:0,zones:d.zones,zoneIndex:0,methods:deliveries,methodIndex:0},
  onLoad(q){this._epoch=0;this.editParam=q.edit;this.setData({editId:Number(q.edit)||0});},
  onShow(){this._visible=true;if(!this.data.ready && !this.data.busy)return this.reload();},
  onHide(){this._visible=false;},
  clearPrivate(){this._epoch++;this.setData({ready:false,unlock:false,form:blank(),savedId:0,uncertain:false,error:'',categoryIndex:0,methodIndex:0,zoneIndex:0});},
  async reload(){if(this.data.ready)return;const v=this._epoch,user=await ui.requireUser('/pages/goods/publish'+(this.editParam!=null?'?edit='+this.editParam:''));if(!user||v!==this._epoch||!this._visible)return;
    if(!d.hasRole(user,'GOODS_PUBLISHER')){this.setData({unlock:true});return;}
    await ui.load(this,async()=>{if(this.editParam!=null && (!/^[1-9]\d*$/.test(this.editParam)||!Number.isSafeInteger(this.data.editId)))throw Error('闲置编号无效');
      const [options,item]=await Promise.all([getApp().api.goodsCategories(),this.data.editId?getApp().api.goodsDetail(this.data.editId):null]);
      const categories=options.filter(c=>c.enabled);if(!categories.length)throw Error('当前没有可用的商品分类');if(item&&(!item.canEdit||item.sellerId!==user.id))throw Error('该闲置不可编辑');return {categories,item};
    },({categories,item})=>{if(!item)return {ready:true,categories};const form=blank();for(const f of Object.keys(form))form[f]=String(item[f]==null?'':item[f]);
      return {ready:true,categories,form,categoryIndex:categories.findIndex(c=>c.id===item.categoryId),zoneIndex:Math.max(0,d.zones.findIndex(z=>z.value===item.campusZone)),methodIndex:Math.max(0,deliveries.findIndex(m=>m.value===item.deliveryMethod))};});},
  closeUnlock(){this.setData({unlock:false});}, opened(){this.closeUnlock();return this.reload();},
  async save(){const app=getApp(),auth=app.auth,epoch=this._epoch;if(!auth.authenticated||!this.data.ready||this.data.busy||this.data.uncertain)return;
    const current=()=>this._alive && this._epoch===epoch;
    await ui.run(this,async()=>{
      let id=this.data.savedId;if(!id){const body=payload(this);let result;
        try{result=this.data.editId?await app.api.updateGoods(this.data.editId,body):await app.api.publishGoods(body);}catch(e){if(current()&&!this.data.editId&&(e.network||e.status>=500||e.status===408))this.setData({uncertain:true});throw e;}
        if(!current())return;id=result.id;this.setData({savedId:id});}
      if(!this._visible)return;const gallery=this.selectComponent('#goods-photos');if(!gallery)throw Error('闲置已保存，图片组件未就绪，请重试');
      try{await gallery.saveTo(id);}catch(e){throw Error('闲置已保存，图片未传完，请重试');}
      if(current()&&this._visible){ui.celebrate('闲置已保存');wx.redirectTo({url:'/pages/goods/detail?id='+id});}
    });},
});
