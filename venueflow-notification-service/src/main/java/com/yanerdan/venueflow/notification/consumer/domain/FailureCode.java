package com.yanerdan.venueflow.notification.consumer.domain;

public enum FailureCode {
  MESSAGE_TOO_LARGE(true),
  INVALID_CONTENT_TYPE(true),
  MALFORMED_ENVELOPE(true),
  UNSUPPORTED_EVENT(true),
  IDENTITY_COLLISION(true),
  PROCESSING_FAILED(false),
  RETRY_EXHAUSTED(true),
  TRANSFER_UNCERTAIN(false);

  private final boolean terminal;

  FailureCode(boolean terminal) {
    this.terminal = terminal;
  }

  public boolean terminal() {
    return terminal;
  }
}
