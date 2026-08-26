// ===================== Customer page =====================

const CAT_IMG = {
  Koshary:'koshary', Breakfast:'breakfast', Mains:'dinner', Desserts:'dessert',
  Drinks:'beverage', Sides:'fries', Bakery:'bakery', Sweets:'cake', Grills:'grill',
  Chicken:'chicken', Pizza:'pizza'
};
function catImage(name) {
  const kw = CAT_IMG[name] || 'food';
  const lock = (name.split('').reduce((a, ch) => a + ch.charCodeAt(0), 0) % 900) + 300;
  return 'https://loremflickr.com/64/64/' + kw + '?lock=' + lock;
}

const SORT_OPTS = [
  { v: 'name', l: 'Sort: Name' },
  { v: 'rating', l: 'Sort: Rating' },
  { v: 'minOrder', l: 'Sort: Min order' }
];

function renderSortMenu() {
  const m = document.getElementById('sortMenu');
  if (!m) return;
  m.innerHTML = SORT_OPTS.map(o => `<button type="button" class="dd-item ${o.v === (window._sort || 'name') ? 'active' : ''}" onclick="pickSort('${o.v}')">${o.l}</button>`).join('');
}
function toggleDD(name) {
  const dd = document.getElementById('dd' + name.charAt(0).toUpperCase() + name.slice(1));
  if (!dd) return;
  const open = dd.classList.contains('open');
  closeDDs();
  if (!open) dd.classList.add('open');
}
function closeDDs() { document.querySelectorAll('.dd.open').forEach(d => d.classList.remove('open')); }
function pickCuisine(val) {
  window._cuisine = val;
  const lbl = document.getElementById('cuisineLabel');
  if (lbl) lbl.textContent = val === '' ? 'All cuisines' : val;
  document.querySelectorAll('#cuisineMenu .dd-item').forEach(it => it.classList.toggle('active', it.getAttribute('onclick') === `pickCuisine('${val}')`));
  closeDDs(); applyFilters();
}
function pickSort(val) {
  window._sort = val;
  const lbl = document.getElementById('sortLabel');
  if (lbl) lbl.textContent = (SORT_OPTS.find(o => o.v === val) || {}).l || val;
  document.querySelectorAll('#sortMenu .dd-item').forEach(it => it.classList.toggle('active', it.getAttribute('onclick') === `pickSort('${val}')`));
  closeDDs(); applyFilters();
}

function init() {
  window.VIEWS = {
    restaurants: { title: 'Restaurants', sub: 'Discover kitchens near you' },
    cart: { title: 'My Cart', sub: 'Review and checkout your items' },
    orders: { title: 'My Orders', sub: 'Track and manage your orders' },
    favorites: { title: 'Favorites', sub: 'Your favorite restaurants' },
    coupons: { title: 'My Coupons', sub: 'Your saved offers' },
    settings: { title: 'Settings', sub: 'Manage your account' }
  };
  window._offersOnly = false;
  window._catName = '';
  window._cuisine = '';
  window._sort = 'name';
  renderSortMenu();
  document.addEventListener('click', (e) => { if (!e.target.closest('.dd')) closeDDs(); });
  const me = getUser();
  const hb = document.getElementById('hbName');
  if (hb && me) hb.textContent = (me.name || 'there').split(' ')[0];
  const spN = document.getElementById('spNameText'), spE = document.getElementById('spEmailText'), spA = document.getElementById('spAvatar');
  if (spN) spN.textContent = me.name || me.email || 'Customer';
  if (spE) spE.textContent = me.email || '';
  if (spA) spA.textContent = (me.name || me.email || 'C').trim().charAt(0).toUpperCase();
  window.onNav = (v) => { if (v === 'cart') loadCart(); if (v === 'orders') loadOrders(); if (v === 'favorites') loadFavorites(); if (v === 'coupons') loadCoupons(); if (v === 'settings') renderSettings(); };
  window.__realtimeRefresh = () => { loadOrders(); loadCart(); };
  navTo('restaurants');
  loadRestaurants();
  loadCart();
}

function renderSettings() {
  const root = document.getElementById('view-settings');
  if (!root) return;
  if (addrMap) { addrMap.remove(); addrMap = null; addrMarker = null; }
  root.innerHTML = renderSettingsShell({ panel: addressCardHtml(), label: 'Address', icon: 'geo-alt' });
  loadAccountSettings();
  loadAddress();
  window.__onSettingsCat = (c) => { if (c === 'role') { if (!addrMap) initAddressMap(); else setTimeout(() => addrMap.invalidateSize(), 60); } };
}

let addrMap = null, addrMarker = null, addrLat = null, addrLng = null;

function addressCardHtml() {
  return `
  <div class="col-12">
    <div class="card"><div class="card-body">
      <div class="section-head mb-3"><span class="sh-ic"><i class="bi bi-geo-alt"></i></span><div><h5 class="mb-0">Delivery Address</h5><div class="text-muted small">Saved for faster checkout</div></div></div>
      <div id="addrAlert"></div>
      <div class="row g-2">
        <div class="col-md-6"><label class="form-label">City</label><input id="addrCity" class="form-control" placeholder="Mansoura"></div>
        <div class="col-md-6"><label class="form-label">Street</label><input id="addrStreet" class="form-control" placeholder="El-Gomhoria St"></div>
      </div>
      <div class="row g-2 mt-1">
        <div class="col-6"><label class="form-label">Building</label><input id="addrBuilding" class="form-control"></div>
        <div class="col-6"><label class="form-label">Apartment</label><input id="addrApartment" class="form-control"></div>
      </div>
      <div class="mb-2 mt-2"><label class="form-label">Extra details</label><input id="addrDetails" class="form-control" placeholder="Floor, landmark..."></div>
      <label class="form-label">Location on map</label>
      <div id="addrMap" class="map-picker mb-2"></div>
      <div class="d-flex align-center gap-2 mb-3">
        <button type="button" class="btn btn-sm btn-soft" onclick="addrUseMyLocation()"><i class="bi bi-geo-alt"></i> Use my location</button>
        <span class="small text-muted" id="addrLocHint">Tap the map to drop a pin</span>
      </div>
      <button class="btn-brand" onclick="saveAddress()"><i class="bi bi-check-lg"></i> Save address</button>
    </div></div>
  </div>`;
}

function initAddressMap() {
  const el = document.getElementById('addrMap');
  if (!el || addrMap) return;
  addrMap = L.map(el).setView([30.0444, 31.2357], 11);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(addrMap);
  addrMap.on('click', (e) => setAddressMarker(e.latlng.lat, e.latlng.lng));
  setTimeout(() => addrMap.invalidateSize(), 200);
}

function setAddressMarker(lat, lng) {
  addrLat = lat; addrLng = lng;
  if (!addrMap) return;
  if (!addrMarker) addrMarker = L.marker([lat, lng]).addTo(addrMap);
  else addrMarker.setLatLng([lat, lng]);
  addrMap.setView([lat, lng], 15);
  const hint = document.getElementById('addrLocHint');
  if (hint) hint.textContent = `Pinned at ${lat.toFixed(5)}, ${lng.toFixed(5)}`;
}

function addrUseMyLocation() {
  if (!navigator.geolocation) { toast('Location', 'Geolocation not supported', 'err'); return; }
  navigator.geolocation.getCurrentPosition(
    pos => setAddressMarker(pos.coords.latitude, pos.coords.longitude),
    err => toast('Location', 'Could not get location: ' + err.message, 'err'),
    { enableHighAccuracy: true, timeout: 10000 }
  );
}

async function loadAddress() {
  try {
    const a = await api('/customers/me/address');
    if (!a) return;
    const set = (id, v) => { const el = document.getElementById(id); if (el && v != null) el.value = v; };
    set('addrCity', a.city); set('addrStreet', a.street); set('addrBuilding', a.buildingNumber);
    set('addrApartment', a.apartment); set('addrDetails', a.details);
    if (a.latitude != null && a.longitude != null) {
      addrLat = a.latitude; addrLng = a.longitude;
      initAddressMap();
      setTimeout(() => setAddressMarker(a.latitude, a.longitude), 250);
    }
  } catch (e) { /* settings still usable without saved address */ }
}

async function saveAddress() {
  clearAlert('addrAlert');
  const get = (id) => (document.getElementById(id).value || '').trim();
  const body = { city: get('addrCity'), street: get('addrStreet'), buildingNumber: get('addrBuilding'), apartment: get('addrApartment'), details: get('addrDetails') };
  body.latitude = addrLat; body.longitude = addrLng;
  if (!body.city || !body.street) { showAlert('addrAlert', 'warning', 'City and street are required'); return; }
  if (body.latitude == null || body.longitude == null) { showAlert('addrAlert', 'warning', 'Please drop a pin on the map'); return; }
  try {
    await api('/customers/me/address', 'POST', body);
    toast('Saved', 'Delivery address updated');
  } catch (e) { showAlert('addrAlert', 'danger', e.message); }
}

async function loadRestaurants() {
  try {
    showSkeletons('restaurantList', 8, 232);
    const [page, favs] = await Promise.all([
      api('/restaurants?page=0&size=100'),
      api('/favorites').catch(() => [])
    ]);
    const list = page.content || [];
    window._rests = list || [];
    window._favIds = new Set((favs || []).map(f => f.restaurantId));
    const cuis = [...new Set((list || []).map(r => r.cuisine).filter(Boolean))];
    const cuMenu = document.getElementById('cuisineMenu');
    if (cuMenu) cuMenu.innerHTML = ['', ...cuis].map(c =>
      `<button type="button" class="dd-item ${c === '' ? 'active' : ''}" onclick="pickCuisine('${escapeHtml(c)}')">${c === '' ? 'All cuisines' : escapeHtml(c)}</button>`
    ).join('');
    try {
      const cats = await api('/restaurants/categories');
      const seen = new Set();
      const uniq = (cats || []).filter(c => { if (seen.has(c.name)) return false; seen.add(c.name); return true; });
      const chips = [{ name: 'All' }].concat(uniq);
      document.getElementById('catChips').innerHTML = chips.map(c => {
        const active = c.name === 'All' ? 'active' : '';
        const ic = c.name === 'All' ? '<i class="bi bi-grid"></i>' : `<img class="cc-img" src="${escapeHtml(catImage(c.name))}" onerror="this.style.display='none'">`;
        return `<button type="button" class="cat-chip ${active}" onclick="selectCategory('${escapeHtml(c.name)}')">${ic}<span>${escapeHtml(c.name)}</span></button>`;
      }).join('');
    } catch (e) {}
    renderRestaurants(list || []);
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

function toggleOffers() {
  window._offersOnly = !window._offersOnly;
  const btn = document.getElementById('offersToggle');
  const banner = document.getElementById('promoBanner');
  if (btn) btn.textContent = window._offersOnly ? 'Clear' : 'Show offers';
  if (banner) banner.classList.toggle('active', window._offersOnly);
  applyFilters();
}
function selectCategory(name) {
  window._catName = (name === 'All') ? '' : name;
  document.querySelectorAll('#catChips .cat-chip').forEach(c => c.classList.toggle('active', c.getAttribute('onclick') === `selectCategory('${name}')`));
  applyFilters();
}

function renderRestaurants(list) {
  const el = document.getElementById('restaurantList');
  if (!list.length) { el.innerHTML = emptyState('bi-shop', 'No restaurants found', 'Try a different search.'); return; }
    el.innerHTML = list.map(r => {
      const rating = r.avgRating || 0;
      const starsHtml = stars(Math.round(rating));
      return `
      <div class="rest-card" onclick="viewRestaurant(${r.id})">
        <div class="rc-logo">
          ${r.imageUrl ? `<img src="${escapeHtml(r.imageUrl)}" onerror="onImgError(this)" data-label="${escapeHtml(r.name)}">` : `<i class="bi bi-shop"></i>`}
        </div>
        <div class="rc-body">
          <div class="rc-name">${escapeHtml(r.name)}</div>
          <div class="rc-line">
            ${rating ? `<span class="rc-rating"><i class="bi bi-star-fill"></i> ${rating}</span><span class="rc-sep">·</span>` : ''}
            ${r.cuisine ? `<span class="rc-cuisine">${escapeHtml(r.cuisine)}</span>` : ''}
          </div>
          <div class="rc-meta">
            ${r.etaMinutes ? `<span><i class="bi bi-clock"></i> ${r.etaMinutes} min</span>` : ''}
            ${r.minOrderTotal ? `<span><i class="bi bi-bag"></i> Min. ${money(r.minOrderTotal)}</span>` : ''}
          </div>
        </div>
        <div class="d-flex flex-column gap-1" style="align-self:center">
          <button type="button" class="btn btn-sm ${(window._favIds && window._favIds.has(r.id)) ? 'btn-outline-danger' : 'btn-outline-secondary'}" onclick="event.stopPropagation(); toggleFavorite(${r.id})" title="Toggle favorite"><i class="bi bi-heart${(window._favIds && window._favIds.has(r.id)) ? '-fill' : ''}"></i></button>
          <button type="button" class="rc-view" onclick="viewRestaurant(${r.id}); event.stopPropagation();" aria-label="View Restaurant"><i class="bi bi-arrow-right"></i></button>
        </div>
      </div>`;
    }).join('');
}

function applyFilters() {
  const q = (document.getElementById('searchInput').value || '').toLowerCase();
  const cu = window._cuisine || '';
  const cat = window._catName || '';
  const sort = window._sort || 'name';
  let list = (window._rests || []).filter(r => {
    const matchQ = !q || (r.name || '').toLowerCase().includes(q) || (r.description || '').toLowerCase().includes(q);
    const matchC = !cu || r.cuisine === cu;
    const matchCat = !cat || (r.categories || []).some(c => c.name === cat);
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
    window._foods = foods;
    window._currentRestaurantId = id;
    window._foodCat = '__all__';
    renderFoodCats();
    renderFoodItems();
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

function foodCardHtml(f) {
  const pic = f.imageUrl ? `<img src="${escapeHtml(f.imageUrl)}" onerror="onImgError(this)" data-label="${escapeHtml(f.name)}">` : `<i class="bi bi-egg-fried"></i>`;
  const priceHtml = f.offer
    ? `<span class="old-price">${money(f.price)}</span><span class="new-price">${money(f.discountPrice)}</span>`
    : `<span class="price">${money(f.price)}</span>`;
  const badges = `${f.categoryName ? `<span class="badge-soft">${escapeHtml(f.categoryName)}</span>` : ''}${f.offer ? `<span class="offer-chip sm"><i class="bi bi-tag-fill"></i> Offer</span>` : ''}`;
  const action = f.available
    ? `<button type="button" class="add-btn" onclick="addToCart(${f.id}, window._currentRestaurantId); event.stopPropagation();" aria-label="Add to cart"><i class="bi bi-plus-lg"></i></button>`
    : `<span class="badge bg-secondary">Unavailable</span>`;
  return `<div class="food-card" onclick="openFoodModal(${f.id})">
    <div class="pic">${pic}</div>
    <div class="body">
      <div class="name">${escapeHtml(f.name)}</div>
      <div class="desc">${escapeHtml(f.description || 'No description')}</div>
      <div class="fc-line">${priceHtml}${badges}</div>
    </div>
    ${action}
  </div>`;
}
function renderFoodCats() {
  const el = document.getElementById('foodCats');
  if (!el) return;
  const cats = [...new Set((window._foods || []).map(f => f.categoryName).filter(Boolean))];
  if (!cats.length) { el.innerHTML = ''; return; }
  const tabs = [{ id: '__all__', name: 'All' }].concat(cats.map(c => ({ id: c, name: c })));
  el.innerHTML = tabs.map(c => `<button type="button" class="cat-tab ${c.id === window._foodCat ? 'active' : ''}" onclick="selectFoodCat('${escapeHtml(c.id)}')">${escapeHtml(c.name)}</button>`).join('');
}
function selectFoodCat(cat) {
  window._foodCat = cat;
  renderFoodCats();
  renderFoodItems();
}
function renderFoodItems() {
  const el = document.getElementById('foodList');
  if (!el) return;
  const list = (window._foods || []).filter(f => window._foodCat === '__all__' || f.categoryName === window._foodCat);
  if (!list.length) { el.innerHTML = emptyState('bi-egg-fried', 'No dishes here', 'Try another category.'); return; }
  el.innerHTML = list.map(foodCardHtml).join('');
}



function backToRestaurants() {
  document.getElementById('foodView').classList.add('d-none');
  document.getElementById('restaurantList').classList.remove('d-none');
  loadRestaurants();
}

async function addToCart(foodId, restaurantId) {
  try {
    const cart = await api('/cart/items');
    const items = cart.items || [];
    if (items.length && restaurantId != null) {
      const other = items.find(it => it.restaurantId != null && it.restaurantId !== restaurantId);
      if (other) {
        const ok = await confirmDialog('Different restaurant',
          'Your cart already has items from another restaurant. Do you want to clear your cart and add this item?');
        if (!ok) { toast('Cancelled', 'Item not added so your cart stays as is'); return; }
        await api('/cart/items', 'DELETE');
      }
    }
    await api('/cart/items', 'POST', { foodId, quantity: 1 });
    toast('Added', 'Item added to your cart');
    loadCart();
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

let _fmId = null;
function confirmDialog(title, bodyHtml) {
  return new Promise((resolve) => {
    const m = document.getElementById('confirmModal');
    if (!m) { resolve(true); return; }
    document.getElementById('confirmTitle').textContent = title;
    document.getElementById('confirmBody').innerHTML = bodyHtml;
    const ok = document.getElementById('confirmOk');
    const cancel = document.getElementById('confirmCancel');
    const inst = new bootstrap.Modal(m);
    const done = (val) => { inst.hide(); resolve(val); };
    ok.onclick = () => done(true);
    cancel.onclick = () => done(false);
    inst.show();
  });
}
function openFoodModal(id) {
  const f = (window._foods || []).find(x => x.id === id);
  if (!f) return;
  _fmId = id;
  document.getElementById('fmPic').innerHTML = f.imageUrl ? `<img src="${escapeHtml(f.imageUrl)}" onerror="onImgError(this)" data-label="${escapeHtml(f.name)}">` : `<i class="bi bi-egg-fried"></i>`;
  document.getElementById('fmCat').innerHTML = `${f.categoryName ? `<span class="badge-soft">${escapeHtml(f.categoryName)}</span>` : ''}${f.offer ? `<span class="offer-chip sm"><i class="bi bi-tag-fill"></i> Offer</span>` : ''}`;
  document.getElementById('fmName').textContent = f.name || '';
  document.getElementById('fmDesc').textContent = f.description || 'No description available.';
  const priceHtml = f.offer ? `<span class="old-price">${money(f.price)}</span><span class="new-price">${money(f.discountPrice)}</span>` : `<span class="price">${money(f.price)}</span>`;
  document.getElementById('fmPrice').innerHTML = priceHtml;
  const addBtn = document.getElementById('fmAdd');
  if (f.available) addBtn.classList.remove('d-none'); else addBtn.classList.add('d-none');
  document.getElementById('foodModal').classList.add('show');
  document.body.style.overflow = 'hidden';
}
function closeFoodModal() {
  const m = document.getElementById('foodModal');
  if (!m) return;
  m.classList.remove('show');
  document.body.style.overflow = '';
  _fmId = null;
}
async function fmAddToCart() {
  if (_fmId == null) return;
  const id = _fmId;
  closeFoodModal();
  await addToCart(id, window._currentRestaurantId);
}
document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closeFoodModal(); });

async function loadCart() {
  try {
    const c = await api('/cart/items');
    const el = document.getElementById('cartList');
    const pill = document.getElementById('cartPill');
    const cnt = document.getElementById('cartCount');
    if (!c.items || !c.items.length) {
      el.innerHTML = emptyState('bi-cart3', 'Your cart is empty', 'Add something tasty from a restaurant.');
      document.getElementById('cartSummaryBody').innerHTML = '';
      if (pill) pill.classList.add('d-none');
      if (cnt) cnt.classList.add('d-none');
      const ncc = document.getElementById('navCartCount'); if (ncc) ncc.classList.add('d-none');
      syncBottomBadge('cartCount');
      const ci = document.getElementById('checkoutCard'); if (ci) ci.classList.add('d-none');
      return;
    }
    if (pill) { pill.classList.remove('d-none'); const pt = document.getElementById('cartTotalPill'); if (pt) pt.textContent = money(c.total); }
    if (cnt) { cnt.classList.remove('d-none'); cnt.textContent = c.items.length; }
    const ncc = document.getElementById('navCartCount');
    if (ncc) { ncc.classList.remove('d-none'); ncc.textContent = c.items.length; }
    syncBottomBadge('cartCount');
    const ci = document.getElementById('checkoutCard'); if (ci) ci.classList.remove('d-none');
    window.cartTotal = c.total;
    const ct = document.getElementById('cartCheckoutTotal'); if (ct) ct.textContent = money(c.total);
    initCheckoutMap();
    setTimeout(() => { if (checkoutMap) checkoutMap.invalidateSize(); }, 300);
    prefillCheckoutAddress();
    el.innerHTML = `<div class="row g-2">` + c.items.map(i => `
      <div class="col-12">
        <div class="card"><div class="card-body d-flex align-center gap-3 flex-wrap">
          ${i.imageUrl
            ? `<img class="cart-thumb" src="${i.imageUrl}" alt="${escapeHtml(i.foodName)}" onerror="onImgError(this)" data-label="${escapeHtml(i.foodName)}">`
            : `<div class="cart-thumb cart-thumb--ph"><i class="bi bi-egg-fried"></i></div>`}
          <div class="flex-1 min-w-0">
            <div class="fw-semibold text-truncate">${escapeHtml(i.foodName)}</div>
            <div class="muted small">${money(i.price)} each</div>
          </div>
          <div class="cart-actions d-flex align-center gap-3">
            <div class="d-flex align-center gap-2">
              <button class="icon-btn" style="width:32px;height:32px" onclick="updateQty(${i.id},${i.foodId},${i.quantity - 1})"><i class="bi bi-dash"></i></button>
              <span class="fw-semibold" style="min-width:22px;text-align:center">${i.quantity}</span>
              <button class="icon-btn" style="width:32px;height:32px" onclick="updateQty(${i.id},${i.foodId},${i.quantity + 1})"><i class="bi bi-plus"></i></button>
            </div>
            <div class="fw-bold" style="min-width:80px;text-align:right">${money(i.subtotal)}</div>
            <button class="icon-btn" style="width:32px;height:32px;color:var(--danger)" onclick="removeItem(${i.id})"><i class="bi bi-trash"></i></button>
          </div>
        </div></div>
      </div>`).join('') + `</div>`;
    document.getElementById('cartSummaryBody').innerHTML = `
      <div class="coupon-box mb-3">
        <label class="form-label small mb-1">Coupon</label>
        <div class="d-flex gap-2">
          <input id="cartCoupon" class="form-control form-control-sm" placeholder="Enter code" value="${appliedCouponCode || ''}">
          <button class="btn-brand btn-sm" onclick="applyCartCoupon()">Apply</button>
        </div>
        <div id="cartCouponMsg" class="small mt-1"></div>
        <div id="myCoupons" class="d-flex flex-wrap gap-2 mt-2"></div>
      </div>
      <div class="d-flex justify-between"><span class="muted">Subtotal</span><b id="cartSubtotal">${money(c.total)}</b></div>
      <div class="d-flex justify-between" id="cartDiscountRow" ${appliedCouponCode ? '' : 'style="display:none"'}>
        <span class="muted">Discount</span><b class="save" id="cartDiscount"></b>
      </div>
      <hr>
      <div class="d-flex justify-between mb-2"><span class="fw-semibold">Total</span><b id="cartFinalTotal">${money(c.total)}</b></div>`;
    loadMyCouponsForCart();
    if (appliedCouponCode) applyCartCoupon();
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

let appliedCouponCode = null;
async function applyCartCoupon() {
  const msg = document.getElementById('cartCouponMsg');
  const code = (document.getElementById('cartCoupon')?.value || '').trim();
  if (!code) { if (msg) { msg.className = 'small mt-1 text-danger'; msg.textContent = 'Enter a coupon code'; } return; }
  try {
    const r = await api('/coupons/preview', 'POST', { code });
    appliedCouponCode = r.code;
    if (msg) { msg.className = 'small mt-1 text-success'; msg.textContent = 'Coupon applied · saved ' + money(r.discount); }
    const dr = document.getElementById('cartDiscountRow'); if (dr) dr.style.display = 'flex';
    const d = document.getElementById('cartDiscount'); if (d) d.textContent = '-' + money(r.discount);
    const ft = document.getElementById('cartFinalTotal'); if (ft) ft.textContent = money(r.finalTotal);
    const ct = document.getElementById('cartCheckoutTotal'); if (ct) ct.textContent = money(r.finalTotal);
  } catch (e) {
    appliedCouponCode = null;
    if (msg) { msg.className = 'small mt-1 text-danger'; msg.textContent = e.message; }
    const dr = document.getElementById('cartDiscountRow'); if (dr) dr.style.display = 'none';
    const ft = document.getElementById('cartFinalTotal'); if (ft) ft.textContent = money(window.cartTotal || 0);
    const ct = document.getElementById('cartCheckoutTotal'); if (ct) ct.textContent = money(window.cartTotal || 0);
  }
}
async function loadMyCouponsForCart() {
  const box = document.getElementById('myCoupons'); if (!box) return;
  try {
    const list = await api('/coupons/mine');
    const usable = (list || []).filter(c => !c.used && c.active);
    if (!usable.length) { box.innerHTML = ''; return; }
    box.innerHTML = `<span class="small muted me-1">Your coupons:</span>` + usable.map(c => `
      <button type="button" class="coupon-chip" onclick="useMyCoupon('${escapeHtml(c.code)}')">${escapeHtml(c.code)} · ${c.discountType === 'PERCENTAGE' ? c.value + '%' : money(c.value)}</button>`).join('');
  } catch (e) { box.innerHTML = ''; }
}
function useMyCoupon(code) {
  const inp = document.getElementById('cartCoupon'); if (inp) inp.value = code;
  applyCartCoupon();
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
function selectPay(val) {
  const inp = document.getElementById('ckPayment'); if (inp) inp.value = val;
  document.querySelectorAll('.pay-opt').forEach(b => b.classList.toggle('active', b.dataset.val === val));
}

async function prefillCheckoutAddress() {
  try {
    const a = await api('/customers/me/address');
    if (!a) return;
    const set = (id, v) => { const el = document.getElementById(id); if (el && v != null) el.value = v; };
    set('ckCity', a.city); set('ckStreet', a.street); set('ckBuilding', a.buildingNumber);
    set('ckApartment', a.apartment); set('ckDetails', a.details);
    if (a.latitude != null && a.longitude != null) {
      window._ckLat = a.latitude; window._ckLng = a.longitude;
      setTimeout(() => setCheckoutMarker(a.latitude, a.longitude), 450);
    }
  } catch (e) { /* user can still enter address manually */ }
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
    couponCode: (document.getElementById('cartCoupon')?.value || '').trim() || appliedCouponCode || null,
    latitude: window._ckLat,
    longitude: window._ckLng,
    paymentMethod: document.getElementById('ckPayment').value || 'CARD'
  };
  try {
    if (body.paymentMethod === 'CASH') {
      const order = await api('/orders/checkout', 'POST', body);
      toast('Order placed', 'Order #' + order.id + ' · ' + order.status);
      loadCart();
      navTo('orders'); loadOrders();
      startCancelTimer(order.id);
    } else {
      await startVisaPayment(body);
    }
  } catch (e) { showAlert('checkoutAlert', 'danger', e.message); }
}

async function loadOrders() {
  try {
    showSkeletons('orderList', 4, 132);
    const page = await api('/orders?page=0&size=100');
    const list = (page.content || []).filter(o => o.status !== 'CANCELLED');
    window._orderMap = Object.fromEntries(list.map(o => [o.id, o]));
    const el = document.getElementById('orderList');
    if (!list.length) { el.innerHTML = emptyState('bi-receipt', 'No orders yet', 'Your orders will appear here.'); return; }
    el.innerHTML = list.map(o => `
      <div class="card order-card mb-3 fade-in" style="cursor:pointer" onclick="openOrderDetail(${o.id})">
        <div class="card-body">
        <div class="oc-top">
          <div class="oc-ic"><i class="bi bi-receipt"></i></div>
          <div style="flex:1;min-width:0">
            <div class="oc-title">Order #${o.id}</div>
            <div class="oc-sub">${escapeHtml(o.restaurantName || 'Restaurant')} · ${fmtDateTime(o.createdAt)}</div>
          </div>
          <div class="oc-badges">${statusBadge(o.status)} ${o.deliveryStatus ? statusBadge(o.deliveryStatus) : ''}${o.paymentMethod ? `<span class="badge bg-dark">${escapeHtml(o.paymentMethod)}</span>` : ''}</div>
        </div>
        <div class="oc-meta">
          ${o.deliveryAddress ? `<div class="m"><i class="bi bi-geo-alt"></i> ${escapeHtml(formatAddr(o.deliveryAddress))}</div>` : ''}
          <div class="m"><i class="bi bi-bag"></i> ${o.items.length} item${o.items.length === 1 ? '' : 's'}</div>
        </div>
        <div class="oc-foot">
          <div class="oc-total">${money(o.totalPrice)}${o.discountAmount ? `<span class="save">saved ${money(o.discountAmount)}${o.couponCode ? ' · ' + escapeHtml(o.couponCode) : ''}</span>` : ''}</div>
          <div class="d-flex gap-2">
            ${o.status === 'DELIVERED' && o.reviewed ? `<span class="badge bg-success-subtle text-success"><i class="bi bi-check-circle"></i> Reviewed</span>` : ''}
            ${(o.status === 'DELIVERED' || o.status === 'CANCELLED') ? `<button class="btn btn-sm btn-soft" onclick="event.stopPropagation(); reorder(${o.id})"><i class="bi bi-arrow-repeat"></i> Reorder</button>` : ''}
            ${(o.status !== 'DELIVERED' && o.status !== 'CANCELLED' && o.status !== 'REJECTED') ? `<button class="btn btn-sm btn-brand" onclick="event.stopPropagation(); trackOrder(${o.id})"><i class="bi bi-geo-alt"></i> Track</button>` : ''}
          </div>
        </div>
        </div>
      </div>`).join('');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

let _cancelTimer = null;
async function cancelOrder(id) {
  try {
    await api('/orders/' + id + '/cancel', 'POST');
    toast('Order cancelled', 'Order #' + id);
    loadOrders();
  } catch (e) { toast('Error', e.message, 'danger'); }
}
// 10-second undo window shown right after an order is placed.
function startCancelTimer(orderId) {
  const banner = document.getElementById('cancelBanner');
  const text = document.getElementById('cancelBannerText');
  const btn = document.getElementById('cancelBannerBtn');
  if (!banner) return;
  let remaining = 10;
  clearInterval(_cancelTimer);
  btn.onclick = () => {
    clearInterval(_cancelTimer);
    banner.style.display = 'none';
    cancelOrder(orderId);
  };
  const render = () => { text.textContent = 'Order #' + orderId + ' placed — you can cancel within ' + remaining + 's'; };
  render();
  banner.style.display = 'flex';
  _cancelTimer = setInterval(() => {
    remaining--;
    if (remaining <= 0) {
      clearInterval(_cancelTimer);
      banner.style.display = 'none';
    } else {
      render();
    }
  }, 1000);
}
// ---------- Payment (Stripe, with dev fallback) ----------
// For Visa the order is NOT created until the card payment succeeds.
let _stripe = null, _card = null, _clientSecret = null, _payDev = true;
async function startVisaPayment(body) {
  clearAlert('checkoutAlert');
  window._visaBody = body;
  try {
    const cfg = await api('/payments/config');
    _payDev = cfg.devMode;
    document.getElementById('payDev').style.display = _payDev ? 'block' : 'none';
    document.getElementById('stripeCard').style.display = _payDev ? 'none' : 'block';
    if (!_payDev) {
      const intent = await api('/payments/create-cart-intent', 'POST');
      _clientSecret = intent.clientSecret;
      document.getElementById('payAmount').textContent = 'Amount: ' + money(intent.amount) + ' ' + (intent.currency || '').toUpperCase();
      if (!_stripe) _stripe = Stripe(cfg.publishableKey);
      const elements = _stripe.elements();
      if (_card) { try { _card.destroy(); } catch (e) {} }
      _card = elements.create('card');
      _card.mount('#stripeCard');
    } else {
      document.getElementById('payAmount').textContent = 'Demo payment';
    }
    new bootstrap.Modal(document.getElementById('payModal')).show();
  } catch (e) { showAlert('checkoutAlert', 'danger', e.message); }
}
async function confirmPayment() {
  clearAlert('payAlert');
  if (_payDev) {
    await finishVisaOrder(null);
    const m = bootstrap.Modal.getInstance(document.getElementById('payModal'));
    if (m) m.hide();
    return;
  }
  try {
    const { error, paymentIntent } = await _stripe.confirmCardPayment(_clientSecret, { payment_method: { card: _card } });
    if (error) { showAlert('payAlert', 'danger', error.message); return; }
    if (paymentIntent && (paymentIntent.status === 'succeeded' || paymentIntent.status === 'requires_capture')) {
      await finishVisaOrder(paymentIntent.id);
      const m = bootstrap.Modal.getInstance(document.getElementById('payModal'));
      if (m) m.hide();
    } else if (paymentIntent) {
      showAlert('payAlert', 'warning', 'Payment status: ' + paymentIntent.status);
    }
  } catch (e) { showAlert('payAlert', 'danger', e.message); }
}
async function finishVisaOrder(paymentReference) {
  const body = window._visaBody;
  if (!body) return;
  try {
    body.paymentMethod = 'CARD';
    const order = await api('/orders/checkout', 'POST', body);
    if (paymentReference) {
      await api('/payments/confirm', 'POST', { orderId: order.id, paymentReference });
    }
    toast('Order placed & paid', 'Order #' + order.id);
    window._visaBody = null;
    loadCart();
    navTo('orders'); loadOrders();
    startCancelTimer(order.id);
  } catch (e) { showAlert('checkoutAlert', 'danger', e.message); }
}


// ---------- Order detail + reviews (items / order / driver) ----------
function starInput(id, current) {
  let s = `<div class="stars" id="stars-${id}">`;
  for (let i = 1; i <= 5; i++) {
    s += `<span class="star ${current >= i ? 'on' : ''}" data-v="${i}" onclick="setStar('${id}',${i})">★</span>`;
  }
  s += `</div><input type="hidden" id="${id}" value="${current || 0}">`;
  return s;
}
function setStar(id, v) {
  document.getElementById(id).value = v;
  document.querySelectorAll('#stars-' + id + ' .star').forEach(st =>
    st.classList.toggle('on', parseInt(st.dataset.v) <= v));
}
function reviewBlock(type, targetId, label, existing, orderId) {
  const rid = 'r_' + type + '_' + targetId;
  const cid = 'c_' + type + '_' + targetId;
  const shown = existing
    ? `<div class="rv-done"><i class="bi bi-check-circle text-success"></i> You rated ${existing.rating}★${existing.comment ? ' — ' + escapeHtml(existing.comment) : ''}</div>`
    : `<div class="rv-form">
         ${starInput(rid, 0)}
         <textarea id="${cid}" class="form-control form-control-sm mt-2" rows="2" placeholder="Your comment (optional)"></textarea>
         <button class="btn btn-sm btn-brand mt-2" onclick="submitDetailReview(${orderId},'${type}',${targetId},'${rid}','${cid}')">Submit review</button>
       </div>`;
  return `<div class="rv-block"><div class="rv-label">${label}</div>${shown}</div>`;
}
async function openOrderDetail(id) {
  const o = window._orderMap && window._orderMap[id];
  if (!o) return;
  window._od = o;
  let reviews = [];
  try { reviews = await api('/reviews/order/' + id); } catch (e) { reviews = []; }
  renderOrderDetail(o, reviews);
  new bootstrap.Modal(document.getElementById('orderDetailModal')).show();
}
function renderOrderDetail(o, reviews) {
  const orderRv = reviews.find(r => r.type === 'ORDER');
  const driverRv = o.driverId ? reviews.find(r => r.type === 'DRIVER') : null;
  const canReview = o.status === 'DELIVERED';
  let itemsHtml = o.items.map(it => {
    const itemRv = reviews.find(r => r.type === 'ITEM' && r.targetId === it.foodId);
    const img = it.imageUrl
      ? `<img class="od-item-img" src="${escapeHtml(it.imageUrl)}" alt="${escapeHtml(it.foodName)}" onerror="this.style.display='none'">`
      : '';
    return `<div class="od-item">
        ${img}
        <div class="od-item-main"><div class="od-item-name">${escapeHtml(it.foodName)}</div>
          <div class="od-item-sub">${it.quantity} × ${money(it.unitPrice)}</div></div>
        <div class="od-item-price">${money(it.subtotal)}</div>
      </div>
      ${canReview ? reviewBlock('ITEM', it.foodId, 'Rate this item', itemRv, o.id) : ''}`;
  }).join('');
  const reviewsHtml = canReview
    ? `<div class="od-section"><h6>Reviews</h6>
         ${reviewBlock('ORDER', 0, 'Rate the order', orderRv, o.id)}
         ${o.driverId ? reviewBlock('DRIVER', o.driverId, 'Rate the driver' + (o.driverName ? ' (' + escapeHtml(o.driverName) + ')' : ''), driverRv, o.id) : ''}
       </div>`
    : `<div class="od-section text-muted small">Reviews are available after the order is delivered.</div>`;
  document.getElementById('orderDetailBody').innerHTML = `
    <div class="od-head">
      <div><div class="oc-title">Order #${o.id}</div>
        <div class="oc-sub">${escapeHtml(o.restaurantName || 'Restaurant')} · ${fmtDateTime(o.createdAt)}</div></div>
      <div class="oc-badges">${statusBadge(o.status)} ${o.deliveryStatus ? statusBadge(o.deliveryStatus) : ''}</div>
    </div>
    <div class="od-section"><h6>Items</h6>${itemsHtml}</div>
    <div class="od-section od-totals">
      <div class="row"><span>Subtotal</span><span>${money(o.subtotal)}</span></div>
      <div class="row"><span>Delivery fee</span><span>${money(o.deliveryFee)}</span></div>
      ${o.discountAmount ? `<div class="row save"><span>Discount${o.couponCode ? ' (' + escapeHtml(o.couponCode) + ')' : ''}</span><span>-${money(o.discountAmount)}</span></div>` : ''}
      <div class="row total"><span>Total</span><span>${money(o.totalPrice)}</span></div>
      <div class="row"><span>Payment</span><span>${escapeHtml(o.paymentMethod || '')}${o.paid ? ' · paid' : ''}</span></div>
    </div>
    ${o.deliveryAddress ? `<div class="od-section"><h6>Delivery address</h6><div class="text-muted">${escapeHtml(formatAddr(o.deliveryAddress))}</div></div>` : ''}
    ${reviewsHtml}`;
}
async function submitDetailReview(orderId, type, targetId, ratingId, commentId) {
  const rating = parseInt(document.getElementById(ratingId).value) || 0;
  const comment = document.getElementById(commentId).value;
  if (rating < 1) { toast('Rate first', 'Please choose a star rating', 'danger'); return; }
  try {
    await api('/reviews', 'POST', { orderId, rating, comment, type, targetId });
    toast('Thanks!', 'Review submitted');
    const reviews = await api('/reviews/order/' + orderId);
    const o = window._od;
    renderOrderDetail(o, reviews);
    loadOrders();
  } catch (e) { toast('Error', e.message, 'danger'); }
}

let _couponFilter = 'all';
let _couponList = [];

async function loadCoupons() {
  try {
    _couponList = await api('/coupons/mine');
    renderCouponTabs();
    renderCouponList();
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

function renderCouponTabs() {
  const el = document.getElementById('couponList');
  const now = new Date();
  const all = _couponList.length;
  const valid = _couponList.filter(c => !c.used && (!c.expiryDate || new Date(c.expiryDate) >= now)).length;
  const used = _couponList.filter(c => c.used).length;
  const expired = _couponList.filter(c => !c.used && c.expiryDate && new Date(c.expiryDate) < now).length;
  el.innerHTML = `
    <div class="coupon-tabs">
      <button class="coupon-tab ${_couponFilter === 'all' ? 'active' : ''}" onclick="filterCoupons('all')">All <span class="ct-count">${all}</span></button>
      <button class="coupon-tab ${_couponFilter === 'valid' ? 'active' : ''}" onclick="filterCoupons('valid')">Valid <span class="ct-count">${valid}</span></button>
      <button class="coupon-tab ${_couponFilter === 'used' ? 'active' : ''}" onclick="filterCoupons('used')">Used <span class="ct-count">${used}</span></button>
      <button class="coupon-tab ${_couponFilter === 'expired' ? 'active' : ''}" onclick="filterCoupons('expired')">Expired <span class="ct-count">${expired}</span></button>
    </div>
    <div id="couponCards"></div>`;
}

function filterCoupons(f) {
  _couponFilter = f;
  renderCouponTabs();
  renderCouponList();
}

function renderCouponList() {
  const box = document.getElementById('couponCards');
  if (!box) return;
  const now = new Date();
  let list = _couponList;
  if (_couponFilter === 'valid') list = list.filter(c => !c.used && (!c.expiryDate || new Date(c.expiryDate) >= now));
  else if (_couponFilter === 'used') list = list.filter(c => c.used);
  else if (_couponFilter === 'expired') list = list.filter(c => !c.used && c.expiryDate && new Date(c.expiryDate) < now);

  if (!list.length) {
    box.innerHTML = `<div class="coupon-empty"><i class="bi bi-ticket-perforated"></i><div>No ${_couponFilter === 'all' ? '' : _couponFilter + ' '}coupons</div></div>`;
    return;
  }
  box.innerHTML = list.map(c => {
    const isExpired = c.expiryDate && new Date(c.expiryDate) < now;
    const statusCls = c.used ? 'used' : isExpired ? 'expired' : 'valid';
    const statusLabel = c.used ? 'Used' : isExpired ? 'Expired' : 'Available';
    return `
    <div class="coupon-card mb-2 ${statusCls}">
      <div class="cc-ic"><i class="bi bi-ticket-perforated"></i></div>
      <div style="flex:1;min-width:0">
        <div class="cc-code">${escapeHtml(c.code)}</div>
        <div class="cc-meta">${escapeHtml(c.description || '')}</div>
        <div class="cc-meta mt-1">${c.discountType === 'PERCENTAGE' ? c.value + '% off' : money(c.value) + ' off'}${c.minOrderTotal ? ' · min ' + money(c.minOrderTotal) : ''} · expires ${escapeHtml(c.expiryDate || 'never')}</div>
      </div>
      <span class="coupon-status ${statusCls}">${statusLabel}</span>
    </div>`;
  }).join('');
}


// ---------- Reorder ----------
async function reorder(orderId) {
  try {
    await api('/orders/' + orderId + '/reorder', 'POST');
    toast('Added to cart', 'Previous order items added');
    loadCart();
    navTo('cart');
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

// ---------- Favorites ----------
async function loadFavorites() {
  try {
    showSkeletons('favList', 4, 150);
    const list = await api('/favorites');
    const el = document.getElementById('favList');
    const countLabel = list.length === 1 ? '1 restaurant saved' : `${list.length} restaurants saved`;
    const header = `
      <div class="favorites-hero">
        <div class="favorites-hero-icon"><i class="bi bi-heart-fill"></i></div>
        <div>
          <span class="favorites-kicker">Your collection</span>
          <h2>Favorite restaurants</h2>
          <p>Keep your go-to places close and order again whenever you like.</p>
        </div>
        <span class="favorites-count">${countLabel}</span>
      </div>`;

    if (!list.length) {
      el.innerHTML = `${header}<div class="favorites-empty">${emptyState('bi-heart', 'No favorites yet', 'Tap the heart on any restaurant to save it here.')}</div>`;
      return;
    }

    el.innerHTML = `${header}<div class="favorites-grid">${list.map(f => {
      const rating = f.avgRating != null ? Number(f.avgRating).toFixed(1) : null;
      return `
        <article class="favorite-card fade-in" onclick="viewRestaurant(${f.restaurantId})">
          <div class="favorite-card-cover">
            ${f.imageUrl ? `<img src="${escapeHtml(f.imageUrl)}" alt="${escapeHtml(f.restaurantName)}" onerror="onImgError(this)" data-label="${escapeHtml(f.restaurantName)}">` : '<i class="bi bi-shop"></i>'}
            <span class="favorite-heart" aria-hidden="true"><i class="bi bi-heart-fill"></i></span>
          </div>
          <div class="favorite-card-body">
            <div class="favorite-card-main">
              <h3>${escapeHtml(f.restaurantName)}</h3>
              <div class="favorite-card-meta">
                ${f.cuisine ? `<span class="favorite-cuisine">${escapeHtml(f.cuisine)}</span>` : ''}
                ${rating ? `<span class="favorite-rating"><i class="bi bi-star-fill"></i> ${rating}</span>` : ''}
              </div>
            </div>
            <button type="button" class="favorite-remove" onclick="event.stopPropagation(); toggleFavorite(${f.restaurantId})" aria-label="Remove ${escapeHtml(f.restaurantName)} from favorites" title="Remove from favorites">
              <i class="bi bi-heartbreak"></i><span>Remove</span>
            </button>
          </div>
        </article>`;
    }).join('')}</div>`;
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}
async function toggleFavorite(restaurantId) {
  try {
    const res = await api('/favorites/' + restaurantId, 'POST');
    if (!window._favIds) window._favIds = new Set();
    if (res.favorited) window._favIds.add(restaurantId); else window._favIds.delete(restaurantId);
    if (window._rests) renderRestaurants(window._rests);
    if (res.favorited) toast('Added', 'Added to favorites');
    else toast('Removed', 'Removed from favorites');
    if (document.getElementById('view-favorites') && !document.getElementById('view-favorites').classList.contains('d-none')) loadFavorites();
  } catch (e) { showAlert('alertBox', 'danger', e.message); }
}

// ---------- Live Tracking ----------
let trackTimer = null, trackMap = null, trackMarker = null, trackCustMarker = null, trackRestMarker = null;
async function trackOrder(id) {
  document.getElementById('trkId').textContent = id;
  clearAlert('trackAlert');
  new bootstrap.Modal(document.getElementById('trackModal')).show();
  await loadTracking(id);
  if (trackTimer) clearInterval(trackTimer);
  trackTimer = setInterval(() => loadTracking(id), 5000);
  document.getElementById('trackModal').addEventListener('hidden.bs.modal', () => {
    if (trackTimer) { clearInterval(trackTimer); trackTimer = null; }
    if (trackMap) { trackMap.remove(); trackMap = null; trackMarker = null; trackCustMarker = null; trackRestMarker = null; }
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
    const restLat = t.restaurantLatitude, restLng = t.restaurantLongitude;
    const points = [];
    if (live) points.push([d.latitude, d.longitude]);
    if (custLat != null && custLng != null) points.push([custLat, custLng]);
    if (restLat != null && restLng != null) points.push([restLat, restLng]);
    if (points.length) {
      if (!trackMap) {
        const center = live ? [d.latitude, d.longitude] : (custLat != null ? [custLat, custLng] : [restLat, restLng]);
        trackMap = L.map('map').setView(center, 14);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '&copy; OpenStreetMap' }).addTo(trackMap);
        if (live) trackMarker = L.marker([d.latitude, d.longitude], { icon: L.divIcon({ className: 'pin-driver', html: '<i class="bi bi-bicycle"></i>', iconSize: [30,30] }) }).addTo(trackMap).bindPopup('Driver is here').openPopup();
      }
      if (live) { trackMarker.setLatLng([d.latitude, d.longitude]); }
      if (custLat != null && custLng != null) {
        if (!trackCustMarker) trackCustMarker = L.marker([custLat, custLng], { icon: L.divIcon({ className: 'pin-cust', html: '<i class="bi bi-house"></i>', iconSize: [30,30] }) }).addTo(trackMap).bindPopup('Your address');
        else trackCustMarker.setLatLng([custLat, custLng]);
      }
      if (restLat != null && restLng != null) {
        if (!trackRestMarker) trackRestMarker = L.marker([restLat, restLng], { icon: L.divIcon({ className: 'pin-rest', html: '<i class="bi bi-shop"></i>', iconSize: [30,30] }) }).addTo(trackMap).bindPopup(escapeHtml(t.restaurantName || 'Restaurant') + ' (pickup)');
        else trackRestMarker.setLatLng([restLat, restLng]);
      }
      if (points.length > 1) trackMap.fitBounds(points, { padding: [40, 40], maxZoom: 15 });
      else trackMap.setView(points[0], 15);
      setTimeout(() => trackMap.invalidateSize(), 250);
    } else if (mapEl) {
      if (trackMap) { trackMap.remove(); trackMap = null; trackMarker = null; trackCustMarker = null; trackRestMarker = null; }
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


// Kick off after all declarations are initialized
boot('CUSTOMER', init);
