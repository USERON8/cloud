package com.cloud.common.exception;

/**
 * 消息发送异常类
 *
 * @author cloud
 * @date 2025/1/15
 */
public class MessageSendException extends BusinessException {

    public MessageSendException(String message) {
        super(message);
    }

    public MessageSendException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageSendException(String topic, String traceId, Throwable cause) {
        super(String.format("消息发送失败 - Topic: %s, TraceId: %s", topic, traceId), cause);
    }
}
