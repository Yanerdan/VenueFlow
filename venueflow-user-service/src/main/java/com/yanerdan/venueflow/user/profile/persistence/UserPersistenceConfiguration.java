package com.yanerdan.venueflow.user.profile.persistence;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("persistence")
@MapperScan(basePackageClasses = UserProfileMapper.class)
public class UserPersistenceConfiguration {}
