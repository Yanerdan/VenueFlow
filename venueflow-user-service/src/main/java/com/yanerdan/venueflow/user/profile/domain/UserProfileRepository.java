package com.yanerdan.venueflow.user.profile.domain;

import java.util.Optional;

public interface UserProfileRepository {

  UserProfileId create(ExternalUserId externalUserId, String displayName);

  Optional<UserProfile> findById(UserProfileId id);

  Optional<UserProfile> findByExternalUserId(ExternalUserId externalUserId);

  VersionedUpdateResult updateDisplayName(
      UserProfileId id, String displayName, long expectedVersion);

  VersionedUpdateResult updateAccountStatus(
      UserProfileId id, AccountStatus accountStatus, long expectedVersion);

  VersionedUpdateResult updateBookingEligibility(
      UserProfileId id, BookingEligibility bookingEligibility, long expectedVersion);
}
