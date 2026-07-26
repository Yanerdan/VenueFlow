package com.yanerdan.venueflow.search.application;

import java.util.Locale;

public record ResourceSearchQuery(String text, Long categoryId, String status, int page, int size) {

  public ResourceSearchQuery {
    text = text == null ? null : text.trim();
    status = status == null ? null : status.trim().toUpperCase(Locale.ROOT);
    if (text != null && text.length() > 100) {
      throw new IllegalArgumentException("text must be at most 100 characters");
    }
    if (categoryId != null && categoryId <= 0) {
      throw new IllegalArgumentException("categoryId must be positive");
    }
    if (status != null && !status.matches("DRAFT|ACTIVE|INACTIVE|MAINTENANCE|ARCHIVED")) {
      throw new IllegalArgumentException("status is unsupported");
    }
    if (page < 0 || page > 1000) {
      throw new IllegalArgumentException("page must be between 0 and 1000");
    }
    if (size < 1 || size > 100) {
      throw new IllegalArgumentException("size must be between 1 and 100");
    }
    if ((long) page * size + size > 10_000) {
      throw new IllegalArgumentException("search result window must not exceed 10000");
    }
  }
}
