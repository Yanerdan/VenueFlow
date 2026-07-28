package com.yanerdan.venueflow.resource.catalog.application;

import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.CATALOG_PERSISTENCE_ERROR;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.CATEGORY_ALREADY_EXISTS;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.CATEGORY_NOT_FOUND;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.INVALID_RESOURCE_STATUS_TRANSITION;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.OPTIMISTIC_LOCK_CONFLICT;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_NOT_FOUND;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_NUMBER_ALREADY_EXISTS;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yanerdan.venueflow.resource.cache.ResourceDetailCache;
import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatusTransitions;
import com.yanerdan.venueflow.resource.catalog.error.CatalogException;
import com.yanerdan.venueflow.resource.catalog.persistence.entity.ResourceCategoryEntity;
import com.yanerdan.venueflow.resource.catalog.persistence.entity.ResourceEntity;
import com.yanerdan.venueflow.resource.catalog.persistence.mapper.ResourceCategoryMapper;
import com.yanerdan.venueflow.resource.catalog.persistence.mapper.ResourceMapper;
import com.yanerdan.venueflow.resource.event.ResourceChangeRecorder;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("persistence")
public class CatalogApplicationService {

  private final ResourceMapper resourceMapper;
  private final ResourceCategoryMapper categoryMapper;
  private final ResourceDetailCache resourceCache;
  private final ResourceChangeRecorder changeRecorder;

  @Autowired
  public CatalogApplicationService(
      ResourceCategoryMapper categoryMapper,
      ResourceMapper resourceMapper,
      ResourceDetailCache resourceCache,
      ResourceChangeRecorder changeRecorder) {
    this.categoryMapper = categoryMapper;
    this.resourceMapper = resourceMapper;
    this.resourceCache = resourceCache;
    this.changeRecorder = changeRecorder;
  }

  CatalogApplicationService(ResourceCategoryMapper categoryMapper, ResourceMapper resourceMapper) {
    this(
        categoryMapper,
        resourceMapper,
        new ResourceDetailCache() {
          @Override
          public ResourceResult get(Long resourceId, Supplier<ResourceResult> loader) {
            return loader.get();
          }

          @Override
          public void evictAfterCommit(Long resourceId) {}
        },
        resource -> {});
  }

  @Transactional
  public CategoryResult createCategory(CreateCategoryCommand command) {
    ResourceCategoryEntity entity = new ResourceCategoryEntity();
    entity.setCode(command.code());
    entity.setName(command.name());

    try {
      int insertedRows = categoryMapper.insert(entity);

      if (insertedRows != 1) {
        throw new IllegalStateException("Expected one Resource Category row to be inserted");
      }
    } catch (DuplicateKeyException exception) {
      throw new CatalogException(
          CATEGORY_ALREADY_EXISTS,
          "Resource category already exists",
          Map.of("code", command.code()),
          exception);
    }

    ResourceCategoryEntity persistedEntity = categoryMapper.selectById(entity.getId());

    if (persistedEntity == null) {
      throw new IllegalStateException("Created Resource Category could not be reloaded");
    }

    return CategoryResult.from(persistedEntity);
  }

  @Transactional(readOnly = true)
  public List<CategoryResult> listCategories() {
    return categoryMapper
        .selectList(
            Wrappers.<ResourceCategoryEntity>lambdaQuery()
                .orderByAsc(ResourceCategoryEntity::getCode)
                .orderByAsc(ResourceCategoryEntity::getId))
        .stream()
        .map(CategoryResult::from)
        .toList();
  }

  @Transactional
  public ResourceResult createResource(CreateResourceCommand command) {
    validateCategoryExists(command.categoryId());

    ResourceEntity entity = new ResourceEntity();
    entity.setResourceNo(command.resourceNo());
    entity.setCategoryId(command.categoryId());
    entity.setName(command.name());
    entity.setDescription(command.description());
    entity.setLocation(command.location());
    entity.setCapacity(command.capacity());
    entity.setStatus(ResourceStatus.DRAFT);
    entity.setVersion(1L);

    try {
      int insertedRows = resourceMapper.insert(entity);

      if (insertedRows != 1) {
        throw persistenceFailure("Resource insert did not affect exactly one row", null);
      }
    } catch (DuplicateKeyException exception) {
      throw new CatalogException(
          RESOURCE_NUMBER_ALREADY_EXISTS,
          "Resource number already exists",
          Map.of("resourceNo", command.resourceNo()),
          exception);
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resource could not be persisted", exception);
    }

    ResourceEntity persistedEntity;

    try {
      persistedEntity = resourceMapper.selectById(entity.getId());
    } catch (DataAccessException exception) {
      throw persistenceFailure("Created Resource could not be reloaded", exception);
    }

    if (persistedEntity == null) {
      throw persistenceFailure("Created Resource could not be reloaded", null);
    }

    ResourceResult result = ResourceResult.from(persistedEntity);
    changeRecorder.record(result);
    resourceCache.evictAfterCommit(result.id());
    return result;
  }

  private void validateCategoryExists(Long categoryId) {
    ResourceCategoryEntity category;

    try {
      category = categoryMapper.selectById(categoryId);
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resource Category could not be queried", exception);
    }

    if (category == null) {
      throw new CatalogException(
          CATEGORY_NOT_FOUND,
          "Resource category was not found",
          Map.of("categoryId", categoryId),
          null);
    }
  }

  private static CatalogException persistenceFailure(String internalReason, Throwable cause) {
    return new CatalogException(
        CATALOG_PERSISTENCE_ERROR,
        "Resource catalog operation failed",
        Map.of("reason", internalReason),
        cause);
  }

  @Transactional(readOnly = true)
  public ResourceResult getResource(Long resourceId) {
    if (resourceId == null || resourceId <= 0) {
      throw new IllegalArgumentException("resourceId must be positive");
    }

    return resourceCache.get(resourceId, () -> loadResource(resourceId));
  }

  private ResourceResult loadResource(Long resourceId) {
    ResourceEntity entity;

    try {
      entity = resourceMapper.selectById(resourceId);
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resource could not be queried", exception);
    }

    if (entity == null) {
      throw new CatalogException(
          RESOURCE_NOT_FOUND, "Resource was not found", Map.of("resourceId", resourceId), null);
    }

    return ResourceResult.from(entity);
  }

  @Transactional(readOnly = true)
  public ResourcePageResult listResources(ResourcePageQuery query) {
    long totalElements;

    try {
      totalElements = resourceMapper.countResources(query.categoryId(), query.status());
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resources could not be counted", exception);
    }

    if (totalElements == 0) {
      return ResourcePageResult.of(List.of(), query, 0);
    }

    List<ResourceEntity> entities;

    try {
      entities =
          resourceMapper.selectResourcePage(
              query.categoryId(), query.status(), query.offset(), query.size());
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resource page could not be queried", exception);
    }

    List<ResourceResult> items = entities.stream().map(ResourceResult::from).toList();

    return ResourcePageResult.of(items, query, totalElements);
  }

  @Transactional
  public ResourceResult changeResourceStatus(ChangeResourceStatusCommand command) {
    ResourceEntity currentEntity = findResourceForStatusChange(command.resourceId());

    validateExpectedVersion(currentEntity, command.expectedVersion());

    validateStatusTransition(currentEntity, command.targetStatus());

    int updatedRows;

    try {
      updatedRows =
          resourceMapper.updateStatusIfVersionMatches(
              currentEntity.getId(),
              currentEntity.getStatus(),
              command.targetStatus(),
              command.expectedVersion());
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resource status could not be updated", exception);
    }

    if (updatedRows != 1) {
      throw resolveFailedConditionalUpdate(command.resourceId(), command.expectedVersion());
    }

    ResourceEntity updatedEntity;

    try {
      updatedEntity = resourceMapper.selectById(command.resourceId());
    } catch (DataAccessException exception) {
      throw persistenceFailure("Updated Resource could not be reloaded", exception);
    }

    if (updatedEntity == null) {
      throw resourceNotFound(command.resourceId());
    }

    ResourceResult result = ResourceResult.from(updatedEntity);
    changeRecorder.record(result);
    resourceCache.evictAfterCommit(result.id());
    return result;
  }

  @Transactional
  public ResourceResult changeResourceOwnership(ChangeResourceOwnershipCommand command) {
    ResourceEntity current = findResourceForStatusChange(command.resourceId());
    validateExpectedVersion(current, command.expectedVersion());
    int updated;
    try {
      updated =
          resourceMapper.updateOwnershipIfVersionMatches(
              command.resourceId(),
              command.ownerDepartment(),
              command.approverExternalUserId(),
              command.approvalMode(),
              command.finalApproverExternalUserId(),
              command.expectedVersion());
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resource ownership could not be updated", exception);
    }
    if (updated != 1) {
      throw resolveFailedConditionalUpdate(command.resourceId(), command.expectedVersion());
    }
    ResourceEntity persisted = resourceMapper.selectById(command.resourceId());
    if (persisted == null) {
      throw resourceNotFound(command.resourceId());
    }
    ResourceResult result = ResourceResult.from(persisted);
    changeRecorder.record(result);
    resourceCache.evictAfterCommit(result.id());
    return result;
  }

  @Transactional
  public ResourceResult changeResourceBookingRules(ChangeResourceBookingRulesCommand command) {
    ResourceEntity current = findResourceForStatusChange(command.resourceId());
    validateExpectedVersion(current, command.expectedVersion());
    int updated;
    try {
      updated =
          resourceMapper.updateBookingRulesIfVersionMatches(
              command.resourceId(),
              command.bookingNotice(),
              command.minAdvanceHours(),
              command.maxAdvanceDays(),
              command.maxDurationMinutes(),
              command.expectedVersion());
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resource booking rules could not be updated", exception);
    }
    if (updated != 1) {
      throw resolveFailedConditionalUpdate(command.resourceId(), command.expectedVersion());
    }
    ResourceEntity persisted = resourceMapper.selectById(command.resourceId());
    if (persisted == null) {
      throw resourceNotFound(command.resourceId());
    }
    ResourceResult result = ResourceResult.from(persisted);
    changeRecorder.record(result);
    resourceCache.evictAfterCommit(result.id());
    return result;
  }

  private ResourceEntity findResourceForStatusChange(Long resourceId) {
    ResourceEntity entity;

    try {
      entity = resourceMapper.selectById(resourceId);
    } catch (DataAccessException exception) {
      throw persistenceFailure("Resource could not be queried for status change", exception);
    }

    if (entity == null) {
      throw resourceNotFound(resourceId);
    }

    return entity;
  }

  private static void validateExpectedVersion(ResourceEntity entity, Long expectedVersion) {
    if (!expectedVersion.equals(entity.getVersion())) {
      throw optimisticLockConflict(entity.getId(), expectedVersion, entity.getVersion());
    }
  }

  private static void validateStatusTransition(ResourceEntity entity, ResourceStatus targetStatus) {
    if (!ResourceStatusTransitions.canTransition(entity.getStatus(), targetStatus)) {
      throw new CatalogException(
          INVALID_RESOURCE_STATUS_TRANSITION,
          "Resource status transition is not allowed",
          Map.of(
              "resourceId", entity.getId(),
              "currentStatus", entity.getStatus().name(),
              "targetStatus", targetStatus.name()),
          null);
    }
  }

  private CatalogException resolveFailedConditionalUpdate(Long resourceId, Long expectedVersion) {
    ResourceEntity latestEntity;

    try {
      latestEntity = resourceMapper.selectById(resourceId);
    } catch (DataAccessException exception) {
      throw persistenceFailure(
          "Resource could not be reloaded after a failed status update", exception);
    }

    if (latestEntity == null) {
      return resourceNotFound(resourceId);
    }

    return optimisticLockConflict(resourceId, expectedVersion, latestEntity.getVersion());
  }

  private static CatalogException resourceNotFound(Long resourceId) {
    return new CatalogException(
        RESOURCE_NOT_FOUND, "Resource was not found", Map.of("resourceId", resourceId), null);
  }

  private static CatalogException optimisticLockConflict(
      Long resourceId, Long expectedVersion, Long actualVersion) {
    return new CatalogException(
        OPTIMISTIC_LOCK_CONFLICT,
        "Resource version is stale",
        Map.of(
            "resourceId", resourceId,
            "expectedVersion", expectedVersion,
            "actualVersion", actualVersion),
        null);
  }
}
