package com.yanerdan.venueflow.user.profile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UserProfileDomainTest {

  @ParameterizedTest
  @CsvSource({
    "ACTIVE, ELIGIBLE, true",
    "ACTIVE, INELIGIBLE, false",
    "SUSPENDED, ELIGIBLE, false",
    "SUSPENDED, INELIGIBLE, false"
  })
  void evaluatesBookingPermission(
      AccountStatus accountStatus, BookingEligibility bookingEligibility, boolean expected) {
    assertThat(BookingEligibilityEvaluator.isBookingPermitted(accountStatus, bookingEligibility))
        .isEqualTo(expected);
  }

  @Test
  void rejectsMissingAccountStatus() {
    assertThatNullPointerException()
        .isThrownBy(
            () -> BookingEligibilityEvaluator.isBookingPermitted(null, BookingEligibility.ELIGIBLE))
        .withMessage("Account status must not be null");
  }

  @Test
  void rejectsMissingBookingEligibility() {
    assertThatNullPointerException()
        .isThrownBy(
            () -> BookingEligibilityEvaluator.isBookingPermitted(AccountStatus.ACTIVE, null))
        .withMessage("Booking eligibility must not be null");
  }

  @Test
  void acceptsPositiveProfileId() {
    UserProfileId id = new UserProfileId(42);

    assertThat(id.value()).isEqualTo(42);
  }

  @Test
  void rejectsNonPositiveProfileId() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new UserProfileId(0))
        .withMessage("User profile id must be positive");
  }

  @Test
  void acceptsValidExternalUserId() {
    ExternalUserId externalUserId = new ExternalUserId("customer-123");

    assertThat(externalUserId.value()).isEqualTo("customer-123");
  }

  @Test
  void rejectsBlankExternalUserId() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ExternalUserId("   "))
        .withMessage("External user id must not be blank");
  }

  @Test
  void rejectsExternalUserIdWithSurroundingWhitespace() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ExternalUserId(" customer-123 "))
        .withMessage("External user id must not contain surrounding whitespace");
  }

  @Test
  void rejectsOversizedExternalUserId() {
    String oversized = "x".repeat(ExternalUserId.MAX_LENGTH + 1);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ExternalUserId(oversized))
        .withMessage("External user id must not exceed 128 characters");
  }
}
