const ui = require('../../lib/ui');
const d = require('../../lib/display');
const rules = require('../../lib/rules');
const { MAX_PHOTOS } = require('../../lib/task-media');
const emptyForm = () => ({ title:'', description:'', originDetail:'', destinationDetail:'', weightKg:'', pickupCode:'', notes:'', rewardAmount:'', acceptanceMode:'GRAB', verificationMode:'PHOTO_AND_CONFIRMATION' });

function payload(page) {
  const f = page.data.form;
  const notes = (f.notes || '').trim(), pickupCode = (f.pickupCode || '').trim();
  const weightGrams = rules.weightGrams(f.weightKg);
  if (notes.length > 1000 || pickupCode.length > 120) throw new Error('注意事项最多 1000 字，取件码最多 120 字');
  return { title:rules.required(f.title,'任务标题',120), description:(f.description || '').trim(), weightGrams, notes:notes || null, pickupCode:pickupCode || null,
    originZone:d.zones[page.data.originIndex].value, destinationZone:d.zones[page.data.destinationIndex].value,
    originDetail:rules.required(f.originDetail,'取件位置',200), destinationDetail:rules.required(f.destinationDetail,'送达位置',200),
    rewardAmount:rules.amount(f.rewardAmount), deadline:rules.future(page.data.date,page.data.time), acceptanceMode:f.acceptanceMode, verificationMode:f.verificationMode };
}
ui.definePage({
  data: { editId:0,editing:false,editReady:false,step:0,zones:d.zones,originIndex:0,destinationIndex:1,form:emptyForm(),photos:[],publishedId:null,uncertain:false,progress:'',date:'',time:'' },
  onLoad(q) { this._epoch=0;this._visible=true;this._editParam=q.edit;this.setData({...d.dateParts(2 * 3600000),editing:q.edit!=null,editId:Number(q.edit)||0}); },
  async onShow() { this._visible=true;await this.reload(); },
  async reload() {
    const v=this._epoch,user=await ui.requireUser('/pages/tasks/publish'+(this.data.editing?'?edit='+this.data.editId:''));
    if(!user || !this._alive || !this._visible || v!==this._epoch || !this.data.editing || this.data.publishedId || this.data.editReady)return;
    await ui.load(this,async()=>{
      if(!/^[1-9]\d*$/.test(this._editParam)||!Number.isSafeInteger(this.data.editId))throw Error('无效的委托编号');
      const w=await getApp().api.taskWorkspace(this.data.editId);
      if(!w.canEdit || w.task.publisherId!==user.id)throw Error('该委托已有报名、已被接单或已结束，不能修改');
      return w;
    },w=>{const t=w.task;return {editReady:true,step:0,originIndex:Math.max(0,d.zones.findIndex(z=>z.value===t.originZone)),destinationIndex:Math.max(0,d.zones.findIndex(z=>z.value===t.destinationZone)),
      date:t.deadline.slice(0,10),time:t.deadline.slice(11,16),form:{title:t.title,description:t.description||'',originDetail:t.originDetail||'',destinationDetail:t.destinationDetail||'',
      weightKg:t.weightGrams==null?'':String(t.weightGrams/1000),notes:t.notes||'',pickupCode:(w.privateDetails&&w.privateDetails.pickupCode)||'',rewardAmount:String(t.rewardAmount),
      acceptanceMode:t.acceptanceMode,verificationMode:t.verificationMode}};});
  },
  onHide() { if (!this._picking) { this._visible=false;this._epoch++;this._loadSequence++;this.setData({'form.pickupCode':'',photos:[],editReady:false}); } },
  next() {
    if(this.data.editing&&!this.data.editReady)return;
    try { payload(this); this.setData({step:1,error:''}); }
    catch(e) { this.setData({error:e.message}); }
  },
  back() { if (!this.data.busy && !this.data.publishedId && !this.data.uncertain) this.setData({step:0,error:''}); },
  mode(e) { if (!this.data.publishedId) this.setData({'form.acceptanceMode':e.currentTarget.dataset.value}); },
  verification(e) { if (!this.data.publishedId) this.setData({'form.verificationMode':e.currentTarget.dataset.value}); },
  async pickPhotos() {
    if (this.data.editing || this._picking || this.data.busy) return;
    const epoch=this._epoch, token=getApp().auth.token, count=MAX_PHOTOS-this.data.photos.length;
    if (count < 1) return;
    this._picking=true;
    try { const added=await getApp().taskMedia.pick(count);
      if (epoch===this._epoch && this._alive && token===getApp().auth.token) this.setData({photos:[...this.data.photos,...added],error:''});
    } catch(e) { if (epoch===this._epoch && this._alive) this.setData({error:e.message}); }
    finally { this._picking=false; }
  },
  removePhoto(e) { if (!this.data.busy) this.setData({photos:this.data.photos.filter(p=>p.key!==e.currentTarget.dataset.key)}); },
  finish() { if (this.data.publishedId) wx.redirectTo({url:'/pages/tasks/detail?id='+this.data.publishedId}); },
  async submit() {
    const start=this._epoch,user=await ui.requireUser('/pages/tasks/publish'+(this.data.editing?'?edit='+this.data.editId:''));
    if (!user || start!==this._epoch || !this._visible || this.data.busy || this.data.uncertain || (this.data.editing && !this.data.editReady && !this.data.publishedId)) return;
    const auth=getApp().auth, token=auth.token, epoch=this._epoch;
    const same=()=>this._alive && token===auth.token && auth.user && user.id===auth.user.id;
    const visible=()=>same() && epoch===this._epoch;
    await ui.run(this, async()=>{
      let taskId=this.data.publishedId;
      if (!taskId) {
        const body=payload(this);
        try { const task=this.data.editing?await getApp().api.updateTask(this.data.editId,body):await getApp().api.publishTask(body);taskId=task.id; }
        catch(e) { if (same() && !this.data.editing && (e.network || e.status>=500 || e.status===408)) this.setData({uncertain:true}); throw e; }
        if (!same()) return;
        this.setData({publishedId:taskId,'form.pickupCode':''});
      }
      for (const photo of this.data.photos.slice()) {
        if (!visible()) return;
        await getApp().taskMedia.upload(taskId,'REQUEST_IMAGE',photo,p=>{if(visible())this.setData({progress:'正在上传 '+p+'%'});});
        if (!visible()) return;
        this.setData({photos:this.data.photos.filter(p=>p.key!==photo.key)});
      }
      if (visible()) { ui.celebrate(this.data.editing?'委托已修改':'跑腿已发布');this.finish(); }
    });
    if (visible()) this.setData({progress:''});
  },
  clearPrivate() { this._epoch++; this.setData({editReady:false,step:0,form:emptyForm(),photos:[],publishedId:null,uncertain:false,progress:''}); },
});
