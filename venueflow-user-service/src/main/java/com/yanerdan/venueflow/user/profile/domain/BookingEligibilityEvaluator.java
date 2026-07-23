package com.yanerdan.venueflow.user.profile.domain;

import java.util.Objects;

public final class BookingEligibilityEvaluator {

  private BookingEligibilityEvaluator() {}

  public static boolean isBookingPermitted(
      AccountStatus accountStatus, BookingEligibility bookingEligibility) {
    Objects.requireNonNull(accountStatus, "Account status must not be null");

    Objects.requireNonNull(bookingEligibility, "Booking eligibility must not be null");

    return accountStatus == AccountStatus.ACTIVE
        && bookingEligibility == BookingEligibility.ELIGIBLE;
  }
}
