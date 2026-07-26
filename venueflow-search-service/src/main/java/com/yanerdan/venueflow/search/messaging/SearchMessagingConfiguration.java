package com.yanerdan.venueflow.search.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("search")
@EnableRabbit
public class SearchMessagingConfiguration {

  @Bean
  CachingConnectionFactory searchRabbitConnectionFactory(
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
    factory.getRabbitConnectionFactory().setConnectionTimeout(connectionTimeout);
    return factory;
  }

  @Bean
  TopicExchange resourceExchange(@Value("${venueflow.search.exchange}") String name) {
    return new TopicExchange(name, true, false);
  }

  @Bean
  TopicExchange searchDeadExchange(@Value("${venueflow.search.dead-exchange}") String name) {
    return new TopicExchange(name, true, false);
  }

  @Bean
  Queue searchResourceQueue(
      @Value("${venueflow.search.queue}") String name,
      @Value("${venueflow.search.dead-exchange}") String deadExchange,
      @Value("${venueflow.search.dead-routing-key}") String deadRoutingKey) {
    return QueueBuilder.durable(name)
        .deadLetterExchange(deadExchange)
        .deadLetterRoutingKey(deadRoutingKey)
        .build();
  }

  @Bean
  Binding searchResourceBinding(Queue searchResourceQueue, TopicExchange resourceExchange) {
    return BindingBuilder.bind(searchResourceQueue)
        .to(resourceExchange)
        .with("resource.changed.v1");
  }

  @Bean
  RabbitAdmin searchRabbitAdmin(CachingConnectionFactory factory) {
    return new RabbitAdmin(factory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory searchListenerContainerFactory(
      CachingConnectionFactory factory) {
    SimpleRabbitListenerContainerFactory listener = new SimpleRabbitListenerContainerFactory();
    listener.setConnectionFactory(factory);
    listener.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
    listener.setDefaultRequeueRejected(false);
    listener.setPrefetchCount(20);
    return listener;
  }
}
