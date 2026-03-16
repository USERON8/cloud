package com.cloud.payment.controller;

import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.domain.vo.payment.PaymentRefundVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.result.Result;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.payment.service.PaymentOrderService;
import com.cloud.payment.service.support.PaymentSecurityCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
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
@Tag(name = "Payment API", description = "Payment order and refund APIs")
public class PaymentOrderController {

  private final PaymentOrderService paymentOrderService;
  private final PaymentSecurityCacheService paymentSecurityCacheService;

  @PostMapping("/orders")
  @PreAuthorize("hasAuthority('order:create')")
  @Operation(summary = "Create payment order")
  public Result<Long> createPaymentOrder(@Valid @RequestBody PaymentOrderCommandDTO command) {
    return Result.success(paymentOrderService.createPaymentOrder(command));
  }

  @GetMapping("/orders/{paymentNo}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get payment order by number")
  public Result<PaymentOrderVO> getPaymentOrderByNo(
      @PathVariable String paymentNo, Authentication authentication) {
    PaymentOrderVO order = paymentOrderService.getPaymentOrderByNo(paymentNo);
    if (order == null) {
      throw new BizException(ResultCode.NOT_FOUND, "payment order not found");
    }
    if (!canReadOrder(authentication, order)) {
      throw new BizException(
          ResultCode.FORBIDDEN, "forbidden to query other user's payment order");
    }
    return Result.success(order);
  }

  @GetMapping("/orders/{paymentNo}/status")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get payment order status")
  public Result<Map<String, Object>> getPaymentStatus(
      @PathVariable String paymentNo, Authentication authentication) {
    PaymentSecurityCacheService.CachedStatus cached =
        paymentSecurityCacheService.getCachedStatus(paymentNo);
    if (cached != null) {
      if (!canReadStatus(authentication, cached.userId())) {
        throw new BizException(
            ResultCode.FORBIDDEN, "forbidden to query other user's payment status");
      }
      return Result.success(Map.of("paymentNo", paymentNo, "status", cached.status()));
    }

    PaymentOrderVO order = paymentOrderService.getPaymentOrderByNo(paymentNo);
    if (order == null) {
      throw new BizException(ResultCode.NOT_FOUND, "payment order not found");
    }
    if (!canReadOrder(authentication, order)) {
      throw new BizException(
          ResultCode.FORBIDDEN, "forbidden to query other user's payment status");
    }
    if (!paymentSecurityCacheService.isFinalStatus(order.getStatus())) {
      paymentSecurityCacheService.cacheStatus(paymentNo, order.getUserId(), order.getStatus());
    } else {
      paymentSecurityCacheService.evictStatus(paymentNo);
    }
    return Result.success(
        Map.of(
            "paymentNo", order.getPaymentNo(),
            "status", order.getStatus()));
  }

  @PostMapping("/callbacks")
  @PreAuthorize("hasAuthority('order:refund')")
  @Operation(summary = "Handle payment callback")
  public Result<Boolean> handleCallback(@Valid @RequestBody PaymentCallbackCommandDTO command) {
    return Result.success(paymentOrderService.handlePaymentCallback(command));
  }

  @PostMapping("/refunds")
  @PreAuthorize("hasAuthority('order:refund')")
  @Operation(summary = "Create payment refund")
  public Result<Long> createRefund(@Valid @RequestBody PaymentRefundCommandDTO command) {
    return Result.success(paymentOrderService.createRefund(command));
  }

  @GetMapping("/refunds/{refundNo}")
  @PreAuthorize("hasAuthority('order:refund')")
  @Operation(summary = "Get refund by number")
  public Result<PaymentRefundVO> getRefundByNo(@PathVariable String refundNo) {
    return Result.success(paymentOrderService.getRefundByNo(refundNo));
  }

  private boolean canReadOrder(Authentication authentication, PaymentOrderVO order) {
    if (SecurityPermissionUtils.isAdmin(authentication)) {
      return true;
    }
    String currentUserId = SecurityPermissionUtils.getCurrentUserId(authentication);
    return currentUserId != null && currentUserId.equals(String.valueOf(order.getUserId()));
  }

  private boolean canReadStatus(Authentication authentication, Long userId) {
    if (SecurityPermissionUtils.isAdmin(authentication)) {
      return true;
    }
    String currentUserId = SecurityPermissionUtils.getCurrentUserId(authentication);
    return currentUserId != null && userId != null && currentUserId.equals(String.valueOf(userId));
  }
}
