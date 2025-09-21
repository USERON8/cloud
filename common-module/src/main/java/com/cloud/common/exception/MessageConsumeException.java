package com.cloud.common.exception;

/**
 * 消息消费异常类
 *
 * @author cloud
 * @date 2025/1/15
 */
public class MessageConsumeException extends BusinessException {

    public MessageConsumeException(String message) {
        super(message);
    }

    public MessageConsumeException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageConsumeException(String topic, String traceId, Throwable cause) {
        super(String.format("消息消费失败 - Topic: %s, TraceId: %s", topic, traceId), cause);
    }
}
