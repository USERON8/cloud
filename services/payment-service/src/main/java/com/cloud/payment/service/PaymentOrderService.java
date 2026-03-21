package com.cloud.payment.service;

import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentCheckoutSessionVO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.domain.vo.payment.PaymentRefundVO;

public interface PaymentOrderService {

  Long createPaymentOrder(PaymentOrderCommandDTO command);

  PaymentOrderVO getPaymentOrderByNo(String paymentNo);

  PaymentOrderVO getPaymentOrderByOrderNo(String mainOrderNo, String subOrderNo);

  PaymentCheckoutSessionVO createCheckoutSession(String paymentNo);

  String renderCheckoutPage(String ticket);

  Boolean handlePaymentCallback(PaymentCallbackCommandDTO command);

  Boolean handleInternalPaymentCallback(PaymentCallbackCommandDTO command);

  Long createRefund(PaymentRefundCommandDTO command);

  PaymentRefundVO getRefundByNo(String refundNo);

  Boolean cancelRefund(String refundNo, String reason);
}
