package com.yanerdan.venueflow.booking.persistence;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BookingStatusLogMapper {
  @Insert(
      """
      INSERT INTO booking_status_log
        (booking_id, from_status, to_status, source, reason_code, created_at)
      VALUES
        (#{bookingId}, #{fromStatus}, #{toStatus}, #{source}, #{reasonCode}, #{createdAt})
      """)
  int insertLog(
      @Param("bookingId") long bookingId,
      @Param("fromStatus") String fromStatus,
      @Param("toStatus") String toStatus,
      @Param("source") String source,
      @Param("reasonCode") String reasonCode,
      @Param("createdAt") LocalDateTime createdAt);
}
