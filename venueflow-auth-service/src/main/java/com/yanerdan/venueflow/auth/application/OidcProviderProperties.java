package com.yanerdan.venueflow.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("venueflow.auth.oidc")
public class OidcProviderProperties {

  private boolean enabled;
  private String providerKey = "campus";
  private String displayName = "校园统一身份认证";
  private String issuerUri;
  private String authorizationUri;
  private String tokenUri;
  private String jwkSetUri;
  private String clientId;
  private String clientSecret;
  private String callbackUri;
  private String browserReturnUri;
  private String usernameClaim = "preferred_username";
  private String campusIdClaim = "campus_id";
  private String scopes = "openid profile";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getProviderKey() {
    return providerKey;
  }

  public void setProviderKey(String providerKey) {
    this.providerKey = providerKey;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getIssuerUri() {
    return issuerUri;
  }

  public void setIssuerUri(String issuerUri) {
    this.issuerUri = issuerUri;
  }

  public String getAuthorizationUri() {
    return authorizationUri;
  }

  public void setAuthorizationUri(String authorizationUri) {
    this.authorizationUri = authorizationUri;
  }

  public String getTokenUri() {
    return tokenUri;
  }

  public void setTokenUri(String tokenUri) {
    this.tokenUri = tokenUri;
  }

  public String getJwkSetUri() {
    return jwkSetUri;
  }

  public void setJwkSetUri(String jwkSetUri) {
    this.jwkSetUri = jwkSetUri;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getCallbackUri() {
    return callbackUri;
  }

  public void setCallbackUri(String callbackUri) {
    this.callbackUri = callbackUri;
  }

  public String getBrowserReturnUri() {
    return browserReturnUri;
  }

  public void setBrowserReturnUri(String browserReturnUri) {
    this.browserReturnUri = browserReturnUri;
  }

  public String getUsernameClaim() {
    return usernameClaim;
  }

  public void setUsernameClaim(String usernameClaim) {
    this.usernameClaim = usernameClaim;
  }

  public String getCampusIdClaim() {
    return campusIdClaim;
  }

  public void setCampusIdClaim(String campusIdClaim) {
    this.campusIdClaim = campusIdClaim;
  }

  public String getScopes() {
    return scopes;
  }

  public void setScopes(String scopes) {
    this.scopes = scopes;
  }
}
