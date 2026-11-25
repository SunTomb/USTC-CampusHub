const ui = require('../../lib/ui');
const display = require('../../lib/display');

// List responses may contain contactInfo. Keep only public card fields in page data.
function card(item) {
  return display.projectView({
    id: item.id, title: item.title, adType: item.adType, summary: item.summary,
    tags: item.tags, campusZone: item.campusZone, status: item.status,
    expiresAt: item.expiresAt, publisherNickname: item.publisherNickname,
  });
}
function publicItems(items) {
  return (Array.isArray(items) ? items : []).filter(item => item.status === 'APPROVED' &&
    (!item.expiresAt || display.localMillis(item.expiresAt) > Date.now()));
}

ui.definePage({
  data: {
    types: [{ value: '', label: '全部宣传' }, ...display.adTypes], adType: '', campusZone: '',
    keywordInput: '', keyword: '', projects: [], featured: [], featuredError: '',
  },
  onLoad() {
    this._authRefresh = getApp().auth.subscribe(() => {
      if (this._alive && this._visible) this.reload();
    });
  },
  onShow() { this._visible = true; this.reload(); },
  onHide() { this._visible = false; this._loadSequence++; this.setData({ loading: false }); },
  onUnload() { if (this._authRefresh) this._authRefresh(); },
  clearPrivate() { /* Public cards deliberately never retain contact or viewer data. */ },
  async reload() {
    const query = { adType: this.data.adType, campusZone: this.data.campusZone, keyword: this.data.keyword };
    return ui.load(this, async () => {
      const api = getApp().api;
      const [items, highlights] = await Promise.all([
        api.showcase(query),
        api.featured().then(items => ({ items })).catch(error => ({ items: [], error: error.message || '精选暂不可用' })),
      ]);
      return { items, highlights };
    }, ({ items, highlights }) => {
      const featured = publicItems(highlights.items).filter(item =>
        (!query.adType || item.adType === query.adType) &&
        (!query.campusZone || item.campusZone === query.campusZone) &&
        (!query.keyword || [item.title, item.summary, item.description, item.tags].some(value =>
          String(value || '').toLowerCase().includes(query.keyword.toLowerCase()))));
      return { projects: publicItems(items).map(card), featured: featured.map(card), featuredError: highlights.error || '' };
    });
  },
  changeType(event) {
    const value = event.currentTarget.dataset.value;
    if (!this.data.types.some(type => type.value === value) || value === this.data.adType) return;
    this.setData({ adType: value }); this.reload();
  },
  changeZone(event) { this.setData({ campusZone: event.detail.value || '' }); this.reload(); },
  keywordInput(event) { this.setData({ keywordInput: event.detail.value }); },
  search() { this.setData({ keyword: this.data.keywordInput.trim() }); this.reload(); },
  resetFilters() {
    this.setData({ adType: '', campusZone: '', keyword: '', keywordInput: '' }); this.reload();
  },
  async publish() {
    if (this._opening) return;
    this._opening = true;
    try {
      const user = await ui.requireUser('/pages/showcase/publish');
      if (user && this._alive && this._visible) ui.open('/pages/showcase/publish');
    } catch (error) { if (this._alive) ui.toast(error.message || '暂时无法验证登录'); }
    finally { this._opening = false; }
  },
});
