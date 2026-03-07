package com.cloud.payment.service.impl;

import com.cloud.payment.config.PaymentCompensationProperties;
import com.cloud.payment.mapper.PaymentOrderMapper;
import com.cloud.payment.mapper.PaymentRefundMapper;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import com.cloud.payment.module.entity.PaymentRefundEntity;
import com.cloud.payment.service.provider.PaymentProviderGateway;
import com.cloud.payment.service.provider.model.PaymentOrderQueryResult;
import com.cloud.payment.service.provider.model.PaymentRefundResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCompensationServiceImplTest {

    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    @Mock
    private PaymentRefundMapper paymentRefundMapper;

    @Mock
    private PaymentProviderGateway paymentProviderGateway;

    private PaymentCompensationServiceImpl paymentCompensationService;

    @BeforeEach
    void setUp() {
        PaymentCompensationProperties properties = new PaymentCompensationProperties();
        properties.getOrderQuery().setMaxAttempts(3);
        properties.getRefundRetry().setMaxAttempts(2);
        paymentCompensationService = new PaymentCompensationServiceImpl(
                paymentOrderMapper,
                paymentRefundMapper,
                properties,
                List.of(paymentProviderGateway)
        );
    }

    @Test
    void reconcilePendingOrdersShouldMarkPaid() {
        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setPaymentNo("PAY-100");
        order.setChannel("ALIPAY");
        order.setStatus("CREATED");
        order.setPollCount(0);
        order.setNextPollAt(LocalDateTime.now().minusSeconds(1));

        when(paymentOrderMapper.selectList(any())).thenReturn(List.of(order));
        when(paymentProviderGateway.supports("ALIPAY")).thenReturn(true);
        when(paymentProviderGateway.queryPaymentOrder(order))
                .thenReturn(PaymentOrderQueryResult.paid("TRADE-1", null, "TRADE_SUCCESS"));

        int handled = paymentCompensationService.reconcilePendingOrders();

        assertThat(handled).isEqualTo(1);
        assertThat(order.getStatus()).isEqualTo("PAID");
        assertThat(order.getProviderTxnNo()).isEqualTo("TRADE-1");
        assertThat(order.getPollCount()).isEqualTo(1);
        assertThat(order.getNextPollAt()).isNull();
        assertThat(order.getLastPollError()).isNull();
        verify(paymentOrderMapper).updateById(order);
    }

    @Test
    void retryPendingRefundsShouldMarkFailedWhenAttemptsExhausted() {
        PaymentRefundEntity refund = new PaymentRefundEntity();
        refund.setRefundNo("REF-200");
        refund.setPaymentNo("PAY-200");
        refund.setStatus("REFUNDING");
        refund.setRetryCount(1);
        refund.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
        refund.setRefundAmount(new BigDecimal("18.80"));
        refund.setReason("timeout");

        PaymentOrderEntity order = new PaymentOrderEntity();
        order.setPaymentNo("PAY-200");
        order.setChannel("ALIPAY");

        when(paymentRefundMapper.selectList(any())).thenReturn(List.of(refund));
        when(paymentOrderMapper.selectOne(any())).thenReturn(order);
        when(paymentProviderGateway.supports("ALIPAY")).thenReturn(true);
        when(paymentProviderGateway.executeRefund(order, refund))
                .thenReturn(PaymentRefundResult.error("gateway timeout"));

        int handled = paymentCompensationService.retryPendingRefunds();

        assertThat(handled).isEqualTo(1);
        assertThat(refund.getStatus()).isEqualTo("REFUND_FAILED");
        assertThat(refund.getRetryCount()).isEqualTo(2);
        assertThat(refund.getNextRetryAt()).isNull();
        assertThat(refund.getLastError()).contains("gateway timeout");
        verify(paymentRefundMapper).updateById(refund);
    }
}
