package com.yanerdan.venueflow.booking.web;

import com.yanerdan.venueflow.booking.application.BookingErrorCode;
import com.yanerdan.venueflow.booking.application.BookingException;
import java.util.Set;

final class BookingRoleGuard {
  private static final Set<String> APPROVAL_ROLES = Set.of("APPROVER", "SYSTEM_ADMIN");

  private BookingRoleGuard() {}

  static void requireApprover(String role) {
    if (!APPROVAL_ROLES.contains(role)) {
      throw new BookingException(
          BookingErrorCode.BOOKING_FORBIDDEN, "Campus management permission is required");
    }
  }
}
