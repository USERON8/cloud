package com.cloud.payment.v2.service;

import com.cloud.payment.v2.entity.PaymentOrderV2;
import com.cloud.payment.v2.entity.PaymentRefundV2;

public interface PaymentV2Service {
    PaymentOrderV2 createPaymentOrder(PaymentOrderV2 paymentOrder);
    PaymentOrderV2 markPaid(Long paymentId, String transactionNo);
    PaymentRefundV2 createRefund(PaymentRefundV2 paymentRefund);
    PaymentRefundV2 markRefunded(Long refundId, String refundTransactionNo);
}

