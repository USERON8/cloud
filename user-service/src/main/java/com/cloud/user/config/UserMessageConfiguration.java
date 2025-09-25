package com.cloud.user.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 用户消息配置
 * 采用函数式编程风格配置消息队列，支持发送到日志服务
 *
 * @author what's up
 * @version 2.0 - 重构为统一命名规范
 * @since 2025-09-20
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.cloud.stream.rocketmq.binder.name-server")
public class UserMessageConfiguration {

    public UserMessageConfiguration() {
        log.info("✅ 函数式用户消息配置已加载 - RocketMQ集成启用，支持日志服务");
    }

    /**
     * 日志服务生产者配置 - 函数式风格
     * 对应 log-producer-out-0 绑定
     */
    @Bean
    public Supplier<String> logProducer() {
        return () -> {
            // 这个Bean主要用于配置，实际发送通过StreamBridge
            log.debug("📡 日志服务生产者配置就绪");
            return "log-service-ready";
        };
    }

    /**
     * 用户事件生产者配置 - 保留原有功能
     * 对应 user-producer-out-0 绑定
     */
    @Bean
    public Supplier<String> userProducer() {
        return () -> {
            log.debug("📡 用户事件生产者配置就绪");
            return "user-events-ready";
        };
    }

    /**
     * 日志消费者配置（如果需要在用户服务中消费日志）
     * 对应 log-consumer-in-0 绑定
     */
    @Bean
    public Consumer<String> logConsumer() {
        return logMessage -> {
            log.debug("📥 接收到日志消息: {}", logMessage);
            // 可以在这里处理来自日志服务的消息
        };
    }
}
