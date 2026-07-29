package com.yanerdan.venueflow.resource.catalog.application;

public record ChangeResourceFactsCommand(
    Long resourceId,
    Long categoryId,
    String name,
    String description,
    String location,
    Integer capacity,
    Long expectedVersion) {

  public ChangeResourceFactsCommand {
    resourceId = positive(resourceId, "resourceId");
    categoryId = positive(categoryId, "categoryId");
    name = required(name, "name", 128);
    description = optional(description, "description", 1000);
    location = required(location, "location", 255);
    capacity = positive(capacity, "capacity");
    expectedVersion = positive(expectedVersion, "expectedVersion");
  }

  private static String required(String value, String field, int maximum) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    String normalized = value.strip();
    if (normalized.length() > maximum) {
      throw new IllegalArgumentException(field + " must not exceed " + maximum + " characters");
    }
    return normalized;
  }

  private static String optional(String value, String field, int maximum) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.strip();
    if (normalized.length() > maximum) {
      throw new IllegalArgumentException(field + " must not exceed " + maximum + " characters");
    }
    return normalized;
  }

  private static Long positive(Long value, String field) {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException(field + " must be positive");
    }
    return value;
  }

  private static Integer positive(Integer value, String field) {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException(field + " must be positive");
    }
    return value;
  }
}
