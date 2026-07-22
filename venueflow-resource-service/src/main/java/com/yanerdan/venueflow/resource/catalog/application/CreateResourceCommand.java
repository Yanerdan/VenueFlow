package com.yanerdan.venueflow.resource.catalog.application;

public record CreateResourceCommand(
    String resourceNo,
    Long categoryId,
    String name,
    String description,
    String location,
    Integer capacity) {

  private static final int MAX_RESOURCE_NO_LENGTH = 64;
  private static final int MAX_NAME_LENGTH = 128;
  private static final int MAX_DESCRIPTION_LENGTH = 1000;
  private static final int MAX_LOCATION_LENGTH = 255;

  public CreateResourceCommand {
    resourceNo = normalizeRequired(resourceNo, "resourceNo", MAX_RESOURCE_NO_LENGTH);

    categoryId = requirePositive(categoryId, "categoryId");

    name = normalizeRequired(name, "name", MAX_NAME_LENGTH);

    description = normalizeOptional(description, "description", MAX_DESCRIPTION_LENGTH);

    location = normalizeOptional(location, "location", MAX_LOCATION_LENGTH);

    capacity = requirePositive(capacity, "capacity");
  }

  private static String normalizeRequired(String value, String fieldName, int maximumLength) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + " must not be null");
    }

    String normalized = value.strip();

    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    if (normalized.length() > maximumLength) {
      throw new IllegalArgumentException(
          fieldName + " must not exceed " + maximumLength + " characters");
    }

    return normalized;
  }

  private static String normalizeOptional(String value, String fieldName, int maximumLength) {
    if (value == null) {
      return null;
    }

    String normalized = value.strip();

    if (normalized.isEmpty()) {
      return null;
    }

    if (normalized.length() > maximumLength) {
      throw new IllegalArgumentException(
          fieldName + " must not exceed " + maximumLength + " characters");
    }

    return normalized;
  }

  private static Long requirePositive(Long value, String fieldName) {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException(fieldName + " must be positive");
    }

    return value;
  }

  private static Integer requirePositive(Integer value, String fieldName) {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException(fieldName + " must be positive");
    }

    return value;
  }
}
