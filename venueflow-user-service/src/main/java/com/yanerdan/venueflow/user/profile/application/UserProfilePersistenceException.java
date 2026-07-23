package com.yanerdan.venueflow.user.profile.application;

public final class UserProfilePersistenceException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public UserProfilePersistenceException(String message) {
    super(message);
  }

  public UserProfilePersistenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
