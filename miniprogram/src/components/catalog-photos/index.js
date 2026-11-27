const { path, transfer } = require('../../lib/catalog-transfer');

const { ApiError } = require('../../lib/request');

function discard(images) {
  for (const image of images) if (image.temp) getApp().taskMedia.discard(image.src);
}

Component({
  options: { styleIsolation: 'apply-shared' },
  properties: {
    kind: String, objectId: Number, editable: Boolean, compact: Boolean,
    variant: { type: String, value: '' }, fallbackChar: { type: String, value: '' },
    fallbackSub: { type: String, value: '' }, tone: { type: Number, value: 0 },
  },
  data: { queue: [], images: [], allUrls: [], totalCount: 0, previewSrc: '', busy: false, error: '' },
  observers: {
    'kind,objectId': function (kind, id) {
      if (!this._alive) return;
      // Creating a draft gives its existing upload queue a parent for the first time.
      if (!(this._kind === kind && !this._parent && id > 0)) this.reset();
      this._kind = kind; this._parent = id;
      this.load();
    },
  },
  lifetimes: {
    attached() {
      this._alive = true; this._rev = 0; this._seq = 0;
      this._kind = this.properties.kind; this._parent = this.properties.objectId;
      const auth = getApp().auth;
      let token = auth.token, uid = auth.user?.id;
      this._off = auth.subscribe(() => {
        if (token !== auth.token || uid !== auth.user?.id) {
          token = auth.token; uid = auth.user?.id; this.reset(); this.load();
        }
      });
      this.load();
    },
    detached() { this._alive = false; if (this._off) this._off(); this.reset(); },
  },
  pageLifetimes: {
    hide() {
      this._hidden = true; this._seq++;
      // Keep only the viewer's temporary downloads until it returns or the session ends.
      if (this.isSelectingMedia() && !this._picking) {
        this._viewerFiles = this.data.images.filter(f => f.temp);
        this.clearImages(false);
      } else this.clearImages();
    },
    show() { this._hidden = false; this.releaseViewer(); this.load(); },
  },
  methods: {
    isSelectingMedia() { return !!this._picking || Date.now() < (this._previewUntil || 0); },
    releaseViewer() { discard(this._viewerFiles || []); this._viewerFiles = []; this._previewUntil = 0; },
    clearImages(unlink = true) {
      if (unlink) discard(this.data.images);
      this.setData({ images: [], allUrls: [], totalCount: 0, previewSrc: '' });
    },
    reset() {
      this._seq++; this._rev++; this.releaseViewer(); this.clearImages();
      this.setData({ queue: [], busy: false, error: '' });
    },
    async load() {
      const { kind, objectId } = this.properties;
      if (!this._alive || !objectId || !kind || this._hidden) return;
      const seq = ++this._seq, app = getApp(), images = [];
      try {
        const files = await app.client.get(path(kind, objectId));
        for (const f of files) {
          if (seq !== this._seq) break;
          const url = app.config.baseUrl + path(kind, objectId, f.id);
          images.push({ id: f.id, temp: !f.published, src: f.published ? url : await transfer('downloadFile', { url }) });
        }
        if (seq === this._seq) {
          discard(this.data.images);
          this.setData({ images, allUrls: images.map(f => f.src), totalCount: images.length });
          return;
        }
      } catch (_) {
        if (seq === this._seq && this.properties.editable && !this.data.error) this.setData({ error: '图片暂时无法加载' });
      }
      discard(images);
    },
    preview(e) {
      const current = e.currentTarget.dataset.src;
      const urls = this.data.allUrls.concat(this.properties.editable ? this.data.queue.map(f => f.path) : []);
      if (!current || !urls.includes(current)) return;
      const rev = this._rev;
      const fallback = () => {
        this._previewUntil = 0;
        if (this._alive && rev === this._rev && !this._hidden) this.setData({ previewSrc: current });
      };
      if (typeof wx === 'undefined' || typeof wx.previewImage !== 'function') return fallback();
      this._previewUntil = Date.now() + 1500;
      wx.previewImage({ current, urls, fail: fallback });
    },
    closePreview() { this.setData({ previewSrc: '' }); },
 async pick(){if(this._picking||this.data.busy||!this.properties.editable)return;this._picking=true;const n=this._rev;try{const capacity=(this.properties.kind==='SHOP'?1:3)-this.data.images.length-this.data.queue.length;if(capacity<1)return;const files=await getApp().taskMedia.pick(capacity);if(n===this._rev&&this._alive)this.setData({queue:this.data.queue.concat(files),error:''});}catch(e){if(n===this._rev)this.setData({error:e.message});}finally{this._picking=false;}},
 drop(e){if(this.data.busy)return;this.setData({queue:this.data.queue.filter(x=>x.key!==e.currentTarget.dataset.key)});},
    async saveTo(id) {
      if (!id || !this.data.queue.length) return;
      if (this.data.busy) throw Error('图片正在上传');
      const rev = this._rev, app = getApp(), token = app.auth.token, kind = this.properties.kind;
      this.setData({ busy: true, error: '' });
      try {
        for (const f of this.data.queue.slice()) {
          if (rev !== this._rev || !this._alive || token !== app.auth.token) throw Error('页面已变化');
          await transfer('uploadFile', { url: app.config.baseUrl + path(kind, id), filePath: f.path, name: 'file', formData: { clientRequestId: f.key } });
          if (rev !== this._rev) throw Error('页面已变化');
          this.setData({ queue: this.data.queue.filter(p => p.key !== f.key) });
        }
        await this.load();
        if (rev === this._rev) this.triggerEvent('changed');
      } catch (e) {
        if (rev !== this._rev) throw e;
        const message = e instanceof ApiError ? e.message : '照片上传失败：未收到具体原因，请重试';
        this.setData({ error: message });
        throw Error(message);
      } finally { if (rev === this._rev) this.setData({ busy: false }); }
    },
    async retry() {
      const rev = this._rev;
      try { await this.saveTo(this.properties.objectId); }
      catch { /* saveTo shows the reason inline. */ }
      if (rev === this._rev && this._alive && !this._hidden) this.triggerEvent('retryresult', { error: this.data.error });
    },
 async remove(e){const {kind,objectId}=this.properties;if(this.data.busy||!this.properties.editable)return;const n=this._rev;this.setData({busy:true,error:''});try{await getApp().client.delete(path(kind,objectId,e.currentTarget.dataset.id));if(n===this._rev){await this.load();this.triggerEvent('changed');}}catch(e){if(n===this._rev)this.setData({error:e.message});}finally{if(n===this._rev)this.setData({busy:false});}}}});
