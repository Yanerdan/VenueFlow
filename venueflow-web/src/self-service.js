const calendarEscape = value => String(value ?? "")
  .replaceAll("\\", "\\\\")
  .replaceAll("\r\n", "\\n")
  .replaceAll("\n", "\\n")
  .replaceAll(",", "\\,")
  .replaceAll(";", "\\;");

const calendarTime = value => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) throw new Error("日历时间无效");
  return date.toISOString().replace(/[-:]/g, "").replace(/\.\d{3}Z$/, "Z");
};

export function buildCalendarEvent(booking, generatedAt = new Date()) {
  if (!booking?.slot?.startAt || !booking?.slot?.endAt) throw new Error("该申请缺少可导出的时段信息");
  const bookingNo = booking.bookingNo || "booking";
  return [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//VenueFlow//Campus Booking//ZH-CN",
    "CALSCALE:GREGORIAN",
    "METHOD:PUBLISH",
    "BEGIN:VEVENT",
    `UID:${calendarEscape(bookingNo)}@venueflow.local`,
    `DTSTAMP:${calendarTime(generatedAt)}`,
    `DTSTART:${calendarTime(booking.slot.startAt)}`,
    `DTEND:${calendarTime(booking.slot.endAt)}`,
    `SUMMARY:${calendarEscape(booking.activityTitle || "校园资源预约")}`,
    `LOCATION:${calendarEscape([booking.resource?.name, booking.resource?.location].filter(Boolean).join(" · "))}`,
    `DESCRIPTION:${calendarEscape(`预约编号：${bookingNo}；联系人：${booking.contactName || "未填写"} ${booking.contactPhone || ""}`)}`,
    "END:VEVENT",
    "END:VCALENDAR",
    ""
  ].join("\r\n");
}

export function transitionCandidates(items, targetStatus) {
  return (items || []).filter(item => item.status !== targetStatus);
}

export function safeCalendarFilename(value) {
  const safe = String(value || "venueflow-booking").replace(/[\\/:*?"<>|]/g, "-").trim();
  return `${safe || "venueflow-booking"}.ics`;
}
