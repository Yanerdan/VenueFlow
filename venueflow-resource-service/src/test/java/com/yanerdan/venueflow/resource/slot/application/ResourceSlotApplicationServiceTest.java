package com.yanerdan.venueflow.resource.slot.application;

import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.INVALID_RESOURCE_SLOT_STATUS_TRANSITION;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.INVALID_SLOT_TIME_RANGE;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.OPTIMISTIC_LOCK_CONFLICT;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_NOT_ACTIVE_FOR_SLOT;
import static com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_SLOT_TIME_OVERLAP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import com.yanerdan.venueflow.resource.catalog.error.CatalogException;
import com.yanerdan.venueflow.resource.catalog.persistence.entity.ResourceEntity;
import com.yanerdan.venueflow.resource.catalog.persistence.mapper.ResourceMapper;
import com.yanerdan.venueflow.resource.slot.domain.ResourceSlotStatus;
import com.yanerdan.venueflow.resource.slot.persistence.entity.ResourceSlotEntity;
import com.yanerdan.venueflow.resource.slot.persistence.mapper.ResourceSlotMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceSlotApplicationServiceTest {

  private static final Instant START = Instant.parse("2026-07-23T10:00:00Z");
  private static final Instant END = Instant.parse("2026-07-23T11:00:00Z");
  private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 7, 22, 15, 0);

  @Mock private ResourceMapper resourceMapper;
  @Mock private ResourceSlotMapper resourceSlotMapper;

  private ResourceSlotApplicationService service;

  @BeforeEach
  void setUp() {
    service = new ResourceSlotApplicationService(resourceMapper, resourceSlotMapper);
  }

  @Test
  void createsOpenSlotForActiveResourceUsingUtcTimes() {
    when(resourceMapper.selectByIdForUpdate(100L))
        .thenReturn(resource(100L, ResourceStatus.ACTIVE));
    when(resourceSlotMapper.countOverlappingSlots(any(), any(), any())).thenReturn(0L);
    doAnswer(
            invocation -> {
              ResourceSlotEntity entity = invocation.getArgument(0);
              entity.setId(500L);
              return 1;
            })
        .when(resourceSlotMapper)
        .insert(any(ResourceSlotEntity.class));
    when(resourceSlotMapper.selectById(500L))
        .thenReturn(slot(500L, 100L, START, END, ResourceSlotStatus.OPEN, 1L));

    ResourceSlotResult result = service.createSlot(new CreateResourceSlotCommand(100L, START, END));

    assertThat(result.id()).isEqualTo(500L);
    assertThat(result.resourceId()).isEqualTo(100L);
    assertThat(result.startAt()).isEqualTo(START);
    assertThat(result.endAt()).isEqualTo(END);
    assertThat(result.status()).isEqualTo(ResourceSlotStatus.OPEN);
    verify(resourceMapper).selectByIdForUpdate(100L);
    verify(resourceSlotMapper)
        .countOverlappingSlots(
            100L,
            LocalDateTime.ofInstant(START, ZoneOffset.UTC),
            LocalDateTime.ofInstant(END, ZoneOffset.UTC));
  }

  @Test
  void rejectsInvalidTimeRangeBeforeQueryingPersistence() {
    assertThatThrownBy(() -> service.createSlot(new CreateResourceSlotCommand(100L, END, START)))
        .isInstanceOfSatisfying(
            CatalogException.class,
            exception -> assertThat(exception.getCode()).isEqualTo(INVALID_SLOT_TIME_RANGE));

    verify(resourceMapper, never()).selectByIdForUpdate(any());
  }

  @Test
  void rejectsSlotForInactiveResource() {
    when(resourceMapper.selectByIdForUpdate(100L)).thenReturn(resource(100L, ResourceStatus.DRAFT));

    assertThatThrownBy(() -> service.createSlot(new CreateResourceSlotCommand(100L, START, END)))
        .isInstanceOfSatisfying(
            CatalogException.class,
            exception -> assertThat(exception.getCode()).isEqualTo(RESOURCE_NOT_ACTIVE_FOR_SLOT));

    verify(resourceSlotMapper, never()).countOverlappingSlots(any(), any(), any());
  }

  @Test
  void rejectsOverlapButPermitsBoundaryAdjacentSlots() {
    when(resourceMapper.selectByIdForUpdate(100L))
        .thenReturn(resource(100L, ResourceStatus.ACTIVE));
    when(resourceSlotMapper.countOverlappingSlots(any(), any(), any())).thenReturn(1L, 0L);

    assertThatThrownBy(
            () ->
                service.createSlot(
                    new CreateResourceSlotCommand(
                        100L, START.plusSeconds(30 * 60), END.plusSeconds(30 * 60))))
        .isInstanceOfSatisfying(
            CatalogException.class,
            exception -> assertThat(exception.getCode()).isEqualTo(RESOURCE_SLOT_TIME_OVERLAP));

    doAnswer(
            invocation -> {
              ResourceSlotEntity entity = invocation.getArgument(0);
              entity.setId(501L);
              return 1;
            })
        .when(resourceSlotMapper)
        .insert(any(ResourceSlotEntity.class));
    when(resourceSlotMapper.selectById(501L))
        .thenReturn(slot(501L, 100L, END, END.plusSeconds(60 * 60), ResourceSlotStatus.OPEN, 1L));

    ResourceSlotResult result =
        service.createSlot(new CreateResourceSlotCommand(100L, END, END.plusSeconds(60 * 60)));

    assertThat(result.id()).isEqualTo(501L);
  }

  @Test
  void listsBoundedIntersectingWindowInApplicationResults() {
    ResourceSlotPageQuery query = ResourceSlotPageQuery.of(100L, START, END, null, null);
    when(resourceMapper.selectById(100L)).thenReturn(resource(100L, ResourceStatus.ACTIVE));
    when(resourceSlotMapper.countSlotsIntersectingWindow(any(), any(), any())).thenReturn(1L);
    when(resourceSlotMapper.selectSlotPage(any(), any(), any(), anyLong(), anyInt()))
        .thenReturn(List.of(slot(500L, 100L, START, END, ResourceSlotStatus.OPEN, 1L)));

    ResourceSlotPageResult result = service.listSlots(query);

    assertThat(result.page()).isZero();
    assertThat(result.size()).isEqualTo(20);
    assertThat(result.totalElements()).isEqualTo(1L);
    assertThat(result.items()).extracting(ResourceSlotResult::id).containsExactly(500L);
    verify(resourceSlotMapper)
        .selectSlotPage(
            100L,
            LocalDateTime.ofInstant(START, ZoneOffset.UTC),
            LocalDateTime.ofInstant(END, ZoneOffset.UTC),
            0L,
            20);
  }

  @Test
  void changesSlotStatusWithOptimisticVersion() {
    ResourceSlotEntity current = slot(500L, 100L, START, END, ResourceSlotStatus.OPEN, 1L);
    ResourceSlotEntity updated = slot(500L, 100L, START, END, ResourceSlotStatus.CLOSED, 2L);
    when(resourceSlotMapper.selectById(500L)).thenReturn(current, updated);
    when(resourceSlotMapper.updateStatusIfVersionMatches(
            500L, ResourceSlotStatus.OPEN, ResourceSlotStatus.CLOSED, 1L))
        .thenReturn(1);

    ResourceSlotResult result =
        service.changeSlotStatus(
            new ChangeResourceSlotStatusCommand(500L, ResourceSlotStatus.CLOSED, 1L));

    assertThat(result.status()).isEqualTo(ResourceSlotStatus.CLOSED);
    assertThat(result.version()).isEqualTo(2L);
  }

  @Test
  void rejectsStaleAndNoOpSlotStatusChanges() {
    ResourceSlotEntity current = slot(500L, 100L, START, END, ResourceSlotStatus.OPEN, 2L);
    when(resourceSlotMapper.selectById(500L)).thenReturn(current);

    assertThatThrownBy(
            () ->
                service.changeSlotStatus(
                    new ChangeResourceSlotStatusCommand(500L, ResourceSlotStatus.CLOSED, 1L)))
        .isInstanceOfSatisfying(
            CatalogException.class,
            exception -> assertThat(exception.getCode()).isEqualTo(OPTIMISTIC_LOCK_CONFLICT));

    assertThatThrownBy(
            () ->
                service.changeSlotStatus(
                    new ChangeResourceSlotStatusCommand(500L, ResourceSlotStatus.OPEN, 2L)))
        .isInstanceOfSatisfying(
            CatalogException.class,
            exception ->
                assertThat(exception.getCode()).isEqualTo(INVALID_RESOURCE_SLOT_STATUS_TRANSITION));
  }

  @Test
  void rejectsOversizedPageSize() {
    assertThatThrownBy(() -> ResourceSlotPageQuery.of(100L, START, END, 0, 101))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("100");
  }

  private static ResourceEntity resource(Long id, ResourceStatus status) {
    ResourceEntity entity = new ResourceEntity();
    entity.setId(id);
    entity.setStatus(status);
    return entity;
  }

  private static ResourceSlotEntity slot(
      Long id,
      Long resourceId,
      Instant startAt,
      Instant endAt,
      ResourceSlotStatus status,
      Long version) {
    ResourceSlotEntity entity = new ResourceSlotEntity();
    entity.setId(id);
    entity.setResourceId(resourceId);
    entity.setStartAt(LocalDateTime.ofInstant(startAt, ZoneOffset.UTC));
    entity.setEndAt(LocalDateTime.ofInstant(endAt, ZoneOffset.UTC));
    entity.setStatus(status);
    entity.setVersion(version);
    entity.setCreatedAt(TIMESTAMP);
    entity.setUpdatedAt(TIMESTAMP);
    return entity;
  }
}
