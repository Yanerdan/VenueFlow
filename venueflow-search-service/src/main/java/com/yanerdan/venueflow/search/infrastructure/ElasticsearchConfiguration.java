package com.yanerdan.venueflow.search.infrastructure;

import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("search")
public class ElasticsearchConfiguration {

  @Bean(destroyMethod = "close")
  Rest5Client venueflowElasticsearchClient(
      @Value("${venueflow.search.elasticsearch-uri}") URI uri) {
    return Rest5Client.builder(uri).build();
  }
}
