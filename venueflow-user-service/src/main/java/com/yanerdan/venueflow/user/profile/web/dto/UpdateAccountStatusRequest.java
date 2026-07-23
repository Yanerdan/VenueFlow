package com.yanerdan.venueflow.user.profile.web.dto;

import com.yanerdan.venueflow.user.profile.domain.AccountStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateAccountStatusRequest(
    @NotNull(message = "Account status is required") AccountStatus accountStatus,
    @NotNull(message = "Expected version is required")
        @PositiveOrZero(message = "Expected version must not be negative")
        Long expectedVersion) {}
