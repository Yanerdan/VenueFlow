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
