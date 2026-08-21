// ===================== Customer page =====================
boot('CUSTOMER', init);

function init() {
  loadRestaurants();
  loadCart();
  go('restaurants', document.querySelector('#sideNav .active'));
}

function go(view, btn) {
  ['restaurants','cart','orders','coupons','reviews'].forEach(v =>
    document.getElementById(v).classList.toggle('d-none', v !== view));
  if (btn) {
    document.querySelectorAll('#sideNav .nav-link').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
  }
  const titles = { restaurants:'Restaurants', cart:'Your Cart', orders:'My Orders', coupons:'My Coupons', reviews:'My Reviews' };
  document.getElementById('pageTitle').textContent = titles[view];
  if (view === 'cart') loadCart();
  if (view === 'orders') loadOrders();
  if (view === 'coupons') loadCoupons();
  if (view === 'reviews') loadReviews();
}

// ---------- Restaurants ----------
async function loadRestaurants() {
  try {
    const list = await api('/restaurants');
    const el = document.getElementById('restaurantList');
    if (!list.length) { el.innerHTML = '<p class="text-muted">No restaurants yet.</p>'; return; }
    el.innerHTML = list.map(r => `
      <div class="col-md-4">
        <div class="card h-100 shadow-sm">
          ${r.imageUrl ? `<img src="${escapeHtml(r.imageUrl)}" class="card-img-top" style="height:140px;object-fit:cover">` : ''}
          <div class="card-body">
            <h5 class="card-title">${escapeHtml(r.name)}</h5>
            <p class="card-text small text-muted">${escapeHtml(r.description || '')}</p>
            ${r.status ? statusBadge(r.status.name || r.status) : ''}
          </div>
          <div class="card-footer bg-white">
            <button class="btn btn-sm btn-brand" onclick="viewRestaurant(${r.id})">View Menu</button>
          </div>
        </div>
      </div>`).join('');
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

async function viewRestaurant(id) {
  try {
    const foods = await api('/restaurants/' + id + '/foods');
    document.getElementById('restaurants').querySelector('#restaurantList').classList.add('d-none');
    const fv = document.getElementById('foodView');
    fv.classList.remove('d-none');
    document.getElementById('foodRestaurantName').textContent = 'Menu';
    const fl = document.getElementById('foodList');
    if (!foods.length) { fl.innerHTML = '<p class="text-muted">No foods available.</p>'; return; }
    fl.innerHTML = foods.map(f => `
      <div class="col-md-4">
        <div class="card h-100">
          ${f.imageUrl ? `<img src="${escapeHtml(f.imageUrl)}" class="card-img-top" style="height:120px;object-fit:cover">` : ''}
          <div class="card-body">
            <h6>${escapeHtml(f.name)}</h6>
            <p class="small text-muted">${escapeHtml(f.description || '')}</p>
            <div class="d-flex justify-content-between align-items-center">
              <span class="fw-bold">${money(f.price)}</span>
              ${f.available ? `<button class="btn btn-sm btn-brand" onclick="addToCart(${f.id})">Add</button>`
                            : `<span class="badge bg-secondary">Unavailable</span>`}
            </div>
          </div>
        </div>
      </div>`).join('');
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

function backToRestaurants() {
  document.getElementById('foodView').classList.add('d-none');
  document.getElementById('restaurants').querySelector('#restaurantList').classList.remove('d-none');
  loadRestaurants();
}

async function addToCart(foodId) {
  try {
    await api('/cart/items', 'POST', { foodId, quantity: 1 });
    showAlert('alertBox','success','Added to cart');
    loadCart();
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

// ---------- Cart ----------
async function loadCart() {
  try {
    const c = await api('/cart/items');
    const el = document.getElementById('cartList');
    if (!c.items || !c.items.length) {
      el.innerHTML = '<p class="text-muted">Your cart is empty.</p>';
      document.getElementById('cartSummary').innerHTML = '';
      document.getElementById('cartCount').textContent = '';
      return;
    }
    document.getElementById('cartCount').textContent = c.items.length;
    el.innerHTML = `<table class="table"><thead><tr>
        <th>Food</th><th>Unit</th><th>Qty</th><th>Subtotal</th><th></th></tr></thead><tbody>
      ${c.items.map(i => `
        <tr>
          <td>${escapeHtml(i.foodName)}</td>
          <td>${money(i.price)}</td>
          <td><input type="number" min="1" value="${i.quantity}" class="form-control form-control-sm" style="width:80px"
              onchange="updateQty(${i.id}, ${i.foodId}, this.value)"></td>
          <td>${money(i.subtotal)}</td>
          <td><button class="btn btn-sm btn-outline-danger" onclick="removeItem(${i.id})">Remove</button></td>
        </tr>`).join('')}
      </tbody></table>`;
    document.getElementById('cartSummary').innerHTML = `
      <div class="d-flex justify-content-between">
        <h5>Total: ${money(c.total)}</h5>
        <button class="btn btn-brand" data-bs-toggle="modal" data-bs-target="#checkoutModal">Checkout</button>
      </div>`;
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

async function updateQty(itemId, foodId, qty) {
  try {
    await api('/cart/items/' + itemId, 'PUT', { foodId, quantity: parseInt(qty) });
    loadCart();
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

async function removeItem(itemId) {
  try {
    await api('/cart/items/' + itemId, 'DELETE');
    loadCart();
  } catch (e) { showAlert('alertBox','danger',e.message); }
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
    showAlert('alertBox','success','Order #' + order.id + ' placed! Status: ' + order.status);
    loadCart();
    go('orders', document.querySelectorAll('#sideNav .nav-link')[2]);
  } catch (e) { showAlert('checkoutAlert','danger',e.message); }
}

// ---------- Orders ----------
async function loadOrders() {
  try {
    const list = await api('/orders');
    const el = document.getElementById('orderList');
    if (!list.length) { el.innerHTML = '<p class="text-muted">No orders yet.</p>'; return; }
    el.innerHTML = list.map(o => `
      <div class="card mb-3">
        <div class="card-header d-flex justify-content-between">
          <span>Order #${o.id} — ${escapeHtml(o.restaurantName || '')}</span>
          <span>${statusBadge(o.status)} ${o.deliveryStatus ? ' / ' + statusBadge(o.deliveryStatus) : ''}</span>
        </div>
        <div class="card-body">
          <ul class="list-unstyled mb-2">
            ${o.items.map(i => `<li>${escapeHtml(i.foodName)} × ${i.quantity} — ${money(i.subtotal)}</li>`).join('')}
          </ul>
          <div>Total: <b>${money(o.totalPrice)}</b>
            ${o.discountAmount ? ` <span class="text-success">(saved ${money(o.discountAmount)}${o.couponCode ? ' via ' + escapeHtml(o.couponCode) : ''})</span>` : ''}</div>
          <div class="mt-2">
            ${o.status === 'PENDING' ? `<button class="btn btn-sm btn-outline-danger" onclick="cancelOrder(${o.id})">Cancel</button>` : ''}
            ${o.status === 'DELIVERED' ? `<button class="btn btn-sm btn-primary" onclick="openReview(${o.id})">Leave Review</button>` : ''}
            ${(o.status === 'PENDING' || o.status === 'ACCEPTED' || o.status === 'PREPARING' || o.status === 'READY' || o.status === 'OUT_FOR_DELIVERY') ? `<button class="btn btn-sm btn-success" onclick="payOrder(${o.id})">Pay</button>` : ''}
            ${(o.status !== 'DELIVERED' && o.status !== 'CANCELLED' && o.status !== 'REJECTED') ? `<button class="btn btn-sm btn-brand" onclick="trackOrder(${o.id})"><i class="bi bi-geo-alt"></i> Track</button>` : ''}
          </div>
        </div>
      </div>`).join('');
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

async function cancelOrder(id) {
  try {
    await api('/orders/' + id + '/cancel', 'POST');
    showAlert('alertBox','success','Order cancelled');
    loadOrders();
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

async function payOrder(id) {
  try {
    const res = await api('/payments/create-intent', 'POST', { orderId: id });
    const msg = res.devMode
      ? 'DEV mode: payment simulated. clientSecret=' + res.clientSecret
      : 'Stripe clientSecret: ' + res.clientSecret;
    showAlert('alertBox','success', msg + ' (amount ' + money(res.amount) + ' ' + res.currency + ')');
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

// ---------- Reviews ----------
async function loadReviews() {
  try {
    const list = await api('/reviews/mine');
    const el = document.getElementById('reviewList');
    if (!list.length) { el.innerHTML = '<p class="text-muted">No reviews yet.</p>'; return; }
    el.innerHTML = list.map(r => `
      <div class="card mb-2"><div class="card-body">
        <div>${'★'.repeat(r.rating)}${'☆'.repeat(5 - r.rating)} — ${escapeHtml(r.restaurantName || 'Restaurant')}</div>
        <div class="small text-muted">${escapeHtml(r.comment || '')}</div>
      </div></div>`).join('');
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

function openReview(orderId) {
  document.getElementById('rvOrderId').value = orderId;
  clearAlert('reviewAlert');
  new bootstrap.Modal(document.getElementById('reviewModal')).show();
}

async function submitReview() {
  clearAlert('reviewAlert');
  const body = {
    orderId: parseInt(document.getElementById('rvOrderId').value),
    rating: parseInt(document.getElementById('rvRating').value),
    comment: document.getElementById('rvComment').value
  };
  try {
    await api('/reviews', 'POST', body);
    bootstrap.Modal.getInstance(document.getElementById('reviewModal')).hide();
    showAlert('alertBox','success','Review submitted');
    loadReviews();
  } catch (e) { showAlert('reviewAlert','danger',e.message); }
}

// ---------- Coupons ----------
async function loadCoupons() {
  try {
    const list = await api('/coupons/mine');
    const el = document.getElementById('couponList');
    if (!list.length) { el.innerHTML = '<p class="text-muted">No coupons assigned.</p>'; return; }
    el.innerHTML = list.map(c => `
      <div class="card mb-2"><div class="card-body d-flex justify-content-between align-items-center">
        <div>
          <span class="badge bg-danger">${escapeHtml(c.code)}</span>
          <span class="ms-2">${escapeHtml(c.description || '')}</span><br>
          <small class="text-muted">${c.discountType} ${c.value}${c.discountType === 'PERCENTAGE' ? '%' : ' EGP'}
            ${c.minOrderTotal ? ' · min ' + money(c.minOrderTotal) : ''} · expires ${c.expiryDate || 'never'}</small>
        </div>
        <span class="badge ${c.used ? 'bg-secondary' : 'bg-success'}">${c.used ? 'Used' : 'Available'}</span>
      </div></div>`).join('');
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

// ---------- Live Tracking ----------
let trackTimer = null;
let trackMap = null;
let trackMarker = null;

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
      ? `<div class="card"><div class="card-body d-flex align-items-center gap-3">
           <div class="logo-mark" style="width:46px;height:46px;font-size:1.2rem;"><i class="bi bi-person-badge"></i></div>
           <div><div class="fw-semibold">${escapeHtml(d.name)}</div>
             <div class="muted small">${escapeHtml(d.vehicleType || '')} · ${escapeHtml(d.vehicleNumber || '')}</div></div>
           <a class="btn btn-soft btn-sm ms-auto" href="tel:${escapeHtml(d.phone || '')}"><i class="bi bi-telephone"></i> Call</a>
         </div></div>`
      : `<div class="alert alert-info mb-0"><i class="bi bi-info-circle"></i> Driver not assigned yet — tracking will appear once a driver accepts your order.</div>`;

    document.getElementById('trackTimeline').innerHTML = buildTimeline(t.orderStatus, t.deliveryStatus);

    if (d && d.latitude && d.longitude) {
      const lat = d.latitude, lng = d.longitude;
      if (!trackMap) {
        trackMap = L.map('map').setView([lat, lng], 15);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          maxZoom: 19, attribution: '&copy; OpenStreetMap'
        }).addTo(trackMap);
        trackMarker = L.marker([lat, lng]).addTo(trackMap).bindPopup('Driver').openPopup();
      } else {
        trackMarker.setLatLng([lat, lng]);
        trackMap.setView([lat, lng]);
      }
      setTimeout(() => trackMap.invalidateSize(), 250);
    } else if (trackMap) {
      document.getElementById('map').insertAdjacentHTML('afterend',
        '<p class="text-muted small mt-2"><i class="bi bi-geo-alt"></i> Waiting for the driver to start sharing their location…</p>');
    }
  } catch (e) { showAlert('trackAlert', 'danger', e.message); }
}

function buildTimeline(orderStatus, deliveryStatus) {
  const steps = [
    { label: 'Order Placed', done: true },
    { label: 'Preparing',
      done: ['PREPARING', 'READY', 'OUT_FOR_DELIVERY', 'DELIVERED'].includes(orderStatus) },
    { label: 'Out for Delivery',
      done: ['OUT_FOR_DELIVERY', 'DELIVERED'].includes(orderStatus) ||
            ['PICKED_UP', 'OUT_FOR_DELIVERY', 'DELIVERED'].includes(deliveryStatus) },
    { label: 'Delivered', done: orderStatus === 'DELIVERED' }
  ];
  return '<div class="d-flex gap-2">' + steps.map(s => `
    <div class="flex-fill text-center">
      <div class="mx-auto mb-1" style="width:36px;height:36px;border-radius:50%;display:grid;place-items:center;
        ${s.done ? 'background:var(--gradient);color:#fff' : 'background:var(--surface-2);color:var(--muted)'}">
        <i class="bi bi-${s.done ? 'check-lg' : 'circle'}"></i></div>
      <div class="small ${s.done ? 'fw-semibold' : 'muted'}">${s.label}</div>
    </div>`).join('') + '</div>';
}
