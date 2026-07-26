package com.yanerdan.venueflow.auth.domain;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

  private static final Pattern USERNAME = Pattern.compile("[a-z0-9][a-z0-9._-]{2,63}");

  public String normalizeUsername(String username) {
    String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    if (!USERNAME.matcher(normalized).matches()) {
      throw new IllegalArgumentException("username");
    }
    return normalized;
  }

  public void validatePassword(char[] password) {
    if (password == null || password.length < 12 || password.length > 72) {
      throw new IllegalArgumentException("password");
    }
    boolean lower = false;
    boolean upper = false;
    boolean digit = false;
    for (char character : password) {
      lower |= Character.isLowerCase(character);
      upper |= Character.isUpperCase(character);
      digit |= Character.isDigit(character);
    }
    if (!lower || !upper || !digit) {
      throw new IllegalArgumentException("password");
    }
  }
}
