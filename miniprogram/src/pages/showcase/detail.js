const ui = require('../../lib/ui');
const display = require('../../lib/display');
const rules = require('../../lib/rules');

function httpsUrl(value) {
  const text = String(value || '').trim();
  return /^https:\/\/[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?(?::\d{1,5})?(?:[/?#][^\s\\]*)?$/i.test(text) ? text : '';
}
function current(page, token, userId) {
  const auth = getApp().auth;
  return page._alive && page._visible && auth.token === token && auth.user && String(auth.user.id) === String(userId);
}
function copyText(value, title) {
  if (!value) return;
  wx.setClipboardData({ data: value, showToast: false, success: () => ui.toast(title), fail: () => ui.toast('复制失败，请重试') });
}

ui.definePage({
  data: {
    project: null, comments: [], commentsError: '', contacts: [], contactHint: '',
    commentSheet: false, contactSheet: false, commentText: '', canEdit: false,
  },
  onLoad(query) {
    this._id = /^\d+$/.test(String(query.id || '')) && Number(query.id) > 0 ? String(query.id) : '';
    this._authRefresh = getApp().auth.subscribe(() => {
      if (!this._alive) return;
      this.clearPrivate();
      if (this._visible && !this._restoring) this.reload();
    });
  },
  async onShow() {
    this._visible = true;
    if (this._returningFromMedia) { this._returningFromMedia=false; return; }
    this._restoring = true;
    try { await getApp().auth.restore(); }
    finally {
      this._restoring = false;
      if (this._alive && this._visible) this.reload();
    }
  },
  onHide() {
    this._visible = false;
    if (ui.catalogMediaOpen(this)) { this._returningFromMedia=true; return; }
    this._loadSequence++; this.clearPrivate();
  },
  onUnload() { if (this._authRefresh) this._authRefresh(); },
  clearPrivate() {
    this._returningFromMedia = false;
    this._detailToken = null;
    this.setData({ project: null, comments: [], contacts: [], commentText: '', commentSheet: false,
      contactSheet: false, contactHint: '', canEdit: false, loading: false, commentsError: '', error: '' });
  },
  async reload() {
    if (!this._id) { this.setData({ error: '宣传帖编号无效', loading: false }); return; }
    const auth = getApp().auth, token = auth.token, userId = auth.user && auth.user.id;
    return ui.load(this, async () => {
      const [project, comments] = await Promise.all([
        getApp().api.project(this._id),
        getApp().api.comments(this._id).then(items => ({ items })).catch(error => ({ items: [], error: error.message || '留言暂时无法加载' })),
      ]);
      if (!userId && project.status !== 'APPROVED') throw new Error('请先验证登录，再查看自己的宣传帖');
      return { project, comments };
    }, ({ project, comments }) => {
      this._detailToken = token;
      const contactVisible = project.contactVisible === true && (!!userId || project.contactVisibility === 'PUBLIC');
      const safeProject = display.projectView({ ...project, contactVisible,
        contactInfo: contactVisible ? project.contactInfo : null,
        favorited: !!userId && project.favorited === true,
        attachments: (Array.isArray(project.attachments) ? project.attachments : []).map(binding => ({
          id: binding.id, name: binding.file && binding.file.originalName || '未命名附件',
        })),
        linkUrl: httpsUrl(project.linkUrl),
      });
      const hints = {
        LOGIN_ONLY: '登录后可按发布者规则查看联系方式。',
        INTERACTION_ONLY: '发布者设置为互动后可见；收藏或留言后重新加载确认。',
        HIDDEN: '发布者暂不公开联系方式，可在下方留言。',
      };
      return {
        project: safeProject,
        contacts: display.contacts(safeProject.contactInfo, contactVisible),
        contactHint: contactVisible ? '点击下方按钮即可复制联系方式' : (hints[project.contactVisibility] || '联系方式暂不可见。'),
        comments: (Array.isArray(comments.items) ? comments.items : []).filter(item => item.status === 'VISIBLE').map(item => ({
          id: item.id, userNickname: item.userNickname || '同学', content: String(item.content || ''),
          dateLabel: display.dateText(item.createdAt), reply: !!item.parentId,
        })),
        commentsError: comments.error || '',
        canEdit: !!userId && String(project.publisherId) === String(userId) && ['DRAFT', 'REJECTED'].includes(project.status),
      };
    });
  },
  photosChanged() {
    this.selectComponent('#project-thumb')?.load();
    return this.reload();
  },
  async authorize() {
    if (this._authorizing || this.data.busy) return null;
    this._authorizing = true;
    try {
      const user = await ui.requireUser('/pages/showcase/detail?id=' + this._id);
      if (!user || !this._alive || !this._visible || !this.data.project) return null;
      if (this._detailToken !== getApp().auth.token) { await this.reload(); return null; }
      return { token: getApp().auth.token, userId: user.id };
    } catch (error) { if (this._alive) ui.toast(error.message || '暂时无法验证登录'); return null; }
    finally { this._authorizing = false; }
  },
  async toggleFavorite() {
    const session = await this.authorize();
    if (!session || !this.data.project || !current(this, session.token, session.userId)) return;
    const remove = this.data.project.favorited;
    await ui.run(this, async () => {
      try { return await getApp().api.favorite(this._id, remove); }
      catch (error) { if (!current(this, session.token, session.userId)) return; throw error; }
    }, async () => {
      if (!current(this, session.token, session.userId) || !this.data.project) return;
      // Visibility can change after unfavoriting. Never retain the old unlocked contact.
      this.setData({ contacts: [], contactSheet: false, 'project.contactInfo': null, 'project.contactVisible': false,
        'project.favorited': !remove, 'project.favoriteCount': Math.max(0, Number(this.data.project.favoriteCount || 0) + (remove ? -1 : 1)) });
      ui.toast(remove ? '已取消收藏' : '已收藏');
      await this.reload();
    });
  },
  async openComment() {
    const session = await this.authorize();
    if (session && current(this, session.token, session.userId)) this.setData({ commentSheet: true });
  },
  commentInput(event) { if (!this.data.busy) this.setData({ commentText: event.detail.value }); },
  closeComment() { if (!this.data.busy) this.setData({ commentSheet: false }); },
  async sendComment() {
    if (!this.data.commentSheet || this.data.busy) return;
    const session = await this.authorize();
    if (!session || !this.data.commentSheet || !current(this, session.token, session.userId)) return;
    await ui.run(this, async () => {
      const content = rules.required(this.data.commentText, '留言内容', 1000);
      try { return await getApp().api.comment(this._id, content); }
      catch (error) { if (!current(this, session.token, session.userId)) return; throw error; }
    }, async () => {
      if (!current(this, session.token, session.userId)) return;
      // Clear after a successful write even if the following read fails, avoiding duplicate posts.
      this.setData({ commentText: '', commentSheet: false });
      ui.toast('留言已发布'); await this.reload();
    });
  },
  openContact() {
    if (this.data.busy || !this.data.project) return;
    this.setData({ contactSheet: true });
  },
  closeContact() { if (!this.data.busy) this.setData({ contactSheet: false }); },
  copyContact(event) {
    if (!this.data.project || this._detailToken !== getApp().auth.token || this.data.project.contactVisible !== true) return;
    const contact = this.data.contacts[Number(event.currentTarget.dataset.index)];
    if (contact) copyText(contact.value, '联系方式已复制');
  },
  copyLink() {
    if (this.data.project) copyText(httpsUrl(this.data.project.linkUrl), '链接已复制');
  },
  edit() { if (this.data.canEdit) ui.open('/pages/showcase/publish?id=' + this._id); },
});
