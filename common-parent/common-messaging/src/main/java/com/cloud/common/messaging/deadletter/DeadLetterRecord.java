package com.cloud.common.messaging.deadletter;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DeadLetterRecord {

  Long id;
  String topic;
  String msgId;
  String payload;
  String failReason;
  String errorMsg;
  Integer status;
  String service;
  LocalDateTime createdAt;
  LocalDateTime handledAt;
}
