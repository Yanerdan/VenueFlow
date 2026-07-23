package com.yanerdan.venueflow.booking.application;

public final class BookingException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final BookingErrorCode code;

  public BookingException(BookingErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public BookingException(BookingErrorCode code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public BookingErrorCode getCode() {
    return code;
  }
}
