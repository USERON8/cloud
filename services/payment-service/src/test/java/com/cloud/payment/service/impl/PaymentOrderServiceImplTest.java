package com.cloud.payment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.vo.order.OrderSubStatusVO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.payment.config.AlipayConfig;
import com.cloud.payment.converter.PaymentOrderConverter;
import com.cloud.payment.mapper.PaymentCallbackLogMapper;
import com.cloud.payment.mapper.PaymentOrderMapper;
import com.cloud.payment.mapper.PaymentRefundMapper;
import com.cloud.payment.messaging.PaymentMessageProducer;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import com.cloud.payment.service.PaymentCompensationService;
import com.cloud.payment.service.provider.PaymentProviderGateway;
import com.cloud.payment.service.support.OrderStatusRemoteService;
import com.cloud.payment.service.support.PaymentCallbackVerifier;
import com.cloud.payment.service.support.PaymentOrderStateSupport;
import com.cloud.payment.service.support.PaymentSecurityCacheService;
import com.cloud.payment.service.support.PaymentStateMachine;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentOrderServiceImplTest {

  @Mock private PaymentOrderMapper paymentOrderMapper;
  @Mock private PaymentRefundMapper paymentRefundMapper;
  @Mock private PaymentCallbackLogMapper paymentCallbackLogMapper;
  @Mock private PaymentOrderConverter paymentOrderConverter;
  @Mock private PaymentCompensationService paymentCompensationService;
  @Mock private AlipayConfig alipayConfig;
  @Mock private PaymentMessageProducer paymentMessageProducer;
  @Mock private OrderStatusRemoteService orderStatusRemoteService;
  @Mock private PaymentCallbackVerifier paymentCallbackVerifier;
  @Mock private PaymentStateMachine paymentStateMachine;
  @Mock private PaymentOrderStateSupport paymentOrderStateSupport;
  @Mock private PaymentSecurityCacheService paymentSecurityCacheService;
  @Mock private TradeMetrics tradeMetrics;

  private PaymentOrderServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new PaymentOrderServiceImpl(
            paymentOrderMapper,
            paymentRefundMapper,
            paymentCallbackLogMapper,
            paymentOrderConverter,
            paymentCompensationService,
            alipayConfig,
            paymentMessageProducer,
            orderStatusRemoteService,
            paymentCallbackVerifier,
            paymentStateMachine,
            paymentOrderStateSupport,
            paymentSecurityCacheService,
            List.<PaymentProviderGateway>of(),
            tradeMetrics);
  }

  @Test
  void getPaymentOrderByOrderNoUsesIndexedMapperQuery() {
    PaymentOrderEntity entity = new PaymentOrderEntity();
    entity.setId(11L);
    entity.setPaymentNo("PAY-11");

    PaymentOrderVO order = new PaymentOrderVO();
    order.setId(11L);
    order.setPaymentNo("PAY-11");

    when(paymentOrderMapper.selectLatestByMainOrderNoAndSubOrderNo("MAIN-1", "SUB-1"))
        .thenReturn(entity);
    when(paymentOrderConverter.toVO(entity)).thenReturn(order);

    PaymentOrderVO result = service.getPaymentOrderByOrderNo("MAIN-1", "SUB-1");

    assertSame(order, result);
    verify(paymentOrderMapper).selectLatestByMainOrderNoAndSubOrderNo("MAIN-1", "SUB-1");
    verify(paymentOrderConverter).toVO(entity);
  }

  @Test
  void getPaymentOrderByNoUsesIndexedMapperQuery() {
    PaymentOrderEntity entity = new PaymentOrderEntity();
    entity.setId(12L);
    entity.setPaymentNo("PAY-12");

    PaymentOrderVO order = new PaymentOrderVO();
    order.setId(12L);
    order.setPaymentNo("PAY-12");

    when(paymentOrderMapper.selectByPaymentNo("PAY-12")).thenReturn(entity);
    when(paymentOrderConverter.toVO(entity)).thenReturn(order);

    PaymentOrderVO result = service.getPaymentOrderByNo("PAY-12");

    assertSame(order, result);
    verify(paymentOrderMapper).selectByPaymentNo("PAY-12");
    verify(paymentOrderConverter).toVO(entity);
  }

  @Test
  void createPaymentOrderReusesExistingOrderFoundByOrderNumbers() {
    PaymentOrderCommandDTO command = new PaymentOrderCommandDTO();
    command.setPaymentNo("PAY-12");
    command.setMainOrderNo("MAIN-2");
    command.setSubOrderNo("SUB-2");
    command.setUserId(20001L);
    command.setAmount(new BigDecimal("99.00"));
    command.setChannel("ALIPAY");
    command.setIdempotencyKey("idem-12");

    OrderSubStatusVO orderStatus = new OrderSubStatusVO();
    orderStatus.setOrderStatus("CREATED");
    orderStatus.setUserId(20001L);
    orderStatus.setPayableAmount(new BigDecimal("99.00"));

    PaymentOrderEntity existingOrder = new PaymentOrderEntity();
    existingOrder.setId(12L);
    existingOrder.setStatus("CREATED");

    when(orderStatusRemoteService.getSubOrderStatus("MAIN-2", "SUB-2")).thenReturn(orderStatus);
    when(paymentSecurityCacheService.allowRateLimit(20001L)).thenReturn(true);
    when(paymentSecurityCacheService.getCachedResult("MAIN-2:SUB-2")).thenReturn(null);
    when(paymentOrderMapper.selectLatestByMainOrderNoAndSubOrderNo("MAIN-2", "SUB-2"))
        .thenReturn(existingOrder);

    Long result = service.createPaymentOrder(command);

    assertEquals(12L, result);
    verify(paymentOrderMapper).selectLatestByMainOrderNoAndSubOrderNo("MAIN-2", "SUB-2");
    verify(paymentSecurityCacheService).markIdempotent("MAIN-2:SUB-2", "idem-12");
    verify(paymentSecurityCacheService).cacheResult("MAIN-2:SUB-2", 12L);
    verify(paymentOrderMapper, never()).insert(any(PaymentOrderEntity.class));
  }

  @Test
  void createPaymentOrderAllowsCreatedOrderStatusWhenCreatingNewPayment() {
    PaymentOrderCommandDTO command = new PaymentOrderCommandDTO();
    command.setPaymentNo("PAY-21");
    command.setMainOrderNo("MAIN-21");
    command.setSubOrderNo("SUB-21");
    command.setUserId(20001L);
    command.setAmount(new BigDecimal("199.00"));
    command.setChannel("ALIPAY");
    command.setIdempotencyKey("idem-21");

    OrderSubStatusVO orderStatus = new OrderSubStatusVO();
    orderStatus.setOrderStatus("CREATED");
    orderStatus.setUserId(20001L);
    orderStatus.setPayableAmount(new BigDecimal("199.00"));

    PaymentOrderEntity entity = new PaymentOrderEntity();
    entity.setId(21L);
    entity.setPaymentNo("PAY-21");

    when(orderStatusRemoteService.getSubOrderStatus("MAIN-21", "SUB-21")).thenReturn(orderStatus);
    when(paymentSecurityCacheService.allowRateLimit(20001L)).thenReturn(true);
    when(paymentSecurityCacheService.getCachedResult("MAIN-21:SUB-21")).thenReturn(null);
    when(paymentOrderMapper.selectLatestByMainOrderNoAndSubOrderNo("MAIN-21", "SUB-21"))
        .thenReturn(null);
    when(paymentOrderMapper.selectByIdempotencyKey("idem-21")).thenReturn(null);
    when(paymentSecurityCacheService.tryAcquireIdempotent("MAIN-21:SUB-21", "idem-21"))
        .thenReturn(true);
    when(paymentOrderConverter.toEntity(command)).thenReturn(entity);

    Long result = service.createPaymentOrder(command);

    assertEquals(21L, result);
    verify(paymentOrderMapper).selectByIdempotencyKey("idem-21");
    verify(paymentOrderMapper).insert(entity);
    verify(paymentSecurityCacheService).markIdempotent("MAIN-21:SUB-21", "idem-21");
    verify(paymentSecurityCacheService).cacheResult("MAIN-21:SUB-21", 21L);
  }
}
