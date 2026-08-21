// ===================== Restaurant Owner page =====================
boot('RESTAURANT_OWNER', init);

function init() {
  window.VIEWS = {
    dashboard: { title: 'Dashboard', sub: 'Overview of your restaurant' },
    menu: { title: 'Menu', sub: 'Manage categories & dishes' },
    orders: { title: 'Orders', sub: 'Accept and fulfil orders' }
  };
  window.onNav = (v) => { if (v === 'menu') { loadCategories(); loadFoods(); } if (v === 'orders') loadOrders(); };
  navTo('dashboard');
  loadRestaurant();
}

async function loadRestaurant() {
  try {
    const r = await api('/restaurant-owner/restaurant');
    document.getElementById('profileBox').innerHTML = '';
    window._rest = r;
    window._cats = r.categories || [];
    renderDashboard(r);
    loadCategories();
  } catch (e) {
    document.getElementById('view-dashboard').classList.add('d-none');
    showProfileForm();
  }
}

function showProfileForm() {
  const me = getUser();
  document.getElementById('profileBox').innerHTML = `
    <div class="card mb-3"><div class="card-body">
      <h4>Complete your restaurant profile</h4>
      <p class="muted">After submitting, an admin must approve your restaurant before you can receive orders.</p>
      <div class="row g-2">
        <div class="col-md-6"><label class="form-label">Name</label><input id="pName" class="form-control"></div>
        <div class="col-md-6"><label class="form-label">Image URL</label><input id="pImg" class="form-control"></div>
      </div>
      <div class="mb-2 mt-2"><label class="form-label">Description</label><input id="pDesc" class="form-control"></div>
      <div class="row g-2">
        <div class="col-md-6"><label class="form-label">Cuisine</label><input id="pCuisine" class="form-control" placeholder="e.g. Grill"></div>
        <div class="col-md-3"><label class="form-label">ETA (min)</label><input id="pEta" type="number" class="form-control"></div>
        <div class="col-md-3"><label class="form-label">Delivery fee</label><input id="pFee" type="number" step="0.01" class="form-control"></div>
      </div>
      <div class="mb-2 mt-2"><label class="form-label">Min order (EGP)</label><input id="pMin" type="number" step="0.01" class="form-control"></div>
      <button class="btn-brand" onclick="submitProfile()">Submit for approval</button>
    </div></div>`;
}

async function submitProfile() {
  const me = getUser();
  const body = {
    name: document.getElementById('pName').value,
    description: document.getElementById('pDesc').value,
    imageUrl: document.getElementById('pImg').value,
    cuisine: document.getElementById('pCuisine').value,
    etaMinutes: document.getElementById('pEta').value || null,
    deliveryFee: document.getElementById('pFee').value || null,
    minOrderTotal: document.getElementById('pMin').value || null
  };
  try {
    await api('/restaurant-owner/profile?userId=' + me.id, 'POST', body);
    toast('Submitted', 'Waiting for admin approval');
    document.getElementById('profileBox').innerHTML = '';
    document.getElementById('view-dashboard').classList.remove('d-none');
    loadRestaurant();
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

function openEditRestaurant() {
  clearAlert('restInfoAlert');
  const r = window._rest || {};
  document.getElementById('riName').value = r.name || '';
  document.getElementById('riDesc').value = r.description || '';
  document.getElementById('riImg').value = r.imageUrl || '';
  document.getElementById('riCuisine').value = r.cuisine || '';
  document.getElementById('riEta').value = r.etaMinutes != null ? r.etaMinutes : '';
  document.getElementById('riFee').value = r.deliveryFee != null ? r.deliveryFee : '';
  document.getElementById('riMin').value = r.minOrderTotal != null ? r.minOrderTotal : '';
  new bootstrap.Modal(document.getElementById('restInfoModal')).show();
}
async function saveRestaurantInfo() {
  clearAlert('restInfoAlert');
  const body = {
    name: document.getElementById('riName').value,
    description: document.getElementById('riDesc').value,
    imageUrl: document.getElementById('riImg').value,
    cuisine: document.getElementById('riCuisine').value,
    etaMinutes: document.getElementById('riEta').value || null,
    deliveryFee: document.getElementById('riFee').value || null,
    minOrderTotal: document.getElementById('riMin').value || null
  };
  try {
    await api('/restaurant-owner/restaurant', 'PUT', body);
    bootstrap.Modal.getInstance(document.getElementById('restInfoModal')).hide();
    toast('Saved', 'Restaurant info updated');
    loadRestaurant();
  } catch (e) { showAlert('restInfoAlert', 'danger', e.message); }
}

async function renderDashboard(r) {
  try {
    const orders = await api('/restaurant-owner/orders');
    const foods = await api('/restaurant-owner/foods');
    const today = new Date().toISOString().slice(0, 10);
    const todays = orders.filter(o => (o.createdAt || '').slice(0, 10) === today);
    const pending = orders.filter(o => o.status === 'PENDING');
    const revenue = orders.filter(o => ['DELIVERED','COMPLETED'].includes(o.status)).reduce((s, o) => s + (o.totalPrice || 0), 0);
    document.getElementById('dashStats').innerHTML = `
      ${statCard('bi-receipt', 'Today\'s orders', todays.length, 'blue')}
      ${statCard('bi-hourglass-split', 'Pending', pending.length, '')}
      ${statCard('bi-cash-stack', 'Revenue (done)', revenue, 'green')}
      ${statCard('bi-egg-fried', 'Menu items', foods.length, 'violet')}`;
    const recent = orders.slice(0, 5);
    document.getElementById('dashOrders').innerHTML = recent.length
      ? `<div class="scroll-x"><table class="table wagba"><thead><tr><th>#</th><th>Customer</th><th>Total</th><th>Status</th><th></th></tr></thead><tbody>`
        + recent.map(o => `<tr><td>${o.id}</td><td>${escapeHtml(o.customerName || 'Customer')}</td><td>${money(o.totalPrice)}</td><td>${statusBadge(o.status)}</td><td><button class="btn btn-sm btn-soft" onclick="navTo('orders')">View</button></td></tr>`).join('') + `</tbody></table></div>`
      : emptyState('bi-receipt', 'No orders yet', 'Orders will appear here once customers start ordering.');
  } catch (e) { /* ignore */ }
}
function statCard(icon, label, val, cls) {
  return `<div class="col-6 col-lg-3"><div class="stat-card ${cls}"><div class="ic"><i class="bi bi-${icon}"></i></div><div class="label">${label}</div><div class="val">${val}</div></div></div>`;
}

// ---------- Categories ----------
function loadCategories() {
  const cats = window._cats || [];
  document.getElementById('catList').innerHTML = cats.length
    ? cats.map(c => `<div class="d-flex justify-between align-center p-2 mb-2" style="background:var(--surface-2);border-radius:10px">
        <span><i class="bi bi-tag me-2 text-muted"></i>${escapeHtml(c.name)}</span>
        <button class="icon-btn" style="width:30px;height:30px" onclick="deleteCategory(${c.id})"><i class="bi bi-trash"></i></button></div>`).join('')
    : '<p class="muted small">No categories yet.</p>';
}
async function addCategory() {
  try {
    const c = await api('/restaurant-owner/categories', 'POST', { name: document.getElementById('catName').value });
    document.getElementById('catName').value = '';
    window._cats.push(c); loadCategories(); toast('Added', 'Category created');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function deleteCategory(id) {
  try { await api('/restaurant-owner/categories/' + id, 'DELETE'); window._cats = window._cats.filter(c => c.id !== id); loadCategories(); }
  catch (e) { showAlert('alertBox', 'danger', e.message); }
}

// ---------- Foods ----------
async function loadFoods() {
  if (!window._cats || !window._cats.length) { try { const r = await api('/restaurant-owner/restaurant'); window._cats = r.categories || []; } catch (e) {} }
  try {
    const foods = await api('/restaurant-owner/foods');
    const rows = document.getElementById('foodRows');
    if (!foods.length) { rows.innerHTML = `<tr><td colspan="4" class="muted text-center py-3">No foods yet.</td></tr>`; return; }
    rows.innerHTML = foods.map(f => `<tr>
      <td><div class="cell-name"><div class="cell-avatar"><i class="bi bi-egg-fried"></i></div>${escapeHtml(f.name)}</div></td>
      <td>${money(f.price)}</td><td>${escapeHtml(f.categoryName || '-')}</td>
      <td class="text-end"><button class="btn btn-sm btn-soft" onclick="openFoodModal(${f.id})">Edit</button>
          <button class="btn btn-sm btn-outline-danger" onclick="deleteFood(${f.id})">Delete</button></td></tr>`).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
function openFoodModal(id) {
  clearAlert('foodAlert');
  const cats = window._cats || [];
  document.getElementById('fCat').innerHTML = cats.length ? cats.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('') : `<option value="">— no category —</option>`;
  if (id) {
    api('/restaurant-owner/foods').then(foods => {
      const f = foods.find(x => x.id === id);
      document.getElementById('fId').value = f.id; document.getElementById('fName').value = f.name;
      document.getElementById('fDesc').value = f.description || ''; document.getElementById('fPrice').value = f.price;
      document.getElementById('fImg').value = f.imageUrl || ''; document.getElementById('fCat').value = f.categoryId || '';
      new bootstrap.Modal(document.getElementById('foodModal')).show();
    });
  } else { ['fId','fName','fDesc','fPrice','fImg'].forEach(i => document.getElementById(i).value = ''); new bootstrap.Modal(document.getElementById('foodModal')).show(); }
}
async function saveFood() {
  clearAlert('foodAlert');
  const id = document.getElementById('fId').value;
  const body = { name: document.getElementById('fName').value, description: document.getElementById('fDesc').value, price: document.getElementById('fPrice').value, imageUrl: document.getElementById('fImg').value, categoryId: document.getElementById('fCat').value || null };
  try {
    if (id) await api('/restaurant-owner/foods/' + id, 'PUT', body); else await api('/restaurant-owner/foods', 'POST', body);
    bootstrap.Modal.getInstance(document.getElementById('foodModal')).hide(); toast('Saved', 'Food updated'); loadFoods();
  } catch (e) { showAlert('foodAlert', 'danger', e.message); }
}
async function deleteFood(id) {
  try { await api('/restaurant-owner/foods/' + id, 'DELETE'); toast('Deleted', 'Food removed'); loadFoods(); }
  catch (e) { showAlert('alertBox', 'danger', e.message); }
}

// ---------- Orders ----------
async function loadOrders() {
  try {
    const list = await api('/restaurant-owner/orders');
    const el = document.getElementById('orderList');
    if (!list.length) { el.innerHTML = emptyState('bi-receipt', 'No orders yet', 'New orders will appear here in real time.'); return; }
    el.innerHTML = list.map(o => `
      <div class="card mb-3 fade-in"><div class="card-body">
        <div class="d-flex justify-between align-center mb-2">
          <div><b>Order #${o.id}</b> · ${escapeHtml(o.customerName || 'Customer')}<div class="muted small">${fmtDateTime(o.createdAt)}</div></div>
          ${statusBadge(o.status)}
        </div>
        <div class="row g-2 mb-2">${o.items.map(i => `<div class="col-md-6"><div class="d-flex gap-2 align-center"><div class="cell-avatar" style="width:30px;height:30px;font-size:.9rem"><i class="bi bi-egg-fried"></i></div><div class="small">${escapeHtml(i.foodName)} × ${i.quantity}</div></div></div>`).join('')}</div>
        <div class="d-flex justify-between align-center">
          <b>${money(o.totalPrice)}</b>
          <div class="d-flex gap-2">
            ${o.status === 'PENDING' ? `<button class="btn btn-sm btn-success" onclick="actOrder(${o.id},'accept')">Accept</button><button class="btn btn-sm btn-danger" onclick="actOrder(${o.id},'reject')">Reject</button>` : ''}
            ${o.status === 'ACCEPTED' ? `<span class="badge bg-info">Accepted — awaiting fulfilment</span>` : ''}
          </div>
        </div>
      </div></div>`).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function actOrder(id, action) {
  try { await api('/restaurant-owner/orders/' + id + '/' + action, 'POST'); toast('Order ' + action + 'ed', '#' + id); loadOrders(); }
  catch (e) { showAlert('alertBox', 'danger', e.message); }
}
function emptyState(icon, title, sub) {
  return `<div class="empty-state"><div class="ico"><i class="bi bi-${icon}"></i></div><div class="fw-semibold">${escapeHtml(title)}</div><div class="small">${escapeHtml(sub || '')}</div></div>`;
}
