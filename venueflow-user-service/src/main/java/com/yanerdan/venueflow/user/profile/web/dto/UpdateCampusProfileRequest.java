package com.yanerdan.venueflow.user.profile.web.dto;

import com.yanerdan.venueflow.user.profile.domain.CampusIdentityType;
import com.yanerdan.venueflow.user.profile.domain.UserProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateCampusProfileRequest(
    @NotBlank @Size(max = UserProfile.MAX_DISPLAY_NAME_LENGTH) String displayName,
    @Size(max = UserProfile.MAX_CAMPUS_ID_LENGTH) String campusId,
    @NotNull CampusIdentityType identityType,
    @Size(max = UserProfile.MAX_DEPARTMENT_LENGTH) String department,
    @Size(max = UserProfile.MAX_PHONE_LENGTH) String phone,
    @Email @Size(max = UserProfile.MAX_EMAIL_LENGTH) String email,
    @NotNull @PositiveOrZero Long expectedVersion) {}
