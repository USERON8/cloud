package com.cloud.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.vo.order.OrderSubStatusVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.payment.mapper.PaymentCallbackLogMapper;
import com.cloud.payment.mapper.PaymentOrderMapper;
import com.cloud.payment.mapper.PaymentRefundMapper;
import com.cloud.payment.messaging.PaymentSuccessTxProducer;
import com.cloud.payment.module.entity.PaymentCallbackLogEntity;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import com.cloud.payment.service.PaymentCompensationService;
import com.cloud.payment.service.support.OrderStatusRemoteService;
import com.cloud.payment.service.support.PaymentOrderStateSupport;
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
    PaymentOrderStateSupport paymentOrderStateSupport =
        new PaymentOrderStateSupport(
            paymentSuccessTxProducerProvider, tradeMetrics, paymentSecurityCacheService);
    service =
        new PaymentOrderServiceImpl(
            paymentOrderMapper,
            paymentRefundMapper,
            paymentCallbackLogMapper,
            paymentCompensationService,
            orderStatusRemoteService,
            paymentOrderStateSupport,
            paymentSecurityCacheService);
  }

  @Test
  void createPaymentOrderShouldReturnCachedResult() {
    PaymentOrderCommandDTO command = buildCommand();
    OrderSubStatusVO status = new OrderSubStatusVO();
    status.setOrderStatus("STOCK_RESERVED");
    status.setUserId(command.getUserId());
    status.setPayableAmount(command.getAmount());
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
    status.setUserId(command.getUserId());
    status.setPayableAmount(command.getAmount());
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

  @Test
  void handlePaymentCallbackShouldPublishWhenOrderBecomesPaid() {
    var command = new com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO();
    command.setPaymentNo("P-2");
    command.setCallbackNo("CB-1");
    command.setCallbackStatus("SUCCESS");
    command.setProviderTxnNo("TXN-2");
    command.setIdempotencyKey("callback-idem");
    command.setAmount(BigDecimal.TEN);

    PaymentOrderEntity order = new PaymentOrderEntity();
    order.setId(12L);
    order.setPaymentNo("P-2");
    order.setMainOrderNo("M-2");
    order.setSubOrderNo("S-2");
    order.setUserId(7L);
    order.setAmount(BigDecimal.TEN);
    order.setChannel("ALIPAY");
    order.setStatus("CREATED");

    when(paymentCallbackLogMapper.selectOne(any())).thenReturn(null);
    when(paymentOrderMapper.selectOne(any())).thenReturn(order);
    when(paymentSuccessTxProducerProvider.getIfAvailable()).thenReturn(paymentSuccessTxProducer);

    Boolean handled = service.handlePaymentCallback(command);

    assertThat(handled).isTrue();
    assertThat(order.getStatus()).isEqualTo("PAID");
    assertThat(order.getProviderTxnNo()).isEqualTo("TXN-2");
    assertThat(order.getPaidAt()).isNotNull();
    verify(paymentCallbackLogMapper).insert(any(PaymentCallbackLogEntity.class));
    verify(paymentOrderMapper).updateById(order);
    verify(paymentSecurityCacheService).evictStatus("P-2");
    verify(paymentSuccessTxProducer).send(any());
    verify(tradeMetrics).incrementPayment("success");
  }

  @Test
  void createPaymentOrderShouldRejectMismatchedUser() {
    PaymentOrderCommandDTO command = buildCommand();
    OrderSubStatusVO status = new OrderSubStatusVO();
    status.setOrderStatus("STOCK_RESERVED");
    status.setUserId(99L);
    status.setPayableAmount(command.getAmount());
    when(orderStatusRemoteService.getSubOrderStatus(
            command.getMainOrderNo(), command.getSubOrderNo()))
        .thenReturn(status);

    assertThatThrownBy(() -> service.createPaymentOrder(command))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("order owner");
  }

  @Test
  void createPaymentOrderShouldRejectMismatchedAmount() {
    PaymentOrderCommandDTO command = buildCommand();
    OrderSubStatusVO status = new OrderSubStatusVO();
    status.setOrderStatus("STOCK_RESERVED");
    status.setUserId(command.getUserId());
    status.setPayableAmount(BigDecimal.ONE);
    when(orderStatusRemoteService.getSubOrderStatus(
            command.getMainOrderNo(), command.getSubOrderNo()))
        .thenReturn(status);

    assertThatThrownBy(() -> service.createPaymentOrder(command))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("order payable amount");
  }

  @Test
  void handlePaymentCallbackShouldIgnoreConflictingTerminalCallback() {
    var command = new com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO();
    command.setPaymentNo("P-3");
    command.setCallbackNo("CB-3");
    command.setCallbackStatus("FAIL");
    command.setProviderTxnNo("TXN-3");
    command.setIdempotencyKey("callback-idem-3");
    command.setAmount(BigDecimal.TEN);

    PaymentOrderEntity order = new PaymentOrderEntity();
    order.setId(13L);
    order.setPaymentNo("P-3");
    order.setAmount(BigDecimal.TEN);
    order.setProviderTxnNo("TXN-3");
    order.setStatus("PAID");

    when(paymentCallbackLogMapper.selectOne(any())).thenReturn(null);
    when(paymentOrderMapper.selectOne(any())).thenReturn(order);

    Boolean handled = service.handlePaymentCallback(command);

    assertThat(handled).isTrue();
    assertThat(order.getStatus()).isEqualTo("PAID");
    verify(paymentCallbackLogMapper).insert(any(PaymentCallbackLogEntity.class));
    verify(paymentOrderMapper, never()).updateById(any(PaymentOrderEntity.class));
    verify(paymentSuccessTxProducerProvider, never()).getIfAvailable();
  }

  @Test
  void handleInternalPaymentCallbackShouldRejectStateMutation() {
    var command = new com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO();
    command.setPaymentNo("P-4");
    command.setCallbackStatus("SUCCESS");

    assertThatThrownBy(() -> service.handleInternalPaymentCallback(command))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getCode())
                    .isEqualTo(ResultCode.BAD_REQUEST.getCode()))
        .hasMessageContaining("internal payment callbacks cannot update payment state");
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
