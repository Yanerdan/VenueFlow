package com.yanerdan.venueflow.user.profile.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yanerdan.venueflow.user.profile.domain.AccountStatus;
import com.yanerdan.venueflow.user.profile.domain.BookingEligibility;
import com.yanerdan.venueflow.user.profile.domain.DuplicateExternalUserIdException;
import com.yanerdan.venueflow.user.profile.domain.ExternalUserId;
import com.yanerdan.venueflow.user.profile.domain.UserProfile;
import com.yanerdan.venueflow.user.profile.domain.UserProfileId;
import com.yanerdan.venueflow.user.profile.domain.VersionedUpdateResult;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class MyBatisUserProfileRepositoryTest {

  private static final long PROFILE_ID = 42L;

  @Mock private UserProfileMapper mapper;

  private MyBatisUserProfileRepository repository;

  @BeforeEach
  void setUp() {
    repository = new MyBatisUserProfileRepository(mapper);
  }

  @Test
  void createsProfileWithExpectedDefaults() {
    when(mapper.insertProfile(any(UserProfileEntity.class)))
        .thenAnswer(
            invocation -> {
              UserProfileEntity entity = invocation.getArgument(0);

              entity.setId(PROFILE_ID);

              return 1;
            });

    UserProfileId createdId = repository.create(new ExternalUserId("customer-123"), "Alice");

    assertThat(createdId).isEqualTo(new UserProfileId(PROFILE_ID));

    ArgumentCaptor<UserProfileEntity> captor = ArgumentCaptor.forClass(UserProfileEntity.class);

    verify(mapper).insertProfile(captor.capture());

    UserProfileEntity inserted = captor.getValue();

    assertThat(inserted.getExternalUserId()).isEqualTo("customer-123");

    assertThat(inserted.getDisplayName()).isEqualTo("Alice");

    assertThat(inserted.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE.name());

    assertThat(inserted.getBookingEligibility()).isEqualTo(BookingEligibility.ELIGIBLE.name());

    assertThat(inserted.getVersion()).isZero();
  }

  @Test
  void translatesDuplicateExternalIdentifier() {
    DuplicateKeyException duplicateKeyException =
        new DuplicateKeyException("Duplicate external user identifier");

    when(mapper.insertProfile(any(UserProfileEntity.class))).thenThrow(duplicateKeyException);

    assertThatThrownBy(() -> repository.create(new ExternalUserId("customer-123"), "Alice"))
        .isInstanceOf(DuplicateExternalUserIdException.class)
        .hasMessage("A user profile already exists for the external user identifier")
        .hasCause(duplicateKeyException);
  }

  @ParameterizedTest
  @CsvSource({
    "ACTIVE, ELIGIBLE, true",
    "ACTIVE, INELIGIBLE, false",
    "SUSPENDED, ELIGIBLE, false",
    "SUSPENDED, INELIGIBLE, false"
  })
  void mapsSupportedPersistedStates(
      AccountStatus accountStatus,
      BookingEligibility bookingEligibility,
      boolean expectedBookingPermission) {
    UserProfileEntity entity = persistedEntity(accountStatus, bookingEligibility, 3L);

    when(mapper.selectById(PROFILE_ID)).thenReturn(entity);

    UserProfile profile = repository.findById(new UserProfileId(PROFILE_ID)).orElseThrow();

    assertThat(profile.id()).isEqualTo(new UserProfileId(PROFILE_ID));

    assertThat(profile.externalUserId()).isEqualTo(new ExternalUserId("customer-123"));

    assertThat(profile.displayName()).isEqualTo("Alice");

    assertThat(profile.accountStatus()).isEqualTo(accountStatus);

    assertThat(profile.bookingEligibility()).isEqualTo(bookingEligibility);

    assertThat(profile.version()).isEqualTo(3L);

    assertThat(profile.bookingPermitted()).isEqualTo(expectedBookingPermission);
  }

  @Test
  void rejectsUnsupportedPersistedAccountStatus() {
    UserProfileEntity entity =
        persistedEntity(AccountStatus.ACTIVE, BookingEligibility.ELIGIBLE, 0L);

    entity.setAccountStatus("LOCKED");

    when(mapper.selectById(PROFILE_ID)).thenReturn(entity);

    assertThatThrownBy(() -> repository.findById(new UserProfileId(PROFILE_ID)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("LOCKED");
  }

  @Test
  void returnsUpdatedForSuccessfulDisplayNameUpdate() {
    when(mapper.updateDisplayName(PROFILE_ID, "Alice Chen", 3L)).thenReturn(1);

    VersionedUpdateResult result =
        repository.updateDisplayName(new UserProfileId(PROFILE_ID), "Alice Chen", 3L);

    assertThat(result).isEqualTo(VersionedUpdateResult.UPDATED);

    verify(mapper).updateDisplayName(PROFILE_ID, "Alice Chen", 3L);
  }

  @Test
  void returnsStaleVersionWhenProfileExistsAfterZeroRowUpdate() {
    when(mapper.updateDisplayName(PROFILE_ID, "Alice Chen", 3L)).thenReturn(0);

    when(mapper.selectById(PROFILE_ID))
        .thenReturn(persistedEntity(AccountStatus.ACTIVE, BookingEligibility.ELIGIBLE, 4L));

    VersionedUpdateResult result =
        repository.updateDisplayName(new UserProfileId(PROFILE_ID), "Alice Chen", 3L);

    assertThat(result).isEqualTo(VersionedUpdateResult.STALE_VERSION);
  }

  @Test
  void returnsNotFoundWhenProfileDoesNotExistAfterZeroRowUpdate() {
    when(mapper.updateDisplayName(PROFILE_ID, "Alice Chen", 0L)).thenReturn(0);

    when(mapper.selectById(PROFILE_ID)).thenReturn(null);

    VersionedUpdateResult result =
        repository.updateDisplayName(new UserProfileId(PROFILE_ID), "Alice Chen", 0L);

    assertThat(result).isEqualTo(VersionedUpdateResult.NOT_FOUND);
  }

  @Test
  void passesAccountStatusToConditionalUpdate() {
    when(mapper.updateAccountStatus(PROFILE_ID, AccountStatus.SUSPENDED.name(), 5L)).thenReturn(1);

    VersionedUpdateResult result =
        repository.updateAccountStatus(new UserProfileId(PROFILE_ID), AccountStatus.SUSPENDED, 5L);

    assertThat(result).isEqualTo(VersionedUpdateResult.UPDATED);

    verify(mapper).updateAccountStatus(PROFILE_ID, AccountStatus.SUSPENDED.name(), 5L);
  }

  @Test
  void passesBookingEligibilityToConditionalUpdate() {
    when(mapper.updateBookingEligibility(PROFILE_ID, BookingEligibility.INELIGIBLE.name(), 6L))
        .thenReturn(1);

    VersionedUpdateResult result =
        repository.updateBookingEligibility(
            new UserProfileId(PROFILE_ID), BookingEligibility.INELIGIBLE, 6L);

    assertThat(result).isEqualTo(VersionedUpdateResult.UPDATED);

    verify(mapper).updateBookingEligibility(PROFILE_ID, BookingEligibility.INELIGIBLE.name(), 6L);
  }

  @Test
  void rejectsUnexpectedAffectedRowCount() {
    when(mapper.updateDisplayName(PROFILE_ID, "Alice Chen", 3L)).thenReturn(2);

    assertThatThrownBy(
            () -> repository.updateDisplayName(new UserProfileId(PROFILE_ID), "Alice Chen", 3L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Versioned update affected an unexpected number of rows: 2");
  }

  private static UserProfileEntity persistedEntity(
      AccountStatus accountStatus, BookingEligibility bookingEligibility, long version) {
    LocalDateTime createdAt = LocalDateTime.of(2026, 7, 23, 9, 0);

    LocalDateTime updatedAt = createdAt.plusMinutes(5);

    UserProfileEntity entity = new UserProfileEntity();

    entity.setId(PROFILE_ID);
    entity.setExternalUserId("customer-123");
    entity.setDisplayName("Alice");
    entity.setAccountStatus(accountStatus.name());
    entity.setBookingEligibility(bookingEligibility.name());
    entity.setVersion(version);
    entity.setCreatedAt(createdAt);
    entity.setUpdatedAt(updatedAt);

    return entity;
  }
}
