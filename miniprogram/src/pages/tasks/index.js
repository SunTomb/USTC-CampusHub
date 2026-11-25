const ui = require('../../lib/ui');
const d = require('../../lib/display');
ui.definePage({
  data: { zone: '', mode: '', tasks: [], allTasks: [], selected: null, message: '', unlock: false },
  onShow() { this.reload(); },
  reload() { return ui.load(this, () => getApp().api.tasks(), items => { const tasks = (items || []).map(d.taskView); return { allTasks: tasks, tasks: this.filtered(tasks) }; }); },
  filtered(items) { return items.filter(task => !task.expired && (!this.data.zone || task.originZone === this.data.zone || task.destinationZone === this.data.zone) && (!this.data.mode || task.acceptanceMode === this.data.mode)); },
  changeZone(event) { this.setData({ zone: event.detail.value }); this.setData({ tasks: this.filtered(this.data.allTasks) }); },
  changeMode(event) { this.setData({ mode: event.currentTarget.dataset.mode }); this.setData({ tasks: this.filtered(this.data.allTasks) }); },
  detail(event) { ui.open('/pages/tasks/detail?id=' + event.currentTarget.dataset.id); },
  async publish() { if (await ui.requireUser('/pages/tasks/publish')) ui.open('/pages/tasks/publish'); },
  async choose(event) {
    const task = this.data.tasks.find(item => item.id === Number(event.currentTarget.dataset.id));
    if (!task || task.expired || this.data.busy) return;
    const user = await ui.requireUser('/pages/tasks/index');
    if (!user) return;
    if (task.publisherId === user.id) { ui.open('/pages/tasks/detail?id=' + task.id); return; }
    if (!d.hasRole(user, 'RUNNER')) { this.setData({ unlock: true }); return; }
    this.setData({ selected: task, message: '', error: '' });
  },
  closeSheet() { if (!this.data.busy) this.setData({ selected: null }); },
  closeUnlock() { this.setData({ unlock: false }); },
  async take() {
    const task = this.data.selected;
    const user = await ui.requireUser('/pages/tasks/index');
    if (!task || !user || task.publisherId === user.id) return;
    const token = getApp().auth.token;
    await ui.run(this, async () => {
      if (task.acceptanceMode === 'APPLICATION') {
        const own = await getApp().api.myTaskApplications(user.id);
        const auth = getApp().auth;
        if (!this._alive || auth.token !== token || !auth.user || auth.user.id !== user.id || !this.data.selected || this.data.selected.id !== task.id) return;
        if (own.some(a => a.taskId === task.id && ['PENDING', 'ACCEPTED'].includes(a.status))) throw new Error('你已经报名，请在通行证查看进度');
        return getApp().api.applyTask(task.id, this.data.message.trim());
      }
      return getApp().api.grab(task.id);
    }, async result => { if (!result) return; this.setData({ selected: null }); ui.celebrate(task.acceptanceMode === 'APPLICATION' ? '报名已发送' : '接单成功'); ui.open('/pages/tasks/detail?id=' + task.id); });
  },
  clearPrivate() { this.setData({ selected: null, unlock: false, message: '' }); },
});
