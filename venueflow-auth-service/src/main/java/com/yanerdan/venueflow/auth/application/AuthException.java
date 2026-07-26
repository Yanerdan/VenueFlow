package com.yanerdan.venueflow.auth.application;

public class AuthException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final AuthErrorCode code;

  public AuthException(AuthErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public AuthException(AuthErrorCode code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public AuthErrorCode code() {
    return code;
  }
}
