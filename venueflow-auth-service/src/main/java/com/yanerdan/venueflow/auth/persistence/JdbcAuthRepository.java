package com.yanerdan.venueflow.auth.persistence;

import com.yanerdan.venueflow.auth.application.AuthRepository;
import com.yanerdan.venueflow.auth.domain.AuthCredential;
import com.yanerdan.venueflow.auth.domain.CampusRole;
import com.yanerdan.venueflow.auth.domain.RefreshSession;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("persistence")
public class JdbcAuthRepository implements AuthRepository {

  private final JdbcTemplate jdbc;

  public JdbcAuthRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void createCredential(
      UUID userId, String username, String passwordHash, LocalDateTime now) {
    jdbc.update(
        """
        INSERT INTO auth_credentials
          (user_id, username, password_hash, failed_attempts, token_version, version,
           created_at, updated_at)
        VALUES (?, ?, ?, 0, 1, 0, ?, ?)
        """,
        userId.toString(),
        username,
        passwordHash,
        now,
        now);
  }

  @Override
  public Optional<AuthCredential> findCredential(String username) {
    return first(
        jdbc.query(
            "SELECT * FROM auth_credentials WHERE username = ?",
            JdbcAuthRepository::credential,
            username));
  }

  @Override
  public Optional<AuthCredential> findCredential(UUID userId) {
    return first(
        jdbc.query(
            "SELECT * FROM auth_credentials WHERE user_id = ?",
            JdbcAuthRepository::credential,
            userId.toString()));
  }

  @Override
  public void setRole(String username, CampusRole role, LocalDateTime now) {
    jdbc.update(
        """
        UPDATE auth_credentials
        SET role = ?, token_version = token_version + 1, version = version + 1, updated_at = ?
        WHERE username = ? AND role <> ?
        """,
        role.name(),
        now,
        username,
        role.name());
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordFailure(
      long id, long version, int attempts, LocalDateTime lockedUntil, LocalDateTime now) {
    jdbc.update(
        """
        UPDATE auth_credentials
        SET failed_attempts = ?, locked_until = ?, version = version + 1, updated_at = ?
        WHERE id = ? AND version = ?
        """,
        attempts,
        lockedUntil,
        now,
        id,
        version);
  }

  @Override
  public void resetFailures(long id, long version, LocalDateTime now) {
    jdbc.update(
        """
        UPDATE auth_credentials
        SET failed_attempts = 0, locked_until = NULL, version = version + 1, updated_at = ?
        WHERE id = ? AND version = ?
        """,
        now,
        id,
        version);
  }

  @Override
  public void createRefresh(
      String hash,
      UUID userId,
      String username,
      long tokenVersion,
      LocalDateTime expiresAt,
      LocalDateTime now) {
    jdbc.update(
        """
        INSERT INTO auth_refresh_tokens
          (token_hash, user_id, username, token_version, expires_at, created_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        hash,
        userId.toString(),
        username,
        tokenVersion,
        expiresAt,
        now);
  }

  @Override
  public Optional<RefreshSession> lockRefresh(String hash) {
    return first(
        jdbc.query(
            "SELECT * FROM auth_refresh_tokens WHERE token_hash = ? FOR UPDATE",
            JdbcAuthRepository::refresh,
            hash));
  }

  @Override
  public boolean revokeRefresh(String hash, String replacementHash, LocalDateTime now) {
    return jdbc.update(
            """
            UPDATE auth_refresh_tokens
            SET revoked_at = ?, replaced_by_hash = ?
            WHERE token_hash = ? AND revoked_at IS NULL
            """,
            now,
            replacementHash,
            hash)
        == 1;
  }

  @Override
  public void revokeRefresh(String hash, LocalDateTime now) {
    jdbc.update(
        "UPDATE auth_refresh_tokens SET revoked_at = ? "
            + "WHERE token_hash = ? AND revoked_at IS NULL",
        now,
        hash);
  }

  private static AuthCredential credential(ResultSet result, int row) throws SQLException {
    return new AuthCredential(
        result.getLong("id"),
        UUID.fromString(result.getString("user_id")),
        result.getString("username"),
        result.getString("password_hash"),
        CampusRole.valueOf(result.getString("role")),
        result.getInt("failed_attempts"),
        result.getObject("locked_until", LocalDateTime.class),
        result.getLong("token_version"),
        result.getLong("version"));
  }

  private static RefreshSession refresh(ResultSet result, int row) throws SQLException {
    return new RefreshSession(
        result.getLong("id"),
        result.getString("token_hash"),
        UUID.fromString(result.getString("user_id")),
        result.getString("username"),
        result.getLong("token_version"),
        result.getObject("expires_at", LocalDateTime.class),
        result.getObject("revoked_at", LocalDateTime.class));
  }

  private static <T> Optional<T> first(List<T> values) {
    return values.stream().findFirst();
  }
}
