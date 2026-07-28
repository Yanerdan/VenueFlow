package com.yanerdan.venueflow.resource.catalog.application;

public record ChangeResourceBookingRulesCommand(
    Long resourceId,
    String bookingNotice,
    Integer minAdvanceHours,
    Integer maxAdvanceDays,
    Integer maxDurationMinutes,
    Long expectedVersion) {

  public ChangeResourceBookingRulesCommand {
    if (resourceId == null || resourceId <= 0) {
      throw new IllegalArgumentException("resourceId must be positive");
    }
    bookingNotice = normalizeNotice(bookingNotice);
    requireRange(minAdvanceHours, 0, 720, "minAdvanceHours");
    requireRange(maxAdvanceDays, 1, 365, "maxAdvanceDays");
    requireRange(maxDurationMinutes, 15, 1440, "maxDurationMinutes");
    if (expectedVersion == null || expectedVersion <= 0) {
      throw new IllegalArgumentException("expectedVersion must be positive");
    }
  }

  private static String normalizeNotice(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.strip();
    if (normalized.length() > 1000) {
      throw new IllegalArgumentException("bookingNotice must not exceed 1000 characters");
    }
    return normalized;
  }

  private static void requireRange(Integer value, int minimum, int maximum, String field) {
    if (value == null || value < minimum || value > maximum) {
      throw new IllegalArgumentException(field + " must be between " + minimum + " and " + maximum);
    }
  }
}
