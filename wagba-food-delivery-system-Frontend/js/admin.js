// ===================== Admin page =====================
boot('ADMIN', init);

function init() {
  document.querySelectorAll('#adminTabs .nav-link').forEach(a => {
    a.addEventListener('click', e => {
      e.preventDefault();
      document.querySelectorAll('#adminTabs .nav-link').forEach(x => x.classList.remove('active'));
      a.classList.add('active');
      ['restaurants','drivers','coupons','users'].forEach(v =>
        document.getElementById(v).classList.toggle('d-none', v !== a.dataset.v));
      if (a.dataset.v === 'restaurants') loadRestaurants();
      if (a.dataset.v === 'drivers') loadDrivers();
      if (a.dataset.v === 'users') loadUsers();
    });
  });
  loadRestaurants();
}

// ---------- Restaurants ----------
async function loadRestaurants() {
  try {
    const list = await api('/admin/restaurants?status=PENDING');
    const el = document.getElementById('restList');
    if (!list.length) { el.innerHTML = '<p class="text-muted">No pending restaurants.</p>'; return; }
    el.innerHTML = list.map(r => `<div class="col-md-6"><div class="card h-100 mb-0"><div class="card-body d-flex justify-content-between align-items-center">
        <div><b>${escapeHtml(r.name)}</b><br><small class="muted">${escapeHtml(r.ownerEmail || '')} · ${r.status}</small></div>
        <div>
          <button class="btn btn-sm btn-success" onclick="approveRestaurant(${r.id})">Approve</button>
          <button class="btn btn-sm btn-danger" onclick="rejectRestaurant(${r.id})">Reject</button>
        </div>
      </div></div></div>`).join('');
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

async function approveRestaurant(id) {
  try { await api('/admin/restaurants/' + id + '/approve', 'POST');
    showAlert('alertBox','success','Restaurant approved'); loadRestaurants(); }
  catch (e) { showAlert('alertBox','danger',e.message); }
}
async function rejectRestaurant(id) {
  try { await api('/admin/restaurants/' + id + '/reject', 'POST');
    showAlert('alertBox','success','Restaurant rejected'); loadRestaurants(); }
  catch (e) { showAlert('alertBox','danger',e.message); }
}

// ---------- Drivers ----------
async function loadDrivers() {
  try {
    const list = await api('/admin/drivers?status=PENDING');
    const el = document.getElementById('driverList');
    if (!list.length) { el.innerHTML = '<p class="text-muted">No pending drivers.</p>'; return; }
    el.innerHTML = list.map(d => `<div class="col-md-6"><div class="card h-100 mb-0"><div class="card-body d-flex justify-content-between align-items-center">
        <div><b>${escapeHtml(d.name)}</b><br><small class="muted">${escapeHtml(d.email || '')} · ${d.status}</small></div>
        <div>
          <button class="btn btn-sm btn-success" onclick="approveDriver(${d.id})">Approve</button>
          <button class="btn btn-sm btn-danger" onclick="rejectDriver(${d.id})">Reject</button>
        </div>
      </div></div></div>`).join('');
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

async function approveDriver(id) {
  try { await api('/admin/drivers/' + id + '/approve', 'POST');
    showAlert('alertBox','success','Driver approved'); loadDrivers(); }
  catch (e) { showAlert('alertBox','danger',e.message); }
}
async function rejectDriver(id) {
  try { await api('/admin/drivers/' + id + '/reject', 'POST');
    showAlert('alertBox','success','Driver rejected'); loadDrivers(); }
  catch (e) { showAlert('alertBox','danger',e.message); }
}

// ---------- Coupons ----------
async function createCoupon() {
  clearAlert('couponAlert');
  const body = {
    code: document.getElementById('cCode').value,
    description: document.getElementById('cDesc').value,
    discountType: document.getElementById('cType').value,
    value: document.getElementById('cValue').value,
    minOrderTotal: document.getElementById('cMin').value || null,
    expiryDate: document.getElementById('cExp').value || null,
    active: true
  };
  try {
    await api('/admin/coupons', 'POST', body);
    showAlert('couponAlert','success','Coupon created');
    ['cCode','cDesc','cValue','cMin','cExp'].forEach(i => document.getElementById(i).value='');
  } catch (e) { showAlert('couponAlert','danger',e.message); }
}

async function assignCoupon() {
  clearAlert('assignAlert');
  const body = {
    userId: parseInt(document.getElementById('aUserId').value),
    code: document.getElementById('aCode').value
  };
  try {
    await api('/admin/coupons/assign', 'POST', body);
    showAlert('assignAlert','success','Coupon assigned');
    document.getElementById('aUserId').value=''; document.getElementById('aCode').value='';
  } catch (e) { showAlert('assignAlert','danger',e.message); }
}

// ---------- Users ----------
async function loadUsers() {
  try {
    const list = await api('/admin/users');
    const el = document.getElementById('userList');
    el.innerHTML = `<table class="table"><thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Status</th></tr></thead><tbody>
      ${list.map(u => `<tr><td>${u.id}</td><td>${escapeHtml(u.name)}</td><td>${escapeHtml(u.email)}</td>
        <td>${u.role}</td><td>${u.status}</td></tr>`).join('')}
    </tbody></table>`;
  } catch (e) { showAlert('alertBox','danger',e.message); }
}
