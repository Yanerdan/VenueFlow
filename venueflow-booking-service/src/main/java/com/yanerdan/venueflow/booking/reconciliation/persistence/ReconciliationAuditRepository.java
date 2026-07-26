package com.yanerdan.venueflow.booking.reconciliation.persistence;

import com.yanerdan.venueflow.booking.reconciliation.application.ReconciliationSummary;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@Profile("persistence & reconciliation")
public class ReconciliationAuditRepository {
  private final JdbcTemplate jdbc;

  public ReconciliationAuditRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long startRun(
      String runKey, String trigger, String owner, LocalDateTime leaseExpiresAt, String reason) {
    GeneratedKeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                  INSERT INTO reconciliation_run
                    (run_key, trigger_type, owner_id, lease_expires_at, operator_reason)
                  VALUES (?, ?, ?, ?, ?)
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, runKey);
          statement.setString(2, trigger);
          statement.setString(3, owner);
          statement.setObject(4, leaseExpiresAt);
          statement.setString(5, reason);
          return statement;
        },
        keys);
    Number key = keys.getKey();
    if (key == null) {
      throw new IllegalStateException("Reconciliation run id was not generated");
    }
    return key.longValue();
  }

  public void completeRun(long runId, ReconciliationSummary summary, String status) {
    jdbc.update(
        """
        UPDATE reconciliation_run
        SET status = ?, claimed_count = ?, consistent_count = ?, repaired_count = ?,
            unresolved_count = ?, failed_count = ?, lease_reclaimed_count = ?,
            completed_at = UTC_TIMESTAMP(6)
        WHERE id = ? AND status = 'RUNNING'
        """,
        status,
        summary.claimed(),
        summary.consistent(),
        summary.repaired(),
        summary.unresolved(),
        summary.failed(),
        summary.leaseReclaimed(),
        runId);
  }

  public void recordIssue(long intentId, String issueCode, String severity) {
    jdbc.update(
        """
        INSERT INTO reconciliation_issue
          (intent_id, issue_code, severity)
        VALUES (?, ?, ?)
        ON DUPLICATE KEY UPDATE occurrence_count = occurrence_count + 1,
          severity = VALUES(severity), state = 'OPEN', last_seen_at = UTC_TIMESTAMP(6),
          resolved_at = NULL
        """,
        intentId,
        issueCode,
        severity);
  }

  public void resolveIssues(long intentId) {
    jdbc.update(
        """
        UPDATE reconciliation_issue
        SET state = 'RESOLVED', resolved_at = UTC_TIMESTAMP(6), last_seen_at = UTC_TIMESTAMP(6)
        WHERE intent_id = ? AND state = 'OPEN'
        """,
        intentId);
  }

  public long startRepair(
      long intentId,
      long runId,
      int attempt,
      String actionType,
      String reasonCode,
      String operationId) {
    GeneratedKeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                  INSERT INTO repair_action
                    (intent_id, run_id, attempt_number, action_type, reason_code, operation_id)
                  VALUES (?, ?, ?, ?, ?, ?)
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          statement.setLong(1, intentId);
          statement.setLong(2, runId);
          statement.setInt(3, attempt);
          statement.setString(4, actionType);
          statement.setString(5, reasonCode);
          statement.setString(6, operationId);
          return statement;
        },
        keys);
    Number key = keys.getKey();
    if (key == null) {
      throw new IllegalStateException("Repair action id was not generated");
    }
    return key.longValue();
  }

  public void completeRepair(long actionId, String status, String resultCode) {
    jdbc.update(
        """
        UPDATE repair_action
        SET status = ?, result_code = ?, completed_at = UTC_TIMESTAMP(6)
        WHERE id = ? AND status = 'STARTED'
        """,
        status,
        resultCode,
        actionId);
  }
}
