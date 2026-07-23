package com.yanerdan.venueflow.user.profile.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.yanerdan.venueflow.user.profile.domain.AccountStatus;
import com.yanerdan.venueflow.user.profile.domain.BookingEligibility;
import com.yanerdan.venueflow.user.profile.domain.ExternalUserId;
import com.yanerdan.venueflow.user.profile.domain.UserProfile;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UserProfileDtoValidationTest {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  @BeforeAll
  static void createValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();

    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void closeValidatorFactory() {
    validatorFactory.close();
  }

  @Test
  void acceptsValidCreationRequest() {
    CreateUserProfileRequest request = new CreateUserProfileRequest("customer-123", "Alice");

    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void rejectsBlankExternalUserId() {
    CreateUserProfileRequest request = new CreateUserProfileRequest(" ", "Alice");

    assertViolation(
        validator.validate(request), "externalUserId", "External user id must not be blank");
  }

  @Test
  void rejectsOversizedExternalUserId() {
    CreateUserProfileRequest request =
        new CreateUserProfileRequest("x".repeat(ExternalUserId.MAX_LENGTH + 1), "Alice");

    assertViolation(
        validator.validate(request),
        "externalUserId",
        "External user id must not exceed 128 characters");
  }

  @Test
  void rejectsBlankDisplayName() {
    CreateUserProfileRequest request = new CreateUserProfileRequest("customer-123", " ");

    assertViolation(validator.validate(request), "displayName", "Display name must not be blank");
  }

  @Test
  void rejectsOversizedDisplayName() {
    UpdateDisplayNameRequest request =
        new UpdateDisplayNameRequest("x".repeat(UserProfile.MAX_DISPLAY_NAME_LENGTH + 1), 0L);

    assertViolation(
        validator.validate(request), "displayName", "Display name must not exceed 120 characters");
  }

  @Test
  void requiresExpectedVersion() {
    UpdateDisplayNameRequest request = new UpdateDisplayNameRequest("Alice", null);

    assertViolation(validator.validate(request), "expectedVersion", "Expected version is required");
  }

  @Test
  void rejectsNegativeExpectedVersion() {
    UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(AccountStatus.ACTIVE, -1L);

    assertViolation(
        validator.validate(request), "expectedVersion", "Expected version must not be negative");
  }

  @Test
  void requiresAccountStatus() {
    UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(null, 0L);

    assertViolation(validator.validate(request), "accountStatus", "Account status is required");
  }

  @Test
  void requiresBookingEligibility() {
    UpdateBookingEligibilityRequest request = new UpdateBookingEligibilityRequest(null, 0L);

    assertViolation(
        validator.validate(request), "bookingEligibility", "Booking eligibility is required");
  }

  @Test
  void acceptsValidEligibilityUpdate() {
    UpdateBookingEligibilityRequest request =
        new UpdateBookingEligibilityRequest(BookingEligibility.INELIGIBLE, 4L);

    assertThat(validator.validate(request)).isEmpty();
  }

  private static void assertViolation(
      Set<? extends ConstraintViolation<?>> violations, String property, String message) {
    assertThat(violations)
        .anySatisfy(
            violation -> {
              assertThat(violation.getPropertyPath().toString()).isEqualTo(property);

              assertThat(violation.getMessage()).isEqualTo(message);
            });
  }
}
