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
    createProfile: (externalUserId, displayName) => request("/api/v1/users", {
      method: "POST", body: JSON.stringify({ externalUserId, displayName })
    }),
    resources: () => request("/api/v1/resources?page=0&size=50"),
    search: text => request(`/api/v1/search/resources?text=${encodeURIComponent(text)}&page=0&size=50`),
    slots: resourceId => {
      const from = new Date().toISOString();
      const to = new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString();
      return request(
        `/api/v1/resources/${resourceId}/slots?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&page=0&size=50`
      );
    },
    createBooking: (userId, slotId, quantity) => request("/api/v1/bookings", {
      method: "POST",
      headers: { "Idempotency-Key": uuid() },
      body: JSON.stringify({ userId, slotId, quantity })
    }),
    bookings: userId => request(`/api/v1/bookings?userId=${userId}&pageNumber=0&pageSize=50`),
    bookingAction: (bookingNo, action) => request(
      `/api/v1/bookings/${encodeURIComponent(bookingNo)}/${action}`, { method: "POST" }
    ),
    notifications: userId => request(
      `/api/v1/notifications?userId=${userId}&pageNumber=0&pageSize=50`
    )
  };
}

export function jwtSubject(token) {
  if (!token) return null;
  try {
    const part = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(globalThis.atob(part)).sub || null;
  } catch {
    return null;
  }
}
