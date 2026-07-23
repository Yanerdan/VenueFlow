package com.yanerdan.venueflow.user.profile.web.dto;

import com.yanerdan.venueflow.user.profile.application.UserBookingEligibilityView;
import com.yanerdan.venueflow.user.profile.domain.AccountStatus;
import com.yanerdan.venueflow.user.profile.domain.BookingEligibility;
import java.time.LocalDateTime;
import java.util.Objects;

public record BookingEligibilityResponse(
    long userId,
    AccountStatus accountStatus,
    BookingEligibility bookingEligibility,
    boolean bookingPermitted,
    long version,
    LocalDateTime updatedAt) {

  public static BookingEligibilityResponse from(UserBookingEligibilityView view) {
    Objects.requireNonNull(view, "Booking eligibility view must not be null");

    return new BookingEligibilityResponse(
        view.userProfileId(),
        view.accountStatus(),
        view.bookingEligibility(),
        view.bookingPermitted(),
        view.version(),
        view.updatedAt());
  }
}
