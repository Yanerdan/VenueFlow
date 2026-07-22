package com.yanerdan.venueflow.resource.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("persistence")
@MapperScan("com.yanerdan.venueflow.resource.catalog.persistence.mapper")
public class ResourcePersistenceConfiguration {}
