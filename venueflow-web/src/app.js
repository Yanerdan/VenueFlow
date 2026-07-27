import { createApi } from "./api.js";

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];
const gateway = $("#gateway");
gateway.value = localStorage.getItem("venueflow.gateway") || gateway.value;

let api = createApi({ baseUrl: gateway.value });
let profile = null;
let activeResource = null;

const statusLabels = {
  PENDING: "待确认",
  CONFIRMED: "已确认",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
  EXPIRED: "已过期"
};

function resetApi() {
  const value = gateway.value.trim() || "http://127.0.0.1:8080";
  localStorage.setItem("venueflow.gateway", value);
  api = createApi({ baseUrl: value });
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, char => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[char]);
}

function dateTime(value) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? escapeHtml(value)
    : new Intl.DateTimeFormat("zh-CN", {
        month: "short", day: "numeric", hour: "2-digit", minute: "2-digit"
      }).format(date);
}

function emptyState(title, copy) {
  return `<div class="empty"><strong>${escapeHtml(title)}</strong>${escapeHtml(copy)}</div>`;
}

function skeletons(count = 2) {
  return Array.from({ length: count }, () => '<div class="skeleton" aria-label="正在加载"></div>').join("");
}

let messageTimer;
function message(text, isError = false) {
  clearTimeout(messageTimer);
  const node = $("#message");
  node.textContent = text;
  node.className = `message${isError ? " error" : ""}`;
  messageTimer = setTimeout(() => node.classList.add("hidden"), 4500);
}

async function ensureProfile(displayName) {
  try {
    profile = await api.currentProfile();
  } catch (error) {
    if (error.status !== 404) throw error;
    const externalUserId = api.subject();
    if (!externalUserId) throw new Error("登录令牌缺少用户身份");
    profile = await api.createProfile(externalUserId, displayName);
  }
}

async function enterApp(displayName = "VenueFlow 用户") {
  await ensureProfile(displayName);
  $("#auth-view").classList.add("hidden");
  $("#app-view").classList.remove("hidden");
  $("#logout").classList.remove("hidden");
  $("#profile-chip").classList.remove("hidden");
  $("#desktop-nav").classList.remove("hidden");
  $("#mobile-nav").classList.remove("hidden");
  $("#profile-name").textContent = profile.displayName;
  $("#profile-initial").textContent = (profile.displayName || "V").trim().charAt(0).toUpperCase();
  $("#welcome-name").textContent = `你好，${profile.displayName}`;
  await Promise.all([loadResources(), loadBookings(), loadNotifications()]);
}

function exitApp() {
  profile = null;
  activeResource = null;
  $("#auth-view").classList.remove("hidden");
  $("#app-view").classList.add("hidden");
  $("#logout").classList.add("hidden");
  $("#profile-chip").classList.add("hidden");
  $("#desktop-nav").classList.add("hidden");
  $("#mobile-nav").classList.add("hidden");
  $("#slot-panel").classList.remove("open");
}

async function loadResources(text = "") {
  $("#resource-list").innerHTML = skeletons(4);
  const page = text ? await api.search(text) : await api.resources();
  const items = page.items || [];
  $("#resource-count").textContent = `${items.length} 个结果`;
  $("#resource-list").innerHTML = items.length ? items.map((resource, index) => `
    <article class="venue-card">
      <div class="venue-cover" aria-hidden="true">
        <span class="availability">可查看时段</span>
        <span class="venue-index">${String(index + 1).padStart(2, "0")}</span>
      </div>
      <div class="venue-content">
        <h3>${escapeHtml(resource.name)}</h3>
        <div class="venue-meta">
          <span>⌖ ${escapeHtml(resource.location || "地点待定")}</span>
          <span>♙ 容量 ${escapeHtml(resource.capacity || "—")}</span>
        </div>
        <p class="venue-description">${escapeHtml(resource.description || "一处等待被探索的 VenueFlow 精选空间。")}</p>
        <div class="venue-footer">
          <span class="resource-no">${escapeHtml(resource.resourceNo || `VENUE-${resource.id}`)}</span>
          <button data-resource-id="${escapeHtml(resource.id)}" data-resource-name="${escapeHtml(resource.name)}">查看开放时段 →</button>
        </div>
      </div>
    </article>`).join("") : emptyState("暂未找到场馆", "换一个关键词，或稍后刷新再试。");
}

async function loadSlots(resourceId, name) {
  activeResource = { id: resourceId, name };
  $("#slot-title").textContent = name;
  $("#slot-hint").textContent = "选择一个开放时段，然后填写参与人数。";
  $("#slot-list").innerHTML = skeletons(2);
  $("#booking-form").classList.add("hidden");
  $("#slot-panel").classList.add("open");
  const page = await api.slots(resourceId);
  const items = (page.items || []).filter(slot => slot.status === "OPEN" || slot.status === "AVAILABLE");
  $("#slot-list").innerHTML = items.length ? items.map(slot => `
    <button class="slot" data-slot-id="${escapeHtml(slot.id)}">
      <strong>${dateTime(slot.startAt)}</strong>
      <span>至 ${dateTime(slot.endAt)} · 开放预订</span>
    </button>`).join("") : emptyState("暂无开放时段", "该场馆未来 90 天暂不可预订。");
}

async function loadBookings() {
  if (!profile) return;
  $("#booking-list").innerHTML = skeletons(3);
  const page = await api.bookings(profile.id);
  const items = page.items || [];
  $("#booking-list").innerHTML = items.length ? items.map(booking => {
    const actions = [];
    if (booking.status === "PENDING") actions.push(["confirmation", "确认"], ["cancellation", "取消"]);
    if (booking.status === "CONFIRMED") actions.push(["check-in", "签到"], ["cancellation", "取消"]);
    return `<article class="booking-card">
      <div>
        <p class="booking-label">预订编号</p>
        <div class="booking-id">${escapeHtml(booking.bookingNo)}</div>
        <span class="status ${String(booking.status).toLowerCase()}">${escapeHtml(statusLabels[booking.status] || booking.status)}</span>
      </div>
      <div><p class="booking-label">场馆时段</p><div class="booking-value">#${escapeHtml(booking.slotId)}</div></div>
      <div><p class="booking-label">参与人数</p><div class="booking-value">${escapeHtml(booking.quantity)} 人</div></div>
      <div class="card-actions">${actions.map(([action, label]) =>
        `<button data-booking="${escapeHtml(booking.bookingNo)}" data-action="${action}">${label}</button>`
      ).join("")}</div>
    </article>`;
  }).join("") : emptyState("还没有预订", "从“发现”中选择一个场馆与开放时段。");
}

async function loadNotifications() {
  if (!profile) return;
  $("#notification-list").innerHTML = skeletons(3);
  const page = await api.notifications(profile.id);
  const items = page.items || [];
  $("#notification-list").innerHTML = items.length ? items.map(item => `
    <article class="notification-card">
      <h3>${escapeHtml(item.title)}</h3>
      <p>${escapeHtml(item.body || "")}</p>
      <div class="notification-meta">
        <span>${escapeHtml(item.type || "BOOKING")}</span>
        ${item.bookingNo ? `<span>${escapeHtml(item.bookingNo)}</span>` : ""}
        <time>${dateTime(item.createdAt)}</time>
      </div>
    </article>`).join("") : emptyState("通知箱很安静", "预订状态变化后，相关消息会出现在这里。");
}

async function run(operation, success) {
  try {
    resetApi();
    await operation();
    if (success) message(success);
  } catch (error) {
    if (error.status === 401 && !api.hasSession()) {
      exitApp();
      message("登录已过期，请重新登录。", true);
      return;
    }
    const trace = error.traceId ? ` · Trace ${error.traceId}` : "";
    message(`${error.message}${trace}`, true);
  }
}

$$("[data-auth-tab]").forEach(button => button.addEventListener("click", () => {
  $$("[data-auth-tab]").forEach(item => item.classList.toggle("active", item === button));
  $("#login-form").classList.toggle("hidden", button.dataset.authTab !== "login");
  $("#register-form").classList.toggle("hidden", button.dataset.authTab !== "register");
}));

$("#login-form").addEventListener("submit", event => {
  event.preventDefault();
  const data = new FormData(event.currentTarget);
  run(async () => {
    await api.login(data.get("username"), data.get("password"));
    await enterApp(data.get("username"));
  }, "登录成功");
});

$("#register-form").addEventListener("submit", event => {
  event.preventDefault();
  const data = new FormData(event.currentTarget);
  run(async () => {
    await api.register(data.get("username"), data.get("password"));
    await api.login(data.get("username"), data.get("password"));
    await enterApp(data.get("displayName"));
  }, "账户已创建");
});

$("#logout").addEventListener("click", () => run(async () => {
  await api.logout();
  exitApp();
}, "已退出"));

$("#search-form").addEventListener("submit", event => {
  event.preventDefault();
  run(() => loadResources(new FormData(event.currentTarget).get("text").trim()));
});

$("#resource-list").addEventListener("click", event => {
  const button = event.target.closest("[data-resource-id]");
  if (button) run(() => loadSlots(button.dataset.resourceId, button.dataset.resourceName));
});

$("#close-slots").addEventListener("click", () => $("#slot-panel").classList.remove("open"));

$("#slot-list").addEventListener("click", event => {
  const button = event.target.closest("[data-slot-id]");
  if (!button) return;
  $$(".slot").forEach(item => item.classList.toggle("selected", item === button));
  $("#booking-form [name=slotId]").value = button.dataset.slotId;
  $("#booking-form").classList.remove("hidden");
});

$("#booking-form").addEventListener("submit", event => {
  event.preventDefault();
  const data = new FormData(event.currentTarget);
  run(async () => {
    await api.createBooking(profile.id, Number(data.get("slotId")), Number(data.get("quantity")));
    $("#slot-panel").classList.remove("open");
    await loadBookings();
    activateView("bookings");
  }, "预订已创建");
});

$("#booking-list").addEventListener("click", event => {
  const button = event.target.closest("[data-booking]");
  if (button) run(async () => {
    await api.bookingAction(button.dataset.booking, button.dataset.action);
    await Promise.all([loadBookings(), loadNotifications()]);
  }, "预订状态已更新");
});

function activateView(id) {
  $$(".view").forEach(view => view.classList.toggle("hidden", view.id !== id));
  $$("[data-view]").forEach(button => button.classList.toggle("active", button.dataset.view === id));
  const headings = {
    discover: [`你好，${profile?.displayName || ""}`, "浏览场馆并查看未来 90 天的开放时段。"],
    bookings: ["你的场馆安排", "确认、取消或在到场时完成签到。"],
    notifications: ["通知中心", "所有预订动态，集中在一处。"]
  };
  $("#welcome-name").textContent = headings[id][0];
  $("#welcome-copy").textContent = headings[id][1];
  window.scrollTo({ top: 0, behavior: "smooth" });
}

$$("[data-view]").forEach(button => button.addEventListener("click", () => activateView(button.dataset.view)));
$("#refresh-bookings").addEventListener("click", () => run(loadBookings, "预订已刷新"));
$("#refresh-notifications").addEventListener("click", () => run(loadNotifications, "通知已刷新"));
$("#refresh-all").addEventListener("click", () => run(
  () => Promise.all([loadResources(), loadBookings(), loadNotifications()]), "数据已刷新"
));
gateway.addEventListener("change", resetApi);

if (api.hasSession()) run(() => enterApp());
