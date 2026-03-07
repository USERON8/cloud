package com.cloud.payment.service;

import com.cloud.payment.module.entity.PaymentOrderEntity;
import com.cloud.payment.module.entity.PaymentRefundEntity;

public interface PaymentCompensationService {

    void initializePaymentOrderCompensation(PaymentOrderEntity order);

    void submitRefund(PaymentOrderEntity order, PaymentRefundEntity refund);

    int reconcilePendingOrders();

    int retryPendingRefunds();
}
