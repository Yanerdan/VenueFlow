package com.yanerdan.venueflow.booking.reconciliation.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReconciliationIntentMapper {

  @Insert(
      """
      INSERT IGNORE INTO booking_reconciliation_intent
        (workflow_type, request_id, booking_id, slot_id, quantity,
         allocation_operation_id, release_operation_id, next_check_at)
      VALUES
        (#{workflowType}, #{requestId}, #{bookingId}, #{slotId}, #{quantity},
         #{allocationOperationId}, #{releaseOperationId}, #{nextCheckAt})
      """)
  int insertIntent(
      @Param("workflowType") String workflowType,
      @Param("requestId") String requestId,
      @Param("bookingId") Long bookingId,
      @Param("slotId") long slotId,
      @Param("quantity") int quantity,
      @Param("allocationOperationId") String allocationOperationId,
      @Param("releaseOperationId") String releaseOperationId,
      @Param("nextCheckAt") LocalDateTime nextCheckAt);

  @Select(
      """
      SELECT *
      FROM booking_reconciliation_intent
      WHERE workflow_type = #{workflowType} AND request_id = #{requestId}
      """)
  ReconciliationIntentEntity selectByWorkflowRequest(
      @Param("workflowType") String workflowType, @Param("requestId") String requestId);

  @Select("SELECT * FROM booking_reconciliation_intent WHERE id = #{id}")
  ReconciliationIntentEntity selectIntent(@Param("id") long id);

  @Select(
      """
      SELECT *
      FROM booking_reconciliation_intent
      WHERE (state = 'OPEN' AND next_check_at <= #{now})
         OR (state = 'LEASED' AND lease_expires_at <= #{now})
      ORDER BY CASE WHEN state = 'OPEN' THEN next_check_at ELSE lease_expires_at END, id
      LIMIT #{limit}
      """)
  List<ReconciliationIntentEntity> selectDue(
      @Param("now") LocalDateTime now, @Param("limit") int limit);

  @Update(
      """
      UPDATE booking_reconciliation_intent
      SET state = 'LEASED', lease_owner = #{leaseOwner}, lease_expires_at = #{leaseExpiresAt},
          attempt_count = attempt_count + 1, version = version + 1, updated_at = #{now}
      WHERE id = #{id} AND version = #{expectedVersion}
        AND ((state = 'OPEN' AND next_check_at <= #{now})
          OR (state = 'LEASED' AND lease_expires_at <= #{now}))
      """)
  int claim(
      @Param("id") long id,
      @Param("expectedVersion") long expectedVersion,
      @Param("leaseOwner") String leaseOwner,
      @Param("now") LocalDateTime now,
      @Param("leaseExpiresAt") LocalDateTime leaseExpiresAt);

  @Update(
      """
      UPDATE booking_reconciliation_intent
      SET booking_id = COALESCE(#{bookingId}, booking_id), state = 'RESOLVED',
          outcome_code = #{outcomeCode}, last_error_code = NULL, next_check_at = NULL,
          lease_owner = NULL, lease_expires_at = NULL, resolved_at = #{resolvedAt},
          version = version + 1, updated_at = #{resolvedAt}
      WHERE workflow_type = #{workflowType} AND request_id = #{requestId} AND state = 'OPEN'
      """)
  int resolveOpen(
      @Param("workflowType") String workflowType,
      @Param("requestId") String requestId,
      @Param("bookingId") Long bookingId,
      @Param("outcomeCode") String outcomeCode,
      @Param("resolvedAt") LocalDateTime resolvedAt);

  @Update(
      """
      UPDATE booking_reconciliation_intent
      SET state = 'RESOLVED', outcome_code = #{outcomeCode}, last_error_code = NULL,
          next_check_at = NULL, lease_owner = NULL, lease_expires_at = NULL,
          resolved_at = #{resolvedAt}, version = version + 1, updated_at = #{resolvedAt}
      WHERE id = #{id} AND version = #{expectedVersion}
        AND state = 'LEASED' AND lease_owner = #{leaseOwner}
      """)
  int resolveLeased(
      @Param("id") long id,
      @Param("expectedVersion") long expectedVersion,
      @Param("leaseOwner") String leaseOwner,
      @Param("outcomeCode") String outcomeCode,
      @Param("resolvedAt") LocalDateTime resolvedAt);

  @Update(
      """
      UPDATE booking_reconciliation_intent
      SET state = 'OPEN', last_error_code = #{errorCode}, next_check_at = #{nextCheckAt},
          lease_owner = NULL, lease_expires_at = NULL, version = version + 1,
          updated_at = #{updatedAt}
      WHERE id = #{id} AND version = #{expectedVersion}
        AND state = 'LEASED' AND lease_owner = #{leaseOwner}
      """)
  int retry(
      @Param("id") long id,
      @Param("expectedVersion") long expectedVersion,
      @Param("leaseOwner") String leaseOwner,
      @Param("errorCode") String errorCode,
      @Param("nextCheckAt") LocalDateTime nextCheckAt,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Update(
      """
      UPDATE booking_reconciliation_intent
      SET state = 'EXHAUSTED', outcome_code = 'ATTEMPTS_EXHAUSTED',
          last_error_code = #{errorCode}, next_check_at = NULL, lease_owner = NULL,
          lease_expires_at = NULL, resolved_at = #{resolvedAt}, version = version + 1,
          updated_at = #{resolvedAt}
      WHERE id = #{id} AND version = #{expectedVersion}
        AND state = 'LEASED' AND lease_owner = #{leaseOwner}
      """)
  int exhaust(
      @Param("id") long id,
      @Param("expectedVersion") long expectedVersion,
      @Param("leaseOwner") String leaseOwner,
      @Param("errorCode") String errorCode,
      @Param("resolvedAt") LocalDateTime resolvedAt);

  @Select(
      """
      SELECT COUNT(*)
      FROM booking_reconciliation_intent
      WHERE (state = 'OPEN' AND next_check_at <= #{now})
         OR (state = 'LEASED' AND lease_expires_at <= #{now})
      """)
  long countDue(@Param("now") LocalDateTime now);

  @Select(
      """
      SELECT COALESCE(TIMESTAMPDIFF(SECOND, MIN(created_at), #{now}), 0)
      FROM booking_reconciliation_intent
      WHERE state IN ('OPEN', 'LEASED')
      """)
  long oldestDueAgeSeconds(@Param("now") LocalDateTime now);
}
