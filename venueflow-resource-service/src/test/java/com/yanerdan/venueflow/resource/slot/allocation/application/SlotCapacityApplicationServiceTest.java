package com.yanerdan.venueflow.resource.slot.allocation.application;

import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.ALLOCATION_OPERATION_CONFLICT;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.INSUFFICIENT_SLOT_CAPACITY;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RELEASE_EXCEEDS_OCCUPIED_CAPACITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import com.yanerdan.venueflow.resource.catalog.error.CatalogException;
import com.yanerdan.venueflow.resource.catalog.persistence.entity.ResourceEntity;
import com.yanerdan.venueflow.resource.catalog.persistence.mapper.ResourceMapper;
import com.yanerdan.venueflow.resource.slot.allocation.domain.SlotAllocationOperationType;
import com.yanerdan.venueflow.resource.slot.allocation.persistence.entity.ResourceSlotAllocationEntity;
import com.yanerdan.venueflow.resource.slot.allocation.persistence.mapper.ResourceSlotAllocationMapper;
import com.yanerdan.venueflow.resource.slot.domain.ResourceSlotStatus;
import com.yanerdan.venueflow.resource.slot.persistence.entity.ResourceSlotEntity;
import com.yanerdan.venueflow.resource.slot.persistence.mapper.ResourceSlotMapper;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlotCapacityApplicationServiceTest {

  @Mock private ResourceMapper resourceMapper;
  @Mock private ResourceSlotMapper resourceSlotMapper;
  @Mock private ResourceSlotAllocationMapper allocationMapper;

  private SlotCapacityApplicationService service;

  @BeforeEach
  void setUp() {
    service =
        new SlotCapacityApplicationService(resourceMapper, resourceSlotMapper, allocationMapper);
  }

  @Test
  void allocatesWithinStaticCapacityAndPersistsAuditableOperation() {
    ResourceSlotEntity slot = slot(3);
    when(resourceSlotMapper.selectByIdForUpdate(500L)).thenReturn(slot);
    when(allocationMapper.selectByOperationId("op-1")).thenReturn(null);
    when(resourceMapper.selectById(100L)).thenReturn(resource(10));
    when(allocationMapper.insert(any(ResourceSlotAllocationEntity.class))).thenReturn(1);
    when(resourceSlotMapper.updateAllocatedQuantity(500L, 5)).thenReturn(1);

    SlotCapacityChangeResult result =
        service.allocate(new SlotCapacityChangeCommand(500L, "op-1", 2));

    assertThat(result.operationType()).isEqualTo(SlotAllocationOperationType.ALLOCATE);
    assertThat(result.capacity().occupiedQuantity()).isEqualTo(5);
    assertThat(result.capacity().availableQuantity()).isEqualTo(5);
    ArgumentCaptor<ResourceSlotAllocationEntity> operationCaptor =
        ArgumentCaptor.forClass(ResourceSlotAllocationEntity.class);
    verify(allocationMapper).insert(operationCaptor.capture());
    assertThat(operationCaptor.getValue().getOccupiedQuantityAfter()).isEqualTo(5);
    assertThat(operationCaptor.getValue().getRequestFingerprint()).hasSize(64);
  }

  @Test
  void replaysIdenticalOperationWithoutChangingOccupiedQuantity() {
    ResourceSlotEntity slot = slot(3);
    ResourceSlotAllocationEntity original = new ResourceSlotAllocationEntity();
    AtomicReference<ResourceSlotAllocationEntity> inserted = new AtomicReference<>();
    when(resourceSlotMapper.selectByIdForUpdate(500L)).thenReturn(slot);
    when(resourceSlotMapper.selectById(500L)).thenReturn(slot);
    when(resourceMapper.selectById(100L)).thenReturn(resource(10));
    when(allocationMapper.selectByOperationId("op-1")).thenReturn(null);
    when(allocationMapper.insert(any(ResourceSlotAllocationEntity.class)))
        .thenAnswer(
            invocation -> {
              ResourceSlotAllocationEntity entity = invocation.getArgument(0);
              original.setSlotId(entity.getSlotId());
              original.setOperationId(entity.getOperationId());
              original.setOperationType(entity.getOperationType());
              original.setQuantity(entity.getQuantity());
              original.setRequestFingerprint(entity.getRequestFingerprint());
              original.setOccupiedQuantityAfter(entity.getOccupiedQuantityAfter());
              inserted.set(original);
              return 1;
            });
    when(resourceSlotMapper.updateAllocatedQuantity(500L, 5)).thenReturn(1);

    service.allocate(new SlotCapacityChangeCommand(500L, "op-1", 2));
    when(allocationMapper.selectByOperationId("op-1")).thenReturn(inserted.get());

    SlotCapacityChangeResult replay =
        service.allocate(new SlotCapacityChangeCommand(500L, "op-1", 2));

    assertThat(replay.capacity().occupiedQuantity()).isEqualTo(5);
    verify(resourceSlotMapper).updateAllocatedQuantity(500L, 5);
  }

  @Test
  void rejectsOversubscriptionAndNegativeRelease() {
    when(resourceSlotMapper.selectByIdForUpdate(500L)).thenReturn(slot(9));
    when(allocationMapper.selectByOperationId(any())).thenReturn(null);
    when(resourceMapper.selectById(100L)).thenReturn(resource(10));

    assertThatThrownBy(() -> service.allocate(new SlotCapacityChangeCommand(500L, "op-2", 2)))
        .isInstanceOfSatisfying(
            CatalogException.class,
            exception -> assertThat(exception.getCode()).isEqualTo(INSUFFICIENT_SLOT_CAPACITY));

    assertThatThrownBy(() -> service.release(new SlotCapacityChangeCommand(500L, "op-3", 10)))
        .isInstanceOfSatisfying(
            CatalogException.class,
            exception ->
                assertThat(exception.getCode()).isEqualTo(RELEASE_EXCEEDS_OCCUPIED_CAPACITY));
  }

  @Test
  void rejectsOperationIdReuseWithDifferentRequestFacts() {
    ResourceSlotAllocationEntity existing = new ResourceSlotAllocationEntity();
    existing.setSlotId(500L);
    existing.setOperationId("op-1");
    existing.setOperationType(SlotAllocationOperationType.ALLOCATE);
    existing.setQuantity(2);
    existing.setRequestFingerprint("different-fingerprint");
    existing.setOccupiedQuantityAfter(5);
    when(resourceSlotMapper.selectByIdForUpdate(500L)).thenReturn(slot(3));
    when(allocationMapper.selectByOperationId("op-1")).thenReturn(existing);

    assertThatThrownBy(() -> service.allocate(new SlotCapacityChangeCommand(500L, "op-1", 2)))
        .isInstanceOfSatisfying(
            CatalogException.class,
            exception -> assertThat(exception.getCode()).isEqualTo(ALLOCATION_OPERATION_CONFLICT));
  }

  private static ResourceEntity resource(int capacity) {
    ResourceEntity entity = new ResourceEntity();
    entity.setId(100L);
    entity.setCapacity(capacity);
    entity.setStatus(ResourceStatus.ACTIVE);
    return entity;
  }

  private static ResourceSlotEntity slot(int allocatedQuantity) {
    ResourceSlotEntity entity = new ResourceSlotEntity();
    entity.setId(500L);
    entity.setResourceId(100L);
    entity.setStatus(ResourceSlotStatus.OPEN);
    entity.setAllocatedQuantity(allocatedQuantity);
    entity.setCreatedAt(LocalDateTime.of(2026, 7, 22, 16, 0));
    entity.setUpdatedAt(LocalDateTime.of(2026, 7, 22, 16, 0));
    return entity;
  }
}
