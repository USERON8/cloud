package com.cloud.order.enums;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderStatusEnumTest {

  @Test
  void pendingPaymentCanOnlyMoveToPaidOrCancelled() {
    assertTrue(OrderStatusEnum.PENDING_PAYMENT.canPay());
    assertTrue(OrderStatusEnum.PENDING_PAYMENT.canCancel());
    assertArrayEquals(
        new OrderStatusEnum[] {OrderStatusEnum.PAID, OrderStatusEnum.CANCELLED},
        OrderStatusEnum.PENDING_PAYMENT.getNextPossibleStatuses());
  }

  @Test
  void paidCanOnlyMoveToShipped() {
    assertTrue(OrderStatusEnum.PAID.canShip());
    assertFalse(OrderStatusEnum.PAID.canCancel());
    assertArrayEquals(
        new OrderStatusEnum[] {OrderStatusEnum.SHIPPED},
        OrderStatusEnum.PAID.getNextPossibleStatuses());
  }

  @Test
  void shippedCanOnlyMoveToCompleted() {
    assertTrue(OrderStatusEnum.SHIPPED.canComplete());
    assertArrayEquals(
        new OrderStatusEnum[] {OrderStatusEnum.COMPLETED},
        OrderStatusEnum.SHIPPED.getNextPossibleStatuses());
  }

  @Test
  void completedAndCancelledAreFinalStates() {
    assertTrue(OrderStatusEnum.COMPLETED.isFinalStatus());
    assertTrue(OrderStatusEnum.CANCELLED.isFinalStatus());
    assertArrayEquals(new OrderStatusEnum[] {}, OrderStatusEnum.COMPLETED.getNextPossibleStatuses());
    assertArrayEquals(new OrderStatusEnum[] {}, OrderStatusEnum.CANCELLED.getNextPossibleStatuses());
  }

  @Test
  void fromCodeRejectsUnknownStatusCode() {
    assertThrows(IllegalArgumentException.class, () -> OrderStatusEnum.fromCode(99));
  }
}
