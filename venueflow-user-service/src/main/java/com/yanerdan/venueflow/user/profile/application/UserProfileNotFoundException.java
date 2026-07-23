package com.yanerdan.venueflow.user.profile.application;

public final class UserProfileNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final long userProfileId;

  public UserProfileNotFoundException(long userProfileId) {
    super("User profile was not found");
    this.userProfileId = userProfileId;
  }

  public long userProfileId() {
    return userProfileId;
  }
}
