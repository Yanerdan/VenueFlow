package com.yanerdan.venueflow.booking.domain;

public record BookingApplicationDetails(
    String activityTitle, String purpose, String contactName, String contactPhone, String note) {

  public static final int MAX_ACTIVITY_TITLE = 160;
  public static final int MAX_PURPOSE = 500;
  public static final int MAX_CONTACT_NAME = 120;
  public static final int MAX_CONTACT_PHONE = 32;
  public static final int MAX_NOTE = 1000;

  public BookingApplicationDetails {
    if (activityTitle != null
        || purpose != null
        || contactName != null
        || contactPhone != null
        || note != null) {
      activityTitle = normalizeRequired(activityTitle, "Activity title", MAX_ACTIVITY_TITLE);
      purpose = normalizeRequired(purpose, "Application purpose", MAX_PURPOSE);
      contactName = normalizeRequired(contactName, "Contact name", MAX_CONTACT_NAME);
      contactPhone = normalizeRequired(contactPhone, "Contact phone", MAX_CONTACT_PHONE);
      note = normalizeOptional(note, MAX_NOTE);
    }
  }

  public static BookingApplicationDetails historical() {
    return new BookingApplicationDetails(null, null, null, null, null);
  }

  private static String normalizeRequired(String value, String name, int maximum) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    String normalized = value.trim();
    if (normalized.length() > maximum) {
      throw new IllegalArgumentException(name + " is too long");
    }
    return normalized;
  }

  private static String normalizeOptional(String value, int maximum) {
    if (value == null || value.isBlank()) return null;
    String normalized = value.trim();
    if (normalized.length() > maximum) {
      throw new IllegalArgumentException("Application note is too long");
    }
    return normalized;
  }
}
