package com.cloud.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 订单服务消息配置类
 *
 * @author cloud
 * @date 2025/1/15
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class OrderMessageConfig {

    public OrderMessageConfig() {
        log.info("✅ 订单服务消息配置已加载 - RocketMQ集成启用");
    }
}
