const ui = require('../../lib/ui');
const d = require('../../lib/display');
const rules = require('../../lib/rules');
ui.definePage({
  data: { step: 0, zones: d.zones, originIndex: 0, destinationIndex: 1, form: { title: '', description: '', originDetail: '', destinationDetail: '', rewardAmount: '', acceptanceMode: 'GRAB', verificationMode: 'PHOTO_AND_CONFIRMATION' }, date: '', time: '' },
  onLoad() { this.setData(d.dateParts(2 * 3600000)); },
  async onShow() { await ui.requireUser('/pages/tasks/publish'); },
  next() {
    try {
      if (this.data.step === 0) { rules.required(this.data.form.originDetail, '取件位置', 200); rules.required(this.data.form.destinationDetail, '送达位置', 200); }
      if (this.data.step === 1) { rules.required(this.data.form.title, '任务标题', 120); rules.required(this.data.form.description, '委托说明', 2000); rules.amount(this.data.form.rewardAmount); rules.future(this.data.date, this.data.time); }
      this.setData({ step: Math.min(2, this.data.step + 1), error: '' });
    } catch (error) { this.setData({ error: error.message }); }
  },
  back() { if (!this.data.busy) this.setData({ step: Math.max(0, this.data.step - 1), error: '' }); },
  mode(event) { this.setData({ 'form.acceptanceMode': event.currentTarget.dataset.value }); },
  verification(event) { this.setData({ 'form.verificationMode': event.currentTarget.dataset.value }); },
  async submit() {
    if (!await ui.requireUser('/pages/tasks/publish')) return;
    await ui.run(this, () => {
      const form = this.data.form;
      return getApp().api.publishTask({ title: rules.required(form.title, '任务标题', 120), description: rules.required(form.description, '委托说明', 2000), originZone: d.zones[Number(this.data.originIndex)].value, destinationZone: d.zones[Number(this.data.destinationIndex)].value, originDetail: rules.required(form.originDetail, '取件位置', 200), destinationDetail: rules.required(form.destinationDetail, '送达位置', 200), rewardAmount: rules.amount(form.rewardAmount), deadline: rules.future(this.data.date, this.data.time), acceptanceMode: form.acceptanceMode, verificationMode: form.verificationMode });
    }, result => { ui.rememberTask(result.id); ui.celebrate('跑腿已发布'); wx.redirectTo({ url: '/pages/tasks/detail?id=' + result.id }); });
  },
  clearPrivate() { this.setData({ step: 0, form: { title: '', description: '', originDetail: '', destinationDetail: '', rewardAmount: '', acceptanceMode: 'GRAB', verificationMode: 'PHOTO_AND_CONFIRMATION' } }); },
});
