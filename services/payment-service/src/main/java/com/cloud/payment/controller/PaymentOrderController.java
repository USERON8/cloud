package com.cloud.payment.controller;

import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.domain.vo.payment.PaymentRefundVO;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.payment.service.PaymentOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentOrderController {

    private final PaymentOrderService paymentOrderService;

    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('SCOPE_internal_api')")
    public Result<Long> createPaymentOrder(@Valid @RequestBody PaymentOrderCommandDTO command) {
        return Result.success(paymentOrderService.createPaymentOrder(command));
    }

    @GetMapping("/orders/{paymentNo}")
    @PreAuthorize("isAuthenticated()")
    public Result<PaymentOrderVO> getPaymentOrderByNo(@PathVariable String paymentNo, Authentication authentication) {
        PaymentOrderVO order = paymentOrderService.getPaymentOrderByNo(paymentNo);
        if (order == null) {
            return Result.notFound("payment order not found");
        }
        if (!canReadOrder(authentication, order)) {
            return Result.forbidden("forbidden to query other user's payment order");
        }
        return Result.success(order);
    }

    @PostMapping("/callbacks")
    @PreAuthorize("hasAuthority('SCOPE_internal_api')")
    public Result<Boolean> handleCallback(@Valid @RequestBody PaymentCallbackCommandDTO command) {
        return Result.success(paymentOrderService.handlePaymentCallback(command));
    }

    @PostMapping("/refunds")
    @PreAuthorize("hasAuthority('SCOPE_internal_api')")
    public Result<Long> createRefund(@Valid @RequestBody PaymentRefundCommandDTO command) {
        return Result.success(paymentOrderService.createRefund(command));
    }

    @GetMapping("/refunds/{refundNo}")
    @PreAuthorize("hasAuthority('SCOPE_internal_api')")
    public Result<PaymentRefundVO> getRefundByNo(@PathVariable String refundNo) {
        return Result.success(paymentOrderService.getRefundByNo(refundNo));
    }

    private boolean canReadOrder(Authentication authentication, PaymentOrderVO order) {
        if (SecurityPermissionUtils.hasAuthority(authentication, "SCOPE_internal_api")
                || SecurityPermissionUtils.isAdmin(authentication)) {
            return true;
        }
        String currentUserId = SecurityPermissionUtils.getCurrentUserId(authentication);
        return currentUserId != null && currentUserId.equals(String.valueOf(order.getUserId()));
    }
}
