package com.yanerdan.venueflow.user.profile.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record UserProfile(
    UserProfileId id,
    ExternalUserId externalUserId,
    String displayName,
    String campusId,
    CampusIdentityType identityType,
    String department,
    String phone,
    String email,
    AccountStatus accountStatus,
    BookingEligibility bookingEligibility,
    long version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static final int MAX_DISPLAY_NAME_LENGTH = 120;
  public static final int MAX_CAMPUS_ID_LENGTH = 64;
  public static final int MAX_DEPARTMENT_LENGTH = 120;
  public static final int MAX_PHONE_LENGTH = 32;
  public static final int MAX_EMAIL_LENGTH = 160;

  public UserProfile {
    Objects.requireNonNull(id, "User profile id must not be null");
    Objects.requireNonNull(externalUserId, "External user id must not be null");
    Objects.requireNonNull(displayName, "Display name must not be null");
    Objects.requireNonNull(identityType, "Identity type must not be null");
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

  public UserProfile(
      UserProfileId id,
      ExternalUserId externalUserId,
      String displayName,
      AccountStatus accountStatus,
      BookingEligibility bookingEligibility,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this(
        id,
        externalUserId,
        displayName,
        null,
        CampusIdentityType.OTHER,
        null,
        null,
        null,
        accountStatus,
        bookingEligibility,
        version,
        createdAt,
        updatedAt);
  }

  public boolean bookingPermitted() {
    return BookingEligibilityEvaluator.isBookingPermitted(accountStatus, bookingEligibility);
  }
}
