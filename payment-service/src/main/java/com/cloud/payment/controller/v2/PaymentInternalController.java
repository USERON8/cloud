package com.cloud.payment.controller.v2;

import com.cloud.api.payment.PaymentFeignClient;
import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.domain.vo.payment.PaymentRefundVO;
import com.cloud.payment.service.PaymentOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v2/payment")
@RequiredArgsConstructor
public class PaymentInternalController implements PaymentFeignClient {

    private final PaymentOrderService paymentOrderService;

    @Override
    @PostMapping("/orders")
    public Long createPaymentOrder(@RequestBody PaymentOrderCommandDTO command) {
        return paymentOrderService.createPaymentOrder(command);
    }

    @Override
    @GetMapping("/orders/{paymentNo}")
    public PaymentOrderVO getPaymentOrderByNo(@PathVariable("paymentNo") String paymentNo) {
        return paymentOrderService.getPaymentOrderByNo(paymentNo);
    }

    @Override
    @PostMapping("/callbacks")
    public Boolean handlePaymentCallback(@RequestBody PaymentCallbackCommandDTO command) {
        return paymentOrderService.handlePaymentCallback(command);
    }

    @Override
    @PostMapping("/refunds")
    public Long createRefund(@RequestBody PaymentRefundCommandDTO command) {
        return paymentOrderService.createRefund(command);
    }

    @Override
    @GetMapping("/refunds/{refundNo}")
    public PaymentRefundVO getRefundByNo(@PathVariable("refundNo") String refundNo) {
        return paymentOrderService.getRefundByNo(refundNo);
    }
}
