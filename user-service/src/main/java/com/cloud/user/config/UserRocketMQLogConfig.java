package com.cloud.user.config;

import com.cloud.common.domain.UserChangeEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * RocketMQ日志消息配置类
 * 专门用于配置用户变更日志消息的异步发送功能
 */
@Configuration
public class UserRocketMQLogConfig {

    /**
     * 用户变更日志消息生产者
     * 用于异步发送用户变更事件到RocketMQ，供日志服务消费并记录日志
     *
     * @return Supplier函数式接口，提供消息构建方式
     */
    @Bean
    public Supplier<Message<UserChangeEvent>> userLogProducer() {
        return () -> {
            // 构建一个空的UserChangeEvent消息，实际使用时通过StreamBridge发送具体消息
            return MessageBuilder.withPayload(new UserChangeEvent())
                    .setHeader("traceId", UUID.randomUUID()
                            .toString())
                    .build();
        };
    }
}