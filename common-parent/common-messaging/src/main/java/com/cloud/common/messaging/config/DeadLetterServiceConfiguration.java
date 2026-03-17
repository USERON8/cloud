package com.cloud.common.messaging.config;

import com.cloud.common.messaging.deadletter.DeadLetterService;
import com.cloud.common.messaging.deadletter.JdbcDeadLetterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnClass({JdbcTemplate.class, DataSource.class})
public class DeadLetterServiceConfiguration {

  @Bean
  @ConditionalOnMissingBean(DeadLetterService.class)
  public DeadLetterService deadLetterService(
      ObjectProvider<DataSource> dataSourceProvider, ObjectProvider<ObjectMapper> mapperProvider) {
    DataSource dataSource = dataSourceProvider.getIfAvailable();
    if (dataSource == null) {
      throw new IllegalStateException("DeadLetterService requires a DataSource");
    }
    return new JdbcDeadLetterService(new JdbcTemplate(dataSource), mapperProvider);
  }
}
