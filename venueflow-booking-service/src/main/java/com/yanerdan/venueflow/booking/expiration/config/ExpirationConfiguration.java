package com.yanerdan.venueflow.booking.expiration.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@Profile("persistence & expiration")
@EnableConfigurationProperties(ExpirationProperties.class)
@EnableScheduling
public class ExpirationConfiguration {}
