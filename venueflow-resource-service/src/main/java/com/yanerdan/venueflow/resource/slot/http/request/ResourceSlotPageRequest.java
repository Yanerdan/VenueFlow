package com.yanerdan.venueflow.resource.slot.http.request;

import com.yanerdan.venueflow.resource.slot.application.ResourceSlotPageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record ResourceSlotPageRequest(
    @NotNull(message = "from must not be null") OffsetDateTime from,
    @NotNull(message = "to must not be null") OffsetDateTime to,
    @Min(value = 0, message = "page must not be negative") Integer page,
    @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must not exceed 100")
        Integer size) {

  public ResourceSlotPageQuery toQuery(Long resourceId) {
    return ResourceSlotPageQuery.of(resourceId, from.toInstant(), to.toInstant(), page, size);
  }
}
