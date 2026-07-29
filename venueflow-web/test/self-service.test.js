import assert from "node:assert/strict";
import test from "node:test";
import { buildCalendarEvent, safeCalendarFilename, transitionCandidates } from "../src/self-service.js";

test("calendar export emits a portable event with escaped user text", () => {
  const text = buildCalendarEvent({
    bookingNo: "VF-2026-1",
    activityTitle: "社团交流,复盘",
    contactName: "Ada",
    contactPhone: "13800000000",
    slot: { startAt: "2026-08-01T01:00:00Z", endAt: "2026-08-01T03:00:00Z" },
    resource: { name: "第一会议室", location: "主楼;201" }
  }, "2026-07-29T00:00:00Z");

  assert.match(text, /DTSTART:20260801T010000Z\r\n/);
  assert.match(text, /SUMMARY:社团交流\\,复盘\r\n/);
  assert.match(text, /LOCATION:第一会议室 · 主楼\\;201\r\n/);
  assert.ok(text.endsWith("END:VCALENDAR\r\n"));
});

test("bulk status transition selects only rows requiring a change", () => {
  const items = [{ id: 1, status: "OPEN" }, { id: 2, status: "CLOSED" }];
  assert.deepEqual(transitionCandidates(items, "OPEN").map(item => item.id), [2]);
  assert.deepEqual(transitionCandidates(items, "CLOSED").map(item => item.id), [1]);
});

test("calendar filenames exclude reserved filesystem characters", () => {
  assert.equal(safeCalendarFilename("活动/A:01"), "活动-A-01.ics");
});
