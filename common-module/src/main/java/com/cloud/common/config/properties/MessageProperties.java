package com.cloud.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 消息配置属性类
 * 支持RocketMQ消息相关配置的外部化
 *
 * @author cloud
 * @date 2025-01-20
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.message")
public class MessageProperties {

    /**
     * 是否启用消息功能
     */
    private boolean enabled = true;

    /**
     * 消息发送重试次数
     */
    private int sendRetryTimes = 3;

    /**
     * 消息发送超时时间(毫秒)
     */
    private long sendTimeout = 3000;

    /**
     * 是否启用消息追踪
     */
    private boolean traceEnabled = true;

    /**
     * 是否启用幂等性检查
     */
    private boolean idempotentEnabled = false;

    /**
     * 幂等性检查过期时间(秒)
     */
    private long idempotentExpireSeconds = 86400;

    /**
     * 消息头配置
     */
    private HeaderConfig header = new HeaderConfig();

    /**
     * 日志配置
     */
    private LogConfig log = new LogConfig();

    @Data
    public static class HeaderConfig {
        /**
         * 是否自动添加追踪ID
         */
        private boolean autoTraceId = true;

        /**
         * 是否自动添加时间戳
         */
        private boolean autoTimestamp = true;

        /**
         * 是否自动添加服务名称
         */
        private boolean autoServiceName = true;

        /**
         * 自定义消息头前缀
         */
        private String customPrefix = "";
    }

    @Data
    public static class LogConfig {
        /**
         * 是否启用详细日志
         */
        private boolean verbose = true;

        /**
         * 是否记录消息体
         */
        private boolean logPayload = false;

        /**
         * 是否记录消息头
         */
        private boolean logHeaders = true;

        /**
         * 消息体日志最大长度
         */
        private int payloadMaxLength = 1000;
    }
}

