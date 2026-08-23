// ============================================================
//  Wagba frontend - common helpers (no build step, plain JS)
// ============================================================

const DEFAULT_API = 'http://localhost:8082/api/v1';

function getApiBase() { return localStorage.getItem('wagba_api') || DEFAULT_API; }
function setApiBase(v) { localStorage.setItem('wagba_api', (v || '').trim()); }

function applyTheme() {
  const t = localStorage.getItem('wagba_theme') || 'light';
  document.documentElement.dataset.theme = t;
}
applyTheme();
function toggleTheme() {
  const t = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
  document.documentElement.dataset.theme = t;
  localStorage.setItem('wagba_theme', t);
  const icon = document.getElementById('themeBtn');
  if (icon) icon.querySelector('i').className = 'bi bi-' + (t === 'dark' ? 'sun' : 'moon-stars');
}

// ---------- auth storage ----------
function getToken() { return localStorage.getItem('wagba_token'); }
function setToken(t) { localStorage.setItem('wagba_token', t); }
function getUser() { const u = localStorage.getItem('wagba_user'); return u ? JSON.parse(u) : null; }
function setUser(u) { localStorage.setItem('wagba_user', JSON.stringify(u)); }
function clearAuth() { localStorage.removeItem('wagba_token'); localStorage.removeItem('wagba_user'); }

// ---------- JWT decode ----------
function decodeJwt(token) {
  try {
    let p = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(escape(window.atob(p)));
    return JSON.parse(json);
  } catch (e) { return null; }
}

// ---------- fetch helper ----------
async function doFetch(url, method, headers, body) {
  const res = await fetch(url, { method, headers, body: body !== null ? JSON.stringify(body) : undefined });
  const text = await res.text();
  let data = null;
  if (text) { try { data = JSON.parse(text); } catch (e) { data = text; } }
  if (res.status === 401) {
    clearAuth();
    if (!location.pathname.endsWith('index.html') && location.pathname !== '/') location.href = 'index.html';
    throw new Error('Session expired. Please login again.');
  }
  if (!res.ok) {
    let msg = 'Request failed';
    if (data && data.message) msg = data.message;
    else if (data && data.error) msg = data.error;
    else if (typeof data === 'string' && data) msg = data;
    const err = new Error(msg); err.status = res.status; throw err;
  }
  return data;
}

async function api(path, method = 'GET', body = null, auth = true) {
  const headers = {};
  if (body !== null) headers['Content-Type'] = 'application/json';
  if (auth) { const t = getToken(); if (t) headers['Authorization'] = 'Bearer ' + t; }
  return doFetch(getApiBase() + path, method, headers, body);
}

// ---------- navigation helpers ----------
function dashFor(role) {
  if (role === 'CUSTOMER') return 'customer.html';
  if (role === 'RESTAURANT_OWNER') return 'owner.html';
  if (role === 'DRIVER') return 'driver.html';
  if (role === 'ADMIN') return 'admin.html';
  return 'index.html';
}

function afterAuth(token, redirect = true) {
  setToken(token);
  const dec = decodeJwt(token) || {};
  return api('/auth/me').then(me => {
    setUser(me);
    if (redirect) location.href = dashFor(me.role);
    return me;
  }).catch(() => {
    setUser({ email: dec.sub, role: dec.role });
    if (redirect) location.href = dashFor(dec.role);
    return getUser();
  });
}

// ---------- page boot ----------
function boot(role, cb) {
  const token = getToken();
  if (!token) { location.href = 'index.html'; return; }
  const u = getUser();
  if (u && u.role === role) { renderNav(u); cb(u); connectRealtime(); return; }
  api('/auth/me').then(me => {
    setUser(me);
    if (me.role !== role) { location.href = dashFor(me.role); return; }
    renderNav(me); cb(me); connectRealtime();
  }).catch(() => { location.href = 'index.html'; });
}

// ---------- navbar ----------
function initials(name) {
  return (name || '?').split(' ').map(s => s[0]).join('').slice(0, 2).toUpperCase();
}

function renderNav(me) {
  const nav = document.getElementById('nav');
  if (!nav) return;
  const isDark = document.documentElement.dataset.theme === 'dark';
  nav.className = 'wagba-nav';
  nav.innerHTML = `
    <a class="brand" href="${dashFor(me.role)}">
      <span class="logo"><i class="bi bi-egg-fried"></i></span> Wagba
    </a>
    <div class="spacer"></div>
    <div class="chip">
      <span class="avatar">${escapeHtml(initials(me.name))}</span>
      <span class="text-dark fw-medium">${escapeHtml(me.name || me.email)}</span>
      <span class="badge role-pill">${me.role}</span>
    </div>
    <button id="notifBtn" class="icon-btn position-relative" onclick="toggleNotifPanel(event)" title="Notifications">
      <i class="bi bi-bell"></i>
      <span id="notifBadge" class="notif-badge d-none">0</span>
    </button>
    <div id="notifPanel" class="notif-panel d-none">
      <div class="notif-head"><span>Notifications</span><button class="btn btn-link btn-sm p-0" onclick="markAllRead()">Mark all read</button></div>
      <div id="notifList" class="notif-list"></div>
    </div>
    <button id="themeBtn" class="icon-btn" onclick="toggleTheme()" title="Toggle theme">
      <i class="bi bi-${isDark ? 'sun' : 'moon-stars'}"></i>
    </button>
    <div class="api-box">
      <i class="bi bi-hdd-network text-muted"></i>
      <input id="apiInput" class="form-control form-control-sm" value="${getApiBase()}">
    </div>
    <button class="btn btn-soft btn-sm" onclick="doLogout()"><i class="bi bi-box-arrow-right"></i> Logout</button>`;
  const inp = document.getElementById('apiInput');
  inp.addEventListener('change', () => setApiBase(inp.value));
  if (!window.__notifOutsideBound) {
    document.addEventListener('click', (e) => {
      const panel = document.getElementById('notifPanel');
      if (!panel || panel.classList.contains('d-none')) return;
      const btn = e.target.closest && e.target.closest('#notifBtn');
      if (!btn && !panel.contains(e.target)) panel.classList.add('d-none');
    });
    window.__notifOutsideBound = true;
  }
  refreshNotifications();
}

function doLogout() {
  api('/auth/logout', 'POST', null, true).catch(() => {}).finally(() => {
    clearAuth(); location.href = 'index.html';
  });
}

// ---------- notifications ----------
async function refreshNotifications() {
  try {
    const d = await api('/notifications?page=0&size=20');
    const items = d.content || [];
    const unread = typeof d.unreadCount === 'number' ? d.unreadCount : items.filter(n => !n.read).length;
    const badge = document.getElementById('notifBadge');
    if (badge) { badge.textContent = unread; badge.classList.toggle('d-none', unread === 0); }
    const listEl = document.getElementById('notifList');
    if (listEl) {
      listEl.innerHTML = items.length ? items.map(n => `
        <div class="notif-item ${n.read ? '' : 'unread'}" onclick="markRead(${n.id})">
          <div class="notif-title">${escapeHtml(n.title || '')}</div>
          <div class="notif-msg small muted">${escapeHtml(n.message || '')}</div>
        </div>`).join('') : '<div class="p-3 small muted">No notifications</div>';
    }
  } catch (e) {}
}
function toggleNotifPanel(e) { if (e && e.stopPropagation) e.stopPropagation(); const p = document.getElementById('notifPanel'); if (p) p.classList.toggle('d-none'); }
async function markRead(id) { try { await api('/notifications/' + id + '/read', 'POST'); await refreshNotifications(); } catch (e) {} }
async function markAllRead() { try { await api('/notifications/read-all', 'POST'); await refreshNotifications(); } catch (e) {} }

// ---------- sidebar navigation (role dashboards) ----------
function navTo(view) {
  document.querySelectorAll('.view').forEach(v => v.classList.add('d-none'));
  const el = document.getElementById('view-' + view);
  if (el) el.classList.remove('d-none');
  document.querySelectorAll('.side-link').forEach(l => l.classList.toggle('active', l.dataset.view === view));
  const t = window.VIEWS && window.VIEWS[view];
  if (t) {
    const pt = document.getElementById('pageTitle'); if (pt) pt.textContent = t.title || '';
    const ps = document.getElementById('pageSub'); if (ps) ps.textContent = t.sub || '';
  }
  if (window.onNav) { try { window.onNav(view); } catch (e) {} }
}

// ---------- toast ----------
function toast(title, msg, type = '') {
  let wrap = document.querySelector('.toast-wrap');
  if (!wrap) { wrap = document.createElement('div'); wrap.className = 'toast-wrap'; document.body.appendChild(wrap); }
  const el = document.createElement('div');
  el.className = 'toast-wagba ' + (type === 'ok' ? 'ok' : type === 'err' ? 'err' : '');
  el.innerHTML = `<div class="t-title">${escapeHtml(title)}</div><div class="t-msg">${escapeHtml(msg || '')}</div>`;
  wrap.appendChild(el);
  setTimeout(() => { el.style.opacity = '0'; el.style.transform = 'translateX(20px)'; setTimeout(() => el.remove(), 300); }, 3200);
}

// ---------- UI utils ----------
function showAlert(containerId, type, msg) {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.innerHTML = `<div class="alert alert-${type} alert-dismissible fade show" role="alert">
    ${escapeHtml(msg)}
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>`;
}
function clearAlert(containerId) {
  const el = document.getElementById(containerId); if (el) el.innerHTML = '';
}
function escapeHtml(s) {
  if (s === null || s === undefined) return '';
  return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function money(v) {
  if (v === null || v === undefined) return '-';
  return Number(v).toFixed(2) + ' EGP';
}
function statusBadge(status) {
  const map = {
    PENDING: 'warning', ACCEPTED: 'info', PREPARING: 'info', READY: 'primary',
    OUT_FOR_DELIVERY: 'primary', DELIVERED: 'success', CANCELLED: 'secondary', REJECTED: 'danger',
    AVAILABLE: 'secondary', PICKED_UP: 'info', ACTIVE: 'success', INACTIVE: 'secondary',
    PENDING_APPROVAL: 'warning', APPROVED: 'success', COMPLETED: 'success'
  };
  const cls = map[status] || 'light';
  return `<span class="badge bg-${cls}">${escapeHtml(status)}</span>`;
}
function stars(n) {
  n = parseInt(n) || 0;
  let s = '';
  for (let i = 1; i <= 5; i++) s += i <= n ? '★' : '☆';
  return s;
}
function onImgError(el, label) {
  el.onerror = null;
  el.style.display = 'none';
  const p = el.parentElement;
  if (p && !p.querySelector('.img-fallback')) {
    const d = document.createElement('div');
    d.className = 'img-fallback';
    d.innerHTML = '<i class="bi bi-image"></i>' + (label ? '<span>' + escapeHtml(label) + '</span>' : '');
    p.insertBefore(d, el);
  }
}
function fmtDateTime(s) {
  if (!s) return '-';
  const d = new Date(s);
  if (isNaN(d)) return escapeHtml(s);
  return d.toLocaleString();
}
function fmtTimeAgo(s) {
  if (!s) return '';
  const d = new Date(s);
  if (isNaN(d)) return '';
  const sec = Math.floor((Date.now() - d.getTime()) / 1000);
  if (sec < 60) return sec + 's ago';
  const min = Math.floor(sec / 60);
  if (min < 60) return min + 'm ago';
  const hr = Math.floor(min / 60);
  return hr + 'h ago';
}
function formatAddr(a) {
  if (!a) return '';
  return [a.street, a.area, a.city, a.building, a.apartment].filter(Boolean).map(String).join(', ');
}

// ============================================================
//  Real-time (WebSocket / STOMP over SockJS)
// ============================================================
let __rtClient = null;
function loadScriptOnce(src) {
  return new Promise((resolve, reject) => {
    if (document.querySelector('script[src="' + src + '"]')) return resolve();
    const s = document.createElement('script');
    s.src = src; s.onload = () => resolve(); s.onerror = () => reject();
    document.head.appendChild(s);
  });
}
function wsBase() { return getApiBase().replace(/\/api\/v1\/?$/, ''); }
async function connectRealtime() {
  const token = getToken();
  if (!token || __rtClient) return;
  try {
    await loadScriptOnce('https://cdn.jsdelivr.net/npm/sockjs-client@1.5.0/dist/sockjs.min.js');
    await loadScriptOnce('https://cdn.jsdelivr.net/npm/@stomp/stompjs@6.1.2/bundle-browser.min.js');
  } catch (e) { return; }
  try {
    const socket = new SockJS(wsBase() + '/ws?token=' + encodeURIComponent(token));
    const client = Stomp.over(socket);
    client.debug = () => {};
    client.reconnectDelay = 5000;
    client.heartbeatOutgoing = 20000;
    client.heartbeatIncoming = 20000;
    client.connect({ token: token }, () => {
      client.subscribe('/user/queue/notifications', (msg) => {
        try { handleRealtime(JSON.parse(msg.body)); } catch (e) {}
      });
      const u = getUser();
      if (u && u.role === 'DRIVER') {
        client.subscribe('/topic/driver/available', () => {
          if (window.__realtimeRefresh) window.__realtimeRefresh({ type: 'AVAILABLE' });
        });
      }
    }, () => { /* connection error -> will retry via reconnectDelay */ });
    __rtClient = client;
  } catch (e) { /* ignore */ }
}
function handleRealtime(p) {
  if (!p) return;
  if (p.title) toast(p.title, p.message, (p.type === 'ORDER_REJECTED' || p.type === 'ORDER_CANCELLED') ? 'err' : 'ok');
  if (window.__realtimeRefresh) { try { window.__realtimeRefresh(p); } catch (e) {} }
  refreshNotifications();
}
