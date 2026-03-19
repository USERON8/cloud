package com.cloud.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.common.metrics.TradeMetrics;
import com.cloud.payment.config.PaymentCompensationProperties;
import com.cloud.payment.mapper.PaymentOrderMapper;
import com.cloud.payment.mapper.PaymentRefundMapper;
import com.cloud.payment.messaging.PaymentMessageProducer;
import com.cloud.payment.messaging.PaymentSuccessTxProducer;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import com.cloud.payment.service.provider.PaymentProviderGateway;
import com.cloud.payment.service.provider.model.PaymentOrderQueryResult;
import com.cloud.payment.service.support.PaymentOrderStateSupport;
import com.cloud.payment.service.support.PaymentSecurityCacheService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class PaymentCompensationServiceImplTest {

  @Mock private PaymentOrderMapper paymentOrderMapper;

  @Mock private PaymentRefundMapper paymentRefundMapper;

  @Mock private PaymentProviderGateway providerGateway;

  @Mock private PaymentMessageProducer paymentMessageProducer;

  @Mock private PaymentSuccessTxProducer paymentSuccessTxProducer;

  @Mock private TradeMetrics tradeMetrics;

  @Mock private PaymentSecurityCacheService paymentSecurityCacheService;

  @Mock private ObjectProvider<PaymentSuccessTxProducer> paymentSuccessTxProducerProvider;

  private PaymentCompensationServiceImpl paymentCompensationService;

  @BeforeEach
  void setUp() {
    PaymentCompensationProperties properties = new PaymentCompensationProperties();
    PaymentOrderStateSupport paymentOrderStateSupport =
        new PaymentOrderStateSupport(
            paymentSuccessTxProducerProvider, tradeMetrics, paymentSecurityCacheService);
    paymentCompensationService =
        new PaymentCompensationServiceImpl(
            paymentOrderMapper,
            paymentRefundMapper,
            properties,
            List.of(providerGateway),
            paymentMessageProducer,
            paymentOrderStateSupport);
  }

  @Test
  void initializePaymentOrderCompensation_setsScheduleFields() {
    PaymentOrderEntity order = new PaymentOrderEntity();
    paymentCompensationService.initializePaymentOrderCompensation(order);

    assertThat(order.getPollCount()).isEqualTo(0);
    assertThat(order.getLastPolledAt()).isNull();
    assertThat(order.getLastPollError()).isNull();
    assertThat(order.getNextPollAt()).isNotNull();
  }

  @Test
  void reconcilePendingOrders_paidOrder_updatesStatusAndPublishes() {
    PaymentOrderEntity order = new PaymentOrderEntity();
    order.setId(11L);
    order.setPaymentNo("P-1");
    order.setMainOrderNo("M-1");
    order.setSubOrderNo("S-1");
    order.setUserId(3L);
    order.setAmount(java.math.BigDecimal.TEN);
    order.setChannel("ALIPAY");
    order.setStatus("CREATED");
    order.setDeleted(0);
    order.setNextPollAt(LocalDateTime.now().minusMinutes(1));

    when(paymentOrderMapper.selectList(any())).thenReturn(List.of(order));
    when(providerGateway.supports("ALIPAY")).thenReturn(true);
    when(providerGateway.queryPaymentOrder(order))
        .thenReturn(PaymentOrderQueryResult.paid("TXN-1", LocalDateTime.now(), "ok"));
    when(paymentSuccessTxProducerProvider.getIfAvailable()).thenReturn(paymentSuccessTxProducer);

    int handled = paymentCompensationService.reconcilePendingOrders();

    assertThat(handled).isEqualTo(1);
    assertThat(order.getStatus()).isEqualTo("PAID");
    assertThat(order.getProviderTxnNo()).isEqualTo("TXN-1");
    assertThat(order.getNextPollAt()).isNull();

    ArgumentCaptor<PaymentOrderEntity> captor = ArgumentCaptor.forClass(PaymentOrderEntity.class);
    verify(paymentOrderMapper).updateById(captor.capture());
    verify(paymentSuccessTxProducer).send(any());
    verify(paymentSecurityCacheService).evictStatus("P-1");
    verify(tradeMetrics).incrementPayment("success");
  }
}
