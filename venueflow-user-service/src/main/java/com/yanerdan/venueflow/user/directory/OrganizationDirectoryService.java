package com.yanerdan.venueflow.user.directory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Profile("persistence")
public class OrganizationDirectoryService {

  private static final Pattern KEY = Pattern.compile("[A-Za-z0-9._:-]{1,96}");
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transactions;
  private final Clock clock;

  public OrganizationDirectoryService(
      JdbcTemplate jdbc, TransactionTemplate transactions, Clock clock) {
    this.jdbc = jdbc;
    this.transactions = transactions;
    this.clock = clock;
  }

  public SyncRun synchronize(SyncCommand command) {
    validate(command);
    SyncRun previous = findRun(command.source(), command.runKey());
    if (previous != null) {
      return previous;
    }
    LocalDateTime now = now();
    jdbc.update(
        """
        INSERT INTO directory_sync_run
          (source, run_key, sync_mode, status, started_at)
        VALUES (?, ?, ?, 'RUNNING', ?)
        """,
        command.source(),
        command.runKey(),
        command.mode(),
        now);
    try {
      transactions.executeWithoutResult(ignored -> apply(command, now));
      jdbc.update(
          """
          UPDATE directory_sync_run
          SET status = 'SUCCEEDED', organization_count = ?, membership_count = ?,
              completed_at = ?
          WHERE source = ? AND run_key = ?
          """,
          command.units().size(),
          command.memberships().size(),
          now(),
          command.source(),
          command.runKey());
    } catch (RuntimeException exception) {
      jdbc.update(
          """
          UPDATE directory_sync_run
          SET status = 'FAILED', error_summary = ?, completed_at = ?
          WHERE source = ? AND run_key = ?
          """,
          bounded(exception.getMessage()),
          now(),
          command.source(),
          command.runKey());
      throw exception;
    }
    return Objects.requireNonNull(findRun(command.source(), command.runKey()));
  }

  public List<OrganizationUnit> organizations(String source) {
    validateSource(source);
    return jdbc.query(
        """
        SELECT source, external_key, code, name, parent_external_key, active, last_synced_at
        FROM organization_unit
        WHERE source = ?
        ORDER BY active DESC, COALESCE(parent_external_key, ''), code, external_key
        LIMIT 500
        """,
        OrganizationDirectoryService::unit,
        source);
  }

  public List<SyncRun> runs(String source) {
    validateSource(source);
    return jdbc.query(
        """
        SELECT source, run_key, sync_mode, status, organization_count, membership_count,
               error_summary, started_at, completed_at
        FROM directory_sync_run
        WHERE source = ?
        ORDER BY started_at DESC, id DESC
        LIMIT 20
        """,
        OrganizationDirectoryService::run,
        source);
  }

  private void apply(SyncCommand command, LocalDateTime now) {
    if ("FULL".equals(command.mode())) {
      jdbc.update(
          "UPDATE organization_unit SET active = 0, updated_at = ? WHERE source = ?",
          now,
          command.source());
      jdbc.update(
          "UPDATE directory_membership SET active = 0, updated_at = ? WHERE source = ?",
          now,
          command.source());
      jdbc.update(
          """
          UPDATE user_profile
          SET authoritative_source = NULL, organization_external_key = NULL,
              directory_synced_at = NULL, version = version + 1, updated_at = ?
          WHERE authoritative_source = ?
          """,
          now,
          command.source());
    }
    for (UnitInput unit : command.units()) {
      if (unit.parentExternalKey() != null
          && !existsUnit(command.source(), unit.parentExternalKey(), command.units())) {
        throw new IllegalArgumentException("Organization parent was not found");
      }
      jdbc.update(
          """
          INSERT INTO organization_unit
            (source, external_key, code, name, parent_external_key, active,
             last_synced_at, created_at, updated_at)
          VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?)
          ON DUPLICATE KEY UPDATE
            code = VALUES(code), name = VALUES(name),
            parent_external_key = VALUES(parent_external_key), active = 1,
            last_synced_at = VALUES(last_synced_at), updated_at = VALUES(updated_at)
          """,
          command.source(),
          unit.externalKey(),
          unit.code(),
          unit.name(),
          blankToNull(unit.parentExternalKey()),
          now,
          now,
          now);
    }
    for (MembershipInput membership : command.memberships()) {
      OrganizationUnit unit = findUnit(command.source(), membership.organizationExternalKey());
      if (unit == null || !unit.active()) {
        throw new IllegalArgumentException("Membership organization was not found");
      }
      jdbc.update(
          """
          INSERT INTO directory_membership
            (source, external_user_id, organization_external_key, campus_id, identity_type,
             active, last_synced_at, created_at, updated_at)
          VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?)
          ON DUPLICATE KEY UPDATE
            organization_external_key = VALUES(organization_external_key),
            campus_id = VALUES(campus_id), identity_type = VALUES(identity_type), active = 1,
            last_synced_at = VALUES(last_synced_at), updated_at = VALUES(updated_at)
          """,
          command.source(),
          membership.externalUserId(),
          membership.organizationExternalKey(),
          blankToNull(membership.campusId()),
          membership.identityType(),
          now,
          now,
          now);
      jdbc.update(
          """
          UPDATE user_profile
          SET campus_id = ?, identity_type = ?, department = ?,
              authoritative_source = ?, organization_external_key = ?,
              directory_synced_at = ?, version = version + 1, updated_at = ?
          WHERE external_user_id = ?
          """,
          blankToNull(membership.campusId()),
          membership.identityType(),
          unit.name(),
          command.source(),
          membership.organizationExternalKey(),
          now,
          now,
          membership.externalUserId());
    }
  }

  private void validate(SyncCommand command) {
    Objects.requireNonNull(command, "Sync command is required");
    validateSource(command.source());
    requireKey(command.runKey(), "run key");
    if (!"PARTIAL".equals(command.mode()) && !"FULL".equals(command.mode())) {
      throw new IllegalArgumentException("Sync mode is invalid");
    }
    if (command.units() == null
        || command.memberships() == null
        || command.units().size() > 500
        || command.memberships().size() > 1000) {
      throw new IllegalArgumentException("Directory batch is outside its bounds");
    }
    Map<String, String> parents = new HashMap<>();
    for (UnitInput unit : command.units()) {
      requireKey(unit.externalKey(), "organization key");
      requireText(unit.code(), 64, "organization code");
      requireText(unit.name(), 160, "organization name");
      if (unit.parentExternalKey() != null) {
        requireKey(unit.parentExternalKey(), "parent key");
      }
      if (parents.put(unit.externalKey(), unit.parentExternalKey()) != null) {
        throw new IllegalArgumentException("Organization key is duplicated");
      }
    }
    for (String key : parents.keySet()) {
      Set<String> seen = new HashSet<>();
      String current = key;
      while (current != null && parents.containsKey(current)) {
        if (!seen.add(current)) {
          throw new IllegalArgumentException("Organization hierarchy contains a cycle");
        }
        current = parents.get(current);
      }
    }
    Set<String> users = new HashSet<>();
    for (MembershipInput membership : command.memberships()) {
      requireText(membership.externalUserId(), 128, "external user ID");
      requireKey(membership.organizationExternalKey(), "membership organization");
      if (!Set.of("STUDENT", "STAFF", "OTHER").contains(membership.identityType())) {
        throw new IllegalArgumentException("Identity type is invalid");
      }
      if (!users.add(membership.externalUserId())) {
        throw new IllegalArgumentException("Directory user is duplicated");
      }
    }
  }

  private boolean existsUnit(String source, String key, List<UnitInput> units) {
    return units.stream().anyMatch(unit -> unit.externalKey().equals(key))
        || findUnit(source, key) != null;
  }

  private OrganizationUnit findUnit(String source, String externalKey) {
    List<OrganizationUnit> units =
        jdbc.query(
            """
            SELECT source, external_key, code, name, parent_external_key, active, last_synced_at
            FROM organization_unit WHERE source = ? AND external_key = ?
            """,
            OrganizationDirectoryService::unit,
            source,
            externalKey);
    return units.stream().findFirst().orElse(null);
  }

  private SyncRun findRun(String source, String runKey) {
    List<SyncRun> values =
        jdbc.query(
            """
            SELECT source, run_key, sync_mode, status, organization_count, membership_count,
                   error_summary, started_at, completed_at
            FROM directory_sync_run WHERE source = ? AND run_key = ?
            """,
            OrganizationDirectoryService::run,
            source,
            runKey);
    return values.stream().findFirst().orElse(null);
  }

  private static OrganizationUnit unit(ResultSet result, int row) throws SQLException {
    return new OrganizationUnit(
        result.getString("source"),
        result.getString("external_key"),
        result.getString("code"),
        result.getString("name"),
        result.getString("parent_external_key"),
        result.getBoolean("active"),
        result.getObject("last_synced_at", LocalDateTime.class));
  }

  private static SyncRun run(ResultSet result, int row) throws SQLException {
    return new SyncRun(
        result.getString("source"),
        result.getString("run_key"),
        result.getString("sync_mode"),
        result.getString("status"),
        result.getInt("organization_count"),
        result.getInt("membership_count"),
        result.getString("error_summary"),
        result.getObject("started_at", LocalDateTime.class),
        result.getObject("completed_at", LocalDateTime.class));
  }

  private void validateSource(String source) {
    requireKey(source, "source");
    if (source.length() > 48) {
      throw new IllegalArgumentException("Directory source is too long");
    }
  }

  private static void requireKey(String value, String name) {
    if (value == null || !KEY.matcher(value).matches()) {
      throw new IllegalArgumentException(name + " is invalid");
    }
  }

  private static void requireText(String value, int maximum, String name) {
    if (value == null || value.isBlank() || value.trim().length() > maximum) {
      throw new IllegalArgumentException(name + " is invalid");
    }
  }

  private static String bounded(String value) {
    String text = value == null ? "Directory synchronization failed" : value;
    return text.length() <= 500 ? text : text.substring(0, 500);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private LocalDateTime now() {
    return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
  }

  public record UnitInput(String externalKey, String code, String name, String parentExternalKey) {}

  public record MembershipInput(
      String externalUserId,
      String organizationExternalKey,
      String campusId,
      String identityType) {}

  public record SyncCommand(
      String source,
      String runKey,
      String mode,
      List<UnitInput> units,
      List<MembershipInput> memberships) {}

  public record OrganizationUnit(
      String source,
      String externalKey,
      String code,
      String name,
      String parentExternalKey,
      boolean active,
      LocalDateTime lastSyncedAt) {}

  public record SyncRun(
      String source,
      String runKey,
      String mode,
      String status,
      int organizationCount,
      int membershipCount,
      String errorSummary,
      LocalDateTime startedAt,
      LocalDateTime completedAt) {}
}
