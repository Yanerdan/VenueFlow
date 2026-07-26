package com.yanerdan.venueflow.booking.collaboration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;

class ResourceInstanceFailoverTest {

  @Test
  void remainingResourceInstanceContinuesAfterOneIsRemoved() {
    ServiceInstance first =
        new DefaultServiceInstance(
            "resource-1", "venueflow-resource-service", "127.0.0.1", 18083, false);
    ServiceInstance second =
        new DefaultServiceInstance(
            "resource-2", "venueflow-resource-service", "127.0.0.1", 28083, false);
    AtomicReference<List<ServiceInstance>> available =
        new AtomicReference<>(List.of(first, second));
    ServiceInstanceListSupplier supplier =
        new ServiceInstanceListSupplier() {
          @Override
          public String getServiceId() {
            return "venueflow-resource-service";
          }

          @Override
          public Flux<List<ServiceInstance>> get() {
            return Flux.just(available.get());
          }
        };
    StaticListableBeanFactory beans =
        new StaticListableBeanFactory(Map.of("resourceInstances", supplier));
    RoundRobinLoadBalancer loadBalancer =
        new RoundRobinLoadBalancer(
            beans.getBeanProvider(ServiceInstanceListSupplier.class),
            "venueflow-resource-service",
            0);

    URI selectedFirst = loadBalancer.choose().block().getServer().getUri();
    URI selectedSecond = loadBalancer.choose().block().getServer().getUri();
    available.set(List.of(second));
    URI afterFailure = loadBalancer.choose().block().getServer().getUri();

    assertThat(List.of(selectedFirst, selectedSecond))
        .containsExactlyInAnyOrder(first.getUri(), second.getUri());
    assertThat(afterFailure).isEqualTo(second.getUri());
  }
}
