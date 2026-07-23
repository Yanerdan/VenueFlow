package com.yanerdan.venueflow.notification.consumer.domain;

public final class EnvelopeException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final FailureCode failureCode;

  public EnvelopeException(FailureCode failureCode) {
    super(failureCode.name());
    this.failureCode = failureCode;
  }

  public FailureCode failureCode() {
    return failureCode;
  }
}
