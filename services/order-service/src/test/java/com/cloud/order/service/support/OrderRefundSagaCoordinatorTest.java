package com.cloud.order.service.support;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.exception.BizException;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderRefundSagaCoordinatorTest {

  @Mock private AfterSaleMapper afterSaleMapper;
  @Mock private OrderMainMapper orderMainMapper;
  @Mock private OrderSubMapper orderSubMapper;
  @Mock private PaymentOrderRemoteService paymentOrderRemoteService;
  @Mock private OrderAggregateCacheService orderAggregateCacheService;

  private OrderRefundSagaCoordinator coordinator;

  @BeforeEach
  void setUp() {
    coordinator =
        new OrderRefundSagaCoordinator(
            afterSaleMapper,
            orderMainMapper,
            orderSubMapper,
            paymentOrderRemoteService,
            orderAggregateCacheService);
  }

  @Test
  void startRefundSagaCreatesPaymentRefundAndMarksAfterSaleRefunding() {
    AfterSale afterSale = afterSale("APPROVED");
    OrderMain mainOrder = mainOrder();
    OrderSub subOrder = subOrder();
    PaymentOrderVO paymentOrder = paymentOrder();
    when(afterSaleMapper.selectById(201L)).thenReturn(afterSale);
    when(orderMainMapper.selectById(101L)).thenReturn(mainOrder);
    when(orderSubMapper.selectById(301L)).thenReturn(subOrder);
    when(paymentOrderRemoteService.getPaymentOrderByOrderNo("M100", "S100"))
        .thenReturn(paymentOrder);
    when(paymentOrderRemoteService.createRefund(argThat(command -> command != null)))
        .thenReturn(401L);

    assertDoesNotThrow(() -> coordinator.startRefundSaga(afterSale, "merchant approved"));

    org.assertj.core.api.Assertions.assertThat(afterSale.getStatus()).isEqualTo("REFUNDING");
    org.assertj.core.api.Assertions.assertThat(afterSale.getApprovedAmount())
        .isEqualByComparingTo("88.00");
    verify(afterSaleMapper).updateById(afterSale);
    verify(orderSubMapper)
        .updateById(argThat((OrderSub updated) -> "REFUNDING".equals(updated.getAfterSaleStatus())));
    verify(orderAggregateCacheService).evict(101L);
    verify(paymentOrderRemoteService)
        .createRefund(
            argThat(
                command ->
                    commandMatches(
                        command,
                        "RFAS201",
                        "PAY100",
                        "AS201",
                        new BigDecimal("88.00"),
                        "merchant approved",
                        "after-sale-refund:AS201")));
  }

  @Test
  void startRefundSagaRejectsWhenPaymentIsNotPaid() {
    AfterSale afterSale = afterSale("APPROVED");
    when(afterSaleMapper.selectById(201L)).thenReturn(afterSale);
    when(orderMainMapper.selectById(101L)).thenReturn(mainOrder());
    when(orderSubMapper.selectById(301L)).thenReturn(subOrder());
    PaymentOrderVO paymentOrder = paymentOrder();
    paymentOrder.setStatus("CREATED");
    when(paymentOrderRemoteService.getPaymentOrderByOrderNo("M100", "S100"))
        .thenReturn(paymentOrder);

    assertThrows(BizException.class, () -> coordinator.startRefundSaga(afterSale, null));
  }

  private boolean commandMatches(
      PaymentRefundCommandDTO command,
      String refundNo,
      String paymentNo,
      String afterSaleNo,
      BigDecimal amount,
      String reason,
      String idempotencyKey) {
    return refundNo.equals(command.getRefundNo())
        && paymentNo.equals(command.getPaymentNo())
        && afterSaleNo.equals(command.getAfterSaleNo())
        && amount.compareTo(command.getRefundAmount()) == 0
        && reason.equals(command.getReason())
        && idempotencyKey.equals(command.getIdempotencyKey());
  }

  private AfterSale afterSale(String status) {
    AfterSale afterSale = new AfterSale();
    afterSale.setId(201L);
    afterSale.setAfterSaleNo("AS201");
    afterSale.setMainOrderId(101L);
    afterSale.setSubOrderId(301L);
    afterSale.setStatus(status);
    afterSale.setApplyAmount(new BigDecimal("88.00"));
    afterSale.setReason("quality issue");
    afterSale.setDeleted(0);
    return afterSale;
  }

  private OrderMain mainOrder() {
    OrderMain mainOrder = new OrderMain();
    mainOrder.setId(101L);
    mainOrder.setMainOrderNo("M100");
    mainOrder.setDeleted(0);
    return mainOrder;
  }

  private OrderSub subOrder() {
    OrderSub subOrder = new OrderSub();
    subOrder.setId(301L);
    subOrder.setMainOrderId(101L);
    subOrder.setSubOrderNo("S100");
    subOrder.setPayableAmount(new BigDecimal("100.00"));
    subOrder.setDeleted(0);
    return subOrder;
  }

  private PaymentOrderVO paymentOrder() {
    PaymentOrderVO paymentOrder = new PaymentOrderVO();
    paymentOrder.setPaymentNo("PAY100");
    paymentOrder.setAmount(new BigDecimal("100.00"));
    paymentOrder.setChannel("ALIPAY");
    paymentOrder.setStatus("PAID");
    return paymentOrder;
  }
}
