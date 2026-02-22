package com.cloud.payment.service;

import com.cloud.payment.module.dto.AlipayCreateRequest;
import com.cloud.payment.module.dto.AlipayCreateResponse;

import java.math.BigDecimal;
import java.util.Map;







public interface AlipayService {

    





    AlipayCreateResponse createPayment(AlipayCreateRequest request);

    





    boolean handleNotify(Map<String, String> params);

    





    boolean verifySign(Map<String, String> params);

    





    String queryPaymentStatus(String outTradeNo);

    







    boolean refund(String outTradeNo, BigDecimal refundAmount, String refundReason);

    





    boolean closeOrder(String outTradeNo);

    





    boolean verifyPayment(String outTradeNo);
}
