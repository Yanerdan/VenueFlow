package com.yanerdan.venueflow.user.profile.web;

import com.yanerdan.venueflow.user.profile.application.UserBookingEligibilityView;
import com.yanerdan.venueflow.user.profile.application.UserProfileApplicationService;
import com.yanerdan.venueflow.user.profile.application.UserDirectoryAccessDeniedException;
import com.yanerdan.venueflow.user.profile.domain.UserProfile;
import com.yanerdan.venueflow.user.profile.web.dto.BookingEligibilityResponse;
import com.yanerdan.venueflow.user.profile.web.dto.CreateUserProfileRequest;
import com.yanerdan.venueflow.user.profile.web.dto.UpdateAccountStatusRequest;
import com.yanerdan.venueflow.user.profile.web.dto.UpdateBookingEligibilityRequest;
import com.yanerdan.venueflow.user.profile.web.dto.UpdateDisplayNameRequest;
import com.yanerdan.venueflow.user.profile.web.dto.UpdateCampusProfileRequest;
import com.yanerdan.venueflow.user.profile.web.dto.UserProfilePageResponse;
import com.yanerdan.venueflow.user.profile.web.dto.UserProfileResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    boolean campusFieldsAbsent =
        request.campusId() == null
            && request.identityType() == null
            && request.department() == null
            && request.phone() == null
            && request.email() == null;
    UserProfile created =
        campusFieldsAbsent
            ? service.create(request.externalUserId(), request.displayName())
            : service.create(
                request.externalUserId(),
                request.displayName(),
                request.campusId(),
                request.identityType(),
                request.department(),
                request.phone(),
                request.email());

    UserProfileResponse response = UserProfileResponse.from(created);

    URI location = URI.create("/api/v1/users/" + response.id());

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping("/{userId}")
  public UserProfileResponse getById(
      @PathVariable("userId") @Positive(message = "User profile id must be positive") long userId) {
    return UserProfileResponse.from(service.getById(userId));
  }

  @GetMapping("/me")
  public UserProfileResponse getCurrent(
      @RequestHeader("X-User-Id")
          @NotBlank
          @Size(max = 128, message = "External user id must not exceed 128 characters")
          String externalUserId) {
    return UserProfileResponse.from(service.getByExternalUserId(externalUserId));
  }

  @PatchMapping("/me/campus-profile")
  public UserProfileResponse updateCurrentCampusProfile(
      @RequestHeader("X-User-Id")
          @NotBlank
          @Size(max = 128)
          String externalUserId,
      @Valid @RequestBody UpdateCampusProfileRequest request) {
    return UserProfileResponse.from(
        service.updateCampusProfile(
            externalUserId,
            request.displayName(),
            request.campusId(),
            request.identityType(),
            request.department(),
            request.phone(),
            request.email(),
            request.expectedVersion()));
  }

  @GetMapping("/management")
  public UserProfilePageResponse managementDirectory(
      @RequestHeader(value = "X-Role", defaultValue = "APPLICANT") String role,
      @RequestParam(required = false) @Size(max = 80) String keyword,
      @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int pageSize) {
    if (!role.equals("APPROVER")
        && !role.equals("RESOURCE_MANAGER")
        && !role.equals("SYSTEM_ADMIN")) {
      throw new UserDirectoryAccessDeniedException();
    }
    return UserProfilePageResponse.from(service.findDirectory(keyword, pageNumber, pageSize));
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
