package com.cloud.common.messaging.config;

import com.cloud.common.messaging.outbox.OutboxEventMapper;
import com.cloud.common.messaging.outbox.OutboxEventService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(OutboxEventMapper.class)
@EnableConfigurationProperties(com.cloud.common.messaging.outbox.OutboxProperties.class)
public class OutboxAutoConfiguration {

  @Bean
  @ConditionalOnBean(OutboxEventMapper.class)
  @ConditionalOnMissingBean(OutboxEventService.class)
  public OutboxEventService outboxEventService(
      ObjectProvider<OutboxEventMapper> outboxEventMapperProvider) {
    return new OutboxEventService(outboxEventMapperProvider);
  }
}
