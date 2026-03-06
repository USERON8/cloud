package com.cloud.payment.service;

import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.domain.vo.payment.PaymentRefundVO;

public interface PaymentOrderService {

    Long createPaymentOrder(PaymentOrderCommandDTO command);

    PaymentOrderVO getPaymentOrderByNo(String paymentNo);

    Boolean handlePaymentCallback(PaymentCallbackCommandDTO command);

    Long createRefund(PaymentRefundCommandDTO command);

    PaymentRefundVO getRefundByNo(String refundNo);
}
