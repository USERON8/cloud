package com.cloud.product.config;

import com.cloud.common.domain.ProductChangeEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * RocketMQ商品日志消息配置类
 * 专门用于配置商品变更日志消息的异步发送功能
 */
@Configuration
public class ProductRocketMQConfig {

    /**
     * 商品变更日志消息生产者
     * 用于异步发送商品变更事件到RocketMQ，供日志服务消费并记录日志
     * 
     * @return Supplier函数式接口，提供消息构建方式
     */
    @Bean
    public Supplier<Message<ProductChangeEvent>> productLogProducer() {
        return () -> {
            // 构建一个空的ProductChangeEvent消息，实际使用时通过StreamBridge发送具体消息
            return MessageBuilder.withPayload(new ProductChangeEvent())
                    .setHeader("traceId", UUID.randomUUID().toString())
                    .build();
        };
    }
}