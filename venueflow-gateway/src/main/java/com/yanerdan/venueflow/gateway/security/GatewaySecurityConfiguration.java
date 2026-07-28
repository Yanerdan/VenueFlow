package com.yanerdan.venueflow.gateway.security;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@Profile("gateway")
public class GatewaySecurityConfiguration {

  @Bean
  ReactiveJwtDecoder gatewayJwtDecoder(
      @Value("${venueflow.gateway.jwt-public-key}") String publicPem,
      @Value("${venueflow.gateway.issuer}") String issuer) {
    NimbusReactiveJwtDecoder decoder =
        NimbusReactiveJwtDecoder.withPublicKey(parsePublicKey(publicPem)).build();
    OAuth2TokenValidator<Jwt> subject =
        jwt ->
            jwt.getSubject() == null || jwt.getSubject().isBlank()
                ? OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Subject is required", null))
                : OAuth2TokenValidatorResult.success();
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer), subject));
    return decoder;
  }

  @Bean
  SecurityWebFilterChain gatewaySecurity(
      ServerHttpSecurity http,
      GatewayAuthenticationEntryPoint errors,
      CorsConfigurationSource corsConfigurationSource) {
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers("/api/v1/auth/**", "/actuator/health/**")
                    .permitAll()
                    .pathMatchers(
                        "/api/v1/users/**",
                        "/api/v1/resources/**",
                        "/api/v1/resource-categories/**",
                        "/api/v1/resource-slots/**",
                        "/api/v1/bookings/**",
                        "/api/v1/search/**",
                        "/api/v1/notifications/**")
                    .authenticated()
                    .anyExchange()
                    .denyAll())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(errors)
                    .accessDeniedHandler(errors::accessDenied))
        .oauth2ResourceServer(
            oauth ->
                oauth
                    .jwt(Customizer.withDefaults())
                    .authenticationEntryPoint(errors)
                    .accessDeniedHandler(errors::accessDenied))
        .build();
  }

  private static RSAPublicKey parsePublicKey(String pem) {
    if (pem == null || pem.isBlank()) {
      throw new IllegalArgumentException("Missing Gateway JWT public key");
    }
    try {
      String body =
          pem.replace("\\n", "\n")
              .replace("-----BEGIN PUBLIC KEY-----", "")
              .replace("-----END PUBLIC KEY-----", "")
              .replaceAll("\\s", "");
      return (RSAPublicKey)
          KeyFactory.getInstance("RSA")
              .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(body)));
    } catch (GeneralSecurityException | IllegalArgumentException exception) {
      throw new IllegalArgumentException("Invalid Gateway JWT public key", exception);
    }
  }
}
