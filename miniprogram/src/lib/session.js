function createSession({ read, write, me }) {
  let initial = null;
  try { initial = read() || null; } catch (_) { /* Browsing must work when device storage is unavailable. */ }
  let sequence = 0;
  let pending = null;
  const listeners = new Set();
  const state = {
    token: initial, user: null, error: '',
    get authenticated() { return !!(this.token && this.user); },
    subscribe(listener) { listeners.add(listener); return () => listeners.delete(listener); },
    notify() { listeners.forEach(listener => listener(this)); },
    login(token, user) {
      sequence++; pending = null;
      this.token = token; this.user = user; this.error = '';
      try { write(token); } catch (_) { /* A verified in-memory session remains usable. */ }
      this.notify();
    },
    logout() {
      sequence++; pending = null;
      this.token = null; this.user = null; this.error = '';
      try { write(null); } catch (_) { /* Runtime state must still be cleared. */ }
      this.notify();
    },
    invalidate(sentToken) { if (sentToken && sentToken === this.token) this.logout(); },
    restore(force = false) {
      if (!this.token) return Promise.resolve(null);
      if (pending) return pending;
      if (this.user && !force) return Promise.resolve(this.user);
      const token = this.token; const requestSequence = ++sequence;
      let request;
      try { request = me(); } catch (error) { request = Promise.reject(error); }
      const loading = Promise.resolve(request).then(user => {
        if (token !== this.token || requestSequence !== sequence) return null;
        this.user = user; this.error = ''; this.notify(); return user;
      }).catch(error => {
        if (token !== this.token || requestSequence !== sequence) return null;
        if (error.status === 401) this.logout();
        else { this.user = null; this.error = error.message || '暂时无法验证登录，请重试'; this.notify(); }
        return null;
      }).finally(() => { if (pending === loading) pending = null; });
      pending = loading;
      return loading;
    },
  };
  return state;
}
module.exports = { createSession };
