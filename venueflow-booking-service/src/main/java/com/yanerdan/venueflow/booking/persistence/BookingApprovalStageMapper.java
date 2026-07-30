package com.yanerdan.venueflow.booking.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BookingApprovalStageMapper {

  @Insert(
      """
      INSERT INTO booking_approval_stage_snapshot
        (booking_id, stage_order, stage_name, approver_external_user_id, stage_status)
      VALUES
        (#{bookingId}, #{stage.stageOrder}, #{stage.stageName},
         #{stage.approverExternalUserId}, 'PENDING')
      """)
  int insert(
      @Param("bookingId") long bookingId, @Param("stage") BookingApprovalStageSnapshot stage);

  @Select(
      """
      SELECT stage_order, stage_name, approver_external_user_id, stage_status, decided_at
      FROM booking_approval_stage_snapshot
      WHERE booking_id = #{bookingId}
      ORDER BY stage_order
      """)
  List<BookingApprovalStageSnapshot> findByBookingId(@Param("bookingId") long bookingId);

  @Select(
      """
      SELECT s.stage_order, s.stage_name, s.approver_external_user_id, s.stage_status, s.decided_at
      FROM booking_approval_stage_snapshot s
      JOIN booking_reservation b ON b.id = s.booking_id
      WHERE b.booking_no = #{bookingNo}
      ORDER BY s.stage_order
      """)
  List<BookingApprovalStageSnapshot> findByBookingNo(@Param("bookingNo") String bookingNo);

  @Select("SELECT COUNT(*) FROM booking_approval_stage_snapshot WHERE booking_id = #{bookingId}")
  int count(@Param("bookingId") long bookingId);

  @Update(
      """
      UPDATE booking_approval_stage_snapshot
      SET stage_status = #{status}, decided_at = #{decidedAt}
      WHERE booking_id = #{bookingId} AND stage_order = #{stageOrder}
      """)
  int decide(
      @Param("bookingId") long bookingId,
      @Param("stageOrder") int stageOrder,
      @Param("status") String status,
      @Param("decidedAt") LocalDateTime decidedAt);
}
