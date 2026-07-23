package com.yanerdan.venueflow.user.profile.web.dto;

import com.yanerdan.venueflow.user.profile.domain.ExternalUserId;
import com.yanerdan.venueflow.user.profile.domain.UserProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserProfileRequest(
    @NotBlank(message = "External user id must not be blank")
        @Size(
            max = ExternalUserId.MAX_LENGTH,
            message =
                "External user id must not exceed " + ExternalUserId.MAX_LENGTH + " characters")
        String externalUserId,
    @NotBlank(message = "Display name must not be blank")
        @Size(
            max = UserProfile.MAX_DISPLAY_NAME_LENGTH,
            message =
                "Display name must not exceed "
                    + UserProfile.MAX_DISPLAY_NAME_LENGTH
                    + " characters")
        String displayName) {}
