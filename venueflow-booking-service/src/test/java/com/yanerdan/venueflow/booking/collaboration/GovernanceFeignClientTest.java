package com.yanerdan.venueflow.booking.collaboration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.RetryableException;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class GovernanceFeignClientTest {

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void timedOutAllocationUsesOperationLookupInsteadOfRepeatingWrite() {
    FeignResourceCapacityApi api = mock(FeignResourceCapacityApi.class);
    Request request =
        Request.create(
            Request.HttpMethod.POST,
            "http://venueflow-resource-service/api/v1/resource-slots/1/allocations",
            Map.of(),
            Request.Body.empty(),
            new RequestTemplate());
    org.mockito.Mockito.doThrow(
            new RetryableException(504, "timeout", Request.HttpMethod.POST, (Long) null, request))
        .when(api)
        .allocate(1L, new FeignResourceCapacityApi.CapacityChange("operation-1", 2));
    when(api.operation(1L, "operation-1"))
        .thenReturn(new FeignResourceCapacityApi.OperationResponse("operation-1", "ALLOCATE", 2));

    new FeignResourceCapacityClient(api, 2).allocate(1L, "operation-1", 2);

    verify(api).allocate(1L, new FeignResourceCapacityApi.CapacityChange("operation-1", 2));
    verify(api).operation(1L, "operation-1");
  }

  @Test
  void interceptorForwardsOnlyCanonicalTraceAndRemovesIdentity() {
    String traceId = "10000000-0000-0000-0000-000000000001";
    MDC.put("traceId", traceId);
    RequestTemplate template = new RequestTemplate();
    template.header("X-User-Id", "forged");
    template.header("X-Role", "admin");

    RequestInterceptor interceptor = new GovernanceFeignConfiguration().traceRequestInterceptor();
    interceptor.apply(template);

    assertThat(template.headers().get("X-Trace-Id")).containsExactly(traceId);
    assertThat(template.headers()).doesNotContainKeys("X-User-Id", "X-Role");
    assertThat(new GovernanceFeignConfiguration().governanceFeignRetryer())
        .isSameAs(feign.Retryer.NEVER_RETRY);
  }
}
