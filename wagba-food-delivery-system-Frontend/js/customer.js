// ===================== Customer page =====================
boot('CUSTOMER', init);

function init() {
  window.VIEWS = {
    restaurants: { title: 'Restaurants', sub: 'Discover kitchens near you' },
    cart: { title: 'My Cart', sub: 'Review and checkout your items' },
    orders: { title: 'My Orders', sub: 'Track and manage your orders' },
    coupons: { title: 'My Coupons', sub: 'Your saved offers' },
    reviews: { title: 'My Reviews', sub: 'What you thought' }
  };
  window.onNav = (v) => { if (v === 'cart') loadCart(); if (v === 'orders') loadOrders(); if (v === 'coupons') loadCoupons(); if (v === 'reviews') loadReviews(); };
  navTo('restaurants');
  loadRestaurants();
  loadCart();
}

async function loadRestaurants() {
  try {
    const list = await api('/restaurants');
    window._rests = list || [];
    const cuis = [...new Set((list || []).map(r => r.cuisine).filter(Boolean))];
    document.getElementById('filterCuisine').innerHTML = '<option value="">All cuisines</option>' + cuis.map(c => `<option>${escapeHtml(c)}</option>`).join('');
    renderRestaurants(list || []);
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

function renderRestaurants(list) {
  const el = document.getElementById('restaurantList');
  if (!list.length) { el.innerHTML = emptyState('bi-shop', 'No restaurants found', 'Try a different search.'); return; }
  el.innerHTML = list.map(r => `
    <div class="rest-card" onclick="viewRestaurant(${r.id})">
      <div class="cover">${r.imageUrl ? `<img src="${escapeHtml(r.imageUrl)}" onerror="onImgError(this,'${escapeHtml(r.name)}')">` : `<i class="bi bi-shop"></i>`}</div>
      <div class="body">
        <div class="d-flex justify-between align-center">
          <div class="name">${escapeHtml(r.name)}</div>
          ${r.avgRating ? `<span class="small fw-semibold"><i class="bi bi-star-fill text-warning"></i> ${r.avgRating}</span>` : ''}
        </div>
        <div class="desc muted small text-truncate">${escapeHtml(r.description || 'Delicious food, delivered fast')}</div>
        <div class="meta">
          ${r.cuisine ? `<span><i class="bi bi-tags"></i> ${escapeHtml(r.cuisine)}</span>` : ''}
          ${r.etaMinutes ? `<span><i class="bi bi-clock"></i> ${r.etaMinutes} min</span>` : ''}
          ${r.deliveryFee != null ? `<span><i class="bi bi-bicycle"></i> ${money(r.deliveryFee)}</span>` : ''}
          ${r.minOrderTotal ? `<span><i class="bi bi-bag"></i> min ${money(r.minOrderTotal)}</span>` : ''}
        </div>
        <div class="mt-2">${r.status ? statusBadge(r.status.name || r.status) : ''}</div>
      </div>
    </div>`).join('');
}

function applyFilters() {
  const q = (document.getElementById('searchInput').value || '').toLowerCase();
  const cu = document.getElementById('filterCuisine').value;
  const sort = document.getElementById('sortBy').value;
  let list = (window._rests || []).filter(r => {
    const matchQ = !q || (r.name || '').toLowerCase().includes(q) || (r.description || '').toLowerCase().includes(q);
    const matchC = !cu || r.cuisine === cu;
    return matchQ && matchC;
  });
  if (sort === 'rating') list = [...list].sort((a, b) => (b.avgRating || 0) - (a.avgRating || 0));
  else if (sort === 'minOrder') list = [...list].sort((a, b) => (a.minOrderTotal || 0) - (b.minOrderTotal || 0));
  else list = [...list].sort((a, b) => (a.name || '').localeCompare(b.name || ''));
  renderRestaurants(list);
}

async function viewRestaurant(id) {
  try {
    const foods = await api('/restaurants/' + id + '/foods');
    const r = (window._rests || []).find(x => x.id === id) || {};
    document.getElementById('restaurantList').classList.add('d-none');
    const fv = document.getElementById('foodView');
    fv.classList.remove('d-none');
    document.getElementById('foodRestaurantName').textContent = r.name || 'Menu';
    document.getElementById('restMeta').innerHTML =
      `${r.cuisine ? escapeHtml(r.cuisine) + ' · ' : ''}${r.etaMinutes ? r.etaMinutes + ' min · ' : ''}${r.deliveryFee != null ? 'Delivery ' + money(r.deliveryFee) : ''}`;
    document.getElementById('restStatus').innerHTML = r.status ? statusBadge(r.status.name || r.status) : '';
    const fl = document.getElementById('foodList');
    if (!foods.length) { fl.innerHTML = emptyState('bi-egg-fried', 'No dishes yet', 'This restaurant has no items right now.'); return; }
    fl.innerHTML = foods.map(f => `
      <div class="food-card">
        <div class="pic">${f.imageUrl ? `<img src="${escapeHtml(f.imageUrl)}" onerror="onImgError(this,'${escapeHtml(f.name)}')">` : `<i class="bi bi-egg-fried"></i>`}</div>
        <div class="body">
          <div class="d-flex justify-between"><div class="name">${escapeHtml(f.name)}</div>${f.categoryName ? `<span class="badge-soft">${escapeHtml(f.categoryName)}</span>` : ''}</div>
          <div class="desc">${escapeHtml(f.description || '')}</div>
          <div class="row">
            <span class="price">${money(f.price)}</span>
            ${f.available
              ? `<button class="btn-brand btn-sm" onclick="addToCart(${f.id})"><i class="bi bi-plus-lg"></i> Add</button>`
              : `<span class="badge bg-secondary">Unavailable</span>`}
          </div>
        </div>
      </div>`).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

function backToRestaurants() {
  document.getElementById('foodView').classList.add('d-none');
  document.getElementById('restaurantList').classList.remove('d-none');
  loadRestaurants();
}

async function addToCart(foodId) {
  try {
    await api('/cart/items', 'POST', { foodId, quantity: 1 });
    toast('Added', 'Item added to your cart');
    loadCart();
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

async function loadCart() {
  try {
    const c = await api('/cart/items');
    const el = document.getElementById('cartList');
    const pill = document.getElementById('cartPill');
    const cnt = document.getElementById('cartCount');
    if (!c.items || !c.items.length) {
      el.innerHTML = emptyState('bi-cart3', 'Your cart is empty', 'Add something tasty from a restaurant.');
      document.getElementById('cartSummary').querySelector('.card-body').innerHTML = '';
      if (pill) pill.classList.add('d-none');
      if (cnt) cnt.classList.add('d-none');
      return;
    }
    if (pill) { pill.classList.remove('d-none'); document.getElementById('cartTotalPill').textContent = money(c.total); }
    if (cnt) { cnt.classList.remove('d-none'); cnt.textContent = c.items.length; }
    el.innerHTML = `<div class="row g-2">` + c.items.map(i => `
      <div class="col-12">
        <div class="card"><div class="card-body d-flex align-center gap-3">
          <div class="cell-avatar"><i class="bi bi-egg-fried"></i></div>
          <div class="flex-1">
            <div class="fw-semibold">${escapeHtml(i.foodName)}</div>
            <div class="muted small">${money(i.price)} each</div>
          </div>
          <div class="d-flex align-center gap-2">
            <button class="icon-btn" style="width:32px;height:32px" onclick="updateQty(${i.id},${i.foodId},${i.quantity - 1})"><i class="bi bi-dash"></i></button>
            <span class="fw-semibold" style="min-width:22px;text-align:center">${i.quantity}</span>
            <button class="icon-btn" style="width:32px;height:32px" onclick="updateQty(${i.id},${i.foodId},${i.quantity + 1})"><i class="bi bi-plus"></i></button>
          </div>
          <div class="fw-bold" style="min-width:80px;text-align:right">${money(i.subtotal)}</div>
          <button class="icon-btn" style="width:32px;height:32px;color:var(--danger)" onclick="removeItem(${i.id})"><i class="bi bi-trash"></i></button>
        </div></div>
      </div>`).join('') + `</div>`;
    document.getElementById('cartSummary').querySelector('.card-body').innerHTML = `
      <h5 class="mb-3">Summary</h5>
      <div class="d-flex justify-between"><span class="muted">Subtotal</span><b>${money(c.total)}</b></div>
      <hr>
      <div class="d-grid"><button class="btn-brand" data-bs-toggle="modal" data-bs-target="#checkoutModal">Checkout · ${money(c.total)}</button></div>`;
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

async function updateQty(itemId, foodId, qty) {
  if (qty < 1) return removeItem(itemId);
  try { await api('/cart/items/' + itemId, 'PUT', { foodId, quantity: parseInt(qty) }); loadCart(); }
  catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function removeItem(itemId) {
  try { await api('/cart/items/' + itemId, 'DELETE'); loadCart(); }
  catch (e) { showAlert('alertBox', 'danger', e.message); }
}

async function submitCheckout() {
  clearAlert('checkoutAlert');
  const body = {
    city: document.getElementById('ckCity').value,
    street: document.getElementById('ckStreet').value,
    buildingNumber: document.getElementById('ckBuilding').value,
    apartment: document.getElementById('ckApartment').value,
    details: document.getElementById('ckDetails').value,
    couponCode: document.getElementById('ckCoupon').value || null
  };
  try {
    const order = await api('/orders/checkout', 'POST', body);
    bootstrap.Modal.getInstance(document.getElementById('checkoutModal')).hide();
    toast('Order placed', 'Order #' + order.id + ' · ' + order.status);
    loadCart();
    navTo('orders'); loadOrders();
  } catch (e) { showAlert('checkoutAlert', 'danger', e.message); }
}

async function loadOrders() {
  try {
    const list = await api('/orders');
    const el = document.getElementById('orderList');
    if (!list.length) { el.innerHTML = emptyState('bi-receipt', 'No orders yet', 'Your orders will appear here.'); return; }
    el.innerHTML = list.map(o => `
      <div class="card mb-3 fade-in"><div class="card-body">
        <div class="d-flex justify-between align-center mb-2">
          <div><b>Order #${o.id}</b> · ${escapeHtml(o.restaurantName || '')}<div class="muted small">${fmtDateTime(o.createdAt)}</div></div>
          <div>${statusBadge(o.status)} ${o.deliveryStatus ? statusBadge(o.deliveryStatus) : ''}</div>
        </div>
        <div class="row g-2">
          ${o.items.map(i => `<div class="col-md-6"><div class="d-flex gap-2 align-center"><div class="cell-avatar" style="width:30px;height:30px;font-size:.9rem"><i class="bi bi-egg-fried"></i></div><div class="small">${escapeHtml(i.foodName)} × ${i.quantity}</div></div></div>`).join('')}
        </div>
        <hr>
        <div class="d-flex justify-between align-center flex-wrap gap-2">
          <div><b>${money(o.totalPrice)}</b>${o.discountAmount ? ` <span class="text-success small">saved ${money(o.discountAmount)}${o.couponCode ? ' · ' + escapeHtml(o.couponCode) : ''}</span>` : ''}</div>
          <div class="d-flex gap-2">
            ${o.status === 'PENDING' ? `<button class="btn btn-sm btn-outline-danger" onclick="cancelOrder(${o.id})">Cancel</button>` : ''}
            ${(['PENDING','ACCEPTED','PREPARING','READY','OUT_FOR_DELIVERY'].includes(o.status)) ? `<button class="btn btn-sm btn-soft" onclick="payOrder(${o.id})"><i class="bi bi-credit-card"></i> Pay</button>` : ''}
            ${o.status === 'DELIVERED' ? `<button class="btn btn-sm btn-soft" onclick="openReview(${o.id})"><i class="bi bi-star"></i> Review</button>` : ''}
            ${(o.status !== 'DELIVERED' && o.status !== 'CANCELLED' && o.status !== 'REJECTED') ? `<button class="btn btn-sm btn-brand" onclick="trackOrder(${o.id})"><i class="bi bi-geo-alt"></i> Track</button>` : ''}
          </div>
        </div>
      </div></div>`).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

async function cancelOrder(id) {
  try { await api('/orders/' + id + '/cancel', 'POST'); toast('Cancelled', 'Order #' + id); loadOrders(); }
  catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function payOrder(id) {
  try {
    const res = await api('/payments/create-intent', 'POST', { orderId: id });
    const msg = res.devMode ? 'DEV mode: payment simulated (amount ' + money(res.amount) + ' ' + res.currency + ')' : 'Stripe clientSecret: ' + res.clientSecret;
    toast('Payment', msg);
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

async function loadReviews() {
  try {
    const list = await api('/reviews/mine');
    const el = document.getElementById('reviewList');
    if (!list.length) { el.innerHTML = emptyState('bi-star', 'No reviews yet', 'Review a delivered order.'); return; }
    el.innerHTML = list.map(r => `
      <div class="card mb-2"><div class="card-body">
        <div class="text-warning">${stars(r.rating)}</div>
        <div class="muted small">${escapeHtml(r.restaurantName || 'Restaurant')}</div>
        <div>${escapeHtml(r.comment || '')}</div>
      </div></div>`).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
function openReview(orderId) {
  document.getElementById('rvOrderId').value = orderId;
  clearAlert('reviewAlert');
  new bootstrap.Modal(document.getElementById('reviewModal')).show();
}
async function submitReview() {
  clearAlert('reviewAlert');
  const body = { orderId: parseInt(document.getElementById('rvOrderId').value), rating: parseInt(document.getElementById('rvRating').value), comment: document.getElementById('rvComment').value };
  try { await api('/reviews', 'POST', body); bootstrap.Modal.getInstance(document.getElementById('reviewModal')).hide(); toast('Thanks!', 'Review submitted'); loadReviews(); }
  catch (e) { showAlert('reviewAlert', 'danger', e.message); }
}

async function loadCoupons() {
  try {
    const list = await api('/coupons/mine');
    const el = document.getElementById('couponList');
    if (!list.length) { el.innerHTML = emptyState('bi-ticket-perforated', 'No coupons', 'Admin coupons assigned to you will show here.'); return; }
    el.innerHTML = list.map(c => `
      <div class="card mb-2"><div class="card-body d-flex justify-between align-center">
        <div><span class="badge bg-danger">${escapeHtml(c.code)}</span> <span class="ms-2">${escapeHtml(c.description || '')}</span><br>
          <small class="muted">${c.discountType} ${c.value}${c.discountType === 'PERCENTAGE' ? '%' : ' EGP'}${c.minOrderTotal ? ' · min ' + money(c.minOrderTotal) : ''} · expires ${c.expiryDate || 'never'}</small></div>
        <span class="badge ${c.used ? 'bg-secondary' : 'bg-success'}">${c.used ? 'Used' : 'Available'}</span>
      </div></div>`).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

// ---------- Live Tracking ----------
let trackTimer = null, trackMap = null, trackMarker = null;
async function trackOrder(id) {
  document.getElementById('trkId').textContent = id;
  clearAlert('trackAlert');
  new bootstrap.Modal(document.getElementById('trackModal')).show();
  await loadTracking(id);
  if (trackTimer) clearInterval(trackTimer);
  trackTimer = setInterval(() => loadTracking(id), 5000);
  document.getElementById('trackModal').addEventListener('hidden.bs.modal', () => {
    if (trackTimer) { clearInterval(trackTimer); trackTimer = null; }
    if (trackMap) { trackMap.remove(); trackMap = null; trackMarker = null; }
  }, { once: true });
}
async function loadTracking(id) {
  try {
    const t = await api('/orders/' + id + '/tracking');
    const d = t.driver;
    document.getElementById('trackDriver').innerHTML = d
      ? `<div class="driver-info"><div class="logo-mark" style="width:46px;height:46px;font-size:1.2rem;background:var(--gradient);color:#fff;border-radius:12px;display:grid;place-items:center"><i class="bi bi-person-badge"></i></div>
          <div><div class="fw-semibold">${escapeHtml(d.name)}</div><div class="muted small">${escapeHtml(d.vehicleType || '')} · ${escapeHtml(d.vehicleNumber || '')}</div></div>
          <a class="btn btn-soft btn-sm ms-auto" href="tel:${escapeHtml(d.phone || '')}"><i class="bi bi-telephone"></i> Call</a></div>`
      : `<div class="alert alert-info mb-0"><i class="bi bi-info-circle"></i> Driver not assigned yet — tracking will appear once a driver accepts your order.</div>`;
    document.getElementById('trackTimeline').innerHTML = buildTimeline(t.orderStatus, t.deliveryStatus);
    if (d && d.latitude && d.longitude) {
      const lat = d.latitude, lng = d.longitude;
      if (!trackMap) {
        trackMap = L.map('map').setView([lat, lng], 15);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(trackMap);
        trackMarker = L.marker([lat, lng]).addTo(trackMap).bindPopup('Driver').openPopup();
      } else { trackMarker.setLatLng([lat, lng]); trackMap.setView([lat, lng]); }
      setTimeout(() => trackMap.invalidateSize(), 250);
    }
  } catch (e) { showAlert('trackAlert', 'danger', e.message); }
}
function buildTimeline(orderStatus, deliveryStatus) {
  const steps = [
    { label: 'Placed', done: true, icon: 'bag-check' },
    { label: 'Preparing', done: ['PREPARING','READY','OUT_FOR_DELIVERY','DELIVERED'].includes(orderStatus), icon: 'egg-fried' },
    { label: 'Out for delivery', done: ['OUT_FOR_DELIVERY','DELIVERED'].includes(orderStatus) || ['PICKED_UP','OUT_FOR_DELIVERY','DELIVERED'].includes(deliveryStatus), icon: 'bicycle' },
    { label: 'Delivered', done: orderStatus === 'DELIVERED', icon: 'house-check' }
  ];
  return '<div class="timeline">' + steps.map(s => `
    <div class="step ${s.done ? 'done' : ''}"><div class="dot"><i class="bi bi-${s.icon}"></i></div><div class="lbl">${s.label}</div></div>`).join('') + '</div>';
}

function emptyState(icon, title, sub) {
  return `<div class="empty-state"><div class="ico"><i class="bi bi-${icon}"></i></div><div class="fw-semibold">${escapeHtml(title)}</div><div class="small">${escapeHtml(sub || '')}</div></div>`;
}
