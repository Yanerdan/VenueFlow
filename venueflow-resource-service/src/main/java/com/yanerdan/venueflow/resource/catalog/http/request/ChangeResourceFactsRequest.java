package com.yanerdan.venueflow.resource.catalog.http.request;

import com.yanerdan.venueflow.resource.catalog.application.ChangeResourceFactsCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChangeResourceFactsRequest(
    @NotNull(message = "categoryId must not be null")
        @Positive(message = "categoryId must be positive")
        Long categoryId,
    @NotBlank(message = "name must not be blank")
        @Size(max = 128, message = "name must not exceed 128 characters")
        String name,
    @Size(max = 1000, message = "description must not exceed 1000 characters") String description,
    @NotBlank(message = "location must not be blank")
        @Size(max = 255, message = "location must not exceed 255 characters")
        String location,
    @NotNull(message = "capacity must not be null") @Positive(message = "capacity must be positive")
        Integer capacity,
    @NotNull(message = "expectedVersion must not be null")
        @Positive(message = "expectedVersion must be positive")
        Long expectedVersion) {

  public ChangeResourceFactsCommand toCommand(Long resourceId) {
    return new ChangeResourceFactsCommand(
        resourceId, categoryId, name, description, location, capacity, expectedVersion);
  }
}
