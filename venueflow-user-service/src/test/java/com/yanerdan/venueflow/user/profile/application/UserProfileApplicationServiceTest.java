package com.yanerdan.venueflow.user.profile.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yanerdan.venueflow.user.profile.domain.AccountStatus;
import com.yanerdan.venueflow.user.profile.domain.BookingEligibility;
import com.yanerdan.venueflow.user.profile.domain.DuplicateExternalUserIdException;
import com.yanerdan.venueflow.user.profile.domain.ExternalUserId;
import com.yanerdan.venueflow.user.profile.domain.UserProfile;
import com.yanerdan.venueflow.user.profile.domain.UserProfileId;
import com.yanerdan.venueflow.user.profile.domain.UserProfileRepository;
import com.yanerdan.venueflow.user.profile.domain.VersionedUpdateResult;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileApplicationServiceTest {

  private static final UserProfileId PROFILE_ID = new UserProfileId(42L);

  @Mock private UserProfileRepository repository;

  private UserProfileApplicationService service;

  @BeforeEach
  void setUp() {
    service = new UserProfileApplicationService(repository);
  }

  @Test
  void createsAndReturnsProfileWithRepositoryDefaults() {
    UserProfile persistedProfile = persistedProfile();

    when(repository.create(new ExternalUserId("customer-123"), "Alice")).thenReturn(PROFILE_ID);

    when(repository.findById(PROFILE_ID)).thenReturn(Optional.of(persistedProfile));

    UserProfile created = service.create("customer-123", "Alice");

    assertThat(created).isEqualTo(persistedProfile);

    assertThat(created.accountStatus()).isEqualTo(AccountStatus.ACTIVE);

    assertThat(created.bookingEligibility()).isEqualTo(BookingEligibility.ELIGIBLE);

    assertThat(created.version()).isZero();

    verify(repository).create(new ExternalUserId("customer-123"), "Alice");

    verify(repository).findById(PROFILE_ID);
  }

  @Test
  void propagatesDuplicateExternalIdentifierConflict() {
    DuplicateExternalUserIdException conflict =
        new DuplicateExternalUserIdException(new IllegalStateException("duplicate"));

    when(repository.create(new ExternalUserId("customer-123"), "Alice")).thenThrow(conflict);

    assertThatThrownBy(() -> service.create("customer-123", "Alice")).isSameAs(conflict);
  }

  @Test
  void failsSafelyWhenCreatedProfileCannotBeReloaded() {
    when(repository.create(new ExternalUserId("customer-123"), "Alice")).thenReturn(PROFILE_ID);

    when(repository.findById(PROFILE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create("customer-123", "Alice"))
        .isInstanceOf(UserProfilePersistenceException.class)
        .hasMessage("Created user profile could not be reloaded");
  }

  @Test
  void retrievesExistingProfile() {
    UserProfile persistedProfile = persistedProfile();

    when(repository.findById(PROFILE_ID)).thenReturn(Optional.of(persistedProfile));

    UserProfile result = service.getById(PROFILE_ID.value());

    assertThat(result).isEqualTo(persistedProfile);
  }

  @Test
  void rejectsMissingProfileWithStableException() {
    when(repository.findById(PROFILE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(PROFILE_ID.value()))
        .isInstanceOf(UserProfileNotFoundException.class)
        .hasMessage("User profile was not found")
        .satisfies(
            exception -> {
              UserProfileNotFoundException notFound = (UserProfileNotFoundException) exception;

              assertThat(notFound.userProfileId()).isEqualTo(PROFILE_ID.value());
            });
  }

  @Test
  void rejectsInvalidProfileIdentifierBeforeRepositoryCall() {
    assertThatThrownBy(() -> service.getById(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("User profile id must be positive");
  }

  private static UserProfile persistedProfile() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 7, 23, 10, 0);

    return new UserProfile(
        PROFILE_ID,
        new ExternalUserId("customer-123"),
        "Alice",
        AccountStatus.ACTIVE,
        BookingEligibility.ELIGIBLE,
        0L,
        createdAt,
        createdAt);
  }

  private static UserProfile persistedProfile(
      String displayName,
      AccountStatus accountStatus,
      BookingEligibility bookingEligibility,
      long version) {
    LocalDateTime createdAt = LocalDateTime.of(2026, 7, 23, 10, 0);

    LocalDateTime updatedAt = createdAt.plusMinutes(version);

    return new UserProfile(
        PROFILE_ID,
        new ExternalUserId("customer-123"),
        displayName,
        accountStatus,
        bookingEligibility,
        version,
        createdAt,
        updatedAt);
  }

  @Test
  void updatesDisplayNameAndReturnsNextVersion() {
    UserProfile updated =
        persistedProfile("Alice Chen", AccountStatus.ACTIVE, BookingEligibility.ELIGIBLE, 4L);

    when(repository.updateDisplayName(PROFILE_ID, "Alice Chen", 3L))
        .thenReturn(VersionedUpdateResult.UPDATED);

    when(repository.findById(PROFILE_ID)).thenReturn(Optional.of(updated));

    UserProfile result = service.updateDisplayName(PROFILE_ID.value(), "Alice Chen", 3L);

    assertThat(result.displayName()).isEqualTo("Alice Chen");

    assertThat(result.version()).isEqualTo(4L);

    verify(repository).updateDisplayName(PROFILE_ID, "Alice Chen", 3L);
  }

  @Test
  void updatesAccountStatusAndReturnsNextVersion() {
    UserProfile updated =
        persistedProfile("Alice", AccountStatus.SUSPENDED, BookingEligibility.ELIGIBLE, 5L);

    when(repository.updateAccountStatus(PROFILE_ID, AccountStatus.SUSPENDED, 4L))
        .thenReturn(VersionedUpdateResult.UPDATED);

    when(repository.findById(PROFILE_ID)).thenReturn(Optional.of(updated));

    UserProfile result =
        service.updateAccountStatus(PROFILE_ID.value(), AccountStatus.SUSPENDED, 4L);

    assertThat(result.accountStatus()).isEqualTo(AccountStatus.SUSPENDED);

    assertThat(result.version()).isEqualTo(5L);
  }

  @Test
  void updatesBookingEligibilityAndReturnsNextVersion() {
    UserProfile updated =
        persistedProfile("Alice", AccountStatus.ACTIVE, BookingEligibility.INELIGIBLE, 6L);

    when(repository.updateBookingEligibility(PROFILE_ID, BookingEligibility.INELIGIBLE, 5L))
        .thenReturn(VersionedUpdateResult.UPDATED);

    when(repository.findById(PROFILE_ID)).thenReturn(Optional.of(updated));

    UserProfile result =
        service.updateBookingEligibility(PROFILE_ID.value(), BookingEligibility.INELIGIBLE, 5L);

    assertThat(result.bookingEligibility()).isEqualTo(BookingEligibility.INELIGIBLE);

    assertThat(result.bookingPermitted()).isFalse();

    assertThat(result.version()).isEqualTo(6L);
  }

  @Test
  void rejectsStaleDisplayNameUpdate() {
    when(repository.updateDisplayName(PROFILE_ID, "Alice Chen", 3L))
        .thenReturn(VersionedUpdateResult.STALE_VERSION);

    assertThatThrownBy(() -> service.updateDisplayName(PROFILE_ID.value(), "Alice Chen", 3L))
        .isInstanceOf(StaleUserProfileVersionException.class)
        .hasMessage("User profile version is stale")
        .satisfies(
            exception -> {
              StaleUserProfileVersionException stale = (StaleUserProfileVersionException) exception;

              assertThat(stale.userProfileId()).isEqualTo(PROFILE_ID.value());

              assertThat(stale.expectedVersion()).isEqualTo(3L);
            });
  }

  @Test
  void rejectsUpdateForMissingProfile() {
    when(repository.updateAccountStatus(PROFILE_ID, AccountStatus.SUSPENDED, 0L))
        .thenReturn(VersionedUpdateResult.NOT_FOUND);

    assertThatThrownBy(
            () -> service.updateAccountStatus(PROFILE_ID.value(), AccountStatus.SUSPENDED, 0L))
        .isInstanceOf(UserProfileNotFoundException.class)
        .hasMessage("User profile was not found");
  }

  @Test
  void failsSafelyWhenUpdatedVersionDidNotAdvanceExactlyOnce() {
    UserProfile unexpected =
        persistedProfile("Alice Chen", AccountStatus.ACTIVE, BookingEligibility.ELIGIBLE, 5L);

    when(repository.updateDisplayName(PROFILE_ID, "Alice Chen", 3L))
        .thenReturn(VersionedUpdateResult.UPDATED);

    when(repository.findById(PROFILE_ID)).thenReturn(Optional.of(unexpected));

    assertThatThrownBy(() -> service.updateDisplayName(PROFILE_ID.value(), "Alice Chen", 3L))
        .isInstanceOf(UserProfilePersistenceException.class)
        .hasMessage("Updated user profile has an unexpected version");
  }

  @Test
  void rejectsNegativeExpectedVersionBeforeRepositoryCall() {
    assertThatThrownBy(() -> service.updateDisplayName(PROFILE_ID.value(), "Alice Chen", -1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Expected version must not be negative");

    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @CsvSource({
    "ACTIVE, ELIGIBLE, true",
    "ACTIVE, INELIGIBLE, false",
    "SUSPENDED, ELIGIBLE, false",
    "SUSPENDED, INELIGIBLE, false"
  })
  void returnsBoundedBookingEligibilityView(
      AccountStatus accountStatus,
      BookingEligibility bookingEligibility,
      boolean expectedBookingPermission) {
    UserProfile profile = persistedProfile("Alice", accountStatus, bookingEligibility, 7L);

    when(repository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

    UserBookingEligibilityView result = service.getBookingEligibility(PROFILE_ID.value());

    assertThat(result.userProfileId()).isEqualTo(PROFILE_ID.value());

    assertThat(result.accountStatus()).isEqualTo(accountStatus);

    assertThat(result.bookingEligibility()).isEqualTo(bookingEligibility);

    assertThat(result.bookingPermitted()).isEqualTo(expectedBookingPermission);

    assertThat(result.version()).isEqualTo(7L);

    assertThat(result.updatedAt()).isEqualTo(profile.updatedAt());

    verify(repository).findById(PROFILE_ID);
  }

  @Test
  void rejectsEligibilityRequestForMissingProfile() {
    when(repository.findById(PROFILE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getBookingEligibility(PROFILE_ID.value()))
        .isInstanceOf(UserProfileNotFoundException.class)
        .hasMessage("User profile was not found");
  }
}
