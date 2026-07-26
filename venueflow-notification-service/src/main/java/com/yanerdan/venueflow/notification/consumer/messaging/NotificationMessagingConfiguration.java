package com.yanerdan.venueflow.notification.consumer.messaging;

import com.yanerdan.venueflow.notification.consumer.domain.BookingEventDecoder;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@Profile("messaging")
@EnableRabbit
@EnableScheduling
public class NotificationMessagingConfiguration {
  @Bean
  MessagingProfileGuard messagingProfileGuard(Environment environment) {
    if (!environment.acceptsProfiles(Profiles.of("persistence"))) {
      throw new IllegalStateException("messaging profile requires persistence");
    }
    return new MessagingProfileGuard();
  }

  @Bean
  CachingConnectionFactory rabbitConnectionFactory(
      @Value("${spring.rabbitmq.host}") String host,
      @Value("${spring.rabbitmq.port}") int port,
      @Value("${spring.rabbitmq.username}") String username,
      @Value("${spring.rabbitmq.password}") String password,
      @Value("${spring.rabbitmq.virtual-host}") String virtualHost,
      @Value("${spring.rabbitmq.connection-timeout}") int connectionTimeout) {
    requireCredential(host, "host");
    requireCredential(username, "username");
    requireCredential(password, "password");
    requireCredential(virtualHost, "virtualHost");
    if (port < 1 || port > 65_535 || connectionTimeout < 100 || connectionTimeout > 30_000) {
      throw new IllegalArgumentException("RabbitMQ port or connection timeout is invalid");
    }
    CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
    connectionFactory.setUsername(username);
    connectionFactory.setPassword(password);
    connectionFactory.setVirtualHost(virtualHost);
    connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
    connectionFactory.setPublisherReturns(true);
    connectionFactory.getRabbitConnectionFactory().setConnectionTimeout(connectionTimeout);
    return connectionFactory;
  }

  @Bean
  RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMandatory(true);
    return template;
  }

  @Bean
  RabbitAdmin rabbitAdmin(CachingConnectionFactory connectionFactory) {
    return new RabbitAdmin(connectionFactory);
  }

  @Bean
  SimpleRabbitListenerContainerFactory notificationListenerContainerFactory(
      CachingConnectionFactory connectionFactory, NotificationConsumerSettings settings) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    factory.setPrefetchCount(settings.prefetch());
    factory.setConcurrentConsumers(settings.concurrency());
    factory.setMaxConcurrentConsumers(settings.concurrency());
    factory.setDefaultRequeueRejected(true);
    return factory;
  }

  @Bean
  BookingEventDecoder bookingEventDecoder(NotificationConsumerSettings settings) {
    return new BookingEventDecoder(
        new com.fasterxml.jackson.databind.ObjectMapper(), settings.maxMessageBytes());
  }

  @Bean
  TopicExchange notificationSourceExchange(NotificationConsumerSettings settings) {
    return new TopicExchange(settings.sourceExchange(), true, false);
  }

  @Bean
  TopicExchange notificationRetryExchange(NotificationConsumerSettings settings) {
    return new TopicExchange(settings.retryExchange(), true, false);
  }

  @Bean
  TopicExchange notificationDeadExchange(NotificationConsumerSettings settings) {
    return new TopicExchange(settings.deadExchange(), true, false);
  }

  @Bean
  Queue notificationWorkQueue(NotificationConsumerSettings settings) {
    return QueueBuilder.durable(settings.workQueue()).build();
  }

  @Bean
  Queue notificationRetryQueue(NotificationConsumerSettings settings) {
    return QueueBuilder.durable(settings.retryQueue())
        .ttl(settings.retryDelayMillis())
        .deadLetterExchange(settings.sourceExchange())
        .build();
  }

  @Bean
  Queue notificationDeadQueue(NotificationConsumerSettings settings) {
    return QueueBuilder.durable(settings.deadQueue()).build();
  }

  @Bean
  Binding confirmedWorkBinding(
      Queue notificationWorkQueue, TopicExchange notificationSourceExchange) {
    return BindingBuilder.bind(notificationWorkQueue)
        .to(notificationSourceExchange)
        .with(BookingEventDecoder.CONFIRMED_ROUTE);
  }

  @Bean
  Binding cancelledWorkBinding(
      Queue notificationWorkQueue, TopicExchange notificationSourceExchange) {
    return BindingBuilder.bind(notificationWorkQueue)
        .to(notificationSourceExchange)
        .with(BookingEventDecoder.CANCELLED_ROUTE);
  }

  @Bean
  Binding expiredWorkBinding(
      Queue notificationWorkQueue, TopicExchange notificationSourceExchange) {
    return BindingBuilder.bind(notificationWorkQueue)
        .to(notificationSourceExchange)
        .with(BookingEventDecoder.EXPIRED_ROUTE);
  }

  @Bean
  Binding confirmedRetryBinding(
      Queue notificationRetryQueue, TopicExchange notificationRetryExchange) {
    return BindingBuilder.bind(notificationRetryQueue)
        .to(notificationRetryExchange)
        .with(BookingEventDecoder.CONFIRMED_ROUTE);
  }

  @Bean
  Binding cancelledRetryBinding(
      Queue notificationRetryQueue, TopicExchange notificationRetryExchange) {
    return BindingBuilder.bind(notificationRetryQueue)
        .to(notificationRetryExchange)
        .with(BookingEventDecoder.CANCELLED_ROUTE);
  }

  @Bean
  Binding expiredRetryBinding(
      Queue notificationRetryQueue, TopicExchange notificationRetryExchange) {
    return BindingBuilder.bind(notificationRetryQueue)
        .to(notificationRetryExchange)
        .with(BookingEventDecoder.EXPIRED_ROUTE);
  }

  @Bean
  Binding deadBinding(Queue notificationDeadQueue, TopicExchange notificationDeadExchange) {
    return BindingBuilder.bind(notificationDeadQueue).to(notificationDeadExchange).with("#");
  }

  private static void requireCredential(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("RabbitMQ " + field + " is required");
    }
  }

  static final class MessagingProfileGuard {}
}
