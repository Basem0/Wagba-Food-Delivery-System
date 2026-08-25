// ============================================================
//  Wagba frontend - common helpers (no build step, plain JS)
// ============================================================

// Must match server.port in the backend's application.properties.
const DEFAULT_API = 'http://localhost:8081/api/v1';

function getApiBase() { return localStorage.getItem('wagba_api') || DEFAULT_API; }
function setApiBase(v) {
  const val = (v || '').trim();
  if (val) localStorage.setItem('wagba_api', val);
  else localStorage.removeItem('wagba_api');
}

// ---------- auth storage ----------
function getToken() { return localStorage.getItem('wagba_token'); }
function setToken(t) { localStorage.setItem('wagba_token', t); }
function getUser() {
  const u = localStorage.getItem('wagba_user');
  if (!u) return null;
  // Corrupt storage used to throw here and break every page that called getUser().
  try { return JSON.parse(u); } catch (e) { localStorage.removeItem('wagba_user'); return null; }
}
function setUser(u) { localStorage.setItem('wagba_user', JSON.stringify(u)); }
function clearAuth() { localStorage.removeItem('wagba_token'); localStorage.removeItem('wagba_user'); }

// ---------- JWT decode ----------
function decodeJwt(token) {
  try {
    let p = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    while (p.length % 4) p += '=';
    const json = decodeURIComponent(escape(window.atob(p)));
    return JSON.parse(json);
  } catch (e) { return null; }
}

// ---------- fetch helper ----------
async function doFetch(url, method, headers, body) {
  let res;
  try {
    res = await fetch(url, { method, headers, body: body !== null ? JSON.stringify(body) : undefined });
  } catch (e) {
    // fetch only rejects on a network-level failure, which almost always means the
    // API base is wrong or the server is down - say so instead of "Failed to fetch".
    const err = new Error('Cannot reach the server at ' + getApiBase() + '. Is the backend running?');
    err.status = 0;
    throw err;
  }
  const text = await res.text();
  let data = null;
  if (text) { try { data = JSON.parse(text); } catch (e) { data = text; } }
  if (res.status === 401) {
    clearAuth();
    if (!isPublicPage()) location.href = 'index.html';
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

function isPublicPage() {
  const p = location.pathname;
  return p === '/' || p.endsWith('index.html') || p.endsWith('/');
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

/**
 * Stores the session and lands the user on their dashboard.
 * Accepts either the AuthResponse object returned by POST /auth/login or a bare
 * token string, so older call sites keep working.
 */
function afterAuth(auth, redirect = true) {
  const token = typeof auth === 'string' ? auth : (auth && auth.token);
  if (!token) return Promise.reject(new Error('Login did not return a token'));
  setToken(token);
  const dec = decodeJwt(token) || {};
  const fallback = (typeof auth === 'object' && auth) ? auth : { email: dec.sub, role: dec.role };
  return api('/auth/me').then(me => {
    setUser(me);
    if (redirect) location.href = dashFor(me.role);
    return me;
  }).catch(() => {
    setUser(fallback);
    if (redirect) location.href = dashFor(fallback.role);
    return getUser();
  });
}

// ---------- page boot ----------
function boot(role, cb) {
  const token = getToken();
  if (!token) { location.href = 'index.html'; return; }
  if (!document.querySelector('.sidebar-backdrop')) {
    const bd = document.createElement('div');
    bd.className = 'sidebar-backdrop';
    bd.addEventListener('click', () => toggleSidebar(false));
    document.body.appendChild(bd);
  }
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
  // filter(Boolean) guards against double spaces / trailing spaces, which used to
  // produce "undefined" here.
  const parts = String(name || '').trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return '?';
  return parts.map(s => s[0]).join('').slice(0, 2).toUpperCase();
}

function renderNav(me) {
  const nav = document.getElementById('nav');
  if (!nav) return;
  const isCust = me.role === 'CUSTOMER';
  nav.className = 'wagba-nav';
  nav.innerHTML = `
    <button class="menu-toggle" onclick="toggleSidebar()" aria-label="Menu"><i class="bi bi-list"></i></button>
    <div class="spacer"></div>
    <button id="notifBtn" class="icon-btn position-relative" onclick="toggleNotifPanel(event)" title="Notifications">
      <i class="bi bi-bell"></i>
      <span id="notifBadge" class="notif-badge d-none">0</span>
    </button>
    ${isCust ? `<button class="icon-btn nav-cart" id="navCartBtn" onclick="navTo('cart')" title="Cart"><i class="bi bi-cart3"></i><span class="side-badge d-none" id="navCartCount">0</span></button>` : ''}
    <div id="notifPanel" class="notif-panel d-none">
      <div class="notif-head"><span>Notifications</span><button class="btn btn-link btn-sm p-0" onclick="markAllRead()">Mark all read</button></div>
      <div id="notifList" class="notif-list"></div>
    </div>
    <div class="chip" onclick="navTo('settings')" title="Settings" role="button" tabindex="0">
      <span class="avatar">${escapeHtml(initials(me.name))}</span>
      <span class="chip-name fw-medium">${escapeHtml(me.name || me.email)}</span>
      <span class="badge role-pill">${escapeHtml(roleLabel(me.role))}</span>
    </div>`;
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

function roleLabel(role) {
  switch (role) {
    case 'RESTAURANT_OWNER': return 'Owner';
    case 'CUSTOMER': return 'Customer';
    case 'DRIVER': return 'Driver';
    case 'ADMIN': return 'Admin';
    default: return role || '';
  }
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
      listEl.innerHTML = items.length ? items.map(n => {
        const m = notifMeta(n.type);
        const t = n.createdAt ? fmtTimeAgo(n.createdAt) : '';
        return `<div class="notif-item ${n.read ? '' : 'unread'}" onclick="markRead(${n.id})">
          <div class="notif-ic ${m.cls}"><i class="bi ${m.icon}"></i></div>
          <div class="notif-body">
            <div class="notif-title">${escapeHtml(n.title || '')}</div>
            <div class="notif-msg">${escapeHtml(n.message || '')}</div>
            ${t ? `<div class="notif-time">${t}</div>` : ''}
          </div>
          ${n.read ? '' : '<span class="notif-dot"></span>'}
        </div>`;
      }).join('') : '<div class="empty-state"><div class="ico"><i class="bi bi-bell"></i></div><div class="et">No notifications</div><div class="es">You\'re all caught up.</div></div>';
    }
  } catch (e) {}
}
function toggleNotifPanel(e) { if (e && e.stopPropagation) e.stopPropagation(); const p = document.getElementById('notifPanel'); if (p) p.classList.toggle('d-none'); }
function notifMeta(type) {
  switch (type) {
    case 'ORDER_CANCELLED':
    case 'ORDER_REJECTED': return { icon: 'bi-x-circle', cls: 'danger' };
    case 'NEW_ORDER':
    case 'NEW_DELIVERY':
    case 'AVAILABLE': return { icon: 'bi-bag-check', cls: 'ok' };
    case 'ORDER_ACCEPTED':
    case 'ORDER_PREPARING': return { icon: 'bi-fire', cls: 'info' };
    case 'ORDER_READY': return { icon: 'bi-bag-check', cls: 'info' };
    case 'ORDER_PICKED_UP':
    case 'ORDER_OUT_FOR_DELIVERY': return { icon: 'bi-scooter', cls: 'info' };
    case 'ORDER_DELIVERED': return { icon: 'bi-check-circle', cls: 'ok' };
    case 'ORDER_PAID':
    case 'PAYOUT': return { icon: 'bi-cash-coin', cls: 'ok' };
    case 'COUPON': return { icon: 'bi-ticket-perforated', cls: 'info' };
    default: return { icon: 'bi-bell', cls: '' };
  }
}
async function markRead(id) { try { await api('/notifications/' + id + '/read', 'POST'); await refreshNotifications(); } catch (e) {} }
async function markAllRead() { try { await api('/notifications/read-all', 'POST'); await refreshNotifications(); } catch (e) {} }

// ---------- sidebar navigation (role dashboards) ----------
function navTo(view) {
  document.querySelectorAll('.view').forEach(v => v.classList.add('d-none'));
  const el = document.getElementById('view-' + view);
  if (el) { el.classList.remove('d-none'); el.classList.add('view-enter'); }
  document.querySelectorAll('.side-link').forEach(l => l.classList.toggle('active', l.dataset.view === view));
  const t = window.VIEWS && window.VIEWS[view];
  if (t) {
    const pt = document.getElementById('pageTitle'); if (pt) pt.textContent = t.title || '';
    const ps = document.getElementById('pageSub'); if (ps) ps.textContent = t.sub || '';
  }
  toggleSidebar(false);
  if (window.onNav) { try { window.onNav(view); } catch (e) {} }
}
function toggleSidebar(force) {
  const sb = document.querySelector('.sidebar');
  const bd = document.querySelector('.sidebar-backdrop');
  if (!sb) return;
  const open = force === undefined ? !sb.classList.contains('open') : force;
  sb.classList.toggle('open', open);
  if (bd) bd.classList.toggle('show', open);
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
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-';
  return Number(v).toFixed(2) + ' EGP';
}
function statusBadge(status) {
  const map = {
    PENDING: 'warning', ACCEPTED: 'info', PREPARING: 'info', READY: 'primary',
    OUT_FOR_DELIVERY: 'primary', DELIVERED: 'success', CANCELLED: 'secondary', REJECTED: 'danger',
    AVAILABLE: 'secondary', PICKED_UP: 'info', ACTIVE: 'success', INACTIVE: 'secondary',
    PENDING_APPROVAL: 'warning', APPROVED: 'success', COMPLETED: 'success',
    SUSPENDED: 'danger', PAID: 'success', UNPAID: 'warning', NONE: 'light'
  };
  const cls = map[status] || 'light';
  return `<span class="badge bg-${cls}">${escapeHtml(prettyStatus(status))}</span>`;
}
/** OUT_FOR_DELIVERY -> "Out for delivery" - enum names read badly in a UI. */
function prettyStatus(s) {
  if (!s) return '';
  const t = String(s).replace(/_/g, ' ').toLowerCase();
  return t.charAt(0).toUpperCase() + t.slice(1);
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
  if (label === undefined) label = (el.dataset && el.dataset.label) || '';
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
  if (sec < 5) return 'just now';
  if (sec < 60) return sec + 's ago';
  const min = Math.floor(sec / 60);
  if (min < 60) return min + 'm ago';
  const hr = Math.floor(min / 60);
  if (hr < 24) return hr + 'h ago';
  const day = Math.floor(hr / 24);
  return day + 'd ago';
}
/** Field names must match AddressResponse (buildingNumber/details, not building/area). */
function formatAddr(a) {
  if (!a) return '';
  const line = [a.street, a.buildingNumber && ('Bldg ' + a.buildingNumber),
                a.apartment && ('Apt ' + a.apartment), a.city]
    .filter(Boolean).map(String).join(', ');
  return a.details ? line + (line ? ' - ' : '') + a.details : line;
}

// ---------- Settings (shared across roles) ----------
function renderSettingsShell(extraHtml) {
  return `
  <div class="dash-hero"><i class="bi bi-gear"></i><div><h4 class="mb-0">Settings</h4><div class="hero-sub">Manage your account and preferences</div></div></div>
  <div class="row g-3">
    <div class="col-lg-6">
      <div class="card"><div class="card-body">
        <div class="section-head mb-3"><span class="sh-ic"><i class="bi bi-person"></i></span><div><h5 class="mb-0">Account</h5></div></div>
        <div id="acctAlert"></div>
        <div class="mb-2"><label class="form-label">Full name</label><input id="acctName" class="form-control" autocomplete="name"></div>
        <div class="mb-2"><label class="form-label">Email</label><input id="acctEmail" class="form-control" disabled></div>
        <div class="mb-2"><label class="form-label">Phone</label><input id="acctPhone" class="form-control" autocomplete="tel" placeholder="01xxxxxxxxx"></div>
        <button class="btn-brand" onclick="saveAccountSettings()">Save changes</button>
      </div></div>
    </div>
    <div class="col-lg-6">
      <div class="card"><div class="card-body">
        <div class="section-head mb-3"><span class="sh-ic"><i class="bi bi-shield-lock"></i></span><div><h5 class="mb-0">Security</h5></div></div>
        <div id="pwAlert"></div>
        <div class="mb-2"><label class="form-label">Current password</label><input id="pwCurrent" type="password" class="form-control" autocomplete="current-password"></div>
        <div class="mb-2"><label class="form-label">New password</label><input id="pwNew" type="password" class="form-control" autocomplete="new-password"><div class="form-hint">At least 8 characters.</div></div>
        <div class="mb-2"><label class="form-label">Confirm new password</label><input id="pwConfirm" type="password" class="form-control" autocomplete="new-password"></div>
        <button class="btn-brand" onclick="saveChangePassword()">Update password</button>
      </div></div>
    </div>
    ${extraHtml || ''}
    <div class="col-lg-6">
      <div class="card"><div class="card-body">
        <div class="section-head mb-3"><span class="sh-ic"><i class="bi bi-hdd-network"></i></span><div><h5 class="mb-0">Advanced</h5><div class="muted small">Only change this if you know what it is</div></div></div>
        <label class="form-label">API base URL</label>
        <div class="d-flex gap-2">
          <input id="apiInput" class="form-control" value="${escapeHtml(getApiBase())}" spellcheck="false">
          <button class="btn btn-soft" onclick="saveApiBase()">Save</button>
        </div>
        <div class="form-hint">Default: ${escapeHtml(DEFAULT_API)}</div>
      </div></div>
    </div>
  </div>
  <div class="row g-3 mt-1">
    <div class="col-12">
      <div class="card"><div class="card-body d-flex justify-between align-center flex-wrap gap-2">
        <div><div class="fw-semibold">Sign out</div><div class="muted small">You'll be returned to the login screen</div></div>
        <button class="btn btn-soft btn-danger-soft" onclick="doLogout()"><i class="bi bi-box-arrow-right"></i> Logout</button>
      </div></div>
    </div>
  </div>`;
}
function saveApiBase() {
  const el = document.getElementById('apiInput');
  if (!el) return;
  let v = el.value.trim().replace(/\/+$/, '');
  if (v && !/^https?:\/\//i.test(v)) { toast('Invalid URL', 'Start with http:// or https://', 'err'); return; }
  setApiBase(v);
  el.value = getApiBase();
  toast('Saved', 'API base set to ' + getApiBase());
}
async function loadAccountSettings() {
  try {
    const u = await api('/auth/me');
    const n = document.getElementById('acctName'); if (n) n.value = u.name || '';
    const e = document.getElementById('acctEmail'); if (e) e.value = u.email || '';
    const p = document.getElementById('acctPhone'); if (p) p.value = u.phone || '';
  } catch (err) { showAlert('acctAlert', 'danger', err.message); }
}
async function saveAccountSettings() {
  clearAlert('acctAlert');
  const name = document.getElementById('acctName').value.trim();
  const phone = document.getElementById('acctPhone').value.trim();
  if (!name) { showAlert('acctAlert', 'warning', 'Name cannot be empty'); return; }
  try {
    const u = await api('/auth/me', 'PUT', { name, phone });
    // Merge so role/status survive - u may not carry every field.
    const cur = Object.assign({}, getUser() || {}, u);
    setUser(cur);
    renderNav(cur);
    toast('Saved', 'Account updated');
  } catch (err) { showAlert('acctAlert', 'danger', err.message); }
}
async function saveChangePassword() {
  clearAlert('pwAlert');
  const cur = document.getElementById('pwCurrent').value;
  const nw = document.getElementById('pwNew').value;
  const cf = document.getElementById('pwConfirm').value;
  if (!cur || !nw) { showAlert('pwAlert', 'warning', 'Please fill all fields'); return; }
  if (nw !== cf) { showAlert('pwAlert', 'warning', 'New passwords do not match'); return; }
  // Must match the backend's @Size(min = 8) on ChangePasswordRequest.
  if (nw.length < 8) { showAlert('pwAlert', 'warning', 'Password must be at least 8 characters'); return; }
  if (nw === cur) { showAlert('pwAlert', 'warning', 'New password must be different'); return; }
  try {
    await api('/auth/change-password', 'POST', { currentPassword: cur, newPassword: nw });
    document.getElementById('pwCurrent').value = '';
    document.getElementById('pwNew').value = '';
    document.getElementById('pwConfirm').value = '';
    toast('Updated', 'Password changed');
  } catch (err) { showAlert('pwAlert', 'danger', err.message); }
}

// ---------- Loading & empty states ----------
function showSkeletons(containerId, count, height) {
  const el = document.getElementById(containerId);
  if (!el) return;
  count = count || 3;
  height = height || 200;
  let html = '';
  for (let i = 0; i < count; i++) html += `<div class="skeleton" style="height:${height}px;border-radius:var(--radius)"></div>`;
  el.innerHTML = html;
}
function skeletonRows(containerId, cols, rows) {
  const el = document.getElementById(containerId);
  if (!el) return;
  rows = rows || 5;
  let html = '';
  for (let r = 0; r < rows; r++) {
    html += '<tr>';
    for (let c = 0; c < cols; c++) html += '<td><div class="skeleton" style="height:14px"></div></td>';
    html += '</tr>';
  }
  el.innerHTML = html;
}
function emptyState(icon, title, sub, actionHtml) {
  return `<div class="empty-state"><div class="ico"><i class="bi ${icon}"></i></div><div class="et">${escapeHtml(title)}</div><div class="es">${escapeHtml(sub || '')}</div>${actionHtml || ''}</div>`;
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
