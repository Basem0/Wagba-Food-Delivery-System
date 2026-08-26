// ===================== Admin page =====================
boot('ADMIN', init);

function init() {
  if (window.innerWidth <= 600) {
    document.querySelector('.content')?.classList.add('admin-mobile');
  }
  window.VIEWS = {
    overview: { title: 'Overview', sub: 'Platform at a glance' },
    restaurants: { title: 'Restaurants', sub: 'Approve and manage kitchens' },
    drivers: { title: 'Drivers', sub: 'Approve and manage couriers' },
    users: { title: 'Users', sub: 'All registered users' },
    coupons: { title: 'Coupons', sub: 'Create & assign offers' },
    settings: { title: 'Settings', sub: 'Manage your account' }
  };
  window.onNav = (v) => {
    if (v === 'restaurants') loadRestaurants();
    if (v === 'drivers') loadDrivers();
    if (v === 'users') loadUsers();
    if (v === 'coupons') loadAdminCoupons();
    if (v === 'overview') renderOverview();
    if (v === 'settings') renderSettings();
  };
  window.__realtimeRefresh = () => { renderOverview(); };
  navTo('overview');
  renderOverview();
}

function renderSettings() {
  const root = document.getElementById('view-settings');
  if (!root) return;
  root.innerHTML = renderSettingsShell({});
  loadAccountSettings();
}

async function renderOverview() {
  try {
    const [analytics, rp, dp] = await Promise.all([
      api('/admin/analytics').catch(() => null),
      api('/admin/restaurants?page=0&size=100'),
      api('/admin/drivers?page=0&size=100')
    ]);
    const rests = rp.content || [];
    const drivers = dp.content || [];
    const pendR = rests.filter(r => (r.status || '') === 'PENDING').length;
    const pendD = drivers.filter(d => (d.status || '') === 'PENDING').length;
    if (analytics) {
      document.getElementById('ovStats').innerHTML =
        statCard('bi-shop', 'Restaurants', analytics.totalRestaurants, '', analytics.pendingRestaurants + ' pending') +
        statCard('bi-bicycle', 'Drivers', analytics.totalDrivers, 'blue', analytics.pendingDrivers + ' pending') +
        statCard('bi-people', 'Users', analytics.totalCustomers + analytics.totalDrivers + analytics.totalOwners, 'violet') +
        statCard('bi-cash-stack', 'Revenue', analytics.totalRevenue, 'green', money(analytics.revenueToday) + ' today');
      document.getElementById('ovOrders').innerHTML = `
        <div class="row g-2">
          <div class="col-4 text-center"><div class="h4 mb-0">${analytics.totalOrders}</div><div class="muted small">Total orders</div></div>
          <div class="col-4 text-center"><div class="h4 mb-0">${analytics.ordersToday}</div><div class="muted small">Today</div></div>
          <div class="col-4 text-center"><div class="h4 mb-0">${analytics.ordersThisWeek}</div><div class="muted small">This week</div></div>
        </div>`;
    } else {
      const up = await api('/admin/users?page=0&size=100');
      const users = up.content || [];
      const owners = users.filter(u => u.role === 'RESTAURANT_OWNER').length;
      const drv = users.filter(u => u.role === 'DRIVER').length;
      const cust = users.filter(u => u.role === 'CUSTOMER').length;
      const apprR = rests.filter(r => (r.status || '') === 'APPROVED').length;
      const apprD = drivers.filter(d => (d.status || '') === 'APPROVED').length;
      document.getElementById('ovStats').innerHTML =
        statCard('bi-shop', 'Restaurants', rests.length, '', apprR + ' approved') +
        statCard('bi-bicycle', 'Drivers', drivers.length, 'blue', apprD + ' approved') +
        statCard('bi-people', 'Users', users.length, 'violet', `${cust} customers · ${drv} drivers · ${owners} owners`) +
        statCard('bi-hourglass-split', 'Pending review', pendR + pendD, 'green', pendR + ' restaurants · ' + pendD + ' drivers');
    }

    const pendRest = rests.filter(r => (r.status || '') === 'PENDING').slice(0, 4);
    document.getElementById('ovRest').innerHTML = pendRest.length
      ? pendRest.map(r => `<div class="d-flex justify-between align-center py-2" style="border-bottom:1px solid var(--border-soft)"><div><b>${escapeHtml(r.name)}</b><div class="muted small">${escapeHtml(r.ownerEmail || '')}</div></div>${statusBadge(r.status)}</div>`).join('')
      : '<p class="muted small mb-0">All caught up. 🎉</p>';
    const pendDrv = drivers.filter(d => (d.status || '') === 'PENDING').slice(0, 4);
    document.getElementById('ovDrivers').innerHTML = pendDrv.length
      ? pendDrv.map(d => `<div class="d-flex justify-between align-center py-2" style="border-bottom:1px solid var(--border-soft)"><div><b>${escapeHtml(d.name)}</b><div class="muted small">${escapeHtml(d.email || '')}</div></div>${statusBadge(d.status)}</div>`).join('')
      : '<p class="muted small mb-0">All caught up. 🎉</p>';
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
function roleBadge(role) {
  const map = { CUSTOMER: 'customer', RESTAURANT_OWNER: 'owner', DRIVER: 'driver', ADMIN: 'admin' };
  return `<span class="role-badge role-${map[role] || 'admin'}">${escapeHtml(role)}</span>`;
}

// ---------- Restaurants ----------
async function loadRestaurants() {
  try {
    showSkeletons('restList', 6, 150);
    const status = document.getElementById('restFilter').value;
    const search = document.getElementById('restSearch') ? document.getElementById('restSearch').value.trim() : '';
    const qs = new URLSearchParams();
    if (status) qs.set('status', status);
    if (search) qs.set('search', search);
    qs.set('page', '0'); qs.set('size', '100');
    const page = await api('/admin/restaurants?' + qs.toString());
    const list = page.content || [];
    const el = document.getElementById('restList');
    if (!list.length) { el.innerHTML = '<div class="col-12">' + emptyState('bi-shop', 'Nothing here', 'No restaurants for this filter.') + '</div>'; return; }
    el.innerHTML = list.map(r => `
      <div class="col-12 col-md-6"><div class="entity-card">
        <div class="ec-head">
          <div class="ec-avatar"><i class="bi bi-shop"></i></div>
          <div class="ec-id"><b>${escapeHtml(r.name)}</b><div class="muted small">${escapeHtml(r.ownerEmail || '')}</div></div>
          ${statusBadge(r.status)}
        </div>
        <div class="ec-body">
          <span class="ec-detail"><i class="bi bi-tags"></i> ${escapeHtml(r.cuisine || '—')}</span>
          <span class="ec-detail"><i class="bi bi-star-fill text-warning"></i> ${r.avgRating != null ? r.avgRating : '—'}</span>
          <span class="ec-detail"><i class="bi bi-telephone"></i> ${escapeHtml(r.phone || '—')}</span>
          ${r.hasOffers ? '<span class="ec-detail offer"><i class="bi bi-tag-fill"></i> Offers</span>' : ''}
        </div>
        <p class="muted small mt-2 mb-2">${escapeHtml(r.description || 'No description')}</p>
        <div class="ec-actions">
          ${(r.status || '') === 'PENDING' ? `<button class="btn btn-sm btn-success" onclick="approveRestaurant(${r.id})"><i class="bi bi-check-lg"></i> Approve</button><button class="btn btn-sm btn-danger" onclick="rejectRestaurant(${r.id})"><i class="bi bi-x-lg"></i> Reject</button>` : `
            <button class="btn btn-sm btn-outline-warning" onclick="suspendRestaurant(${r.id})">Suspend</button>
            <button class="btn btn-sm btn-outline-success" onclick="activateRestaurant(${r.id})">Activate</button>
            <button class="btn btn-sm btn-outline-primary" onclick="editRestaurantPrompt(${r.id}, '${escapeHtml(r.name)}', '${escapeHtml(r.cuisine || '')}')">Edit</button>
            <button class="btn btn-sm btn-outline-danger" onclick="deleteRestaurant(${r.id})">Delete</button>`}
        </div>
      </div></div>`).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function approveRestaurant(id) { try { await api('/admin/restaurants/' + id + '/approve', 'POST'); toast('Approved', 'Restaurant #' + id); loadRestaurants(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }
async function rejectRestaurant(id) { try { await api('/admin/restaurants/' + id + '/reject', 'POST'); toast('Rejected', 'Restaurant #' + id); loadRestaurants(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }

// ---------- Drivers ----------
async function loadDrivers() {
  try {
    showSkeletons('driverList', 6, 150);
    const status = document.getElementById('drvFilter').value;
    const search = document.getElementById('drvSearch') ? document.getElementById('drvSearch').value.trim() : '';
    const qs = new URLSearchParams();
    if (status) qs.set('status', status);
    if (search) qs.set('search', search);
    qs.set('page', '0'); qs.set('size', '100');
    const page = await api('/admin/drivers?' + qs.toString());
    const list = page.content || [];
    const el = document.getElementById('driverList');
    if (!list.length) { el.innerHTML = '<div class="col-12">' + emptyState('bi-bicycle', 'Nothing here', 'No drivers for this filter.') + '</div>'; return; }
    el.innerHTML = list.map(d => `
      <div class="col-12 col-md-6"><div class="entity-card">
        <div class="ec-head">
          <div class="ec-avatar"><i class="bi bi-person-badge"></i></div>
          <div class="ec-id"><b>${escapeHtml(d.name)}</b><div class="muted small">${escapeHtml(d.email || '')}</div></div>
          ${statusBadge(d.status)}
        </div>
        <div class="ec-body">
          <span class="ec-detail"><i class="bi bi-truck"></i> ${escapeHtml(d.vehicleType || '—')}</span>
          <span class="ec-detail"><i class="bi bi-hash"></i> ${escapeHtml(d.vehicleNumber || '—')}</span>
          <span class="ec-detail"><i class="bi bi-telephone"></i> ${escapeHtml(d.phoneNumber || '—')}</span>
          <span class="ec-detail"><i class="bi bi-card-text"></i> ${escapeHtml(d.licenseNumber || '—')}</span>
        </div>
        <div class="ec-actions">
          ${(d.status || '') === 'PENDING' ? `<button class="btn btn-sm btn-success" onclick="approveDriver(${d.id})"><i class="bi bi-check-lg"></i> Approve</button><button class="btn btn-sm btn-danger" onclick="rejectDriver(${d.id})"><i class="bi bi-x-lg"></i> Reject</button>` : `
            <button class="btn btn-sm btn-outline-primary" onclick="showDriverPerformance(${d.id})"><i class="bi bi-graph-up"></i> Performance</button>
            <button class="btn btn-sm btn-outline-warning" onclick="suspendUser(${d.id})">Suspend</button>
            <button class="btn btn-sm btn-outline-success" onclick="activateUser(${d.id})">Activate</button>
            <button class="btn btn-sm btn-outline-dark" onclick="banDriver(${d.id})">Ban</button>
            <button class="btn btn-sm btn-outline-secondary" onclick="unbanDriver(${d.id})">Unban</button>
            <button class="btn btn-sm btn-outline-danger" onclick="deleteUser(${d.id})">Delete</button>`}
        </div>
      </div></div>`).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function approveDriver(id) { try { await api('/admin/drivers/' + id + '/approve', 'POST'); toast('Approved', 'Driver #' + id); loadDrivers(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }
async function rejectDriver(id) { try { await api('/admin/drivers/' + id + '/reject', 'POST'); toast('Rejected', 'Driver #' + id); loadDrivers(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }

// ---------- Users ----------
async function loadUsers() {
  try {
    const ur = document.getElementById('userRows');
    if (ur) ur.innerHTML = '<tr><td colspan="5" class="muted text-center py-3">Loading users…</td></tr>';
    const search = document.getElementById('userSearch') ? document.getElementById('userSearch').value.trim() : '';
    const qs = new URLSearchParams();
    if (search) qs.set('search', search);
    qs.set('page', '0'); qs.set('size', '100');
    const page = await api('/admin/users?' + qs.toString());
    const list = page.content || [];
    const isMobile = window.innerWidth <= 600;
    if (isMobile) {
      let cardContainer = document.getElementById('userCardList');
      if (!cardContainer) {
        const tbl = document.querySelector('#view-users .scroll-x');
        if (tbl) { cardContainer = document.createElement('div'); cardContainer.id = 'userCardList'; cardContainer.className = 'user-card-list'; tbl.parentNode.insertBefore(cardContainer, tbl.nextSibling); }
      }
      if (cardContainer) {
        cardContainer.innerHTML = list.map(u => `
          <div class="user-card">
            <div class="uc-top">
              <div class="cell-avatar" style="width:36px;height:36px;font-size:.8rem">${escapeHtml(initials(u.name))}</div>
              <div style="flex:1;min-width:0">
                <div class="uc-name">${escapeHtml(u.name)}</div>
                <div class="uc-email">${escapeHtml(u.email)}</div>
              </div>
              ${statusBadge(u.status)}
            </div>
            <div class="uc-meta">${roleBadge(u.role)}</div>
            <div class="uc-actions">
              <button class="btn btn-sm btn-outline-warning" onclick="suspendUser(${u.id})">Suspend</button>
              <button class="btn btn-sm btn-outline-success" onclick="activateUser(${u.id})">Activate</button>
              <button class="btn btn-sm btn-outline-danger" onclick="deleteUser(${u.id})">Delete</button>
            </div>
          </div>`).join('');
      }
    } else {
      document.getElementById('userRows').innerHTML = list.map(u => `<tr>
        <td>${u.id}</td>
        <td><div class="cell-name"><div class="cell-avatar" style="width:30px;height:30px;font-size:.85rem">${escapeHtml(initials(u.name))}</div>${escapeHtml(u.name)}</div></td>
        <td>${escapeHtml(u.email)}</td><td>${roleBadge(u.role)}</td><td>${statusBadge(u.status)}</td>
        <td class="d-flex gap-1 flex-wrap">
          <button class="btn btn-sm btn-outline-warning" onclick="suspendUser(${u.id})">Suspend</button>
          <button class="btn btn-sm btn-outline-success" onclick="activateUser(${u.id})">Activate</button>
          <button class="btn btn-sm btn-outline-danger" onclick="deleteUser(${u.id})">Delete</button>
        </td></tr>`).join('');
    }
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

// ---------- Admin powers ----------
async function suspendRestaurant(id) { try { await api('/admin/restaurants/' + id + '/suspend', 'POST'); toast('Suspended', 'Restaurant #' + id); loadRestaurants(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }
async function activateRestaurant(id) { try { await api('/admin/restaurants/' + id + '/activate', 'POST'); toast('Activated', 'Restaurant #' + id); loadRestaurants(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }
async function deleteRestaurant(id) { if (!confirm('Delete restaurant #' + id + '? This cannot be undone.')) return; try { await api('/admin/restaurants/' + id, 'DELETE'); toast('Deleted', 'Restaurant #' + id); loadRestaurants(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }
function editRestaurantPrompt(id, name, cuisine) {
  document.getElementById('editRestId').value = id;
  document.getElementById('editRestName').value = name || '';
  document.getElementById('editRestCuisine').value = cuisine || '';
  clearAlert('editRestAlert');
  new bootstrap.Modal(document.getElementById('editRestModal')).show();
}
async function saveEditRestaurant() {
  clearAlert('editRestAlert');
  const id = document.getElementById('editRestId').value;
  const body = { name: document.getElementById('editRestName').value, cuisine: document.getElementById('editRestCuisine').value };
  try {
    await api('/admin/restaurants/' + id, 'PUT', body);
    bootstrap.Modal.getInstance(document.getElementById('editRestModal')).hide();
    toast('Updated', 'Restaurant #' + id); loadRestaurants(); renderOverview();
  } catch (e) { showAlert('editRestAlert', 'danger', e.message); }
}
async function suspendUser(id) { try { await api('/admin/users/' + id + '/suspend', 'POST'); toast('Suspended', 'User #' + id); loadDrivers(); loadUsers(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }
async function activateUser(id) { try { await api('/admin/users/' + id + '/activate', 'POST'); toast('Activated', 'User #' + id); loadDrivers(); loadUsers(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }
async function deleteUser(id) { if (!confirm('Delete user #' + id + '? This cannot be undone.')) return; try { await api('/admin/users/' + id, 'DELETE'); toast('Deleted', 'User #' + id); loadDrivers(); loadUsers(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }
async function banDriver(id) { try { await api('/admin/drivers/' + id + '/ban', 'POST'); toast('Banned', 'Driver #' + id); loadDrivers(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }
async function unbanDriver(id) { try { await api('/admin/drivers/' + id + '/unban', 'POST'); toast('Unbanned', 'Driver #' + id); loadDrivers(); renderOverview(); } catch (e) { showAlert('alertBox', 'danger', e.message); } }

// ---------- Driver Performance ----------
async function showDriverPerformance(id) {
  try {
    const p = await api('/admin/drivers/' + id + '/performance');
    const perfEl = document.getElementById('drvPerf');
    if (perfEl) {
      perfEl.innerHTML = `
        <div class="card mb-3"><div class="card-body">
          <h5><i class="bi bi-graph-up"></i> ${escapeHtml(p.name)} — Performance</h5>
          <div class="row g-3 mt-2">
            <div class="col-4 text-center"><div class="h4 mb-0">${p.totalDeliveries}</div><div class="muted small">Deliveries</div></div>
            <div class="col-4 text-center"><div class="h4 mb-0">${money(p.totalEarnings)}</div><div class="muted small">Earnings</div></div>
            <div class="col-4 text-center"><div class="h4 mb-0">${p.avgRating != null ? p.avgRating + ' ⭐' : '—'}</div><div class="muted small">${p.reviewCount || 0} reviews</div></div>
          </div>
        </div></div>`;
      new bootstrap.Modal(document.getElementById('drvPerfModal')).show();
    }
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
async function loadAdminCoupons() {
  try {
    const list = await api('/admin/coupons');
    const el = document.getElementById('couponList');
    if (!list.length) { el.innerHTML = '<p class="muted small mb-0">No coupons yet.</p>'; return; }
    el.innerHTML = list.map(c => `
      <div class="col-12 col-md-6"><div class="coupon-ticket ${c.active ? '' : 'inactive'}">
        <div class="ct-left">
          <div class="ct-discount">${c.discountType === 'PERCENTAGE' ? c.value + '%' : money(c.value)}</div>
          <div class="ct-off">OFF</div>
        </div>
        <div class="ct-divider"></div>
        <div class="ct-info">
          <div class="d-flex justify-between align-center mb-1">
            <b class="text-uppercase">${escapeHtml(c.code)}</b>
            ${c.active ? '<span class="badge bg-success">Active</span>' : '<span class="badge bg-secondary">Inactive</span>'}
          </div>
          <div class="muted small">${escapeHtml(c.description || 'No description')}</div>
          <div class="small mt-1">${c.discountType === 'PERCENTAGE' ? c.value + '% off' : money(c.value) + ' off'}${c.minOrderTotal ? ' · min ' + money(c.minOrderTotal) : ''}</div>
          ${c.expiryDate ? `<div class="muted small">Expires: ${escapeHtml(c.expiryDate)}</div>` : ''}
        </div>
      </div></div>`).join('');
  } catch (e) { showAlert('couponAlert', 'danger', e.message); }
}

