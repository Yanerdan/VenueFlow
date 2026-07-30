const ACCESS = "venueflow.access";
const REFRESH = "venueflow.refresh";

export function createApi({
  baseUrl,
  storage = globalThis.sessionStorage,
  fetchImpl = globalThis.fetch,
  uuid = () => globalThis.crypto.randomUUID()
}) {
  const origin = baseUrl.replace(/\/+$/, "");

  async function request(path, options = {}) {
    const { auth = true, retry = true, headers = {}, ...init } = options;
    const token = storage.getItem(ACCESS);
    const response = await fetchImpl(origin + path, {
      ...init,
      headers: {
        ...(init.body ? { "Content-Type": "application/json" } : {}),
        ...(auth && token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers
      }
    });
    if (response.status === 401 && auth && retry && storage.getItem(REFRESH)) {
      if (await refresh()) return request(path, { ...options, retry: false });
    }
    const payload = response.status === 204 ? null : await response.json().catch(() => null);
    if (!response.ok) {
      const error = new Error(payload?.message || `请求失败 (${response.status})`);
      error.status = response.status;
      error.code = payload?.code;
      error.traceId = payload?.traceId;
      throw error;
    }
    return payload?.data ?? payload;
  }

  function saveTokens(tokens) {
    storage.setItem(ACCESS, tokens.accessToken);
    storage.setItem(REFRESH, tokens.refreshToken);
  }

  async function refresh() {
    try {
      const tokens = await request("/api/v1/auth/refresh", {
        method: "POST",
        auth: false,
        retry: false,
        body: JSON.stringify({ refreshToken: storage.getItem(REFRESH) })
      });
      saveTokens(tokens);
      return true;
    } catch {
      clear();
      return false;
    }
  }

  function clear() {
    storage.removeItem(ACCESS);
    storage.removeItem(REFRESH);
  }

  return {
    hasSession: () => Boolean(storage.getItem(ACCESS)),
    subject: () => jwtSubject(storage.getItem(ACCESS)),
    role: () => jwtClaims(storage.getItem(ACCESS))?.role || "APPLICANT",
    register: (username, password) => request("/api/v1/auth/register", {
      method: "POST", auth: false, body: JSON.stringify({ username, password })
    }),
    async login(username, password) {
      const tokens = await request("/api/v1/auth/login", {
        method: "POST", auth: false, body: JSON.stringify({ username, password })
      });
      saveTokens(tokens);
      return tokens;
    },
    ssoProviders: () => request("/api/v1/auth/sso/providers", { auth: false }),
    startSso: providerKey => request(
      `/api/v1/auth/sso/${encodeURIComponent(providerKey)}/authorize`,
      { method: "POST", auth: false }
    ),
    async completeSso(completionCode) {
      const tokens = await request("/api/v1/auth/sso/complete", {
        method: "POST", auth: false, body: JSON.stringify({ completionCode })
      });
      saveTokens(tokens);
      return tokens;
    },
    async logout() {
      const refreshToken = storage.getItem(REFRESH);
      try {
        if (refreshToken) await request("/api/v1/auth/logout", {
          method: "POST", auth: false, body: JSON.stringify({ refreshToken })
        });
      } finally {
        clear();
      }
    },
    clear,
    currentProfile: () => request("/api/v1/users/me"),
    createProfile: (externalUserId, displayName, campus = {}) => request("/api/v1/users", {
      method: "POST", body: JSON.stringify({ externalUserId, displayName, ...campus })
    }),
    updateCampusProfile: payload => request("/api/v1/users/me/campus-profile", {
      method: "PATCH", body: JSON.stringify(payload)
    }),
    managementUsers: (keyword = "") => request(
      `/api/v1/users/management?pageNumber=0&pageSize=100${keyword ? `&keyword=${encodeURIComponent(keyword)}` : ""}`
    ),
    organizations: (source = "campus") => request(
      `/api/v1/organizations?source=${encodeURIComponent(source)}`
    ),
    directorySyncRuns: (source = "campus") => request(
      `/api/v1/organizations/sync-runs?source=${encodeURIComponent(source)}`
    ),
    synchronizeDirectory: payload => request("/api/v1/organizations/sync", {
      method: "POST", body: JSON.stringify(payload)
    }),
    authAccounts: () => request("/api/v1/auth/management/accounts"),
    approverAccounts: () => request("/api/v1/auth/management/accounts/approvers"),
    changeAccountRole: (userId, role, expectedVersion) => request(
      `/api/v1/auth/management/accounts/${encodeURIComponent(userId)}/role`, {
        method: "PATCH", body: JSON.stringify({ role, expectedVersion })
      }
    ),
    resources: () => request("/api/v1/resources?page=0&size=50"),
    resource: resourceId => request(`/api/v1/resources/${resourceId}`),
    categories: () => request("/api/v1/resource-categories"),
    createCategory: payload => request("/api/v1/resource-categories", {
      method: "POST", body: JSON.stringify(payload)
    }),
    createResource: payload => request("/api/v1/resources", {
      method: "POST", body: JSON.stringify(payload)
    }),
    changeResourceStatus: (resourceId, targetStatus, expectedVersion) => request(
      `/api/v1/resources/${resourceId}/status`, {
        method: "PATCH", body: JSON.stringify({ targetStatus, expectedVersion })
      }
    ),
    changeResourceOwnership: (
      resourceId, ownerDepartment, approverExternalUserId, approvalMode,
      finalApproverExternalUserId, expectedVersion
    ) => {
      if (typeof approvalMode === "number") {
        expectedVersion = approvalMode;
        approvalMode = "DIRECT";
        finalApproverExternalUserId = null;
      }
      return request(`/api/v1/resources/${resourceId}/ownership`, {
        method: "PATCH",
        body: JSON.stringify({
          ownerDepartment: ownerDepartment || null,
          approverExternalUserId: approverExternalUserId || null,
          approvalMode: approvalMode || "DIRECT",
          finalApproverExternalUserId: finalApproverExternalUserId || null,
          expectedVersion
        })
      })
    },
    replaceApprovalPolicy: (resourceId, expectedVersion, policyName, stages) => request(
      `/api/v1/resources/${resourceId}/approval-policy`, {
        method: "PATCH",
        body: JSON.stringify({ expectedVersion, policyName, stages })
      }
    ),
    changeResourceBookingRules: (
      resourceId, bookingNotice, minAdvanceHours, maxAdvanceDays,
      maxDurationMinutes, expectedVersion
    ) => request(`/api/v1/resources/${resourceId}/booking-rules`, {
      method: "PATCH",
      body: JSON.stringify({
        bookingNotice: bookingNotice || null,
        minAdvanceHours,
        maxAdvanceDays,
        maxDurationMinutes,
        expectedVersion
      })
    }),
    changeResourceFacts: (
      resourceId, categoryId, name, description, location, capacity, expectedVersion
    ) => request(`/api/v1/resources/${resourceId}/facts`, {
      method: "PATCH",
      body: JSON.stringify({
        categoryId, name, description: description || null, location, capacity, expectedVersion
      })
    }),
    approvalActions: bookingNo => request(
      `/api/v1/bookings/${encodeURIComponent(bookingNo)}/approval-actions`
    ),
    approvalStages: bookingNo => request(
      `/api/v1/bookings/${encodeURIComponent(bookingNo)}/approval-stages`
    ),
    async search(text) {
      const page = await request(
        `/api/v1/search/resources?text=${encodeURIComponent(text)}&page=0&size=50`
      );
      return {
        ...page,
        items: (page.items || []).map(resource => ({
          ...resource,
          id: resource.id ?? resource.resourceId
        }))
      };
    },
    slots: resourceId => {
      const from = new Date().toISOString();
      const to = new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString();
      return request(
        `/api/v1/resources/${resourceId}/slots?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&page=0&size=50`
      );
    },
    slot: slotId => request(`/api/v1/resource-slots/${slotId}`),
    slotCapacity: slotId => request(`/api/v1/resource-slots/${slotId}/capacity`),
    createBooking: (userId, slotId, quantity, details = {}) => request("/api/v1/bookings", {
      method: "POST",
      headers: { "Idempotency-Key": uuid() },
      body: JSON.stringify({ userId, slotId, quantity, ...details })
    }),
    bookings: userId => request(`/api/v1/bookings?userId=${userId}&pageNumber=0&pageSize=50`),
    managementBookings: status => request(
      `/api/v1/bookings/management?pageNumber=0&pageSize=100${status ? `&status=${status}` : ""}`
    ),
    operationalReport: () => request("/api/v1/bookings/management/report"),
    bookingAction: (bookingNo, action, note) => {
      const body = action === "confirmation"
        ? JSON.stringify({ reviewNote: note || "" })
        : note
          ? JSON.stringify(action === "rejection" ? { reason: note } : { reviewNote: note })
          : undefined;
      return request(`/api/v1/bookings/${encodeURIComponent(bookingNo)}/${action}`, {
        method: "POST", ...(body ? { body } : {})
      });
    },
    createSlot: (resourceId, startAt, endAt) => request(
      `/api/v1/resources/${resourceId}/slots`, {
        method: "POST", body: JSON.stringify({ startAt, endAt })
      }
    ),
    changeSlotStatus: (slotId, targetStatus, expectedVersion) => request(
      `/api/v1/resource-slots/${slotId}/status`, {
        method: "PATCH", body: JSON.stringify({ targetStatus, expectedVersion })
      }
    ),
    notifications: userId => request(
      `/api/v1/notifications?userId=${userId}&pageNumber=0&pageSize=50`
    )
  };
}

export function jwtSubject(token) {
  return jwtClaims(token)?.sub || null;
}

export function jwtClaims(token) {
  if (!token) return null;
  try {
    const part = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(globalThis.atob(part));
  } catch {
    return null;
  }
}
