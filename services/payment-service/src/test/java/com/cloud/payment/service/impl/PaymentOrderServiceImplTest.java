package com.cloud.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.vo.order.OrderSubStatusVO;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.payment.mapper.PaymentCallbackLogMapper;
import com.cloud.payment.mapper.PaymentOrderMapper;
import com.cloud.payment.mapper.PaymentRefundMapper;
import com.cloud.payment.messaging.PaymentSuccessTxProducer;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import com.cloud.payment.service.PaymentCompensationService;
import com.cloud.payment.service.support.OrderStatusRemoteService;
import com.cloud.payment.service.support.PaymentSecurityCacheService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class PaymentOrderServiceImplTest {

  @Mock private PaymentOrderMapper paymentOrderMapper;

  @Mock private PaymentRefundMapper paymentRefundMapper;

  @Mock private PaymentCallbackLogMapper paymentCallbackLogMapper;

  @Mock private PaymentCompensationService paymentCompensationService;

  @Mock private OrderStatusRemoteService orderStatusRemoteService;

  @Mock private PaymentSuccessTxProducer paymentSuccessTxProducer;

  @Mock private ObjectProvider<PaymentSuccessTxProducer> paymentSuccessTxProducerProvider;

  @Mock private TradeMetrics tradeMetrics;

  @Mock private PaymentSecurityCacheService paymentSecurityCacheService;

  private PaymentOrderServiceImpl service;

  @BeforeEach
  void setUp() {
    when(paymentSuccessTxProducerProvider.getIfAvailable()).thenReturn(paymentSuccessTxProducer);
    service =
        new PaymentOrderServiceImpl(
            paymentOrderMapper,
            paymentRefundMapper,
            paymentCallbackLogMapper,
            paymentCompensationService,
            orderStatusRemoteService,
            paymentSuccessTxProducerProvider,
            tradeMetrics,
            paymentSecurityCacheService);
  }

  @Test
  void createPaymentOrderShouldReturnCachedResult() {
    PaymentOrderCommandDTO command = buildCommand();
    OrderSubStatusVO status = new OrderSubStatusVO();
    status.setOrderStatus("STOCK_RESERVED");
    when(orderStatusRemoteService.getSubOrderStatus(
            command.getMainOrderNo(), command.getSubOrderNo()))
        .thenReturn(status);
    when(paymentSecurityCacheService.allowRateLimit(command.getUserId())).thenReturn(true);
    when(paymentSecurityCacheService.getCachedResult(
            command.getMainOrderNo() + ":" + command.getSubOrderNo()))
        .thenReturn(88L);

    Long result = service.createPaymentOrder(command);

    assertThat(result).isEqualTo(88L);
    verify(paymentOrderMapper, never()).selectOne(any());
    verify(paymentOrderMapper, never()).insert(any(PaymentOrderEntity.class));
  }

  @Test
  void createPaymentOrderShouldReturnDuplicatedWhenIdempotentLockFails() {
    PaymentOrderCommandDTO command = buildCommand();
    OrderSubStatusVO status = new OrderSubStatusVO();
    status.setOrderStatus("PAID");
    when(orderStatusRemoteService.getSubOrderStatus(
            command.getMainOrderNo(), command.getSubOrderNo()))
        .thenReturn(status);
    when(paymentSecurityCacheService.allowRateLimit(command.getUserId())).thenReturn(true);
    when(paymentSecurityCacheService.getCachedResult(any())).thenReturn(null);
    PaymentOrderEntity duplicated = new PaymentOrderEntity();
    duplicated.setId(77L);
    when(paymentOrderMapper.selectOne(any())).thenReturn(duplicated);

    Long result = service.createPaymentOrder(command);

    assertThat(result).isEqualTo(77L);
    verify(paymentSecurityCacheService)
        .cacheResult(command.getMainOrderNo() + ":" + command.getSubOrderNo(), 77L);
    verify(paymentOrderMapper, never()).insert(any(PaymentOrderEntity.class));
  }

  private PaymentOrderCommandDTO buildCommand() {
    PaymentOrderCommandDTO command = new PaymentOrderCommandDTO();
    command.setPaymentNo("P-1");
    command.setMainOrderNo("M-1");
    command.setSubOrderNo("S-1");
    command.setUserId(9L);
    command.setAmount(BigDecimal.valueOf(9.9));
    command.setChannel("ALIPAY");
    command.setIdempotencyKey("idem-1");
    return command;
  }
}
