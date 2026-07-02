package com.cloud.order.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderRefundStatusEnumTest {

  @Test
  void nullCodeMeansNoRefund() {
    assertSame(OrderRefundStatusEnum.NO_REFUND, OrderRefundStatusEnum.fromCode(null));
    assertFalse(OrderRefundStatusEnum.NO_REFUND.hasRefund());
  }

  @Test
  void refundApplyingAndRefundingAreProcessingStates() {
    assertTrue(OrderRefundStatusEnum.REFUND_APPLYING.isRefundProcessing());
    assertTrue(OrderRefundStatusEnum.REFUNDING.isRefundProcessing());
    assertFalse(OrderRefundStatusEnum.REFUND_SUCCESS.isRefundProcessing());
  }

  @Test
  void successFailedAndClosedAreFinishedStates() {
    assertTrue(OrderRefundStatusEnum.REFUND_SUCCESS.isRefundFinished());
    assertTrue(OrderRefundStatusEnum.REFUND_FAILED.isRefundFinished());
    assertTrue(OrderRefundStatusEnum.REFUND_CLOSED.isRefundFinished());
    assertFalse(OrderRefundStatusEnum.REFUNDING.isRefundFinished());
  }

  @Test
  void fromCodeRejectsUnknownStatus() {
    assertThrows(IllegalArgumentException.class, () -> OrderRefundStatusEnum.fromCode(99));
  }
}
