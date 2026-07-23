package com.yanerdan.venueflow.booking.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookingIdempotencyMapper extends BaseMapper<BookingIdempotencyEntity> {
  @Insert(
      """
      INSERT IGNORE INTO booking_idempotency
        (user_id, operation, idempotency_key, request_hash, request_id, booking_id, status,
         failure_code, version, created_at, updated_at, expires_at)
      VALUES
        (#{userId}, #{operation}, #{idempotencyKey}, #{requestHash}, #{requestId}, #{bookingId},
         #{status}, #{failureCode}, #{version}, #{createdAt}, #{updatedAt}, #{expiresAt})
      """)
  int tryClaim(BookingIdempotencyEntity entity);
}
