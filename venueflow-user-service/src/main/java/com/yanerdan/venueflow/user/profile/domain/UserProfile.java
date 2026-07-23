package com.yanerdan.venueflow.user.profile.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record UserProfile(
    UserProfileId id,
    ExternalUserId externalUserId,
    String displayName,
    AccountStatus accountStatus,
    BookingEligibility bookingEligibility,
    long version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static final int MAX_DISPLAY_NAME_LENGTH = 120;

  public UserProfile {
    Objects.requireNonNull(id, "User profile id must not be null");
    Objects.requireNonNull(externalUserId, "External user id must not be null");
    Objects.requireNonNull(displayName, "Display name must not be null");
    Objects.requireNonNull(accountStatus, "Account status must not be null");
    Objects.requireNonNull(bookingEligibility, "Booking eligibility must not be null");
    Objects.requireNonNull(createdAt, "Created timestamp must not be null");
    Objects.requireNonNull(updatedAt, "Updated timestamp must not be null");

    if (displayName.isBlank()) {
      throw new IllegalArgumentException("Display name must not be blank");
    }

    if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
      throw new IllegalArgumentException(
          "Display name must not exceed " + MAX_DISPLAY_NAME_LENGTH + " characters");
    }

    if (version < 0) {
      throw new IllegalArgumentException("User profile version must not be negative");
    }
  }

  public boolean bookingPermitted() {
    return BookingEligibilityEvaluator.isBookingPermitted(accountStatus, bookingEligibility);
  }
}
