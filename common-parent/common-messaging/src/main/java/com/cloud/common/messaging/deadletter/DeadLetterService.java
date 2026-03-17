package com.cloud.common.messaging.deadletter;

public interface DeadLetterService {

  void record(String topic, String msgId, String payload, DeadLetterReason reason, Throwable error);

  default void record(
      String topic, String msgId, byte[] payload, DeadLetterReason reason, Throwable error) {
    if (payload == null) {
      record(topic, msgId, "", reason, error);
      return;
    }
    record(
        topic, msgId, new String(payload, java.nio.charset.StandardCharsets.UTF_8), reason, error);
  }

  default void record(
      String topic, String msgId, Object payload, DeadLetterReason reason, Throwable error) {
    record(topic, msgId, payload == null ? "" : String.valueOf(payload), reason, error);
  }
}
