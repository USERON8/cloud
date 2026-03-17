package com.cloud.common.messaging.config;

import com.cloud.common.messaging.deadletter.DeadLetterService;
import com.cloud.common.messaging.deadletter.JdbcDeadLetterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@ConditionalOnClass(JdbcTemplate.class)
public class DeadLetterAutoConfiguration {

  @Bean
  @ConditionalOnBean(JdbcTemplate.class)
  @ConditionalOnMissingBean(DeadLetterService.class)
  public DeadLetterService deadLetterService(
      JdbcTemplate jdbcTemplate, ObjectProvider<ObjectMapper> mapperProvider) {
    return new JdbcDeadLetterService(jdbcTemplate, mapperProvider);
  }
}
