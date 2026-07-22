package com.yanerdan.venueflow.resource.catalog.http.request;

import com.yanerdan.venueflow.resource.catalog.application.CreateResourceCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateResourceRequest(
    @NotBlank(message = "resourceNo must not be blank")
        @Size(max = 64, message = "resourceNo must not exceed 64 characters")
        String resourceNo,
    @NotNull(message = "categoryId must not be null")
        @Positive(message = "categoryId must be positive")
        Long categoryId,
    @NotBlank(message = "name must not be blank")
        @Size(max = 128, message = "name must not exceed 128 characters")
        String name,
    @Size(max = 1000, message = "description must not exceed 1000 characters") String description,
    @Size(max = 255, message = "location must not exceed 255 characters") String location,
    @NotNull(message = "capacity must not be null") @Positive(message = "capacity must be positive")
        Integer capacity) {

  public CreateResourceCommand toCommand() {
    return new CreateResourceCommand(resourceNo, categoryId, name, description, location, capacity);
  }
}
