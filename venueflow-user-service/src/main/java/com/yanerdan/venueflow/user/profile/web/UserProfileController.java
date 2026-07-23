package com.yanerdan.venueflow.user.profile.web;

import com.yanerdan.venueflow.user.profile.application.UserBookingEligibilityView;
import com.yanerdan.venueflow.user.profile.application.UserProfileApplicationService;
import com.yanerdan.venueflow.user.profile.domain.UserProfile;
import com.yanerdan.venueflow.user.profile.web.dto.BookingEligibilityResponse;
import com.yanerdan.venueflow.user.profile.web.dto.CreateUserProfileRequest;
import com.yanerdan.venueflow.user.profile.web.dto.UpdateAccountStatusRequest;
import com.yanerdan.venueflow.user.profile.web.dto.UpdateBookingEligibilityRequest;
import com.yanerdan.venueflow.user.profile.web.dto.UpdateDisplayNameRequest;
import com.yanerdan.venueflow.user.profile.web.dto.UserProfileResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("persistence")
@RequestMapping("/api/v1/users")
public class UserProfileController {

  private final UserProfileApplicationService service;

  public UserProfileController(UserProfileApplicationService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<UserProfileResponse> create(
      @Valid @RequestBody CreateUserProfileRequest request) {
    UserProfile created = service.create(request.externalUserId(), request.displayName());

    UserProfileResponse response = UserProfileResponse.from(created);

    URI location = URI.create("/api/v1/users/" + response.id());

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping("/{userId}")
  public UserProfileResponse getById(
      @PathVariable("userId") @Positive(message = "User profile id must be positive") long userId) {
    return UserProfileResponse.from(service.getById(userId));
  }

  @GetMapping("/{userId}/booking-eligibility")
  public BookingEligibilityResponse getBookingEligibility(
      @PathVariable("userId") @Positive(message = "User profile id must be positive") long userId) {
    UserBookingEligibilityView view = service.getBookingEligibility(userId);

    return BookingEligibilityResponse.from(view);
  }

  @PatchMapping("/{userId}/profile")
  public UserProfileResponse updateDisplayName(
      @PathVariable("userId") @Positive(message = "User profile id must be positive") long userId,
      @Valid @RequestBody UpdateDisplayNameRequest request) {
    UserProfile updated =
        service.updateDisplayName(
            userId, request.displayName(), request.expectedVersion().longValue());

    return UserProfileResponse.from(updated);
  }

  @PatchMapping("/{userId}/account-status")
  public UserProfileResponse updateAccountStatus(
      @PathVariable("userId") @Positive(message = "User profile id must be positive") long userId,
      @Valid @RequestBody UpdateAccountStatusRequest request) {
    UserProfile updated =
        service.updateAccountStatus(
            userId, request.accountStatus(), request.expectedVersion().longValue());

    return UserProfileResponse.from(updated);
  }

  @PatchMapping("/{userId}/booking-eligibility")
  public UserProfileResponse updateBookingEligibility(
      @PathVariable("userId") @Positive(message = "User profile id must be positive") long userId,
      @Valid @RequestBody UpdateBookingEligibilityRequest request) {
    UserProfile updated =
        service.updateBookingEligibility(
            userId, request.bookingEligibility(), request.expectedVersion().longValue());

    return UserProfileResponse.from(updated);
  }
}
