package com.yanerdan.venueflow.user.profile.persistence;

import com.yanerdan.venueflow.user.profile.domain.AccountStatus;
import com.yanerdan.venueflow.user.profile.domain.BookingEligibility;
import com.yanerdan.venueflow.user.profile.domain.CampusIdentityType;
import com.yanerdan.venueflow.user.profile.domain.DuplicateExternalUserIdException;
import com.yanerdan.venueflow.user.profile.domain.ExternalUserId;
import com.yanerdan.venueflow.user.profile.domain.UserProfile;
import com.yanerdan.venueflow.user.profile.domain.UserProfileId;
import com.yanerdan.venueflow.user.profile.domain.UserProfilePage;
import com.yanerdan.venueflow.user.profile.domain.UserProfileRepository;
import com.yanerdan.venueflow.user.profile.domain.VersionedUpdateResult;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@Profile("persistence")
public class MyBatisUserProfileRepository implements UserProfileRepository {

  private final UserProfileMapper mapper;

  public MyBatisUserProfileRepository(UserProfileMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public UserProfileId create(
      ExternalUserId externalUserId,
      String displayName,
      String campusId,
      CampusIdentityType identityType,
      String department,
      String phone,
      String email) {
    validateDisplayName(displayName);

    UserProfileEntity entity = new UserProfileEntity();

    entity.setExternalUserId(externalUserId.value());
    entity.setDisplayName(displayName);
    entity.setCampusId(blankToNull(campusId));
    entity.setIdentityType(
        Objects.requireNonNullElse(identityType, CampusIdentityType.OTHER).name());
    entity.setDepartment(blankToNull(department));
    entity.setPhone(blankToNull(phone));
    entity.setEmail(blankToNull(email));
    entity.setAccountStatus(AccountStatus.ACTIVE.name());
    entity.setBookingEligibility(BookingEligibility.ELIGIBLE.name());
    entity.setVersion(0L);

    int affectedRows;

    try {
      affectedRows = mapper.insertProfile(entity);
    } catch (DuplicateKeyException exception) {
      throw new DuplicateExternalUserIdException(exception);
    }

    if (affectedRows != 1 || entity.getId() == null) {
      throw new IllegalStateException(
          "User profile insert did not produce one generated identifier");
    }

    return new UserProfileId(entity.getId());
  }

  @Override
  public Optional<UserProfile> findById(UserProfileId id) {
    return Optional.ofNullable(mapper.selectById(id.value()))
        .map(MyBatisUserProfileRepository::toDomain);
  }

  @Override
  public Optional<UserProfile> findByExternalUserId(ExternalUserId externalUserId) {
    return Optional.ofNullable(mapper.selectByExternalUserId(externalUserId.value()))
        .map(MyBatisUserProfileRepository::toDomain);
  }

  @Override
  public VersionedUpdateResult updateDisplayName(
      UserProfileId id, String displayName, long expectedVersion) {
    validateDisplayName(displayName);
    validateExpectedVersion(expectedVersion);

    int affectedRows = mapper.updateDisplayName(id.value(), displayName, expectedVersion);

    return classifyUpdate(id, affectedRows);
  }

  @Override
  public VersionedUpdateResult updateAccountStatus(
      UserProfileId id, AccountStatus accountStatus, long expectedVersion) {
    Objects.requireNonNull(accountStatus, "Account status must not be null");
    validateExpectedVersion(expectedVersion);

    int affectedRows =
        mapper.updateAccountStatus(id.value(), accountStatus.name(), expectedVersion);

    return classifyUpdate(id, affectedRows);
  }

  @Override
  public VersionedUpdateResult updateBookingEligibility(
      UserProfileId id, BookingEligibility bookingEligibility, long expectedVersion) {
    Objects.requireNonNull(bookingEligibility, "Booking eligibility must not be null");
    validateExpectedVersion(expectedVersion);

    int affectedRows =
        mapper.updateBookingEligibility(id.value(), bookingEligibility.name(), expectedVersion);

    return classifyUpdate(id, affectedRows);
  }

  @Override
  public VersionedUpdateResult updateCampusProfile(
      UserProfileId id,
      String displayName,
      String campusId,
      CampusIdentityType identityType,
      String department,
      String phone,
      String email,
      long expectedVersion) {
    validateDisplayName(displayName);
    validateExpectedVersion(expectedVersion);
    int affectedRows =
        mapper.updateCampusProfile(
            id.value(),
            displayName.trim(),
            blankToNull(campusId),
            Objects.requireNonNull(identityType, "Identity type must not be null").name(),
            blankToNull(department),
            blankToNull(phone),
            blankToNull(email),
            expectedVersion);
    return classifyUpdate(id, affectedRows);
  }

  @Override
  public UserProfilePage findPage(String keyword, int pageNumber, int pageSize) {
    String normalized = blankToNull(keyword);
    long offset = (long) pageNumber * pageSize;
    return new UserProfilePage(
        mapper.selectPage(normalized, offset, pageSize).stream()
            .map(MyBatisUserProfileRepository::toDomain)
            .toList(),
        pageNumber,
        pageSize,
        mapper.countPage(normalized));
  }

  private VersionedUpdateResult classifyUpdate(UserProfileId id, int affectedRows) {
    if (affectedRows == 1) {
      return VersionedUpdateResult.UPDATED;
    }

    if (affectedRows != 0) {
      throw new IllegalStateException(
          "Versioned update affected an unexpected number of rows: " + affectedRows);
    }

    if (mapper.selectById(id.value()) == null) {
      return VersionedUpdateResult.NOT_FOUND;
    }

    return VersionedUpdateResult.STALE_VERSION;
  }

  private static UserProfile toDomain(UserProfileEntity entity) {
    return new UserProfile(
        new UserProfileId(requirePersistedValue(entity.getId(), "id")),
        new ExternalUserId(requirePersistedValue(entity.getExternalUserId(), "external_user_id")),
        requirePersistedValue(entity.getDisplayName(), "display_name"),
        entity.getCampusId(),
        entity.getIdentityType() == null
            ? CampusIdentityType.OTHER
            : CampusIdentityType.valueOf(entity.getIdentityType()),
        entity.getDepartment(),
        entity.getPhone(),
        entity.getEmail(),
        entity.getAuthoritativeSource(),
        entity.getOrganizationExternalKey(),
        entity.getDirectorySyncedAt(),
        AccountStatus.valueOf(requirePersistedValue(entity.getAccountStatus(), "account_status")),
        BookingEligibility.valueOf(
            requirePersistedValue(entity.getBookingEligibility(), "booking_eligibility")),
        requirePersistedValue(entity.getVersion(), "version"),
        requirePersistedValue(entity.getCreatedAt(), "created_at"),
        requirePersistedValue(entity.getUpdatedAt(), "updated_at"));
  }

  private static void validateDisplayName(String displayName) {
    Objects.requireNonNull(displayName, "Display name must not be null");

    if (displayName.isBlank()) {
      throw new IllegalArgumentException("Display name must not be blank");
    }

    if (displayName.length() > UserProfile.MAX_DISPLAY_NAME_LENGTH) {
      throw new IllegalArgumentException(
          "Display name must not exceed " + UserProfile.MAX_DISPLAY_NAME_LENGTH + " characters");
    }
  }

  private static void validateExpectedVersion(long expectedVersion) {
    if (expectedVersion < 0) {
      throw new IllegalArgumentException("Expected version must not be negative");
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static <T> T requirePersistedValue(T value, String column) {
    return Objects.requireNonNull(value, "Persisted column must not be null: " + column);
  }
}
