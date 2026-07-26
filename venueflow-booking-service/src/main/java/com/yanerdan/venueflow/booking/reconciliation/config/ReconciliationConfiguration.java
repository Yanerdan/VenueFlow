package com.yanerdan.venueflow.booking.reconciliation.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@Profile("persistence & reconciliation")
@EnableConfigurationProperties(ReconciliationProperties.class)
@EnableScheduling
public class ReconciliationConfiguration {}
