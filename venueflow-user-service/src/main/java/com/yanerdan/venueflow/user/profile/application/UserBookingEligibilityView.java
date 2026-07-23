package com.yanerdan.venueflow.user.profile.application;

import com.yanerdan.venueflow.user.profile.domain.AccountStatus;
import com.yanerdan.venueflow.user.profile.domain.BookingEligibility;
import com.yanerdan.venueflow.user.profile.domain.UserProfile;
import java.time.LocalDateTime;
import java.util.Objects;

public record UserBookingEligibilityView(
    long userProfileId,
    AccountStatus accountStatus,
    BookingEligibility bookingEligibility,
    boolean bookingPermitted,
    long version,
    LocalDateTime updatedAt) {

  public static UserBookingEligibilityView from(UserProfile profile) {
    Objects.requireNonNull(profile, "User profile must not be null");

    return new UserBookingEligibilityView(
        profile.id().value(),
        profile.accountStatus(),
        profile.bookingEligibility(),
        profile.bookingPermitted(),
        profile.version(),
        profile.updatedAt());
  }
}
