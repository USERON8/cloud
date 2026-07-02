package com.cloud.payment.service.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.cloud.common.metrics.TradeMetrics;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentOrderStateSupportTest {

  @Mock private TradeMetrics tradeMetrics;
  @Mock private PaymentSecurityCacheService paymentSecurityCacheService;

  @Test
  void handlePersistedStateEvictsAndCountsPaidTransition() {
    PaymentOrderStateSupport support =
        new PaymentOrderStateSupport(tradeMetrics, paymentSecurityCacheService);
    PaymentOrderEntity order = order("PAY-1", PaymentOrderStateSupport.ORDER_STATUS_PAID);

    support.handlePersistedState(order, PaymentOrderStateSupport.ORDER_STATUS_CREATED);

    verify(paymentSecurityCacheService).evictStatus("PAY-1");
    verify(tradeMetrics).incrementPayment("success");
    verify(tradeMetrics, never()).incrementPayment("failed");
  }

  @Test
  void handlePersistedStateEvictsAndCountsFailedTransition() {
    PaymentOrderStateSupport support =
        new PaymentOrderStateSupport(tradeMetrics, paymentSecurityCacheService);
    PaymentOrderEntity order = order("PAY-2", PaymentOrderStateSupport.ORDER_STATUS_FAILED);

    support.handlePersistedState(order, PaymentOrderStateSupport.ORDER_STATUS_CREATED);

    verify(paymentSecurityCacheService).evictStatus("PAY-2");
    verify(tradeMetrics).incrementPayment("failed");
    verify(tradeMetrics, never()).incrementPayment("success");
  }

  @Test
  void handlePersistedStateDoesNotDoubleCountExistingTerminalStatus() {
    PaymentOrderStateSupport support =
        new PaymentOrderStateSupport(tradeMetrics, paymentSecurityCacheService);
    PaymentOrderEntity order = order("PAY-3", PaymentOrderStateSupport.ORDER_STATUS_PAID);

    support.handlePersistedState(order, PaymentOrderStateSupport.ORDER_STATUS_PAID);

    verify(paymentSecurityCacheService).evictStatus("PAY-3");
    verify(tradeMetrics, never()).incrementPayment("success");
    verify(tradeMetrics, never()).incrementPayment("failed");
  }

  @Test
  void terminalStatusDetectionOnlyAcceptsPaidAndFailed() {
    PaymentOrderStateSupport support =
        new PaymentOrderStateSupport(tradeMetrics, paymentSecurityCacheService);

    assertTrue(support.isTerminalStatus(PaymentOrderStateSupport.ORDER_STATUS_PAID));
    assertTrue(support.isTerminalStatus(PaymentOrderStateSupport.ORDER_STATUS_FAILED));
    assertFalse(support.isTerminalStatus(PaymentOrderStateSupport.ORDER_STATUS_CREATED));
    assertFalse(support.isTerminalStatus(null));
  }

  private PaymentOrderEntity order(String paymentNo, String status) {
    PaymentOrderEntity order = new PaymentOrderEntity();
    order.setPaymentNo(paymentNo);
    order.setStatus(status);
    return order;
  }
}
