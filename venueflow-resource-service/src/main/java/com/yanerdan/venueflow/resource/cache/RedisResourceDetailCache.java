package com.yanerdan.venueflow.resource.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanerdan.venueflow.resource.catalog.application.ResourceResult;
import com.yanerdan.venueflow.resource.catalog.error.CatalogException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Profile("persistence & cache")
public final class RedisResourceDetailCache implements ResourceDetailCache {

  private static final String MISSING = "__missing__";
  private final AtomicReference<StringRedisTemplate> redis;
  private final AtomicReference<ObjectMapper> objectMapper;
  private final String prefix;
  private final Duration detailTtl;
  private final Duration missingTtl;
  private final int jitterSeconds;
  private final Map<Long, Object> rebuildLocks = new ConcurrentHashMap<>();

  public RedisResourceDetailCache(
      StringRedisTemplate redis,
      ObjectMapper objectMapper,
      @Value("${venueflow.cache.environment}") String environment,
      @Value("${venueflow.cache.detail-ttl}") Duration detailTtl,
      @Value("${venueflow.cache.missing-ttl}") Duration missingTtl,
      @Value("${venueflow.cache.jitter-seconds}") int jitterSeconds) {
    this.redis = new AtomicReference<>(redis);
    this.objectMapper = new AtomicReference<>(objectMapper);
    this.prefix = "venueflow:" + requireSegment(environment) + ":resource:detail:";
    this.detailTtl = detailTtl;
    this.missingTtl = missingTtl;
    this.jitterSeconds = Math.max(0, jitterSeconds);
  }

  @Override
  public ResourceResult get(Long resourceId, Supplier<ResourceResult> loader) {
    String key = prefix + resourceId;
    String cached = readRaw(key);
    if (cached != null) {
      ResourceResult decoded = decode(resourceId, cached);
      if (decoded != null) {
        return decoded;
      }
    }
    Object lock = rebuildLocks.computeIfAbsent(resourceId, ignored -> new Object());
    synchronized (lock) {
      try {
        cached = readRaw(key);
        if (cached != null) {
          ResourceResult decoded = decode(resourceId, cached);
          if (decoded != null) {
            return decoded;
          }
        }
        try {
          ResourceResult loaded = loader.get();
          write(key, loaded, jitter(detailTtl));
          return loaded;
        } catch (CatalogException exception) {
          if (exception.getCode()
              == com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode
                  .RESOURCE_NOT_FOUND) {
            writeRaw(key, MISSING, missingTtl);
          }
          throw exception;
        }
      } finally {
        rebuildLocks.remove(resourceId, lock);
      }
    }
  }

  @Override
  public void evictAfterCommit(Long resourceId) {
    Runnable eviction = () -> safelyDelete(prefix + resourceId);
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              eviction.run();
            }
          });
    } else {
      eviction.run();
    }
  }

  private String readRaw(String key) {
    try {
      return redis.get().opsForValue().get(key);
    } catch (RuntimeException exception) {
      return null;
    }
  }

  private ResourceResult decode(Long resourceId, String value) {
    if (MISSING.equals(value)) {
      throw new CatalogException(
          com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode.RESOURCE_NOT_FOUND,
          "Resource was not found",
          Map.of("resourceId", resourceId),
          null);
    }
    try {
      return objectMapper.get().readValue(value, ResourceResult.class);
    } catch (JsonProcessingException exception) {
      return null;
    }
  }

  private void write(String key, ResourceResult result, Duration ttl) {
    try {
      writeRaw(key, objectMapper.get().writeValueAsString(result), ttl);
    } catch (JsonProcessingException ignored) {
      // Cache serialization failure must not affect the MySQL response.
    }
  }

  private void writeRaw(String key, String value, Duration ttl) {
    try {
      redis.get().opsForValue().set(key, value, ttl);
    } catch (RuntimeException ignored) {
      // Redis is optional.
    }
  }

  private void safelyDelete(String key) {
    try {
      redis.get().delete(key);
    } catch (RuntimeException ignored) {
      // A bounded TTL repairs missed eviction.
    }
  }

  private Duration jitter(Duration base) {
    if (jitterSeconds == 0) {
      return base;
    }
    return base.plusSeconds(ThreadLocalRandom.current().nextInt(jitterSeconds + 1));
  }

  private static String requireSegment(String value) {
    if (value == null || !value.matches("[a-zA-Z0-9_-]{1,32}")) {
      throw new IllegalArgumentException("cache environment must be a bounded key segment");
    }
    return value;
  }
}
