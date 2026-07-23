package com.yanerdan.venueflow.user.profile.web.error;

import com.yanerdan.venueflow.user.profile.application.StaleUserProfileVersionException;
import com.yanerdan.venueflow.user.profile.application.UserProfileNotFoundException;
import com.yanerdan.venueflow.user.profile.application.UserProfilePersistenceException;
import com.yanerdan.venueflow.user.profile.domain.DuplicateExternalUserIdException;
import com.yanerdan.venueflow.user.profile.web.UserProfileController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackageClasses = UserProfileController.class)
@Profile("persistence")
public class UserProfileExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileExceptionHandler.class);

  private static final String TRACE_ID_ATTRIBUTE =
      UserProfileExceptionHandler.class.getName() + ".traceId";

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<UserProfileErrorResponse> handleRequestBodyValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    Map<String, String> fieldErrors = new TreeMap<>();

    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      String message = fieldError.getDefaultMessage();

      fieldErrors.putIfAbsent(fieldError.getField(), message == null ? "Invalid value" : message);
    }

    return response(
        HttpStatus.BAD_REQUEST,
        UserProfileErrorCode.USER_PROFILE_INVALID_REQUEST,
        "Request validation failed",
        Map.of("fields", fieldErrors),
        request);
  }

  @ExceptionHandler({
    HandlerMethodValidationException.class,
    ConstraintViolationException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<UserProfileErrorResponse> handleMethodValidation(
      Exception exception, HttpServletRequest request) {
    return response(
        HttpStatus.BAD_REQUEST,
        UserProfileErrorCode.USER_PROFILE_INVALID_REQUEST,
        "Request validation failed",
        Map.of(),
        request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<UserProfileErrorResponse> handleUnreadableRequest(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return response(
        HttpStatus.BAD_REQUEST,
        UserProfileErrorCode.USER_PROFILE_INVALID_REQUEST,
        "Request body is malformed",
        Map.of(),
        request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<UserProfileErrorResponse> handleIllegalArgument(
      IllegalArgumentException exception, HttpServletRequest request) {
    return response(
        HttpStatus.BAD_REQUEST,
        UserProfileErrorCode.USER_PROFILE_INVALID_REQUEST,
        "Request validation failed",
        Map.of(),
        request);
  }

  @ExceptionHandler(UserProfileNotFoundException.class)
  public ResponseEntity<UserProfileErrorResponse> handleNotFound(
      UserProfileNotFoundException exception, HttpServletRequest request) {
    return response(
        HttpStatus.NOT_FOUND,
        UserProfileErrorCode.USER_PROFILE_NOT_FOUND,
        "User profile was not found",
        Map.of("userId", exception.userProfileId()),
        request);
  }

  @ExceptionHandler(DuplicateExternalUserIdException.class)
  public ResponseEntity<UserProfileErrorResponse> handleDuplicateExternalIdentifier(
      DuplicateExternalUserIdException exception, HttpServletRequest request) {
    return response(
        HttpStatus.CONFLICT,
        UserProfileErrorCode.USER_PROFILE_EXTERNAL_ID_CONFLICT,
        "External user identifier is already in use",
        Map.of(),
        request);
  }

  @ExceptionHandler(StaleUserProfileVersionException.class)
  public ResponseEntity<UserProfileErrorResponse> handleStaleVersion(
      StaleUserProfileVersionException exception, HttpServletRequest request) {
    Map<String, Object> details = new LinkedHashMap<>();

    details.put("userId", exception.userProfileId());

    details.put("expectedVersion", exception.expectedVersion());

    return response(
        HttpStatus.CONFLICT,
        UserProfileErrorCode.USER_PROFILE_VERSION_CONFLICT,
        "User profile version is stale",
        details,
        request);
  }

  @ExceptionHandler({
    UserProfilePersistenceException.class,
    DataAccessException.class,
    IllegalStateException.class
  })
  public ResponseEntity<UserProfileErrorResponse> handlePersistenceFailure(
      Exception exception, HttpServletRequest request) {
    return internalError(
        UserProfileErrorCode.USER_PROFILE_PERSISTENCE_ERROR,
        "User profile operation failed",
        exception,
        request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<UserProfileErrorResponse> handleUnexpectedFailure(
      Exception exception, HttpServletRequest request) {
    return internalError(
        UserProfileErrorCode.USER_PROFILE_INTERNAL_ERROR,
        "An unexpected error occurred",
        exception,
        request);
  }

  private ResponseEntity<UserProfileErrorResponse> internalError(
      UserProfileErrorCode code, String message, Exception exception, HttpServletRequest request) {
    String traceId = traceId(request);

    LOGGER.error("User profile request failed. traceId={}, code={}", traceId, code, exception);

    return response(HttpStatus.INTERNAL_SERVER_ERROR, code, message, Map.of(), traceId);
  }

  private static ResponseEntity<UserProfileErrorResponse> response(
      HttpStatus status,
      UserProfileErrorCode code,
      String message,
      Map<String, Object> details,
      HttpServletRequest request) {
    return response(status, code, message, details, traceId(request));
  }

  private static ResponseEntity<UserProfileErrorResponse> response(
      HttpStatus status,
      UserProfileErrorCode code,
      String message,
      Map<String, Object> details,
      String traceId) {
    UserProfileErrorResponse body =
        new UserProfileErrorResponse(code, message, details, traceId, Instant.now());

    return ResponseEntity.status(status).body(body);
  }

  private static String traceId(HttpServletRequest request) {
    Object existing = request.getAttribute(TRACE_ID_ATTRIBUTE);

    if (existing instanceof String value && !value.isBlank()) {
      return value;
    }

    String generated = UUID.randomUUID().toString();

    request.setAttribute(TRACE_ID_ATTRIBUTE, generated);

    return generated;
  }
}
