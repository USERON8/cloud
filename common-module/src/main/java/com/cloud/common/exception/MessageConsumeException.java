package com.cloud.common.exception;

/**
 * 消息消费异常类
 * 用于处理RocketMQ消息消费过程中的异常情况
 *
 * @author cloud
 * @date 2025/1/15
 * @since 1.0.0
 */
public class MessageConsumeException extends BusinessException {

    /**
     * 使用指定消息创建消息消费异常
     *
     * @param message 异常消息
     */
    public MessageConsumeException(String message) {
        super(message);
    }

    /**
     * 使用指定消息和原因创建消息消费异常
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public MessageConsumeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 使用Topic、跟踪ID和原因创建消息消费异常
     *
     * @param topic   消息主题
     * @param traceId 跟踪ID
     * @param cause   异常原因
     */
    public MessageConsumeException(String topic, String traceId, Throwable cause) {
        super(String.format("消息消费失败 - Topic: %s, TraceId: %s", topic, traceId), cause);
    }
}
