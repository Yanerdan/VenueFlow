package com.yanerdan.venueflow.user.profile.web.dto;

import com.yanerdan.venueflow.user.profile.domain.UserProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateDisplayNameRequest(
    @NotBlank(message = "Display name must not be blank")
        @Size(
            max = UserProfile.MAX_DISPLAY_NAME_LENGTH,
            message =
                "Display name must not exceed "
                    + UserProfile.MAX_DISPLAY_NAME_LENGTH
                    + " characters")
        String displayName,
    @NotNull(message = "Expected version is required")
        @PositiveOrZero(message = "Expected version must not be negative")
        Long expectedVersion) {}
