package com.yanerdan.venueflow.user.profile.application;

public final class StaleUserProfileVersionException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final long userProfileId;
  private final long expectedVersion;

  public StaleUserProfileVersionException(long userProfileId, long expectedVersion) {
    super("User profile version is stale");
    this.userProfileId = userProfileId;
    this.expectedVersion = expectedVersion;
  }

  public long userProfileId() {
    return userProfileId;
  }

  public long expectedVersion() {
    return expectedVersion;
  }
}
