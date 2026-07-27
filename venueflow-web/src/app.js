import { createApi } from "./api.js";

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];
const gateway = $("#gateway");
gateway.value = localStorage.getItem("venueflow.gateway") || gateway.value;
let api = createApi({ baseUrl: gateway.value });
let profile = null;

function resetApi() {
  localStorage.setItem("venueflow.gateway", gateway.value.trim());
  api = createApi({ baseUrl: gateway.value.trim() });
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, char => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[char]);
}

let messageTimer;
function message(text, error = false) {
  clearTimeout(messageTimer);
  const node = $("#message");
  node.textContent = text;
  node.className = `message${error ? " error" : ""}`;
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
  $("#welcome-name").textContent = `你好，${profile.displayName}`;
  await Promise.all([loadResources(), loadBookings(), loadNotifications()]);
}

function exitApp() {
  profile = null;
  $("#auth-view").classList.remove("hidden");
  $("#app-view").classList.add("hidden");
  $("#logout").classList.add("hidden");
}

async function loadResources(text = "") {
  const page = text ? await api.search(text) : await api.resources();
  const items = page.items || [];
  $("#resource-list").innerHTML = items.length ? items.map(resource => `
    <article class="card">
      <h3>${escapeHtml(resource.name)}</h3>
      <div class="meta">${escapeHtml(resource.location || "地点待定")} · 容量 ${escapeHtml(resource.capacity || "—")}</div>
      <p>${escapeHtml(resource.description || "暂无介绍")}</p>
      <button data-resource-id="${resource.id}" data-resource-name="${escapeHtml(resource.name)}">查看时段</button>
    </article>`).join("") : '<div class="empty">没有找到可用场馆。</div>';
}

async function loadSlots(resourceId, name) {
  const page = await api.slots(resourceId);
  const items = (page.items || []).filter(slot => slot.status === "AVAILABLE");
  $("#slot-title").textContent = name;
  $("#slot-list").innerHTML = items.length ? items.map(slot => `
    <button class="slot" data-slot-id="${slot.id}">
      ${escapeHtml(new Date(slot.startAt).toLocaleString())} – ${escapeHtml(new Date(slot.endAt).toLocaleString())}
    </button>`).join("") : '<div class="empty">暂无可预订时段。</div>';
  $("#booking-form").classList.add("hidden");
}

async function loadBookings() {
  if (!profile) return;
  const page = await api.bookings(profile.id);
  const items = page.items || [];
  $("#booking-list").innerHTML = items.length ? items.map(booking => {
    const actions = [];
    if (booking.status === "PENDING") actions.push(["confirmation", "确认"], ["cancellation", "取消"]);
    if (booking.status === "CONFIRMED") actions.push(["check-in", "签到"], ["cancellation", "取消"]);
    return `<article class="card">
      <h3>${escapeHtml(booking.bookingNo)}</h3>
      <div class="meta">时段 #${booking.slotId} · ${booking.quantity} 人 · ${escapeHtml(booking.status)}</div>
      <div class="card-actions">${actions.map(([action, label]) =>
        `<button data-booking="${escapeHtml(booking.bookingNo)}" data-action="${action}">${label}</button>`
      ).join("")}</div>
    </article>`;
  }).join("") : '<div class="empty">还没有预订记录。</div>';
}

async function loadNotifications() {
  if (!profile) return;
  const page = await api.notifications(profile.id);
  const items = page.items || [];
  $("#notification-list").innerHTML = items.length ? items.map(item => `
    <article class="card">
      <h3>${escapeHtml(item.title)}</h3>
      <p>${escapeHtml(item.content)}</p>
      <div class="meta">${escapeHtml(item.eventType)} · ${escapeHtml(new Date(item.createdAt).toLocaleString())}</div>
    </article>`).join("") : '<div class="empty">暂无通知。</div>';
}

async function run(operation, success) {
  try {
    resetApi();
    await operation();
    if (success) message(success);
  } catch (error) {
    message(error.message, true);
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
    await loadBookings();
    activateView("bookings");
  }, "预订已创建");
});

$("#booking-list").addEventListener("click", event => {
  const button = event.target.closest("[data-booking]");
  if (button) run(async () => {
    await api.bookingAction(button.dataset.booking, button.dataset.action);
    await loadBookings();
  }, "预订状态已更新");
});

function activateView(id) {
  $$(".view").forEach(view => view.classList.toggle("hidden", view.id !== id));
  $$("[data-view]").forEach(button => button.classList.toggle("active", button.dataset.view === id));
}
$$("[data-view]").forEach(button => button.addEventListener("click", () => activateView(button.dataset.view)));
$("#refresh-bookings").addEventListener("click", () => run(loadBookings, "预订已刷新"));
$("#refresh-notifications").addEventListener("click", () => run(loadNotifications, "通知已刷新"));
$("#refresh-all").addEventListener("click", () => run(
  () => Promise.all([loadResources(), loadBookings(), loadNotifications()]), "数据已刷新"
));

if (api.hasSession()) run(() => enterApp());
