package com.yanerdan.venueflow.user.profile.application;

import com.yanerdan.venueflow.user.profile.domain.AccountStatus;
import com.yanerdan.venueflow.user.profile.domain.BookingEligibility;
import com.yanerdan.venueflow.user.profile.domain.ExternalUserId;
import com.yanerdan.venueflow.user.profile.domain.UserProfile;
import com.yanerdan.venueflow.user.profile.domain.UserProfileId;
import com.yanerdan.venueflow.user.profile.domain.UserProfileRepository;
import com.yanerdan.venueflow.user.profile.domain.VersionedUpdateResult;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("persistence")
@Transactional
public class UserProfileApplicationService {

  private final UserProfileRepository repository;

  public UserProfileApplicationService(UserProfileRepository repository) {
    this.repository = repository;
  }

  public UserProfile create(String externalUserId, String displayName) {
    ExternalUserId validatedExternalUserId = new ExternalUserId(externalUserId);

    UserProfileId createdId = repository.create(validatedExternalUserId, displayName);

    return repository
        .findById(createdId)
        .orElseThrow(
            () ->
                new UserProfilePersistenceException("Created user profile could not be reloaded"));
  }

  @Transactional(readOnly = true)
  public UserProfile getById(long userProfileId) {
    UserProfileId id = new UserProfileId(userProfileId);

    return findRequired(id);
  }

  @Transactional(readOnly = true)
  public UserProfile getByExternalUserId(String externalUserId) {
    return repository
        .findByExternalUserId(new ExternalUserId(externalUserId))
        .orElseThrow(() -> new UserProfileNotFoundException(0L));
  }

  public UserProfile updateDisplayName(
      long userProfileId, String displayName, long expectedVersion) {
    UserProfileId id = new UserProfileId(userProfileId);

    validateExpectedVersion(expectedVersion);

    VersionedUpdateResult result = repository.updateDisplayName(id, displayName, expectedVersion);

    return completeVersionedUpdate(id, expectedVersion, result);
  }

  public UserProfile updateAccountStatus(
      long userProfileId, AccountStatus accountStatus, long expectedVersion) {
    UserProfileId id = new UserProfileId(userProfileId);

    Objects.requireNonNull(accountStatus, "Account status must not be null");

    validateExpectedVersion(expectedVersion);

    VersionedUpdateResult result =
        repository.updateAccountStatus(id, accountStatus, expectedVersion);

    return completeVersionedUpdate(id, expectedVersion, result);
  }

  public UserProfile updateBookingEligibility(
      long userProfileId, BookingEligibility bookingEligibility, long expectedVersion) {
    UserProfileId id = new UserProfileId(userProfileId);

    Objects.requireNonNull(bookingEligibility, "Booking eligibility must not be null");

    validateExpectedVersion(expectedVersion);

    VersionedUpdateResult result =
        repository.updateBookingEligibility(id, bookingEligibility, expectedVersion);

    return completeVersionedUpdate(id, expectedVersion, result);
  }

  private UserProfile completeVersionedUpdate(
      UserProfileId id, long expectedVersion, VersionedUpdateResult result) {
    return switch (result) {
      case UPDATED -> reloadUpdatedProfile(id, expectedVersion);
      case NOT_FOUND -> throw new UserProfileNotFoundException(id.value());
      case STALE_VERSION -> throw new StaleUserProfileVersionException(id.value(), expectedVersion);
    };
  }

  private UserProfile reloadUpdatedProfile(UserProfileId id, long expectedVersion) {
    UserProfile updated =
        repository
            .findById(id)
            .orElseThrow(
                () ->
                    new UserProfilePersistenceException(
                        "Updated user profile could not be reloaded"));

    long expectedUpdatedVersion = expectedVersion + 1;

    if (updated.version() != expectedUpdatedVersion) {
      throw new UserProfilePersistenceException("Updated user profile has an unexpected version");
    }

    return updated;
  }

  private UserProfile findRequired(UserProfileId id) {
    return repository.findById(id).orElseThrow(() -> new UserProfileNotFoundException(id.value()));
  }

  private static void validateExpectedVersion(long expectedVersion) {
    if (expectedVersion < 0) {
      throw new IllegalArgumentException("Expected version must not be negative");
    }

    if (expectedVersion == Long.MAX_VALUE) {
      throw new IllegalArgumentException("Expected version is too large");
    }
  }

  @Transactional(readOnly = true)
  public UserBookingEligibilityView getBookingEligibility(long userProfileId) {
    UserProfileId id = new UserProfileId(userProfileId);

    return UserBookingEligibilityView.from(findRequired(id));
  }
}
