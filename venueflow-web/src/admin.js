import { createApi } from "./api.js?v=20260728-c33";

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];
const baseUrl = localStorage.getItem("venueflow.gateway") || "http://127.0.0.1:8080";
const api = createApi({ baseUrl });
const managementRoles = ["APPROVER", "RESOURCE_MANAGER", "SYSTEM_ADMIN"];
const approvalRoles = ["APPROVER", "SYSTEM_ADMIN"];
const sectionsByRole = {
  APPROVER: ["dashboard", "reports", "approvals", "users"],
  RESOURCE_MANAGER: ["dashboard", "resources", "slots", "users"],
  SYSTEM_ADMIN: ["dashboard", "reports", "approvals", "resources", "slots", "users"]
};
let resources = [];
let bookings = [];
let users = [];
let accounts = [];
let approvers = [];
let report;
let selectedBooking;
let messageTimer;

const escapeHtml = value => String(value ?? "").replace(/[&<>"']/g, char =>
  ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
const dateTime = value => value ? new Intl.DateTimeFormat("zh-CN", {
  year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit"
}).format(new Date(value)) : "—";
const statusLabel = value => ({
  APPLICANT: "申请人", APPROVER: "审批人员", RESOURCE_MANAGER: "资源管理员",
  SYSTEM_ADMIN: "系统管理员",
  STUDENT: "学生", STAFF: "教职工", OTHER: "其他",
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
  const allowed = sectionsByRole[role];
  $$("[data-section]").forEach(button =>
    button.classList.toggle("hidden", !allowed.includes(button.dataset.section)));
  $$("[data-jump]").forEach(button =>
    button.classList.toggle("hidden", !allowed.includes(button.dataset.jump)));
  return true;
}
async function loadData() {
  const role = api.role();
  const [resourcePage, bookingPage, userPage, reportData, accountData, approverData] =
    await Promise.all([
      api.resources(),
      approvalRoles.includes(role) ? api.managementBookings() : Promise.resolve(null),
      api.managementUsers(),
      approvalRoles.includes(role) ? api.operationalReport() : Promise.resolve(null),
      role === "SYSTEM_ADMIN" ? api.authAccounts() : Promise.resolve([]),
      ["RESOURCE_MANAGER", "SYSTEM_ADMIN"].includes(role)
        ? api.approverAccounts()
        : Promise.resolve([])
    ]);
  resources = (resourcePage.items || []).filter(item => item.status !== "ARCHIVED");
  bookings = bookingPage?.items || [];
  users = userPage?.items || [];
  report = reportData || null;
  accounts = accountData || [];
  approvers = approverData || [];
  renderDashboard(); renderReports(); renderApprovals(); renderResources(); renderResourceOptions(); renderResourcePicker(); renderUsers();
}
const userFor = userId => users.find(user => Number(user.id) === Number(userId));
const profileForExternalId = externalId =>
  users.find(user => user.externalUserId === externalId);
const accountFor = externalId =>
  accounts.find(account => account.userId === externalId);
const applicantLabel = userId => {
  const user = userFor(userId);
  return user
    ? `${escapeHtml(user.displayName)} · ${escapeHtml(user.department || "院系待完善")}`
    : `用户 #${userId}`;
};
function renderDashboard() {
  const pending = bookings.filter(item => item.status === "PENDING_CONFIRMATION");
  $("#metric-pending").textContent = pending.length;
  $("#pending-badge").textContent = pending.length;
  $("#metric-resources").textContent = resources.length;
  $("#metric-confirmed").textContent = bookings.filter(item => item.status === "CONFIRMED").length;
  $("#metric-completed").textContent = bookings.filter(item => item.status === "COMPLETED").length;
  $("#dashboard-pending").innerHTML = pending.length ? pending.slice(0, 5).map(item => `
    <div class="compact-row"><div><strong>${escapeHtml(item.bookingNo)}</strong><span>${applicantLabel(item.userId)} · 时段 #${item.slotId} · ${item.quantity} 人</span></div>
      <button data-approval="${escapeHtml(item.bookingNo)}" data-action="confirmation">通过</button></div>`).join("") :
    empty("待办已清空", approvalRoles.includes(api.role()) ? "当前没有需要处理的申请。" : "该角色不承担预约审批。");
  const active = resources.filter(item => item.status === "ACTIVE").length;
  const draft = resources.filter(item => item.status === "DRAFT").length;
  $("#resource-health").innerHTML = `
    <div><span>已发布</span><strong>${active}</strong></div><div><span>草稿</span><strong>${draft}</strong></div>
    <div><span>暂停 / 归档</span><strong>${resources.length - active - draft}</strong></div>`;
}
function renderReports() {
  if (!report) {
    $("#reports").classList.add("hidden");
    $('[data-section="reports"]').classList.add("hidden");
    return;
  }
  $('[data-section="reports"]').classList.remove("hidden");
  const summary = report.summary;
  $("#report-total").textContent = summary.totalBookings;
  $("#report-pending").textContent = summary.pendingBookings;
  $("#report-rate").textContent = `${summary.approvalRate}%`;
  $("#report-attendees").textContent = summary.totalAttendees;
  const resourceName = id => resources.find(item => Number(item.id) === Number(id))?.name || `资源 #${id}`;
  const rankedResources = report.resources.filter(item =>
    resources.some(resource => Number(resource.id) === Number(item.resourceId)));
  const recentReviews = report.recentReviews.some(item => String(item.bookingNo).startsWith("VF-SHOW-"))
    ? report.recentReviews.filter(item => String(item.bookingNo).startsWith("VF-SHOW-"))
    : report.recentReviews;
  $("#report-resources").innerHTML = rankedResources.length ? rankedResources.map(item => `
    <div><span>${escapeHtml(resourceName(item.resourceId))}</span><strong>${item.bookingCount} 单 · ${item.attendeeCount} 人</strong></div>
  `).join("") : empty("暂无资源统计", "有申请提交后将在这里形成排行。");
  $("#report-departments").innerHTML = report.departments.length ? report.departments.map(item => `
    <div><span>${escapeHtml(item.department)}</span><strong>${item.bookingCount} 单 · ${item.attendeeCount} 人</strong></div>
  `).join("") : empty("暂无部门统计", "资源归属快照会形成部门工作量分布。");
  $("#report-audit").innerHTML = recentReviews.length ? recentReviews.map(item => `
    <tr><td><strong>${escapeHtml(item.bookingNo)}</strong></td><td>${item.decision === "APPROVED" ? "通过" : "驳回"}</td>
      <td>${item.reviewerRole ? statusLabel(item.reviewerRole) : "历史记录"}</td><td>${escapeHtml(item.reviewNote || "未填写")}</td><td>${dateTime(item.reviewedAt)}</td></tr>
  `).join("") : `<tr><td colspan="5">${empty("暂无审批记录", "审批通过或驳回后将在这里留下处理记录。")}</td></tr>`;
}

function approvalActions(item) {
  if (!approvalRoles.includes(api.role())) return "—";
  if (item.status === "PENDING_CONFIRMATION") return `<button data-review-booking="${item.bookingNo}">查看并审批</button>`;
  if (item.status === "CONFIRMED") return `<button data-approval="${item.bookingNo}" data-action="check-in">签到核销</button><button class="danger" data-approval="${item.bookingNo}" data-action="cancellation">取消</button>`;
  return "—";
}
async function openReview(item) {
  selectedBooking = item;
  const applicant = userFor(item.userId);
  const actions = await api.approvalActions(item.bookingNo);
  const trajectory = actions.length
    ? actions.map(action => `<li>第 ${action.approvalStep} 级 · ${action.decision === "APPROVED" ? "通过" : "驳回"} · ${statusLabel(action.actorRole)}${action.reviewNote ? ` · ${escapeHtml(action.reviewNote)}` : ""} · ${dateTime(action.createdAt)}</li>`).join("")
    : "<li>尚无审批动作</li>";
  $("#review-title").textContent = item.activityTitle || "历史场地申请";
  $("#review-detail").innerHTML = `
    <div><span>申请人</span><strong>${applicantLabel(item.userId)}</strong></div>
    <div><span>申请编号</span><strong>${escapeHtml(item.bookingNo)}</strong></div>
    <div><span>活动用途</span><p>${escapeHtml(item.purpose || "历史申请未记录")}</p></div>
    <div><span>联系人</span><p>${escapeHtml(item.contactName || applicant?.displayName || "待完善")} · ${escapeHtml(item.contactPhone || applicant?.phone || "待完善")}</p></div>
    <div><span>资源归属</span><p>${escapeHtml(item.ownerDepartment || "未分配部门")} · ${item.assignedApproverExternalUserId ? `审批人 #${item.assignedApproverExternalUserId}` : "未指定审批人"}</p></div>
    <div><span>审批进度</span><p>第 ${item.currentApprovalStep || 1} / ${item.totalApprovalSteps || 1} 级 · ${item.approvalMode === "TWO_STAGE" ? "两级审批" : "直接审批"}</p><ul>${trajectory}</ul></div>
    ${item.note ? `<div><span>补充说明</span><p>${escapeHtml(item.note)}</p></div>` : ""}
    ${item.reviewNote ? `<div><span>已有处理意见</span><p>${escapeHtml(item.reviewNote)}</p></div>` : ""}`;
  $("#review-note").value = item.reviewNote || "";
  $("#approve-review").classList.toggle("hidden", item.status !== "PENDING_CONFIRMATION");
  $("#reject-review").classList.toggle("hidden", item.status !== "PENDING_CONFIRMATION");
  $("#review-panel").classList.remove("hidden");
}
function renderApprovals() {
  $("#approval-table").innerHTML = bookings.length ? bookings.map(item => `
    <tr><td><strong>${escapeHtml(item.bookingNo)}</strong></td><td>${applicantLabel(item.userId)}</td><td>#${item.slotId}</td><td>${item.quantity} 人</td>
      <td><span class="status ${item.status.toLowerCase()}">${statusLabel(item.status)}</span></td><td>${dateTime(item.createdAt)}</td><td class="table-actions">${approvalActions(item)}</td></tr>
  `).join("") : `<tr><td colspan="7">${empty("暂无申请记录", "新的师生申请会出现在这里。")}</td></tr>`;
}
function renderUsers() {
  $("#user-table").innerHTML = users.length ? users.map(user => `
    <tr><td><strong>${escapeHtml(user.displayName)}</strong></td><td>${escapeHtml(user.campusId || "待完善")}</td>
      <td>${statusLabel(user.identityType)}</td><td>${roleEditor(user)}</td><td>${escapeHtml(user.department || "待完善")}</td>
      <td>${escapeHtml(user.phone || "待完善")}</td><td>${escapeHtml(user.email || "待完善")}</td>
      <td><span class="status ${user.accountStatus.toLowerCase()}">${user.accountStatus === "ACTIVE" ? "正常" : "停用"}</span></td></tr>
  `).join("") : `<tr><td colspan="8">${empty("暂无人员资料", "没有符合条件的平台用户。")}</td></tr>`;
}
function roleEditor(user) {
  const account = accountFor(user.externalUserId);
  if (!account) return "—";
  if (api.role() !== "SYSTEM_ADMIN") return statusLabel(account.role);
  const options = ["APPLICANT", "APPROVER", "RESOURCE_MANAGER", "SYSTEM_ADMIN"]
    .map(role => `<option value="${role}" ${role === account.role ? "selected" : ""}>${statusLabel(role)}</option>`)
    .join("");
  return `<form class="role-form" data-role-user="${account.userId}" data-version="${account.version}">
    <select name="role" aria-label="${escapeHtml(user.displayName)}的平台角色">${options}</select>
    <button type="submit">保存</button>
  </form>`;
}
function approverOptions(selectedId) {
  const options = approvers.map(account => {
    const profile = profileForExternalId(account.userId);
    const name = profile?.displayName || account.username;
    const department = profile?.department || "部门待完善";
    return `<option value="${account.userId}" ${account.userId === selectedId ? "selected" : ""}>${escapeHtml(name)} · ${escapeHtml(department)} · ${escapeHtml(account.username)}</option>`;
  }).join("");
  const legacy = selectedId && !approvers.some(account => account.userId === selectedId)
    ? `<option value="${escapeHtml(selectedId)}" selected>历史审批人 · ${escapeHtml(selectedId)}</option>`
    : "";
  return `<option value="">未指定审批人</option>${legacy}${options}`;
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
      <div class="policy-summary"><strong>预约规则</strong><span>提前 ${item.minAdvanceHours ?? 0} 小时至 ${item.maxAdvanceDays ?? 90} 天 · 最长 ${item.maxDurationMinutes ?? 480} 分钟</span>${item.bookingNotice ? `<p>${escapeHtml(item.bookingNotice)}</p>` : ""}</div>
      <form class="ownership-form" data-resource-ownership="${item.id}" data-version="${item.version}">
        <input name="ownerDepartment" maxlength="160" placeholder="归属部门" value="${escapeHtml(item.ownerDepartment || "")}">
        <select name="approvalMode" aria-label="${escapeHtml(item.name)}的审批模式"><option value="DIRECT" ${item.approvalMode !== "TWO_STAGE" ? "selected" : ""}>直接审批</option><option value="TWO_STAGE" ${item.approvalMode === "TWO_STAGE" ? "selected" : ""}>两级审批</option></select>
        <select name="approverExternalUserId" aria-label="${escapeHtml(item.name)}的初审人">${approverOptions(item.approverExternalUserId)}</select>
        <select name="finalApproverExternalUserId" aria-label="${escapeHtml(item.name)}的终审人">${approverOptions(item.finalApproverExternalUserId)}</select>
        <button type="submit">保存归属</button>
      </form>
      <form class="booking-rules-form" data-resource-rules="${item.id}" data-version="${item.version}">
        <textarea name="bookingNotice" maxlength="1000" rows="2" placeholder="申请须知，例如携带校园卡">${escapeHtml(item.bookingNotice || "")}</textarea>
        <label>最少提前（小时）<input name="minAdvanceHours" type="number" min="0" max="720" required value="${item.minAdvanceHours ?? 0}"></label>
        <label>最多提前（天）<input name="maxAdvanceDays" type="number" min="1" max="365" required value="${item.maxAdvanceDays ?? 90}"></label>
        <label>最长使用（分钟）<input name="maxDurationMinutes" type="number" min="15" max="1440" required value="${item.maxDurationMinutes ?? 480}"></label>
        <button type="submit">保存预约规则</button>
      </form>
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
  await api.bookingAction(button.dataset.approval, button.dataset.action, button.dataset.note);
  await loadData();
  notify(button.dataset.action === "confirmation" ? "申请已审批通过" : button.dataset.action === "check-in" ? "已完成签到核销" : "申请已取消");
}
async function reviewAction(action) {
  if (!selectedBooking) return;
  const note = $("#review-note").value.trim();
  if (action === "rejection" && !note) {
    notify("驳回申请必须填写原因", true);
    return;
  }
  await api.bookingAction(selectedBooking.bookingNo, action, note);
  $("#review-panel").classList.add("hidden");
  await loadData();
  notify(action === "confirmation" ? "申请已审批通过" : "申请已驳回");
}

$$("[data-section]").forEach(button => button.addEventListener("click", () => {
  const section = button.dataset.section;
  $$(".admin-section").forEach(item => item.classList.toggle("hidden", item.id !== section));
  $$("[data-section]").forEach(item => item.classList.toggle("active", item === button));
  $("#section-title").textContent = ({ dashboard: "运营总览", reports: "运营报表", approvals: "申请审批", resources: "资源管理", slots: "开放时段", users: "人员目录" })[section];
}));
$$("[data-jump]").forEach(button => button.addEventListener("click", () => $(`[data-section="${button.dataset.jump}"]`).click()));
$("#admin-refresh").addEventListener("click", () => loadData().then(() => notify("管理数据已更新")).catch(fail));
$("#admin-logout").addEventListener("click", async () => { await api.logout(); location.href = "./index.html"; });
$("#approval-filter").addEventListener("change", async event => {
  try { const page = await api.managementBookings(event.target.value); bookings = page.items || []; renderApprovals(); } catch (error) { fail(error); }
});
$("#user-search-form").addEventListener("submit", async event => {
  event.preventDefault();
  try {
    const page = await api.managementUsers(new FormData(event.currentTarget).get("keyword").trim());
    users = page.items || [];
    renderUsers();
  } catch (error) { fail(error); }
});
$("#user-table").addEventListener("submit", async event => {
  const form = event.target.closest("[data-role-user]"); if (!form) return;
  event.preventDefault();
  try {
    await api.changeAccountRole(
      form.dataset.roleUser,
      new FormData(form).get("role"),
      Number(form.dataset.version)
    );
    await loadData();
    notify("平台角色已更新，目标用户重新登录后生效");
  } catch (error) { fail(error); }
});
document.addEventListener("click", event => {
  const approval = event.target.closest("[data-approval]");
  if (approval) actBooking(approval).catch(fail);
  const review = event.target.closest("[data-review-booking]");
  if (review) {
    const item = bookings.find(value => value.bookingNo === review.dataset.reviewBooking);
    if (item) openReview(item).catch(fail);
  }
  const close = event.target.closest("[data-close-form]");
  if (close) close.closest("form").classList.add("hidden");
});
$("#close-review").addEventListener("click", () => $("#review-panel").classList.add("hidden"));
$("#approve-review").addEventListener("click", () => reviewAction("confirmation").catch(fail));
$("#reject-review").addEventListener("click", () => reviewAction("rejection").catch(fail));
$("#show-resource-form").addEventListener("click", async () => {
  try {
    const categories = await api.categories();
    $("#category-select").innerHTML = (categories || []).map(item => `<option value="${item.id}">${escapeHtml(item.name)}</option>`).join("");
    $("#resource-form").classList.remove("hidden");
  } catch (error) { fail(error); }
});
$("#show-slot-form").addEventListener("click", () => $("#slot-form").classList.remove("hidden"));
$("#resource-form").addEventListener("submit", async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const data = Object.fromEntries(new FormData(form));
  data.categoryId = Number(data.categoryId); data.capacity = Number(data.capacity);
  try {
    await api.createResource(data);
    form.reset();
    form.classList.add("hidden");
    await loadData();
    notify("资源草稿已创建");
  } catch (error) { fail(error); }
});
$("#resource-admin-list").addEventListener("click", async event => {
  const button = event.target.closest("[data-resource-status]"); if (!button) return;
  try { await api.changeResourceStatus(button.dataset.resourceStatus, button.dataset.target, Number(button.dataset.version)); await loadData(); notify("资源状态已更新"); } catch (error) { fail(error); }
});
$("#resource-admin-list").addEventListener("submit", async event => {
  const form = event.target.closest("[data-resource-ownership]"); if (!form) return;
  event.preventDefault();
  const data = new FormData(form);
  const approver = data.get("approverExternalUserId");
  const finalApprover = data.get("finalApproverExternalUserId");
  try {
    await api.changeResourceOwnership(
      Number(form.dataset.resourceOwnership),
      data.get("ownerDepartment").trim(),
      approver || null,
      data.get("approvalMode"),
      finalApprover || null,
      Number(form.dataset.version)
    );
    await loadData(); notify("资源归属已更新");
  } catch (error) { fail(error); }
});
$("#resource-admin-list").addEventListener("submit", async event => {
  const form = event.target.closest("[data-resource-rules]"); if (!form) return;
  event.preventDefault();
  if (form.dataset.submitting === "true") return;
  const data = new FormData(form);
  const button = form.querySelector('button[type="submit"]');
  form.dataset.submitting = "true"; button.disabled = true;
  try {
    await api.changeResourceBookingRules(
      Number(form.dataset.resourceRules),
      data.get("bookingNotice").trim(),
      Number(data.get("minAdvanceHours")),
      Number(data.get("maxAdvanceDays")),
      Number(data.get("maxDurationMinutes")),
      Number(form.dataset.version)
    );
    await loadData(); notify("预约规则已更新");
  } catch (error) { fail(error); }
  finally { form.dataset.submitting = "false"; button.disabled = false; }
});
$("#slot-form").addEventListener("submit", async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const data = new FormData(form);
  try {
    await api.createSlot(Number(data.get("resourceId")), new Date(data.get("startAt")).toISOString(), new Date(data.get("endAt")).toISOString());
    form.reset();
    form.classList.add("hidden");
    notify("开放时段已发布");
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
