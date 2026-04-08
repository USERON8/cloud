package com.cloud.common.exception;

public class MessageConsumeException extends BizException {

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
