package com.cloud.log.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 日志服务消息配置类
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class LogMessageConfig {

    public LogMessageConfig() {
        log.info("✅ 日志服务消息配置已加载 - RocketMQ消费者集成启用");
    }
}
