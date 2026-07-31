package com.yanerdan.venueflow.auth.application;

import com.yanerdan.venueflow.auth.domain.CampusRole;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("persistence")
public class AuthAdminBootstrap implements ApplicationRunner {
  private final AuthRepository repository;
  private final AuthService authService;
  private final String username;
  private final String password;

  public AuthAdminBootstrap(
      AuthRepository repository,
      AuthService authService,
      @Value("${venueflow.auth.bootstrap-admin-username:}") String username,
      @Value("${venueflow.auth.bootstrap-admin-password:}") String password) {
    this.repository = repository;
    this.authService = authService;
    this.username = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    this.password = password == null ? "" : password;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    if (username.isBlank() || password.isBlank()) {
      return;
    }
    if (repository.findCredential(username).isEmpty()) {
      authService.register(username, password.toCharArray());
    }
    repository.setRole(username, CampusRole.SYSTEM_ADMIN, LocalDateTime.now(ZoneOffset.UTC));
  }
}
