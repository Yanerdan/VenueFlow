package com.yanerdan.venueflow.resource.slot.allocation.http.request;

import com.yanerdan.venueflow.resource.slot.allocation.application.SlotAllocationOperationPageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SlotAllocationOperationPageRequest(
    @Min(value = 0, message = "page must not be negative") Integer page,
    @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must not exceed 100")
        Integer size) {

  public SlotAllocationOperationPageQuery toQuery(Long slotId) {
    return SlotAllocationOperationPageQuery.of(slotId, page, size);
  }
}
