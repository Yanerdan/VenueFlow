import { createApi } from "./api.js?v=20260728-c25";

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];
const baseUrl = localStorage.getItem("venueflow.gateway") || "http://127.0.0.1:8080";
const api = createApi({ baseUrl });
const managementRoles = ["APPROVER", "RESOURCE_MANAGER", "SYSTEM_ADMIN"];
const approvalRoles = ["APPROVER", "SYSTEM_ADMIN"];
let resources = [];
let bookings = [];
let messageTimer;

const escapeHtml = value => String(value ?? "").replace(/[&<>"']/g, char =>
  ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
const dateTime = value => value ? new Intl.DateTimeFormat("zh-CN", {
  year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit"
}).format(new Date(value)) : "—";
const statusLabel = value => ({
  APPLICANT: "申请人", APPROVER: "审批人员", RESOURCE_MANAGER: "资源管理员",
  SYSTEM_ADMIN: "系统管理员",
  PENDING_CONFIRMATION: "待审批", CONFIRMED: "已通过", COMPLETED: "已核销",
  CANCELLED: "已取消", EXPIRED: "已失效", DRAFT: "草稿", ACTIVE: "已发布",
  SUSPENDED: "已暂停", ARCHIVED: "已归档", OPEN: "开放", CLOSED: "关闭"
})[value] || value;
const empty = (title, copy) => `<div class="empty"><strong>${title}</strong>${copy}</div>`;

function notify(text, error = false) {
  clearTimeout(messageTimer); $("#message").textContent = text;
  $("#message").className = `message${error ? " error" : ""}`;
  messageTimer = setTimeout(() => $("#message").classList.add("hidden"), 4500);
}
function fail(error) {
  notify(`${error.message}${error.traceId ? ` · 追踪号 ${error.traceId}` : ""}`, true);
}
function guard() {
  const role = api.role();
  $("#admin-role").textContent = statusLabel(role);
  if (!api.hasSession() || !managementRoles.includes(role)) {
    $("#admin-content").classList.add("hidden"); $("#admin-guard").classList.remove("hidden");
    $("#guard-copy").textContent = api.hasSession() ? `当前身份为 ${statusLabel(role)}，没有管理工作台权限。` : "请先登录管理人员账号。";
    return false;
  }
  return true;
}
async function loadData() {
  const tasks = [api.resources()];
  if (approvalRoles.includes(api.role())) tasks.push(api.managementBookings());
  const [resourcePage, bookingPage] = await Promise.all(tasks);
  resources = resourcePage.items || [];
  bookings = bookingPage?.items || [];
  renderDashboard(); renderApprovals(); renderResources(); renderResourceOptions(); renderResourcePicker();
}
function renderDashboard() {
  const pending = bookings.filter(item => item.status === "PENDING_CONFIRMATION");
  $("#metric-pending").textContent = pending.length;
  $("#pending-badge").textContent = pending.length;
  $("#metric-resources").textContent = resources.length;
  $("#metric-confirmed").textContent = bookings.filter(item => item.status === "CONFIRMED").length;
  $("#metric-completed").textContent = bookings.filter(item => item.status === "COMPLETED").length;
  $("#dashboard-pending").innerHTML = pending.length ? pending.slice(0, 5).map(item => `
    <div class="compact-row"><div><strong>${escapeHtml(item.bookingNo)}</strong><span>用户 #${item.userId} · 时段 #${item.slotId} · ${item.quantity} 人</span></div>
      <button data-approval="${escapeHtml(item.bookingNo)}" data-action="confirmation">通过</button></div>`).join("") :
    empty("待办已清空", approvalRoles.includes(api.role()) ? "当前没有需要处理的申请。" : "该角色不承担预约审批。");
  const active = resources.filter(item => item.status === "ACTIVE").length;
  const draft = resources.filter(item => item.status === "DRAFT").length;
  $("#resource-health").innerHTML = `
    <div><span>已发布</span><strong>${active}</strong></div><div><span>草稿</span><strong>${draft}</strong></div>
    <div><span>暂停 / 归档</span><strong>${resources.length - active - draft}</strong></div>`;
}
function approvalActions(item) {
  if (!approvalRoles.includes(api.role())) return "—";
  if (item.status === "PENDING_CONFIRMATION") return `<button data-approval="${item.bookingNo}" data-action="confirmation">通过</button><button class="danger" data-approval="${item.bookingNo}" data-action="cancellation">驳回</button>`;
  if (item.status === "CONFIRMED") return `<button data-approval="${item.bookingNo}" data-action="check-in">签到核销</button><button class="danger" data-approval="${item.bookingNo}" data-action="cancellation">取消</button>`;
  return "—";
}
function renderApprovals() {
  $("#approval-table").innerHTML = bookings.length ? bookings.map(item => `
    <tr><td><strong>${escapeHtml(item.bookingNo)}</strong></td><td>用户 #${item.userId}</td><td>#${item.slotId}</td><td>${item.quantity} 人</td>
      <td><span class="status ${item.status.toLowerCase()}">${statusLabel(item.status)}</span></td><td>${dateTime(item.createdAt)}</td><td class="table-actions">${approvalActions(item)}</td></tr>
  `).join("") : `<tr><td colspan="7">${empty("暂无申请记录", "新的师生申请会出现在这里。")}</td></tr>`;
}
function nextResourceStatus(item) {
  if (item.status === "DRAFT") return ["ACTIVE", "发布"];
  if (item.status === "ACTIVE") return ["SUSPENDED", "暂停"];
  if (item.status === "SUSPENDED") return ["ACTIVE", "恢复"];
  return null;
}
function renderResources() {
  $("#resource-admin-list").innerHTML = resources.length ? resources.map(item => {
    const next = nextResourceStatus(item);
    return `<article class="resource-admin-card"><div><span class="status ${item.status.toLowerCase()}">${statusLabel(item.status)}</span><small>${escapeHtml(item.resourceNo)}</small></div>
      <h3>${escapeHtml(item.name)}</h3><p>${escapeHtml(item.location || "位置待完善")} · 容量 ${item.capacity} 人</p>
      <footer><span>分类 #${item.categoryId}</span>${next ? `<button data-resource-status="${item.id}" data-target="${next[0]}" data-version="${item.version}">${next[1]}</button>` : ""}</footer></article>`;
  }).join("") : empty("暂无校园资源", "点击“新增资源”建立统一资源目录。");
}
function renderResourceOptions() {
  const options = resources.map(item => `<option value="${item.id}">${escapeHtml(item.name)}（${statusLabel(item.status)}）</option>`).join("");
  $("#slot-resource-select").innerHTML = `<option value="">请选择资源</option>${options}`;
}
function renderResourcePicker() {
  $("#slot-resource-list").innerHTML = resources.length ? resources.map(item =>
    `<button data-slot-resource="${item.id}" data-name="${escapeHtml(item.name)}"><strong>${escapeHtml(item.name)}</strong><span>${escapeHtml(item.location || item.resourceNo)}</span></button>`
  ).join("") : empty("暂无资源", "请先创建校园资源。");
}
async function selectSlots(id, name) {
  $("#slot-admin-list").innerHTML = '<div class="skeleton"></div>';
  const page = await api.slots(id); const items = page.items || [];
  $("#slot-admin-list").innerHTML = `<div class="panel-title"><div><p class="eyebrow">OPEN WINDOWS</p><h2>${escapeHtml(name)}</h2></div></div>` +
    (items.length ? `<div class="slot-manage-list">${items.map(item => `<div><div><strong>${dateTime(item.startAt)}</strong><span>至 ${dateTime(item.endAt)}</span></div>
      <span class="status ${item.status.toLowerCase()}">${statusLabel(item.status)}</span><button data-slot-status="${item.id}" data-target="${item.status === "OPEN" ? "CLOSED" : "OPEN"}" data-version="${item.version}">${item.status === "OPEN" ? "关闭" : "开放"}</button></div>`).join("")}</div>` :
      empty("暂无开放时段", "使用上方按钮为该资源新增开放时段。"));
}
async function actBooking(button) {
  await api.bookingAction(button.dataset.approval, button.dataset.action);
  const page = await api.managementBookings($("#approval-filter").value);
  bookings = page.items || []; renderDashboard(); renderApprovals();
  notify(button.dataset.action === "confirmation" ? "申请已审批通过" : button.dataset.action === "check-in" ? "已完成签到核销" : "申请已取消");
}

$$("[data-section]").forEach(button => button.addEventListener("click", () => {
  const section = button.dataset.section;
  $$(".admin-section").forEach(item => item.classList.toggle("hidden", item.id !== section));
  $$("[data-section]").forEach(item => item.classList.toggle("active", item === button));
  $("#section-title").textContent = ({ dashboard: "运营总览", approvals: "申请审批", resources: "资源管理", slots: "开放时段" })[section];
}));
$$("[data-jump]").forEach(button => button.addEventListener("click", () => $(`[data-section="${button.dataset.jump}"]`).click()));
$("#admin-refresh").addEventListener("click", () => loadData().then(() => notify("管理数据已更新")).catch(fail));
$("#admin-logout").addEventListener("click", async () => { await api.logout(); location.href = "./index.html"; });
$("#approval-filter").addEventListener("change", async event => {
  try { const page = await api.managementBookings(event.target.value); bookings = page.items || []; renderApprovals(); } catch (error) { fail(error); }
});
document.addEventListener("click", event => {
  const approval = event.target.closest("[data-approval]");
  if (approval) actBooking(approval).catch(fail);
  const close = event.target.closest("[data-close-form]");
  if (close) close.closest("form").classList.add("hidden");
});
$("#show-resource-form").addEventListener("click", async () => {
  try {
    const categories = await api.categories();
    $("#category-select").innerHTML = (categories || []).map(item => `<option value="${item.id}">${escapeHtml(item.name)}</option>`).join("");
    $("#resource-form").classList.remove("hidden");
  } catch (error) { fail(error); }
});
$("#show-slot-form").addEventListener("click", () => $("#slot-form").classList.remove("hidden"));
$("#resource-form").addEventListener("submit", async event => {
  event.preventDefault(); const data = Object.fromEntries(new FormData(event.currentTarget));
  data.categoryId = Number(data.categoryId); data.capacity = Number(data.capacity);
  try { await api.createResource(data); event.currentTarget.reset(); event.currentTarget.classList.add("hidden"); await loadData(); notify("资源草稿已创建"); } catch (error) { fail(error); }
});
$("#resource-admin-list").addEventListener("click", async event => {
  const button = event.target.closest("[data-resource-status]"); if (!button) return;
  try { await api.changeResourceStatus(button.dataset.resourceStatus, button.dataset.target, Number(button.dataset.version)); await loadData(); notify("资源状态已更新"); } catch (error) { fail(error); }
});
$("#slot-form").addEventListener("submit", async event => {
  event.preventDefault(); const data = new FormData(event.currentTarget);
  try {
    await api.createSlot(Number(data.get("resourceId")), new Date(data.get("startAt")).toISOString(), new Date(data.get("endAt")).toISOString());
    event.currentTarget.reset(); event.currentTarget.classList.add("hidden"); notify("开放时段已发布");
  } catch (error) { fail(error); }
});
$("#slot-resource-list").addEventListener("click", event => {
  const button = event.target.closest("[data-slot-resource]"); if (button) selectSlots(button.dataset.slotResource, button.dataset.name).catch(fail);
});
$("#slot-admin-list").addEventListener("click", async event => {
  const button = event.target.closest("[data-slot-status]"); if (!button) return;
  try { await api.changeSlotStatus(button.dataset.slotStatus, button.dataset.target, Number(button.dataset.version)); notify("时段状态已更新"); } catch (error) { fail(error); }
});

if (guard()) loadData().catch(fail);
