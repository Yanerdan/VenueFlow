package com.yanerdan.venueflow.resource.slot.application;

import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.CATALOG_PERSISTENCE_ERROR;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.INVALID_RESOURCE_SLOT_STATUS_TRANSITION;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.INVALID_SLOT_TIME_RANGE;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.OPTIMISTIC_LOCK_CONFLICT;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_NOT_ACTIVE_FOR_SLOT;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_NOT_FOUND;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_SLOT_NOT_FOUND;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_SLOT_TIME_OVERLAP;

import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import com.yanerdan.venueflow.resource.catalog.error.CatalogException;
import com.yanerdan.venueflow.resource.catalog.persistence.entity.ResourceEntity;
import com.yanerdan.venueflow.resource.catalog.persistence.mapper.ResourceMapper;
import com.yanerdan.venueflow.resource.slot.domain.ResourceSlotStatus;
import com.yanerdan.venueflow.resource.slot.domain.ResourceSlotStatusTransitions;
import com.yanerdan.venueflow.resource.slot.persistence.entity.ResourceSlotEntity;
import com.yanerdan.venueflow.resource.slot.persistence.mapper.ResourceSlotMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("persistence")
public class ResourceSlotApplicationService {

  private final ResourceMapper resourceMapper;
  private final ResourceSlotMapper resourceSlotMapper;

  public ResourceSlotApplicationService(
      ResourceMapper resourceMapper, ResourceSlotMapper resourceSlotMapper) {
    this.resourceMapper = resourceMapper;
    this.resourceSlotMapper = resourceSlotMapper;
  }

  @Transactional
  public ResourceSlotResult createSlot(CreateResourceSlotCommand command) {
    validateTimeRange(command.startAt(), command.endAt());
    ResourceEntity resource = lockResource(command.resourceId());
    requireActive(resource);

    LocalDateTime startAt = utc(command.startAt());
    LocalDateTime endAt = utc(command.endAt());

    try {
      if (resourceSlotMapper.countOverlappingSlots(command.resourceId(), startAt, endAt) > 0) {
        throw slotOverlap(command.resourceId(), command.startAt(), command.endAt());
      }

      ResourceSlotEntity entity = new ResourceSlotEntity();
      entity.setResourceId(command.resourceId());
      entity.setStartAt(startAt);
      entity.setEndAt(endAt);
      entity.setStatus(ResourceSlotStatus.OPEN);
      entity.setVersion(1L);

      if (resourceSlotMapper.insert(entity) != 1) {
        throw persistenceFailure("Resource Slot insert did not affect exactly one row", null);
      }

      ResourceSlotEntity persisted = resourceSlotMapper.selectById(entity.getId());
      if (persisted == null) {
        throw persistenceFailure("Created Resource Slot could not be reloaded", null);
      }
      return ResourceSlotResult.from(persisted);
    } catch (CatalogException exception) {
      throw exception;
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resource Slot could not be persisted", exception);
    }
  }

  @Transactional(readOnly = true)
  public ResourceSlotResult getSlot(Long slotId) {
    if (slotId == null || slotId <= 0) {
      throw new IllegalArgumentException("slotId must be positive");
    }
    try {
      ResourceSlotEntity entity = resourceSlotMapper.selectById(slotId);
      if (entity == null) {
        throw slotNotFound(slotId);
      }
      return ResourceSlotResult.from(entity);
    } catch (CatalogException exception) {
      throw exception;
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resource Slot could not be queried", exception);
    }
  }

  @Transactional(readOnly = true)
  public ResourceSlotPageResult listSlots(ResourceSlotPageQuery query) {
    validateTimeRange(query.from(), query.to());
    requireResource(query.resourceId());
    LocalDateTime from = utc(query.from());
    LocalDateTime to = utc(query.to());
    try {
      long totalElements =
          resourceSlotMapper.countSlotsIntersectingWindow(query.resourceId(), from, to);
      if (totalElements == 0) {
        return ResourceSlotPageResult.of(List.of(), query, 0);
      }
      List<ResourceSlotResult> items =
          resourceSlotMapper
              .selectSlotPage(query.resourceId(), from, to, query.offset(), query.size())
              .stream()
              .map(ResourceSlotResult::from)
              .toList();
      return ResourceSlotPageResult.of(items, query, totalElements);
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resource Slot page could not be queried", exception);
    }
  }

  @Transactional
  public ResourceSlotResult changeSlotStatus(ChangeResourceSlotStatusCommand command) {
    ResourceSlotEntity current = findSlot(command.slotId());
    if (!command.expectedVersion().equals(current.getVersion())) {
      throw optimisticLockConflict(
          command.slotId(), command.expectedVersion(), current.getVersion());
    }
    if (!ResourceSlotStatusTransitions.canTransition(current.getStatus(), command.targetStatus())) {
      throw new CatalogException(
          INVALID_RESOURCE_SLOT_STATUS_TRANSITION,
          "Resource Slot status transition is not allowed",
          Map.of(
              "slotId", command.slotId(),
              "currentStatus", current.getStatus().name(),
              "targetStatus", command.targetStatus().name()),
          null);
    }

    try {
      int updated =
          resourceSlotMapper.updateStatusIfVersionMatches(
              command.slotId(),
              current.getStatus(),
              command.targetStatus(),
              command.expectedVersion());
      if (updated != 1) {
        throw resolveFailedConditionalUpdate(command.slotId(), command.expectedVersion());
      }
      ResourceSlotEntity updatedEntity = resourceSlotMapper.selectById(command.slotId());
      if (updatedEntity == null) {
        throw slotNotFound(command.slotId());
      }
      return ResourceSlotResult.from(updatedEntity);
    } catch (CatalogException exception) {
      throw exception;
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resource Slot status could not be updated", exception);
    }
  }

  private ResourceEntity lockResource(Long resourceId) {
    try {
      ResourceEntity resource = resourceMapper.selectByIdForUpdate(resourceId);
      if (resource == null) {
        throw resourceNotFound(resourceId);
      }
      return resource;
    } catch (CatalogException exception) {
      throw exception;
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resource could not be locked for slot creation", exception);
    }
  }

  private void requireResource(Long resourceId) {
    try {
      if (resourceMapper.selectById(resourceId) == null) {
        throw resourceNotFound(resourceId);
      }
    } catch (CatalogException exception) {
      throw exception;
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resource could not be queried for slot listing", exception);
    }
  }

  private static void requireActive(ResourceEntity resource) {
    if (resource.getStatus() != ResourceStatus.ACTIVE) {
      throw new CatalogException(
          RESOURCE_NOT_ACTIVE_FOR_SLOT,
          "Resource must be ACTIVE before slots can be created",
          Map.of("resourceId", resource.getId(), "status", resource.getStatus().name()),
          null);
    }
  }

  private ResourceSlotEntity findSlot(Long slotId) {
    try {
      ResourceSlotEntity entity = resourceSlotMapper.selectById(slotId);
      if (entity == null) {
        throw slotNotFound(slotId);
      }
      return entity;
    } catch (CatalogException exception) {
      throw exception;
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resource Slot could not be queried", exception);
    }
  }

  private CatalogException resolveFailedConditionalUpdate(Long slotId, Long expectedVersion) {
    ResourceSlotEntity latest = findSlot(slotId);
    return optimisticLockConflict(slotId, expectedVersion, latest.getVersion());
  }

  private static void validateTimeRange(Instant startAt, Instant endAt) {
    if (!endAt.isAfter(startAt)) {
      throw new CatalogException(
          INVALID_SLOT_TIME_RANGE,
          "Resource Slot endAt must be after startAt",
          Map.of("startAt", startAt.toString(), "endAt", endAt.toString()),
          null);
    }
  }

  private static LocalDateTime utc(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private static CatalogException resourceNotFound(Long resourceId) {
    return new CatalogException(
        RESOURCE_NOT_FOUND, "Resource was not found", Map.of("resourceId", resourceId), null);
  }

  private static CatalogException slotNotFound(Long slotId) {
    return new CatalogException(
        RESOURCE_SLOT_NOT_FOUND, "Resource Slot was not found", Map.of("slotId", slotId), null);
  }

  private static CatalogException slotOverlap(Long resourceId, Instant startAt, Instant endAt) {
    return new CatalogException(
        RESOURCE_SLOT_TIME_OVERLAP,
        "Resource Slot overlaps an existing slot",
        Map.of("resourceId", resourceId, "startAt", startAt.toString(), "endAt", endAt.toString()),
        null);
  }

  private static CatalogException optimisticLockConflict(
      Long slotId, Long expectedVersion, Long actualVersion) {
    return new CatalogException(
        OPTIMISTIC_LOCK_CONFLICT,
        "Resource Slot version is stale",
        Map.of(
            "slotId", slotId,
            "expectedVersion", expectedVersion,
            "actualVersion", actualVersion),
        null);
  }

  private static CatalogException persistenceFailure(String internalReason, Throwable cause) {
    return new CatalogException(
        CATALOG_PERSISTENCE_ERROR,
        "Resource catalog operation failed",
        Map.of("reason", internalReason),
        cause);
  }
}
