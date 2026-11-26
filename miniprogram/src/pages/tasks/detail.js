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
    events: (value.events || []).map(d.eventView).map(e => ({...e,eventTypeLabel:({TASK_UPDATED:'委托已修改',CANCELLED:'委托已撤下'})[e.eventType] || e.eventTypeLabel})),
  };
}

ui.definePage({
  data: {
    task: null, workspace: null, applicants: [], photos: [], privateDetails: null, isOwner: false, isAssigned: false, action: null, canConfirm: false,
    canUploadRequest: false, canUploadDelivery: false, canSubmitEvidence: false, previewPath: '',
    confirmLabel: '确认已收到并完成', canGenerateCode: false, canRedeemCode: false, completionCode: null, completionExpiresAt: '',
    enteredCompletionCode: '', canIssue: false, progress: [], issueOpen: false, issueIndex: 0, issueDescription: '',
    issueTypes: [
      { value: 'PICKUP_FAILED', label: '无法取件' }, { value: 'LOCATION_ERROR', label: '地点有误' },
      { value: 'CONTACT_FAILED', label: '联系不上' }, { value: 'TIMEOUT', label: '预计延误' },
      { value: 'ITEM_ABNORMAL', label: '物品异常' }, { value: 'OTHER', label: '其他问题' },
    ],
  },
  onLoad(query) { this.taskId = Number(query.id); this._privateEpoch = 0; this._visible = true; },
  onShow() { this._visible = true; this._shown = this.reload(); },
  onHide() { this._visible = false; this.clearPrivate(); },
  onUnload() { this._visible = false; this.clearPrivate(); },
  async reload() {
    if (!this._alive || !this._visible) return;
    this.clearPrivate();
    const epoch = this._privateEpoch;
    await getApp().auth.restore();
    if (epoch !== this._privateEpoch) return;
    await ui.load(this, async () => {
      const api = getApp().api, user = getApp().auth.user, task = await api.task(this.taskId);
      let ownApplications = [], applicants = [], workspace = null;
      if (epoch !== this._privateEpoch) return;
      if (user) {
        if (task.publisherId === user.id) applicants = await api.taskApplicants(task.id);
        else ownApplications = await api.myTaskApplications(user.id);
      }
      const isOwner = !!user && user.id === task.publisherId;
      const isAssigned = !!user && ownApplications.some(application => application.id === task.acceptedApplicationId && application.applicantId === user.id && ['ACCEPTED', 'COMPLETED'].includes(application.status));
      if (epoch !== this._privateEpoch) return;
      if (isOwner || isAssigned) workspace = await api.taskWorkspace(task.id);
      return { task, user, applicants, workspace, isOwner, isAssigned };
    }, ({ task, user, applicants, workspace, isOwner, isAssigned }) => {
      const privateTask = workspace && workspace.task ? workspace.task : task;
      const view = d.taskView(privateTask), codes = ['ACCEPTED', 'HEADING_TO_PICKUP', 'PICKED_UP', 'DELIVERING', 'PENDING_CONFIRMATION', 'COMPLETED'];
      const current = codes.indexOf(privateTask.workflowStatus), completionMode = privateTask.verificationMode === 'COMPLETION_CODE';
      const active = ACTIVE_WORKFLOW.includes(privateTask.workflowStatus);
      const photos = ((workspace && workspace.evidence) || []).map(({id, evidenceType}) => ({id, evidenceType, previewPath: '', failed: false}));
      const deliveryCount = photos.filter(p => p.evidenceType === 'DELIVERY_PHOTO').length;
      const canConfirm = isOwner && privateTask.workflowStatus === 'PENDING_CONFIRMATION' && (completionMode || deliveryCount > 0);
      const deliveryStage = isAssigned && privateTask.verificationMode === 'PHOTO_AND_CONFIRMATION' && ['DELIVERING', 'DELIVERED'].includes(privateTask.workflowStatus);
      return {
        task: { ...view, initial: (view.title || '物').slice(0, 1) }, workspace: privateWorkspace(workspace), photos, privateDetails: workspace && workspace.privateDetails || null, isOwner, isAssigned,
        applicants: applicants.map(application => ({ ...application, statusLabel: d.status(application.status), selectable: !!user && isOwner && !view.expired && privateTask.workflowStatus === 'PUBLISHED' && ['PENDING','APPLIED'].includes(application.status) && application.applicantId !== user.id })),
        action: rules.nextTaskAction(privateTask, isAssigned && d.hasRole(user, 'RUNNER')), canConfirm,
        confirmLabel: completionMode && privateTask.workflowStatus === 'PENDING_CONFIRMATION' ? '确认完成（发布者兜底）' : '确认已收到并完成',
        canGenerateCode: isOwner && completionMode && active, canRedeemCode: isAssigned && completionMode && privateTask.workflowStatus === 'PENDING_CONFIRMATION',
        canIssue: (isOwner || isAssigned) && active, canUploadRequest: isOwner && (active || privateTask.workflowStatus === 'PUBLISHED') && photos.filter(p => p.evidenceType === 'REQUEST_IMAGE').length < 3,
        canUploadDelivery: deliveryStage && deliveryCount < 3, canSubmitEvidence: deliveryStage && deliveryCount > 0,
        progress: codes.map((code, index) => ({ code, label: d.status(code), reached: current >= index, current: current === index })),
      };
    });
    if (epoch === this._privateEpoch) await Promise.all(this.data.photos.map(photo => this.loadPhoto(photo, epoch)));
  },
  async loadPhoto(photo, epoch) {
    const media = getApp().taskMedia, token = getApp().auth.token;
    const current = () => this._alive && this._visible && epoch === this._privateEpoch && token === getApp().auth.token;
    try {
      const path = await media.download(this.taskId, photo.id);
      if (!current()) { media.discard(path); return; }
      this.setData({ photos: this.data.photos.map(p => p.id === photo.id ? {...p, previewPath:path, failed:false} : p) });
    } catch (_) {
      if (current()) this.setData({ photos: this.data.photos.map(p => p.id === photo.id ? {...p, failed:true} : p) });
    }
  },
  editListing() {
    if(this.data.isOwner && this.data.workspace && this.data.workspace.canEdit)ui.open('/pages/tasks/publish?edit='+this.taskId);
  },
  async withdraw() {
    const v=this._privateEpoch;
    if(this.data.busy || !this.data.isOwner || !this.data.workspace || !this.data.workspace.canWithdraw)return;
    if(!await ui.confirm('撤下委托','下架后不再接单，待选报名将关闭并收到通知。历史记录保留在「我的订单」。')||v!==this._privateEpoch)return;
    const ok=await ui.run(this,()=>getApp().api.withdrawTask(this.taskId));
    if(v===this._privateEpoch && this._visible){
      const failure=ok?'':this.data.error;if(ok)ui.celebrate('委托已撤下');await this.reload();
      if(failure && this._visible && this.data.workspace)this.setData({error:failure});
    }
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
  async chooseEvidence(event) {
    const kind = event.currentTarget.dataset.kind;
    const allowed = () => kind === 'REQUEST_IMAGE' ? this.data.canUploadRequest : kind === 'DELIVERY_PHOTO' && this.data.canUploadDelivery;
    if (!allowed() || this.data.busy || this._picking) return;
    const auth = getApp().auth, token = auth.token, actor = auth.user && auth.user.id;
    const same = () => this._alive && auth.token === token && actor && auth.user && auth.user.id === actor;
    this._picking = true;
    try {
      const photos = await getApp().taskMedia.pick(1);
      // The system picker may hide the page. Use fresh permissions when it returns.
      if (this._shown) await this._shown;
      if (!photos.length || !same() || !this._visible || !allowed()) return;
      await ui.run(this, () => getApp().taskMedia.upload(this.taskId, kind, photos[0]), () => this.reload());
    } catch (error) { if (same() && this._visible) this.setData({error:error.message}); }
    finally { this._picking = false; }
  },
  async submitEvidence() {
    if (!this.data.canSubmitEvidence || !await ui.requireUser('/pages/tasks/detail?id=' + this.taskId) || !this.data.canSubmitEvidence) return;
    await ui.run(this, () => getApp().api.submitEvidence(this.taskId), () => this.reload());
  },
  previewEvidence(event) {
    const photo = this.data.photos[Number(event.currentTarget.dataset.index)];
    if (photo && photo.previewPath) this.setData({previewPath:photo.previewPath});
    else if (photo && photo.failed) this.reload();
  },
  closePreview() { this.setData({previewPath:''}); },
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
    for (const photo of this.data.photos) if (photo.previewPath) getApp().taskMedia.discard(photo.previewPath);
    this.setData({ task: null, previewPath: '', canSubmitEvidence: false, canUploadRequest: false, canUploadDelivery: false, canIssue: false, loading: false, workspace: null, photos: [], privateDetails: null, applicants: [], isOwner: false, isAssigned: false, action: null, canConfirm: false, canGenerateCode: false, canRedeemCode: false, completionCode: null, completionExpiresAt: '', enteredCompletionCode: '', issueOpen: false, issueDescription: '' });
  },
});
