const ui = require('../../lib/ui');
const display = require('../../lib/display');
const rules = require('../../lib/rules');
const LOCAL_KEY = 'showcase-editor';
const AD_OPTIONS = [{ value: '', label: '请选择宣传类型' }, ...display.adTypes];
const VISIBILITY = [
  { value: 'LOGIN_ONLY', label: '登录后可见' },
  { value: 'INTERACTION_ONLY', label: '收藏或留言后可见' },
  { value: 'PUBLIC', label: '所有访客可见' },
  { value: 'HIDDEN', label: '不公开' },
];
const TEXT_FIELDS = ['title', 'adType', 'description', 'contactInfo', 'contactVisibility', 'summary', 'tags', 'campusZone', 'linkUrl', 'expiryDate', 'expiryTime'];
function validId(value) { return /^\d+$/.test(String(value || '')) && Number(value) > 0; }
function blankForm() {
  const parts = display.dateParts(86400000);
  return { title: '', adType: '', description: '', contactInfo: '', contactVisibility: 'LOGIN_ONLY',
    summary: '', tags: '', campusZone: '', linkUrl: '', expiryEnabled: false, expiryDate: parts.date, expiryTime: parts.time };
}
function localForm(value) {
  const form = blankForm();
  if (!value || typeof value !== 'object') return form;
  TEXT_FIELDS.forEach(key => { if (typeof value[key] === 'string') form[key] = value[key]; });
  form.expiryEnabled = value.expiryEnabled === true;
  return form;
}
function serverForm(item) {
  const form = blankForm();
  TEXT_FIELDS.filter(key => !key.startsWith('expiry')).forEach(key => { form[key] = String(item[key] || ''); });
  if (item.expiresAt) {
    const parts = String(item.expiresAt).match(/^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2})/);
    if (parts) { form.expiryEnabled = true; form.expiryDate = parts[1]; form.expiryTime = parts[2]; }
  }
  // Even the owner's form must respect the server's explicit contact decision.
  if (item.contactVisible !== true) form.contactInfo = '';
  return form;
}
function labels(form) {
  return {
    typeLabel: (display.adTypes.find(item => item.value === form.adType) || {}).label || '请选择宣传类型',
    visibilityLabel: (VISIBILITY.find(item => item.value === form.contactVisibility) || {}).label || '请选择可见范围',
    zoneLabel: display.zone(form.campusZone),
    typeIndex: Math.max(0, AD_OPTIONS.findIndex(item => item.value === form.adType)),
    visibilityIndex: Math.max(0, VISIBILITY.findIndex(item => item.value === form.contactVisibility)),
  };
}
function optional(value, label, max) { const text = String(value || '').trim(); return text ? rules.required(text, label, max) : null; }
function payloadFor(form, coverFileId) {
  const adType = rules.required(form.adType, '宣传类型', 32);
  if (!display.adTypes.some(item => item.value === adType)) throw new Error('请选择有效的宣传类型');
  const contactVisibility = rules.required(form.contactVisibility, '联系方式可见范围', 32);
  if (!VISIBILITY.some(item => item.value === contactVisibility)) throw new Error('请选择有效的联系方式可见范围');
  if (form.campusZone && !display.zones.some(item => item.value === form.campusZone)) throw new Error('请选择有效校区');
  const linkUrl = optional(form.linkUrl, '外部链接', 255);
  if (linkUrl && !/^https:\/\/[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?(?::\d{1,5})?(?:[/?#][^\s\\]*)?$/i.test(linkUrl)) throw new Error('外部链接须为完整的 https 地址，不支持其他协议');
  let expiresAt = null;
  if (form.expiryEnabled) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(form.expiryDate) || !/^([01]\d|2[0-3]):[0-5]\d$/.test(form.expiryTime)) throw new Error('请选择完整的截止日期和时间');
    expiresAt = rules.future(form.expiryDate, form.expiryTime);
  }
  return {
    title: rules.required(form.title, '标题', 120), adType,
    description: rules.required(form.description, '正文', 20000),
    contactInfo: rules.required(form.contactInfo, '联系方式', 255), contactVisibility,
    summary: optional(form.summary, '摘要', 500), tags: optional(form.tags, '标签', 500),
    campusZone: form.campusZone || null, linkUrl, expiresAt, coverFileId: coverFileId || null,
  };
}

ui.definePage({
  data: {
    authorized: false, ready: false, submitting: false, storageWarning: '', form: blankForm(), types: AD_OPTIONS, visibilityOptions: VISIBILITY,
    typeLabel: '请选择宣传类型', typeIndex: 0, visibilityIndex: 0, zoneLabel: '全校', visibilityLabel: '登录后可见', draftId: '', statusLabel: '尚未保存',
    editable: true, canSubmit: true, dirty: false, savedMessage: '', restoreHint: '', creationUncertain: false,
  },
  onLoad(query) {
    this._queryId = query.id ? String(query.id) : '';
    this._invalidId = !!this._queryId && !validId(this._queryId);
    this._authRefresh = getApp().auth.subscribe(auth => {
      if (!this._alive) return;
      if (!auth.user || this._editorToken !== auth.token || String(this._editorUser) !== String(auth.user.id)) this.clearPrivate();
      if (auth.user && this._visible && !this._restoring) this.openEditor();
    });
  },
  onShow() {
    this._visible = true;
    if (this._openingEditor) { this._reopenOnShow = true; return; }
    this.openEditor(this._initialized && !this.data.dirty);
  },
  onHide() { this._visible = false; this._loadSequence++; this.setData({ loading: false }); },
  onUnload() { if (this._authRefresh) this._authRefresh(); },
  clearPrivate() {
    this._initialized = false; this._editorToken = null; this._editorUser = null;
    this._savedPayload = ''; this._coverFileId = null; this._awaitingSubmit = false; this._storageUnavailable = false;
    this.setData({ authorized: false, ready: false, submitting: false, storageWarning: '', form: blankForm(), draftId: '', statusLabel: '尚未保存', editable: true,
      canSubmit: true, dirty: false, savedMessage: '', restoreHint: '', creationUncertain: false,
      typeLabel: '请选择宣传类型', visibilityLabel: '登录后可见', typeIndex: 0, visibilityIndex: 0, zoneLabel: '全校', loading: false, error: '' });
  },
  sameSession() {
    const auth = getApp().auth;
    return this._alive && auth.authenticated && auth.token === this._editorToken &&
      String(auth.user.id) === String(this._editorUser);
  },
  nextRoute() { return '/pages/showcase/publish' + (this._queryId && validId(this._queryId) ? '?id=' + this._queryId : ''); },
  async openEditor(force = false) {
    if (this._openingEditor || this.data.busy) return;
    this._openingEditor = true; this._restoring = true;
    try {
      const user = await ui.requireUser(this.nextRoute());
      this._restoring = false;
      if (!user || !this._alive || !this._visible) return;
      const token = getApp().auth.token;
      if (this._initialized && !force && this._editorToken === token && String(this._editorUser) === String(user.id)) return;
      this._editorToken = token; this._editorUser = user.id;
      this.setData({ authorized: true, ready: false });
      if (this._invalidId) { this.setData({ error: '草稿编号无效' }); return; }
      await ui.load(this, async () => {
        // ui.readLocal derives a user-specific key. Never read before authorization.
        const stored = ui.readLocal(LOCAL_KEY, null);
        const local = stored && String(stored.ownerId) === String(user.id) ? stored : null;
        const id = this._queryId || this.data.draftId || (local && validId(local.id) ? String(local.id) : '');
        if (id) {
          const project = await getApp().api.project(id);
          if (String(project.publisherId) !== String(user.id)) throw new Error('只能编辑自己发布的宣传，站务身份也不能代为编辑');
          const editable = ['DRAFT', 'REJECTED'].includes(project.status);
          const matching = local && String(local.id) === String(id);
          const unsent = editable && matching && local.dirty === true;
          return { id, project, local: matching ? local : null, form: unsent ? localForm(local.form) : serverForm(project),
            editable, dirty: !!unsent, hint: unsent ? '已恢复这份草稿在本机尚未保存的修改。' : '已读取服务器保存的宣传。' };
        }
        return { id: '', project: null, local, form: local && !local.id ? localForm(local.form) : blankForm(),
          editable: true, dirty: !!(local && local.dirty), hint: local && !local.id ? '已恢复当前账号的本机未发送草稿。' : '' };
      }, result => {
        this._initialized = true;
        this._savedStatus=result.project && result.project.status;
        this._coverFileId = result.project && result.project.coverFileId || null;
        this._savedPayload = result.local && !result.dirty ? result.local.savedPayload || '' : '';
        this._awaitingSubmit = !!(result.id && result.local && result.local.awaitingSubmit &&
          ['DRAFT', 'REJECTED'].includes(result.project.status));
        return {
          ready: true, form: result.form, ...labels(result.form), draftId: String(result.id || ''),
          statusLabel: result.project ? display.status(result.project.status) : '尚未保存',
          editable: result.editable, canSubmit: result.editable || this._awaitingSubmit, dirty: result.dirty,
          restoreHint: result.hint, savedMessage: '', creationUncertain: !result.id && !!(result.local && result.local.creationUncertain),
        };
      });
    } catch (error) {
      if (this._alive && this._visible) this.setData({ error: error.message || '暂时无法读取草稿' });
    } finally {
      this._openingEditor = false; this._restoring = false;
      if (this._reopenOnShow) {
        this._reopenOnShow = false;
        if (this._alive && this._visible) this.openEditor();
      }
    }
  },
  enter() { this.openEditor(true); },
  async reload() {
    if (this.data.busy || this._confirming) return;
    if (this.data.dirty && !(await ui.confirm('重新读取草稿？', '会重新核对服务器记录，并恢复本机尚未保存的修改。不会提交审核。'))) return;
    return this.openEditor(true);
  },
  persist() {
    if (!this.sameSession() || this._storageUnavailable) return false;
    const saved = ui.writeLocal(LOCAL_KEY, { ownerId: this._editorUser, id: this.data.draftId || null,
      form: this.data.form, dirty: this.data.dirty, savedPayload: this._savedPayload || '',
      awaitingSubmit: this._awaitingSubmit === true, creationUncertain: this.data.creationUncertain });
    if (!saved) {
      this._storageUnavailable = true;
      this.setData({ storageWarning: '本机暂存不可用，未保存的文字在离开页面后可能丢失。服务器保存结果不受影响，请记下草稿编号。' });
    }
    return saved;
  },
  changeForm(patch) {
    if (!this.sameSession() || !this._visible || !this._initialized || this.data.busy || !this.data.editable) return;
    const form = { ...this.data.form, ...patch };
    this.setData({ form, ...labels(form), dirty: true, savedMessage: '' }); this.persist();
  },
  input(event) {
    const key = event.currentTarget.dataset.field;
    if (TEXT_FIELDS.includes(key)) this.changeForm({ [key]: event.detail.value });
  },
  changeType(event) {
    const item = AD_OPTIONS[Number(event.detail.value)]; if (item) this.changeForm({ adType: item.value });
  },
  changeVisibility(event) {
    const item = VISIBILITY[Number(event.detail.value)]; if (item) this.changeForm({ contactVisibility: item.value });
  },
  changeZone(event) { this.changeForm({ campusZone: event.detail.value || '' }); },
  changeExpiry(event) { this.changeForm({ expiryEnabled: event.detail.value === true }); },
  changeDate(event) { this.changeForm({ expiryDate: event.detail.value }); },
  changeTime(event) { this.changeForm({ expiryTime: event.detail.value }); },
  async save(event) {
    const submit = event.currentTarget.dataset.mode === 'submit';
    if (!this.sameSession() || !this._initialized || this.data.busy || this.data.loading || this._confirming ||
      (submit ? !this.data.canSubmit : !this.data.editable)) return;
    if (!submit) {
      if (this.persist()) { this.setData({ savedMessage: '本机草稿已保存，尚未发送审核。' }); ui.toast('已存为本机草稿'); }
      return;
    }
    const token = this._editorToken, userId = this._editorUser;
    const active = () => this.sameSession() && token === this._editorToken && String(userId) === String(this._editorUser);
    if (submit) {
      this._confirming = true;
      const confirmed = await ui.confirm('提交站务核验？', '提交后内容会发送到服务器并进入审核。仅想保留文字，请选择存为本机草稿。');
      this._confirming = false;
      if (!confirmed || !active()) return;
    }
    this.setData({ submitting: submit });
    await ui.run(this, async () => {
      if (!active()) return;
      const payload = payloadFor(this.data.form, this._coverFileId);
      const fingerprint = JSON.stringify(payload);
      let id = this.data.draftId;
      if (!id && this.data.creationUncertain) throw new Error('请先核对我的宣传，确认上次保存是否成功');
      if (!id || fingerprint !== this._savedPayload || this.data.dirty) {
        if (id && !this.data.editable) throw new Error('当前状态不能修改，请重新读取宣传状态');
        let saved;
        if (!id) { this.setData({ creationUncertain: true }); this.persist(); }
        try { saved = id ? await getApp().api.updateProject(id, payload) : await getApp().api.createProject(payload); }
        catch (error) {
          if (!active()) return;
          if (!id) {
            const uncertain = !!(error.network || !error.status || error.status >= 500 || error.status === 200);
            // Store the pending marker BEFORE create, so unloading or switching accounts cannot lead to an automatic duplicate.
            this.setData({ creationUncertain: uncertain, savedMessage: uncertain ? '未能确认新草稿是否保存。请先到个人 → 我的订单 → 宣传核对，不要重复创建。' : '' }); this.persist();
          }
          throw error;
        }
        if (!active()) return;
        if (!saved || !validId(saved.id)) {
          this.setData({ creationUncertain: !id, savedMessage: '服务器未返回可用编号，请先核对我的宣传。' }); this.persist();
          throw new Error('无法确认草稿编号，请勿重复创建');
        }
        id = String(saved.id);
        // Persist the created id BEFORE attempting submit. A submit retry must use this same record.
        this._savedPayload = fingerprint; this._awaitingSubmit = true;
        this.setData({ draftId: id, dirty: false, statusLabel: display.status(saved.status), creationUncertain: false,
          editable: ['DRAFT', 'REJECTED'].includes(saved.status), canSubmit: true,
          savedMessage: '已保存到服务器（编号 ' + id + '），当前状态：' + display.status(saved.status) + '。' });
        this.persist();
        this._savedStatus=saved.status;
      }
      if (!active() || !this._visible) return;
      const photos=this.selectComponent?.('#project-media');
      if(photos)await photos.saveTo(Number(id));
      if(!active())return;
      if (this._savedStatus === 'PENDING_REVIEW') {
          this._awaitingSubmit = false;
          this.setData({ canSubmit: false, savedMessage: '已提交站务核验（编号 ' + id + '），通过后公开展示。' });
          this.persist(); ui.toast('已提交站务核验'); return;
      }
      if (!active() || !this._visible) return;
      if (!submit) { if (!this._storageUnavailable) ui.toast('已保存'); return; }
      this.setData({ savedMessage: '草稿已保存（编号 ' + id + '）。提交审核尚未完成；若失败，可重试提交，不会重新创建。' });
      let result;
      try { result = await getApp().api.submitProject(id); }
      catch (error) { if (!active()) return; throw error; }
      if (!active()) return;
      if (!result || result.status !== 'PENDING_REVIEW') {
        if (result) this.setData({ statusLabel: display.status(result.status) });
        throw new Error('未确认进入待核验状态，请重新读取宣传状态后再操作');
      }
      this._awaitingSubmit = false;
      this.setData({ statusLabel: display.status(result.status), editable: false, canSubmit: false, dirty: false,
        savedMessage: '已保存并完成提交，当前状态：' + display.status(result.status) + '。' });
      // Keep the id locally so a later visit cannot accidentally recreate the same publication.
      this.persist(); if (!this._storageUnavailable && this._visible) ui.toast('已提交站务核验');
    });
  },
  async resolveCreate() {
    if (!this.sameSession() || this.data.busy || !this.data.creationUncertain || this._confirming) return;
    this._confirming = true;
    const confirmed = await ui.confirm('已核对我的宣传？', '若上次已经创建，请从个人 → 我的订单 → 宣传进入草稿。只有确认不存在该宣传时，才继续尝试创建。');
    this._confirming = false;
    if (confirmed && this.sameSession()) { this.setData({ creationUncertain: false, savedMessage: '' }); this.persist(); }
  },
  async startNew() {
    if (!this.sameSession() || this.data.busy || this._confirming) return;
    this._confirming = true;
    const confirmed = await ui.confirm('另写一份宣传？', '当前服务器记录会保留，本机编辑内容将替换为空白新稿。');
    this._confirming = false;
    if (!confirmed || !this.sameSession()) return;
    this._queryId = ''; this._savedPayload = ''; this._coverFileId = null; this._awaitingSubmit = false;
    this.setData({ form: blankForm(), draftId: '', statusLabel: '尚未保存', dirty: false, editable: true, canSubmit: true,
      savedMessage: '', restoreHint: '', error: '', creationUncertain: false, typeLabel: '请选择宣传类型', visibilityLabel: '登录后可见' });
    this.persist();
  },
});
