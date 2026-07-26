package com.yanerdan.venueflow.booking.web;

import com.yanerdan.venueflow.booking.application.BookingErrorCode;
import com.yanerdan.venueflow.booking.application.BookingException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
@Profile("persistence")
public class BookingExceptionHandler {
  @ExceptionHandler(BookingException.class)
  ResponseEntity<BookingErrorResponse> handleBooking(BookingException exception) {
    return response(status(exception.getCode()), exception.getCode(), exception.getMessage());
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    MissingRequestHeaderException.class,
    HandlerMethodValidationException.class,
    ConstraintViolationException.class,
    IllegalArgumentException.class
  })
  ResponseEntity<BookingErrorResponse> handleValidation(Exception exception) {
    return response(
        HttpStatus.BAD_REQUEST,
        BookingErrorCode.BOOKING_VALIDATION_FAILED,
        "Request validation failed");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<BookingErrorResponse> handleUnexpected(Exception exception) {
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        BookingErrorCode.BOOKING_PERSISTENCE_FAILED,
        "Booking operation failed");
  }

  private static ResponseEntity<BookingErrorResponse> response(
      HttpStatus status, BookingErrorCode code, String message) {
    return ResponseEntity.status(status)
        .body(
            new BookingErrorResponse(
                code.name(), message, Map.of(), UUID.randomUUID().toString(), Instant.now()));
  }

  private static HttpStatus status(BookingErrorCode code) {
    return switch (code) {
      case BOOKING_VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
      case BOOKING_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case BOOKING_IDEMPOTENCY_CONFLICT,
          BOOKING_REQUEST_IN_PROGRESS,
          BOOKING_USER_NOT_ELIGIBLE,
          BOOKING_CAPACITY_UNAVAILABLE,
          BOOKING_CONFIRMATION_DEADLINE_EXPIRED,
          BOOKING_TIMEOUT_IN_PROGRESS,
          BOOKING_CHECK_IN_WINDOW_INVALID,
          BOOKING_STATE_CONFLICT ->
          HttpStatus.CONFLICT;
      case BOOKING_DOWNSTREAM_UNAVAILABLE,
          BOOKING_ALLOCATION_OUTCOME_UNKNOWN,
          BOOKING_RESOURCE_CONTRACT_INVALID ->
          HttpStatus.SERVICE_UNAVAILABLE;
      case BOOKING_PERSISTENCE_FAILED, BOOKING_COMPENSATION_REQUIRED ->
          HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
