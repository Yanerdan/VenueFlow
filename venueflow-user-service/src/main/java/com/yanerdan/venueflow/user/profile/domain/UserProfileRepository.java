package com.yanerdan.venueflow.user.profile.domain;

import java.util.Optional;

public interface UserProfileRepository {

  UserProfileId create(
      ExternalUserId externalUserId,
      String displayName,
      String campusId,
      CampusIdentityType identityType,
      String department,
      String phone,
      String email);

  default UserProfileId create(ExternalUserId externalUserId, String displayName) {
    return create(
        externalUserId, displayName, null, CampusIdentityType.OTHER, null, null, null);
  }

  Optional<UserProfile> findById(UserProfileId id);

  Optional<UserProfile> findByExternalUserId(ExternalUserId externalUserId);

  VersionedUpdateResult updateDisplayName(
      UserProfileId id, String displayName, long expectedVersion);

  VersionedUpdateResult updateAccountStatus(
      UserProfileId id, AccountStatus accountStatus, long expectedVersion);

  VersionedUpdateResult updateBookingEligibility(
      UserProfileId id, BookingEligibility bookingEligibility, long expectedVersion);

  VersionedUpdateResult updateCampusProfile(
      UserProfileId id,
      String displayName,
      String campusId,
      CampusIdentityType identityType,
      String department,
      String phone,
      String email,
      long expectedVersion);

  UserProfilePage findPage(String keyword, int pageNumber, int pageSize);
}
