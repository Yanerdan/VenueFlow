package com.yanerdan.venueflow.resource.catalog.application;

public record CreateCategoryCommand(String code, String name) {

  private static final int MAX_CODE_LENGTH = 64;
  private static final int MAX_NAME_LENGTH = 128;

  public CreateCategoryCommand {
    code = normalizeRequired(code, "code", MAX_CODE_LENGTH);
    name = normalizeRequired(name, "name", MAX_NAME_LENGTH);
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
}
