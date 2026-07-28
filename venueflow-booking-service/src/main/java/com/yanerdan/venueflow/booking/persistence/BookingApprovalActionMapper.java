package com.yanerdan.venueflow.booking.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BookingApprovalActionMapper {
  @Insert("""
      INSERT INTO booking_approval_action
        (booking_id, approval_step, actor_external_user_id, actor_role, decision, review_note,
         created_at)
      VALUES
        (#{bookingId}, #{step}, #{actorId}, #{actorRole}, #{decision}, #{note}, #{createdAt})
      """)
  int insertAction(
      @Param("bookingId") long bookingId,
      @Param("step") int step,
      @Param("actorId") String actorId,
      @Param("actorRole") String actorRole,
      @Param("decision") String decision,
      @Param("note") String note,
      @Param("createdAt") LocalDateTime createdAt);

  @Select("""
      SELECT approval_step AS approvalStep, actor_external_user_id AS actorExternalUserId,
             actor_role AS actorRole, decision, review_note AS reviewNote, created_at AS createdAt
      FROM booking_approval_action
      WHERE booking_id = (SELECT id FROM booking_reservation WHERE booking_no = #{bookingNo})
      ORDER BY approval_step, id
      LIMIT 20
      """)
  List<BookingApprovalAction> findByBookingNo(@Param("bookingNo") String bookingNo);
}
