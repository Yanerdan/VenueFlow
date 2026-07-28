import assert from "node:assert/strict";
import test from "node:test";
import { createApi } from "../src/api.js";

function storage(seed = {}) {
  const values = new Map(Object.entries(seed));
  return {
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: key => values.delete(key)
  };
}

function response(status, body) {
  return { status, ok: status >= 200 && status < 300, json: async () => body };
}

test("login stores tokens and authenticated requests use the gateway", async () => {
  const calls = [];
  const store = storage();
  const fetchImpl = async (url, options) => {
    calls.push({ url, options });
    return calls.length === 1
      ? response(200, { data: { accessToken: "access", refreshToken: "refresh" } })
      : response(200, { id: 7, displayName: "Ada" });
  };
  const api = createApi({ baseUrl: "http://localhost:8080/", storage: store, fetchImpl });
  await api.login("ada", "long-enough-password");
  await api.currentProfile();
  assert.equal(calls[1].url, "http://localhost:8080/api/v1/users/me");
  assert.equal(calls[1].options.headers.Authorization, "Bearer access");
});

test("a 401 refreshes once and retries with the rotated token", async () => {
  const calls = [];
  const store = storage({ "venueflow.access": "old", "venueflow.refresh": "refresh" });
  const fetchImpl = async (url, options) => {
    calls.push({ url, options });
    if (calls.length === 1) return response(401, { message: "expired" });
    if (url.endsWith("/refresh")) {
      return response(200, { data: { accessToken: "new", refreshToken: "next" } });
    }
    return response(200, { items: [] });
  };
  const api = createApi({ baseUrl: "http://gateway", storage: store, fetchImpl });
  await api.resources();
  assert.equal(calls.length, 3);
  assert.equal(calls[2].options.headers.Authorization, "Bearer new");
});

test("booking creation sends a generated idempotency key", async () => {
  let captured;
  const api = createApi({
    baseUrl: "http://gateway",
    storage: storage({ "venueflow.access": "token" }),
    uuid: () => "request-123",
    fetchImpl: async (_url, options) => {
      captured = options;
      return response(201, { data: { bookingNo: "B-1" } });
    }
  });
  await api.createBooking(1, 2, 3);
  assert.equal(captured.headers["Idempotency-Key"], "request-123");
  assert.deepEqual(JSON.parse(captured.body), { userId: 1, slotId: 2, quantity: 3 });
});

test("search results expose the resource id used by slot navigation", async () => {
  const api = createApi({
    baseUrl: "http://gateway",
    storage: storage({ "venueflow.access": "token" }),
    fetchImpl: async () =>
      response(200, { items: [{ resourceId: 7, name: "Emerald Hall" }] })
  });
  const page = await api.search("Emerald");
  assert.equal(page.items[0].id, 7);
});

test("API errors retain status, server message and trace id", async () => {
  const api = createApi({
    baseUrl: "http://gateway",
    storage: storage(),
    fetchImpl: async () => response(404, { message: "profile missing", traceId: "trace-42" })
  });
  await assert.rejects(api.currentProfile(), error => {
    assert.equal(error.status, 404);
    assert.equal(error.message, "profile missing");
    assert.equal(error.traceId, "trace-42");
    return true;
  });
});

test("campus role and management query come from the authenticated session", async () => {
  const claims = Buffer.from(JSON.stringify({ sub: "user-1", role: "SYSTEM_ADMIN" }))
    .toString("base64url");
  let requestedUrl;
  const api = createApi({
    baseUrl: "http://gateway",
    storage: storage({ "venueflow.access": `header.${claims}.signature` }),
    fetchImpl: async url => {
      requestedUrl = url;
      return response(200, { data: { items: [] } });
    }
  });
  assert.equal(api.role(), "SYSTEM_ADMIN");
  await api.managementBookings("PENDING_CONFIRMATION");
  assert.equal(
    requestedUrl,
    "http://gateway/api/v1/bookings/management?pageNumber=0&pageSize=100&status=PENDING_CONFIRMATION"
  );
});

test("campus profile and user directory use bounded management endpoints", async () => {
  const calls = [];
  const api = createApi({
    baseUrl: "http://gateway",
    storage: storage({ "venueflow.access": "token" }),
    fetchImpl: async (url, options) => {
      calls.push({ url, options });
      return response(200, { items: [] });
    }
  });
  await api.updateCampusProfile({
    displayName: "Ada", identityType: "STAFF", department: "计算机学院", expectedVersion: 1
  });
  await api.managementUsers("计算机");
  assert.equal(calls[0].url, "http://gateway/api/v1/users/me/campus-profile");
  assert.equal(calls[0].options.method, "PATCH");
  assert.equal(
    calls[1].url,
    "http://gateway/api/v1/users/management?pageNumber=0&pageSize=100&keyword=%E8%AE%A1%E7%AE%97%E6%9C%BA"
  );
});

test("booking review actions send bounded approval and rejection notes", async () => {
  const calls = [];
  const api = createApi({
    baseUrl: "http://gateway",
    storage: storage({ "venueflow.access": "token" }),
    fetchImpl: async (url, options) => {
      calls.push({ url, options });
      return response(200, { data: { status: "CONFIRMED" } });
    }
  });
  await api.bookingAction("B-1", "confirmation", "材料完整");
  await api.bookingAction("B-2", "rejection", "活动用途不符合场地规则");
  assert.deepEqual(JSON.parse(calls[0].options.body), { reviewNote: "材料完整" });
  assert.deepEqual(JSON.parse(calls[1].options.body), { reason: "活动用途不符合场地规则" });
});

test("resource ownership update sends department, approver and version", async () => {
  let captured;
  const api = createApi({
    baseUrl: "http://gateway",
    storage: storage({ "venueflow.access": "token" }),
    fetchImpl: async (url, options) => {
      captured = { url, options };
      return response(200, { id: 7 });
    }
  });
  await api.changeResourceOwnership(7, "学生工作处", "approver-12", 3);
  assert.equal(captured.url, "http://gateway/api/v1/resources/7/ownership");
  assert.deepEqual(JSON.parse(captured.options.body), {
    ownerDepartment: "学生工作处",
    approverExternalUserId: "approver-12",
    expectedVersion: 3
  });
});
