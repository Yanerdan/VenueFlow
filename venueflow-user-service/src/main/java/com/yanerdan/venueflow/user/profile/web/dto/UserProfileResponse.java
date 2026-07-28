package com.yanerdan.venueflow.user.profile.web.dto;

import com.yanerdan.venueflow.user.profile.domain.AccountStatus;
import com.yanerdan.venueflow.user.profile.domain.BookingEligibility;
import com.yanerdan.venueflow.user.profile.domain.CampusIdentityType;
import com.yanerdan.venueflow.user.profile.domain.UserProfile;
import java.time.LocalDateTime;
import java.util.Objects;

public record UserProfileResponse(
    long id,
    String externalUserId,
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

  public static UserProfileResponse from(UserProfile profile) {
    Objects.requireNonNull(profile, "User profile must not be null");

    return new UserProfileResponse(
        profile.id().value(),
        profile.externalUserId().value(),
        profile.displayName(),
        profile.campusId(),
        profile.identityType(),
        profile.department(),
        profile.phone(),
        profile.email(),
        profile.accountStatus(),
        profile.bookingEligibility(),
        profile.version(),
        profile.createdAt(),
        profile.updatedAt());
  }
}
