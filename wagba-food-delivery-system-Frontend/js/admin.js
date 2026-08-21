// ===================== Admin page =====================
boot('ADMIN', init);

function init() {
  window.VIEWS = {
    overview: { title: 'Overview', sub: 'Platform at a glance' },
    restaurants: { title: 'Restaurants', sub: 'Approve and manage kitchens' },
    drivers: { title: 'Drivers', sub: 'Approve and manage couriers' },
    users: { title: 'Users', sub: 'All registered users' },
    coupons: { title: 'Coupons', sub: 'Create & assign offers' }
  };
  window.onNav = (v) => {
    if (v === 'restaurants') loadRestaurants();
    if (v === 'drivers') loadDrivers();
    if (v === 'users') loadUsers();
    if (v === 'overview') renderOverview();
  };
  navTo('overview');
  renderOverview();
}

async function renderOverview() {
  try {
    const [rests, drivers, users] = await Promise.all([
      api('/admin/restaurants'), api('/admin/drivers'), api('/admin/users')
    ]);
    const pendR = rests.filter(r => (r.status || '') === 'PENDING').length;
    const pendD = drivers.filter(d => (d.status || '') === 'PENDING').length;
    const owners = users.filter(u => u.role === 'RESTAURANT_OWNER').length;
    const drv = users.filter(u => u.role === 'DRIVER').length;
    const cust = users.filter(u => u.role === 'CUSTOMER').length;
    document.getElementById('ovStats').innerHTML =
      statCard('bi-shop', 'Restaurants', rests.length, '') +
      statCard('bi-bicycle', 'Drivers', drivers.length, 'blue') +
      statCard('bi-people', 'Users', users.length, 'violet') +
      statCard('bi-hourglass-split', 'Pending review', pendR + pendD, 'green');

    const pendRest = rests.filter(r => (r.status || '') === 'PENDING').slice(0, 4);
    document.getElementById('ovRest').innerHTML = pendRest.length
      ? pendRest.map(r => `<div class="d-flex justify-between py-2" style="border-bottom:1px solid var(--line)"><div><b>${escapeHtml(r.name)}</b><div class="muted small">${escapeHtml(r.ownerEmail || '')}</div></div>${statusBadge(r.status)}</div>`).join('')
      : '<p class="muted small mb-0">All caught up.</p>';
    const pendDrv = drivers.filter(d => (d.status || '') === 'PENDING').slice(0, 4);
    document.getElementById('ovDrivers').innerHTML = pendDrv.length
      ? pendDrv.map(d => `<div class="d-flex justify-between py-2" style="border-bottom:1px solid var(--line)"><div><b>${escapeHtml(d.name)}</b><div class="muted small">${escapeHtml(d.email || '')}</div></div>${statusBadge(d.status)}</div>`).join('')
      : '<p class="muted small mb-0">All caught up.</p>';
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
function statCard(icon, label, val, cls) {
  return `<div class="col-6 col-lg-3"><div class="stat-card ${cls}"><div class="ic"><i class="bi bi-${icon}"></i></div><div class="label">${label}</div><div class="val">${val}</div></div></div>`;
}

// ---------- Restaurants ----------
async function loadRestaurants() {
  try {
    const status = document.getElementById('restFilter').value;
    const list = await api('/admin/restaurants' + (status ? '?status=' + status : ''));
    const el = document.getElementById('restList');
    if (!list.length) { el.innerHTML = emptyState('bi-shop', 'Nothing here', 'No restaurants for this filter.'); return; }
    el.innerHTML = list.map(r => `
      <div class="col-md-6"><div class="card h-100"><div class="card-body">
        <div class="d-flex justify-between align-center mb-2">
          <div class="cell-name"><div class="cell-avatar"><i class="bi bi-shop"></i></div><div><b>${escapeHtml(r.name)}</b><div class="muted small">${escapeHtml(r.ownerEmail || '')}</div></div></div>
          ${statusBadge(r.status)}
        </div>
        <p class="muted small mb-2">${escapeHtml(r.description || 'No description')}</p>
        <div class="d-flex gap-2">
          ${(r.status || '') === 'PENDING' ? `<button class="btn btn-sm btn-success" onclick="approveRestaurant(${r.id})">Approve</button><button class="btn btn-sm btn-danger" onclick="rejectRestaurant(${r.id})">Reject</button>` : `<span class="muted small">${escapeHtml(r.cuisine || '')}</span>`}
        </div>
      </div></div></div>`).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function approveRestaurant(id) { try { await api('/admin/restaurants/' + id + '/approve', 'POST'); toast('Approved', 'Restaurant #' + id); loadRestaurants(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }
async function rejectRestaurant(id) { try { await api('/admin/restaurants/' + id + '/reject', 'POST'); toast('Rejected', 'Restaurant #' + id); loadRestaurants(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }

// ---------- Drivers ----------
async function loadDrivers() {
  try {
    const status = document.getElementById('drvFilter').value;
    const list = await api('/admin/drivers' + (status ? '?status=' + status : ''));
    const el = document.getElementById('driverList');
    if (!list.length) { el.innerHTML = emptyState('bi-bicycle', 'Nothing here', 'No drivers for this filter.'); return; }
    el.innerHTML = list.map(d => `
      <div class="col-md-6"><div class="card h-100"><div class="card-body">
        <div class="d-flex justify-between align-center mb-2">
          <div class="cell-name"><div class="cell-avatar"><i class="bi bi-person-badge"></i></div><div><b>${escapeHtml(d.name)}</b><div class="muted small">${escapeHtml(d.email || '')}</div></div></div>
          ${statusBadge(d.status)}
        </div>
        <div class="muted small mb-2">${d.vehicleType ? escapeHtml(d.vehicleType) + ' · ' : ''}${d.vehicleNumber ? escapeHtml(d.vehicleNumber) : ''}</div>
        <div class="d-flex gap-2">
          ${(d.status || '') === 'PENDING' ? `<button class="btn btn-sm btn-success" onclick="approveDriver(${d.id})">Approve</button><button class="btn btn-sm btn-danger" onclick="rejectDriver(${d.id})">Reject</button>` : ''}
        </div>
      </div></div></div>`).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function approveDriver(id) { try { await api('/admin/drivers/' + id + '/approve', 'POST'); toast('Approved', 'Driver #' + id); loadDrivers(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }
async function rejectDriver(id) { try { await api('/admin/drivers/' + id + '/reject', 'POST'); toast('Rejected', 'Driver #' + id); loadDrivers(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }

// ---------- Users ----------
async function loadUsers() {
  try {
    const list = await api('/admin/users');
    document.getElementById('userRows').innerHTML = list.map(u => `<tr>
      <td>${u.id}</td>
      <td><div class="cell-name"><div class="cell-avatar" style="width:30px;height:30px;font-size:.85rem">${escapeHtml(initials(u.name))}</div>${escapeHtml(u.name)}</div></td>
      <td>${escapeHtml(u.email)}</td><td>${u.role}</td><td>${u.status}</td></tr>`).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

// ---------- Coupons ----------
async function createCoupon() {
  clearAlert('couponAlert');
  const body = { code: document.getElementById('cCode').value, description: document.getElementById('cDesc').value, discountType: document.getElementById('cType').value, value: document.getElementById('cValue').value, minOrderTotal: document.getElementById('cMin').value || null, expiryDate: document.getElementById('cExp').value || null, active: true };
  try { await api('/admin/coupons', 'POST', body); toast('Created', body.code); ['cCode','cDesc','cValue','cMin','cExp'].forEach(i => document.getElementById(i).value = ''); }
  catch (e) { showAlert('couponAlert', 'danger', e.message); }
}
async function assignCoupon() {
  clearAlert('assignAlert');
  const body = { userId: parseInt(document.getElementById('aUserId').value), code: document.getElementById('aCode').value };
  try { await api('/admin/coupons/assign', 'POST', body); toast('Assigned', body.code + ' → user ' + body.userId); document.getElementById('aUserId').value = ''; document.getElementById('aCode').value = ''; }
  catch (e) { showAlert('assignAlert', 'danger', e.message); }
}

function emptyState(icon, title, sub) {
  return `<div class="col-12"><div class="empty-state"><div class="ico"><i class="bi bi-${icon}"></i></div><div class="fw-semibold">${escapeHtml(title)}</div><div class="small">${escapeHtml(sub || '')}</div></div></div>`;
}
