package com.yanerdan.venueflow.booking.expiration.domain;

public record TimeoutReservation(
    long id,
    String bookingNo,
    long slotId,
    int quantity,
    String releaseOperationId,
    long version,
    int attemptCount,
    String leaseOwner) {}
