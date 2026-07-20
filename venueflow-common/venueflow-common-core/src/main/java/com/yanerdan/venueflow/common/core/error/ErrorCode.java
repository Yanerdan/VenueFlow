package com.yanerdan.venueflow.common.core.error;

import java.util.Objects;
import java.util.regex.Pattern;

/** A stable, transport-neutral error identifier such as {@code AUTH_INVALID_CREDENTIALS}. */
public record ErrorCode(String value) {

  private static final Pattern FORMAT = Pattern.compile("[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+");

  public ErrorCode {
    Objects.requireNonNull(value, "value must not be null");
    if (!FORMAT.matcher(value).matches()) {
      throw new IllegalArgumentException(
          "value must contain at least a domain and reason in UPPER_SNAKE_CASE");
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
