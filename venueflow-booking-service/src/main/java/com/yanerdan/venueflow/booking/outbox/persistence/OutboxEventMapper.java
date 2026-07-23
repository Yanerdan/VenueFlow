package com.yanerdan.venueflow.booking.outbox.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEventEntity> {
  @Select(
      """
      SELECT *
      FROM booking_outbox_event
      WHERE status = 'NEW'
         OR (status = 'RETRY' AND next_retry_at <= #{now})
         OR (status = 'PUBLISHING' AND lease_until <= #{now})
      ORDER BY id
      LIMIT #{limit}
      FOR UPDATE SKIP LOCKED
      """)
  List<OutboxEventEntity> lockEligible(@Param("now") LocalDateTime now, @Param("limit") int limit);

  @Update(
      """
      UPDATE booking_outbox_event
      SET status = 'PUBLISHING', claim_token = #{token}, lease_until = #{leaseUntil},
          next_retry_at = NULL, last_error_code = NULL, version = version + 1
      WHERE id = #{id}
      """)
  int markClaimed(
      @Param("id") long id,
      @Param("token") String token,
      @Param("leaseUntil") LocalDateTime leaseUntil);

  @Update(
      """
      UPDATE booking_outbox_event
      SET status = 'PUBLISHED', published_at = #{now}, claim_token = NULL, lease_until = NULL,
          next_retry_at = NULL, last_error_code = NULL, version = version + 1
      WHERE event_id = #{eventId} AND status = 'PUBLISHING' AND claim_token = #{token}
      """)
  int markPublished(
      @Param("eventId") String eventId,
      @Param("token") String token,
      @Param("now") LocalDateTime now);

  @Update(
      """
      UPDATE booking_outbox_event
      SET status = #{status}, retry_count = retry_count + 1, next_retry_at = #{nextRetryAt},
          claim_token = NULL, lease_until = NULL, last_error_code = #{errorCode},
          version = version + 1
      WHERE event_id = #{eventId} AND status = 'PUBLISHING' AND claim_token = #{token}
      """)
  int markFailed(
      @Param("eventId") String eventId,
      @Param("token") String token,
      @Param("status") String status,
      @Param("nextRetryAt") LocalDateTime nextRetryAt,
      @Param("errorCode") String errorCode);

  @Update(
      """
      UPDATE booking_outbox_event
      SET status = 'RETRY', next_retry_at = #{now}, claim_token = NULL, lease_until = NULL,
          last_error_code = 'MANUAL_REQUEUE', version = version + 1
      WHERE event_id = #{eventId} AND status = 'DEAD'
      """)
  int requeue(@Param("eventId") String eventId, @Param("now") LocalDateTime now);

  @Select(
      """
      SELECT COUNT(*)
      FROM booking_outbox_event
      WHERE status IN ('NEW', 'RETRY', 'PUBLISHING', 'DEAD')
      """)
  long countBacklog();

  @Select(
      """
      SELECT COALESCE(TIMESTAMPDIFF(SECOND, MIN(created_at), UTC_TIMESTAMP()), 0)
      FROM booking_outbox_event
      WHERE status IN ('NEW', 'RETRY', 'PUBLISHING')
      """)
  long oldestEligibleAgeSeconds();
}
