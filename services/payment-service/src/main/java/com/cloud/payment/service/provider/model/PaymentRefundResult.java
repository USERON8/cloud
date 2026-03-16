package com.cloud.payment.service.provider.model;

import java.time.LocalDateTime;

public record PaymentRefundResult(Status status, LocalDateTime refundedAt, String message) {

  public enum Status {
    REFUNDED,
    PENDING,
    FAILED,
    ERROR
  }

  public static PaymentRefundResult refunded(LocalDateTime refundedAt, String message) {
    return new PaymentRefundResult(Status.REFUNDED, refundedAt, message);
  }

  public static PaymentRefundResult pending(String message) {
    return new PaymentRefundResult(Status.PENDING, null, message);
  }

  public static PaymentRefundResult failed(String message) {
    return new PaymentRefundResult(Status.FAILED, null, message);
  }

  public static PaymentRefundResult error(String message) {
    return new PaymentRefundResult(Status.ERROR, null, message);
  }
}
