package com.yanerdan.venueflow.user.profile.domain;

import java.util.Objects;

public record ExternalUserId(String value) {

  public static final int MAX_LENGTH = 128;

  public ExternalUserId {
    Objects.requireNonNull(value, "External user id must not be null");

    if (value.isBlank()) {
      throw new IllegalArgumentException("External user id must not be blank");
    }

    if (!value.equals(value.strip())) {
      throw new IllegalArgumentException(
          "External user id must not contain surrounding whitespace");
    }

    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException(
          "External user id must not exceed " + MAX_LENGTH + " characters");
    }
  }
}
