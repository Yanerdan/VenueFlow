package com.yanerdan.venueflow.user.profile.domain;

public final class DuplicateExternalUserIdException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public DuplicateExternalUserIdException(Throwable cause) {
    super("A user profile already exists for the external user identifier", cause);
  }
}
