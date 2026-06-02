const AUTH_STORAGE_KEY = "spacemate_client_auth";
const PHONE_STORAGE_KEY = "spacemate_user_phone";

const pageState = {
  auth: null
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

function isPhone(value) {
  return /^1\d{10}$/.test((value || "").trim());
}

function isEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test((value || "").trim());
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
  }, 2600);
}

function loadAuth() {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) return null;

  try {
    const parsed = JSON.parse(raw);
    if (!parsed?.token?.accessToken) return null;
    return parsed;
  } catch {
    return null;
  }
}

function saveAuth(auth, identifierType, identifier) {
  pageState.auth = auth;
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));

  const phone = auth?.user?.phone;
  if (isPhone(phone)) {
    localStorage.setItem(PHONE_STORAGE_KEY, phone);
  } else if (identifierType === "PHONE" && isPhone(identifier)) {
    localStorage.setItem(PHONE_STORAGE_KEY, identifier.trim());
  }
}

function clearAuth() {
  pageState.auth = null;
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

function getAccessToken() {
  return pageState.auth?.token?.accessToken || "";
}

function getRefreshToken() {
  return pageState.auth?.token?.refreshToken || "";
}

function renderSession() {
  const root = document.getElementById("sessionStatus");
  if (!root) return;

  if (!pageState.auth?.token?.accessToken) {
    root.textContent = "未登录";
    return;
  }

  const user = pageState.auth.user || {};
  root.innerHTML = `
    <div class="kv-list">
      <div><span>用户 ID</span><strong>${esc(user.id || "-")}</strong></div>
      <div><span>昵称</span><strong>${esc(user.nickname || "-")}</strong></div>
      <div><span>手机号</span><strong>${esc(user.phone || "未绑定")}</strong></div>
    </div>
  `;
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

function getIdentifier(typeId, inputId) {
  const type = document.getElementById(typeId).value;
  const identifier = (document.getElementById(inputId).value || "").trim();

  if (!identifier) {
    throw new Error("请输入账号");
  }

  if (type === "PHONE" && !isPhone(identifier)) {
    throw new Error("手机号格式不正确");
  }

  if (type === "EMAIL" && !isEmail(identifier)) {
    throw new Error("邮箱格式不正确");
  }

  return { type, identifier };
}

function switchPanel(panel) {
  document.querySelectorAll("#authTabs button").forEach((button) => {
    button.classList.toggle("active", button.dataset.panel === panel);
  });

  document.querySelectorAll(".auth-panel").forEach((target) => {
    target.classList.toggle("hidden", target.id !== `panel-${panel}`);
  });
}

function syncLoginMethod() {
  const method = document.getElementById("loginMethod").value;
  document.getElementById("loginCodeWrap").classList.toggle("hidden", method !== "CODE");
  document.getElementById("loginPasswordWrap").classList.toggle("hidden", method !== "PASSWORD");
}

async function sendCode(scene, typeId, inputId) {
  const { type, identifier } = getIdentifier(typeId, inputId);

  const data = await requestApi("/api/v1/auth/send-code", {
    method: "POST",
    body: {
      scene,
      identifierType: type,
      identifier
    }
  });

  showFeedback(`验证码已发送到 ${data?.identifier || identifier}，有效期 ${data?.expireSeconds || "-"} 秒`, "success");
}

async function login() {
  const { type, identifier } = getIdentifier("loginIdentifierType", "loginIdentifier");
  const method = document.getElementById("loginMethod").value;

  const body = { identifierType: type, identifier };

  if (method === "PASSWORD") {
    const password = (document.getElementById("loginPassword").value || "").trim();
    if (!password) throw new Error("请输入密码");
    body.password = password;
  } else {
    const code = (document.getElementById("loginCode").value || "").trim();
    if (!code) throw new Error("请输入验证码");
    body.code = code;
  }

  const auth = await requestApi("/api/v1/auth/login", { method: "POST", body });
  saveAuth(auth, type, identifier);
  renderSession();
  showFeedback("登录成功，正在返回客户端首页", "success");

  window.setTimeout(() => {
    window.location.href = "./index.html";
  }, 500);
}

async function register() {
  const { type, identifier } = getIdentifier("registerIdentifierType", "registerIdentifier");
  const code = (document.getElementById("registerCode").value || "").trim();
  const password = (document.getElementById("registerPassword").value || "").trim();
  const agreeTerms = document.getElementById("registerAgreeTerms").checked;

  if (!code) throw new Error("请输入验证码");
  if (!agreeTerms) throw new Error("请先同意服务协议");

  const auth = await requestApi("/api/v1/auth/register", {
    method: "POST",
    body: {
      identifierType: type,
      identifier,
      code,
      password: password || null,
      agreeTerms
    }
  });

  saveAuth(auth, type, identifier);
  renderSession();
  showFeedback("注册成功，已自动登录", "success");

  window.setTimeout(() => {
    window.location.href = "./index.html";
  }, 700);
}

async function resetPassword() {
  const { type, identifier } = getIdentifier("resetIdentifierType", "resetIdentifier");
  const code = (document.getElementById("resetCode").value || "").trim();
  const newPassword = (document.getElementById("resetPassword").value || "").trim();

  if (!code) throw new Error("请输入验证码");
  if (!newPassword) throw new Error("请输入新密码");

  await requestApi("/api/v1/auth/password/reset", {
    method: "POST",
    body: {
      identifierType: type,
      identifier,
      code,
      newPassword
    }
  });

  showFeedback("密码重置成功，请使用新密码登录", "success");
  switchPanel("login");
  document.getElementById("loginIdentifierType").value = type;
  document.getElementById("loginIdentifier").value = identifier;
}

async function refreshToken() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    throw new Error("当前没有可用 refresh token");
  }

  const token = await requestApi("/api/v1/auth/token/refresh", {
    method: "POST",
    body: { refreshToken }
  });

  saveAuth({ ...(pageState.auth || {}), token });
  renderSession();
  showFeedback("Token 刷新成功", "success");
}

async function queryMe() {
  if (!getAccessToken()) {
    throw new Error("请先登录");
  }

  const user = await requestApi("/api/v1/auth/me");
  saveAuth({ ...(pageState.auth || {}), user });
  renderSession();
  showFeedback("已获取当前用户信息", "success");
}

async function logout() {
  const refreshToken = getRefreshToken();

  if (refreshToken) {
    await requestApi("/api/v1/auth/logout", {
      method: "POST",
      body: { refreshToken }
    });
  }

  clearAuth();
  renderSession();
  showFeedback("已退出登录", "success");
}

function bindEvents() {
  document.querySelectorAll("#authTabs button").forEach((button) => {
    button.addEventListener("click", () => switchPanel(button.dataset.panel));
  });

  document.getElementById("loginMethod")?.addEventListener("change", syncLoginMethod);

  document.getElementById("sendLoginCodeBtn")?.addEventListener("click", () => {
    sendCode("LOGIN", "loginIdentifierType", "loginIdentifier").catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("sendRegisterCodeBtn")?.addEventListener("click", () => {
    sendCode("REGISTER", "registerIdentifierType", "registerIdentifier").catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("sendResetCodeBtn")?.addEventListener("click", () => {
    sendCode("RESET_PASSWORD", "resetIdentifierType", "resetIdentifier").catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("loginSubmitBtn")?.addEventListener("click", () => {
    login().catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("registerSubmitBtn")?.addEventListener("click", () => {
    register().catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("resetSubmitBtn")?.addEventListener("click", () => {
    resetPassword().catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("refreshTokenBtn")?.addEventListener("click", () => {
    refreshToken().catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("queryMeBtn")?.addEventListener("click", () => {
    queryMe().catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("logoutBtn")?.addEventListener("click", () => {
    logout().catch((error) => showFeedback(error.message, "error"));
  });
}

function boot() {
  pageState.auth = loadAuth();
  bindEvents();
  syncLoginMethod();
  renderSession();
}

boot();
