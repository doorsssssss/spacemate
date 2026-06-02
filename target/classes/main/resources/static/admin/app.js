const baseUrl = (localStorage.getItem('spacemate_base_url') || '').replace(/\/$/, '');
const adminToken = localStorage.getItem('spacemate_admin_token') || '';
if (!baseUrl || !adminToken) {
  window.location.href = './login.html';
}

document.getElementById('envInfo').textContent = baseUrl;

const state = {
  spaces: [],
  seatSpaceMap: new Map()
};

const modals = {};

function initModals() {
  modals.space = new bootstrap.Modal(document.getElementById('spaceModal'));
  modals.seat = new bootstrap.Modal(document.getElementById('seatModal'));
  modals.user = new bootstrap.Modal(document.getElementById('userModal'));
}

async function request(path, options = {}) {
  const res = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-Admin-Token': adminToken,
      ...(options.headers || {})
    }
  });
  const payload = await res.json().catch(() => ({}));
  if (!res.ok || payload.code !== 200) {
    throw new Error(payload.message || '请求失败');
  }
  return payload.data;
}

function esc(s) {
  return (s ?? '').toString().replace(/[&<>"']/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
}

function tag(status) {
  return status === 1 ? '<span class="tag tag-ok">启用</span>' : '<span class="tag tag-off">停用</span>';
}

function bookingTag(status) {
  if (status === 1) return '<span class="tag tag-ok">待使用</span>';
  if (status === 2) return '<span class="tag tag-ok">已使用</span>';
  return '<span class="tag tag-off">已取消</span>';
}

function setActiveSection(section) {
  document.querySelectorAll('#sideTabs .nav-link').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.section === section);
  });
  document.querySelectorAll('.section').forEach(sec => sec.classList.remove('active'));
  document.getElementById(`section-${section}`).classList.add('active');
}

document.querySelectorAll('#sideTabs .nav-link').forEach(btn => {
  btn.addEventListener('click', () => setActiveSection(btn.dataset.section));
});

document.getElementById('logoutBtn').addEventListener('click', () => {
  localStorage.removeItem('spacemate_admin_token');
  window.location.href = './login.html';
});

function refreshSpaceSelects() {
  const opts = ['<option value="">全部空间</option>'].concat(
    state.spaces.map(s => `<option value="${s.id}">${esc(s.name)}</option>`)
  ).join('');
  document.getElementById('seatSpaceId').innerHTML = opts;
  document.getElementById('bookingSpaceId').innerHTML = opts;
  document.getElementById('seatSpaceIdEdit').innerHTML = state.spaces.map(s => `<option value="${s.id}">${esc(s.name)}</option>`).join('');
}

async function loadSpaces() {
  const keyword = document.getElementById('spaceKeyword').value.trim();
  const status = document.getElementById('spaceStatus').value;
  const qs = new URLSearchParams({ page: '1', size: '100' });
  if (keyword) qs.set('keyword', keyword);
  if (status !== '') qs.set('status', status);
  const data = await request(`/api/v1/admin/spaces?${qs.toString()}`);
  state.spaces = data.items || [];
  state.seatSpaceMap = new Map(state.spaces.map(s => [s.id, s]));
  document.getElementById('spaceTableBody').innerHTML = state.spaces.map(item => `
    <tr>
      <td>${item.id}</td>
      <td>${esc(item.code)}</td>
      <td>${esc(item.name)}</td>
      <td>${esc(item.openStartTime)} - ${esc(item.openEndTime)}</td>
      <td>${item.priceHourly}</td>
      <td>${tag(item.status)}</td>
      <td>
        <button class="btn btn-sm btn-outline-primary me-1" onclick="editSpace(${item.id})">编辑</button>
        <button class="btn btn-sm btn-outline-secondary me-1" onclick="viewSpace(${item.id})">详情</button>
        <button class="btn btn-sm btn-outline-danger" onclick="deleteSpace(${item.id})">删除</button>
      </td>
    </tr>
  `).join('');
  refreshSpaceSelects();
}

async function loadSeats() {
  const spaceId = document.getElementById('seatSpaceId').value;
  const status = document.getElementById('seatStatus').value;
  const qs = new URLSearchParams({ page: '1', size: '100' });
  if (spaceId) qs.set('spaceId', spaceId);
  if (status !== '') qs.set('status', status);
  const data = await request(`/api/v1/admin/seats?${qs.toString()}`);
  const rows = data.items || [];
  document.getElementById('seatTableBody').innerHTML = rows.map(item => `
    <tr>
      <td>${item.id}</td>
      <td>${esc(state.seatSpaceMap.get(item.spaceId)?.name || item.spaceId)}</td>
      <td>${esc(item.seatNumber)}</td>
      <td>${item.hasSocket ? '是' : '否'}</td>
      <td>${item.isQuiet ? '是' : '否'}</td>
      <td>${tag(item.status)}</td>
      <td>
        <button class="btn btn-sm btn-outline-primary me-1" onclick='editSeat(${item.id}, ${item.spaceId}, ${JSON.stringify(item.seatNumber || '')}, ${item.hasSocket ? 1 : 0}, ${item.isQuiet ? 1 : 0}, ${item.status})'>编辑</button>
        <button class="btn btn-sm btn-outline-secondary me-1" onclick="viewSeat(${item.id})">详情</button>
        <button class="btn btn-sm btn-outline-danger" onclick="deleteSeat(${item.id})">删除</button>
      </td>
    </tr>
  `).join('');
}

async function loadBookings() {
  const spaceId = document.getElementById('bookingSpaceId').value;
  const phone = document.getElementById('bookingPhone').value.trim();
  const status = document.getElementById('bookingStatus').value;
  const qs = new URLSearchParams({ page: '1', size: '100' });
  if (spaceId) qs.set('spaceId', spaceId);
  if (phone) qs.set('phone', phone);
  if (status !== '') qs.set('status', status);
  const data = await request(`/api/v1/admin/bookings?${qs.toString()}`);
  const rows = data.items || [];
  document.getElementById('bookingTableBody').innerHTML = rows.map(item => `
    <tr>
      <td>${item.id}</td>
      <td>${esc(item.phone || '-')}</td>
      <td>${esc(item.spaceName || '-')}</td>
      <td>${esc(item.seatNumber || '-')}</td>
      <td>${esc(item.startAt || '-')}</td>
      <td>${esc(item.endAt || '-')}</td>
      <td>${bookingTag(item.status)}</td>
      <td>
        <button class="btn btn-sm btn-outline-secondary me-1" onclick="viewBooking(${item.id})">详情</button>
        ${item.status === 1 ? `
          <button class="btn btn-sm btn-outline-success me-1" onclick="updateBookingStatus(${item.id},2)">已使用</button>
          <button class="btn btn-sm btn-outline-danger me-1" onclick="updateBookingStatus(${item.id},3)">取消</button>
        ` : ''}
        <button class="btn btn-sm btn-outline-danger" onclick="deleteBooking(${item.id})">删除</button>
      </td>
    </tr>
  `).join('');
}

async function loadUsers() {
  const phone = document.getElementById('userPhone').value.trim();
  const status = document.getElementById('userStatus').value;
  const qs = new URLSearchParams({ page: '1', size: '100' });
  if (phone) qs.set('phone', phone);
  if (status !== '') qs.set('status', status);
  const data = await request(`/api/v1/admin/users?${qs.toString()}`);
  const rows = data.items || [];
  document.getElementById('userTableBody').innerHTML = rows.map(item => `
    <tr>
      <td>${item.id}</td>
      <td>${esc(item.phone || '-')}</td>
      <td>${esc(item.nickname || '-')}</td>
      <td>${item.role === 9 ? '管理员' : '用户'}</td>
      <td>${tag(item.status)}</td>
      <td>${esc(item.createdAt || '-')}</td>
      <td>
        <button class="btn btn-sm btn-outline-secondary me-1" onclick="viewUser(${item.id})">详情</button>
        <button class="btn btn-sm btn-outline-primary me-1" onclick='editUser(${item.id}, ${JSON.stringify(item.phone || '')}, ${JSON.stringify(item.nickname || '')}, ${item.role || 1}, ${item.status || 1})'>编辑</button>
        ${item.role === 9 ? '-' : (item.status === 1
          ? `<button class="btn btn-sm btn-outline-warning me-1" onclick="updateUserStatus(${item.id},0)">禁用</button>`
          : `<button class="btn btn-sm btn-outline-success me-1" onclick="updateUserStatus(${item.id},1)">启用</button>`)}
        ${item.role === 9 ? '' : `<button class="btn btn-sm btn-outline-danger" onclick="deleteUser(${item.id})">删除</button>`}
      </td>
    </tr>
  `).join('');
}

async function loadDashboard() {
  const date = document.getElementById('dashboardDate').value;
  const q = date ? `?date=${date}` : '';
  const data = await request(`/api/v1/admin/dashboard/overview${q}`);
  document.getElementById('mTotal').textContent = data.totalBookings ?? 0;
  document.getElementById('mPending').textContent = data.pendingBookings ?? 0;
  document.getElementById('mUsed').textContent = data.usedBookings ?? 0;
  document.getElementById('mCancelled').textContent = data.cancelledBookings ?? 0;
  document.getElementById('mRate').textContent = `${((data.occupancyRate || 0) * 100).toFixed(2)}%`;
}

function openModal(id) {
  modals[id].show();
}

function closeModal(id) {
  modals[id].hide();
}

document.getElementById('addSpaceBtn').addEventListener('click', () => {
  document.getElementById('spaceModalTitle').textContent = '新增空间';
  document.getElementById('spaceForm').reset();
  document.getElementById('spaceId').value = '';
  document.getElementById('spaceCode').disabled = false;
  openModal('space');
});

document.getElementById('saveSpaceBtn').addEventListener('click', async () => {
  try {
    const id = document.getElementById('spaceId').value;
    const payload = {
      code: document.getElementById('spaceCode').value.trim(),
      name: document.getElementById('spaceName').value.trim(),
      openStartTime: document.getElementById('spaceOpenStart').value,
      openEndTime: document.getElementById('spaceOpenEnd').value,
      wifiSsid: document.getElementById('spaceWifiSsid').value.trim(),
      wifiPassword: document.getElementById('spaceWifiPassword').value.trim(),
      rules: document.getElementById('spaceRules').value.trim(),
      priceHourly: Number(document.getElementById('spacePrice').value),
      status: Number(document.getElementById('spaceStatusEdit').value)
    };
    if (!payload.code || !payload.name) throw new Error('请填写完整空间信息');
    if (!payload.openStartTime || !payload.openEndTime || payload.openStartTime >= payload.openEndTime) {
      throw new Error('营业时间不合法');
    }
    if (id) {
      await request(`/api/v1/admin/spaces/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
    } else {
      await request('/api/v1/admin/spaces', { method: 'POST', body: JSON.stringify(payload) });
    }
    closeModal('space');
    await loadSpaces();
    await loadDashboard();
  } catch (e) {
    alert(e.message);
  }
});

window.editSpace = async (id) => {
  const item = await request(`/api/v1/admin/spaces/${id}`);
  document.getElementById('spaceModalTitle').textContent = '编辑空间';
  document.getElementById('spaceId').value = item.id;
  document.getElementById('spaceCode').value = item.code || '';
  document.getElementById('spaceCode').disabled = true;
  document.getElementById('spaceName').value = item.name || '';
  document.getElementById('spaceOpenStart').value = item.openStartTime || '';
  document.getElementById('spaceOpenEnd').value = item.openEndTime || '';
  document.getElementById('spaceWifiSsid').value = item.wifiSsid || '';
  document.getElementById('spaceWifiPassword').value = '';
  document.getElementById('spaceRules').value = item.rules || '';
  document.getElementById('spacePrice').value = item.priceHourly || 0;
  document.getElementById('spaceStatusEdit').value = item.status ?? 1;
  openModal('space');
};

window.viewSpace = async (id) => {
  const item = await request(`/api/v1/admin/spaces/${id}`);
  alert(`空间详情\n编码：${item.code}\n名称：${item.name}\n营业：${item.openStartTime}-${item.openEndTime}\n价格：${item.priceHourly}`);
};

window.deleteSpace = async (id) => {
  if (!confirm('确认删除空间？该操作会联动下线座位并取消未来预约。')) return;
  await request(`/api/v1/admin/spaces/${id}`, { method: 'DELETE' });
  await loadSpaces();
  await loadSeats();
  await loadBookings();
  await loadDashboard();
};

document.getElementById('addSeatBtn').addEventListener('click', () => {
  document.getElementById('seatModalTitle').textContent = '新增座位';
  document.getElementById('seatForm').reset();
  document.getElementById('seatId').value = '';
  openModal('seat');
});

document.getElementById('saveSeatBtn').addEventListener('click', async () => {
  try {
    const id = document.getElementById('seatId').value;
    const payload = {
      spaceId: Number(document.getElementById('seatSpaceIdEdit').value),
      seatNumber: document.getElementById('seatNumber').value.trim(),
      hasSocket: Number(document.getElementById('seatHasSocket').value),
      isQuiet: Number(document.getElementById('seatIsQuiet').value),
      status: Number(document.getElementById('seatStatusEdit').value)
    };
    if (!payload.spaceId || !payload.seatNumber) throw new Error('请填写完整座位信息');
    if (id) {
      await request(`/api/v1/admin/seats/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
    } else {
      await request('/api/v1/admin/seats', { method: 'POST', body: JSON.stringify(payload) });
    }
    closeModal('seat');
    await loadSeats();
  } catch (e) {
    alert(e.message);
  }
});

window.editSeat = async (id, spaceId, seatNumber, hasSocket, isQuiet, status) => {
  document.getElementById('seatModalTitle').textContent = '编辑座位';
  document.getElementById('seatId').value = id;
  document.getElementById('seatSpaceIdEdit').value = spaceId;
  document.getElementById('seatNumber').value = seatNumber || '';
  document.getElementById('seatHasSocket').value = hasSocket;
  document.getElementById('seatIsQuiet').value = isQuiet;
  document.getElementById('seatStatusEdit').value = status;
  openModal('seat');
};

window.viewSeat = async (id) => {
  const item = await request(`/api/v1/admin/seats/${id}`);
  alert(`座位详情\n座位号：${item.seatNumber}\n插座：${item.hasSocket ? '是' : '否'}\n安静区：${item.isQuiet ? '是' : '否'}`);
};

window.deleteSeat = async (id) => {
  if (!confirm('确认删除座位？该操作会取消未来预约。')) return;
  await request(`/api/v1/admin/seats/${id}`, { method: 'DELETE' });
  await loadSeats();
  await loadBookings();
};

window.viewBooking = async (id) => {
  const item = await request(`/api/v1/admin/bookings/${id}`);
  alert(`预约详情\n单号：${item.bookingNo}\n空间：${item.spaceName}\n座位：${item.seatNumber}\n状态：${item.status}`);
};

window.updateBookingStatus = async (id, status) => {
  const reason = status === 3 ? prompt('请输入取消原因（可选）', '') || '' : '管理员核销';
  await request(`/api/v1/admin/bookings/${id}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status, reason })
  });
  await loadBookings();
  await loadDashboard();
};

window.deleteBooking = async (id) => {
  if (!confirm('确认删除该预约？')) return;
  await request(`/api/v1/admin/bookings/${id}`, { method: 'DELETE' });
  await loadBookings();
  await loadDashboard();
};

document.getElementById('addUserBtn').addEventListener('click', () => {
  document.getElementById('userModalTitle').textContent = '新增用户';
  document.getElementById('userForm').reset();
  document.getElementById('userId').value = '';
  openModal('user');
});

document.getElementById('saveUserBtn').addEventListener('click', async () => {
  try {
    const id = document.getElementById('userId').value;
    const payload = {
      phone: document.getElementById('userPhoneEdit').value.trim(),
      nickname: document.getElementById('userNicknameEdit').value.trim(),
      role: Number(document.getElementById('userRoleEdit').value),
      status: Number(document.getElementById('userStatusEdit').value)
    };
    if (!payload.phone || !/^1\d{10}$/.test(payload.phone)) throw new Error('手机号格式不正确');
    if (id) {
      await request(`/api/v1/admin/users/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
    } else {
      await request('/api/v1/admin/users', { method: 'POST', body: JSON.stringify(payload) });
    }
    closeModal('user');
    await loadUsers();
  } catch (e) {
    alert(e.message);
  }
});

window.editUser = async (id, phone, nickname, role, status) => {
  document.getElementById('userModalTitle').textContent = '编辑用户';
  document.getElementById('userId').value = id;
  document.getElementById('userPhoneEdit').value = phone || '';
  document.getElementById('userNicknameEdit').value = nickname || '';
  document.getElementById('userRoleEdit').value = role || 1;
  document.getElementById('userStatusEdit').value = status || 1;
  openModal('user');
};

window.viewUser = async (id) => {
  const item = await request(`/api/v1/admin/users/${id}`);
  alert(`用户详情\n手机号：${item.phone}\n昵称：${item.nickname || '-'}\n角色：${item.role === 9 ? '管理员' : '用户'}`);
};

window.updateUserStatus = async (id, status) => {
  await request(`/api/v1/admin/users/${id}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status })
  });
  await loadUsers();
};

window.deleteUser = async (id) => {
  if (!confirm('确认删除该用户？')) return;
  await request(`/api/v1/admin/users/${id}`, { method: 'DELETE' });
  await loadUsers();
};

document.getElementById('searchSpaceBtn').addEventListener('click', loadSpaces);
document.getElementById('searchSeatBtn').addEventListener('click', loadSeats);
document.getElementById('searchBookingBtn').addEventListener('click', loadBookings);
document.getElementById('searchUserBtn').addEventListener('click', loadUsers);
document.getElementById('refreshDashboardBtn').addEventListener('click', loadDashboard);

function today() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

async function initPage() {
  initModals();
  document.getElementById('dashboardDate').value = today();
  await loadSpaces();
  await Promise.all([loadSeats(), loadBookings(), loadUsers(), loadDashboard()]);
}

initPage().catch(err => {
  alert(`初始化失败：${err.message}`);
  localStorage.removeItem('spacemate_admin_token');
  window.location.href = './login.html';
});
