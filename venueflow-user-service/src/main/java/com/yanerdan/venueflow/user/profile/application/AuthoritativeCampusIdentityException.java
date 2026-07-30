package com.yanerdan.venueflow.user.profile.application;

public class AuthoritativeCampusIdentityException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public AuthoritativeCampusIdentityException() {
    super("Directory-owned campus identity cannot be changed by self-service");
  }
}
