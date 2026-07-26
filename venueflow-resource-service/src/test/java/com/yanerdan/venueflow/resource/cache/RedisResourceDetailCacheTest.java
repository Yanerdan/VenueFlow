package com.yanerdan.venueflow.resource.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanerdan.venueflow.resource.catalog.application.ResourceResult;
import com.yanerdan.venueflow.resource.catalog.domain.ResourceStatus;
import com.yanerdan.venueflow.resource.catalog.error.CatalogErrorCode;
import com.yanerdan.venueflow.resource.catalog.error.CatalogException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisResourceDetailCacheTest {

  private StringRedisTemplate redis;
  private ValueOperations<String, String> values;
  private RedisResourceDetailCache cache;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    redis = mock(StringRedisTemplate.class);
    values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    cache =
        new RedisResourceDetailCache(
            redis,
            new ObjectMapper().findAndRegisterModules(),
            "test",
            Duration.ofMinutes(5),
            Duration.ofSeconds(20),
            0);
  }

  @Test
  void returnsCachedDetailWithoutLoadingMysql() throws Exception {
    ResourceResult expected = resource();
    String cached = new ObjectMapper().writeValueAsString(expected);
    when(values.get("venueflow:test:resource:detail:7")).thenReturn(cached);

    assertThat(cache.get(7L, () -> null)).isEqualTo(expected);

    verify(values, never()).set(anyString(), anyString(), any(Duration.class));
  }

  @Test
  void fallsBackToLoaderWhenRedisFails() {
    when(values.get(anyString())).thenThrow(new IllegalStateException("down"));

    assertThat(cache.get(7L, RedisResourceDetailCacheTest::resource)).isEqualTo(resource());
  }

  @Test
  void negativeCachePreventsRepeatedMysqlMiss() {
    AtomicInteger calls = new AtomicInteger();
    CatalogException missing =
        new CatalogException(
            CatalogErrorCode.RESOURCE_NOT_FOUND, "missing", Map.of("resourceId", 7L), null);
    when(values.get(anyString())).thenReturn(null, null, "__missing__");

    assertThatThrownBy(
            () ->
                cache.get(
                    7L,
                    () -> {
                      calls.incrementAndGet();
                      throw missing;
                    }))
        .isSameAs(missing);
    assertThatThrownBy(() -> cache.get(7L, () -> resource())).isInstanceOf(CatalogException.class);
    assertThat(calls).hasValue(1);
  }

  private static ResourceResult resource() {
    return new ResourceResult(
        7L, "R-7", 2L, "Room", "Description", "A", 10, ResourceStatus.ACTIVE, 3L, null, null);
  }
}
