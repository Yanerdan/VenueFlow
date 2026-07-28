package com.yanerdan.venueflow.auth.web;

import com.yanerdan.venueflow.auth.application.AuthResult.Identity;
import com.yanerdan.venueflow.auth.application.AuthResult.Tokens;
import com.yanerdan.venueflow.auth.application.AuthService;
import com.yanerdan.venueflow.auth.web.AuthDtos.IdentityResponse;
import com.yanerdan.venueflow.auth.web.AuthDtos.LoginRequest;
import com.yanerdan.venueflow.auth.web.AuthDtos.RefreshRequest;
import com.yanerdan.venueflow.auth.web.AuthDtos.RegisterRequest;
import com.yanerdan.venueflow.auth.web.AuthDtos.SuccessResponse;
import com.yanerdan.venueflow.auth.web.AuthDtos.TokenResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Profile("persistence")
public class AuthController {

  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/register")
  public ResponseEntity<SuccessResponse<IdentityResponse>> register(
      @Valid @RequestBody RegisterRequest request) {
    Identity identity = service.register(request.username(), request.password().toCharArray());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(success(new IdentityResponse(identity.userId(), identity.username()), "registered"));
  }

  @PostMapping("/login")
  public SuccessResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    return success(
        tokens(service.login(request.username(), request.password().toCharArray())),
        "authenticated");
  }

  @PostMapping("/refresh")
  public SuccessResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    return success(tokens(service.refresh(request.refreshToken())), "refreshed");
  }

  @PostMapping("/logout")
  public SuccessResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
    service.logout(request.refreshToken());
    return success(null, "logged out");
  }

  private static TokenResponse tokens(Tokens tokens) {
    return new TokenResponse(
        tokens.accessToken(), tokens.refreshToken(), tokens.tokenType(), tokens.expiresInSeconds());
  }

  static <T> SuccessResponse<T> success(T data, String message) {
    return new SuccessResponse<>("OK", message, data, traceId());
  }

  static String traceId() {
    String traceId = MDC.get("traceId");
    return traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId;
  }
}
