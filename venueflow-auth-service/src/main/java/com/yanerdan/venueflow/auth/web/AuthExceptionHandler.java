package com.yanerdan.venueflow.auth.web;

import com.yanerdan.venueflow.auth.application.AuthException;
import com.yanerdan.venueflow.auth.web.AuthDtos.ErrorResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Profile("persistence")
public class AuthExceptionHandler {

  @ExceptionHandler(AuthException.class)
  ResponseEntity<ErrorResponse> auth(AuthException exception) {
    HttpStatus status =
        switch (exception.code()) {
          case AUTH_USERNAME_EXISTS -> HttpStatus.CONFLICT;
          case AUTH_INVALID_CREDENTIALS, AUTH_INVALID_REFRESH_TOKEN -> HttpStatus.UNAUTHORIZED;
          case AUTH_FORBIDDEN -> HttpStatus.FORBIDDEN;
          case AUTH_ACCOUNT_NOT_FOUND -> HttpStatus.NOT_FOUND;
          case AUTH_ROLE_CONFLICT -> HttpStatus.CONFLICT;
          case AUTH_PERSISTENCE_FAILURE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    return error(status, exception.code().name(), exception.getMessage());
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
  ResponseEntity<ErrorResponse> validation(Exception exception) {
    return error(HttpStatus.BAD_REQUEST, "AUTH_VALIDATION_FAILED", "Request is invalid");
  }

  private static ResponseEntity<ErrorResponse> error(
      HttpStatus status, String code, String message) {
    return ResponseEntity.status(status)
        .body(new ErrorResponse(code, message, List.of(), AuthController.traceId(), Instant.now()));
  }
}
