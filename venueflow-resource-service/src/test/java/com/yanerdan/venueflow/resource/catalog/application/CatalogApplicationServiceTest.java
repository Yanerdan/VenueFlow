package com.yanerdan.venueflow.resource.catalog.application;

import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.CATEGORY_ALREADY_EXISTS;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.CATEGORY_NOT_FOUND;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.INVALID_RESOURCE_STATUS_TRANSITION;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.OPTIMISTIC_LOCK_CONFLICT;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_NOT_FOUND;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_NUMBER_ALREADY_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import com.yanerdan.venueflow.resource.catalog.error.CatalogException;
import com.yanerdan.venueflow.resource.catalog.persistence.entity.ResourceCategoryEntity;
import com.yanerdan.venueflow.resource.catalog.persistence.entity.ResourceEntity;
import com.yanerdan.venueflow.resource.catalog.persistence.mapper.ResourceCategoryMapper;
import com.yanerdan.venueflow.resource.catalog.persistence.mapper.ResourceMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class CatalogApplicationServiceTest {

  @Mock private ResourceMapper resourceMapper;
  @Mock private ResourceCategoryMapper categoryMapper;

  private CatalogApplicationService service;

  @BeforeEach
  void setUp() {
    service = new CatalogApplicationService(categoryMapper, resourceMapper);
  }

  @Test
  void createsAndReloadsCategory() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 21, 17, 30);

    doAnswer(
            invocation -> {
              ResourceCategoryEntity entity = invocation.getArgument(0);
              entity.setId(1L);
              return 1;
            })
        .when(categoryMapper)
        .insert(any(ResourceCategoryEntity.class));

    ResourceCategoryEntity persisted = category(1L, "MEETING_ROOM", "Meeting Room", now);
    when(categoryMapper.selectById(1L)).thenReturn(persisted);

    CategoryResult result =
        service.createCategory(new CreateCategoryCommand(" MEETING_ROOM ", " Meeting Room "));

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.code()).isEqualTo("MEETING_ROOM");
    assertThat(result.name()).isEqualTo("Meeting Room");
    assertThat(result.createdAt()).isEqualTo(now);
  }

  @Test
  void translatesDuplicateCategoryIntoStableCatalogError() {
    when(categoryMapper.insert(any(ResourceCategoryEntity.class)))
        .thenThrow(new DuplicateKeyException("duplicate category"));

    assertThatThrownBy(
            () -> service.createCategory(new CreateCategoryCommand("MEETING_ROOM", "Meeting Room")))
        .isInstanceOfSatisfying(
            CatalogException.class,
            exception -> {
              assertThat(exception.getCode()).isEqualTo(CATEGORY_ALREADY_EXISTS);
              assertThat(exception.getDetails()).containsEntry("code", "MEETING_ROOM");
            });
  }

  @Test
  void listsCategoriesAsApplicationResults() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 21, 17, 30);

    when(categoryMapper.selectList(any()))
        .thenReturn(
            List.of(
                category(1L, "DESK", "Desk", now),
                category(2L, "MEETING_ROOM", "Meeting Room", now)));

    List<CategoryResult> results = service.listCategories();

    assertThat(results).extracting(CategoryResult::code).containsExactly("DESK", "MEETING_ROOM");
  }

  private static ResourceCategoryEntity category(
      Long id, String code, String name, LocalDateTime timestamp) {
    ResourceCategoryEntity entity = new ResourceCategoryEntity();
    entity.setId(id);
    entity.setCode(code);
    entity.setName(name);
    entity.setCreatedAt(timestamp);
    entity.setUpdatedAt(timestamp);
    return entity;
  }

  @Test
  void createsResourceWithDraftStatusAndInitialVersion() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 21, 18, 30);

    when(categoryMapper.selectById(10L))
        .thenReturn(category(10L, "MEETING_ROOM", "Meeting Room", now));

    doAnswer(
            invocation -> {
              ResourceEntity entity = invocation.getArgument(0);
              entity.setId(100L);
              return 1;
            })
        .when(resourceMapper)
        .insert(any(ResourceEntity.class));

    ResourceEntity persisted =
        resource(100L, "ROOM-A-101", 10L, "Room A101", 10, ResourceStatus.DRAFT, 1L, now);

    when(resourceMapper.selectById(100L)).thenReturn(persisted);

    ResourceResult result =
        service.createResource(
            new CreateResourceCommand(
                " ROOM-A-101 ",
                10L,
                " Room A101 ",
                " First-floor meeting room ",
                " Building A ",
                10));

    assertThat(result.id()).isEqualTo(100L);
    assertThat(result.resourceNo()).isEqualTo("ROOM-A-101");
    assertThat(result.categoryId()).isEqualTo(10L);
    assertThat(result.status()).isEqualTo(ResourceStatus.DRAFT);
    assertThat(result.version()).isEqualTo(1L);
  }

  @Test
  void rejectsResourceWhenCategoryDoesNotExist() {
    when(categoryMapper.selectById(999L)).thenReturn(null);

    assertThatThrownBy(
            () ->
                service.createResource(
                    new CreateResourceCommand("ROOM-A-101", 999L, "Room A101", null, null, 10)))
        .isInstanceOfSatisfying(
            CatalogException.class,
            exception -> {
              assertThat(exception.getCode()).isEqualTo(CATEGORY_NOT_FOUND);
              assertThat(exception.getDetails()).containsEntry("categoryId", 999L);
            });

    verifyNoInteractions(resourceMapper);
  }

  @Test
  void translatesDuplicateResourceNumberIntoStableError() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 21, 18, 30);

    when(categoryMapper.selectById(10L))
        .thenReturn(category(10L, "MEETING_ROOM", "Meeting Room", now));

    when(resourceMapper.insert(any(ResourceEntity.class)))
        .thenThrow(new DuplicateKeyException("duplicate resource number"));

    assertThatThrownBy(
            () ->
                service.createResource(
                    new CreateResourceCommand("ROOM-A-101", 10L, "Room A101", null, null, 10)))
        .isInstanceOfSatisfying(
            CatalogException.class,
            exception -> {
              assertThat(exception.getCode()).isEqualTo(RESOURCE_NUMBER_ALREADY_EXISTS);

              assertThat(exception.getDetails()).containsEntry("resourceNo", "ROOM-A-101");
            });
  }

  private static ResourceEntity resource(
      Long id,
      String resourceNo,
      Long categoryId,
      String name,
      Integer capacity,
      ResourceStatus status,
      Long version,
      LocalDateTime timestamp) {
    ResourceEntity entity = new ResourceEntity();
    entity.setId(id);
    entity.setResourceNo(resourceNo);
    entity.setCategoryId(categoryId);
    entity.setName(name);
    entity.setCapacity(capacity);
    entity.setStatus(status);
    entity.setVersion(version);
    entity.setCreatedAt(timestamp);
    entity.setUpdatedAt(timestamp);
    return entity;
  }

  @Test
  void returnsResourceDetail() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 21, 19, 0);

    ResourceEntity persisted =
        resource(100L, "ROOM-A-101", 10L, "Room A101", 10, ResourceStatus.ACTIVE, 2L, now);

    when(resourceMapper.selectById(100L)).thenReturn(persisted);

    ResourceResult result = service.getResource(100L);

    assertThat(result.id()).isEqualTo(100L);
    assertThat(result.resourceNo()).isEqualTo("ROOM-A-101");
    assertThat(result.status()).isEqualTo(ResourceStatus.ACTIVE);
    assertThat(result.version()).isEqualTo(2L);
  }

  @Test
  void reportsResourceNotFound() {
    when(resourceMapper.selectById(999L)).thenReturn(null);

    assertThatThrownBy(() -> service.getResource(999L))
        .isInstanceOfSatisfying(
            CatalogException.class,
            exception -> {
              assertThat(exception.getCode()).isEqualTo(RESOURCE_NOT_FOUND);
              assertThat(exception.getDetails()).containsEntry("resourceId", 999L);
            });
  }

  @Test
  void appliesDefaultResourcePageParameters() {
    ResourcePageQuery query = ResourcePageQuery.of(null, null, null, null);

    assertThat(query.page()).isZero();
    assertThat(query.size()).isEqualTo(ResourcePageQuery.DEFAULT_SIZE);
    assertThat(query.offset()).isZero();
  }

  @Test
  void calculatesResourcePageOffset() {
    ResourcePageQuery query = ResourcePageQuery.of(2, 20, null, null);

    assertThat(query.offset()).isEqualTo(40L);
  }

  @Test
  void rejectsOversizedResourcePage() {
    assertThatThrownBy(() -> ResourcePageQuery.of(0, 101, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("100");
  }

  @Test
  void rejectsNegativePageNumber() {
    assertThatThrownBy(() -> ResourcePageQuery.of(-1, 20, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void returnsBoundedFilteredResourcePage() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 21, 19, 0);

    ResourcePageQuery query = ResourcePageQuery.of(1, 20, 10L, ResourceStatus.ACTIVE);

    when(resourceMapper.countResources(10L, ResourceStatus.ACTIVE)).thenReturn(25L);

    when(resourceMapper.selectResourcePage(10L, ResourceStatus.ACTIVE, 20L, 20))
        .thenReturn(
            List.of(
                resource(21L, "ROOM-A-121", 10L, "Room A121", 10, ResourceStatus.ACTIVE, 1L, now)));

    ResourcePageResult result = service.listResources(query);

    assertThat(result.page()).isEqualTo(1);
    assertThat(result.size()).isEqualTo(20);
    assertThat(result.totalElements()).isEqualTo(25L);
    assertThat(result.totalPages()).isEqualTo(2L);
    assertThat(result.items()).hasSize(1);

    verify(resourceMapper).selectResourcePage(10L, ResourceStatus.ACTIVE, 20L, 20);
  }

  @Test
  void returnsEmptyPageWithoutRunningPageQuery() {
    ResourcePageQuery query = ResourcePageQuery.of(null, null, 10L, ResourceStatus.SUSPENDED);

    when(resourceMapper.countResources(10L, ResourceStatus.SUSPENDED)).thenReturn(0L);

    ResourcePageResult result = service.listResources(query);

    assertThat(result.items()).isEmpty();
    assertThat(result.totalElements()).isZero();
    assertThat(result.totalPages()).isZero();

    verify(resourceMapper, never()).selectResourcePage(10L, ResourceStatus.SUSPENDED, 0L, 20);
  }

  @Test
  void changesResourceStatusWithExpectedVersion() {
    LocalDateTime before = LocalDateTime.of(2026, 7, 21, 19, 30);

    LocalDateTime after = LocalDateTime.of(2026, 7, 21, 19, 31);

    ResourceEntity current =
        resource(100L, "ROOM-A-101", 10L, "Room A101", 10, ResourceStatus.DRAFT, 1L, before);

    ResourceEntity updated =
        resource(100L, "ROOM-A-101", 10L, "Room A101", 10, ResourceStatus.ACTIVE, 2L, after);

    when(resourceMapper.selectById(100L)).thenReturn(current, updated);

    when(resourceMapper.updateStatusIfVersionMatches(
            100L, ResourceStatus.DRAFT, ResourceStatus.ACTIVE, 1L))
        .thenReturn(1);

    ResourceResult result =
        service.changeResourceStatus(
            new ChangeResourceStatusCommand(100L, ResourceStatus.ACTIVE, 1L));

    assertThat(result.status()).isEqualTo(ResourceStatus.ACTIVE);

    assertThat(result.version()).isEqualTo(2L);
  }

  @Test
  void changesBookingRulesWithExpectedVersion() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 21, 19, 30);
    ResourceEntity current =
        resource(100L, "ROOM-A-101", 10L, "Room A101", 10, ResourceStatus.ACTIVE, 2L, now);
    ResourceEntity updated =
        resource(100L, "ROOM-A-101", 10L, "Room A101", 10, ResourceStatus.ACTIVE, 3L, now);
    updated.setBookingNotice("Bring a campus card");
    updated.setMinAdvanceHours(2);
    updated.setMaxAdvanceDays(30);
    updated.setMaxDurationMinutes(120);
    when(resourceMapper.selectById(100L)).thenReturn(current, updated);
    when(resourceMapper.updateBookingRulesIfVersionMatches(
            100L, "Bring a campus card", 2, 30, 120, 2L))
        .thenReturn(1);

    ResourceResult result =
        service.changeResourceBookingRules(
            new ChangeResourceBookingRulesCommand(100L, " Bring a campus card ", 2, 30, 120, 2L));

    assertThat(result.bookingNotice()).isEqualTo("Bring a campus card");
    assertThat(result.maxDurationMinutes()).isEqualTo(120);
    assertThat(result.version()).isEqualTo(3L);
  }

  @Test
  void rejectsStaleExpectedVersionBeforeUpdate() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 21, 19, 30);

    ResourceEntity current =
        resource(100L, "ROOM-A-101", 10L, "Room A101", 10, ResourceStatus.ACTIVE, 2L, now);

    when(resourceMapper.selectById(100L)).thenReturn(current);

    assertThatThrownBy(
            () ->
                service.changeResourceStatus(
                    new ChangeResourceStatusCommand(100L, ResourceStatus.ARCHIVED, 1L)))
        .isInstanceOfSatisfying(
            CatalogException.class,
            exception -> {
              assertThat(exception.getCode()).isEqualTo(OPTIMISTIC_LOCK_CONFLICT);

              assertThat(exception.getDetails())
                  .containsEntry("expectedVersion", 1L)
                  .containsEntry("actualVersion", 2L);
            });

    verify(resourceMapper, never()).updateStatusIfVersionMatches(any(), any(), any(), any());
  }

  @Test
  void rejectsInvalidResourceStatusTransition() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 21, 19, 30);

    ResourceEntity current =
        resource(100L, "ROOM-A-101", 10L, "Room A101", 10, ResourceStatus.ARCHIVED, 3L, now);

    when(resourceMapper.selectById(100L)).thenReturn(current);

    assertThatThrownBy(
            () ->
                service.changeResourceStatus(
                    new ChangeResourceStatusCommand(100L, ResourceStatus.ACTIVE, 3L)))
        .isInstanceOfSatisfying(
            CatalogException.class,
            exception ->
                assertThat(exception.getCode()).isEqualTo(INVALID_RESOURCE_STATUS_TRANSITION));
  }

  @Test
  void reportsConflictWhenConditionalUpdateLosesRace() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 21, 19, 30);

    ResourceEntity original =
        resource(100L, "ROOM-A-101", 10L, "Room A101", 10, ResourceStatus.DRAFT, 1L, now);

    ResourceEntity concurrent =
        resource(100L, "ROOM-A-101", 10L, "Room A101", 10, ResourceStatus.ACTIVE, 2L, now);

    when(resourceMapper.selectById(100L)).thenReturn(original, concurrent);

    when(resourceMapper.updateStatusIfVersionMatches(
            100L, ResourceStatus.DRAFT, ResourceStatus.ARCHIVED, 1L))
        .thenReturn(0);

    assertThatThrownBy(
            () ->
                service.changeResourceStatus(
                    new ChangeResourceStatusCommand(100L, ResourceStatus.ARCHIVED, 1L)))
        .isInstanceOfSatisfying(
            CatalogException.class,
            exception -> assertThat(exception.getCode()).isEqualTo(OPTIMISTIC_LOCK_CONFLICT));
  }

  @Test
  void usesMaximumPageSizeAsBoundedMapperLimit() {
    ResourcePageQuery query = ResourcePageQuery.of(0, 100, null, null);

    when(resourceMapper.countResources(null, null)).thenReturn(150L);

    when(resourceMapper.selectResourcePage(null, null, 0L, 100)).thenReturn(List.of());

    ResourcePageResult result = service.listResources(query);

    assertThat(result.page()).isZero();
    assertThat(result.size()).isEqualTo(100);
    assertThat(result.totalElements()).isEqualTo(150L);
    assertThat(result.totalPages()).isEqualTo(2L);

    verify(resourceMapper).selectResourcePage(null, null, 0L, 100);
  }

  @Test
  void rejectsPageSizeAboveMaximum() {
    assertThatThrownBy(() -> ResourcePageQuery.of(0, 101, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("100");
  }
}
