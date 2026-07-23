package com.yanerdan.venueflow.user.profile.domain;

public record UserProfileId(long value) {

  public UserProfileId {
    if (value <= 0) {
      throw new IllegalArgumentException("User profile id must be positive");
    }
  }
}
