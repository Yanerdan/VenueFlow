package com.yanerdan.venueflow.resource.catalog.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanerdan.venueflow.resource.catalog.domain.ApprovalMode;
import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import com.yanerdan.venueflow.resource.catalog.persistence.entity.ResourceEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ResourceMapper extends BaseMapper<ResourceEntity> {

  long countResources(@Param("categoryId") Long categoryId, @Param("status") ResourceStatus status);

  List<ResourceEntity> selectResourcePage(
      @Param("categoryId") Long categoryId,
      @Param("status") ResourceStatus status,
      @Param("offset") long offset,
      @Param("limit") int limit);

  int updateStatusIfVersionMatches(
      @Param("resourceId") Long resourceId,
      @Param("currentStatus") ResourceStatus currentStatus,
      @Param("targetStatus") ResourceStatus targetStatus,
      @Param("expectedVersion") Long expectedVersion);

  int updateOwnershipIfVersionMatches(
      @Param("resourceId") Long resourceId,
      @Param("ownerDepartment") String ownerDepartment,
      @Param("approverExternalUserId") String approverExternalUserId,
      @Param("approvalMode") ApprovalMode approvalMode,
      @Param("finalApproverExternalUserId") String finalApproverExternalUserId,
      @Param("expectedVersion") Long expectedVersion);

  int updateBookingRulesIfVersionMatches(
      @Param("resourceId") Long resourceId,
      @Param("bookingNotice") String bookingNotice,
      @Param("minAdvanceHours") Integer minAdvanceHours,
      @Param("maxAdvanceDays") Integer maxAdvanceDays,
      @Param("maxDurationMinutes") Integer maxDurationMinutes,
      @Param("expectedVersion") Long expectedVersion);

  int updateFactsIfVersionMatches(
      @Param("resourceId") Long resourceId,
      @Param("categoryId") Long categoryId,
      @Param("name") String name,
      @Param("description") String description,
      @Param("location") String location,
      @Param("capacity") Integer capacity,
      @Param("expectedVersion") Long expectedVersion);

  ResourceEntity selectByIdForUpdate(@Param("resourceId") Long resourceId);
}
