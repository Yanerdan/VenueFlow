package com.yanerdan.venueflow.resource.catalog.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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

  ResourceEntity selectByIdForUpdate(@Param("resourceId") Long resourceId);
}
