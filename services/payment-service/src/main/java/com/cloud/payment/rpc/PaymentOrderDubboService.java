package com.cloud.payment.rpc;

import com.cloud.api.payment.PaymentDubboApi;
import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.domain.vo.payment.PaymentRefundVO;
import com.cloud.payment.service.PaymentOrderService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(interfaceClass = PaymentDubboApi.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class PaymentOrderDubboService implements PaymentDubboApi {

    private final PaymentOrderService paymentOrderService;

    @Override
    public Long createPaymentOrder(PaymentOrderCommandDTO command) {
        return paymentOrderService.createPaymentOrder(command);
    }

    @Override
    public PaymentOrderVO getPaymentOrderByNo(String paymentNo) {
        return paymentOrderService.getPaymentOrderByNo(paymentNo);
    }

    @Override
    public PaymentOrderVO getPaymentOrderByOrderNo(String mainOrderNo, String subOrderNo) {
        return paymentOrderService.getPaymentOrderByOrderNo(mainOrderNo, subOrderNo);
    }

    @Override
    public Boolean handlePaymentCallback(PaymentCallbackCommandDTO command) {
        return paymentOrderService.handlePaymentCallback(command);
    }

    @Override
    public Long createRefund(PaymentRefundCommandDTO command) {
        return paymentOrderService.createRefund(command);
    }

    @Override
    public PaymentRefundVO getRefundByNo(String refundNo) {
        return paymentOrderService.getRefundByNo(refundNo);
    }

    @Override
    public Boolean cancelRefund(String refundNo, String reason) {
        return paymentOrderService.cancelRefund(refundNo, reason);
    }
}

