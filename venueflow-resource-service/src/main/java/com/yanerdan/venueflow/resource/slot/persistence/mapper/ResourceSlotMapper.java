package com.yanerdan.venueflow.resource.slot.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanerdan.venueflow.resource.slot.domain.ResourceSlotStatus;
import com.yanerdan.venueflow.resource.slot.persistence.entity.ResourceSlotEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ResourceSlotMapper extends BaseMapper<ResourceSlotEntity> {

  long countOverlappingSlots(
      @Param("resourceId") Long resourceId,
      @Param("startAt") LocalDateTime startAt,
      @Param("endAt") LocalDateTime endAt);

  long countSlotsIntersectingWindow(
      @Param("resourceId") Long resourceId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to);

  List<ResourceSlotEntity> selectSlotPage(
      @Param("resourceId") Long resourceId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("offset") long offset,
      @Param("limit") int limit);

  int updateStatusIfVersionMatches(
      @Param("slotId") Long slotId,
      @Param("currentStatus") ResourceSlotStatus currentStatus,
      @Param("targetStatus") ResourceSlotStatus targetStatus,
      @Param("expectedVersion") Long expectedVersion);
}
