package com.yanerdan.venueflow.search.web;

import com.yanerdan.venueflow.search.application.SearchUnavailableException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Profile("search")
public final class SearchExceptionHandler {

  @ExceptionHandler(SearchUnavailableException.class)
  ResponseEntity<Map<String, Object>> unavailable() {
    return error(HttpStatus.SERVICE_UNAVAILABLE, "SEARCH_UNAVAILABLE", "Search is unavailable");
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    ConstraintViolationException.class,
    MethodArgumentNotValidException.class
  })
  ResponseEntity<Map<String, Object>> invalid() {
    return error(HttpStatus.BAD_REQUEST, "SEARCH_INVALID_REQUEST", "Search request is invalid");
  }

  private static ResponseEntity<Map<String, Object>> error(
      HttpStatus status, String code, String message) {
    String traceId = MDC.get("traceId");
    if (traceId == null) {
      traceId = UUID.randomUUID().toString();
    }
    return ResponseEntity.status(status)
        .body(
            Map.of(
                "code", code,
                "message", message,
                "traceId", traceId,
                "timestamp", Instant.now().toString()));
  }
}
