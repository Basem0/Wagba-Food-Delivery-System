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
  window._offersOnly = false;
  window.onNav = (v) => { if (v === 'cart') loadCart(); if (v === 'orders') loadOrders(); if (v === 'coupons') loadCoupons(); if (v === 'reviews') loadReviews(); };
  window.__realtimeRefresh = () => { loadOrders(); loadCart(); };
  navTo('restaurants');
  loadRestaurants();
  loadCart();
}

async function loadRestaurants() {
  try {
    const page = await api('/restaurants?page=0&size=100');
    const list = page.content || [];
    window._rests = list || [];
    const cuis = [...new Set((list || []).map(r => r.cuisine).filter(Boolean))];
    document.getElementById('filterCuisine').innerHTML = '<option value="">All cuisines</option>' + cuis.map(c => `<option>${escapeHtml(c)}</option>`).join('');
    try {
      const cats = await api('/restaurants/categories');
      document.getElementById('filterCategory').innerHTML = '<option value="">All categories</option>'
        + (cats || []).map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
    } catch (e) {}
    renderRestaurants(list || []);
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

function toggleOffers() {
  window._offersOnly = !window._offersOnly;
  const btn = document.getElementById('offersToggle');
  btn.classList.toggle('active', window._offersOnly);
  applyFilters();
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
          <div class="mt-2 d-flex gap-2 align-center">
            ${r.status ? statusBadge(r.status.name || r.status) : ''}
            ${r.hasOffers ? `<span class="offer-chip"><i class="bi bi-tag-fill"></i> Offers</span>` : ''}
          </div>
        </div>
    </div>`).join('');
}

function applyFilters() {
  const q = (document.getElementById('searchInput').value || '').toLowerCase();
  const cu = document.getElementById('filterCuisine').value;
  const cat = document.getElementById('filterCategory').value;
  const sort = document.getElementById('sortBy').value;
  let list = (window._rests || []).filter(r => {
    const matchQ = !q || (r.name || '').toLowerCase().includes(q) || (r.description || '').toLowerCase().includes(q);
    const matchC = !cu || r.cuisine === cu;
    const matchCat = !cat || (r.categories || []).some(c => String(c.id) === String(cat));
    const matchOffer = !window._offersOnly || r.hasOffers;
    return matchQ && matchC && matchCat && matchOffer;
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
             <span class="price">${f.offer ? `<span class="old-price">${money(f.price)}</span> <span class="new-price">${money(f.discountPrice)}</span> <span class="offer-chip sm"><i class="bi bi-tag-fill"></i> Offer</span>` : money(f.price)}</span>
             ${f.available
               ? `<button class="btn-brand btn-sm" onclick="addToCart(${f.id})"><i class="bi bi-plus-lg"></i> Add</button>`
               : `<span class="badge bg-secondary">Unavailable</span>`}
           </div>
         </div>
      </div>`).join('');
    renderRestaurantReviews(id);
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

async function renderRestaurantReviews(restaurantId) {
  const el = document.getElementById('restReviews');
  if (!el) return;
  try {
    const list = await api('/reviews/restaurant/' + restaurantId);
    if (!list.length) { el.innerHTML = '<p class="muted small mb-0">No reviews yet for this restaurant.</p>'; return; }
    el.innerHTML = list.map(rv => `
      <div class="review-item">
        <div class="d-flex justify-between align-center">
          <div class="fw-semibold">${escapeHtml(rv.customerName || 'Customer')}</div>
          <div class="text-warning small">${stars(rv.rating)}</div>
        </div>
        <div class="muted small">${escapeHtml(rv.comment || '')}</div>
      </div>`).join('');
  } catch (e) { el.innerHTML = ''; }
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
      <div class="d-grid"><button class="btn-brand" onclick="openCheckout()">Checkout · ${money(c.total)}</button></div>`;
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

let checkoutMap = null, checkoutMarker = null;
window._ckLat = null; window._ckLng = null;
function openCheckout() {
  clearAlert('checkoutAlert');
  new bootstrap.Modal(document.getElementById('checkoutModal')).show();
  setTimeout(initCheckoutMap, 300);
}
function initCheckoutMap() {
  const el = document.getElementById('checkoutMap');
  if (!el || checkoutMap) return;
  checkoutMap = L.map(el).setView([30.0444, 31.2357], 11);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(checkoutMap);
  checkoutMap.on('click', (e) => setCheckoutMarker(e.latlng.lat, e.latlng.lng));
  setTimeout(() => checkoutMap.invalidateSize(), 200);
}
function setCheckoutMarker(lat, lng) {
  window._ckLat = lat; window._ckLng = lng;
  if (!checkoutMap) return;
  if (!checkoutMarker) checkoutMarker = L.marker([lat, lng]).addTo(checkoutMap);
  else checkoutMarker.setLatLng([lat, lng]);
  checkoutMap.setView([lat, lng], 15);
  const hint = document.getElementById('locHint');
  if (hint) hint.textContent = `Pinned at ${lat.toFixed(5)}, ${lng.toFixed(5)}`;
}
function useMyLocation() {
  if (!navigator.geolocation) { toast('Location', 'Geolocation not supported', 'err'); return; }
  navigator.geolocation.getCurrentPosition(
    pos => setCheckoutMarker(pos.coords.latitude, pos.coords.longitude),
    err => toast('Location', 'Could not get location: ' + err.message, 'err'),
    { enableHighAccuracy: true, timeout: 10000 }
  );
}

async function submitCheckout() {
  clearAlert('checkoutAlert');
  const body = {
    city: document.getElementById('ckCity').value,
    street: document.getElementById('ckStreet').value,
    buildingNumber: document.getElementById('ckBuilding').value,
    apartment: document.getElementById('ckApartment').value,
    details: document.getElementById('ckDetails').value,
    couponCode: document.getElementById('ckCoupon').value || null,
    latitude: window._ckLat,
    longitude: window._ckLng,
    paymentMethod: document.getElementById('ckPayment').value || 'CARD'
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
    const page = await api('/orders?page=0&size=100');
    const list = page.content || [];
    const el = document.getElementById('orderList');
    if (!list.length) { el.innerHTML = emptyState('bi-receipt', 'No orders yet', 'Your orders will appear here.'); return; }
    el.innerHTML = list.map(o => `
      <div class="card mb-3 fade-in"><div class="card-body">
        <div class="d-flex justify-between align-center mb-2">
          <div><b>Order #${o.id}</b> · ${escapeHtml(o.restaurantName || '')}<div class="muted small">${fmtDateTime(o.createdAt)}</div>${o.deliveryAddress ? `<div class="muted small"><i class="bi bi-geo-alt"></i> ${escapeHtml(formatAddr(o.deliveryAddress))}</div>` : ''}</div>
          <div class="d-flex gap-1 flex-wrap justify-end">${statusBadge(o.status)} ${o.deliveryStatus ? statusBadge(o.deliveryStatus) : ''}${o.paymentMethod ? `<span class="badge bg-dark">${escapeHtml(o.paymentMethod)}</span>` : ''}</div>
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
            ${o.status === 'DELIVERED' && !o.reviewed ? `<button class="btn btn-sm btn-soft" onclick="openReview(${o.id})"><i class="bi bi-star"></i> Review</button>` : ''}
            ${o.status === 'DELIVERED' && o.reviewed ? `<span class="badge bg-success-subtle text-success"><i class="bi bi-check-circle"></i> Reviewed</span>` : ''}
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
// ---------- Payment (Stripe, with dev fallback) ----------
let _stripe = null, _card = null, _clientSecret = null, _payDev = true;
async function payOrder(id) {
  clearAlert('payAlert');
  window._payId = id;
  document.getElementById('payOrderId').textContent = id;
  try {
    const cfg = await api('/payments/config');
    _payDev = cfg.devMode;
    document.getElementById('payDev').style.display = _payDev ? 'block' : 'none';
    document.getElementById('stripeCard').style.display = _payDev ? 'none' : 'block';
    if (!_payDev) {
      const intent = await api('/payments/create-intent', 'POST', { orderId: id });
      _clientSecret = intent.clientSecret;
      document.getElementById('payAmount').textContent = 'Amount: ' + money(intent.amount) + ' ' + (intent.currency || '').toUpperCase();
      if (!_stripe) _stripe = Stripe(cfg.publishableKey);
      const elements = _stripe.elements();
      _card = elements.create('card');
      _card.mount('#stripeCard');
    } else {
      document.getElementById('payAmount').textContent = 'Order #' + id;
    }
    new bootstrap.Modal(document.getElementById('payModal')).show();
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function confirmPayment() {
  clearAlert('payAlert');
  const id = window._payId;
  if (_payDev) {
    toast('Payment (demo)', 'Simulated success for order #' + id);
    bootstrap.Modal.getInstance(document.getElementById('payModal')).hide();
    loadOrders();
    return;
  }
  try {
    const { error, paymentIntent } = await _stripe.confirmCardPayment(_clientSecret, { payment_method: { card: _card } });
    if (error) { showAlert('payAlert', 'danger', error.message); return; }
    if (paymentIntent && paymentIntent.status === 'succeeded') {
      toast('Payment successful', 'Order #' + id + ' paid');
      bootstrap.Modal.getInstance(document.getElementById('payModal')).hide();
      loadOrders();
    } else if (paymentIntent) {
      showAlert('payAlert', 'warning', 'Payment status: ' + paymentIntent.status);
    }
  } catch (e) { showAlert('payAlert', 'danger', e.message); }
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
let trackTimer = null, trackMap = null, trackMarker = null, trackCustMarker = null;
async function trackOrder(id) {
  document.getElementById('trkId').textContent = id;
  clearAlert('trackAlert');
  new bootstrap.Modal(document.getElementById('trackModal')).show();
  await loadTracking(id);
  if (trackTimer) clearInterval(trackTimer);
  trackTimer = setInterval(() => loadTracking(id), 5000);
  document.getElementById('trackModal').addEventListener('hidden.bs.modal', () => {
    if (trackTimer) { clearInterval(trackTimer); trackTimer = null; }
    if (trackMap) { trackMap.remove(); trackMap = null; trackMarker = null; trackCustMarker = null; }
  }, { once: true });
}
async function loadTracking(id) {
  try {
    const t = await api('/orders/' + id + '/tracking');
    const d = t.driver;
    const live = d && d.latitude && d.longitude;
    document.getElementById('trackDriver').innerHTML = d
      ? `<div class="driver-info"><div class="logo-mark" style="width:46px;height:46px;font-size:1.2rem;background:var(--gradient);color:#fff;border-radius:12px;display:grid;place-items:center"><i class="bi bi-person-badge"></i></div>
          <div><div class="fw-semibold d-flex align-center gap-2">${escapeHtml(d.name)} ${live ? '<span class="live-dot"></span> <span class="small text-success fw-semibold">LIVE</span>' : ''}</div>
          <div class="muted small">${escapeHtml(d.vehicleType || '')} · ${escapeHtml(d.vehicleNumber || '')}${live && d.locationUpdatedAt ? ' · <i class="bi bi-broadcast"></i> ' + fmtTimeAgo(d.locationUpdatedAt) : ''}</div></div>
          <a class="btn btn-soft btn-sm ms-auto" href="tel:${escapeHtml(d.phone || '')}"><i class="bi bi-telephone"></i> Call</a></div>`
      : `<div class="alert alert-info mb-0"><i class="bi bi-info-circle"></i> Driver not assigned yet — tracking will appear once a driver accepts your order.</div>`;
    document.getElementById('trackTimeline').innerHTML = buildTimeline(t.orderStatus, t.deliveryStatus);
    const mapEl = document.getElementById('map');
    const custLat = t.customerLatitude, custLng = t.customerLongitude;
    if (live) {
      const lat = d.latitude, lng = d.longitude;
      if (!trackMap) {
        trackMap = L.map('map').setView([lat, lng], 15);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(trackMap);
        trackMarker = L.marker([lat, lng], { icon: L.divIcon({ className: 'pin-driver', html: '<i class="bi bi-bicycle"></i>', iconSize: [30,30] }) }).addTo(trackMap).bindPopup('Driver is here').openPopup();
      } else { trackMarker.setLatLng([lat, lng]); trackMap.setView([lat, lng]); }
      if (custLat != null && custLng != null) {
        if (!trackCustMarker) trackCustMarker = L.marker([custLat, custLng], { icon: L.divIcon({ className: 'pin-cust', html: '<i class="bi bi-house"></i>', iconSize: [30,30] }) }).addTo(trackMap).bindPopup('Delivery address');
        else trackCustMarker.setLatLng([custLat, custLng]);
      }
      setTimeout(() => trackMap.invalidateSize(), 250);
    } else if (custLat != null && custLng != null) {
      if (!trackMap) {
        trackMap = L.map('map').setView([custLat, custLng], 15);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(trackMap);
      }
      if (!trackCustMarker) trackCustMarker = L.marker([custLat, custLng], { icon: L.divIcon({ className: 'pin-cust', html: '<i class="bi bi-house"></i>', iconSize: [30,30] }) }).addTo(trackMap).bindPopup('Delivery address').openPopup();
      else trackCustMarker.setLatLng([custLat, custLng]);
      setTimeout(() => trackMap.invalidateSize(), 250);
    } else if (mapEl) {
      if (trackMap) { trackMap.remove(); trackMap = null; trackMarker = null; trackCustMarker = null; }
      mapEl.innerHTML = '<div class="map-empty"><i class="bi bi-geo-alt"></i><div>Live map appears here once the driver shares their location.</div></div>';
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
