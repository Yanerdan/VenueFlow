package com.yanerdan.venueflow.booking.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BookingReservationMapper extends BaseMapper<BookingReservationEntity> {
  @Select(
      """
      SELECT *
      FROM booking_reservation
      WHERE status = 'PENDING_CONFIRMATION' AND expire_at <= #{now}
        AND ((timeout_state IN ('IDLE', 'RETRY') AND timeout_next_check_at <= #{now})
          OR (timeout_state = 'LEASED' AND timeout_lease_expires_at <= #{now}))
      ORDER BY COALESCE(timeout_next_check_at, timeout_lease_expires_at), id
      LIMIT #{limit}
      """)
  List<BookingReservationEntity> selectTimeoutDue(
      @Param("now") LocalDateTime now, @Param("limit") int limit);

  @Update(
      """
      UPDATE booking_reservation
      SET timeout_state = 'LEASED', timeout_lease_owner = #{owner},
          timeout_lease_expires_at = #{leaseExpiresAt},
          timeout_attempt_count = timeout_attempt_count + 1,
          version = version + 1, updated_at = #{now}
      WHERE id = #{id} AND version = #{version} AND status = 'PENDING_CONFIRMATION'
        AND expire_at <= #{now}
        AND ((timeout_state IN ('IDLE', 'RETRY') AND timeout_next_check_at <= #{now})
          OR (timeout_state = 'LEASED' AND timeout_lease_expires_at <= #{now}))
      """)
  int claimTimeout(
      @Param("id") long id,
      @Param("version") long version,
      @Param("owner") String owner,
      @Param("now") LocalDateTime now,
      @Param("leaseExpiresAt") LocalDateTime leaseExpiresAt);

  @Update(
      """
      UPDATE booking_reservation
      SET timeout_state = 'RETRY', timeout_lease_owner = NULL,
          timeout_lease_expires_at = NULL, timeout_next_check_at = #{nextCheckAt},
          timeout_last_error_code = #{errorCode}, version = version + 1, updated_at = #{now}
      WHERE id = #{id} AND version = #{version} AND status = 'PENDING_CONFIRMATION'
        AND timeout_state = 'LEASED' AND timeout_lease_owner = #{owner}
      """)
  int retryTimeout(
      @Param("id") long id,
      @Param("version") long version,
      @Param("owner") String owner,
      @Param("errorCode") String errorCode,
      @Param("nextCheckAt") LocalDateTime nextCheckAt,
      @Param("now") LocalDateTime now);

  @Update(
      """
      UPDATE booking_reservation
      SET timeout_state = 'EXHAUSTED', timeout_lease_owner = NULL,
          timeout_lease_expires_at = NULL, timeout_next_check_at = NULL,
          timeout_last_error_code = #{errorCode}, version = version + 1, updated_at = #{now}
      WHERE id = #{id} AND version = #{version} AND status = 'PENDING_CONFIRMATION'
        AND timeout_state = 'LEASED' AND timeout_lease_owner = #{owner}
      """)
  int exhaustTimeout(
      @Param("id") long id,
      @Param("version") long version,
      @Param("owner") String owner,
      @Param("errorCode") String errorCode,
      @Param("now") LocalDateTime now);

  @Select(
      """
      SELECT COUNT(*) FROM booking_reservation
      WHERE status = 'PENDING_CONFIRMATION' AND expire_at <= #{now}
        AND timeout_state IN ('IDLE', 'RETRY', 'LEASED')
      """)
  long countTimeoutDue(@Param("now") LocalDateTime now);

  @Select(
      """
      SELECT COALESCE(TIMESTAMPDIFF(SECOND, MIN(expire_at), #{now}), 0)
      FROM booking_reservation
      WHERE status = 'PENDING_CONFIRMATION' AND expire_at <= #{now}
        AND timeout_state IN ('IDLE', 'RETRY', 'LEASED')
      """)
  long oldestTimeoutAgeSeconds(@Param("now") LocalDateTime now);

  @Select(
      """
      SELECT *
      FROM booking_reservation
      WHERE user_id = #{userId}
      ORDER BY created_at DESC, id DESC
      LIMIT #{limit} OFFSET #{offset}
      """)
  List<BookingReservationEntity> selectHistory(
      @Param("userId") long userId, @Param("offset") long offset, @Param("limit") int limit);

  @Select("SELECT COUNT(*) FROM booking_reservation WHERE user_id = #{userId}")
  long countHistory(@Param("userId") long userId);

  @Select(
      """
      SELECT *
      FROM booking_reservation
      WHERE (#{status} IS NULL OR status = #{status})
      ORDER BY created_at DESC, id DESC
      LIMIT #{limit} OFFSET #{offset}
      """)
  List<BookingReservationEntity> selectManagementHistory(
      @Param("status") String status, @Param("offset") long offset, @Param("limit") int limit);

  @Select(
      """
      SELECT COUNT(*)
      FROM booking_reservation
      WHERE (#{status} IS NULL OR status = #{status})
      """)
  long countManagementHistory(@Param("status") String status);
}
