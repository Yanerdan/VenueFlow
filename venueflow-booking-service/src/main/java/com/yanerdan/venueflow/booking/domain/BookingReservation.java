package com.yanerdan.venueflow.booking.domain;

import java.time.LocalDateTime;

public record BookingReservation(
    Long id,
    String bookingNo,
    String requestId,
    long userId,
    long slotId,
    int quantity,
    BookingStatus status,
    String allocationOperationId,
    String releaseOperationId,
    long version,
    LocalDateTime createdAt,
    LocalDateTime confirmedAt,
    LocalDateTime cancelledAt,
    LocalDateTime updatedAt) {}
