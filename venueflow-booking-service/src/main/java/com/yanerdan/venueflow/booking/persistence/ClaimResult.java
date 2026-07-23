package com.yanerdan.venueflow.booking.persistence;

import com.yanerdan.venueflow.booking.domain.BookingReservation;

public record ClaimResult(
    Kind kind, String requestId, BookingReservation reservation, String failureCode) {
  public enum Kind {
    OWNER,
    SUCCEEDED,
    PROCESSING,
    FAILED,
    CONFLICT
  }
}
