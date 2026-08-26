// ===================== Restaurant Owner page =====================
boot('RESTAURANT_OWNER', init);

function init() {
  window.VIEWS = {
    dashboard: { title: 'Dashboard', sub: 'Overview of your restaurant' },
    menu: { title: 'Menu', sub: 'Manage categories & dishes' },
    orders: { title: 'Orders', sub: 'Accept and fulfil orders' },
    earnings: { title: 'Earnings', sub: 'Wallet & payouts' },
    settings: { title: 'Settings', sub: 'Manage your account' }
  };
  window.onNav = (v) => { if (v === 'menu') { loadCategories(); loadFoods(); } if (v === 'orders') loadOrders(); if (v === 'earnings') loadEarnings(); if (v === 'settings') renderSettings(); };
  window.__realtimeRefresh = () => { loadOrders(); loadRestaurant(); };
  navTo('dashboard');
  loadRestaurant();
}

const OWNER_PROFILE_PANEL = `
<div class="card detail-card mb-3"><div class="card-body">
  <div class="section-head mb-3"><span class="sh-ic"><i class="bi bi-shop-window"></i></span><div><h5 class="mb-0">Restaurant info</h5><div class="text-muted small">What customers see on your storefront</div></div></div>
  <div id="ownAlert"></div>
  <div class="row g-2">
    <div class="col-sm-6 mb-2"><label class="form-label">Name</label><input id="ownName" class="form-control"></div>
    <div class="col-sm-6 mb-2"><label class="form-label">Cuisine</label><input id="ownCuisine" class="form-control"></div>
    <div class="col-12 mb-2"><label class="form-label">Description</label><textarea id="ownDesc" class="form-control" rows="2"></textarea></div>
    <div class="col-sm-6 mb-2"><label class="form-label">Phone</label><input id="ownPhone" class="form-control"></div>
    <div class="col-sm-6 mb-2"><label class="form-label">ETA (minutes)</label><input id="ownEta" type="number" class="form-control"></div>
    <div class="col-sm-6 mb-2"><label class="form-label">Delivery fee (EGP)</label><input id="ownFee" type="number" step="0.01" class="form-control"></div>
    <div class="col-sm-6 mb-2"><label class="form-label">Min order (EGP)</label><input id="ownMin" type="number" step="0.01" class="form-control"></div>
    <div class="col-12 mb-2"><label class="form-label">Image URL</label>
      <input id="pfImg" class="form-control">
      <div class="mt-2 d-flex gap-2 align-center">
        <input type="file" id="pfImgFile" accept="image/*" class="form-control form-control-sm">
        <button class="btn btn-sm btn-soft" type="button" onclick="uploadImage('pfImg','pfImgFile')"><i class="bi bi-upload"></i> Upload</button>
      </div>
    </div>
  </div>
  <button class="btn-brand" onclick="saveOwnerSettings()"><i class="bi bi-check-lg"></i> Save restaurant info</button>
</div></div>

<div class="card detail-card"><div class="card-body">
  <div class="d-flex justify-between align-center mb-2">
    <div class="section-head mb-0"><span class="sh-ic"><i class="bi bi-geo-alt"></i></span><div><h5 class="mb-0">Restaurant location</h5></div></div>
    <button type="button" class="btn btn-sm btn-soft" onclick="useRestaurantLocation()"><i class="bi bi-geo-alt"></i> Use my location</button>
  </div>
  <div class="d-flex gap-2 mb-2">
    <input id="pfSearch" class="form-control" placeholder="Search your address (e.g. Tahrir, Cairo)">
    <button class="btn btn-soft btn-sm" type="button" onclick="searchRestaurantAddress()"><i class="bi bi-search"></i></button>
  </div>
  <div id="profileMap" class="map-picker"></div>
  <div class="muted small mt-1">Search your address or tap the map to drop your restaurant's pin.</div>
  <div class="row g-2 mt-3">
    <div class="col-6"><label class="form-label">City</label><input id="pfCity" class="form-control"></div>
    <div class="col-6"><label class="form-label">Street</label><input id="pfStreet" class="form-control"></div>
    <div class="col-4"><label class="form-label">Building</label><input id="pfBuilding" class="form-control"></div>
    <div class="col-4"><label class="form-label">Apartment</label><input id="pfApartment" class="form-control"></div>
    <div class="col-4"><label class="form-label">Landmark</label><input id="pfDetails" class="form-control"></div>
  </div>
  <button class="btn-brand mt-3" onclick="saveOwnerSettings()"><i class="bi bi-check-lg"></i> Save location</button>
</div></div>`;

function renderSettings() {
  const root = document.getElementById('view-settings');
  if (!root) return;
  if (profileMap) { profileMap.remove(); profileMap = null; profileMarker = null; }
  root.innerHTML = renderSettingsShell({ panel: OWNER_PROFILE_PANEL, label: 'Restaurant', icon: 'shop-window' });
  loadAccountSettings();
  loadOwnerSettings();
  window.__onSettingsCat = (c) => { if (c === 'role') { initProfileMap(); setTimeout(() => profileMap && profileMap.invalidateSize(), 60); } };
}
async function loadOwnerSettings() {
  try {
    const r = await api('/restaurant-owner/restaurant');
    const s = (v) => v == null ? '' : String(v);
    window._rest = r;
    if (document.getElementById('ownName')) document.getElementById('ownName').value = s(r.name);
    if (document.getElementById('ownCuisine')) document.getElementById('ownCuisine').value = s(r.cuisine);
    if (document.getElementById('ownDesc')) document.getElementById('ownDesc').value = s(r.description);
    if (document.getElementById('ownPhone')) document.getElementById('ownPhone').value = s(r.phone);
    if (document.getElementById('ownEta')) document.getElementById('ownEta').value = s(r.etaMinutes);
    if (document.getElementById('ownFee')) document.getElementById('ownFee').value = s(r.deliveryFee);
    if (document.getElementById('ownMin')) document.getElementById('ownMin').value = s(r.minOrderTotal);
    if (document.getElementById('pfImg')) document.getElementById('pfImg').value = s(r.imageUrl);
    if (document.getElementById('pfCity')) document.getElementById('pfCity').value = s(r.city);
    if (document.getElementById('pfStreet')) document.getElementById('pfStreet').value = s(r.street);
    if (document.getElementById('pfBuilding')) document.getElementById('pfBuilding').value = s(r.buildingNumber);
    if (document.getElementById('pfApartment')) document.getElementById('pfApartment').value = s(r.apartment);
    if (document.getElementById('pfDetails')) document.getElementById('pfDetails').value = s(r.details);
  } catch (e) { showAlert('ownAlert', 'danger', e.message); }
}
async function saveOwnerSettings() {
  clearAlert('ownAlert');
  const g = (id) => { const el = document.getElementById(id); return el ? el.value.trim() : ''; };
  const body = {
    name: g('ownName'),
    cuisine: g('ownCuisine'),
    description: g('ownDesc'),
    phone: g('ownPhone'),
    etaMinutes: g('ownEta') ? parseInt(g('ownEta')) : null,
    deliveryFee: g('ownFee') ? parseFloat(g('ownFee')) : null,
    minOrderTotal: g('ownMin') ? parseFloat(g('ownMin')) : null,
    imageUrl: g('pfImg'),
    city: g('pfCity') || null,
    street: g('pfStreet') || null,
    buildingNumber: g('pfBuilding') || null,
    apartment: g('pfApartment') || null,
    details: g('pfDetails') || null,
    latitude: profileMarker ? profileMarker.getLatLng().lat : (window._rest && window._rest.latitude),
    longitude: profileMarker ? profileMarker.getLatLng().lng : (window._rest && window._rest.longitude)
  };
  try {
    await api('/restaurant-owner/restaurant', 'PUT', body);
    toast('Saved', 'Restaurant updated');
  } catch (e) { showAlert('ownAlert', 'danger', e.message); }
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
        <div class="col-md-6"><label class="form-label">Image URL</label>
          <input id="pImg" class="form-control">
          <div class="mt-2 d-flex gap-2 align-center">
            <input type="file" id="pImgFile" accept="image/*" class="form-control form-control-sm">
            <button class="btn btn-sm btn-soft" type="button" onclick="uploadImage('pImg','pImgFile')"><i class="bi bi-upload"></i> Upload</button>
          </div>
        </div>
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
    const op = await api('/restaurant-owner/orders?page=0&size=100');
    const orders = op.content || [];
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


// ---------- Categories ----------
function loadCategories() {
  const cats = window._cats || [];
  const el = document.getElementById('catList');
  if (el) el.innerHTML = cats.length
    ? cats.map(c => `<div class="cat-item">
        <span class="cat-ic"><i class="bi bi-tag"></i></span>
        <span class="cat-name">${escapeHtml(c.name)}</span>
        <button class="icon-btn cat-del" onclick="deleteCategory(${c.id})" title="Delete"><i class="bi bi-trash"></i></button>
      </div>`).join('')
    : '<p class="muted small mb-0">No categories yet.</p>';
  const cc = document.getElementById('catCount');
  if (cc) cc.textContent = cats.length + (cats.length === 1 ? ' category' : ' categories');
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
    const el = document.getElementById('foodRows');
    const fc = document.getElementById('foodCount');
    if (fc) fc.textContent = foods.length + (foods.length === 1 ? ' item' : ' items');
    if (!foods.length) { el.innerHTML = emptyState('bi-egg-fried', 'No menu items yet', 'Add your first dish to start selling.'); return; }
    el.innerHTML = foods.map(f => {
      const pic = f.imageUrl ? `<img src="${escapeHtml(f.imageUrl)}" onerror="onImgError(this)" data-label="${escapeHtml(f.name)}">` : `<i class="bi bi-egg-fried"></i>`;
      const priceHtml = f.offer
        ? `<span class="old-price">${money(f.price)}</span> <span class="new-price">${money(f.discountPrice)}</span>`
        : money(f.price);
      const avail = f.available === false
        ? '<span class="badge bg-secondary">Unavailable</span>'
        : '<span class="badge bg-success">Available</span>';
      return `<div class="food-card">
        <div class="pic">${pic}</div>
        <div class="body">
          <div class="d-flex justify-between align-center gap-2">
            <div class="name text-truncate">${escapeHtml(f.name)}</div>
            <button class="btn btn-sm ${f.available === false ? 'btn-outline-secondary' : 'btn-outline-success'}" onclick="toggleFoodAvail(${f.id})" title="${f.available === false ? 'Enable' : 'Disable'}">
              <i class="bi bi-${f.available === false ? 'slash-circle' : 'check-circle'}"></i>
            </button>
          </div>
          <div class="desc">${escapeHtml(f.categoryName || 'Uncategorized')}</div>
          <div class="row">
            <span class="price">${priceHtml}</span>
            <div class="d-flex gap-1">
              <button class="btn btn-sm btn-soft" onclick="openFoodModal(${f.id})" title="Edit"><i class="bi bi-pencil"></i></button>
              <button class="btn btn-sm btn-outline-danger" onclick="deleteFood(${f.id})" title="Delete"><i class="bi bi-trash"></i></button>
            </div>
          </div>
        </div>
      </div>`;
    }).join('');
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
      document.getElementById('fDiscount').value = f.discountPrice != null ? f.discountPrice : '';
      document.getElementById('fImg').value = f.imageUrl || ''; document.getElementById('fCat').value = f.categoryId || '';
      document.getElementById('fAvailable').checked = f.available !== false;
      new bootstrap.Modal(document.getElementById('foodModal')).show();
    });
  } else { ['fId','fName','fDesc','fPrice','fImg'].forEach(i => document.getElementById(i).value = ''); document.getElementById('fDiscount').value=''; document.getElementById('fAvailable').checked = true; new bootstrap.Modal(document.getElementById('foodModal')).show(); }
}
async function saveFood() {
  clearAlert('foodAlert');
  const id = document.getElementById('fId').value;
  const body = { name: document.getElementById('fName').value, description: document.getElementById('fDesc').value, price: document.getElementById('fPrice').value, imageUrl: document.getElementById('fImg').value, categoryId: document.getElementById('fCat').value || null, discountPrice: document.getElementById('fDiscount').value || null, available: document.getElementById('fAvailable').checked };
  try {
    if (id) await api('/restaurant-owner/foods/' + id, 'PUT', body); else await api('/restaurant-owner/foods', 'POST', body);
    bootstrap.Modal.getInstance(document.getElementById('foodModal')).hide(); toast('Saved', 'Food updated'); loadFoods();
  } catch (e) { showAlert('foodAlert', 'danger', e.message); }
}
async function deleteFood(id) {
  try { await api('/restaurant-owner/foods/' + id, 'DELETE'); toast('Deleted', 'Food removed'); loadFoods(); }
  catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function toggleFoodAvail(id) {
  try {
    const res = await api('/restaurant-owner/foods/' + id + '/availability', 'PUT');
    toast('Updated', res.name + ' is now ' + (res.available ? 'available' : 'unavailable'));
    loadFoods();
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

// ---------- Restaurant profile (location map, used inside Settings) ----------
let profileMap = null, profileMarker = null;
function initProfileMap() {
  const el = document.getElementById('profileMap');
  if (!el || profileMap) return;
  const lat = window._rest && window._rest.latitude ? window._rest.latitude : 30.0444;
  const lng = window._rest && window._rest.longitude ? window._rest.longitude : 31.2357;
  profileMap = L.map(el).setView([lat, lng], window._rest && window._rest.latitude ? 15 : 11);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(profileMap);
  if (window._rest && window._rest.latitude) setProfileMarker(lat, lng);
  profileMap.on('click', (e) => setProfileMarker(e.latlng.lat, e.latlng.lng));
  setTimeout(() => profileMap.invalidateSize(), 200);
}
function setProfileMarker(lat, lng) {
  if (!profileMap) return;
  if (!profileMarker) profileMarker = L.marker([lat, lng]).addTo(profileMap);
  else profileMarker.setLatLng([lat, lng]);
  profileMap.setView([lat, lng], 15);
}
function useRestaurantLocation() {
  if (!navigator.geolocation) { toast('Location', 'Geolocation not supported', 'err'); return; }
  navigator.geolocation.getCurrentPosition(
    pos => { if (!profileMap) initProfileMap(); setProfileMarker(pos.coords.latitude, pos.coords.longitude); },
    err => toast('Location', 'Could not get location: ' + err.message, 'err'),
    { enableHighAccuracy: true, timeout: 10000 }
  );
}
async function searchRestaurantAddress() {
  const q = document.getElementById('pfSearch').value.trim();
  if (!q) { toast('Search', 'Type an address first', 'err'); return; }
  try {
    const res = await fetch('https://nominatim.openstreetmap.org/search?format=json&limit=1&q=' + encodeURIComponent(q));
    const data = await res.json();
    if (!data.length) { toast('Not found', 'No match for that address', 'err'); return; }
    const lat = parseFloat(data[0].lat), lng = parseFloat(data[0].lon);
    if (!profileMap) initProfileMap();
    setProfileMarker(lat, lng);
    const a = data[0].address || {};
    if (a.city || a.town || a.village) document.getElementById('pfCity').value = a.city || a.town || a.village;
    if (a.road) document.getElementById('pfStreet').value = a.road;
    if (a.house_number) document.getElementById('pfBuilding').value = a.house_number;
    if (a.suburb || a.neighbourhood) document.getElementById('pfDetails').value = a.suburb || a.neighbourhood;
    toast('Location set', data[0].display_name);
  } catch (e) { toast('Error', e.message, 'err'); }
}

// ---------- Earnings / Wallet ----------
async function loadEarnings() {
  try {
    const [w, chart] = await Promise.all([
      api('/restaurant-owner/wallet'),
      api('/restaurant-owner/earnings/chart?days=30').catch(() => [])
    ]);
    window._ownerBalance = w.balance != null ? w.balance : 0;
    document.getElementById('walletBalance').textContent = money(window._ownerBalance);
    const chartEl = document.getElementById('earningsChart');
    if (chartEl && chart.length) {
      const maxAmt = Math.max(...chart.map(c => c.amount), 1);
      chartEl.innerHTML = chart.map(c => {
        const pct = Math.round((c.amount / maxAmt) * 100);
        return `<div class="chart-bar" style="height:${Math.max(pct, 2)}%" title="${c.date}: ${money(c.amount)}"><span class="chart-label">${c.date.slice(5)}</span></div>`;
      }).join('');
    }
    const txns = w.transactions || [];
    document.getElementById('walletTxns').innerHTML = txns.length
      ? `<table class="table wagba"><thead><tr><th>Date</th><th>Type</th><th>Description</th><th class="text-end">Amount</th></tr></thead><tbody>`
        + txns.map(t => `<tr><td>${fmtDateTime(t.createdAt)}</td><td>${t.type}</td><td>${escapeHtml(t.description || '')}</td><td class="text-end ${t.type === 'CREDIT' ? 'text-success' : 'text-danger'} fw-semibold">${t.type === 'CREDIT' ? '+' : '-'}${money(t.amount)}</td></tr>`).join('')
        + `</tbody></table>`
      : '<div class="empty-txns"><i class="bi bi-receipt"></i><div>No transactions yet</div><div class="small">You\'ll be paid into your wallet when card orders are delivered.</div></div>';
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

async function openWithdraw() {
  clearAlert('withdrawAlert');
  document.getElementById('wdAmount').value = '';
  document.getElementById('wdAvail').textContent = money(window._ownerBalance || 0);
  new bootstrap.Modal(document.getElementById('withdrawModal')).show();
}
async function withdrawEarnings() {
  clearAlert('withdrawAlert');
  const amt = parseFloat(document.getElementById('wdAmount').value);
  if (!amt || amt <= 0) { showAlert('withdrawAlert', 'danger', 'Enter a valid amount'); return; }
  if (amt > (window._ownerBalance || 0)) { showAlert('withdrawAlert', 'danger', 'Amount exceeds your wallet balance'); return; }
  try {
    const res = await api('/restaurant-owner/wallet/withdraw', 'POST', { amount: amt });
    bootstrap.Modal.getInstance(document.getElementById('withdrawModal')).hide();
    toast('Withdrawal ' + (res.devMode ? '(demo)' : '') + ' sent', 'Stripe payout ' + (res.status || 'processed') + ' · ' + money(amt));
    loadEarnings();
  } catch (e) { showAlert('withdrawAlert', 'danger', e.message); }
}

// ---------- Image upload (#6) ----------
async function uploadImage(targetId, fileId) {
  const fileInput = document.getElementById(fileId);
  if (!fileInput || !fileInput.files || !fileInput.files[0]) { toast('No file', 'Choose an image first', 'err'); return; }
  const fd = new FormData();
  fd.append('file', fileInput.files[0]);
  try {
    const res = await fetch(getApiBase() + '/files/upload', {
      method: 'POST',
      headers: { Authorization: 'Bearer ' + getToken() },
      body: fd
    });
    const data = await res.json();
    if (!res.ok || !data.url) throw new Error(data.error || 'Upload failed');
    document.getElementById(targetId).value = data.url;
    toast('Uploaded', 'Image URL set — save to apply');
  } catch (e) { toast('Upload failed', e.message, 'err'); }
}

// ---------- Orders ----------
async function loadOrders() {
  try {
    showSkeletons('orderList', 4, 132);
    const op = await api('/restaurant-owner/orders?page=0&size=100');
    const list = op.content || [];
    const el = document.getElementById('orderList');
    if (!list.length) { el.innerHTML = emptyState('bi-receipt', 'No orders yet', 'New orders will appear here in real time.'); return; }
    el.innerHTML = list.map(o => `
      <div class="card order-card mb-3 fade-in"><div class="card-body">
        <div class="oc-top">
          <div class="oc-ic"><i class="bi bi-receipt"></i></div>
          <div style="flex:1;min-width:0">
            <div class="oc-title">Order #${o.id}</div>
            <div class="oc-sub">${escapeHtml(o.customerName || 'Customer')} · ${fmtDateTime(o.createdAt)}</div>
          </div>
          <div class="oc-badges">${statusBadge(o.status)}</div>
        </div>
        <div class="oc-meta">
          ${o.deliveryAddress ? `<div class="m"><i class="bi bi-geo-alt"></i> ${escapeHtml(formatAddr(o.deliveryAddress))}</div>` : ''}
          <div class="m"><i class="bi bi-bag"></i> ${o.items.length} item${o.items.length === 1 ? '' : 's'}</div>
        </div>
        <div class="oc-foot">
          <div class="oc-total">${money(o.totalPrice)}</div>
          <div class="d-flex gap-2">
            ${o.status === 'PENDING' ? `<button class="btn btn-sm btn-success" onclick="actOrder(${o.id},'accept')">Accept</button><button class="btn btn-sm btn-danger" onclick="actOrder(${o.id},'reject')">Reject</button>` : ''}
            ${o.status === 'ACCEPTED' ? `<button class="btn btn-sm btn-brand" onclick="setStatus(${o.id},'PREPARING')"><i class="bi bi-egg-fried"></i> Start preparing</button>` : ''}
            ${o.status === 'PREPARING' ? `<button class="btn btn-sm btn-brand" onclick="setStatus(${o.id},'READY')"><i class="bi bi-check2-circle"></i> Ready</button>` : ''}
            ${o.status === 'READY' ? `<button class="btn btn-sm btn-brand" onclick="setStatus(${o.id},'OUT_FOR_DELIVERY')"><i class="bi bi-bicycle"></i> Out for delivery</button>` : ''}
          </div>
        </div>
      </div></div>`).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function actOrder(id, action) {
  try { await api('/restaurant-owner/orders/' + id + '/' + action, 'POST'); toast('Order ' + action + 'ed', '#' + id); loadOrders(); }
  catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function setStatus(id, status) {
  try { await api('/restaurant-owner/orders/' + id + '/status?status=' + status, 'PUT'); toast('Updated', 'Order #' + id + ' → ' + status); loadOrders(); }
  catch (e) { showAlert('alertBox', 'danger', e.message); }
}

