package com.yanerdan.venueflow.resource.slot.allocation.application;

import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.ALLOCATION_OPERATION_CONFLICT;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.CATALOG_PERSISTENCE_ERROR;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.INSUFFICIENT_SLOT_CAPACITY;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RELEASE_EXCEEDS_OCCUPIED_CAPACITY;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_NOT_FOUND;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_SLOT_NOT_FOUND;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_SLOT_NOT_OPEN_FOR_ALLOCATION;

import com.yanerdan.venueflow.resource.catalog.error.CatalogException;
import com.yanerdan.venueflow.resource.catalog.persistence.entity.ResourceEntity;
import com.yanerdan.venueflow.resource.catalog.persistence.mapper.ResourceMapper;
import com.yanerdan.venueflow.resource.slot.allocation.domain.SlotAllocationOperationType;
import com.yanerdan.venueflow.resource.slot.allocation.persistence.entity.ResourceSlotAllocationEntity;
import com.yanerdan.venueflow.resource.slot.allocation.persistence.mapper.ResourceSlotAllocationMapper;
import com.yanerdan.venueflow.resource.slot.domain.ResourceSlotStatus;
import com.yanerdan.venueflow.resource.slot.persistence.entity.ResourceSlotEntity;
import com.yanerdan.venueflow.resource.slot.persistence.mapper.ResourceSlotMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("persistence")
public class SlotCapacityApplicationService {

  private final ResourceMapper resourceMapper;
  private final ResourceSlotMapper resourceSlotMapper;
  private final ResourceSlotAllocationMapper allocationMapper;

  public SlotCapacityApplicationService(
      ResourceMapper resourceMapper,
      ResourceSlotMapper resourceSlotMapper,
      ResourceSlotAllocationMapper allocationMapper) {
    this.resourceMapper = resourceMapper;
    this.resourceSlotMapper = resourceSlotMapper;
    this.allocationMapper = allocationMapper;
  }

  @Transactional
  public SlotCapacityChangeResult allocate(SlotCapacityChangeCommand command) {
    return change(command, SlotAllocationOperationType.ALLOCATE);
  }

  @Transactional
  public SlotCapacityChangeResult release(SlotCapacityChangeCommand command) {
    return change(command, SlotAllocationOperationType.RELEASE);
  }

  @Transactional(readOnly = true)
  public SlotCapacityResult getCapacity(Long slotId) {
    if (slotId == null || slotId <= 0) {
      throw new IllegalArgumentException("slotId must be positive");
    }
    try {
      return capacityOf(requireSlot(slotId));
    } catch (CatalogException exception) {
      throw exception;
    } catch (DataAccessException exception) {
      throw persistenceFailure("Slot capacity could not be queried", exception);
    }
  }

  @Transactional(readOnly = true)
  public SlotAllocationOperationPageResult listOperations(SlotAllocationOperationPageQuery query) {
    try {
      requireSlot(query.slotId());
      long totalElements = allocationMapper.countBySlotId(query.slotId());
      List<SlotAllocationOperationResult> items =
          allocationMapper.selectPageBySlotId(query.slotId(), query.offset(), query.size()).stream()
              .map(SlotAllocationOperationResult::from)
              .toList();
      return SlotAllocationOperationPageResult.of(items, query, totalElements);
    } catch (CatalogException exception) {
      throw exception;
    } catch (DataAccessException exception) {
      throw persistenceFailure("Slot allocation operations could not be queried", exception);
    }
  }

  private SlotCapacityChangeResult change(
      SlotCapacityChangeCommand command, SlotAllocationOperationType operationType) {
    try {
      ResourceSlotEntity slot = lockSlot(command.slotId());
      String fingerprint = fingerprint(command, operationType);
      ResourceSlotAllocationEntity existing =
          allocationMapper.selectByOperationId(command.operationId());
      if (existing != null) {
        return replay(existing, command, operationType, fingerprint);
      }
      if (slot.getStatus() != ResourceSlotStatus.OPEN) {
        throw new CatalogException(
            RESOURCE_SLOT_NOT_OPEN_FOR_ALLOCATION,
            "Resource Slot must be OPEN for capacity changes",
            Map.of("slotId", command.slotId(), "status", slot.getStatus().name()),
            null);
      }

      int occupied = allocated(slot);
      int nextOccupied = nextOccupied(slot, command.quantity(), operationType, occupied);
      ResourceSlotAllocationEntity operation = new ResourceSlotAllocationEntity();
      operation.setSlotId(slot.getId());
      operation.setOperationId(command.operationId());
      operation.setOperationType(operationType);
      operation.setQuantity(command.quantity());
      operation.setRequestFingerprint(fingerprint);
      operation.setOccupiedQuantityAfter(nextOccupied);

      try {
        if (allocationMapper.insert(operation) != 1) {
          throw persistenceFailure("Slot allocation insert did not affect exactly one row", null);
        }
      } catch (DuplicateKeyException exception) {
        ResourceSlotAllocationEntity duplicate =
            allocationMapper.selectByOperationId(command.operationId());
        if (duplicate != null) {
          return replay(duplicate, command, operationType, fingerprint);
        }
        throw persistenceFailure("Slot allocation operation id could not be persisted", exception);
      }

      if (resourceSlotMapper.updateAllocatedQuantity(slot.getId(), nextOccupied) != 1) {
        throw persistenceFailure("Slot occupancy update did not affect exactly one row", null);
      }
      return result(operation, capacityOf(slot, nextOccupied));
    } catch (CatalogException exception) {
      throw exception;
    } catch (DataAccessException exception) {
      throw persistenceFailure("Slot capacity could not be changed", exception);
    }
  }

  private SlotCapacityChangeResult replay(
      ResourceSlotAllocationEntity existing,
      SlotCapacityChangeCommand command,
      SlotAllocationOperationType operationType,
      String fingerprint) {
    if (!existing.getSlotId().equals(command.slotId())
        || existing.getOperationType() != operationType
        || !existing.getRequestFingerprint().equals(fingerprint)) {
      throw new CatalogException(
          ALLOCATION_OPERATION_CONFLICT,
          "operationId has already been used with different request facts",
          Map.of("operationId", command.operationId()),
          null);
    }
    ResourceSlotEntity slot = requireSlot(command.slotId());
    return result(existing, capacityOf(slot, existing.getOccupiedQuantityAfter()));
  }

  private int nextOccupied(
      ResourceSlotEntity slot, int quantity, SlotAllocationOperationType type, int occupied) {
    ResourceEntity resource = requireResource(slot.getResourceId());
    if (type == SlotAllocationOperationType.RELEASE) {
      if (quantity > occupied) {
        throw new CatalogException(
            RELEASE_EXCEEDS_OCCUPIED_CAPACITY,
            "Release quantity exceeds occupied capacity",
            Map.of("slotId", slot.getId(), "occupiedQuantity", occupied, "quantity", quantity),
            null);
      }
      return occupied - quantity;
    }

    long requested = (long) occupied + quantity;
    if (requested > resource.getCapacity()) {
      throw new CatalogException(
          INSUFFICIENT_SLOT_CAPACITY,
          "Requested quantity exceeds available slot capacity",
          Map.of(
              "slotId",
              slot.getId(),
              "staticCapacity",
              resource.getCapacity(),
              "occupiedQuantity",
              occupied,
              "quantity",
              quantity),
          null);
    }
    return (int) requested;
  }

  private SlotCapacityChangeResult result(
      ResourceSlotAllocationEntity operation, SlotCapacityResult capacity) {
    return new SlotCapacityChangeResult(
        operation.getOperationId(),
        operation.getOperationType(),
        operation.getQuantity(),
        capacity);
  }

  private SlotCapacityResult capacityOf(ResourceSlotEntity slot) {
    return capacityOf(slot, allocated(slot));
  }

  private SlotCapacityResult capacityOf(ResourceSlotEntity slot, int occupied) {
    ResourceEntity resource = requireResource(slot.getResourceId());
    return new SlotCapacityResult(
        slot.getId(),
        resource.getCapacity(),
        occupied,
        resource.getCapacity() - occupied,
        slot.getStatus());
  }

  private ResourceSlotEntity lockSlot(Long slotId) {
    ResourceSlotEntity slot = resourceSlotMapper.selectByIdForUpdate(slotId);
    if (slot == null) {
      throw slotNotFound(slotId);
    }
    return slot;
  }

  private ResourceSlotEntity requireSlot(Long slotId) {
    ResourceSlotEntity slot = resourceSlotMapper.selectById(slotId);
    if (slot == null) {
      throw slotNotFound(slotId);
    }
    return slot;
  }

  private ResourceEntity requireResource(Long resourceId) {
    ResourceEntity resource = resourceMapper.selectById(resourceId);
    if (resource == null) {
      throw new CatalogException(
          RESOURCE_NOT_FOUND, "Resource was not found", Map.of("resourceId", resourceId), null);
    }
    return resource;
  }

  private static int allocated(ResourceSlotEntity slot) {
    return slot.getAllocatedQuantity() == null ? 0 : slot.getAllocatedQuantity();
  }

  private static CatalogException slotNotFound(Long slotId) {
    return new CatalogException(
        RESOURCE_SLOT_NOT_FOUND, "Resource Slot was not found", Map.of("slotId", slotId), null);
  }

  private static String fingerprint(
      SlotCapacityChangeCommand command, SlotAllocationOperationType operationType) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(
                  (command.slotId() + "|" + operationType.name() + "|" + command.quantity())
                      .getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is required by the JDK", exception);
    }
  }

  private static CatalogException persistenceFailure(String reason, Throwable cause) {
    return new CatalogException(
        CATALOG_PERSISTENCE_ERROR,
        "Resource catalog operation failed",
        Map.of("reason", reason),
        cause);
  }
}
