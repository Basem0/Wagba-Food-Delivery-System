// ============================================================
//  Wagba frontend - common helpers (no build step, plain JS)
// ============================================================

const DEFAULT_API = 'http://localhost:8081/api/v1';

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
  const icon = document.querySelector('.wagba-nav .icon-btn i');
  if (icon) icon.className = 'bi bi-' + (t === 'dark' ? 'sun' : 'moon-stars');
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
  if (u && u.role === role) { renderNav(u); cb(u); return; }
  api('/auth/me').then(me => {
    setUser(me);
    if (me.role !== role) { location.href = dashFor(me.role); return; }
    renderNav(me); cb(me);
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
    <button class="icon-btn" onclick="toggleTheme()" title="Toggle theme">
      <i class="bi bi-${isDark ? 'sun' : 'moon-stars'}"></i>
    </button>
    <div class="api-box">
      <i class="bi bi-hdd-network text-muted"></i>
      <input id="apiInput" class="form-control form-control-sm" value="${getApiBase()}">
    </div>
    <button class="btn btn-soft btn-sm" onclick="doLogout()"><i class="bi bi-box-arrow-right"></i> Logout</button>`;
  const inp = document.getElementById('apiInput');
  inp.addEventListener('change', () => setApiBase(inp.value));
}

function doLogout() {
  api('/auth/logout', 'POST', null, true).catch(() => {}).finally(() => {
    clearAuth(); location.href = 'index.html';
  });
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
