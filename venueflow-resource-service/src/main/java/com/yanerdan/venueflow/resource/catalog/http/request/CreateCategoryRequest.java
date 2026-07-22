package com.yanerdan.venueflow.resource.catalog.http.request;

import com.yanerdan.venueflow.resource.catalog.application.CreateCategoryCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
    @NotBlank(message = "code must not be blank")
        @Size(max = 64, message = "code must not exceed 64 characters")
        String code,
    @NotBlank(message = "name must not be blank")
        @Size(max = 128, message = "name must not exceed 128 characters")
        String name) {

  public CreateCategoryCommand toCommand() {
    return new CreateCategoryCommand(code, name);
  }
}
