package com.yanerdan.venueflow.user.profile.application;

public class UserDirectoryAccessDeniedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public UserDirectoryAccessDeniedException() {
    super("Current role cannot access the user directory");
  }
}
