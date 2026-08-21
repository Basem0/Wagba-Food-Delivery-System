// ===================== Driver page =====================
boot('DRIVER', init);

function init() {
  window.VIEWS = {
    dashboard: { title: 'Dashboard', sub: 'Your delivery overview' },
    available: { title: 'Available deliveries', sub: 'Pick up an order near you' },
    mine: { title: 'My Deliveries', sub: 'Track your active & past deliveries' }
  };
  window.onNav = (v) => { if (v === 'available') loadAvailable(); if (v === 'mine') loadMine(); };
  api('/driver/deliveries').then(() => {
    document.getElementById('profileBox').innerHTML = '';
    navTo('dashboard'); renderDashboard();
  }).catch(() => { showProfileForm(); });
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
    const [avail, mine] = await Promise.all([api('/driver/deliveries/available'), api('/driver/deliveries')]);
    const active = mine.filter(d => ['ACCEPTED','PICKED_UP','OUT_FOR_DELIVERY'].includes(d.status));
    const done = mine.filter(d => d.status === 'DELIVERED');
    const earnings = done.reduce((s, d) => s + (d.earning || d.fee || 0), 0);
    document.getElementById('dashStats').innerHTML =
      statCard('bi-inboxes', 'Available now', avail.length, '') +
      statCard('bi-bicycle', 'Active', active.length, 'blue') +
      statCard('bi-bag-check', 'Completed', done.length, 'green') +
      statCard('bi-cash-stack', 'Earnings', earnings, 'violet');
  } catch (e) {}
}
function statCard(icon, label, val, cls) {
  return `<div class="col-6 col-lg-3"><div class="stat-card ${cls}"><div class="ic"><i class="bi bi-${icon}"></i></div><div class="label">${label}</div><div class="val">${val}</div></div></div>`;
}

function deliveryCard(d, withActions) {
  let actions = '';
  if (withActions) {
    if (d.status === 'AVAILABLE') actions = `<button class="btn btn-sm btn-brand" onclick="actDelivery(${d.id},'accept','available')">Accept</button>`;
    else if (d.status === 'ACCEPTED') actions = `<button class="btn btn-sm btn-brand" onclick="actDelivery(${d.id},'pickup','mine')">Pick Up</button>`;
    else if (d.status === 'PICKED_UP' || d.status === 'OUT_FOR_DELIVERY') actions = `<button class="btn btn-sm btn-brand" onclick="actDelivery(${d.id},'deliver','mine')">Deliver</button>`;
    else actions = `<span class="muted small">${d.status}</span>`;
  }
  const meta = [
    d.restaurantName ? `<div><i class="bi bi-shop me-1 text-muted"></i>${escapeHtml(d.restaurantName)}</div>` : '',
    d.customerName ? `<div><i class="bi bi-person me-1 text-muted"></i>${escapeHtml(d.customerName)}</div>` : '',
    d.address ? `<div><i class="bi bi-geo-alt me-1 text-muted"></i>${escapeHtml(d.address)}</div>` : ''
  ].join('');
  return `<div class="col-md-6"><div class="card h-100"><div class="card-body">
    <div class="d-flex justify-between align-center mb-2">
      <div><b>Delivery #${d.id}</b> <span class="muted small">· Order #${d.orderId}</span></div>
      ${statusBadge(d.status)}
    </div>
    ${meta ? `<div class="muted small mb-2">${meta}</div>` : ''}
    <div class="muted small">
      ${d.acceptedAt ? `<div>Accepted: ${fmtDateTime(d.acceptedAt)}</div>` : ''}
      ${d.deliveredAt ? `<div>Delivered: ${fmtDateTime(d.deliveredAt)}</div>` : ''}
      ${d.earning != null ? `<div>Earning: ${money(d.earning)}</div>` : ''}
    </div>
    <div class="mt-2">${actions}</div>
  </div></div></div>`;
}

async function loadAvailable() {
  try {
    const list = await api('/driver/deliveries/available');
    const el = document.getElementById('availList');
    if (!list.length) { el.innerHTML = emptyState('bi-emoji-smile', 'No available deliveries', 'Check back soon — new orders appear here.'); return; }
    el.innerHTML = list.map(d => deliveryCard(d, true)).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function loadMine() {
  try {
    const list = await api('/driver/deliveries');
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
function emptyState(icon, title, sub) {
  return `<div class="col-12"><div class="empty-state"><div class="ico"><i class="bi bi-${icon}"></i></div><div class="fw-semibold">${escapeHtml(title)}</div><div class="small">${escapeHtml(sub || '')}</div></div></div>`;
}
