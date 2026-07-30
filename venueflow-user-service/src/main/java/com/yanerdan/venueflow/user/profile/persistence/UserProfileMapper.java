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
            campus_id,
            identity_type,
            department,
            phone,
            email,
            account_status,
            booking_eligibility,
            version
        )
        VALUES
        (
            #{externalUserId},
            #{displayName},
            #{campusId},
            #{identityType},
            #{department},
            #{phone},
            #{email},
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
            campus_id AS campusId,
            identity_type AS identityType,
            department,
            phone,
            email,
            authoritative_source AS authoritativeSource,
            organization_external_key AS organizationExternalKey,
            directory_synced_at AS directorySyncedAt,
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

  @Update(
      """
        UPDATE user_profile
        SET display_name = #{displayName},
            campus_id = #{campusId},
            identity_type = #{identityType},
            department = #{department},
            phone = #{phone},
            email = #{email},
            version = version + 1,
            updated_at = CURRENT_TIMESTAMP(6)
        WHERE id = #{id}
          AND version = #{expectedVersion}
        """)
  int updateCampusProfile(
      @Param("id") long id,
      @Param("displayName") String displayName,
      @Param("campusId") String campusId,
      @Param("identityType") String identityType,
      @Param("department") String department,
      @Param("phone") String phone,
      @Param("email") String email,
      @Param("expectedVersion") long expectedVersion);

  @Select(
      """
        SELECT id, external_user_id AS externalUserId, display_name AS displayName,
               campus_id AS campusId, identity_type AS identityType, department, phone, email,
               authoritative_source AS authoritativeSource,
               organization_external_key AS organizationExternalKey,
               directory_synced_at AS directorySyncedAt,
               account_status AS accountStatus, booking_eligibility AS bookingEligibility,
               version, created_at AS createdAt, updated_at AS updatedAt
        FROM user_profile
        WHERE #{keyword} IS NULL
           OR display_name LIKE CONCAT('%', #{keyword}, '%')
           OR campus_id LIKE CONCAT('%', #{keyword}, '%')
           OR department LIKE CONCAT('%', #{keyword}, '%')
        ORDER BY created_at DESC, id DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
  java.util.List<UserProfileEntity> selectPage(
      @Param("keyword") String keyword, @Param("offset") long offset, @Param("limit") int limit);

  @Select(
      """
        SELECT COUNT(*)
        FROM user_profile
        WHERE #{keyword} IS NULL
           OR display_name LIKE CONCAT('%', #{keyword}, '%')
           OR campus_id LIKE CONCAT('%', #{keyword}, '%')
           OR department LIKE CONCAT('%', #{keyword}, '%')
        """)
  long countPage(@Param("keyword") String keyword);
}
