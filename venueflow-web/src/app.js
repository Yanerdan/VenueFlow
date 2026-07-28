import { createApi } from "./api.js?v=20260728-c27";

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];
const gateway = $("#gateway");
gateway.value = localStorage.getItem("venueflow.gateway") || gateway.value;
let api = createApi({ baseUrl: gateway.value });
let profile;

const labels = {
  PENDING_CONFIRMATION: "等待审批", CONFIRMED: "审批通过", COMPLETED: "已核销",
  CANCELLED: "已撤回", EXPIRED: "已失效"
};
const escapeHtml = value => String(value ?? "").replace(/[&<>"']/g, char =>
  ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
const dateTime = value => value ? new Intl.DateTimeFormat("zh-CN", {
  month: "short", day: "numeric", hour: "2-digit", minute: "2-digit"
}).format(new Date(value)) : "—";
const empty = (title, copy) => `<div class="empty"><strong>${title}</strong>${copy}</div>`;
const skeletons = (count = 3) => Array.from({ length: count }, () => '<div class="skeleton"></div>').join("");

let messageTimer;
function notify(text, error = false) {
  clearTimeout(messageTimer);
  $("#message").textContent = text;
  $("#message").className = `message${error ? " error" : ""}`;
  messageTimer = setTimeout(() => $("#message").classList.add("hidden"), 4500);
}
function fail(error) {
  notify(`${error.message}${error.traceId ? ` · 追踪号 ${error.traceId}` : ""}`, true);
}
function resetApi() {
  const value = gateway.value.trim() || "http://127.0.0.1:8080";
  localStorage.setItem("venueflow.gateway", value);
  api = createApi({ baseUrl: value });
}
async function ensureProfile(registration) {
  try { profile = await api.currentProfile(); }
  catch (error) {
    if (error.status !== 404) throw error;
    profile = await api.createProfile(
      api.subject(),
      registration?.displayName || registration || "校园用户",
      typeof registration === "object" ? registration : {}
    );
  }
}
function renderProfile() {
  if (!profile) return;
  const form = $("#profile-form");
  ["displayName", "campusId", "identityType", "department", "phone", "email"].forEach(name => {
    form.elements[name].value = profile[name] || (name === "identityType" ? "OTHER" : "");
  });
}
async function enter(registration) {
  await ensureProfile(registration);
  $("#auth-view").classList.add("hidden");
  $("#app-view").classList.remove("hidden");
  ["#logout", "#profile-chip", "#desktop-nav", "#mobile-nav"].forEach(id => $(id).classList.remove("hidden"));
  $("#profile-name").textContent = profile.displayName;
  $("#profile-initial").textContent = profile.displayName.trim().charAt(0);
  $("#welcome-name").textContent = `${profile.displayName}，发现校园空间`;
  renderProfile();
  if (["APPROVER", "RESOURCE_MANAGER", "SYSTEM_ADMIN"].includes(api.role())) $("#admin-entry").classList.remove("hidden");
  await Promise.allSettled([loadResources(), loadBookings(), loadNotifications()]);
}
function exit() {
  profile = null;
  $("#auth-view").classList.remove("hidden");
  $("#app-view").classList.add("hidden");
  ["#logout", "#profile-chip", "#desktop-nav", "#mobile-nav", "#admin-entry"].forEach(id => $(id).classList.add("hidden"));
}
async function loadResources(text = "") {
  $("#resource-list").innerHTML = skeletons(4);
  const page = text ? await api.search(text) : await api.resources();
  const items = (page.items || []).filter(item => !item.status || item.status === "ACTIVE");
  $("#resource-count").textContent = `${items.length} 个空间`;
  $("#resource-list").innerHTML = items.length ? items.map((r, index) => `
    <article class="venue-card">
      <div class="venue-cover"><span class="availability">开放申请</span><span class="venue-index">${String(index + 1).padStart(2, "0")}</span></div>
      <div class="venue-content"><p class="resource-no">${escapeHtml(r.resourceNo || `RESOURCE-${r.id}`)}</p><h3>${escapeHtml(r.name)}</h3>
        <div class="venue-meta"><span>位置 · ${escapeHtml(r.location || "校内")}</span><span>容量 · ${escapeHtml(r.capacity || "—")} 人</span></div>
        <p class="venue-description">${escapeHtml(r.description || "校园共享空间，具体使用要求以管理部门审批为准。")}</p>
        <div class="venue-footer"><span>统一预约管理</span><button data-resource-id="${r.id}" data-resource-name="${escapeHtml(r.name)}">查看时段 →</button></div>
      </div>
    </article>`).join("") : empty("没有匹配的空间", "请更换关键词或稍后刷新。");
}
async function loadSlots(resourceId, name) {
  $("#slot-title").textContent = name;
  $("#slot-hint").textContent = "选择开放时段并填写预计使用人数，提交后由管理部门审批。";
  $("#slot-list").innerHTML = skeletons(2);
  $("#booking-form").classList.add("hidden");
  $("#slot-panel").classList.add("open");
  const page = await api.slots(resourceId);
  const items = (page.items || []).filter(slot => slot.status === "OPEN");
  $("#slot-list").innerHTML = items.length ? items.map(slot => `
    <button class="slot" data-slot-id="${slot.id}"><strong>${dateTime(slot.startAt)}</strong><span>至 ${dateTime(slot.endAt)} · 可提交申请</span></button>
  `).join("") : empty("暂无开放时段", "该空间未来 90 天暂未发布可申请时段。");
}
async function loadBookings() {
  if (!profile) return;
  $("#booking-list").innerHTML = skeletons();
  const page = await api.bookings(profile.id);
  const items = page.items || [];
  $("#booking-list").innerHTML = items.length ? items.map(b => `
    <article class="booking-card">
      <div><p class="booking-label">申请编号</p><div class="booking-id">${escapeHtml(b.bookingNo)}</div><strong class="activity-name">${escapeHtml(b.activityTitle || "历史场地申请")}</strong><span class="status ${b.status.toLowerCase()}">${labels[b.status] || b.status}</span></div>
      <div><p class="booking-label">资源时段</p><div class="booking-value">#${b.slotId}</div></div>
      <div><p class="booking-label">使用人数</p><div class="booking-value">${b.quantity} 人</div></div>
      <details class="booking-detail"><summary>申请详情</summary><div><span>用途</span><p>${escapeHtml(b.purpose || "历史申请未记录")}</p><span>联系人</span><p>${escapeHtml(b.contactName || "待完善")} · ${escapeHtml(b.contactPhone || "待完善")}</p>${b.note ? `<span>补充说明</span><p>${escapeHtml(b.note)}</p>` : ""}${b.reviewNote ? `<span>处理意见</span><p>${escapeHtml(b.reviewNote)}</p>` : ""}</div></details>
      <div class="card-actions">${["PENDING_CONFIRMATION", "CONFIRMED"].includes(b.status) ? `<button data-booking="${escapeHtml(b.bookingNo)}" data-action="cancellation">撤回申请</button>` : ""}</div>
    </article>`).join("") : empty("还没有申请记录", "在空间大厅选择开放时段即可提交第一份申请。");
}
async function loadNotifications() {
  if (!profile) return;
  $("#notification-list").innerHTML = skeletons(2);
  const page = await api.notifications(profile.id);
  const items = page.items || [];
  $("#notification-list").innerHTML = items.length ? items.map(n => `
    <article class="notification-card"><h3>${escapeHtml(n.title || n.eventType || "预约状态更新")}</h3><p>${escapeHtml(n.content || n.message || "预约状态已发生变化，请查看申请记录。")}</p><div class="notification-meta"><span>${dateTime(n.createdAt)}</span><span>${escapeHtml(n.channel || "站内消息")}</span></div></article>
  `).join("") : empty("暂无新消息", "审批结果和预约变化会显示在这里。");
}
async function refreshAll() {
  await Promise.all([loadResources(), loadBookings(), loadNotifications()]);
  notify("数据已更新");
}

gateway.addEventListener("change", () => { resetApi(); notify("网关地址已保存"); });
$$("[data-auth-tab]").forEach(button => button.addEventListener("click", () => {
  $$("[data-auth-tab]").forEach(item => item.classList.toggle("active", item === button));
  $("#login-form").classList.toggle("hidden", button.dataset.authTab !== "login");
  $("#register-form").classList.toggle("hidden", button.dataset.authTab !== "register");
}));
$("#login-form").addEventListener("submit", async event => {
  event.preventDefault(); resetApi(); const data = new FormData(event.currentTarget);
  try { await api.login(data.get("username"), data.get("password")); await enter(); notify("登录成功"); } catch (error) { fail(error); }
});
$("#register-form").addEventListener("submit", async event => {
  event.preventDefault(); resetApi(); const data = new FormData(event.currentTarget);
  try {
    await api.register(data.get("username"), data.get("password"));
    await api.login(data.get("username"), data.get("password"));
    await enter({
      displayName: data.get("displayName"),
      campusId: data.get("campusId") || null,
      identityType: data.get("identityType"),
      department: data.get("department") || null,
      phone: data.get("phone") || null,
      email: data.get("email") || null
    }); notify("账号已创建，可以开始提交申请");
  } catch (error) { fail(error); }
});
$("#logout").addEventListener("click", async () => { try { await api.logout(); } finally { exit(); } });
$("#refresh-all").addEventListener("click", () => refreshAll().catch(fail));
$("#refresh-bookings").addEventListener("click", () => loadBookings().catch(fail));
$("#refresh-notifications").addEventListener("click", () => loadNotifications().catch(fail));
$("#search-form").addEventListener("submit", event => {
  event.preventDefault(); loadResources(new FormData(event.currentTarget).get("text").trim()).catch(fail);
});
$("#resource-list").addEventListener("click", event => {
  const button = event.target.closest("[data-resource-id]");
  if (button) loadSlots(button.dataset.resourceId, button.dataset.resourceName).catch(fail);
});
$("#slot-list").addEventListener("click", event => {
  const button = event.target.closest("[data-slot-id]"); if (!button) return;
  $$(".slot").forEach(item => item.classList.toggle("selected", item === button));
  $("#booking-form").elements.slotId.value = button.dataset.slotId;
  $("#booking-form").classList.remove("hidden");
});
$("#booking-form").addEventListener("submit", async event => {
  event.preventDefault(); const data = new FormData(event.currentTarget);
  try {
    await api.createBooking(profile.id, Number(data.get("slotId")), Number(data.get("quantity")), {
      activityTitle: data.get("activityTitle"),
      purpose: data.get("purpose"),
      contactName: data.get("contactName"),
      contactPhone: data.get("contactPhone"),
      note: data.get("note") || null
    });
    await loadBookings(); notify("申请已提交，等待管理部门审批");
  } catch (error) { fail(error); }
});
$("#booking-list").addEventListener("click", async event => {
  const button = event.target.closest("[data-booking]"); if (!button) return;
  try { await api.bookingAction(button.dataset.booking, button.dataset.action); await loadBookings(); notify("申请已撤回"); } catch (error) { fail(error); }
});
$("#profile-form").addEventListener("submit", async event => {
  event.preventDefault();
  const data = Object.fromEntries(new FormData(event.currentTarget));
  try {
    profile = await api.updateCampusProfile({
      ...data,
      campusId: data.campusId || null,
      department: data.department || null,
      phone: data.phone || null,
      email: data.email || null,
      expectedVersion: profile.version
    });
    $("#profile-name").textContent = profile.displayName;
    $("#profile-initial").textContent = profile.displayName.trim().charAt(0);
    renderProfile();
    notify("校园资料已保存");
  } catch (error) { fail(error); }
});
$("#close-slots").addEventListener("click", () => $("#slot-panel").classList.remove("open"));
$$("[data-view]").forEach(button => button.addEventListener("click", () => {
  const view = button.dataset.view;
  $$(".view").forEach(section => section.classList.toggle("hidden", section.id !== view));
  $$(`[data-view]`).forEach(item => item.classList.toggle("active", item.dataset.view === view));
}));

if (api.hasSession()) enter().catch(error => { api.clear(); exit(); fail(error); });
