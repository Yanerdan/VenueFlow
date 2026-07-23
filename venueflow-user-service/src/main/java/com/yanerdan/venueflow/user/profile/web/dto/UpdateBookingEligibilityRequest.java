package com.yanerdan.venueflow.user.profile.web.dto;

import com.yanerdan.venueflow.user.profile.domain.BookingEligibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateBookingEligibilityRequest(
    @NotNull(message = "Booking eligibility is required") BookingEligibility bookingEligibility,
    @NotNull(message = "Expected version is required")
        @PositiveOrZero(message = "Expected version must not be negative")
        Long expectedVersion) {}
