package com.yanerdan.venueflow.auth.web;

import com.yanerdan.venueflow.auth.application.FederatedIdentityService;
import com.yanerdan.venueflow.auth.application.FederatedIdentityService.AuthorizationStart;
import com.yanerdan.venueflow.auth.application.FederatedIdentityService.ProviderReadiness;
import com.yanerdan.venueflow.auth.web.AuthDtos.ExternalCompletionRequest;
import com.yanerdan.venueflow.auth.web.AuthDtos.SuccessResponse;
import com.yanerdan.venueflow.auth.web.AuthDtos.TokenResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/v1/auth/sso")
@Profile("persistence")
public class FederatedIdentityController {

  private final FederatedIdentityService service;

  public FederatedIdentityController(FederatedIdentityService service) {
    this.service = service;
  }

  @GetMapping("/providers")
  public SuccessResponse<List<ProviderReadiness>> providers() {
    return AuthController.success(List.of(service.provider()), "providers loaded");
  }

  @PostMapping("/{providerKey}/authorize")
  public SuccessResponse<AuthorizationStart> authorize(
      @PathVariable("providerKey") String providerKey) {
    return AuthController.success(service.initiate(providerKey), "authorization started");
  }

  @GetMapping("/{providerKey}/callback")
  public RedirectView callback(
      @PathVariable("providerKey") String providerKey,
      @RequestParam("state") String state,
      @RequestParam("code") String code) {
    String completion = service.complete(providerKey, state, code);
    return new RedirectView(service.browserReturn(completion));
  }

  @PostMapping("/complete")
  public SuccessResponse<TokenResponse> complete(
      @Valid @RequestBody ExternalCompletionRequest request) {
    var tokens = service.exchangeCompletion(request.completionCode());
    return AuthController.success(
        new TokenResponse(
            tokens.accessToken(),
            tokens.refreshToken(),
            tokens.tokenType(),
            tokens.expiresInSeconds()),
        "authenticated");
  }
}
