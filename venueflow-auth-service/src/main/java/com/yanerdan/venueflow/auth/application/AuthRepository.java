package com.yanerdan.venueflow.auth.application;

import com.yanerdan.venueflow.auth.domain.AuthCredential;
import com.yanerdan.venueflow.auth.domain.AuthAccount;
import com.yanerdan.venueflow.auth.domain.CampusRole;
import com.yanerdan.venueflow.auth.domain.RefreshSession;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface AuthRepository {

  void createCredential(UUID userId, String username, String passwordHash, LocalDateTime now);

  Optional<AuthCredential> findCredential(String username);

  Optional<AuthCredential> findCredential(UUID userId);

  void setRole(String username, CampusRole role, LocalDateTime now);

  List<AuthAccount> listAccounts(int limit);

  Optional<AuthAccount> findAccount(UUID userId);

  boolean changeRole(
      UUID userId, CampusRole role, long expectedVersion, LocalDateTime now);

  void recordFailure(
      long id, long version, int attempts, LocalDateTime lockedUntil, LocalDateTime now);

  void resetFailures(long id, long version, LocalDateTime now);

  void createRefresh(
      String hash,
      UUID userId,
      String username,
      long tokenVersion,
      LocalDateTime expiresAt,
      LocalDateTime now);

  Optional<RefreshSession> lockRefresh(String hash);

  boolean revokeRefresh(String hash, String replacementHash, LocalDateTime now);

  void revokeRefresh(String hash, LocalDateTime now);
}
