package com.cloud.payment.service.provider;

import com.cloud.payment.module.entity.PaymentOrderEntity;
import com.cloud.payment.module.entity.PaymentRefundEntity;
import com.cloud.payment.service.provider.model.PaymentOrderQueryResult;
import com.cloud.payment.service.provider.model.PaymentRefundResult;

public interface PaymentProviderGateway {

    boolean supports(String channel);

    PaymentOrderQueryResult queryPaymentOrder(PaymentOrderEntity order);

    PaymentRefundResult executeRefund(PaymentOrderEntity order, PaymentRefundEntity refund);
}
