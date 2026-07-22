package com.yanerdan.venueflow.resource.catalog.http.error;

import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.CATALOG_PERSISTENCE_ERROR;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.INTERNAL_ERROR;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.VALIDATION_ERROR;

import com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode;
import com.yanerdan.venueflow.resource.catalog.error.CatalogException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Profile("persistence")
public class CatalogExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogExceptionHandler.class);

  @ExceptionHandler(CatalogException.class)
  public ResponseEntity<CatalogErrorResponse> handleCatalogException(CatalogException exception) {
    HttpStatus status = statusFor(exception.getCode());

    Map<String, Object> details = safeCatalogDetails(exception);

    if (status.is5xxServerError()) {
      return logAndBuildServerError(status, exception.getCode(), exception.getMessage(), exception);
    }

    return buildResponse(status, exception.getCode(), exception.getMessage(), details);
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<CatalogErrorResponse> handleBindingException(BindException exception) {
    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      if (fieldError.isBindingFailure()) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            VALIDATION_ERROR,
            "Request parameter has an unsupported value",
            Map.of(
                "parameter",
                fieldError.getField(),
                "rejectedValue",
                String.valueOf(fieldError.getRejectedValue())));
      }
    }

    return buildResponse(
        HttpStatus.BAD_REQUEST,
        VALIDATION_ERROR,
        "Request validation failed",
        validationDetails(exception));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<CatalogErrorResponse> handleConstraintViolation(
      ConstraintViolationException exception) {
    Map<String, String> fields = new TreeMap<>();

    for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
      fields.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage());
    }

    Map<String, Object> details =
        fields.isEmpty() ? Map.of() : Map.of("fields", Map.copyOf(fields));

    return buildResponse(
        HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "Request validation failed", details);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<CatalogErrorResponse> handleMethodValidation(
      HandlerMethodValidationException exception) {
    return buildResponse(
        HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "Request validation failed", Map.of());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<CatalogErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception) {
    return buildResponse(
        HttpStatus.BAD_REQUEST,
        VALIDATION_ERROR,
        "Request parameter has an unsupported value",
        Map.of(
            "parameter",
            exception.getName(),
            "rejectedValue",
            String.valueOf(exception.getValue())));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<CatalogErrorResponse> handleUnreadableMessage(
      HttpMessageNotReadableException exception) {
    return buildResponse(
        HttpStatus.BAD_REQUEST,
        VALIDATION_ERROR,
        "Request body is malformed or contains an unsupported value",
        Map.of());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<CatalogErrorResponse> handleIllegalArgument(
      IllegalArgumentException exception) {
    return buildResponse(
        HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "Request validation failed", Map.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<CatalogErrorResponse> handleUnexpectedException(Exception exception) {
    return logAndBuildServerError(
        HttpStatus.INTERNAL_SERVER_ERROR,
        INTERNAL_ERROR,
        "An unexpected error occurred",
        exception);
  }

  private static Map<String, Object> validationDetails(BindException exception) {
    Map<String, String> fields = new TreeMap<>();

    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      String message =
          fieldError.getDefaultMessage() == null ? "invalid value" : fieldError.getDefaultMessage();

      fields.putIfAbsent(fieldError.getField(), message);
    }

    if (fields.isEmpty()) {
      return Map.of();
    }

    return Map.of("fields", Map.copyOf(fields));
  }

  private static Map<String, Object> safeCatalogDetails(CatalogException exception) {
    if (exception.getCode() == CATALOG_PERSISTENCE_ERROR) {
      return Map.of();
    }

    return new LinkedHashMap<>(exception.getDetails());
  }

  private ResponseEntity<CatalogErrorResponse> logAndBuildServerError(
      HttpStatus status, CatalogErrorCode code, String message, Exception exception) {
    String traceId = currentTraceId();

    LOGGER.error("Catalog request failed. traceId={}, code={}", traceId, code, exception);

    return buildResponse(status, code, message, Map.of(), traceId);
  }

  private static ResponseEntity<CatalogErrorResponse> buildResponse(
      HttpStatus status, CatalogErrorCode code, String message, Map<String, Object> details) {
    return buildResponse(status, code, message, details, currentTraceId());
  }

  private static ResponseEntity<CatalogErrorResponse> buildResponse(
      HttpStatus status,
      CatalogErrorCode code,
      String message,
      Map<String, Object> details,
      String traceId) {
    CatalogErrorResponse response =
        new CatalogErrorResponse(code.name(), message, details, traceId, Instant.now());

    return ResponseEntity.status(status).body(response);
  }

  private static String currentTraceId() {
    String traceId = MDC.get(CatalogTraceIdFilter.MDC_KEY);

    if (traceId == null || traceId.isBlank()) {
      return UUID.randomUUID().toString();
    }

    return traceId;
  }

  private static HttpStatus statusFor(CatalogErrorCode code) {
    return switch (code) {
      case VALIDATION_ERROR, INVALID_SLOT_TIME_RANGE -> HttpStatus.BAD_REQUEST;

      case CATEGORY_NOT_FOUND, RESOURCE_NOT_FOUND, RESOURCE_SLOT_NOT_FOUND -> HttpStatus.NOT_FOUND;

      case CATEGORY_ALREADY_EXISTS,
          RESOURCE_NUMBER_ALREADY_EXISTS,
          RESOURCE_NOT_ACTIVE_FOR_SLOT,
          RESOURCE_SLOT_TIME_OVERLAP,
          INVALID_RESOURCE_STATUS_TRANSITION,
          INVALID_RESOURCE_SLOT_STATUS_TRANSITION,
          OPTIMISTIC_LOCK_CONFLICT ->
          HttpStatus.CONFLICT;

      case RESOURCE_SLOT_NOT_OPEN_FOR_ALLOCATION,
          INSUFFICIENT_SLOT_CAPACITY,
          RELEASE_EXCEEDS_OCCUPIED_CAPACITY,
          ALLOCATION_OPERATION_CONFLICT ->
          HttpStatus.CONFLICT;

      case CATALOG_PERSISTENCE_ERROR, INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
