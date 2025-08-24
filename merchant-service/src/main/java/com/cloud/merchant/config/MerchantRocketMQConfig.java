package com.cloud.merchant.config;

import com.cloud.common.domain.MerchantChangeEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * RocketMQ商家日志消息配置类
 * 专门用于配置商家变更日志消息的异步发送功能
 */
@Configuration
public class MerchantRocketMQConfig {

    /**
     * 商家变更日志消息生产者
     * 用于异步发送商家变更事件到RocketMQ，供日志服务消费并记录日志
     * 
     * @return Supplier函数式接口，提供消息构建方式
     */
    @Bean
    public Supplier<Message<MerchantChangeEvent>> merchantLogProducer() {
        return () -> {
            // 构建一个空的MerchantChangeEvent消息，实际使用时通过StreamBridge发送具体消息
            return MessageBuilder.withPayload(new MerchantChangeEvent())
                    .setHeader("traceId", UUID.randomUUID().toString())
                    .build();
        };
    }
}