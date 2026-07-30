import { createApi } from "./api.js?v=20260730-c37";
import { buildCalendarEvent, safeCalendarFilename } from "./self-service.js?v=20260729-c36";

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];
const gateway = $("#gateway");
gateway.value = localStorage.getItem("venueflow.gateway") || gateway.value;

let api = createApi({ baseUrl: gateway.value });
let profile;
let resourceCatalog = [];
let activeCategory = "";
let favoritesOnly = false;
let bookingHistory = [];
let notificationItems = [];
let currentResource;
let selectedSlot;
let availableResourceIds = null;
const categoryCodes = new Map();
const slotCache = new Map();
const resourceCache = new Map();

const labels = {
  PENDING_CONFIRMATION: "等待审批", CONFIRMED: "审批通过", COMPLETED: "已核销",
  CANCELLED: "已结束", EXPIRED: "已失效"
};
const statusClasses = {
  PENDING_CONFIRMATION: "pending", CONFIRMED: "confirmed", COMPLETED: "completed",
  CANCELLED: "cancelled", EXPIRED: "expired"
};
const escapeHtml = value => String(value ?? "").replace(/[&<>"']/g, char =>
  ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
const dateTime = value => value ? new Intl.DateTimeFormat("zh-CN", {
  month: "short", day: "numeric", hour: "2-digit", minute: "2-digit"
}).format(new Date(value)) : "—";
const dateRange = (startAt, endAt) => startAt
  ? `${dateTime(startAt)} — ${new Intl.DateTimeFormat("zh-CN", { hour: "2-digit", minute: "2-digit" }).format(new Date(endAt))}`
  : "时间记录暂不可用";
const empty = (title, copy) => `<div class="empty"><strong>${title}</strong>${copy}</div>`;
const skeletons = (count = 3) => Array.from({ length: count }, () => '<div class="skeleton"></div>').join("");
const ruleSummary = resource =>
  `至少提前 ${resource.minAdvanceHours ?? 0} 小时 · 最多提前 ${resource.maxAdvanceDays ?? 90} 天 · 单次不超过 ${resource.maxDurationMinutes ?? 480} 分钟`;
const approvalLabel = resource => resource?.approvalStages?.length
  ? resource.approvalStages.map(stage => stage.stageName).join(" → ")
  : resource?.approvalMode === "TWO_STAGE" ? "院系初审 + 校级终审" : "管理部门直接审批";
const draftFields = ["activityTitle", "purpose", "contactName", "contactPhone", "note", "quantity"];
const identityKey = suffix => `venueflow.${suffix}.${profile?.externalUserId || profile?.id || "anonymous"}`;
const localDate = value => {
  const date = new Date(value);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
};
const publicComplete = resource =>
  Boolean(resource.name?.trim() && resource.location?.trim() && Number(resource.capacity) > 0 && resource.ownerDepartment?.trim());

function favoriteResourceIds() {
  try { return new Set(JSON.parse(localStorage.getItem(identityKey("favorite-resources")) || "[]").map(Number)); }
  catch { return new Set(); }
}
function saveFavoriteResourceIds(ids) {
  localStorage.setItem(identityKey("favorite-resources"), JSON.stringify([...ids]));
}
function renderFavoriteFilter() {
  const ids = favoriteResourceIds();
  $("#favorite-count").textContent = String(ids.size);
  $("#favorites-filter").classList.toggle("active", favoritesOnly);
  $("#favorites-filter").setAttribute("aria-pressed", String(favoritesOnly));
}

let messageTimer;
function notify(text, error = false) {
  clearTimeout(messageTimer);
  $("#message").textContent = text;
  $("#message").className = `message${error ? " error" : ""}`;
  messageTimer = setTimeout(() => $("#message").classList.add("hidden"), 5000);
}
function fail(error) {
  const friendlyMessages = {
    BOOKING_CAPACITY_UNAVAILABLE: "该时段剩余容量不足，请减少使用人数或选择其他时段",
    BOOKING_VALIDATION_FAILED: "该时段不符合提前预约或最长使用规则，请选择其他时段"
  };
  const message = friendlyMessages[error.code] || error.message || "服务暂时不可用，请稍后重试";
  notify(`${message}${error.traceId ? ` · 追踪号 ${error.traceId}` : ""}`, true);
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
      api.subject(), registration?.displayName || registration || "校园用户",
      typeof registration === "object" ? registration : {}
    );
  }
}
function profileMissingFields() {
  if (!profile) return [];
  const names = { campusId: "学工号", department: "院系/部门", phone: "联系电话", email: "邮箱" };
  return Object.entries(names).filter(([key]) => !profile[key]).map(([, label]) => label);
}
function renderProfile() {
  if (!profile) return;
  const form = $("#profile-form");
  ["displayName", "campusId", "identityType", "department", "phone", "email"].forEach(name => {
    form.elements[name].value = profile[name] || (name === "identityType" ? "OTHER" : "");
  });
  const authoritative = Boolean(profile.authoritativeSource);
  ["campusId", "identityType", "department"].forEach(name => {
    form.elements[name].disabled = authoritative;
  });
  const missing = profileMissingFields();
  $("#profile-completeness").innerHTML = missing.length
    ? `<strong>资料完成度 ${Math.round((6 - missing.length) / 6 * 100)}%</strong><span>建议补充：${escapeHtml(missing.join("、"))}。完整资料有助于管理部门联系和审核。</span>`
    : authoritative
      ? `<strong>校园身份已由组织目录同步</strong><span>${escapeHtml(profile.department || "学校组织")} · 最近同步 ${escapeHtml(dateTime(profile.directorySyncedAt))}；联系电话和邮箱仍可自行维护。</span>`
      : "<strong>校园资料已完整</strong><span>申请表将自动带入姓名与联系电话，你仍可在提交前修改。</span>";
}
async function loadSsoProviders() {
  try {
    const providers = await api.ssoProviders();
    const ready = (providers || []).filter(provider => provider.ready);
    $("#campus-sso").classList.toggle("hidden", !ready.length);
    $("#sso-providers").innerHTML = ready.map(provider =>
      `<button type="button" data-sso-provider="${escapeHtml(provider.key)}">${escapeHtml(provider.displayName)} <span>→</span></button>`
    ).join("");
  } catch {
    $("#campus-sso").classList.add("hidden");
  }
}
function renderPersonalSummary() {
  const active = bookingHistory.filter(item => ["PENDING_CONFIRMATION", "CONFIRMED"].includes(item.status)).length;
  const approved = bookingHistory.filter(item => item.status === "CONFIRMED").length;
  $("#summary-active").textContent = String(active);
  $("#summary-approved").textContent = String(approved);
  const read = notificationReadIds();
  $("#summary-messages").textContent = String(notificationItems.filter(item => !read.has(String(item.id))).length);
  const missing = profileMissingFields();
  $("#summary-next").textContent = missing.length
    ? `先完善 ${missing.slice(0, 2).join("、")}`
    : active
      ? `有 ${active} 项申请正在流转`
      : "选择合适空间并提交申请";
}
async function enter(registration) {
  await ensureProfile(registration);
  $("#auth-view").classList.add("hidden");
  $("#app-view").classList.remove("hidden");
  ["#logout", "#profile-chip", "#desktop-nav", "#mobile-nav"].forEach(id => $(id).classList.remove("hidden"));
  $("#profile-name").textContent = profile.displayName;
  $("#profile-initial").textContent = profile.displayName.trim().charAt(0);
  $("#welcome-name").textContent = `${profile.displayName}，欢迎回来`;
  renderProfile();
  renderFavoriteFilter();
  restoreDraft();
  renderPersonalSummary();
  if (["APPROVER", "RESOURCE_MANAGER", "SYSTEM_ADMIN"].includes(api.role())) $("#admin-entry").classList.remove("hidden");
  await Promise.allSettled([loadResources(), loadBookings(), loadNotifications()]);
}
function exit() {
  profile = null;
  bookingHistory = [];
  notificationItems = [];
  $("#auth-view").classList.remove("hidden");
  $("#app-view").classList.add("hidden");
  ["#logout", "#profile-chip", "#desktop-nav", "#mobile-nav", "#admin-entry"].forEach(id => $(id).classList.add("hidden"));
}
function filteredResources() {
  const data = new FormData($("#search-form"));
  const requiredCapacity = Number(data.get("capacity")) || 0;
  const accepted = activeCategory ? activeCategory.split(",") : null;
  const favorites = favoriteResourceIds();
  return resourceCatalog.filter(resource =>
    (!accepted || accepted.includes(categoryCodes.get(resource.categoryId))) &&
    (!favoritesOnly || favorites.has(Number(resource.id))) &&
    Number(resource.capacity) >= requiredCapacity &&
    (!availableResourceIds || availableResourceIds.has(Number(resource.id))));
}
function renderResources() {
  const items = filteredResources();
  const favorites = favoriteResourceIds();
  $("#resource-count").textContent = `${items.length} 个空间`;
  $("#resource-list").innerHTML = items.length ? items.map((resource, index) => `
    <article class="venue-card">
      <div class="venue-cover"><span class="availability">开放申请</span><button class="favorite-button${favorites.has(Number(resource.id)) ? " active" : ""}" data-favorite-resource="${resource.id}" aria-pressed="${favorites.has(Number(resource.id))}" aria-label="${favorites.has(Number(resource.id)) ? "取消收藏" : "收藏"} ${escapeHtml(resource.name)}">${favorites.has(Number(resource.id)) ? "★" : "☆"}</button><span class="venue-index">${String(index + 1).padStart(2, "0")}</span></div>
      <div class="venue-content"><p class="resource-no">${escapeHtml(resource.resourceNo || `RESOURCE-${resource.id}`)}</p><h3>${escapeHtml(resource.name)}</h3>
        <div class="venue-meta"><span>位置 · ${escapeHtml(resource.location || "校内")}</span><span>容量 · ${escapeHtml(resource.capacity || "—")} 人</span><span>负责单位 · ${escapeHtml(resource.ownerDepartment || "校级资源中心")}</span></div>
        <p class="venue-description">${escapeHtml(resource.description || "校园共享空间，具体使用要求以管理部门审批为准。")}</p>
        <div class="service-cues"><span>${escapeHtml(approvalLabel(resource))}</span><span>未来 90 天时段</span></div>
        <div class="booking-policy"><strong>申请规则</strong><span>${escapeHtml(ruleSummary(resource))}</span>${resource.bookingNotice ? `<p>${escapeHtml(resource.bookingNotice)}</p>` : ""}</div>
        <div class="venue-footer"><span>${escapeHtml(resource.ownerDepartment || "资源管理部门")}负责</span><button data-resource-id="${resource.id}" data-resource-name="${escapeHtml(resource.name)}">查看开放时段 →</button></div>
      </div>
    </article>`).join("") : empty("没有匹配的空间", "请切换分类、更换关键词或稍后刷新。");
}
async function loadResources() {
  $("#resource-list").innerHTML = skeletons(4);
  const filters = new FormData($("#search-form"));
  const text = String(filters.get("text") || "").trim();
  const intendedDate = String(filters.get("date") || "");
  const [page, categories] = await Promise.all([
    text ? api.search(text) : api.resources(),
    categoryCodes.size ? Promise.resolve([]) : api.categories().catch(() => [])
  ]);
  (categories.items || categories || []).forEach(category => categoryCodes.set(category.id, category.code));
  resourceCatalog = (page.items || [])
    .filter(item => (!item.status || item.status === "ACTIVE") && publicComplete(item))
    .sort((a, b) => Number(!String(a.resourceNo).startsWith("VF-CAMPUS-")) - Number(!String(b.resourceNo).startsWith("VF-CAMPUS-")));
  resourceCatalog.forEach(resource => resourceCache.set(Number(resource.id), resource));
  availableResourceIds = null;
  if (intendedDate) {
    const checks = await Promise.all(resourceCatalog.map(async resource => {
      const page = await api.slots(resource.id).catch(() => ({ items: [] }));
      const usable = (page.items || []).some(slot =>
        slot.status === "OPEN" && localDate(slot.startAt) === intendedDate);
      return usable ? Number(resource.id) : null;
    }));
    availableResourceIds = new Set(checks.filter(Boolean));
  }
  renderResources();
}
async function loadSlots(resourceId, name) {
  $("#slot-title").textContent = name;
  $("#slot-hint").textContent = "正在读取开放安排与申请规则…";
  $("#slot-list").innerHTML = skeletons(2);
  $("#booking-form").classList.add("hidden");
  $("#slot-panel").classList.add("open");
  const [page, resource] = await Promise.all([api.slots(resourceId), api.resource(resourceId)]);
  currentResource = resource;
  resourceCache.set(Number(resource.id), resource);
  $("#slot-hint").textContent = `${resource.ownerDepartment || "资源管理部门"}负责 · ${ruleSummary(resource)}${resource.bookingNotice ? `；${resource.bookingNotice}` : ""}`;
  const openSlots = (page.items || []).filter(slot => {
    if (slot.status !== "OPEN") return false;
    if (!String(resource.resourceNo).startsWith("VF-CAMPUS-")) return true;
    const start = new Date(slot.startAt); const end = new Date(slot.endAt);
    return start.getHours() >= 7 && end.getHours() <= 22 && start.toDateString() === end.toDateString();
  });
  const items = await Promise.all(openSlots.map(async slot => ({
    ...slot, capacity: await api.slotCapacity(slot.id).catch(() => null)
  })));
  items.forEach(slot => slotCache.set(Number(slot.id), slot));
  const grouped = items.reduce((map, slot) => {
    const day = localDate(slot.startAt);
    if (!map.has(day)) map.set(day, []);
    map.get(day).push(slot);
    return map;
  }, new Map());
  $("#slot-list").innerHTML = items.length ? [...grouped.entries()].map(([day, slots]) => `
    <div class="slot-day"><strong>${new Intl.DateTimeFormat("zh-CN", { month: "long", day: "numeric", weekday: "short" }).format(new Date(`${day}T12:00:00`))}</strong>
      ${slots.map(slot => `<button class="slot" data-slot-id="${slot.id}" data-remaining="${slot.capacity?.availableQuantity ?? ""}" ${slot.capacity?.availableQuantity <= 0 ? "disabled" : ""}>
        <strong>${new Intl.DateTimeFormat("zh-CN", { hour: "2-digit", minute: "2-digit" }).format(new Date(slot.startAt))} — ${new Intl.DateTimeFormat("zh-CN", { hour: "2-digit", minute: "2-digit" }).format(new Date(slot.endAt))}</strong>
        <span>${slot.capacity ? `剩余 ${slot.capacity.availableQuantity} / ${slot.capacity.staticCapacity} 人` : "可提交申请"}</span>
      </button>`).join("")}</div>
  `).join("") : empty("暂无开放时段", "该空间未来 90 天暂未发布可申请时段，可联系负责单位了解后续安排。");
}
async function cachedSlot(slotId) {
  const key = Number(slotId);
  if (!slotCache.has(key)) slotCache.set(key, await api.slot(key));
  return slotCache.get(key);
}
async function cachedResource(resourceId) {
  const key = Number(resourceId);
  if (!resourceCache.has(key)) resourceCache.set(key, await api.resource(key));
  return resourceCache.get(key);
}
const bookingLabel = booking => booking.status === "CANCELLED" && booking.reviewDecision === "REJECTED"
  ? "未通过" : booking.status === "CANCELLED" ? "已撤回" : labels[booking.status] || booking.status;

async function enrichBooking(item) {
  const [approvalActions, approvalStages, slot] = await Promise.all([
    api.approvalActions(item.bookingNo).catch(() => []),
    api.approvalStages(item.bookingNo).catch(() => item.approvalStages || []),
    cachedSlot(item.slotId).catch(() => null)
  ]);
  const resource = await cachedResource(item.resourceId || slot?.resourceId).catch(() => null);
  return { ...item, approvalActions, approvalStages, slot, resource };
}
function bookingMatchesFilters(booking) {
  const filters = new FormData($("#booking-filter-form"));
  const text = String(filters.get("text") || "").trim().toLowerCase();
  const status = String(filters.get("status") || "");
  const haystack = `${booking.bookingNo} ${booking.activityTitle || ""} ${booking.resource?.name || ""}`.toLowerCase();
  return (!status || booking.status === status) && (!text || haystack.includes(text));
}
function renderBookings(focusBookingNo) {
  const items = bookingHistory.filter(bookingMatchesFilters);
  $("#booking-list").innerHTML = items.length ? items.map(booking => `
    <article class="booking-card${booking.bookingNo === focusBookingNo ? " focused" : ""}" data-booking-card="${escapeHtml(booking.bookingNo)}">
      <div><p class="booking-label">${escapeHtml(booking.bookingNo)}</p><strong class="activity-name">${escapeHtml(booking.activityTitle || "历史场地申请")}</strong><span class="status ${statusClasses[booking.status] || ""}">${escapeHtml(bookingLabel(booking))}</span></div>
      <div><p class="booking-label">使用空间</p><div class="booking-value">${escapeHtml(booking.resource?.name || `资源记录 ${booking.resourceId || "待恢复"}`)}</div><small>${escapeHtml(booking.resource?.location || booking.ownerDepartment || "位置记录暂不可用")}</small></div>
      <div><p class="booking-label">使用时间</p><div class="booking-value">${escapeHtml(dateRange(booking.slot?.startAt, booking.slot?.endAt))}</div><small>${booking.quantity} 人 · ${escapeHtml(booking.ownerDepartment || booking.resource?.ownerDepartment || "管理部门待确认")}</small></div>
      <details class="booking-detail"><summary>查看申请与审批详情</summary><div><span>用途</span><p>${escapeHtml(booking.purpose || "历史申请未记录")}</p><span>联系人</span><p>${escapeHtml(booking.contactName || "待完善")} · ${escapeHtml(booking.contactPhone || "待完善")}</p><span>审批路径</span><p>第 ${booking.currentApprovalStep || 1} / ${booking.totalApprovalSteps || 1} 级 · ${booking.approvalStages?.length ? booking.approvalStages.map(stage => escapeHtml(stage.stageName)).join(" → ") : booking.approvalMode === "TWO_STAGE" ? "院系初审 + 校级终审" : "管理部门直接审批"}</p>${booking.approvalActions.length ? `<ul>${booking.approvalActions.map(action => `<li>第 ${action.approvalStep} 级 ${action.decision === "APPROVED" ? "已通过" : "未通过"}${action.reviewNote ? ` · ${escapeHtml(action.reviewNote)}` : ""}</li>`).join("")}</ul>` : ""}${booking.note ? `<span>补充说明</span><p>${escapeHtml(booking.note)}</p>` : ""}${booking.reviewNote ? `<span>处理意见</span><p>${escapeHtml(booking.reviewNote)}</p>` : ""}</div></details>
      <div class="card-actions"><button data-repeat-booking="${escapeHtml(booking.bookingNo)}">再次申请</button>${booking.status === "CONFIRMED" && booking.slot ? `<button data-calendar-booking="${escapeHtml(booking.bookingNo)}">加入日历</button>` : ""}${["PENDING_CONFIRMATION", "CONFIRMED"].includes(booking.status) && booking.resource ? `<button data-reschedule-booking="${escapeHtml(booking.bookingNo)}">改期</button>` : ""}${["PENDING_CONFIRMATION", "CONFIRMED"].includes(booking.status) ? `<button data-booking="${escapeHtml(booking.bookingNo)}" data-action="cancellation">撤回申请</button>` : ""}</div>
    </article>`).join("") : empty(
      bookingHistory.length ? "没有符合筛选条件的申请" : "还没有申请记录",
      bookingHistory.length ? "请清除筛选条件后查看全部申请。" : "在空间大厅选择开放时段即可提交第一份申请。"
    );
  if (focusBookingNo) {
    $("#booking-list").querySelector(`[data-booking-card="${CSS.escape(focusBookingNo)}"]`)?.scrollIntoView({ behavior: "smooth", block: "center" });
  }
}
async function loadBookings() {
  if (!profile) return;
  $("#booking-list").innerHTML = skeletons();
  const page = await api.bookings(profile.id);
  bookingHistory = await Promise.all((page.items || []).map(enrichBooking));
  renderPersonalSummary();
  renderBookings();
}
function notificationReadIds() {
  try { return new Set(JSON.parse(localStorage.getItem(identityKey("notification-read")) || "[]")); }
  catch { return new Set(); }
}
function markNotificationRead(id) {
  const read = notificationReadIds();
  read.add(String(id));
  localStorage.setItem(identityKey("notification-read"), JSON.stringify([...read].slice(-200)));
}
function renderNotifications() {
  const read = notificationReadIds();
  const unread = notificationItems.filter(item => !read.has(String(item.id))).length;
  $("#notification-count").textContent = notificationItems.length ? `${unread} 条未读` : "";
  $("#notification-list").innerHTML = notificationItems.length ? notificationItems.map(notification => `
    <button class="notification-card${read.has(String(notification.id)) ? " read" : ""}" data-notification-id="${notification.id}" data-notification-booking="${escapeHtml(notification.bookingNo || "")}">
      <h3>${escapeHtml(notification.title || notification.type || "预约状态更新")}</h3><p>${escapeHtml(notification.body || notification.message || "预约状态已发生变化，请查看申请记录。")}</p>
      <div class="notification-meta"><span>${dateTime(notification.createdAt)}</span><span>${notification.bookingNo ? `${escapeHtml(notification.bookingNo)} · 查看申请 →` : "站内服务通知"}</span></div>
    </button>
  `).join("") : empty("暂无服务消息", "申请提交后，审批结果和预约变化会显示在这里。");
}
async function loadNotifications() {
  if (!profile) return;
  $("#notification-list").innerHTML = skeletons(2);
  const page = await api.notifications(profile.id);
  notificationItems = page.items || [];
  renderPersonalSummary();
  renderNotifications();
}
function saveDraft() {
  if (!profile) return;
  const form = $("#booking-form");
  const draft = Object.fromEntries(draftFields.map(name => [name, form.elements[name]?.value || ""]));
  localStorage.setItem(identityKey("booking-draft"), JSON.stringify(draft));
}
function prefillBooking(booking) {
  const form = $("#booking-form");
  draftFields.forEach(name => {
    if (form.elements[name]) form.elements[name].value = booking[name] ?? (name === "quantity" ? 1 : "");
  });
  saveDraft();
}
function downloadCalendar(booking) {
  const blob = new Blob([buildCalendarEvent(booking)], { type: "text/calendar;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = safeCalendarFilename(`${booking.activityTitle || "校园资源预约"}-${booking.bookingNo}`);
  link.click();
  URL.revokeObjectURL(url);
}
function restoreDraft() {
  if (!profile) return;
  try {
    const draft = JSON.parse(localStorage.getItem(identityKey("booking-draft")) || "null");
    if (!draft) return;
    const form = $("#booking-form");
    draftFields.forEach(name => { if (form.elements[name] && draft[name] != null) form.elements[name].value = draft[name]; });
  } catch {
    localStorage.removeItem(identityKey("booking-draft"));
  }
}
async function refreshAll() {
  await Promise.all([loadResources(), loadBookings(), loadNotifications()]);
  notify("个人服务数据已更新");
}
function showView(view) {
  $$(".view").forEach(section => section.classList.toggle("hidden", section.id !== view));
  $$(`[data-view]`).forEach(item => item.classList.toggle("active", item.dataset.view === view));
  if (view !== "discover") $("#slot-panel").classList.remove("open");
  $("#app-view").classList.toggle("task-view", view !== "discover");
  globalThis.scrollTo({ top: 0, behavior: "smooth" });
}

gateway.addEventListener("change", () => { resetApi(); notify("网关地址已保存"); });
$$('[data-demo-login]').forEach(button => button.addEventListener("click", () => {
  const account = button.dataset.demoLogin === "admin"
    ? { username: "campus.admin", password: "Campus-Admin-2026!", label: "管理人员" }
    : { username: "campus.user", password: "Campus-User-2026!", label: "申请人" };
  const form = $("#login-form");
  form.elements.username.value = account.username;
  form.elements.password.value = account.password;
  notify(`已填入本地${account.label}演示账号，请点击“进入平台”`);
}));
$$('[data-auth-tab]').forEach(button => button.addEventListener("click", () => {
  $$('[data-auth-tab]').forEach(item => item.classList.toggle("active", item === button));
  $("#login-form").classList.toggle("hidden", button.dataset.authTab !== "login");
  $("#register-form").classList.toggle("hidden", button.dataset.authTab !== "register");
}));
$("#login-form").addEventListener("submit", async event => {
  event.preventDefault(); resetApi(); const form = event.currentTarget; const data = new FormData(form);
  const button = form.querySelector('button[type="submit"]'); button.disabled = true;
  try { await api.login(data.get("username"), data.get("password")); await enter(); notify("登录成功，个人服务状态已同步"); }
  catch (error) { fail(error); }
  finally { button.disabled = false; }
});
$("#register-form").addEventListener("submit", async event => {
  event.preventDefault(); resetApi(); const data = new FormData(event.currentTarget);
  try {
    await api.register(data.get("username"), data.get("password"));
    await api.login(data.get("username"), data.get("password"));
    await enter({
      displayName: data.get("displayName"), campusId: data.get("campusId") || null,
      identityType: data.get("identityType"), department: data.get("department") || null,
      phone: data.get("phone") || null, email: data.get("email") || null
    });
    notify("账号已创建，可以开始提交申请");
  } catch (error) { fail(error); }
});
$("#logout").addEventListener("click", async () => { try { await api.logout(); } finally { exit(); } });
$("#refresh-all").addEventListener("click", () => refreshAll().catch(fail));
$("#refresh-bookings").addEventListener("click", () => loadBookings().catch(fail));
$("#refresh-notifications").addEventListener("click", () => loadNotifications().catch(fail));
$("#search-form").addEventListener("submit", event => {
  event.preventDefault(); loadResources().catch(fail);
});
$("#clear-search").addEventListener("click", () => {
  $("#search-form").reset();
  activeCategory = "";
  $$('[data-category]').forEach(item => item.classList.toggle("active", item.dataset.category === ""));
  loadResources().catch(fail);
});
$("#favorites-filter").addEventListener("click", () => {
  favoritesOnly = !favoritesOnly;
  renderFavoriteFilter();
  renderResources();
});
$$('[data-category]').forEach(button => button.addEventListener("click", () => {
  activeCategory = button.dataset.category;
  $$('[data-category]').forEach(item => item.classList.toggle("active", item === button));
  renderResources();
}));
$("#resource-list").addEventListener("click", event => {
  const favorite = event.target.closest("[data-favorite-resource]");
  if (favorite) {
    const id = Number(favorite.dataset.favoriteResource);
    const ids = favoriteResourceIds();
    const adding = !ids.has(id);
    if (adding) ids.add(id); else ids.delete(id);
    saveFavoriteResourceIds(ids);
    renderFavoriteFilter();
    renderResources();
    notify(adding ? "已加入我的收藏" : "已取消收藏");
    return;
  }
  const button = event.target.closest("[data-resource-id]");
  if (button) loadSlots(button.dataset.resourceId, button.dataset.resourceName).catch(fail);
});
$("#slot-list").addEventListener("click", event => {
  const button = event.target.closest("[data-slot-id]"); if (!button || button.disabled) return;
  $$(".slot").forEach(item => item.classList.toggle("selected", item === button));
  selectedSlot = slotCache.get(Number(button.dataset.slotId));
  const form = $("#booking-form");
  form.elements.slotId.value = button.dataset.slotId;
  if (!form.elements.contactName.value) form.elements.contactName.value = profile.displayName || "";
  if (!form.elements.contactPhone.value) form.elements.contactPhone.value = profile.phone || "";
  const remaining = Number(button.dataset.remaining);
  if (button.dataset.remaining !== "" && Number.isFinite(remaining)) {
    form.elements.quantity.max = String(remaining);
    if (Number(form.elements.quantity.value) > remaining) form.elements.quantity.value = String(Math.max(1, remaining));
  } else form.elements.quantity.removeAttribute("max");
  $("#selected-slot-summary").innerHTML = `<strong>${escapeHtml(currentResource.name)}</strong><span>${escapeHtml(dateRange(selectedSlot?.startAt, selectedSlot?.endAt))} · ${escapeHtml(currentResource.location || "校内")}</span>`;
  $("#approval-summary").innerHTML = `<strong>提交后由 ${escapeHtml(currentResource.ownerDepartment || "资源管理部门")} 处理</strong><span>${escapeHtml(approvalLabel(currentResource))}；你可以在“我的申请”查看进度和处理意见。</span>`;
  form.classList.remove("hidden");
  form.scrollIntoView({ behavior: "smooth", block: "nearest" });
});
$("#booking-form").addEventListener("submit", async event => {
  event.preventDefault(); const form = event.currentTarget;
  if (form.dataset.submitting === "true") return;
  const button = form.querySelector('button[type="submit"]');
  form.dataset.submitting = "true"; button.disabled = true; button.firstChild.textContent = "正在提交… ";
  const data = new FormData(form);
  try {
    await api.createBooking(profile.id, Number(data.get("slotId")), Number(data.get("quantity")), {
      activityTitle: data.get("activityTitle"), purpose: data.get("purpose"),
      contactName: data.get("contactName"), contactPhone: data.get("contactPhone"), note: data.get("note") || null
    });
    form.reset(); localStorage.removeItem(identityKey("booking-draft")); form.classList.add("hidden"); selectedSlot = null;
    await Promise.all([loadBookings(), loadNotifications()]);
    showView("bookings");
    notify("申请已提交，可在此查看审批进度和管理部门意见");
  } catch (error) { fail(error); }
  finally { form.dataset.submitting = "false"; button.disabled = false; button.firstChild.textContent = "提交使用申请 "; }
});
$("#booking-form").addEventListener("input", saveDraft);
$("#booking-list").addEventListener("click", async event => {
  const calendar = event.target.closest("[data-calendar-booking]");
  if (calendar) {
    const booking = bookingHistory.find(item => item.bookingNo === calendar.dataset.calendarBooking);
    if (!booking) return;
    try { downloadCalendar(booking); notify("日历文件已导出"); }
    catch (error) { fail(error); }
    return;
  }
  const reschedule = event.target.closest("[data-reschedule-booking]");
  if (reschedule) {
    const booking = bookingHistory.find(item => item.bookingNo === reschedule.dataset.rescheduleBooking);
    if (!booking || !globalThis.confirm("改期会先撤回当前申请并释放原时段，再由你选择新的开放时段。是否继续？")) return;
    try {
      reschedule.disabled = true;
      await api.bookingAction(booking.bookingNo, "cancellation");
      prefillBooking(booking);
      await loadBookings();
      showView("discover");
      await loadSlots(booking.resource.id, booking.resource.name);
      notify("原申请已撤回，信息已保留，请选择新的开放时段");
    } catch (error) { fail(error); }
    finally { reschedule.disabled = false; }
    return;
  }
  const repeat = event.target.closest("[data-repeat-booking]");
  if (repeat) {
    const booking = bookingHistory.find(item => item.bookingNo === repeat.dataset.repeatBooking);
    if (!booking) return;
    prefillBooking(booking);
    showView("discover");
    if (booking.resource) await loadSlots(booking.resource.id, booking.resource.name);
    notify("已带入上次申请信息，请选择新的开放时段");
    return;
  }
  const button = event.target.closest("[data-booking]"); if (!button) return;
  if (!globalThis.confirm("确认撤回这份申请？撤回后如需使用须重新提交。")) return;
  try { button.disabled = true; await api.bookingAction(button.dataset.booking, button.dataset.action); await loadBookings(); notify("申请已撤回"); }
  catch (error) { fail(error); }
  finally { button.disabled = false; }
});
$("#booking-filter-form").addEventListener("input", () => renderBookings());
$("#booking-filter-form").addEventListener("reset", () => setTimeout(renderBookings));
$("#notification-list").addEventListener("click", event => {
  const card = event.target.closest("[data-notification-id]"); if (!card) return;
  markNotificationRead(card.dataset.notificationId);
  renderNotifications(); renderPersonalSummary();
  if (card.dataset.notificationBooking) {
    $("#booking-filter-form").elements.text.value = "";
    $("#booking-filter-form").elements.status.value = "";
    showView("bookings");
    renderBookings(card.dataset.notificationBooking);
  }
});
$("#mark-notifications-read").addEventListener("click", () => {
  notificationItems.forEach(item => markNotificationRead(item.id));
  renderNotifications(); renderPersonalSummary(); notify("消息已全部标记为已读");
});
$("#profile-form").addEventListener("submit", async event => {
  event.preventDefault(); const form = event.currentTarget;
  if (form.dataset.submitting === "true") return;
  const button = form.querySelector('button[type="submit"]'); form.dataset.submitting = "true"; button.disabled = true;
  const data = Object.fromEntries(new FormData(form));
  try {
    profile = await api.updateCampusProfile({
      ...data,
      campusId: profile.authoritativeSource ? profile.campusId : data.campusId || null,
      identityType: profile.authoritativeSource ? profile.identityType : data.identityType,
      department: profile.authoritativeSource ? profile.department : data.department || null,
      phone: data.phone || null, email: data.email || null, expectedVersion: profile.version
    });
    $("#profile-name").textContent = profile.displayName;
    $("#profile-initial").textContent = profile.displayName.trim().charAt(0);
    renderProfile(); renderPersonalSummary(); notify("校园资料已保存并用于后续申请");
  } catch (error) { fail(error); }
  finally { form.dataset.submitting = "false"; button.disabled = false; }
});
$("#close-slots").addEventListener("click", () => $("#slot-panel").classList.remove("open"));
$$('[data-view]').forEach(button => button.addEventListener("click", () => showView(button.dataset.view)));

$("#sso-providers").addEventListener("click", async event => {
  const button = event.target.closest("[data-sso-provider]"); if (!button) return;
  try {
    button.disabled = true;
    const start = await api.startSso(button.dataset.ssoProvider);
    location.assign(start.authorizationUrl);
  } catch (error) {
    button.disabled = false;
    fail(error);
  }
});

async function bootstrapAuthentication() {
  const url = new URL(location.href);
  const completionCode = url.searchParams.get("sso_code");
  if (completionCode) {
    await api.completeSso(completionCode);
    url.searchParams.delete("sso_code");
    history.replaceState({}, "", url);
    await enter();
    notify("校园统一身份认证成功");
    return;
  }
  if (api.hasSession()) {
    await enter();
    return;
  }
  await loadSsoProviders();
}
bootstrapAuthentication().catch(error => { api.clear(); exit(); fail(error); });
