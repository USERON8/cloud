package com.cloud.order.service.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderSubMapper;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderRefundSagaServiceTest {

  @Mock private AfterSaleMapper afterSaleMapper;

  @Mock private OrderSubMapper orderSubMapper;

  @Mock private PaymentOrderRemoteService paymentOrderRemoteService;

  @Mock private OrderAggregateCacheService orderAggregateCacheService;

  @InjectMocks private OrderRefundSagaService orderRefundSagaService;

  @Test
  void applyRefund_statusMismatch_throws() {
    AfterSale afterSale = new AfterSale();
    afterSale.setId(1L);
    afterSale.setStatus("APPLIED");
    afterSale.setDeleted(0);
    when(afterSaleMapper.selectById(1L)).thenReturn(afterSale);

    assertThatThrownBy(
            () ->
                orderRefundSagaService.applyRefund(
                    Map.of("afterSaleId", 1L, "previousStatus", "APPROVED")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("status mismatch");
  }

  @Test
  void createRefund_success_callsRemote() {
    when(paymentOrderRemoteService.createRefund(any(PaymentRefundCommandDTO.class))).thenReturn(9L);

    orderRefundSagaService.createRefund(
        Map.of(
            "refundNo", "R1",
            "paymentNo", "P1",
            "afterSaleNo", "A1",
            "refundAmount", BigDecimal.ONE,
            "reason", "test",
            "idempotencyKey", "k1"));

    verify(paymentOrderRemoteService).createRefund(any(PaymentRefundCommandDTO.class));
  }

  @Test
  void cancelRefund_updatesAfterSaleAndCache() {
    AfterSale afterSale = new AfterSale();
    afterSale.setId(2L);
    afterSale.setStatus("REFUNDING");
    afterSale.setSubOrderId(3L);
    afterSale.setMainOrderId(4L);
    afterSale.setDeleted(0);
    when(afterSaleMapper.selectById(2L)).thenReturn(afterSale);

    OrderSub sub = new OrderSub();
    sub.setId(3L);
    sub.setDeleted(0);
    when(orderSubMapper.selectById(3L)).thenReturn(sub);

    orderRefundSagaService.cancelRefund(Map.of("afterSaleId", 2L, "previousStatus", "CLOSED"));

    verify(afterSaleMapper).updateById(afterSale);
    verify(orderSubMapper).updateById(sub);
    verify(orderAggregateCacheService).evict(4L);
  }
}
