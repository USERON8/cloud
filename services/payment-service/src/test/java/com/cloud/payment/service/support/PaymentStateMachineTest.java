package com.cloud.payment.service.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cloud.common.exception.BizException;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PaymentStateMachineTest {

  private final PaymentStateMachine stateMachine = new PaymentStateMachine();

  @Test
  void applyMarksCreatedOrderAsPaidAndKeepsExistingPaidAt() {
    LocalDateTime existingPaidAt = LocalDateTime.parse("2026-07-01T10:15:30");
    PaymentOrderEntity order = order(PaymentOrderStateSupport.ORDER_STATUS_CREATED);
    order.setPaidAt(existingPaidAt);

    stateMachine.apply(order, result("SUCCESS", "TXN-1"), LocalDateTime.parse("2026-07-02T10:15:30"));

    assertEquals(PaymentOrderStateSupport.ORDER_STATUS_PAID, order.getStatus());
    assertEquals("TXN-1", order.getProviderTxnNo());
    assertEquals(existingPaidAt, order.getPaidAt());
  }

  @Test
  void applyMarksCreatedOrderAsFailed() {
    PaymentOrderEntity order = order(PaymentOrderStateSupport.ORDER_STATUS_CREATED);

    stateMachine.apply(order, result("FAIL", "TXN-2"), null);

    assertEquals(PaymentOrderStateSupport.ORDER_STATUS_FAILED, order.getStatus());
  }

  @Test
  void applyRejectsAlreadyTerminalOrders() {
    assertThrows(
        BizException.class,
        () -> stateMachine.apply(order(PaymentOrderStateSupport.ORDER_STATUS_PAID), result("SUCCESS", "TXN"), null));
    assertThrows(
        BizException.class,
        () -> stateMachine.apply(order(PaymentOrderStateSupport.ORDER_STATUS_FAILED), result("SUCCESS", "TXN"), null));
  }

  @Test
  void applyRejectsUnsupportedCurrentOrTargetStatus() {
    assertThrows(BizException.class, () -> stateMachine.apply(order("CLOSED"), result("SUCCESS", "TXN"), null));
    assertThrows(
        BizException.class,
        () -> stateMachine.apply(order(PaymentOrderStateSupport.ORDER_STATUS_CREATED), result("WAIT_BUYER_PAY", "TXN"), null));
  }

  private PaymentOrderEntity order(String status) {
    PaymentOrderEntity order = new PaymentOrderEntity();
    order.setStatus(status);
    order.setPaymentNo("PAY-1");
    return order;
  }

  private PaymentCallbackVerificationResult result(String status, String providerTxnNo) {
    return new PaymentCallbackVerificationResult(
        status,
        "ALIPAY",
        "trade_status_sync",
        "app-1",
        "seller-1",
        providerTxnNo,
        new BigDecimal("88.00"),
        "{}",
        "hash");
  }
}
