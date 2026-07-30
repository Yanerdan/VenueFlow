package com.yanerdan.venueflow.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.yanerdan.venueflow.auth.domain.PasswordPolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class FederatedIdentityServiceTest {

  @Test
  void incompleteProviderIsVisibleButCannotStart() {
    OidcProviderProperties properties = new OidcProviderProperties();
    FederatedIdentityService service = service(mock(FederatedIdentityRepository.class), properties);

    assertThat(service.provider().ready()).isFalse();
    assertThat(service.provider().status()).isEqualTo("未启用");
  }

  @Test
  void readyProviderCreatesBoundedPkceAuthorization() {
    FederatedIdentityRepository repository = mock(FederatedIdentityRepository.class);
    OidcProviderProperties properties = readyProperties();
    FederatedIdentityService.AuthorizationStart result =
        service(repository, properties).initiate("campus");

    assertThat(result.authorizationUrl())
        .startsWith("https://id.example.edu/authorize?")
        .contains("response_type=code")
        .contains("code_challenge_method=S256")
        .contains("client_id=venueflow");
    verify(repository)
        .createTransaction(
            any(String.class),
            eq("campus"),
            any(String.class),
            any(String.class),
            eq("https://venue.example.edu/api/v1/auth/sso/campus/callback"),
            any(),
            any());
  }

  private static FederatedIdentityService service(
      FederatedIdentityRepository repository, OidcProviderProperties properties) {
    return new FederatedIdentityService(
        repository,
        mock(AuthService.class),
        new PasswordPolicy(),
        properties,
        Clock.fixed(Instant.parse("2026-07-30T00:00:00Z"), ZoneOffset.UTC),
        RestClient.builder());
  }

  private static OidcProviderProperties readyProperties() {
    OidcProviderProperties properties = new OidcProviderProperties();
    properties.setEnabled(true);
    properties.setIssuerUri("https://id.example.edu");
    properties.setAuthorizationUri("https://id.example.edu/authorize");
    properties.setTokenUri("https://id.example.edu/token");
    properties.setJwkSetUri("https://id.example.edu/jwks");
    properties.setClientId("venueflow");
    properties.setClientSecret("untracked-test-secret");
    properties.setCallbackUri("https://venue.example.edu/api/v1/auth/sso/campus/callback");
    properties.setBrowserReturnUri("https://venue.example.edu/");
    return properties;
  }
}
