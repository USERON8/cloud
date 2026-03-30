package com.cloud.common.messaging.config;

import com.cloud.common.config.RocketMQConfig;
import com.cloud.common.config.properties.MessageProperties;
import com.cloud.common.messaging.MessageIdempotencyService;
import com.cloud.common.messaging.deadletter.DeadLetterOpsService;
import com.cloud.common.messaging.outbox.OutboxEventMapper;
import com.cloud.common.messaging.outbox.OutboxProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties({MessageProperties.class, OutboxProperties.class})
@Import({RocketMQConfig.class, MessageIdempotencyService.class})
public class MessagingSupportAutoConfiguration {

  @Bean
  @ConditionalOnBean(MeterRegistry.class)
  @ConditionalOnProperty(
      name = "app.message.monitor.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public RocketMqLagMonitor rocketMqLagMonitor(
      ListableBeanFactory beanFactory,
      MeterRegistry meterRegistry,
      MessageProperties messageProperties) {
    return new RocketMqLagMonitor(beanFactory, meterRegistry, messageProperties);
  }

  @Bean
  @ConditionalOnBean({DeadLetterOpsService.class, MeterRegistry.class})
  @ConditionalOnProperty(
      name = "app.message.monitor.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public DeadLetterMonitor deadLetterMonitor(
      DeadLetterOpsService deadLetterOpsService,
      MessageProperties messageProperties,
      MeterRegistry meterRegistry) {
    return new DeadLetterMonitor(deadLetterOpsService, messageProperties, meterRegistry);
  }

  @Bean
  @ConditionalOnProperty(
      name = "app.message.monitor.admin-endpoint-enabled",
      havingValue = "true",
      matchIfMissing = true)
  public RocketMqConsumerTopology rocketMqConsumerTopology(
      ListableBeanFactory beanFactory, MessageProperties messageProperties) {
    return new RocketMqConsumerTopology(beanFactory, messageProperties);
  }

  @Bean
  @ConditionalOnBean({RocketMqConsumerTopology.class, DeadLetterOpsService.class})
  @ConditionalOnProperty(
      name = "app.message.monitor.admin-endpoint-enabled",
      havingValue = "true",
      matchIfMissing = true)
  public RocketMqGovernanceController rocketMqGovernanceController(
      RocketMqConsumerTopology consumerTopology, DeadLetterOpsService deadLetterOpsService) {
    return new RocketMqGovernanceController(consumerTopology, deadLetterOpsService);
  }

  @Bean
  @ConditionalOnBean({OutboxEventMapper.class, MeterRegistry.class})
  public OutboxMetricsMonitor outboxMetricsMonitor(
      OutboxEventMapper outboxEventMapper, MeterRegistry meterRegistry) {
    return new OutboxMetricsMonitor(outboxEventMapper, meterRegistry);
  }
}
