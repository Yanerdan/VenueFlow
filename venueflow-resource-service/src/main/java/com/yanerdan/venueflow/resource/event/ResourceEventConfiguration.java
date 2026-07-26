package com.yanerdan.venueflow.resource.event;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@Profile("persistence & resource-events")
@EnableScheduling
public class ResourceEventConfiguration {

  @Bean
  TopicExchange resourceExchange(@Value("${venueflow.resource-events.exchange}") String exchange) {
    return new TopicExchange(exchange, true, false);
  }

  @Bean
  CachingConnectionFactory resourceRabbitConnectionFactory(
      @Value("${spring.rabbitmq.host}") String host,
      @Value("${spring.rabbitmq.port}") int port,
      @Value("${spring.rabbitmq.username}") String username,
      @Value("${spring.rabbitmq.password}") String password,
      @Value("${spring.rabbitmq.virtual-host}") String virtualHost,
      @Value("${spring.rabbitmq.connection-timeout}") int connectionTimeout) {
    CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
    factory.setUsername(username);
    factory.setPassword(password);
    factory.setVirtualHost(virtualHost);
    factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
    factory.setPublisherReturns(true);
    factory.getRabbitConnectionFactory().setConnectionTimeout(connectionTimeout);
    return factory;
  }

  @Bean
  RabbitTemplate resourceRabbitTemplate(CachingConnectionFactory factory) {
    RabbitTemplate template = new RabbitTemplate(factory);
    template.setMandatory(true);
    return template;
  }

  @Bean
  RabbitAdmin resourceRabbitAdmin(CachingConnectionFactory factory) {
    return new RabbitAdmin(factory);
  }
}
