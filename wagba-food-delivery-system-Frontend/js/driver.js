// ===================== Driver page =====================
boot('DRIVER', init);

function init() {
  // detect whether profile exists by trying to load deliveries
  api('/driver/deliveries').then(() => {
    document.getElementById('profileBox').innerHTML = '';
    document.getElementById('dashboard').classList.remove('d-none');
    bindTabs(); loadAvailable();
  }).catch(() => {
    document.getElementById('dashboard').classList.add('d-none');
    showProfileForm();
  });

  document.querySelectorAll('#driverTabs .nav-link').forEach(a => {
    a.addEventListener('click', e => {
      e.preventDefault();
      document.querySelectorAll('#driverTabs .nav-link').forEach(x => x.classList.remove('active'));
      a.classList.add('active');
      document.getElementById('available').classList.toggle('d-none', a.dataset.v !== 'available');
      document.getElementById('mine').classList.toggle('d-none', a.dataset.v !== 'mine');
      if (a.dataset.v === 'available') loadAvailable();
      if (a.dataset.v === 'mine') loadMine();
    });
  });
}

function bindTabs() {}

function showProfileForm() {
  const me = getUser();
  document.getElementById('profileBox').innerHTML = `
    <div class="card"><div class="card-body">
      <h4>Complete your driver profile</h4>
      <div class="mb-2"><label class="form-label">Phone</label><input id="dPhone" class="form-control"></div>
      <div class="mb-2"><label class="form-label">National ID</label><input id="dNat" class="form-control"></div>
      <div class="mb-2"><label class="form-label">Vehicle Type</label><input id="dVType" class="form-control" placeholder="Motorcycle"></div>
      <div class="mb-2"><label class="form-label">Vehicle Number</label><input id="dVNum" class="form-control"></div>
      <div class="mb-2"><label class="form-label">License Number</label><input id="dLic" class="form-control"></div>
      <button class="btn btn-brand" onclick="submitProfile()">Submit (pending admin approval)</button>
    </div></div>`;
}

async function submitProfile() {
  const me = getUser();
  const body = {
    phoneNumber: document.getElementById('dPhone').value,
    nationalId: document.getElementById('dNat').value,
    vehicleType: document.getElementById('dVType').value,
    vehicleNumber: document.getElementById('dVNum').value,
    licenseNumber: document.getElementById('dLic').value
  };
  try {
    await api('/driver/profile?userId=' + me.id, 'POST', body);
    showAlert('alertBox','success','Profile submitted! Waiting for admin approval.');
    document.getElementById('profileBox').innerHTML = '';
    document.getElementById('dashboard').classList.remove('d-none');
    bindTabs(); loadAvailable();
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

function deliveryCard(d, withActions) {
  let actions = '';
  if (withActions) {
    if (d.status === 'AVAILABLE') actions = `<button class="btn btn-sm btn-brand" onclick="actDelivery(${d.id},'accept','available')">Accept</button>`;
    else if (d.status === 'ACCEPTED') actions = `<button class="btn btn-sm btn-brand" onclick="actDelivery(${d.id},'pickup','mine')">Pick Up</button>`;
    else if (d.status === 'PICKED_UP' || d.status === 'OUT_FOR_DELIVERY') actions = `<button class="btn btn-sm btn-brand" onclick="actDelivery(${d.id},'deliver','mine')">Deliver</button>`;
    else actions = `<span class="text-muted small">${d.status}</span>`;
  }
  return `<div class="card h-100"><div class="card-body d-flex justify-content-between align-items-center">
    <div>
      <div class="fw-semibold">Delivery #${d.id} <span class="muted">· Order #${d.orderId}</span></div>
      <div class="mt-1">${statusBadge(d.status)}
        ${d.acceptedAt ? `<span class="muted small">· accepted ${escapeHtml(d.acceptedAt)}</span>` : ''}
        ${d.deliveredAt ? `<span class="muted small">· delivered ${escapeHtml(d.deliveredAt)}</span>` : ''}</div>
    </div>
    <div>${actions}</div>
  </div></div>`;
}

async function loadAvailable() {
  try {
    const list = await api('/driver/deliveries/available');
    const el = document.getElementById('availList');
    if (!list.length) { el.innerHTML = '<div class="col-12"><div class="empty-state"><div class="ico"><i class="bi bi-emoji-smile"></i></div>No available deliveries right now.</div></div>'; return; }
    el.innerHTML = list.map(d => `<div class="col-md-6">${deliveryCard(d, true)}</div>`).join('');
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

async function loadMine() {
  try {
    const list = await api('/driver/deliveries');
    const el = document.getElementById('mineList');
    if (!list.length) { el.innerHTML = '<div class="col-12"><div class="empty-state"><div class="ico"><i class="bi bi-inbox"></i></div>No deliveries yet.</div></div>'; return; }
    el.innerHTML = list.map(d => `<div class="col-md-6">${deliveryCard(d, true)}</div>`).join('');
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

async function actDelivery(id, action, reload) {
  try {
    await api('/driver/deliveries/' + id + '/' + action, 'POST');
    showAlert('alertBox','success','Delivery ' + action + 'ed');
    if (reload === 'available') loadAvailable(); else loadMine();
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

// ---------- Live Location Sharing ----------
let watchId = null;

function toggleLiveLocation() {
  const btn = document.getElementById('locBtn');
  const status = document.getElementById('locStatus');
  if (watchId !== null) {
    navigator.geolocation.clearWatch(watchId);
    watchId = null;
    btn.innerHTML = '<i class="bi bi-broadcast"></i> Share Live Location';
    status.textContent = 'Stopped sharing';
    return;
  }
  if (!navigator.geolocation) {
    status.textContent = 'Geolocation is not supported by this browser';
    return;
  }
  btn.innerHTML = '<i class="bi bi-broadcast"></i> Stop Sharing';
  status.textContent = 'Requesting location…';
  watchId = navigator.geolocation.watchPosition(
    pos => {
      const { latitude, longitude } = pos.coords;
      api('/driver/location', 'POST', { latitude, longitude }).then(() => {
        status.textContent = 'Sharing · last update ' + new Date().toLocaleTimeString();
      }).catch(e => { status.textContent = 'Update failed: ' + e.message; });
    },
    err => { status.textContent = 'Location error: ' + err.message; },
    { enableHighAccuracy: true, maximumAge: 5000, timeout: 10000 }
  );
}
