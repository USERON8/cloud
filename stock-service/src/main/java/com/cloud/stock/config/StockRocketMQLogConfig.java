package com.cloud.stock.config;

import com.cloud.common.domain.StockChangeEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * RocketMQ日志消息配置类
 * 专门用于配置库存变更日志消息的异步发送功能
 *
 * @author cloud
 * @since 1.0.0
 */
@Configuration
public class StockRocketMQLogConfig {

    /**
     * 库存变更日志消息生产者
     * 用于异步发送库存变更事件到RocketMQ，供日志服务消费并记录日志
     *
     * @return Supplier函数式接口，提供消息构建方式
     */
    @Bean
    public Supplier<Message<StockChangeEvent>> stockLogProducer() {
        return () -> {
            // 构建一个空的StockChangeEvent消息，实际使用时通过StreamBridge发送具体消息
            return MessageBuilder.withPayload(new StockChangeEvent())
                    .setHeader("traceId", UUID.randomUUID().toString())
                    .build();
        };
    }
}