package com.yanerdan.venueflow.resource.catalog.http.request;

import com.yanerdan.venueflow.resource.catalog.application.ChangeResourceBookingRulesCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChangeResourceBookingRulesRequest(
    @Size(max = 1000) String bookingNotice,
    @NotNull @Min(0) @Max(720) Integer minAdvanceHours,
    @NotNull @Min(1) @Max(365) Integer maxAdvanceDays,
    @NotNull @Min(15) @Max(1440) Integer maxDurationMinutes,
    @NotNull @Positive Long expectedVersion) {

  public ChangeResourceBookingRulesCommand toCommand(Long resourceId) {
    return new ChangeResourceBookingRulesCommand(
        resourceId,
        bookingNotice,
        minAdvanceHours,
        maxAdvanceDays,
        maxDurationMinutes,
        expectedVersion);
  }
}
