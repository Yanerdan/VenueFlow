package com.yanerdan.venueflow.booking.outbox.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yanerdan.venueflow.booking.outbox.domain.OutboxEvent;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("persistence")
public class OutboxRepository {
  private final OutboxEventMapper mapper;
  private final Clock clock = Clock.systemUTC();

  public OutboxRepository(OutboxEventMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional
  public List<OutboxEvent> claimBatch(int limit, long leaseMillis) {
    LocalDateTime now = now();
    LocalDateTime leaseUntil = now.plusNanos(leaseMillis * 1_000_000L);
    List<OutboxEventEntity> entities = mapper.lockEligible(now, limit);
    return entities.stream()
        .map(
            entity -> {
              String token = UUID.randomUUID().toString();
              if (mapper.markClaimed(entity.getId(), token, leaseUntil) != 1) {
                throw new IllegalStateException("Outbox claim failed");
              }
              entity.setStatus(
                  com.yanerdan.venueflow.booking.outbox.domain.OutboxStatus.PUBLISHING);
              entity.setClaimToken(token);
              entity.setLeaseUntil(leaseUntil);
              entity.setNextRetryAt(null);
              return entity.toDomain();
            })
        .toList();
  }

  @Transactional
  public boolean markPublished(String eventId, String token) {
    return mapper.markPublished(eventId, token, now()) == 1;
  }

  @Transactional
  public boolean markFailed(
      String eventId,
      String token,
      String errorCode,
      int retryCount,
      int maxAttempts,
      long delayMillis) {
    boolean dead = retryCount + 1 >= maxAttempts;
    LocalDateTime retryAt = dead ? null : now().plusNanos(delayMillis * 1_000_000L);
    return mapper.markFailed(eventId, token, dead ? "DEAD" : "RETRY", retryAt, errorCode) == 1;
  }

  @Transactional(readOnly = true)
  public OutboxEvent find(String eventId) {
    OutboxEventEntity entity =
        mapper.selectOne(
            new LambdaQueryWrapper<OutboxEventEntity>().eq(OutboxEventEntity::getEventId, eventId));
    return entity == null ? null : entity.toDomain();
  }

  @Transactional
  public boolean requeue(String eventId, String operatorReason, boolean confirmed) {
    if (!confirmed || operatorReason == null || operatorReason.isBlank()) {
      throw new IllegalArgumentException("Confirmed replay and operator reason are required");
    }
    return mapper.requeue(eventId, now()) == 1;
  }

  @Transactional(readOnly = true)
  public long backlogCount() {
    return mapper.countBacklog();
  }

  @Transactional(readOnly = true)
  public long oldestEligibleAgeSeconds() {
    return mapper.oldestEligibleAgeSeconds();
  }

  private LocalDateTime now() {
    return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
  }
}
