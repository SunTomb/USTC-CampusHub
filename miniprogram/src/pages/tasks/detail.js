const ui = require('../../lib/ui');
const d = require('../../lib/display');
const rules = require('../../lib/rules');

const ACTIVE_WORKFLOW = ['ACCEPTED', 'HEADING_TO_PICKUP', 'PICKED_UP', 'DELIVERING', 'DELIVERED', 'PENDING_CONFIRMATION'];

function privateWorkspace(value) {
  if (!value) return null;
  return {
    ...value,
    publisherContact: d.contactView(value.publisherContact),
    runnerContact: d.contactView(value.runnerContact),
    events: (value.events || []).map(d.eventView),
  };
}

ui.definePage({
  data: {
    task: null, workspace: null, applicants: [], photos: [], privateDetails: null, isOwner: false, isAssigned: false, action: null, canConfirm: false,
    confirmLabel: '确认已收到并完成', canGenerateCode: false, canRedeemCode: false, completionCode: null, completionExpiresAt: '',
    enteredCompletionCode: '', canIssue: false, progress: [], issueOpen: false, issueIndex: 0, issueDescription: '',
    issueTypes: [
      { value: 'PICKUP_FAILED', label: '无法取件' }, { value: 'LOCATION_ERROR', label: '地点有误' },
      { value: 'CONTACT_FAILED', label: '联系不上' }, { value: 'TIMEOUT', label: '预计延误' },
      { value: 'ITEM_ABNORMAL', label: '物品异常' }, { value: 'OTHER', label: '其他问题' },
    ],
  },
  onLoad(query) { this.taskId = Number(query.id); this._privateEpoch = 0; },
  onShow() { this.reload(); },
  onHide() { this.clearPrivate(); },
  async reload() {
    const epoch = this._privateEpoch;
    await getApp().auth.restore();
    if (epoch !== this._privateEpoch) return;
    return ui.load(this, async () => {
      const api = getApp().api, user = getApp().auth.user, task = await api.task(this.taskId);
      let ownApplications = [], applicants = [], workspace = null;
      if (user) {
        if (task.publisherId === user.id) applicants = await api.taskApplicants(task.id);
        else ownApplications = await api.myTaskApplications(user.id);
      }
      const isOwner = !!user && user.id === task.publisherId;
      const isAssigned = !!user && ownApplications.some(application => application.id === task.acceptedApplicationId && application.applicantId === user.id && ['ACCEPTED', 'COMPLETED'].includes(application.status));
      if (isOwner || isAssigned) workspace = await api.taskWorkspace(task.id);
      return { task, user, applicants, workspace, isOwner, isAssigned };
    }, ({ task, user, applicants, workspace, isOwner, isAssigned }) => {
      const privateTask = workspace && workspace.task ? workspace.task : task;
      const view = d.taskView(privateTask), codes = ['ACCEPTED', 'HEADING_TO_PICKUP', 'PICKED_UP', 'DELIVERING', 'PENDING_CONFIRMATION', 'COMPLETED'];
      const current = codes.indexOf(privateTask.workflowStatus), completionMode = privateTask.verificationMode === 'COMPLETION_CODE';
      const active = ACTIVE_WORKFLOW.includes(privateTask.workflowStatus), canConfirm = isOwner && privateTask.workflowStatus === 'PENDING_CONFIRMATION';
      return {
        task: view, workspace: privateWorkspace(workspace), photos: workspace ? (workspace.evidence || []).map(item => ({ ...item, downloading: false })) : [], privateDetails: workspace && workspace.privateDetails || null, isOwner, isAssigned,
        applicants: applicants.map(application => ({ ...application, statusLabel: d.status(application.status), selectable: !!user && isOwner && privateTask.workflowStatus === 'PUBLISHED' && application.status === 'PENDING' && application.applicantId !== user.id })),
        action: rules.nextTaskAction(privateTask, isAssigned && d.hasRole(user, 'RUNNER')), canConfirm,
        confirmLabel: completionMode && privateTask.workflowStatus === 'PENDING_CONFIRMATION' ? '确认完成（发布者兜底）' : '确认已收到并完成',
        canGenerateCode: isOwner && completionMode && active, canRedeemCode: isAssigned && completionMode && privateTask.workflowStatus === 'PENDING_CONFIRMATION',
        canIssue: (isOwner || isAssigned) && active, canSubmitEvidence: isAssigned && privateTask.verificationMode === 'PHOTO_AND_CONFIRMATION' && ['DELIVERING','DELIVERED'].includes(privateTask.workflowStatus) && !(workspace && workspace.evidence && workspace.evidence.length),
        progress: codes.map((code, index) => ({ code, label: d.status(code), reached: current >= index, current: current === index })),
      };
    });
  },
  async advance() {
    const action = this.data.action;
    if (!action || !await ui.requireUser('/pages/tasks/detail?id=' + this.taskId)) return;
    if (!await ui.confirm(action.label, '请仅在真实完成对应步骤后继续。')) return;
    await ui.run(this, () => getApp().api.advanceTask(this.taskId, action.status), async () => { ui.celebrate('进度已更新'); await this.reload(); });
  },
  async generateCode() {
    const epoch = this._privateEpoch;
    if (!this.data.canGenerateCode || !await ui.requireUser('/pages/tasks/detail?id=' + this.taskId)) return;
    if (this.data.completionCode && !await ui.confirm('重新生成完成码', '旧完成码会立即失效，请确认接单同学还未核销。')) return;
    if (epoch !== this._privateEpoch) return;
    await ui.run(this, () => getApp().api.generateCompletionCode(this.taskId), result => {
      if (epoch !== this._privateEpoch) return;
      this.setData({ completionCode: result.completionCode, completionExpiresAt: result.expiresAt || '' }); ui.celebrate('完成码已生成');
    });
  },
  async redeemCode() {
    if (!this.data.canRedeemCode || !await ui.requireUser('/pages/tasks/detail?id=' + this.taskId)) return;
    let completionCode;
    try { completionCode = rules.completionCode(this.data.enteredCompletionCode); } catch (error) { this.setData({ error: error.message }); return; }
    await ui.run(this, () => getApp().api.completeWithCode(this.taskId, completionCode), async () => {
      this.setData({ enteredCompletionCode: '', completionCode: null, completionExpiresAt: '' }); ui.celebrate('完成码核销成功'); await this.reload();
    });
  },
  copyContact(event) { ui.copy(event.currentTarget.dataset.value); },
  async chooseEvidence() {
    if (!this.data.canSubmitEvidence || !await ui.requireUser('/pages/tasks/detail?id=' + this.taskId)) return;
    const epoch = this._privateEpoch, token = getApp().auth.token;
    try { const photos = await getApp().taskMedia.pick(1); if (!photos.length || epoch !== this._privateEpoch || token !== getApp().auth.token) return; await ui.run(this, () => getApp().taskMedia.upload(this.taskId, 'DELIVERY_PHOTO', photos[0]), async () => { ui.celebrate('凭证已上传'); await this.reload(); }); } catch (error) { this.setData({ error: error.message }); }
  },
  async submitEvidence() {
    if (!this.data.canSubmitEvidence || !await ui.requireUser('/pages/tasks/detail?id=' + this.taskId)) return;
    await ui.run(this, () => getApp().api.submitEvidence(this.taskId), async () => { ui.celebrate('已提交送达凭证'); await this.reload(); });
  },
  async previewEvidence(event) {
    const photo = this.data.photos[Number(event.currentTarget.dataset.index)]; if (!photo) return;
    try { const path = await getApp().taskMedia.download(this.taskId, photo.id); wx.previewImage({ current:path, urls:[path] }); } catch (error) { this.setData({ error:error.message }); }
  },
  async confirmComplete() {
    if (!this.data.canConfirm || !await ui.requireUser('/pages/tasks/detail?id=' + this.taskId)) return;
    if (!await ui.confirm('确认任务完成', '请确认物品已收到，报酬与对方直接结算。')) return;
    await ui.run(this, () => getApp().api.confirmTask(this.taskId), async () => { ui.celebrate('已确认完成'); await this.reload(); });
  },
  async chooseApplicant(event) {
    const application = this.data.applicants.find(item => item.id === Number(event.currentTarget.dataset.id));
    if (!application || !application.selectable || !this.data.isOwner) return;
    if (!await ui.confirm('选择这位同学', '确认交由 ' + application.applicantNickname + ' 承接本次任务？')) return;
    await ui.run(this, () => getApp().api.acceptApplicant(this.taskId, application.id), async () => { ui.celebrate('接单人已确定'); await this.reload(); });
  },
  openIssue() { if (this.data.canIssue) this.setData({ issueOpen: true, issueDescription: '', error: '' }); },
  closeIssue() { if (!this.data.busy) this.setData({ issueOpen: false }); },
  async sendIssue() {
    if (!this.data.canIssue) return;
    await ui.run(this, () => getApp().api.taskIssue(this.taskId, { issueType: this.data.issueTypes[Number(this.data.issueIndex)].value, description: rules.required(this.data.issueDescription, '具体情况', 1000) }), async () => { this.setData({ issueOpen: false }); ui.celebrate('问题已记录'); await this.reload(); });
  },
  clearPrivate() {
    this._privateEpoch++; this._loadSequence++;
    this.setData({ canIssue: false, loading: false, workspace: null, photos: [], privateDetails: null, applicants: [], isOwner: false, isAssigned: false, action: null, canConfirm: false, canGenerateCode: false, canRedeemCode: false, completionCode: null, completionExpiresAt: '', enteredCompletionCode: '', issueOpen: false, issueDescription: '' });
  },
});
