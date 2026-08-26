// ===================== Driver page =====================
boot('DRIVER', init);

function init() {
  window.VIEWS = {
    dashboard: { title: 'Dashboard', sub: 'Your delivery overview' },
    available: { title: 'Available deliveries', sub: 'Pick up an order near you' },
    mine: { title: 'My Deliveries', sub: 'Track your active & past deliveries' },
    earnings: { title: 'Earnings', sub: 'Wallet & payouts' },
    settings: { title: 'Settings', sub: 'Manage your account' }
  };
  window.onNav = (v) => { if (v === 'available') loadAvailable(); if (v === 'mine') loadMine(); if (v === 'earnings') loadDriverEarnings(); if (v === 'settings') renderSettings(); };
  window.__realtimeRefresh = (p) => {
    renderDashboard();
    if (p && (p.type === 'AVAILABLE' || p.type === 'NEW_DELIVERY')) loadAvailable();
    loadMine();
  };
  api('/driver/deliveries').then(() => {
    document.getElementById('profileBox').innerHTML = '';
    navTo('dashboard'); renderDashboard();
  }).catch(() => { showProfileForm(); });
}

const DRIVER_SETTINGS_EXTRA = `
<div class="col-12">
  <div class="card detail-card"><div class="card-body">
    <h5 class="card-title">Driver details</h5>
    <div id="drvAlert"></div>
    <div class="row g-2">
      <div class="col-sm-6 mb-2"><label class="form-label">Phone number</label><input id="drvPhone" class="form-control"></div>
      <div class="col-sm-6 mb-2"><label class="form-label">National ID</label><input id="drvNationalId" class="form-control"></div>
      <div class="col-sm-6 mb-2"><label class="form-label">Vehicle type</label><input id="drvVehicleType" class="form-control" placeholder="Motorcycle"></div>
      <div class="col-sm-6 mb-2"><label class="form-label">Vehicle number</label><input id="drvVehicleNumber" class="form-control"></div>
      <div class="col-sm-6 mb-2"><label class="form-label">License number</label><input id="drvLicense" class="form-control"></div>
    </div>
    <button class="btn-brand" onclick="saveDriverSettings()">Save driver details</button>
  </div></div>
</div>`;

function renderSettings() {
  const root = document.getElementById('view-settings');
  if (!root) return;
  root.innerHTML = renderSettingsShell({ panel: DRIVER_SETTINGS_EXTRA, label: 'Driver', icon: 'bicycle' });
  loadAccountSettings();
  loadDriverSettings();
}
async function loadDriverSettings() {
  try {
    const d = await api('/driver/profile');
    const s = (v) => v == null ? '' : String(v);
    if (document.getElementById('drvPhone')) document.getElementById('drvPhone').value = s(d.phoneNumber);
    if (document.getElementById('drvNationalId')) document.getElementById('drvNationalId').value = s(d.nationalId);
    if (document.getElementById('drvVehicleType')) document.getElementById('drvVehicleType').value = s(d.vehicleType);
    if (document.getElementById('drvVehicleNumber')) document.getElementById('drvVehicleNumber').value = s(d.vehicleNumber);
    if (document.getElementById('drvLicense')) document.getElementById('drvLicense').value = s(d.licenseNumber);
  } catch (e) { showAlert('drvAlert', 'danger', e.message); }
}
async function saveDriverSettings() {
  clearAlert('drvAlert');
  const g = (id) => { const el = document.getElementById(id); return el ? el.value.trim() : ''; };
  const body = {
    phoneNumber: g('drvPhone'),
    nationalId: g('drvNationalId'),
    vehicleType: g('drvVehicleType'),
    vehicleNumber: g('drvVehicleNumber'),
    licenseNumber: g('drvLicense')
  };
  try {
    await api('/driver/profile', 'PUT', body);
    toast('Saved', 'Driver details updated');
  } catch (e) { showAlert('drvAlert', 'danger', e.message); }
}

function showProfileForm() {
  const me = getUser();
  document.getElementById('view-dashboard').classList.add('d-none');
  document.getElementById('profileBox').innerHTML = `
    <div class="card mb-3"><div class="card-body">
      <h4>Complete your driver profile</h4>
      <p class="muted">Submit your details — an admin will approve your account before you can take deliveries.</p>
      <div class="row g-2">
        <div class="col-md-6"><label class="form-label">Phone</label><input id="dPhone" class="form-control"></div>
        <div class="col-md-6"><label class="form-label">National ID</label><input id="dNat" class="form-control"></div>
        <div class="col-md-6"><label class="form-label">Vehicle Type</label><input id="dVType" class="form-control" placeholder="Motorcycle"></div>
        <div class="col-md-6"><label class="form-label">Vehicle Number</label><input id="dVNum" class="form-control"></div>
        <div class="col-md-6"><label class="form-label">License Number</label><input id="dLic" class="form-control"></div>
      </div>
      <button class="btn-brand mt-3" onclick="submitProfile()">Submit for approval</button>
    </div></div>`;
}
async function submitProfile() {
  const me = getUser();
  const body = { phoneNumber: document.getElementById('dPhone').value, nationalId: document.getElementById('dNat').value, vehicleType: document.getElementById('dVType').value, vehicleNumber: document.getElementById('dVNum').value, licenseNumber: document.getElementById('dLic').value };
  try {
    await api('/driver/profile?userId=' + me.id, 'POST', body);
    toast('Submitted', 'Waiting for admin approval');
    document.getElementById('profileBox').innerHTML = '';
    document.getElementById('view-dashboard').classList.remove('d-none');
    renderDashboard();
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

async function renderDashboard() {
  try {
    const [ap, mn] = await Promise.all([api('/driver/deliveries/available?page=0&size=100'), api('/driver/deliveries?page=0&size=100')]);
    const avail = ap.content || [];
    const mine = mn.content || [];
    window._avail = avail; window._mine = mine;
    const active = mine.filter(d => ['ACCEPTED','PICKED_UP','OUT_FOR_DELIVERY'].includes(d.status));
    const done = mine.filter(d => d.status === 'DELIVERED');
    const earnings = done.reduce((s, d) => s + (d.earning || d.fee || 0), 0);
    document.getElementById('dashStats').innerHTML =
      statCard('bi-inboxes', 'Available now', avail.length, '') +
      statCard('bi-bicycle', 'Active', active.length, 'blue') +
      statCard('bi-bag-check', 'Completed', done.length, 'green') +
      statCard('bi-cash-stack', 'Earnings', earnings, 'violet');
    const navCard = document.getElementById('navCard');
    if (active.length) {
      const d = active[0];
      const rest = { lat: d.restaurantLatitude, lng: d.restaurantLongitude, name: d.restaurantName, addr: d.restaurantAddress };
      const cust = { lat: d.customerLatitude, lng: d.customerLongitude, name: d.customerName, addr: d.customerAddress };
      fillStops('navD_', rest, cust);
      navCard.classList.remove('d-none');
      setTimeout(() => drawRouteMap('dash', 'navMap', rest, cust), 80);
    } else {
      navCard.classList.add('d-none');
    }
  } catch (e) {}
}
function statCard(icon, label, val, cls) {
  return `<div class="col-6 col-lg-3"><div class="stat-card ${cls}"><div class="ic"><i class="bi bi-${icon}"></i></div><div class="label">${label}</div><div class="val">${val}</div></div></div>`;
}

function deliveryCard(d, withActions, locked) {
  let actions = '';
  if (withActions) {
    if (d.status === 'AVAILABLE') actions = locked
      ? `<button class="btn btn-sm btn-secondary" disabled>Complete current delivery first</button>`
      : `<button class="btn btn-sm btn-brand" onclick="actDelivery(${d.id},'accept','available')">Accept</button>`;
    else if (d.status === 'ACCEPTED') actions = `<button class="btn btn-sm btn-brand" onclick="actDelivery(${d.id},'pickup','mine')">Pick Up</button>`;
    else if (d.status === 'PICKED_UP' || d.status === 'OUT_FOR_DELIVERY') actions = `<button class="btn btn-sm btn-brand" onclick="actDelivery(${d.id},'deliver','mine')">Deliver</button>`;
    else actions = `<span class="muted small">${d.status}</span>`;
    if (['ACCEPTED','PICKED_UP','OUT_FOR_DELIVERY'].includes(d.status)) {
      actions += `<button class="btn btn-sm btn-soft" onclick="navigateToDelivery(${d.id})"><i class="bi bi-map"></i> Navigate</button>`;
    }
  }
  const meta = [
    d.restaurantName ? `<div><i class="bi bi-shop me-1 text-muted"></i>${escapeHtml(d.restaurantName)}</div>` : '',
    d.customerName ? `<div><i class="bi bi-person me-1 text-muted"></i>${escapeHtml(d.customerName)}</div>` : '',
    d.address ? `<div><i class="bi bi-geo-alt me-1 text-muted"></i>${escapeHtml(d.address)}</div>` : ''
  ].join('');
  return `<div class="col-md-6"><div class="card order-card h-100"><div class="card-body">
    <div class="oc-top">
      <div class="oc-ic"><i class="bi bi-bicycle"></i></div>
      <div style="flex:1;min-width:0">
        <div class="oc-title">Delivery #${d.id}</div>
        <div class="oc-sub">Order #${d.orderId}</div>
      </div>
      <div class="oc-badges">${statusBadge(d.status)}</div>
    </div>
    <div class="oc-meta">
      ${d.restaurantName ? `<div class="m"><i class="bi bi-shop"></i> ${escapeHtml(d.restaurantName)}</div>` : ''}
      ${d.customerName ? `<div class="m"><i class="bi bi-person"></i> ${escapeHtml(d.customerName)}</div>` : ''}
      ${d.address ? `<div class="m" style="grid-column:1/-1"><i class="bi bi-geo-alt"></i> ${escapeHtml(d.address)}</div>` : ''}
      ${d.acceptedAt ? `<div class="m"><i class="bi bi-clock"></i> Accepted ${fmtDateTime(d.acceptedAt)}</div>` : ''}
      ${d.deliveredAt ? `<div class="m"><i class="bi bi-check-circle"></i> Delivered ${fmtDateTime(d.deliveredAt)}</div>` : ''}
      ${d.earning != null ? `<div class="m"><i class="bi bi-cash"></i> Earning ${money(d.earning)}</div>` : ''}
    </div>
    ${actions ? `<div class="oc-foot"><div></div><div class="d-flex gap-2">${actions}</div></div>` : ''}
  </div></div></div>`;
}

// ---------- Navigation (pickup + drop-off map) ----------
let _navMaps = {};
function navigateToDelivery(id) {
  const all = [].concat(window._mine || [], window._avail || []);
  const d = all.find(x => x.id === id);
  if (d) openNavigation(d);
}
function openNavigation(d) {
  const rest = { lat: d.restaurantLatitude, lng: d.restaurantLongitude, name: d.restaurantName, addr: d.restaurantAddress };
  const cust = { lat: d.customerLatitude, lng: d.customerLongitude, name: d.customerName, addr: d.customerAddress };
  fillStops('navM_', rest, cust);
  const modalEl = document.getElementById('navModal');
  const handler = () => drawRouteMap('modal', 'navMapModal', rest, cust);
  modalEl.addEventListener('shown.bs.modal', handler, { once: true });
  new bootstrap.Modal(modalEl).show();
}
function fillStops(prefix, rest, cust) {
  const rn = document.getElementById(prefix + 'RestName'), ra = document.getElementById(prefix + 'RestAddr');
  const cn = document.getElementById(prefix + 'CustName'), ca = document.getElementById(prefix + 'CustAddr');
  if (rn) rn.textContent = rest.name || 'Restaurant';
  if (ra) ra.textContent = rest.addr || (rest.lat != null ? rest.lat + ', ' + rest.lng : '—');
  if (cn) cn.textContent = cust.name || 'Customer';
  if (ca) ca.textContent = cust.addr || (cust.lat != null ? cust.lat + ', ' + cust.lng : '—');
  const link = document.getElementById(prefix + 'MapsLink');
  if (link) {
    if (rest.lat != null && rest.lng != null && cust.lat != null && cust.lng != null)
      link.href = `https://www.google.com/maps/dir/?api=1&origin=${rest.lat},${rest.lng}&destination=${cust.lat},${cust.lng}`;
    else link.removeAttribute('href');
  }
}
function drawRouteMap(key, containerId, rest, cust) {
  const el = document.getElementById(containerId);
  if (!el) return null;
  if (_navMaps[key]) { _navMaps[key].remove(); _navMaps[key] = null; }
  const hasRest = rest.lat != null && rest.lng != null;
  const hasCust = cust.lat != null && cust.lng != null;
  const center = hasRest ? [rest.lat, rest.lng] : hasCust ? [cust.lat, cust.lng] : [30.0444, 31.2357];
  const map = L.map(el).setView(center, 13);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(map);
  const pts = [];
  if (hasRest) { L.marker([rest.lat, rest.lng]).addTo(map).bindPopup('<b>' + escapeHtml(rest.name || 'Restaurant') + '</b><br>Pickup point'); pts.push([rest.lat, rest.lng]); }
  if (hasCust) { L.marker([cust.lat, cust.lng]).addTo(map).bindPopup('<b>' + escapeHtml(cust.name || 'Customer') + '</b><br>Drop-off point'); pts.push([cust.lat, cust.lng]); }
  if (pts.length === 2) { L.polyline(pts, { color: '#f97316', weight: 4, opacity: .85 }).addTo(map); map.fitBounds(pts, { padding: [40, 40] }); }
  else if (pts.length === 1) map.setView(pts[0], 15);
  setTimeout(() => map.invalidateSize(), 60);
  _navMaps[key] = map;
  return map;
}

async function loadAvailable() {
  try {
    showSkeletons('availList', 4, 200);
    const [ap, mn] = await Promise.all([
      api('/driver/deliveries/available?page=0&size=100'),
      api('/driver/deliveries?page=0&size=100')
    ]);
    const list = ap.content || [];
    const mine = mn.content || [];
    const hasActive = mine.some(d => ['ACCEPTED', 'PICKED_UP', 'OUT_FOR_DELIVERY'].includes(d.status));
    const el = document.getElementById('availList');
    if (hasActive) {
      el.innerHTML = `<div class="col-12"><div class="alert alert-info d-flex align-center gap-2"><i class="bi bi-info-circle"></i> You already have an active delivery. Complete it before accepting another one.</div></div>`;
      return;
    }
    if (!list.length) { el.innerHTML = emptyState('bi-emoji-smile', 'No available deliveries', 'Check back soon — new orders appear here.'); return; }
    el.innerHTML = list.map(d => deliveryCard(d, true, hasActive)).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function loadMine() {
  try {
    showSkeletons('mineList', 4, 132);
    const page = await api('/driver/deliveries?page=0&size=100');
    const list = page.content || [];
    const el = document.getElementById('mineList');
    if (!list.length) { el.innerHTML = emptyState('bi-inbox', 'No deliveries yet', 'Accept an available delivery to get started.'); return; }
    el.innerHTML = list.map(d => deliveryCard(d, true)).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function actDelivery(id, action, reload) {
  try { await api('/driver/deliveries/' + id + '/' + action, 'POST'); toast('Delivery ' + action + 'ed', '#' + id); if (reload === 'available') loadAvailable(); else loadMine(); renderDashboard(); }
  catch (e) { showAlert('alertBox', 'danger', e.message); }
}

// ---------- Live Location Sharing ----------
let watchId = null, locMap = null, locMarker = null;
function toggleLiveLocation() {
  const btn = document.getElementById('locBtn');
  const status = document.getElementById('locStatus');
  const mapEl = document.getElementById('locMap');
  if (watchId !== null) {
    navigator.geolocation.clearWatch(watchId); watchId = null;
    btn.innerHTML = '<i class="bi bi-broadcast"></i> Share Live Location';
    status.textContent = 'Stopped sharing.';
    mapEl.classList.add('d-none');
    return;
  }
  if (!navigator.geolocation) { status.textContent = 'Geolocation not supported.'; return; }
  btn.innerHTML = '<i class="bi bi-broadcast"></i> Stop Sharing';
  status.textContent = 'Requesting location…';
  watchId = navigator.geolocation.watchPosition(
    pos => {
      const { latitude, longitude } = pos.coords;
      api('/driver/location', 'POST', { latitude, longitude }).then(() => {
        status.textContent = 'Sharing · last update ' + new Date().toLocaleTimeString();
        mapEl.classList.remove('d-none');
        if (!locMap) { locMap = L.map('locMap').setView([latitude, longitude], 15); L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(locMap); locMarker = L.marker([latitude, longitude]).addTo(locMap).bindPopup('You').openPopup(); }
        else { locMarker.setLatLng([latitude, longitude]); locMap.setView([latitude, longitude]); }
        setTimeout(() => locMap.invalidateSize(), 250);
      }).catch(e => { status.textContent = 'Update failed: ' + e.message; });
    },
    err => { status.textContent = 'Location error: ' + err.message; },
    { enableHighAccuracy: true, maximumAge: 5000, timeout: 10000 }
  );
}
// ---------- Earnings / Wallet ----------
async function loadDriverEarnings() {
  try {
    const w = await api('/driver/wallet');
    window._driverBalance = w.balance != null ? w.balance : 0;
    document.getElementById('driverWalletBalance').textContent = money(window._driverBalance);
    const txns = w.transactions || [];
    document.getElementById('driverWalletTxns').innerHTML = txns.length
      ? `<table class="table wagba"><thead><tr><th>Date</th><th>Type</th><th>Description</th><th class="text-end">Amount</th></tr></thead><tbody>`
        + txns.map(t => `<tr><td>${fmtDateTime(t.createdAt)}</td><td>${t.type}</td><td>${escapeHtml(t.description || '')}</td><td class="text-end ${t.type === 'CREDIT' ? 'text-success' : 'text-danger'} fw-semibold">${t.type === 'CREDIT' ? '+' : '-'}${money(t.amount)}</td></tr>`).join('')
        + `</tbody></table>`
      : '<div class="empty-txns"><i class="bi bi-receipt"></i><div>No transactions yet</div><div class="small">You\'ll be paid into your wallet when you complete deliveries.</div></div>';
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

async function openWithdraw() {
  clearAlert('withdrawAlert');
  document.getElementById('wdAmount').value = '';
  document.getElementById('wdAvail').textContent = money(window._driverBalance || 0);
  new bootstrap.Modal(document.getElementById('withdrawModal')).show();
}
async function withdrawEarnings() {
  clearAlert('withdrawAlert');
  const amt = parseFloat(document.getElementById('wdAmount').value);
  if (!amt || amt <= 0) { showAlert('withdrawAlert', 'danger', 'Enter a valid amount'); return; }
  if (amt > (window._driverBalance || 0)) { showAlert('withdrawAlert', 'danger', 'Amount exceeds your wallet balance'); return; }
  try {
    const res = await api('/driver/wallet/withdraw', 'POST', { amount: amt });
    bootstrap.Modal.getInstance(document.getElementById('withdrawModal')).hide();
    toast('Withdrawal ' + (res.devMode ? '(demo)' : '') + ' sent', 'Stripe payout ' + (res.status || 'processed') + ' · ' + money(amt));
    loadDriverEarnings();
  } catch (e) { showAlert('withdrawAlert', 'danger', e.message); }
}

function emptyState(icon, title, sub) {
  return `<div class="col-12"><div class="empty-state"><div class="ico"><i class="bi bi-${icon}"></i></div><div class="fw-semibold">${escapeHtml(title)}</div><div class="small">${escapeHtml(sub || '')}</div></div></div>`;
}
