// ============================================================
//  Wagba frontend - common helpers (no build step, plain JS)
// ============================================================

// Single, fixed backend origin. The server runs on 8082.
const DEFAULT_API = 'http://localhost:8082/api/v1';

// The API base is now fixed (unified) and can no longer be changed from the UI.
function getApiBase() { return DEFAULT_API; }

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
  if (u && u.role === role) { renderNav(u); renderBottomNav(u); cb(u); connectRealtime(); return; }
  api('/auth/me').then(me => {
    setUser(me);
    if (me.role !== role) { location.href = dashFor(me.role); return; }
    renderNav(me); renderBottomNav(me); cb(me); connectRealtime();
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
    <a class="nav-brand" onclick="navTo(homeView('${me.role}'))" role="button" title="Home" aria-label="Home">
      <span class="logo"><i class="bi bi-egg-fried"></i></span> <span class="brand-name">Wagba</span>
    </a>
    <div class="spacer"></div>
    <button id="notifBtn" class="icon-btn position-relative" onclick="toggleNotifPanel(event)" title="Notifications">
      <i class="bi bi-bell"></i>
      <span id="notifBadge" class="notif-badge d-none">0</span>
    </button>
    ${isCust ? `<button class="icon-btn nav-cart" id="navCartBtn" onclick="navTo('cart')" title="Cart"><i class="bi bi-cart3"></i><span class="side-badge d-none" id="navCartCount">0</span></button>` : ''}
    <div id="notifPanel" class="notif-panel d-none">
      <div class="notif-head"><span>Notifications</span><button id="notifClearBtn" class="btn-notif-clear" onclick="markAllRead()"><i class="bi bi-check2-all"></i> Mark all read</button></div>
      <div id="notifList" class="notif-list"></div>
    </div>
    <div class="chip" onclick="navTo('settings')" title="Settings" role="button" tabindex="0">
      <span class="avatar">${escapeHtml(initials(me.name))}</span>
      <span class="chip-name fw-medium">${escapeHtml(me.name || me.email)}</span>
      <span class="badge role-pill">${escapeHtml(roleLabel(me.role))}</span>
    </div>`;
  renderSideUser(me);
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

function renderSideUser(me) {
  const el = document.getElementById('sideUser');
  if (!el || !me) return;
  const roleColors = { CUSTOMER: 'var(--info)', RESTAURANT_OWNER: 'var(--brand)', DRIVER: 'var(--ok)', ADMIN: 'var(--violet)' };
  el.innerHTML = `
    <div class="su-avatar" style="background:${roleColors[me.role] || 'var(--gradient)'}">${escapeHtml(initials(me.name))}</div>
    <div class="su-info">
      <div class="su-name">${escapeHtml(me.name || 'User')}</div>
      <div class="su-email">${escapeHtml(me.email || '')}</div>
    </div>
    <span class="su-role">${escapeHtml(roleLabel(me.role))}</span>`;
  el.onclick = () => navTo('settings');
}

// ---------- mobile bottom navigation ----------
// Flat, primary destinations per role. The "User" item maps to the settings
// view (the account chip that lives in the top bar on desktop).
const BOTTOM_NAV = {
  CUSTOMER: [
    { view: 'restaurants', label: 'Restaurants', icon: 'bi-shop' },
    { view: 'cart', label: 'Cart', icon: 'bi-cart3', badge: 'cartCount' },
    { view: 'orders', label: 'Orders', icon: 'bi-receipt' },
    { view: 'favorites', label: 'Favorites', icon: 'bi-heart' },
    { view: 'coupons', label: 'Coupons', icon: 'bi-ticket-perforated' },
    { view: 'settings', label: 'User', icon: 'bi-person-circle' },
  ],
  RESTAURANT_OWNER: [
    { view: 'dashboard', label: 'Dashboard', icon: 'bi-grid-1x2' },
    { view: 'menu', label: 'Menu', icon: 'bi-menu-button' },
    { view: 'orders', label: 'Orders', icon: 'bi-receipt' },
    { view: 'earnings', label: 'Earnings', icon: 'bi-wallet2' },
    { view: 'settings', label: 'User', icon: 'bi-person-circle' },
  ],
  DRIVER: [
    { view: 'dashboard', label: 'Dashboard', icon: 'bi-grid-1x2' },
    { view: 'available', label: 'Availables', icon: 'bi-inboxes' },
    { view: 'mine', label: 'Delivers', icon: 'bi-bag-check' },
    { view: 'earnings', label: 'Earnings', icon: 'bi-wallet2' },
    { view: 'settings', label: 'User', icon: 'bi-person-circle' },
  ],
  ADMIN: [
    { view: 'overview', label: 'Overview', icon: 'bi-grid-1x2' },
    { view: 'restaurants', label: 'Restaurants', icon: 'bi-shop' },
    { view: 'drivers', label: 'Drivers', icon: 'bi-bicycle' },
    { view: 'users', label: 'Users', icon: 'bi-people' },
    { view: 'coupons', label: 'Coupons', icon: 'bi-ticket-perforated' },
    { view: 'settings', label: 'User', icon: 'bi-person-circle' },
  ],
};
function renderBottomNav(me) {
  const host = document.getElementById('bottomNav');
  if (!host) return;
  const items = BOTTOM_NAV[me.role] || [];
  host.innerHTML = items.map(it => `
    <button class="bottom-link" data-view="${it.view}" onclick="navTo('${it.view}')" aria-label="${it.label}">
      <i class="bi ${it.icon}"></i>
      <span class="bl-label">${it.label}</span>
      ${it.badge ? `<span id="${it.badge}Bottom" class="bl-badge d-none">0</span>` : ''}
    </button>`).join('');
  let active = 'restaurants';
  const vis = document.querySelector('.view:not(.d-none)');
  if (vis && vis.id && vis.id.indexOf('view-') === 0) active = vis.id.slice(5);
  setBottomActive(active);
  syncBottomBadge('cartCount');
}
function setBottomActive(view) {
  document.querySelectorAll('.bottom-link').forEach(l => l.classList.toggle('active', l.dataset.view === view));
}
function syncBottomBadge(srcId) {
  const src = document.getElementById(srcId);
  const dst = document.getElementById(srcId + 'Bottom');
  if (!src || !dst) return;
  dst.textContent = src.textContent;
  dst.classList.toggle('d-none', src.classList.contains('d-none'));
}

function homeView(role) {
  if (role === 'CUSTOMER') return 'restaurants';
  if (role === 'RESTAURANT_OWNER') return 'dashboard';
  if (role === 'DRIVER') return 'dashboard';
  if (role === 'ADMIN') return 'overview';
  return 'restaurants';
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
    const clearBtn = document.getElementById('notifClearBtn');
    if (clearBtn) { clearBtn.disabled = unread === 0; clearBtn.classList.toggle('is-empty', unread === 0); }
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
  setBottomActive(view);
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
  const icon = type === 'ok' ? 'bi-check-lg' : type === 'err' ? 'bi-x-lg' : 'bi-bell';
  el.innerHTML = `<div class="t-icon"><i class="bi ${icon}"></i></div><div class="t-body"><div class="t-title">${escapeHtml(title)}</div><div class="t-msg">${escapeHtml(msg || '')}</div></div>`;
  wrap.appendChild(el);
  setTimeout(() => { el.style.opacity = '0'; el.style.transform = 'translateX(24px) scale(.96)'; setTimeout(() => el.remove(), 300); }, 3200);
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

// ---------- Settings (shared across roles, categorized) ----------
// opts: { panel, label, icon }  — panel is the role-specific HTML (profile/address/store).
function renderSettingsShell(opts) {
  opts = opts || {};
  const panel = opts.panel || '';
  const label = opts.label || 'Profile';
  const icon = opts.icon || 'person-gear';
  const roleNav = panel ? `<button class="set-cat" data-cat="role" onclick="selectSettingsCat('role')"><i class="bi bi-${icon}"></i> ${escapeHtml(label)}</button>` : '';
  return `
  <div class="dash-hero"><i class="bi bi-gear"></i><div><h4 class="mb-0">Settings</h4><div class="hero-sub">Manage your account and preferences</div></div></div>
  <div class="settings-wrap">
    <aside class="settings-nav card">
      <button class="set-cat active" data-cat="account" onclick="selectSettingsCat('account')"><i class="bi bi-person"></i> Account</button>
      <button class="set-cat" data-cat="security" onclick="selectSettingsCat('security')"><i class="bi bi-shield-lock"></i> Security</button>
      ${roleNav}
    </aside>
    <div class="settings-body">
      <section class="set-panel active" data-cat="account">
        <div class="card"><div class="card-body">
          <div class="section-head mb-3"><span class="sh-ic"><i class="bi bi-person"></i></span><div><h5 class="mb-0">Account</h5></div></div>
          <div id="acctAlert"></div>
          <div class="mb-2"><label class="form-label">Full name</label><input id="acctName" class="form-control" autocomplete="name"></div>
          <div class="mb-2"><label class="form-label">Email</label><input id="acctEmail" class="form-control" disabled></div>
          <div class="mb-2"><label class="form-label">Phone</label><input id="acctPhone" class="form-control" autocomplete="tel" placeholder="01xxxxxxxxx"></div>
          <button class="btn-brand" onclick="saveAccountSettings()">Save changes</button>
        </div></div>
        <div class="card mt-3"><div class="card-body d-flex justify-between align-center flex-wrap gap-2">
          <div><div class="fw-semibold">Sign out</div><div class="muted small">You'll be returned to the login screen</div></div>
          <button class="btn btn-soft btn-danger-soft" onclick="doLogout()"><i class="bi bi-box-arrow-right"></i> Logout</button>
        </div></div>
      </section>
      <section class="set-panel" data-cat="security">
        <div class="card"><div class="card-body">
          <div class="section-head mb-3"><span class="sh-ic"><i class="bi bi-shield-lock"></i></span><div><h5 class="mb-0">Security</h5></div></div>
          <div id="pwAlert"></div>
          <div class="mb-2"><label class="form-label">Current password</label><input id="pwCurrent" type="password" class="form-control" autocomplete="current-password"></div>
          <div class="mb-2"><label class="form-label">New password</label><input id="pwNew" type="password" class="form-control" autocomplete="new-password"><div class="form-hint">At least 8 characters.</div></div>
          <div class="mb-2"><label class="form-label">Confirm new password</label><input id="pwConfirm" type="password" class="form-control" autocomplete="new-password"></div>
          <button class="btn-brand" onclick="saveChangePassword()">Update password</button>
        </div></div>
      </section>
      ${panel ? `<section class="set-panel" data-cat="role">${panel}</section>` : ''}
    </div>
  </div>`;
}
function selectSettingsCat(cat) {
  document.querySelectorAll('.settings-nav .set-cat').forEach(b => b.classList.toggle('active', b.dataset.cat === cat));
  document.querySelectorAll('.settings-body .set-panel').forEach(p => p.classList.toggle('active', p.dataset.cat === cat));
  if (window.__onSettingsCat) { try { window.__onSettingsCat(cat); } catch (e) {} }
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
function statCard(icon, label, val, cls, sub) {
  return `<div class="col-6 col-lg-3"><div class="stat-card ${cls}"><div class="ic"><i class="bi bi-${icon}"></i></div><div class="label">${label}</div><div class="val">${val}</div>${sub ? `<div class="sub">${escapeHtml(sub)}</div>` : ''}</div></div>`;
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
