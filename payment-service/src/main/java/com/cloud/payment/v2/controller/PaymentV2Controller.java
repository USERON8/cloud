package com.cloud.payment.v2.controller;

import com.cloud.common.result.Result;
import com.cloud.payment.v2.entity.PaymentOrderV2;
import com.cloud.payment.v2.entity.PaymentRefundV2;
import com.cloud.payment.v2.service.PaymentV2Service;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/payments")
@RequiredArgsConstructor
public class PaymentV2Controller {

    private final PaymentV2Service paymentV2Service;

    @PostMapping("/order")
    public Result<PaymentOrderV2> createPaymentOrder(@RequestBody PaymentOrderV2 paymentOrder) {
        return Result.success(paymentV2Service.createPaymentOrder(paymentOrder));
    }

    @PostMapping("/order/{paymentId}/paid")
    public Result<PaymentOrderV2> markPaid(@PathVariable Long paymentId, @RequestBody MarkPaidRequest request) {
        return Result.success(paymentV2Service.markPaid(paymentId, request.getTransactionNo()));
    }

    @PostMapping("/refund")
    public Result<PaymentRefundV2> createRefund(@RequestBody PaymentRefundV2 paymentRefund) {
        return Result.success(paymentV2Service.createRefund(paymentRefund));
    }

    @PostMapping("/refund/{refundId}/done")
    public Result<PaymentRefundV2> markRefunded(@PathVariable Long refundId, @RequestBody MarkPaidRequest request) {
        return Result.success(paymentV2Service.markRefunded(refundId, request.getTransactionNo()));
    }

    @Data
    public static class MarkPaidRequest {
        private String transactionNo;
    }
}

