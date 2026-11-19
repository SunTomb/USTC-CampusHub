const ui = require('../../lib/ui');
const d = require('../../lib/display');
const rules = require('../../lib/rules');
const { MAX_PHOTOS } = require('../../lib/task-media');
const emptyForm = () => ({ title:'', originDetail:'', destinationDetail:'', weightKg:'', pickupCode:'', notes:'', rewardAmount:'', acceptanceMode:'GRAB', verificationMode:'PHOTO_AND_CONFIRMATION' });

function payload(page) {
  const f = page.data.form;
  const notes = (f.notes || '').trim(), pickupCode = (f.pickupCode || '').trim();
  const weightGrams = rules.weightGrams(f.weightKg);
  if (notes.length > 1000 || pickupCode.length > 120) throw new Error('注意事项最多 1000 字，取件码最多 120 字');
  return { title:rules.required(f.title,'任务标题',120), description:'', weightGrams, notes:notes || null, pickupCode:pickupCode || null,
    originZone:d.zones[page.data.originIndex].value, destinationZone:d.zones[page.data.destinationIndex].value,
    originDetail:rules.required(f.originDetail,'取件位置',200), destinationDetail:rules.required(f.destinationDetail,'送达位置',200),
    rewardAmount:rules.amount(f.rewardAmount), deadline:rules.future(page.data.date,page.data.time), acceptanceMode:f.acceptanceMode, verificationMode:f.verificationMode };
}
ui.definePage({
  data: { step:0,zones:d.zones,originIndex:0,destinationIndex:1,form:emptyForm(),photos:[],publishedId:null,uncertain:false,progress:'',date:'',time:'' },
  onLoad() { this._epoch=0; this.setData(d.dateParts(2 * 3600000)); },
  async onShow() { await ui.requireUser('/pages/tasks/publish'); },
  onHide() { if (!this._picking) { this._epoch++; this.setData({'form.pickupCode':'',photos:[]}); } },
  next() {
    try { payload(this); this.setData({step:1,error:''}); }
    catch(e) { this.setData({error:e.message}); }
  },
  back() { if (!this.data.busy && !this.data.publishedId && !this.data.uncertain) this.setData({step:0,error:''}); },
  mode(e) { if (!this.data.publishedId) this.setData({'form.acceptanceMode':e.currentTarget.dataset.value}); },
  verification(e) { if (!this.data.publishedId) this.setData({'form.verificationMode':e.currentTarget.dataset.value}); },
  async pickPhotos() {
    if (this._picking || this.data.busy) return;
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
    const user=await ui.requireUser('/pages/tasks/publish');
    if (!user || this.data.busy || this.data.uncertain) return;
    const auth=getApp().auth, token=auth.token, epoch=this._epoch;
    const same=()=>this._alive && token===auth.token && auth.user && user.id===auth.user.id;
    const visible=()=>same() && epoch===this._epoch;
    await ui.run(this, async()=>{
      let taskId=this.data.publishedId;
      if (!taskId) {
        const body=payload(this);
        try { const task=await getApp().api.publishTask(body);taskId=task.id; }
        catch(e) { if (same() && (e.network || e.status>=500)) this.setData({uncertain:true}); throw e; }
        if (!same()) return;
        this.setData({publishedId:taskId,'form.pickupCode':''});
      }
      for (const photo of this.data.photos.slice()) {
        if (!visible()) return;
        await getApp().taskMedia.upload(taskId,'REQUEST_IMAGE',photo,p=>{if(visible())this.setData({progress:'正在上传 '+p+'%'});});
        if (!visible()) return;
        this.setData({photos:this.data.photos.filter(p=>p.key!==photo.key)});
      }
      if (visible()) { ui.celebrate('跑腿已发布');this.finish(); }
    });
    if (visible()) this.setData({progress:''});
  },
  clearPrivate() { this._epoch++; this.setData({step:0,form:emptyForm(),photos:[],publishedId:null,uncertain:false,progress:''}); },
});
