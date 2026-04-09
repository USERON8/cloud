package com.cloud.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class RocketMQConfig {}
