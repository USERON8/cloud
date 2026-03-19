package com.cloud.common.messaging.config;

import com.cloud.common.config.RocketMQConfig;
import com.cloud.common.config.properties.MessageProperties;
import com.cloud.common.messaging.MessageIdempotencyService;
import com.cloud.common.messaging.outbox.OutboxProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties({MessageProperties.class, OutboxProperties.class})
@Import({RocketMQConfig.class, MessageIdempotencyService.class})
public class MessagingSupportAutoConfiguration {}
