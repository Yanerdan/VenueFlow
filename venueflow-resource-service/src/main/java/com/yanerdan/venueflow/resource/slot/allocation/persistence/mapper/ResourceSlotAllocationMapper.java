package com.yanerdan.venueflow.resource.slot.allocation.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yanerdan.venueflow.resource.slot.allocation.persistence.entity.ResourceSlotAllocationEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ResourceSlotAllocationMapper extends BaseMapper<ResourceSlotAllocationEntity> {

  long countBySlotId(@Param("slotId") Long slotId);

  List<ResourceSlotAllocationEntity> selectPageBySlotId(
      @Param("slotId") Long slotId, @Param("offset") long offset, @Param("limit") int limit);

  ResourceSlotAllocationEntity selectByOperationId(@Param("operationId") String operationId);
}
