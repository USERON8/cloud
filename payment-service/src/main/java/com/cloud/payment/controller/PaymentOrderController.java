package com.cloud.payment.controller;

import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.domain.vo.payment.PaymentRefundVO;
import com.cloud.common.result.Result;
import com.cloud.payment.service.PaymentOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/payments")
@RequiredArgsConstructor
public class PaymentOrderController {

    private final PaymentOrderService paymentOrderService;

    @PostMapping("/orders")
    public Result<Long> createPaymentOrder(@Valid @RequestBody PaymentOrderCommandDTO command) {
        return Result.success(paymentOrderService.createPaymentOrder(command));
    }

    @GetMapping("/orders/{paymentNo}")
    public Result<PaymentOrderVO> getPaymentOrderByNo(@PathVariable String paymentNo) {
        return Result.success(paymentOrderService.getPaymentOrderByNo(paymentNo));
    }

    @PostMapping("/callbacks")
    public Result<Boolean> handleCallback(@Valid @RequestBody PaymentCallbackCommandDTO command) {
        return Result.success(paymentOrderService.handlePaymentCallback(command));
    }

    @PostMapping("/refunds")
    public Result<Long> createRefund(@Valid @RequestBody PaymentRefundCommandDTO command) {
        return Result.success(paymentOrderService.createRefund(command));
    }

    @GetMapping("/refunds/{refundNo}")
    public Result<PaymentRefundVO> getRefundByNo(@PathVariable String refundNo) {
        return Result.success(paymentOrderService.getRefundByNo(refundNo));
    }
}
