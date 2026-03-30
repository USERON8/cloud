package com.cloud.common.messaging.config;

import com.cloud.common.config.properties.MessageProperties;
import com.cloud.common.messaging.deadletter.DeadLetterAdminController;
import com.cloud.common.messaging.deadletter.DeadLetterOpsService;
import com.cloud.common.messaging.deadletter.JdbcDeadLetterOpsService;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;

@AutoConfiguration
@ConditionalOnClass(DataSource.class)
public class DeadLetterOpsAutoConfiguration {

  @Bean
  @ConditionalOnBean(DataSource.class)
  @ConditionalOnMissingBean(DeadLetterOpsService.class)
  public DeadLetterOpsService deadLetterOpsService(DataSource dataSource) {
    return new JdbcDeadLetterOpsService(dataSource);
  }

  @Bean
  @ConditionalOnBean(DeadLetterOpsService.class)
  @ConditionalOnClass(RestController.class)
  @ConditionalOnMissingBean(DeadLetterAdminController.class)
  @ConditionalOnProperty(
      name = "app.message.monitor.admin-endpoint-enabled",
      havingValue = "true",
      matchIfMissing = true)
  public DeadLetterAdminController deadLetterAdminController(
      DeadLetterOpsService deadLetterOpsService, MessageProperties messageProperties) {
    return new DeadLetterAdminController(deadLetterOpsService, messageProperties);
  }
}
