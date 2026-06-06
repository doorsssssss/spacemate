const AUTH_STORAGE_KEY = "spacemate_client_auth";
const PHONE_STORAGE_KEY = "spacemate_user_phone";

const pageState = {
  auth: null,
  spaceId: null,
  space: null,
  selectedDate: null,
  selectedSlot: null,
  slots: [],
  commentsPage: 1,
  commentsSize: 20,
  comments: []
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

function isValidPhone(value) {
  return /^1\d{10}$/.test((value || "").trim());
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

function clearAuth() {
  pageState.auth = null;
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

function getAccessToken() {
  return pageState.auth?.token?.accessToken || "";
}

function getBusinessPhoneOrAsk(actionText = "操作") {
  const phoneFromUser = pageState.auth?.user?.phone || "";
  const phoneFromCache = localStorage.getItem(PHONE_STORAGE_KEY) || "";
  const candidate = isValidPhone(phoneFromUser) ? phoneFromUser : phoneFromCache;

  if (isValidPhone(candidate)) {
    return candidate;
  }

  const input = (window.prompt(`${actionText}需要手机号，请输入 11 位手机号`) || "").trim();
  if (!isValidPhone(input)) {
    throw new Error("手机号格式不正确，无法继续操作");
  }

  localStorage.setItem(PHONE_STORAGE_KEY, input);
  return input;
}

function getOptionalPhone() {
  const phoneFromUser = pageState.auth?.user?.phone || "";
  const phoneFromCache = localStorage.getItem(PHONE_STORAGE_KEY) || "";
  if (isValidPhone(phoneFromUser)) return phoneFromUser;
  if (isValidPhone(phoneFromCache)) return phoneFromCache;
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

function formatIso(dateText, timeText) {
  return `${dateText}T${timeText}:00`;
}

function formatDate(date) {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

function formatDateTime(value) {
  if (!value) return "刚刚";
  return String(value).replace("T", " ").slice(0, 16);
}

function parseTimeToMinutes(value) {
  const [hour, minute] = String(value || "00:00").split(":").map(Number);
  if (Number.isNaN(hour) || Number.isNaN(minute)) {
    return 0;
  }
  return hour * 60 + minute;
}

function minutesToTime(totalMinutes) {
  const hour = Math.floor(totalMinutes / 60);
  const minute = totalMinutes % 60;
  return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
}

function createSlots(startText, endText, stepMinutes) {
  const start = parseTimeToMinutes(startText);
  const end = parseTimeToMinutes(endText);

  if (end <= start) {
    return [];
  }

  const slots = [];
  for (let point = start; point + stepMinutes <= end; point += stepMinutes) {
    const slotStart = minutesToTime(point);
    const slotEnd = minutesToTime(point + stepMinutes);
    slots.push({
      start: slotStart,
      end: slotEnd,
      startAt: formatIso(pageState.selectedDate, slotStart),
      endAt: formatIso(pageState.selectedDate, slotEnd),
      count: 0,
      loaded: false
    });
  }

  return slots;
}

async function loadSpaceDetail() {
  const data = await requestApi(`/api/v1/spaces/${pageState.spaceId}`);
  pageState.space = data;

  document.getElementById("spacePageTitle").textContent = data.name || "空间时段查询";
  document.getElementById("spacePageSubTitle").textContent = "按当天时段查看剩余可预约座位，也可以查看真实用户评论";

  document.getElementById("spaceInfo").innerHTML = `
    <div><span>空间名称</span><strong>${esc(data.name || "-")}</strong></div>
    <div><span>营业时间</span><strong>${esc(data.openStartTime || "--:--")} - ${esc(data.openEndTime || "--:--")}</strong></div>
    <div><span>价格</span><strong>¥${esc(data.priceHourly ?? "0")}/小时</strong></div>
    <div><span>规则</span><strong>${esc(data.rules || "暂无")}</strong></div>
  `;
}

function renderAuthActions() {
  const loggedIn = !!pageState.auth?.token?.accessToken;
  document.getElementById("spaceLoginLink")?.classList.toggle("hidden", loggedIn);
  document.getElementById("spaceLogoutBtn")?.classList.toggle("hidden", !loggedIn);
}

async function loadSlotAvailability() {
  if (!pageState.space) {
    return;
  }

  const slotMinutes = Number(document.getElementById("slotMinutes").value);
  pageState.selectedDate = document.getElementById("scheduleDate").value;
  pageState.selectedSlot = null;

  document.getElementById("selectedSlotText").textContent = "请先点击上面的可预约时段";
  document.getElementById("seatList").innerHTML = "<div class=\"muted\">暂无数据</div>";

  if (!pageState.selectedDate) {
    throw new Error("请选择查询日期");
  }

  const slots = createSlots(pageState.space.openStartTime, pageState.space.openEndTime, slotMinutes);
  if (!slots.length) {
    document.getElementById("slotList").innerHTML = "<div class=\"muted\">营业时间配置异常，无法生成时段</div>";
    return;
  }

  pageState.slots = slots;
  renderSlots();

  const requests = slots.map(async (slot) => {
    const qs = new URLSearchParams({
      spaceId: String(pageState.spaceId),
      startAt: slot.startAt,
      endAt: slot.endAt,
      page: "1",
      size: "1"
    });
    const result = await requestApi(`/api/v1/seats/available?${qs.toString()}`);
    slot.count = Number(result?.total || 0);
    slot.loaded = true;
  });

  await Promise.all(requests);
  renderSlots();
}

function renderSlots() {
  const root = document.getElementById("slotList");
  if (!root) return;

  if (!pageState.slots.length) {
    root.innerHTML = "<div class=\"muted\">暂无时段数据</div>";
    return;
  }

  root.innerHTML = pageState.slots.map((slot, index) => {
    const available = slot.loaded && slot.count > 0;
    const full = slot.loaded && slot.count === 0;

    const stateTag = !slot.loaded
      ? "<span class=\"tag\">加载中</span>"
      : available
        ? "<span class=\"tag tag-ok\">可预约</span>"
        : "<span class=\"tag tag-stop\">已满</span>";

    const countText = slot.loaded ? `${slot.count} 个可用座位` : "正在计算余量...";

    return `
      <article class="slot-row ${available ? "slot-available" : full ? "slot-full" : ""}">
        <div>
          <div class="slot-time">${esc(slot.start)} - ${esc(slot.end)}</div>
          <div class="muted">${countText}</div>
        </div>
        <div class="slot-actions">
          ${stateTag}
          <button class="btn btn-ghost" data-action="pick-slot" data-index="${index}" type="button" ${available ? "" : "disabled"}>查看可用座位</button>
        </div>
      </article>
    `;
  }).join("");
}

async function pickSlot(index) {
  const slot = pageState.slots[index];
  if (!slot || !slot.loaded || slot.count <= 0) {
    return;
  }

  pageState.selectedSlot = slot;
  document.getElementById("selectedSlotText").textContent = `已选择：${slot.start} - ${slot.end}`;
  await loadSeatsForSelectedSlot();
}

async function loadSeatsForSelectedSlot() {
  if (!pageState.selectedSlot) {
    return;
  }

  const slot = pageState.selectedSlot;
  const qs = new URLSearchParams({
    spaceId: String(pageState.spaceId),
    startAt: slot.startAt,
    endAt: slot.endAt,
    page: "1",
    size: "100"
  });

  const data = await requestApi(`/api/v1/seats/available?${qs.toString()}`);
  const items = data?.items || [];

  const root = document.getElementById("seatList");
  if (!root) return;

  if (!items.length) {
    root.innerHTML = "<div class=\"muted\">该时段已无可用座位</div>";
    return;
  }

  root.innerHTML = items.map((seat) => `
    <article class="list-item">
      <div class="list-head">
        <h4>座位 ${esc(seat.seatNumber || seat.id)}</h4>
        <span class="tag tag-ok">可预约</span>
      </div>
      <p class="muted">插座：${seat.hasSocket ? "有" : "无"} · 安静区：${seat.isQuiet ? "是" : "否"}</p>
      <button class="btn" data-action="book-seat" data-seat-id="${seat.id}" type="button">预约该座位</button>
    </article>
  `).join("");
}

async function bookSeat(seatId) {
  if (!pageState.selectedSlot) {
    throw new Error("请先选择时段");
  }

  const phone = getBusinessPhoneOrAsk("预约");
  const payload = {
    seatId: Number(seatId),
    startAt: pageState.selectedSlot.startAt,
    endAt: pageState.selectedSlot.endAt
  };

  const booking = await requestApi("/api/v1/bookings", {
    method: "POST",
    headers: { "X-User-Phone": phone },
    body: payload
  });

  showFeedback(`预约成功：${booking.bookingNo}（确认码 ${booking.confirmCode}）`, "success");
  await loadSlotAvailability();
}

async function loadComments() {
  const phone = getOptionalPhone();
  const qs = new URLSearchParams({
    page: String(pageState.commentsPage),
    size: String(pageState.commentsSize)
  });
  const headers = phone ? { "X-User-Phone": phone } : {};
  const data = await requestApi(`/api/v1/spaces/${pageState.spaceId}/comments?${qs.toString()}`, { headers });
  pageState.comments = data?.items || [];
  renderComments(data?.total || 0);
}

function renderComments(total) {
  const root = document.getElementById("commentList");
  if (!root) return;

  document.getElementById("commentCountText").textContent = `${total} 条讨论`;

  if (!pageState.comments.length) {
    root.innerHTML = "<div class=\"muted\">还没有评论，来做第一个分享体验的人吧。</div>";
    return;
  }

  root.innerHTML = pageState.comments.map(renderComment).join("");
}

function renderComment(comment) {
  const replies = comment.replies || [];
  return `
    <article class="comment-item">
      <div class="comment-main">
        <div class="comment-avatar">${esc((comment.nickname || "U").slice(0, 1))}</div>
        <div class="comment-body">
          <div class="comment-meta">
            <strong>${esc(comment.nickname || "匿名用户")}</strong>
            <span>${esc(comment.maskedPhone || "")}</span>
            <span>${esc(formatDateTime(comment.createdAt))}</span>
          </div>
          <p>${esc(comment.content)}</p>
          <div class="comment-actions">
            <button class="text-btn ${comment.liked ? "text-btn-hot" : ""}" data-action="toggle-comment-like" data-comment-id="${comment.id}" data-liked="${comment.liked ? "1" : "0"}" type="button">
              ${comment.liked ? "已点赞" : "点赞"} · ${comment.likeCount || 0}
            </button>
            <button class="text-btn" data-action="reply-comment" data-comment-id="${comment.id}" data-nickname="${esc(comment.nickname || "用户")}" type="button">回复</button>
            <span class="muted">${comment.replyCount || replies.length || 0} 条回复</span>
          </div>
        </div>
      </div>
      ${replies.length ? `<div class="reply-list">${replies.map(renderReply).join("")}</div>` : ""}
    </article>
  `;
}

function renderReply(reply) {
  return `
    <article class="reply-item">
      <div class="comment-meta">
        <strong>${esc(reply.nickname || "匿名用户")}</strong>
        <span>${esc(reply.maskedPhone || "")}</span>
        <span>${esc(formatDateTime(reply.createdAt))}</span>
      </div>
      <p>${esc(reply.content)}</p>
      <div class="comment-actions">
        <button class="text-btn ${reply.liked ? "text-btn-hot" : ""}" data-action="toggle-comment-like" data-comment-id="${reply.id}" data-liked="${reply.liked ? "1" : "0"}" type="button">
          ${reply.liked ? "已点赞" : "点赞"} · ${reply.likeCount || 0}
        </button>
        <button class="text-btn" data-action="reply-comment" data-comment-id="${reply.id}" data-nickname="${esc(reply.nickname || "用户")}" type="button">回复</button>
      </div>
    </article>
  `;
}

async function submitComment(parentId = 0) {
  const textarea = document.getElementById("commentContent");
  const content = (textarea?.value || "").trim();
  if (!content) {
    throw new Error("评论内容不能为空");
  }

  const phone = getBusinessPhoneOrAsk(parentId ? "回复" : "评论");
  await requestApi(`/api/v1/spaces/${pageState.spaceId}/comments`, {
    method: "POST",
    headers: { "X-User-Phone": phone },
    body: {
      parentId,
      content
    }
  });

  textarea.value = "";
  clearReplyTarget();
  showFeedback(parentId ? "回复成功" : "评论发布成功", "success");
  await loadComments();
}

function setReplyTarget(commentId, nickname) {
  const target = document.getElementById("replyTarget");
  const content = document.getElementById("commentContent");
  if (!target || !content) return;

  target.dataset.parentId = String(commentId);
  target.innerHTML = `正在回复 <strong>${esc(nickname)}</strong> <button class="text-btn" data-action="cancel-reply" type="button">取消</button>`;
  target.classList.remove("hidden");
  content.placeholder = `回复 ${nickname}...`;
  content.focus();
}

function clearReplyTarget() {
  const target = document.getElementById("replyTarget");
  const content = document.getElementById("commentContent");
  if (!target || !content) return;

  target.dataset.parentId = "0";
  target.textContent = "";
  target.classList.add("hidden");
  content.placeholder = "分享你对这个空间的体验、安静程度、插座情况...";
}

async function toggleCommentLike(commentId, liked) {
  const phone = getBusinessPhoneOrAsk("点赞");
  await requestApi(`/api/v1/comments/${commentId}/like`, {
    method: liked ? "DELETE" : "POST",
    headers: { "X-User-Phone": phone }
  });
  await loadComments();
}

async function logout() {
  const refreshToken = pageState.auth?.token?.refreshToken;

  try {
    if (refreshToken) {
      await requestApi("/api/v1/auth/logout", {
        method: "POST",
        body: { refreshToken }
      });
    }
  } finally {
    clearAuth();
    renderAuthActions();
    showFeedback("已退出登录", "success");
  }
}

function bindEvents() {
  document.getElementById("backBtn")?.addEventListener("click", () => {
    window.location.href = "./index.html";
  });

  document.getElementById("loadSlotsBtn")?.addEventListener("click", () => {
    loadSlotAvailability().catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("slotMinutes")?.addEventListener("change", () => {
    loadSlotAvailability().catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("slotList")?.addEventListener("click", (event) => {
    const button = event.target.closest("button[data-action='pick-slot']");
    if (!button) return;
    pickSlot(Number(button.dataset.index)).catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("seatList")?.addEventListener("click", (event) => {
    const button = event.target.closest("button[data-action='book-seat']");
    if (!button) return;
    bookSeat(Number(button.dataset.seatId)).catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("commentSubmitBtn")?.addEventListener("click", () => {
    const parentId = Number(document.getElementById("replyTarget")?.dataset.parentId || 0);
    submitComment(parentId).catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("commentRefreshBtn")?.addEventListener("click", () => {
    loadComments().catch((error) => showFeedback(error.message, "error"));
  });

  document.getElementById("commentList")?.addEventListener("click", (event) => {
    const button = event.target.closest("button[data-action]");
    if (!button) return;

    if (button.dataset.action === "toggle-comment-like") {
      toggleCommentLike(Number(button.dataset.commentId), button.dataset.liked === "1")
        .catch((error) => showFeedback(error.message, "error"));
    }

    if (button.dataset.action === "reply-comment") {
      setReplyTarget(Number(button.dataset.commentId), button.dataset.nickname || "用户");
    }
  });

  document.getElementById("replyTarget")?.addEventListener("click", (event) => {
    const button = event.target.closest("button[data-action='cancel-reply']");
    if (!button) return;
    clearReplyTarget();
  });

  document.getElementById("spaceLogoutBtn")?.addEventListener("click", () => {
    logout().catch((error) => showFeedback(error.message, "error"));
  });
}

function resolveSpaceId() {
  const params = new URLSearchParams(window.location.search);
  const id = params.get("id");

  if (!id || Number.isNaN(Number(id))) {
    throw new Error("缺少有效的 spaceId 参数");
  }

  return Number(id);
}

async function boot() {
  pageState.auth = loadAuth();
  pageState.spaceId = resolveSpaceId();
  renderAuthActions();

  const today = formatDate(new Date());
  document.getElementById("scheduleDate").value = today;
  pageState.selectedDate = today;

  bindEvents();

  await loadSpaceDetail();
  await Promise.all([
    loadSlotAvailability(),
    loadComments()
  ]);
}

boot().catch((error) => {
  showFeedback(`初始化失败：${error.message}`, "error");
});