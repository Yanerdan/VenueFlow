package com.yanerdan.venueflow.booking.outbox.messaging;

import com.yanerdan.venueflow.booking.outbox.application.OutboxPublisherSettings;
import org.springframework.amqp.core.TopicExchange;
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
@EnableScheduling
public final class OutboxMessagingConfiguration {
  public OutboxMessagingConfiguration(Environment environment) {
    if (!environment.acceptsProfiles(Profiles.of("persistence"))) {
      throw new IllegalStateException("messaging profile requires persistence");
    }
  }

  @Bean
  TopicExchange bookingEventExchange(OutboxPublisherSettings settings) {
    return new TopicExchange(settings.exchange(), true, false);
  }

  @Bean
  CachingConnectionFactory rabbitConnectionFactory(
      @Value("${spring.rabbitmq.host}") String host,
      @Value("${spring.rabbitmq.port}") int port,
      @Value("${spring.rabbitmq.username}") String username,
      @Value("${spring.rabbitmq.password}") String password,
      @Value("${spring.rabbitmq.virtual-host}") String virtualHost,
      @Value("${spring.rabbitmq.connection-timeout}") int connectionTimeout) {
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
}
