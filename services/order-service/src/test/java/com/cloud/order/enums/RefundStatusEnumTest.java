package com.cloud.order.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RefundStatusEnumTest {

  @Test
  void onlyEarlyRefundStatesCanBeCancelledByUser() {
    assertTrue(RefundStatusEnum.PENDING_AUDIT.canCancel());
    assertTrue(RefundStatusEnum.AUDIT_PASSED.canCancel());
    assertFalse(RefundStatusEnum.RETURNING.canCancel());
    assertFalse(RefundStatusEnum.REFUNDING.canCancel());
    assertFalse(RefundStatusEnum.COMPLETED.canCancel());
  }

  @Test
  void finalRefundStatesAreTerminal() {
    assertTrue(RefundStatusEnum.AUDIT_REJECTED.isFinalStatus());
    assertTrue(RefundStatusEnum.COMPLETED.isFinalStatus());
    assertTrue(RefundStatusEnum.CANCELLED.isFinalStatus());
    assertTrue(RefundStatusEnum.CLOSED.isFinalStatus());
    assertFalse(RefundStatusEnum.PENDING_AUDIT.isFinalStatus());
    assertFalse(RefundStatusEnum.REFUNDING.isFinalStatus());
  }

  @Test
  void fromCodeReturnsNullForUnknownStatus() {
    assertNull(RefundStatusEnum.fromCode(99));
  }
}
