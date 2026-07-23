package com.yanerdan.venueflow.notification.consumer.domain;

public final class IdentityCollisionException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public IdentityCollisionException() {
    super(FailureCode.IDENTITY_COLLISION.name());
  }
}
