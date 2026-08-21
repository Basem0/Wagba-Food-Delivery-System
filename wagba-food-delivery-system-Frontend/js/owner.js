// ===================== Restaurant Owner page =====================
boot('RESTAURANT_OWNER', init);

function init() {
  loadRestaurant();
  document.querySelectorAll('#ownerTabs .nav-link').forEach(a => {
    a.addEventListener('click', e => {
      e.preventDefault();
      document.querySelectorAll('#ownerTabs .nav-link').forEach(x => x.classList.remove('active'));
      a.classList.add('active');
      ['categories','foods','orders'].forEach(v =>
        document.getElementById(v).classList.toggle('d-none', v !== a.dataset.v));
      if (a.dataset.v === 'foods') loadFoods();
      if (a.dataset.v === 'orders') loadOrders();
    });
  });
}

async function loadRestaurant() {
  try {
    const r = await api('/restaurant-owner/restaurant');
    document.getElementById('profileBox').innerHTML = '';
    document.getElementById('dashboard').classList.remove('d-none');
    document.getElementById('rName').textContent = r.name;
    document.getElementById('rDesc').textContent = r.description || '';
    document.getElementById('rStatus').innerHTML = statusBadge(r.status ? r.status.name : r.status);
    window._cats = r.categories || [];
    loadCategories();
  } catch (e) {
    // no restaurant yet -> show profile form
    document.getElementById('dashboard').classList.add('d-none');
    showProfileForm();
  }
}

function showProfileForm() {
  const me = getUser();
  document.getElementById('profileBox').innerHTML = `
    <div class="card"><div class="card-body">
      <h4>Complete your restaurant profile</h4>
      <p class="text-muted">After submitting, an admin must approve your restaurant.</p>
      <div class="mb-2"><label class="form-label">Name</label><input id="pName" class="form-control"></div>
      <div class="mb-2"><label class="form-label">Description</label><input id="pDesc" class="form-control"></div>
      <div class="mb-2"><label class="form-label">Image URL</label><input id="pImg" class="form-control"></div>
      <button class="btn btn-brand" onclick="submitProfile()">Submit</button>
    </div></div>`;
}

async function submitProfile() {
  const me = getUser();
  const body = {
    name: document.getElementById('pName').value,
    description: document.getElementById('pDesc').value,
    imageUrl: document.getElementById('pImg').value
  };
  try {
    await api('/restaurant-owner/profile?userId=' + me.id, 'POST', body);
    showAlert('alertBox','success','Profile submitted! Waiting for admin approval.');
    loadRestaurant();
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

// ---------- Categories ----------
function loadCategories() {
  const cats = window._cats || [];
  document.getElementById('catList').innerHTML = cats.length
    ? cats.map(c => `<span class="badge bg-light text-dark border me-2 mb-2 p-2">
        ${escapeHtml(c.name)}
        <button class="btn-close btn-close-sm ms-2" onclick="deleteCategory(${c.id})"></button></span>`).join('')
    : '<p class="text-muted">No categories yet.</p>';
}

async function addCategory(e) {
  e.preventDefault();
  try {
    const c = await api('/restaurant-owner/categories', 'POST', { name: document.getElementById('catName').value });
    document.getElementById('catName').value = '';
    window._cats.push(c);
    loadCategories();
    showAlert('alertBox','success','Category added');
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

async function deleteCategory(id) {
  try {
    await api('/restaurant-owner/categories/' + id, 'DELETE');
    window._cats = window._cats.filter(c => c.id !== id);
    loadCategories();
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

// ---------- Foods ----------
async function loadFoods() {
  if (!window._cats || !window._cats.length) {
    try { const r = await api('/restaurant-owner/restaurant'); window._cats = r.categories || []; } catch(e){}
  }
  try {
    const foods = await api('/restaurant-owner/foods');
    const el = document.getElementById('foodList');
    if (!foods.length) { el.innerHTML = '<p class="text-muted">No foods yet.</p>'; return; }
    el.innerHTML = `<table class="table"><thead><tr><th>Name</th><th>Price</th><th>Category</th><th></th></tr></thead><tbody>
      ${foods.map(f => `<tr>
        <td>${escapeHtml(f.name)}</td>
        <td>${money(f.price)}</td>
        <td>${escapeHtml(f.categoryName || '')}</td>
        <td>
          <button class="btn btn-sm btn-outline-secondary" onclick="openFoodModal(${f.id})">Edit</button>
          <button class="btn btn-sm btn-outline-danger" onclick="deleteFood(${f.id})">Delete</button>
        </td></tr>`).join('')}
    </tbody></table>`;
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

function openFoodModal(id) {
  clearAlert('foodAlert');
  const cats = window._cats || [];
  document.getElementById('fCat').innerHTML = cats.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
  if (id) {
    api('/restaurant-owner/foods').then(foods => {
      const f = foods.find(x => x.id === id);
      document.getElementById('fId').value = f.id;
      document.getElementById('fName').value = f.name;
      document.getElementById('fDesc').value = f.description || '';
      document.getElementById('fPrice').value = f.price;
      document.getElementById('fImg').value = f.imageUrl || '';
      document.getElementById('fCat').value = f.categoryId || '';
      new bootstrap.Modal(document.getElementById('foodModal')).show();
    });
  } else {
    document.getElementById('fId').value = '';
    document.getElementById('fName').value = '';
    document.getElementById('fDesc').value = '';
    document.getElementById('fPrice').value = '';
    document.getElementById('fImg').value = '';
    new bootstrap.Modal(document.getElementById('foodModal')).show();
  }
}

async function saveFood() {
  clearAlert('foodAlert');
  const id = document.getElementById('fId').value;
  const body = {
    name: document.getElementById('fName').value,
    description: document.getElementById('fDesc').value,
    price: document.getElementById('fPrice').value,
    imageUrl: document.getElementById('fImg').value,
    categoryId: document.getElementById('fCat').value || null
  };
  try {
    if (id) await api('/restaurant-owner/foods/' + id, 'PUT', body);
    else await api('/restaurant-owner/foods', 'POST', body);
    bootstrap.Modal.getInstance(document.getElementById('foodModal')).hide();
    showAlert('alertBox','success','Food saved');
    loadFoods();
  } catch (e) { showAlert('foodAlert','danger',e.message); }
}

async function deleteFood(id) {
  try {
    await api('/restaurant-owner/foods/' + id, 'DELETE');
    showAlert('alertBox','success','Food deleted');
    loadFoods();
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

// ---------- Orders ----------
async function loadOrders() {
  try {
    const list = await api('/restaurant-owner/orders');
    const el = document.getElementById('orderList');
    if (!list.length) { el.innerHTML = '<p class="text-muted">No orders yet.</p>'; return; }
    el.innerHTML = list.map(o => `
      <div class="card mb-2"><div class="card-body">
        <div class="d-flex justify-content-between">
          <span>Order #${o.id} — ${money(o.totalPrice)}</span>
          ${statusBadge(o.status)}
        </div>
        <ul class="list-unstyled small mt-2">
          ${o.items.map(i => `<li>${escapeHtml(i.foodName)} × ${i.quantity}</li>`).join('')}
        </ul>
        <div>
          ${o.status === 'PENDING' ? `
            <button class="btn btn-sm btn-success" onclick="actOrder(${o.id},'accept')">Accept</button>
            <button class="btn btn-sm btn-danger" onclick="actOrder(${o.id},'reject')">Reject</button>` : ''}
        </div>
      </div></div>`).join('');
  } catch (e) { showAlert('alertBox','danger',e.message); }
}

async function actOrder(id, action) {
  try {
    await api('/restaurant-owner/orders/' + id + '/' + action, 'POST');
    showAlert('alertBox','success','Order ' + action + 'ed');
    loadOrders();
  } catch (e) { showAlert('alertBox','danger',e.message); }
}
