const AUTH_STORAGE_KEY = "spacemate_client_auth";
const PHONE_STORAGE_KEY = "spacemate_user_phone";

const state = {
  auth: null,
  spaces: [],
  bookings: []
};

function esc(input) {
  return String(input ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  }[char]));
}

function showFeedback(message, type = "info") {
  const bar = document.getElementById("globalFeedback");
  if (!bar) return;

  bar.textContent = message;
  bar.classList.remove("hidden", "feedback-error", "feedback-success");

  if (type === "error") {
    bar.classList.add("feedback-error");
  } else if (type === "success") {
    bar.classList.add("feedback-success");
  }

  window.clearTimeout(showFeedback.timer);
  showFeedback.timer = window.setTimeout(() => {
    bar.classList.add("hidden");
  }, 2400);
}

function isValidPhone(value) {
  return /^1\d{10}$/.test((value || "").trim());
}

function loadAuth() {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) return null;

  try {
    const data = JSON.parse(raw);
    if (!data?.token?.accessToken) return null;
    return data;
  } catch {
    return null;
  }
}

function saveAuth(auth) {
  state.auth = auth;

  if (auth) {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
    const phone = auth.user?.phone;
    if (isValidPhone(phone)) {
      localStorage.setItem(PHONE_STORAGE_KEY, phone);
    }
  } else {
    localStorage.removeItem(AUTH_STORAGE_KEY);
  }
}

function clearAuth() {
  state.auth = null;
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

function getAccessToken() {
  return state.auth?.token?.accessToken || "";
}

function parsePhoneFromUser() {
  const phoneFromUser = state.auth?.user?.phone;
  if (isValidPhone(phoneFromUser)) {
    return phoneFromUser;
  }

  const cached = localStorage.getItem(PHONE_STORAGE_KEY) || "";
  if (isValidPhone(cached)) {
    return cached;
  }

  return "";
}

async function parseJsonSafe(response) {
  const text = await response.text();
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

async function requestApi(path, options = {}) {
  const hasBody = options.body !== undefined && options.body !== null;
  const headers = {
    ...(hasBody ? { "Content-Type": "application/json" } : {}),
    ...(options.headers || {})
  };

  const token = getAccessToken();
  if (token && !headers.Authorization) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(path, {
    ...options,
    headers,
    body: hasBody ? JSON.stringify(options.body) : undefined
  });

  const payload = await parseJsonSafe(response);

  if (!response.ok) {
    throw new Error(payload?.message || `请求失败(${response.status})`);
  }

  if (payload && typeof payload.code === "number") {
    if (payload.code !== 200) {
      throw new Error(payload.message || "请求失败");
    }
    return payload.data;
  }

  return payload;
}

function formatDateTime(input) {
  if (!input) return "-";

  const date = new Date(input);
  if (Number.isNaN(date.getTime())) {
    return esc(input);
  }

  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  const hh = String(date.getHours()).padStart(2, "0");
  const mi = String(date.getMinutes()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
}

function statusLabel(status) {
  if (status === 1) return "待使用";
  if (status === 2) return "已使用";
  return "已取消";
}

function statusClass(status) {
  if (status === 1) return "tag tag-wait";
  if (status === 2) return "tag tag-ok";
  return "tag tag-stop";
}

function renderTabs() {
  const tabButtons = document.querySelectorAll("#mainTabs button[data-tab]");
  tabButtons.forEach((button) => {
    button.addEventListener("click", () => {
      tabButtons.forEach((item) => item.classList.remove("active"));
      button.classList.add("active");

      const tab = button.dataset.tab;
      document.querySelectorAll(".tab-panel").forEach((panel) => panel.classList.add("hidden"));
      const current = document.getElementById(`panel-${tab}`);
      if (current) {
        current.classList.remove("hidden");
      }
    });
  });
}

function renderProfile() {
  const loggedIn = !!state.auth?.token?.accessToken;
  const nickname = state.auth?.user?.nickname || "访客";
  const phone = state.auth?.user?.phone || "未绑定";
  const userId = state.auth?.user?.id || "-";

  const authBadge = document.getElementById("authStateBadge");
  const greeting = document.getElementById("profileGreeting");
  const statusLine = document.getElementById("authStatusLine");
  const detail = document.getElementById("profileDetail");

  const topLoginLink = document.getElementById("topLoginLink");
  const topLogoutBtn = document.getElementById("topLogoutBtn");
  const profileLoginBtn = document.getElementById("profileLoginBtn");
  const profileLogoutBtn = document.getElementById("profileLogoutBtn");

  if (topLoginLink) topLoginLink.classList.toggle("hidden", loggedIn);
  if (topLogoutBtn) topLogoutBtn.classList.toggle("hidden", !loggedIn);
  if (profileLoginBtn) profileLoginBtn.classList.toggle("hidden", loggedIn);
  if (profileLogoutBtn) profileLogoutBtn.classList.toggle("hidden", !loggedIn);

  if (authBadge) {
    authBadge.textContent = loggedIn ? "已登录" : "未登录";
    authBadge.classList.toggle("status-online", loggedIn);
  }

  if (greeting) {
    greeting.textContent = loggedIn ? `欢迎回来，${nickname}` : "欢迎来到 SpaceMate";
  }

  if (statusLine) {
    statusLine.textContent = loggedIn
      ? "你可以直接预约座位，也可以查看个人预约记录。"
      : "当前未登录，你可以先浏览空间，再决定是否登录预约。";
  }

  if (detail) {
    detail.innerHTML = `
      <div><span>用户 ID</span><strong>${esc(userId)}</strong></div>
      <div><span>昵称</span><strong>${esc(nickname)}</strong></div>
      <div><span>手机号</span><strong>${esc(phone)}</strong></div>
      <div><span>状态</span><strong>${loggedIn ? "已登录" : "未登录"}</strong></div>
    `;
  }

  const phoneInput = document.getElementById("minePhoneInput");
  const cachedPhone = parsePhoneFromUser();
  if (phoneInput && cachedPhone) {
    phoneInput.value = cachedPhone;
  }
}

function renderSpaceCards() {
  const keywordInput = document.getElementById("spaceKeywordInput");
  const root = document.getElementById("spaceGrid");

  if (!root || !keywordInput) return;

  const keyword = (keywordInput.value || "").trim().toLowerCase();

  const filtered = state.spaces.filter((space) => {
    const textMatch = keyword ? (space.name || "").toLowerCase().includes(keyword) : true;
    return textMatch;
  });

  const totalEl = document.getElementById("metricSpaceCount");
  const activeEl = document.getElementById("metricActiveCount");
  if (totalEl) totalEl.textContent = String(state.spaces.length);
  if (activeEl) activeEl.textContent = String(state.spaces.filter((space) => space.status === 1).length);

  if (!filtered.length) {
    root.innerHTML = "<article class=\"card muted\">没有匹配的空间，请调整筛选条件。</article>";
    return;
  }

  root.innerHTML = filtered.map((space) => {
    const openWindow = `${esc(space.openStartTime || "--:--")} - ${esc(space.openEndTime || "--:--")}`;
    const status = space.status === 1 ? "营业中" : "已停用";
    const statusCls = space.status === 1 ? "tag tag-ok" : "tag tag-stop";

    return `
      <article class="card space-card">
        <div class="space-head">
          <h3>${esc(space.name || "未命名空间")}</h3>
          <span class="${statusCls}">${status}</span>
        </div>
        <p class="muted">营业时间：${openWindow}</p>
        <p class="muted">价格：¥${esc(space.priceHourly ?? "0")}/小时</p>
        <p class="muted">规则：${esc(space.rules || "暂无")}</p>
        <div class="space-actions">
          <button class="btn" data-action="open-space" data-id="${space.id}" type="button">选择并预约</button>
        </div>
      </article>
    `;
  }).join("");
}

function renderBookings() {
  const root = document.getElementById("bookingList");
  if (!root) return;

  if (!state.bookings.length) {
    root.innerHTML = "<div class=\"muted\">暂无预约记录</div>";
    return;
  }

  root.innerHTML = state.bookings.map((item) => `
    <article class="list-item">
      <div class="list-head">
        <h4>${esc(item.spaceName || "-")} · 座位 ${esc(item.seatNumber || "-")}</h4>
        <span class="${statusClass(item.status)}">${statusLabel(item.status)}</span>
      </div>
      <p class="muted">时间：${formatDateTime(item.startAt)} - ${formatDateTime(item.endAt)}</p>
      <p class="muted">预约单号：${esc(item.bookingNo || "-")}</p>
      <p class="muted">确认码：${esc(item.confirmCode || "-")}</p>
      ${item.status === 1 ? `<button class="btn btn-danger" data-action="cancel-booking" data-id="${item.id}" type="button">取消预约</button>` : ""}
    </article>
  `).join("");
}

async function refreshCurrentUser() {
  if (!state.auth?.token?.accessToken) {
    throw new Error("当前未登录");
  }

  const user = await requestApi("/api/v1/auth/me");
  saveAuth({ ...(state.auth || {}), user });
  renderProfile();
  showFeedback("用户信息已更新", "success");
}

async function doLogout() {
  const refreshToken = state.auth?.token?.refreshToken;

  try {
    if (refreshToken) {
      await requestApi("/api/v1/auth/logout", {
        method: "POST",
        body: { refreshToken }
      });
    }
  } finally {
    clearAuth();
    renderProfile();
    showFeedback("已退出登录", "success");
  }
}

async function loadSpaces() {
  state.spaces = (await requestApi("/api/v1/spaces")) || [];
  renderSpaceCards();
}

async function loadMineBookings() {
  const phoneInput = document.getElementById("minePhoneInput");
  const statusSelect = document.getElementById("mineStatusSelect");
  if (!phoneInput || !statusSelect) return;

  let phone = (phoneInput.value || "").trim();
  if (!phone) {
    phone = parsePhoneFromUser();
    if (phone) {
      phoneInput.value = phone;
    }
  }

  if (!isValidPhone(phone)) {
    throw new Error("请先登录或输入正确手机号");
  }

  localStorage.setItem(PHONE_STORAGE_KEY, phone);

  const qs = new URLSearchParams({ page: "1", size: "50" });
  const status = statusSelect.value;
  if (status) {
    qs.set("status", status);
  }

  const data = await requestApi(`/api/v1/bookings/mine?${qs.toString()}`, {
    headers: { "X-User-Phone": phone }
  });

  state.bookings = data?.items || [];
  renderBookings();
  showFeedback(`已加载 ${state.bookings.length} 条预约`, "success");
}

async function cancelBooking(bookingId) {
  const inputPhone = (document.getElementById("minePhoneInput")?.value || "").trim();
  const phone = inputPhone || parsePhoneFromUser();
  if (!isValidPhone(phone)) {
    throw new Error("取消预约需要手机号");
  }

  const reason = window.prompt("请输入取消原因（可选）") || "";

  await requestApi(`/api/v1/bookings/${bookingId}`, {
    method: "DELETE",
    headers: { "X-User-Phone": phone },
    body: { cancelReason: reason }
  });

  await loadMineBookings();
}

function bindEvents() {
  renderTabs();

  document.getElementById("spaceKeywordInput")?.addEventListener("input", renderSpaceCards);

  document.getElementById("reloadSpaceBtn")?.addEventListener("click", () => {
    loadSpaces().catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("spaceGrid")?.addEventListener("click", (event) => {
    const button = event.target.closest("button[data-action='open-space']");
    if (!button) return;
    const id = button.dataset.id;
    window.location.href = `./space.html?id=${encodeURIComponent(id)}`;
  });

  document.getElementById("loadMineBtn")?.addEventListener("click", () => {
    loadMineBookings().catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("bookingList")?.addEventListener("click", (event) => {
    const button = event.target.closest("button[data-action='cancel-booking']");
    if (!button) return;
    cancelBooking(button.dataset.id).catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("topLogoutBtn")?.addEventListener("click", () => {
    doLogout().catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("profileLogoutBtn")?.addEventListener("click", () => {
    doLogout().catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("profileLoginBtn")?.addEventListener("click", () => {
    window.location.href = "./login.html";
  });

  document.getElementById("profileMeBtn")?.addEventListener("click", () => {
    refreshCurrentUser().catch((error) => showFeedback(error.message, "error"));
  });
}

async function boot() {
  state.auth = loadAuth();
  renderProfile();
  bindEvents();

  try {
    await loadSpaces();
  } catch (error) {
    showFeedback(`空间加载失败：${error.message}`, "error");
  }
}

boot().catch((error) => {
  showFeedback(`初始化失败：${error.message}`, "error");
});
