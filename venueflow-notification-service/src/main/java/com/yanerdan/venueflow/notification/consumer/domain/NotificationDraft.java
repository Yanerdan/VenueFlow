package com.yanerdan.venueflow.notification.consumer.domain;

public record NotificationDraft(String type, String title, String body) {
  public static NotificationDraft from(BookingEvent event) {
    if ("CONFIRMED".equals(event.status())) {
      return new NotificationDraft(
          "BOOKING_CONFIRMED", "预约已确认", "预约 " + event.bookingNo() + " 已确认，数量 " + event.quantity());
    }
    if ("EXPIRED".equals(event.status())) {
      return new NotificationDraft(
          "BOOKING_EXPIRED", "预约已过期", "预约 " + event.bookingNo() + " 已过期，数量 " + event.quantity());
    }
    return new NotificationDraft(
        "BOOKING_CANCELLED", "预约已取消", "预约 " + event.bookingNo() + " 已取消，数量 " + event.quantity());
  }
}
