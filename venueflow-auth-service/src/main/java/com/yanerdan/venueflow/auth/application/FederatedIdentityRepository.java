package com.yanerdan.venueflow.auth.application;

import com.yanerdan.venueflow.auth.domain.ExternalIdentityBinding;
import com.yanerdan.venueflow.auth.domain.ExternalLoginCompletion;
import com.yanerdan.venueflow.auth.domain.OidcLoginTransaction;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface FederatedIdentityRepository {

  void createTransaction(
      String stateHash,
      String providerKey,
      String nonce,
      String codeVerifier,
      String redirectUri,
      LocalDateTime expiresAt,
      LocalDateTime now);

  Optional<OidcLoginTransaction> consumeTransaction(
      String stateHash, String providerKey, LocalDateTime now);

  Optional<ExternalIdentityBinding> findBinding(String issuer, String subject);

  ExternalIdentityBinding createExternalAccount(
      String providerKey,
      String issuer,
      String subject,
      UUID userId,
      String username,
      String campusId,
      LocalDateTime now);

  void touchBinding(String issuer, String subject, LocalDateTime now);

  void createCompletion(String codeHash, UUID userId, LocalDateTime expiresAt, LocalDateTime now);

  Optional<ExternalLoginCompletion> consumeCompletion(String codeHash, LocalDateTime now);
}
