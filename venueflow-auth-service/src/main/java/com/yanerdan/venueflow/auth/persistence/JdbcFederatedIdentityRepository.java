package com.yanerdan.venueflow.auth.persistence;

import com.yanerdan.venueflow.auth.application.FederatedIdentityRepository;
import com.yanerdan.venueflow.auth.domain.ExternalIdentityBinding;
import com.yanerdan.venueflow.auth.domain.ExternalLoginCompletion;
import com.yanerdan.venueflow.auth.domain.OidcLoginTransaction;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("persistence")
public class JdbcFederatedIdentityRepository implements FederatedIdentityRepository {

  private final JdbcTemplate jdbc;

  public JdbcFederatedIdentityRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void createTransaction(
      String stateHash,
      String providerKey,
      String nonce,
      String codeVerifier,
      String redirectUri,
      LocalDateTime expiresAt,
      LocalDateTime now) {
    jdbc.update(
        """
        INSERT INTO auth_oidc_transactions
          (state_hash, provider_key, nonce, code_verifier, redirect_uri, expires_at, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
        stateHash,
        providerKey,
        nonce,
        codeVerifier,
        redirectUri,
        expiresAt,
        now);
  }

  @Override
  @Transactional
  public Optional<OidcLoginTransaction> consumeTransaction(
      String stateHash, String providerKey, LocalDateTime now) {
    Optional<OidcLoginTransaction> transaction =
        first(
            jdbc.query(
                """
                SELECT * FROM auth_oidc_transactions
                WHERE state_hash = ? AND provider_key = ?
                FOR UPDATE
                """,
                JdbcFederatedIdentityRepository::transaction,
                stateHash,
                providerKey));
    if (transaction.isEmpty() || !transaction.get().activeAt(now)) {
      return Optional.empty();
    }
    int changed =
        jdbc.update(
            """
            UPDATE auth_oidc_transactions SET consumed_at = ?
            WHERE state_hash = ? AND consumed_at IS NULL
            """,
            now,
            stateHash);
    return changed == 1 ? transaction : Optional.empty();
  }

  @Override
  public Optional<ExternalIdentityBinding> findBinding(String issuer, String subject) {
    return first(
        jdbc.query(
            "SELECT * FROM auth_external_identities WHERE issuer = ? AND subject = ?",
            JdbcFederatedIdentityRepository::binding,
            issuer,
            subject));
  }

  @Override
  @Transactional
  public ExternalIdentityBinding createExternalAccount(
      String providerKey,
      String issuer,
      String subject,
      UUID userId,
      String username,
      String campusId,
      LocalDateTime now) {
    jdbc.update(
        """
        INSERT INTO auth_credentials
          (user_id, username, credential_source, password_hash, role, failed_attempts,
           token_version, version, created_at, updated_at)
        VALUES (?, ?, 'OIDC', NULL, 'APPLICANT', 0, 1, 0, ?, ?)
        """,
        userId.toString(),
        username,
        now,
        now);
    jdbc.update(
        """
        INSERT INTO auth_external_identities
          (provider_key, issuer, subject, user_id, username, campus_id, created_at, last_login_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        providerKey,
        issuer,
        subject,
        userId.toString(),
        username,
        campusId,
        now,
        now);
    return new ExternalIdentityBinding(
        providerKey, issuer, subject, userId, username, campusId, now);
  }

  @Override
  public void touchBinding(String issuer, String subject, LocalDateTime now) {
    jdbc.update(
        "UPDATE auth_external_identities SET last_login_at = ? WHERE issuer = ? AND subject = ?",
        now,
        issuer,
        subject);
  }

  @Override
  public void createCompletion(
      String codeHash, UUID userId, LocalDateTime expiresAt, LocalDateTime now) {
    jdbc.update(
        """
        INSERT INTO auth_login_completions
          (code_hash, user_id, expires_at, created_at)
        VALUES (?, ?, ?, ?)
        """,
        codeHash,
        userId.toString(),
        expiresAt,
        now);
  }

  @Override
  @Transactional
  public Optional<ExternalLoginCompletion> consumeCompletion(String codeHash, LocalDateTime now) {
    Optional<ExternalLoginCompletion> completion =
        first(
            jdbc.query(
                "SELECT * FROM auth_login_completions WHERE code_hash = ? FOR UPDATE",
                JdbcFederatedIdentityRepository::completion,
                codeHash));
    if (completion.isEmpty() || !completion.get().activeAt(now)) {
      return Optional.empty();
    }
    int changed =
        jdbc.update(
            """
            UPDATE auth_login_completions SET consumed_at = ?
            WHERE code_hash = ? AND consumed_at IS NULL
            """,
            now,
            codeHash);
    return changed == 1 ? completion : Optional.empty();
  }

  private static OidcLoginTransaction transaction(ResultSet result, int row) throws SQLException {
    return new OidcLoginTransaction(
        result.getString("state_hash"),
        result.getString("provider_key"),
        result.getString("nonce"),
        result.getString("code_verifier"),
        result.getString("redirect_uri"),
        result.getObject("expires_at", LocalDateTime.class),
        result.getObject("consumed_at", LocalDateTime.class));
  }

  private static ExternalIdentityBinding binding(ResultSet result, int row) throws SQLException {
    return new ExternalIdentityBinding(
        result.getString("provider_key"),
        result.getString("issuer"),
        result.getString("subject"),
        UUID.fromString(result.getString("user_id")),
        result.getString("username"),
        result.getString("campus_id"),
        result.getObject("last_login_at", LocalDateTime.class));
  }

  private static ExternalLoginCompletion completion(ResultSet result, int row) throws SQLException {
    return new ExternalLoginCompletion(
        result.getString("code_hash"),
        UUID.fromString(result.getString("user_id")),
        result.getObject("expires_at", LocalDateTime.class),
        result.getObject("consumed_at", LocalDateTime.class));
  }

  private static <T> Optional<T> first(List<T> values) {
    return values.stream().findFirst();
  }
}
