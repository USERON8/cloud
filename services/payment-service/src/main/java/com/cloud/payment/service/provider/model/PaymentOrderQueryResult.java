package com.cloud.payment.service.provider.model;

import java.time.LocalDateTime;

public record PaymentOrderQueryResult(
    Status status, String providerTxnNo, LocalDateTime paidAt, String message) {

  public enum Status {
    PAID,
    PENDING,
    FAILED,
    ERROR
  }

  public static PaymentOrderQueryResult paid(
      String providerTxnNo, LocalDateTime paidAt, String message) {
    return new PaymentOrderQueryResult(Status.PAID, providerTxnNo, paidAt, message);
  }

  public static PaymentOrderQueryResult pending(String providerTxnNo, String message) {
    return new PaymentOrderQueryResult(Status.PENDING, providerTxnNo, null, message);
  }

  public static PaymentOrderQueryResult failed(String providerTxnNo, String message) {
    return new PaymentOrderQueryResult(Status.FAILED, providerTxnNo, null, message);
  }

  public static PaymentOrderQueryResult error(String message) {
    return new PaymentOrderQueryResult(Status.ERROR, null, null, message);
  }
}
