package com.yanerdan.venueflow.user.profile.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserProfileMapper extends BaseMapper<UserProfileEntity> {

  @Insert(
      """
        INSERT INTO user_profile
        (
            external_user_id,
            display_name,
            account_status,
            booking_eligibility,
            version
        )
        VALUES
        (
            #{externalUserId},
            #{displayName},
            #{accountStatus},
            #{bookingEligibility},
            #{version}
        )
        """)
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  int insertProfile(UserProfileEntity entity);

  @Select(
      """
        SELECT
            id,
            external_user_id AS externalUserId,
            display_name AS displayName,
            account_status AS accountStatus,
            booking_eligibility AS bookingEligibility,
            version,
            created_at AS createdAt,
            updated_at AS updatedAt
        FROM user_profile
        WHERE external_user_id = #{externalUserId}
        """)
  UserProfileEntity selectByExternalUserId(@Param("externalUserId") String externalUserId);

  @Update(
      """
        UPDATE user_profile
        SET
            display_name = #{displayName},
            version = version + 1,
            updated_at = CURRENT_TIMESTAMP(6)
        WHERE id = #{id}
          AND version = #{expectedVersion}
        """)
  int updateDisplayName(
      @Param("id") long id,
      @Param("displayName") String displayName,
      @Param("expectedVersion") long expectedVersion);

  @Update(
      """
        UPDATE user_profile
        SET
            account_status = #{accountStatus},
            version = version + 1,
            updated_at = CURRENT_TIMESTAMP(6)
        WHERE id = #{id}
          AND version = #{expectedVersion}
        """)
  int updateAccountStatus(
      @Param("id") long id,
      @Param("accountStatus") String accountStatus,
      @Param("expectedVersion") long expectedVersion);

  @Update(
      """
        UPDATE user_profile
        SET
            booking_eligibility = #{bookingEligibility},
            version = version + 1,
            updated_at = CURRENT_TIMESTAMP(6)
        WHERE id = #{id}
          AND version = #{expectedVersion}
        """)
  int updateBookingEligibility(
      @Param("id") long id,
      @Param("bookingEligibility") String bookingEligibility,
      @Param("expectedVersion") long expectedVersion);
}
