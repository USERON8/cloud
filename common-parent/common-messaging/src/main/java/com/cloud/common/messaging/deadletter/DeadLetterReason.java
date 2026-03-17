package com.cloud.common.messaging.deadletter;

public enum DeadLetterReason {
  DESERIALIZE_FAIL,
  BIZ_FAIL,
  MAX_RECONSUME,
  SYS_NONRETRYABLE,
  UNKNOWN
}
